package com.steto.jaurmon.monitor.pvoutput.integration;

import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurmon.monitor.*;
import jssc.SerialPortException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class TestPublication2PvOutput {


    private String pvOutDirPath=null;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void before() throws IOException {
        pvOutDirPath = tempFolder.newFolder().getAbsolutePath();

    }

    @After
    public void after() throws IOException {
        new File(pvOutDirPath).delete();
        //tempFolder.delete();

    }


    @Test
    public void shouldFailOnTimeout() throws IOException, InterruptedException, SerialPortException {

       /*
        // setup
        int port = 8080;
        String pvOutService = "/pvoutputservice";
        String pvOutUrl = "http://localhost:" + port + pvOutService;
        String pvOutKey = "a908fds653";
        Integer systemId = 1223;

        FakePVOutputServer fakePVOutputServer = new FakePVOutputServer(port, pvOutKey, systemId, pvOutService);
        fakePVOutputServer.setResponseDelay(5000);
        new Thread(fakePVOutputServer).start();
        Thread.sleep(1000);

        AuroraMonitorTestImpl auroraMonitor = new AuroraMonitorTestImpl(mock(AuroraDriver.class), "resources/aurora.cfg", pvOutDirPath);
        auroraMonitor.setPvOutputUrl(pvOutUrl);
        auroraMonitor.setPvOutputApiKey(pvOutKey);
        auroraMonitor.setPvOutputSystemId(systemId);
        auroraMonitor.setDailyCumulatedEnergy(701);
        auroraMonitor.setAllPowerGeneration(2001);
        auroraMonitor.setInverterTemperature(40.3);
        auroraMonitor.setAllGridVoltage(240.7);
        auroraMonitor.pvOutputHttpRequestTimeout = 1000;

        // execution
        boolean executed = auroraMonitor.publish2PvOutput();

        // verify
        assertFalse(executed);

         */
    }


    @Test
    public void shouldReadMalformedData() throws IOException, InterruptedException, SerialPortException {

        /*

        pvOutDirPath = "resources";

        AuroraMonitorTestImpl auroraMonitor = new AuroraMonitorTestImpl(mock(AuroraDriver.class), "resources/aurora.cfg", pvOutDirPath);

        PvOutputRecord expected = new PvOutputRecord();
        expected.timestamp=  1423302087286L;
        expected.dailyCumulatedEnergy= (float) 117.0;
        expected.totalPowerGenerated= (float) 344.0;
        expected.temperature= (float) 22.598003;
        expected.totalGridVoltage= (float) 234.2446;



        List<PvOutputRecord> pvOutputDataReadList = auroraMonitor.readPvOutputRecordSet("resources/2015-02-07_09.csv");


        assertTrue(expected.equals(pvOutputDataReadList.get(0)));
          */

    }

    @Test
    public void shouldReadSavePvOutputRecord() throws IOException, InterruptedException, SerialPortException {

        /*
        // setup
        int port = 8080;
        String pvOutService = "/pvoutputservice";
        String pvOutUrl = "http://localhost:" + port + pvOutService;
        String pvOutKey = "a908fds653";
        Integer systemId = 1223;


        FakePVOutputServer fakePVOutputServer = new FakePVOutputServer(port, pvOutKey, systemId, pvOutService);
        new Thread(fakePVOutputServer).start();
        Thread.sleep(1000);

        AuroraMonitorTestImpl auroraMonitor = new AuroraMonitorTestImpl(mock(AuroraDriver.class), "resources/aurora.cfg", pvOutDirPath);

        // execution
        PvOutputRecord pvData1 = RandomObjectGenerator.getPvOutputRecord();
        PvOutputRecord pvData2 = RandomObjectGenerator.getPvOutputRecord();
        List<PvOutputRecord> pvOutputDataToStore = new ArrayList<>();
        pvOutputDataToStore.add(pvData1);
        pvOutputDataToStore.add(pvData2);

        auroraMonitor.savePvOutputRecord(pvData1);
        String datafileName = auroraMonitor.savePvOutputRecord(pvData2);
        List<PvOutputRecord> pvOutputDataReadList = auroraMonitor.readPvOutputRecordSet(datafileName);


        assertTrue(pvOutputDataToStore.equals(pvOutputDataReadList));

          */
    }

    @Test
    public void shouldBackupPvOutputDataOnUpdateFailureCauseServerRefusal() throws Exception {

        // setup
        int port = 8080;
        String pvOutService = "/pvoutputservice";
        String pvOutUrl = "http://localhost:" + port + pvOutService;
        String pvOutKey = "a908fds65";
        Integer systemId = 1223;

        FakePVOutputServer fakePVOutputServer = new FakePVOutputServer(port, "wrongKey", systemId, pvOutService);
        new Thread(fakePVOutputServer).start();
        Thread.sleep(1000);

        /*
        AuroraMonitorTestImpl auroraMonitor = new AuroraMonitorTestImpl(mock(AuroraDriver.class), "resources/aurora.cfg", pvOutDirPath);
        auroraMonitor.setPvOutputUrl(pvOutUrl);
        auroraMonitor.setPvOutputApiKey(pvOutKey);
        auroraMonitor.setPvOutputSystemId(systemId);
        auroraMonitor.setDailyCumulatedEnergy(701);
        auroraMonitor.setAllPowerGeneration(2001);
        auroraMonitor.setInverterTemperature(40.3);
        auroraMonitor.setAllGridVoltage(240.7);

        // execution
        auroraMonitor.publish2PvOutput();
        List<PvOutputRecord> pvOutputDataReadList = null;

        File folder = new File(pvOutDirPath);
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                pvOutputDataReadList = auroraMonitor.readPvOutputRecordSet(file.getAbsolutePath());
            }
        }

        assertEquals(auroraMonitor.getAllGridVoltage(), pvOutputDataReadList.get(0).totalGridVoltage, 0.0001);
        assertEquals(auroraMonitor.getInverterTemperature(), pvOutputDataReadList.get(0).temperature, 0.0001);
        assertEquals(auroraMonitor.getCumulatedEnergyReadout(), pvOutputDataReadList.get(0).dailyCumulatedEnergy, 0.0001);
        assertEquals(auroraMonitor.getAllPowerGeneration(), pvOutputDataReadList.get(0).totalPowerGenerated, 0.0001);

          */
    }




}