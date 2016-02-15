package com.steto.jaurmon.monitor.core.integration;

import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurmon.monitor.AuroraMonitorTestImpl;
import com.steto.jaurmon.monitor.HwSettings;
import com.steto.jaurmon.monitor.MonitorSettings;
import com.steto.jaurmon.monitor.RandomObjectGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.steto.jaurmon.monitor.TestUtility.createAuroraConfigFile;
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

    public static void copyFile( File from, File to ) throws IOException {
        Files.copy(from.toPath(), to.toPath());
    }

    @Test
    public void shouldLoadAndSaveConfigurationFile() throws Exception {
        File temp = new File(configFileDirPath+"/aurora.cfg");
        String fileName = temp.getAbsolutePath();

        String serialPort="/dev/usb0";
        int inverterAddress=21;
        int baudRate=9600;
        float inverterQueryPeriodSec = (float) 30.5;

        HwSettings hwSettings = new HwSettings();
        hwSettings.serialPort = serialPort;
        hwSettings.inverterAddress = inverterAddress;
        hwSettings.serialPortBaudRate = baudRate;

        MonitorSettings monitorSettings = new MonitorSettings();
        monitorSettings.inverterInterrogationPeriodSec = inverterQueryPeriodSec;

        createAuroraConfigFile(fileName, RandomObjectGenerator.getA_HwSettings(), RandomObjectGenerator.getA_MonitorSettings());


        AuroraMonitorTestImpl auroraMonitorSave = new AuroraMonitorTestImpl(mock(AuroraDriver.class),fileName, configFileDirPath);

        auroraMonitorSave.setInverterAddress(inverterAddress);
        auroraMonitorSave.setSerialPortBaudRate(baudRate);
        auroraMonitorSave.setSerialPortName(serialPort);
        auroraMonitorSave.setInverterInterrogationPeriod(inverterQueryPeriodSec);

        auroraMonitorSave.saveHwSettingsConfiguration();
        auroraMonitorSave.saveConfiguration();


        AuroraMonitorTestImpl auroraMonitorLoad = new AuroraMonitorTestImpl(mock(AuroraDriver.class),fileName, configFileDirPath);


        assertEquals(serialPort,auroraMonitorLoad.getSerialPortName());
        assertEquals(inverterAddress,auroraMonitorLoad.getInverterAddress());
        assertEquals(baudRate,auroraMonitorLoad.getSerialPortBaudRate());
        assertEquals(inverterQueryPeriodSec,auroraMonitorLoad.getInverterInterrogationPeriod(), 0.0001);


    }

}
