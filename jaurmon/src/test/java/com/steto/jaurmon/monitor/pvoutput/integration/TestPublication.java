package com.steto.jaurmon.monitor.pvoutput.integration;

import com.google.common.eventbus.EventBus;
import com.steto.jaurmon.monitor.FakePVOutputServer;
import com.steto.jaurmon.monitor.MonitorMsgInverterStatus;
import com.steto.jaurmon.monitor.PeriodicInverterTelemetries;
import com.steto.jaurmon.monitor.RandomObjectGenerator;
import com.steto.jaurmon.monitor.pvoutput.PVOutputParams;
import com.steto.jaurmon.monitor.pvoutput.PvOutputNew;
import com.steto.jaurmon.monitor.pvoutput.PvOutputRecord;
import com.steto.jaurmon.utils.HttpUtils;
import jssc.SerialPortException;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.steto.jaurmon.monitor.RandomObjectGenerator.getInt;
import static com.steto.jaurmon.monitor.TestUtility.createPvoutputConfigFile;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;

/**
 * Created by stefano on 14/02/16.
 */
public class TestPublication {


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
    /*
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

        org.junit.Assert.assertEquals(values[0], AuroraMonitorOld.convertDate(pvOutputRecord1.getDate()));
        assertEquals(values[1],  AuroraMonitorOld.convertDayTime(pvOutputRecord1.getDate()));
        assertEquals(Float.parseFloat(values[2]), pvOutputRecord1.dailyCumulatedEnergy, 0.0001);
        assertEquals(Float.parseFloat(values[3]), pvOutputRecord1.totalPowerGenerated, 0.0001);
        assertEquals(Float.parseFloat(values[4]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[5]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[6]), pvOutputRecord1.temperature, 0.0001);
        assertEquals(Float.parseFloat(values[7]), pvOutputRecord1.totalGridVoltage, 0.0001);

        values = recordsValues[1].split(",");

        assertEquals(values[0],  AuroraMonitorOld.convertDate(pvOutputRecord2.getDate()));
        assertEquals(values[1],  AuroraMonitorOld.convertDayTime(pvOutputRecord2.getDate()));
        assertEquals(Float.parseFloat(values[2]), pvOutputRecord2.dailyCumulatedEnergy, 0.0001);
        assertEquals(Float.parseFloat(values[3]), pvOutputRecord2.totalPowerGenerated, 0.0001);
        assertEquals(Float.parseFloat(values[4]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[5]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[6]), pvOutputRecord2.temperature, 0.0001);
        assertEquals(Float.parseFloat(values[7]), pvOutputRecord2.totalGridVoltage, 0.0001);
      */

    }

    @Test
    public void shouldSaveAndPublishData() throws IOException, ConfigurationException, InterruptedException {

        //Setup
        Integer pvOutputPort = getInt(65532);
        String pvOutServiceUrl = "/pvoutputservice";
        String pvOutUrl = "http://localhost:" + pvOutputPort + pvOutServiceUrl;

        PVOutputParams pvOutputParams = RandomObjectGenerator.getA_PvOutputParams();
        pvOutputParams.url = pvOutUrl;
        pvOutputParams.period = (float) 0.2;
        pvOutputParams.timeWindowSec = (float) 0.4;

        FakePVOutputServer fakePVOutputServer = new FakePVOutputServer(pvOutputPort, pvOutputParams.apiKey, pvOutputParams.systemId, pvOutServiceUrl);

        EventBus eventBus = new EventBus();
        File tempFile = File.createTempFile("aurora", "cfg");
        createPvoutputConfigFile(tempFile.getAbsolutePath(), pvOutputParams);
        PvOutputNew pvOutput = new PvOutputNew(tempFile.getAbsolutePath(), eventBus);
        pvOutput.start();
        Thread.sleep(300);


        MonitorMsgInverterStatus monitorMsgInverterStatus = new MonitorMsgInverterStatus(false);

        //Exercise
        PeriodicInverterTelemetries periodicInverterTelemetries1 = RandomObjectGenerator.getA_PeriodicInverterTelemetries();
        eventBus.post( periodicInverterTelemetries1 );
        Thread.sleep((long) (pvOutputParams.timeWindowSec*1000*1.1));
        PeriodicInverterTelemetries periodicInverterTelemetries2 = RandomObjectGenerator.getA_PeriodicInverterTelemetries();
        eventBus.post(periodicInverterTelemetries2);
        Thread.sleep(2000);
        pvOutput.stop();

        PvOutputNew pvOutputSecondRun = new PvOutputNew(tempFile.getAbsolutePath(), eventBus);
        pvOutputSecondRun.start();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> fakeServerExecutorFuture = executor.submit(fakePVOutputServer);
        eventBus.post(monitorMsgInverterStatus);

        //Verify
        String httpString = fakePVOutputServer.waitForRequest(2000);
        assertNotNull(httpString);
        String generatedRequest = fakePVOutputServer.getLastRequest();
        Map<String, String> queryMap = HttpUtils.getQueryMap(generatedRequest);

        String csvData = (queryMap.get("data"));

        String[] recordsValues = csvData.split(";");
        String[] values = recordsValues[0].split(",");

        PvOutputRecord pvOutputRecord1 = new PvOutputRecord(periodicInverterTelemetries1);
        PvOutputRecord pvOutputRecord2 = new PvOutputRecord(periodicInverterTelemetries2);

        assertEquals(values[0], PvOutputNew.convertDate(pvOutputRecord1.getDate()));
        assertEquals(values[1], PvOutputNew.convertDayTime(pvOutputRecord1.getDate()));
        assertEquals(Float.parseFloat(values[2]), pvOutputRecord1.dailyCumulatedEnergy, 0.0001);
        assertEquals(Float.parseFloat(values[3]), pvOutputRecord1.totalPowerGenerated, 0.0001);
        assertEquals(Float.parseFloat(values[4]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[5]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[6]), pvOutputRecord1.temperature, 0.0001);
        assertEquals(Float.parseFloat(values[7]), pvOutputRecord1.totalGridVoltage, 0.0001);

        values = recordsValues[1].split(",");

        assertEquals(values[0], PvOutputNew.convertDate(pvOutputRecord2.getDate()));
        assertEquals(values[1], PvOutputNew.convertDayTime(pvOutputRecord2.getDate()));
        assertEquals(Float.parseFloat(values[2]), pvOutputRecord2.dailyCumulatedEnergy, 0.0001);
        assertEquals(Float.parseFloat(values[3]), pvOutputRecord2.totalPowerGenerated, 0.0001);
        assertEquals(Float.parseFloat(values[4]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[5]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[6]), pvOutputRecord2.temperature, 0.0001);
        assertEquals(Float.parseFloat(values[7]), pvOutputRecord2.totalGridVoltage, 0.0001);


    }
}
