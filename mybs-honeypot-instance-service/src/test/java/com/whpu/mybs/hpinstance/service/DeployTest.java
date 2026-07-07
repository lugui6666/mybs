package com.whpu.mybs.hpinstance.service;

import com.whpu.mybs.hpinstance.entity.HoneypotInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DeployTest {

    @Autowired
    private DockerDeployService dockerDeployService;  // 注入真实 Bean

    @Test
    public void testStartRealInstance() throws Exception {
        HoneypotInstance instance = new HoneypotInstance();
        instance.setId(1L);
        instance.setInstanceName("integration-test");
        instance.setStatus("DEPLOYING");
        instance.setTypeId(3L);  // 需要确保类型存在

        // 真正执行部署（注意：会实际调用 WSL/QEMU）
        dockerDeployService.startRealInstance(instance);
    }

    @Test
    public void testCleanupFailedInstance() {
        HoneypotInstance instance = new HoneypotInstance();
        instance.setId(1L);
        instance.setInstanceName("integration-test");
        instance.setStatus("DEPLOYING");
        instance.setTypeId(3L);  // 需要确保类型存在

        // 删除失败的实例
        dockerDeployService.cleanupFailedInstance(instance);
    }
}