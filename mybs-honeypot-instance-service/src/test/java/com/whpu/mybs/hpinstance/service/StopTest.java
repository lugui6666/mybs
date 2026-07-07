package com.whpu.mybs.hpinstance.service;

import com.whpu.mybs.hpinstance.entity.HoneypotInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class StopTest {

    @Autowired
    private DockerDeployService dockerDeployService;  // 注入真实 Bean

    @Test
    public void testStopRealInstance() throws Exception {
        HoneypotInstance instance = new HoneypotInstance();
        instance.setId(1L);
        instance.setIpAddress("192.168.100.10");
        instance.setPort(22001);
        instance.setInstanceName("integration-test");
        instance.setStatus("RUNNING");
        instance.setTypeId(3L);
        dockerDeployService.stopVm(instance);
    }
}
