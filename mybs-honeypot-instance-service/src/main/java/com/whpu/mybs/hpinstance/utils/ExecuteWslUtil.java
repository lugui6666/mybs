package com.whpu.mybs.hpinstance.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class ExecuteWslUtil {

    @Value("${deploy.wsl.distro:Ubuntu-22.04}")
    private String wslDistro;    // WSL 发行版名称，如 Ubuntu

    @Value("${deploy.timeout.seconds:300}")
    private int timeoutSeconds;  // 命令执行超时时间

    // ======================= WSL 命令执行工具 =======================

    /**
     * 在 WSL 中执行命令并返回标准输出（用于需要解析输出结果的场景）
     *
     * @param command 要执行的 bash 命令
     * @return 命令的标准输出字符串（已 trim）
     * @throws Exception 命令执行失败或超时
     */
    public String executeWsl(String command) throws Exception {
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
