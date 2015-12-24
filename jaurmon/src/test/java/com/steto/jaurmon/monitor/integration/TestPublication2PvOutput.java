package com.steto.jaurmon.monitor.integration;

import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurmon.monitor.*;
import com.steto.jaurmon.monitor.pvoutput.PvOutputRecord;
import com.steto.jaurmon.utils.HttpUtils;
import jssc.SerialPortException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public void shouldPublishInverterData() throws IOException, InterruptedException, SerialPortException {


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
        auroraMonitor.setPvOutputUrl(pvOutUrl);
        auroraMonitor.setPvOutputApiKey(pvOutKey);
        auroraMonitor.setPvOutputSystemId(systemId);
        auroraMonitor.setDailyCumulatedEnergy(701);
        auroraMonitor.setAllPowerGeneration(2001);
        auroraMonitor.setInverterTemperature(40.3);
        auroraMonitor.setAllGridVoltage(240.7);

        // execution
        auroraMonitor.publish2PvOutput();

        // verify
        String generatedRequest = fakePVOutputServer.getLastRequest();
        Map<String, String> queryMap = HttpUtils.getQueryMap(generatedRequest);

        Double dailyEnergy = Double.parseDouble(queryMap.get("v1"));
        Double power = Double.parseDouble(queryMap.get("v2"));
        Double temperature = Double.parseDouble(queryMap.get("v5"));
        Double volt = Double.parseDouble(queryMap.get("v6"));
        String date = queryMap.get("d");
        String time = queryMap.get("t");
        Integer id = Integer.parseInt(queryMap.get("sid"));
        String key = queryMap.get("key");

        assertEquals(key, pvOutKey);
        assertEquals(id, systemId);
        assertEquals(dailyEnergy, auroraMonitor.getDailyCumulatedEnergy(), 0.000001);
        assertEquals(power, auroraMonitor.getAllPowerGeneration(), 0.000001);
        assertEquals(temperature, auroraMonitor.getInverterTemperature(), 0.000001);
        assertEquals(volt, auroraMonitor.getAllGridVoltage(), 0.000001);
        assertNotNull(date);
        assertNotNull(time);


    }

    @Test
    public void shouldFailOnTimeout() throws IOException, InterruptedException, SerialPortException {


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


    }

    @Test
    public void shouldContactPVOutputServer() throws IOException, InterruptedException, SerialPortException {

        // setup
        Integer port = 8081;
        String pvOutService = "/pvoutputservice";
        String pvOutUrl = "http://localhost:" + port + pvOutService;
        String pvOutKey = "a908fds653";
        Integer systemId = 1223;

        FakePVOutputServer fakePVOutputServer = new FakePVOutputServer(port, pvOutKey, systemId, pvOutService);
        new Thread(fakePVOutputServer).start();
        Thread.sleep(500);

        AuroraMonitorTestImpl auroraMonitor = new AuroraMonitorTestImpl(mock(AuroraDriver.class), "resources/aurora.cfg", "pvoutput");
        auroraMonitor.setPvOutputUrl(pvOutUrl);
        auroraMonitor.setPvOutputApiKey(pvOutKey);
        auroraMonitor.setPvOutputSystemId(systemId);

        // execution
        auroraMonitor.testPvOutputServer();

        // verify
        String generatedRequest = fakePVOutputServer.getLastRequest();
        Map<String, String> queryMap = HttpUtils.getQueryMap(generatedRequest);

        Integer id = Integer.parseInt(queryMap.get("sid"));
        String key = queryMap.get("key");

        assertEquals(key, pvOutKey);
        assertEquals(id, systemId);

    }

    @Test
    public void shouldReadMalformedData() throws IOException, InterruptedException, SerialPortException {


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


    }

    @Test
    public void shouldReadSavePvOutputRecord() throws IOException, InterruptedException, SerialPortException {

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


    }

    @Test
    public void shouldBackupPvOutputDataOnUpdateFailureCauseServerRefusal() throws IOException, InterruptedException, SerialPortException {

        // setup
        int port = 8080;
        String pvOutService = "/pvoutputservice";
        String pvOutUrl = "http://localhost:" + port + pvOutService;
        String pvOutKey = "a908fds65";
        Integer systemId = 1223;

        FakePVOutputServer fakePVOutputServer = new FakePVOutputServer(port, "wrongKey", systemId, pvOutService);
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

    }

    @Test
    public void shouldBackupPvOutputDataOnUpdateFailure() throws IOException, InterruptedException, SerialPortException {

        // setup
        int port = 8080;
        String pvOutService = "/pvoutputservice";
        String pvOutUrl = "http://localhost:" + port + pvOutService;
        String pvOutKey = "a90fds653";
        Integer systemId = 1223;


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

    }

    @Test
    public void shouldPublishBatchDataFile2PvOutputCorrectly() throws IOException, InterruptedException, SerialPortException {


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
        auroraMonitor.setPvOutputUrl(pvOutUrl);
        auroraMonitor.setPvOutputApiKey(pvOutKey);
        auroraMonitor.setPvOutputSystemId(systemId);
        auroraMonitor.setDailyCumulatedEnergy(701);
        auroraMonitor.setAllPowerGeneration(2001);
        auroraMonitor.setInverterTemperature(40.3);
        auroraMonitor.setAllGridVoltage(240.7);


        PvOutputRecord pvOutputRecord1 = RandomObjectGenerator.getPvOutputRecord();
        PvOutputRecord pvOutputRecord2 = RandomObjectGenerator.getPvOutputRecord();

        auroraMonitor.savePvOutputRecord(pvOutputRecord1);
        String dataStorageFileName = auroraMonitor.savePvOutputRecord(pvOutputRecord2);

        // execution
        auroraMonitor.batchPublish2PvOutput(dataStorageFileName);

        // verify
        String generatedRequest = fakePVOutputServer.getLastRequest();
        Map<String, String> queryMap = HttpUtils.getQueryMap(generatedRequest);

        String csvData = (queryMap.get("data"));

        String[] recordsValues = csvData.split(";");
        String[] values = recordsValues[0].split(",");


        assertFalse( new File(dataStorageFileName).exists());

        org.junit.Assert.assertEquals(values[0], AuroraMonitor.convertDate(pvOutputRecord1.getDate()));
        assertEquals(values[1],  AuroraMonitor.convertDayTime(pvOutputRecord1.getDate()));
        assertEquals(Float.parseFloat(values[2]), pvOutputRecord1.dailyCumulatedEnergy, 0.0001);
        assertEquals(Float.parseFloat(values[3]), pvOutputRecord1.totalPowerGenerated, 0.0001);
        assertEquals(Float.parseFloat(values[4]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[5]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[6]), pvOutputRecord1.temperature, 0.0001);
        assertEquals(Float.parseFloat(values[7]), pvOutputRecord1.totalGridVoltage, 0.0001);

        values = recordsValues[1].split(",");

        assertEquals(values[0],  AuroraMonitor.convertDate(pvOutputRecord2.getDate()));
        assertEquals(values[1],  AuroraMonitor.convertDayTime(pvOutputRecord2.getDate()));
        assertEquals(Float.parseFloat(values[2]), pvOutputRecord2.dailyCumulatedEnergy, 0.0001);
        assertEquals(Float.parseFloat(values[3]), pvOutputRecord2.totalPowerGenerated, 0.0001);
        assertEquals(Float.parseFloat(values[4]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[5]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[6]), pvOutputRecord2.temperature, 0.0001);
        assertEquals(Float.parseFloat(values[7]), pvOutputRecord2.totalGridVoltage, 0.0001);


    }


}