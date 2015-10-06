package com.steto.jaurmon.monitor.integration;

import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurmon.monitor.AuroraMonitorTestImpl;
import jssc.SerialPortException;
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
    private String pvOutDirPath;

    @Before
    public void before() throws IOException {
        pvOutDirPath =  tempFolder.newFolder().getAbsolutePath();

    }

    @After
    public void after() throws IOException {
        tempFolder.delete();

    }

    @Test
    public void shouldLoadAndSaveConfigurationFile() throws IOException, SerialPortException {
        File temp = File.createTempFile("aurora", ".cfg");
        String fileName = temp.getAbsolutePath();

        AuroraMonitorTestImpl auroraMonitorSave = new AuroraMonitorTestImpl(mock(AuroraDriver.class),fileName,pvOutDirPath);

        // PvOutput params
        String key="abcdefgh01234";
        String url="http://unsito/service";
        int systemId=12345;
        int period=15;
        // Settings
        String serialPort="/dev/usb0";
        int inverterAddress=21;
        int baudRate=9600;

        auroraMonitorSave.setPvOutputSystemId(systemId);
        auroraMonitorSave.setPvOutputApiKey(key);
        auroraMonitorSave.setPvOutputUrl(url);
        auroraMonitorSave.setPvOutputPeriod(period);
        auroraMonitorSave.savePvOutputConfiguration();
        // cambio parametri Pvoutput
        auroraMonitorSave.setPvOutputSystemId(systemId+1);
        auroraMonitorSave.setPvOutputApiKey(key+"@");
        auroraMonitorSave.setPvOutputUrl(url+"l");
        auroraMonitorSave.setPvOutputPeriod(period+1);

        auroraMonitorSave.setInverterAddress(inverterAddress);
        auroraMonitorSave.setSerialPortBaudRate(baudRate);
        auroraMonitorSave.setSerialPortName(serialPort);
        auroraMonitorSave.saveHwSettingsConfiguration();


        AuroraMonitorTestImpl auroraMonitorLoad = new AuroraMonitorTestImpl(mock(AuroraDriver.class),fileName,pvOutDirPath);


        assertEquals(serialPort,auroraMonitorLoad.getSerialPortName());
        assertEquals(inverterAddress,auroraMonitorLoad.getInverterAddress());
        assertEquals(baudRate,auroraMonitorLoad.getSerialPortBaudRate());

        assertEquals(key,auroraMonitorLoad.getPvOutputApiKey());
        assertEquals(url,auroraMonitorLoad.getPvOutputUrl());
        assertEquals(systemId,auroraMonitorLoad.getPvOutputSystemId());
        assertEquals(period,auroraMonitorLoad.getPvOutputPeriod(),001);

    }
}
