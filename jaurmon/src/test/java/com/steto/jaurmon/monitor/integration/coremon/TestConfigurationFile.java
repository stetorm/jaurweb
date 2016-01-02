package com.steto.jaurmon.monitor.integration.coremon;

import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurmon.monitor.AuroraMonitorTestImpl;
import jssc.SerialPortException;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Created by stefano on 23/12/14.
 */
public class TestConfigurationFile {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private String configFileDirPath;

    @Before
    public void before() throws IOException {
        configFileDirPath =  tempFolder.newFolder().getAbsolutePath();

    }

    @After
    public void after() throws IOException {
        tempFolder.delete();

    }

    @Test
    public void shouldLoadAndSaveConfigurationFile() throws IOException, SerialPortException, ConfigurationException {
        File temp = File.createTempFile("aurora", ".cfg");
        String fileName = temp.getAbsolutePath();

        AuroraMonitorTestImpl auroraMonitorSave = new AuroraMonitorTestImpl(mock(AuroraDriver.class),fileName, configFileDirPath);

        // PvOutput params
        String key="abcdefgh01234";
        String url="http://unsito/service";
        int systemId=12345;
        int period=15;
        // Settings
        String serialPort="/dev/usb0";
        int inverterAddress=21;
        int baudRate=9600;


        auroraMonitorSave.setInverterAddress(inverterAddress);
        auroraMonitorSave.setSerialPortBaudRate(baudRate);
        auroraMonitorSave.setSerialPortName(serialPort);
        auroraMonitorSave.saveHwSettingsConfiguration();


        AuroraMonitorTestImpl auroraMonitorLoad = new AuroraMonitorTestImpl(mock(AuroraDriver.class),fileName, configFileDirPath);


        assertEquals(serialPort,auroraMonitorLoad.getSerialPortName());
        assertEquals(inverterAddress,auroraMonitorLoad.getInverterAddress());
        assertEquals(baudRate,auroraMonitorLoad.getSerialPortBaudRate());


    }
}
