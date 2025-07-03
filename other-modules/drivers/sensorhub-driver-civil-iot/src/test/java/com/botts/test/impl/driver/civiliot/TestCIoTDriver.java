package com.botts.test.impl.driver.civiliot;

import com.botts.impl.driver.civiliot.CIoTDriver;
import com.botts.impl.driver.civiliot.CIoTDriverConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.comm.HTTPConfig;

import java.util.ArrayList;

public class TestCIoTDriver {


    CIoTDriver driver;

    @Before
    public void setup() throws SensorHubException {
        if(!System.getProperties().containsKey("javax.net.ssl.trustStore") && !System.getProperties().containsKey("javax.net.ssl.trustStorePassword"))
            return;

        driver = new CIoTDriver();

        CIoTDriverConfig config = new CIoTDriverConfig();
        // Create some configs
        CIoTDriverConfig.DatastreamPollConfig pollConfig1 = new CIoTDriverConfig.DatastreamPollConfig();
        pollConfig1.pollInterval = 10;
        pollConfig1.datastreamId = 130;
        CIoTDriverConfig.DatastreamPollConfig pollConfig2 = new CIoTDriverConfig.DatastreamPollConfig();
        pollConfig2.pollInterval = 10;
        pollConfig2.datastreamId = 58;
        // Add Datastream configs to driver config
        config.datastreamIds = new ArrayList<>();
        config.datastreamIds.add(pollConfig1);
        config.datastreamIds.add(pollConfig2);
        // Other configs
        config.sensorThingsEndpoint = new HTTPConfig();
        config.sensorThingsEndpoint.remoteHost = "sta.ci.taiwan.gov.tw";
        config.sensorThingsEndpoint.remotePort = 443;
        config.sensorThingsEndpoint.resourcePath = "/STA_CCTV/v1.0/";
        config.sensorThingsEndpoint.enableTLS = true;

        // Configure and start driver
        driver.init(config);
        boolean b = driver.waitForState(ModuleEvent.ModuleState.INITIALIZED, 10000);
        int tries = 0;
        while (!b) {
            tries++;
            b = driver.waitForState(ModuleEvent.ModuleState.INITIALIZED, 10000);
            if (b || tries == 5) break;
        }
        driver.start();
    }

    @Test
    public void test() throws SensorHubException {
        if (driver != null)
            Assert.assertFalse(driver.getOutputs().isEmpty());
    }

}
