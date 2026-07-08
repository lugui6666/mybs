package com.whpu.mybs.hpinstance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whpu.mybs.common.dto.HoneypotTypeDTO;
import com.whpu.mybs.common.dto.R;
import com.whpu.mybs.common.enums.InstanceStatus;
import com.whpu.mybs.hpinstance.entity.HoneypotInstance;
import com.whpu.mybs.hpinstance.feign.HoneypotTypeFeignClient;
import com.whpu.mybs.hpinstance.mapper.HoneypotInstanceMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Docker/QEMU 部署服务（负责启动和清理虚拟机实例）
 * 采用 WSL 作为运行环境，使用 qemu-img 差异磁盘实现快速克隆，
 * 通过多播网络实现虚拟机间二层互通。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DockerDeployService {

    @Value("${deploy.wsl.distro:Ubuntu-22.04}")
    private String wslDistro;    // WSL 发行版名称，如 Ubuntu

    @Value("${deploy.image.source:/home/lugui/bs/images}")
    private String baseImagePath;  // 基础镜像在 WSL 中的路径

    @Value("${deploy.instance.base:/home/lugui/bs/instances}")
    private String instanceBasePath; // 实例目录在 WSL 中的基础路径

    @Value("${deploy.multicast.group:239.255.1.1}")
    private String multicastGroup;         // 多播组地址

    @Value("${deploy.multicast.port:1234}")
    private int multicastPort;             // 多播端口

    @Value("${deploy.subnet.prefix:192.168.100.}")
    private String subnetPrefix;           // 多播子网前缀（虚拟机 IP 将存为 prefix + (100+id)）

    @Value("${deploy.vm.ssh.password:123456}")
    private String vmPassword;             // 虚拟机 root 密码（用于 SSH 执行 docker compose）

    @Value("${deploy.timeout.seconds:300}")
    private int timeoutSeconds;  // 命令执行超时时间

    @Value("${deploy.ip.pool.start:192.168.100.10}")
    private String ipPoolStart;

    @Value("${deploy.ip.pool.end:192.168.100.250}")
    private String ipPoolEnd;

    private final HoneypotTypeFeignClient honeypotTypeFeignClient;

    private final Set<String> allocatedIps = ConcurrentHashMap.newKeySet();

    private final Set<Integer> allocatedPorts = ConcurrentHashMap.newKeySet();

    private final HoneypotInstanceMapper instanceMapper; // 注入 Mapper

    /**
     * 部署或启动实例（智能判断：若镜像不存在则克隆，否则直接启动）
     * 适用于首次部署和停止后重新启动
     *
     * @param instance 实例实体（包含 id, cpu, memory 等；部署成功后需由调用方更新 IP 和状态）
     * @throws Exception 任何步骤失败都会抛出异常，由调用方（Worker）触发清理
     */
    public void startRealInstance(HoneypotInstance instance) throws Exception {
        Long id = instance.getId();
        String instanceId = id.toString();
        String instanceDir = instanceBasePath + "/" + instanceId;
        String targetImagePath = instanceDir + "/disk.qcow2";

        // 1. 确保镜像存在（不存在则克隆）
        ensureImageExists(instance, targetImagePath);

        // 2. 分配资源（端口和IP）
        int sshPort = allocatePort(id.intValue());
        String vmIp = allocateIp();
        instance.setPort(sshPort);
        instance.setIpAddress(vmIp);

        try {
            // 3. 获取类型配置（用于CPU/内存）
            R<HoneypotTypeDTO> response = honeypotTypeFeignClient.getByTypeId(instance.getTypeId());
            HoneypotTypeDTO typeDTO = response.getData();
            int cpuCores = typeDTO.getMinCpu();
            int memoryMb = typeDTO.getMinMemory();

            // 4. 启动 QEMU
            String qemuCmd = buildQemuCommand(targetImagePath, sshPort, cpuCores, memoryMb, instance);
            log.info("启动 QEMU，命令: {}", qemuCmd);
            executeWsl(qemuCmd);

            // 5. 等待 SSH 就绪
            waitForSshReady(sshPort);

            // 6. 配置虚拟机网络（多播网卡 IP）
            configureVmNetwork(sshPort, vmIp);

            // 7. （可选）获取实际 IP 以确认
            String actualIp = getVmIp(sshPort);
            if (actualIp != null && !actualIp.isEmpty()) {
                instance.setIpAddress(actualIp);
                log.info("虚拟机 IP 确认: {}", actualIp);
            } else {
                log.warn("未获取到 eth1 IP，但已配置，使用分配的 IP: {}", vmIp);
            }

            // 8. （可选）在虚拟机内启动 MySQL
            // startMysqlInVm(sshPort);

            log.info("实例 {} 启动成功", instanceId);

        } catch (Exception e) {
            // 失败时释放已分配的资源
            freePort(sshPort);
            freeIp(vmIp);
            log.error("实例 {} 启动失败，已释放资源", instanceId, e);
            throw e;
        }
    }

    /**
     * 检查差异磁盘是否存在，若不存在则从基础镜像克隆
     */
    private void ensureImageExists(HoneypotInstance instance, String targetImagePath) throws Exception {
        String instanceId = instance.getId().toString();
        String instanceDir = targetImagePath.substring(0, targetImagePath.lastIndexOf('/'));

        // 检查目标镜像是否存在
        String checkCmd = "test -f " + targetImagePath;
        try {
            executeWsl(checkCmd);
            log.info("实例 {} 的镜像已存在: {}", instanceId, targetImagePath);
            return; // 存在，直接返回
        } catch (Exception e) {
            log.info("实例 {} 的镜像不存在，开始克隆", instanceId);
        }

        // 获取基础镜像路径
        R<HoneypotTypeDTO> response = honeypotTypeFeignClient.getByTypeId(instance.getTypeId());
        HoneypotTypeDTO typeDTO = response.getData();
        String imageName = typeDTO.getImageName();
        String baseImagePath = this.baseImagePath + "/" + imageName + ".qcow2";

        // 创建实例目录
        executeWsl("mkdir -p " + instanceDir);

        // 创建差异磁盘
        String createCmd = String.format(
                "qemu-img create -f qcow2 -b %s -F qcow2 %s",
                baseImagePath, targetImagePath
        );
        executeWsl(createCmd);
        log.info("差异磁盘创建成功: {}", targetImagePath);
    }

    /**
     * 分配端口（基于基端口 + 偏移量）
     */
    private int allocatePort(int offset) {
        int port = 22000 + offset;
        synchronized (allocatedPorts) {
            if (allocatedPorts.contains(port)) {
                throw new RuntimeException("端口 " + port + " 已被占用");
            }
            allocatedPorts.add(port);
            return port;
        }
    }

    /**
     * 释放端口
     */
    private void freePort(int port) {
        allocatedPorts.remove(port);
    }

    /**
     * 等待虚拟机 SSH 服务就绪（最多 60 秒）
     */
    private void waitForSshReady(int sshPort) throws Exception {
        String sshCmd = String.format(
                "sshpass -p '%s' ssh -o StrictHostKeyChecking=no -p %d root@localhost 'echo ready'",
                vmPassword, sshPort
        );
        boolean ready = false;
        for (int i = 0; i < 30; i++) {
            try {
                String result = executeWsl(sshCmd);
                if ("ready".equals(result.trim())) {
                    ready = true;
                    break;
                }
            } catch (Exception e) {
                log.debug("SSH 未就绪，第 {} 次尝试...", i + 1);
            }
            TimeUnit.SECONDS.sleep(2);
        }
        if (!ready) {
            throw new RuntimeException("SSH 服务在 60 秒内未就绪");
        }
        log.info("SSH 服务已就绪");
    }

    /**
     * 在虚拟机内启动 MySQL 容器（docker compose up -d）
     */
    private void startMysqlInVm(int sshPort) throws Exception {
        String cmd = String.format(
                "sshpass -p '%s' ssh -o StrictHostKeyChecking=no -p %d root@localhost 'cd /app && docker compose up -d'",
                vmPassword, sshPort
        );
        executeWsl(cmd);
        log.info("MySQL 容器已启动");
    }

    /**
     * 停止虚拟机实例（通过 SSH 发送 poweroff，优雅关机）
     */
    public void stopVm(HoneypotInstance instance) throws Exception {
        Long id = instance.getId();
        String instanceId = id.toString();
        int sshPort = instance.getPort();

        log.info("开始停止虚拟机实例: {} (SSH端口: {})", instanceId, sshPort);

        // 1. 通过 SSH 发送 poweroff 命令
        String shutdownCmd = String.format(
                "sshpass -p '%s' ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -p %d root@localhost 'poweroff'",
                vmPassword, sshPort
        );

        try {
            // 执行关机命令（忽略返回码，因为 SSH 可能在关机过程中断开）
            executeWsl(shutdownCmd);
        } catch (Exception e) {
            // 关机命令可能导致 SSH 连接提前断开，这里不视为致命错误
            log.warn("执行 poweroff 命令时 SSH 连接已断开（正常情况）: {}", e.getMessage());
        }

        // 2. 等待 QEMU 进程退出（最多等待 30 秒）
        boolean stopped = false;
        for (int i = 0; i < 30; i++) {
            // 检查 PID 文件是否还存在
            String checkCmd = String.format(
                    "if [ -f /tmp/qemu-%s.pid ]; then kill -0 $(cat /tmp/qemu-%s.pid) 2>/dev/null; else exit 1; fi",
                    instanceId, instanceId
            );
            try {
                executeWsl(checkCmd);
                // 进程还在，继续等待
                log.debug("等待 QEMU 进程退出... {}s", i + 1);
                TimeUnit.SECONDS.sleep(1);
            } catch (Exception e) {
                // 进程已退出或 PID 文件已删除
                stopped = true;
                break;
            }
        }

        // 3. 如果 30 秒后仍未停止，强制 kill
        if (!stopped) {
            log.warn("虚拟机 {} 未在 30 秒内优雅关闭，执行强制 kill", instanceId);
            String forceKillCmd = String.format(
                    "pkill -F /tmp/qemu-%s.pid 2>/dev/null || pkill -f 'qemu-system.*%s' 2>/dev/null || true",
                    instanceId, instanceId
            );
            executeWsl(forceKillCmd);
        }

        // 4. 清理辅助文件（PID、Monitor Socket、日志）
        String cleanCmd = String.format(
                "rm -f /tmp/qemu-%s.pid /tmp/qemu-%s-monitor.sock /tmp/qemu-%s.log",
                instanceId, instanceId, instanceId
        );
        executeWsl(cleanCmd);

        if (instance.getIpAddress() != null && !instance.getIpAddress().isEmpty()) {
            freeIp(instance.getIpAddress());
        }

        log.info("虚拟机实例 {} 已停止，资源已释放", instanceId);
    }

    /**
     * 销毁虚拟机实例：删除镜像文件、释放端口和 IP、清理临时文件
     */
    public void destroyVm(HoneypotInstance instance) throws Exception {
        Long id = instance.getId();
        String instanceId = id.toString();
        String instanceDir = instanceBasePath + "/" + instanceId;
        String imagePath = instanceDir + "/disk.qcow2";

        log.info("开始销毁实例 {} 的资源", instanceId);

        // 1. 确保虚拟机已停止（如果还在运行则强制 kill，但调用前应已 stop）
        String forceKillCmd = String.format(
                "pkill -F /tmp/qemu-%s.pid 2>/dev/null || pkill -f 'qemu-system.*%s' 2>/dev/null || true",
                instanceId, instanceId
        );
        executeWsl(forceKillCmd);

        // 2. 删除镜像文件
        String deleteImageCmd = "rm -f " + imagePath;
        executeWsl(deleteImageCmd);
        log.info("已删除镜像文件: {}", imagePath);

        // 3. 删除实例目录（如果为空目录，删除；否则可能还有其它文件，但通常只有镜像）
        String deleteDirCmd = "rm -rf " + instanceDir;
        executeWsl(deleteDirCmd);
        log.info("已删除实例目录: {}", instanceDir);

        // 4. 删除临时文件（PID、Monitor Socket、日志）
        String cleanCmd = String.format(
                "rm -f /tmp/qemu-%s.pid /tmp/qemu-%s-monitor.sock /tmp/qemu-%s.log",
                instanceId, instanceId, instanceId
        );
        executeWsl(cleanCmd);

        // 5. 释放端口和 IP（如果已分配）
        if (instance.getPort() != null && instance.getPort() > 0) {
            freePort(instance.getPort());
        }
        if (instance.getIpAddress() != null && !instance.getIpAddress().isEmpty()) {
            freeIp(instance.getIpAddress());
        }

        log.info("实例 {} 资源销毁完成", instanceId);
    }

    /**
     * 服务启动时自动调用，恢复实例状态
     */
    @PostConstruct
    public void restore() {
        log.info("开始恢复实例状态...");

        // 1. 查询数据库中所有状态为 RUNNING 的实例
        LambdaQueryWrapper<HoneypotInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HoneypotInstance::getStatus, InstanceStatus.RUNNING.getCode());
        List<HoneypotInstance> runningInstances = instanceMapper.selectList(wrapper);

        if (runningInstances.isEmpty()) {
            log.info("没有正在运行的实例需要恢复");
            return;
        }

        log.info("发现 {} 个数据库标记为 RUNNING 的实例", runningInstances.size());

        // 2. 扫描 WSL 中所有正在运行的 QEMU 进程（获取 PID 和实例 ID 映射）
        Map<String, Integer> aliveProcesses = scanRunningQemuProcesses();

        // 3. 遍历每个实例，检查进程是否存活
        for (HoneypotInstance instance : runningInstances) {
            String instanceId = instance.getId().toString();
            Integer pid = aliveProcesses.get(instanceId);

            if (pid != null && pid > 0) {
                // 进程存活，恢复资源
                log.info("实例 {} (PID={}) 仍在运行，恢复资源占用", instanceId, pid);
                // 恢复端口占用
                if (instance.getPort() != null) {
                    allocatedPorts.add(instance.getPort());
                }
                // 恢复 IP 占用
                if (instance.getIpAddress() != null && !instance.getIpAddress().isEmpty()) {
                    allocatedIps.add(instance.getIpAddress());
                }
                // 可选：将实例信息重新放入内存缓存（如果有 VmInfo 缓存）
                // 这里可以调用一个方法重建缓存，但当前设计没有 VmInfo，所以暂时略过
            } else {
                // 进程不存在，更新数据库状态为 ERROR
                log.warn("实例 {} 标记为 RUNNING 但 QEMU 进程不存在，更新状态为 ERROR", instanceId);
                instance.setStatus(InstanceStatus.ERROR.getCode());
                instanceMapper.updateById(instance);
                // 释放数据库中的资源（端口/IP 不需要手动释放，因为内存中没占用，新部署可复用）
                // 但为避免重复，建议后续清理残留文件
                cleanupFailedInstance(instance); // 可调用已有的清理方法
            }
        }

        // 4. 处理孤儿进程（数据库无记录但进程仍在运行）
        Set<String> dbInstanceIds = runningInstances.stream()
                .map(inst -> inst.getId().toString())
                .collect(Collectors.toSet());
        for (Map.Entry<String, Integer> entry : aliveProcesses.entrySet()) {
            String instanceId = entry.getKey();
            if (!dbInstanceIds.contains(instanceId)) {
                log.warn("发现孤儿进程：实例 {} (PID={})，数据库无对应记录，强制终止", instanceId, entry.getValue());
                // 强制终止进程并清理文件
                try {
                    String killCmd = String.format(
                            "pkill -F /tmp/qemu-%s.pid 2>/dev/null || pkill -f 'qemu-system.*%s' 2>/dev/null || true",
                            instanceId, instanceId
                    );
                    executeWsl(killCmd);
                    // 清理相关文件
                    executeWsl("rm -f /tmp/qemu-" + instanceId + ".pid /tmp/qemu-" + instanceId + "-monitor.sock /tmp/qemu-" + instanceId + ".log");
                } catch (Exception e) {
                    log.error("清理孤儿进程 {} 失败", instanceId, e);
                }
            }
        }

        log.info("实例状态恢复完成，已恢复 {} 个 RUNNING 实例，清理孤儿进程 {} 个",
                runningInstances.size() - (int) aliveProcesses.values().stream().filter(p -> p == 0).count(),
                aliveProcesses.size() - dbInstanceIds.size());
    }

    /**
     * 扫描 WSL 中正在运行的 QEMU 进程，返回 map: instanceId -> PID
     */
    private Map<String, Integer> scanRunningQemuProcesses() {
        Map<String, Integer> result = new HashMap<>();
        try {
            // 通过 /tmp/qemu-*.pid 文件扫描
            String scanCmd =
                    "for pid_file in /tmp/qemu-*.pid; do " +
                            "  if [ -f \"$pid_file\" ]; then " +
                            "    PID=$(cat \"$pid_file\" 2>/dev/null) && " +
                            "    if kill -0 $PID 2>/dev/null; then " +
                            "      INSTANCE_ID=$(basename \"$pid_file\" .pid | sed 's/qemu-//') && " +
                            "      echo \"$INSTANCE_ID|$PID\" " +
                            "    fi " +
                            "  fi " +
                            "done";
            String output = executeWsl(scanCmd);
            for (String line : output.split("\n")) {
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    result.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                }
            }
            log.info("扫描到 {} 个正在运行的 QEMU 进程", result.size());
        } catch (Exception e) {
            log.error("扫描 QEMU 进程失败", e);
        }
        return result;
    }

    // ======================= 构建 QEMU 命令 =======================

    /**
     * 构建完整的 QEMU 启动命令（参考 Rust 实现）
     * 包含双网卡（用户模式用于 SSH 管理，多播模式用于虚拟机间通信）、
     * 监控 socket、串口日志、后台运行等。
     *
     * @param imagePath   差异磁盘路径
     * @param sshHostPort 宿主机映射的 SSH 端口
     * @param cpuCores    CPU 核数
     * @param memoryMb    内存大小 (MB)
     * @param instance    实例对象（用于获取 ID 等信息）
     * @return 完整的 QEMU 命令行字符串
     */
    private String buildQemuCommand(String imagePath, int sshHostPort,
                                    int cpuCores, int memoryMb, HoneypotInstance instance) {
        Long id = instance.getId();
        String instanceId = id.toString();
        String vmName = "honeypot-" + instanceId;
        String macAddr = generateMacAddress(id);                 // 多播网卡 MAC
        String monitorSocket = "/tmp/qemu-" + instanceId + "-monitor.sock";
        String serialLog = "/tmp/qemu-" + instanceId + ".log";

        return String.format(
                "qemu-system-x86_64 " +
                        "-name '%s' " +
                        "-m %dM " +
                        "-smp cores=%d " +
                        "-drive file='%s',format=qcow2,if=virtio " +
                        // 网卡1：用户模式（用于 SSH 端口转发，方便管理）
                        "-netdev user,id=n1,hostfwd=tcp::%d-:22 " +
                        "-device e1000,netdev=n1 " +
                        // 网卡2：多播 socket（虚拟机间二层互通）
                        "-netdev socket,id=n2,mcast=%s:%d " +
                        "-device e1000,netdev=n2,mac='%s' " +
                        // 后台运行
                        "-daemonize " +
                        // 监控 socket（可执行 qemu-monitor 命令）
                        "-monitor unix:%s,server,nowait " +
                        // 串口日志（内核启动信息）
                        "-serial file:%s " +
                        "-pidfile /tmp/qemu-%s.pid",
                vmName, memoryMb, cpuCores, imagePath,
                sshHostPort,
                multicastGroup, multicastPort,
                macAddr,
                monitorSocket, serialLog,
                instanceId
        );
    }

    /**
     * 生成唯一的 MAC 地址（基于实例 ID）
     * 使用 QEMU 官方推荐的 OUI 前缀 52:54:00
     *
     * @param id 实例 ID
     * @return MAC 地址字符串 (如 52:54:00:12:34:56)
     */
    private String generateMacAddress(Long id) {
        String hex = String.format("%06x", id.intValue() & 0xFFFFFF);
        return "52:54:00:" +
                hex.substring(0, 2) + ":" +
                hex.substring(2, 4) + ":" +
                hex.substring(4, 6);
    }

    /**
     * 从 IP 池中分配一个可用的 IP（按顺序遍历，跳过已占用的）
     */
    private String allocateIp() throws UnknownHostException {
        Inet4Address start = (Inet4Address) InetAddress.getByName(ipPoolStart);
        Inet4Address end = (Inet4Address) InetAddress.getByName(ipPoolEnd);
        int startInt = ByteBuffer.wrap(start.getAddress()).getInt();
        int endInt = ByteBuffer.wrap(end.getAddress()).getInt();
        if (startInt > endInt) {
            throw new RuntimeException("IP 池起始地址大于结束地址");
        }
        synchronized (allocatedIps) {
            for (int ipInt = startInt; ipInt <= endInt; ipInt++) {
                byte[] bytes = ByteBuffer.allocate(4).putInt(ipInt).array();
                String ip = InetAddress.getByAddress(bytes).getHostAddress();
                if (!allocatedIps.contains(ip)) {
                    allocatedIps.add(ip);
                    return ip;
                }
            }
            throw new RuntimeException("IP 池已耗尽");
        }
    }

    /**
     * 释放 IP 地址
     */
    private void freeIp(String ip) {
        allocatedIps.remove(ip);
    }

    /**
     * 通过 SSH 配置虚拟机 eth1 网卡的 IP
     */
    private void configureVmNetwork(int sshPort, String vmIp) throws Exception {
        String cmd = String.format(
                "sshpass -p '%s' ssh -o StrictHostKeyChecking=no -p %d root@localhost " +
                        "'ip link set eth1 up && ip addr add %s/24 dev eth1'",
                vmPassword, sshPort, vmIp
        );
        executeWsl(cmd);
        log.info("已配置虚拟机 eth1 IP: {}", vmIp);
    }

    /**
     * 通过 SSH 获取虚拟机的 eth1 IP（若未配置则返回空）
     */
    private String getVmIp(int sshPort) throws Exception {
        String cmd = String.format(
                "sshpass -p '%s' ssh -o StrictHostKeyChecking=no -p %d root@localhost " +
                        "'ip -4 addr show eth1 | grep inet | awk \"{print \\$2}\" | cut -d/ -f1'",
                vmPassword, sshPort
        );
        String ip = executeWsl(cmd);
        if (ip.isEmpty()) {
            return null;
        }
        return ip.trim();
    }

    // ======================= 清理失败实例 =======================

    /**
     * 清理因部署失败而残留的资源（QEMU 进程、磁盘文件、监控 socket、日志等）
     *
     * @param instance 要清理的实例（ID 必须有效）
     */
    public void cleanupFailedInstance(HoneypotInstance instance) {
        Long id = instance.getId();
        String instanceId = id.toString();
        String instanceDir = instanceBasePath + "/" + instanceId;

        try {
            log.info("开始清理实例 {} 的资源", instanceId);

            // 1. 强制杀死 QEMU 进程（通过 pidfile）
            executeWsl("pkill -F /tmp/qemu-" + instanceId + ".pid 2>/dev/null || true");

            // 2. 删除实例目录（包含差异磁盘）
            executeWsl("rm -rf " + instanceDir);

            // 3. 删除辅助文件（PID、监控 socket、串口日志）
            executeWsl("rm -f /tmp/qemu-" + instanceId + ".pid");
            executeWsl("rm -f /tmp/qemu-" + instanceId + "-monitor.sock");
            executeWsl("rm -f /tmp/qemu-" + instanceId + ".log");

            log.info("实例 {} 资源清理完成", instanceId);

        } catch (Exception e) {
            // 清理失败不应阻断流程，但需记录错误以便人工介入
            log.error("清理实例 {} 过程中发生异常，可能需要手动处理", instanceId, e);
        }
    }

    // ======================= WSL 命令执行工具 =======================

    /**
     * 在 WSL 中执行命令并返回标准输出（用于需要解析输出结果的场景）
     *
     * @param command 要执行的 bash 命令
     * @return 命令的标准输出字符串（已 trim）
     * @throws Exception 命令执行失败或超时
     */
    private String executeWsl(String command) throws Exception {
        // 启动 wsl bash -s，从 stdin 读取命令
        String[] cmdArray = {"cmd.exe", "/c", "wsl", "-d", wslDistro, "bash", "-s"};
        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 将命令写入 stdin
        try (OutputStream os = process.getOutputStream()) {
            os.write(command.getBytes(StandardCharsets.UTF_8));
            os.write('\n');  // 命令以换行结束
            os.flush();
        }

        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("命令执行超时: " + command);
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException(String.format(
                    "命令执行失败，退出码 %d，输出: %s", exitCode, output
            ));
        }
        return output.toString().trim();
    }
}