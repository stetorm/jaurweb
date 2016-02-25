package com.steto.jaurmon.monitor.pvoutput.integration;

import com.google.common.eventbus.EventBus;
import com.steto.jaurlib.eventbus.EBResponse;
import com.steto.jaurlib.eventbus.EBResponseOK;
import com.steto.jaurmon.monitor.FakePVOutputServer;
import com.steto.jaurmon.monitor.PeriodicInverterTelemetries;
import com.steto.jaurmon.monitor.pvoutput.EBPvOutputRequest;
import com.steto.jaurmon.monitor.pvoutput.PVOutputParams;
import com.steto.jaurmon.monitor.pvoutput.PvOutputNew;
import com.steto.jaurmon.utils.HttpUtils;
import jssc.SerialPortException;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.steto.jaurmon.monitor.RandomObjectGenerator.getA_PvOutputParams;
import static com.steto.jaurmon.monitor.RandomObjectGenerator.getInt;
import static com.steto.jaurmon.monitor.TestUtility.createPvoutputConfigFile;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.*;

/**
 * Created by stefano on 31/01/16.
 */
public class TestEventBusInterface {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    String tempPvOutputFile;
    FakePVOutputServer fakePVOutputServer;
    Future<?> fakeServerExecutorFuture;
    private PVOutputParams pvOutputParams;


    @Before
    public void setupFakeServerAndConfigFile() throws ConfigurationException, IOException, InterruptedException {

        tempPvOutputFile = tempFolder.newFile().getAbsolutePath();

        Integer pvOutputPort = getInt(65532);
        String pvOutServiceUrl = "/pvoutputservice";
        String pvOutUrl = "http://localhost:" + pvOutputPort + pvOutServiceUrl;
        String pvOutKey = "a908fds653";
        Integer systemId = 1223;
        double period = 0.5;


        //Setup

        pvOutputParams = getA_PvOutputParams();
        pvOutputParams.url = pvOutUrl;
        pvOutputParams.apiKey = pvOutKey;
        pvOutputParams.systemId = systemId;
        pvOutputParams.period = (float) period;
        pvOutputParams.timeWindowSec = (float) 1000;

        createPvoutputConfigFile(tempPvOutputFile, pvOutputParams);

        fakePVOutputServer = new FakePVOutputServer(pvOutputPort, pvOutKey, systemId, pvOutServiceUrl);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        fakeServerExecutorFuture = executor.submit(fakePVOutputServer);
        Thread.sleep(2000);


    }

    @After
    public void after() throws Exception {
        fakePVOutputServer.stop();
        if (fakeServerExecutorFuture != null) {
            fakeServerExecutorFuture.cancel(true);
        }

    }

    @Test
    public void shouldReadAndSaveConfigurationData() throws IOException, SerialPortException, ConfigurationException {


        //Setup
        PVOutputParams pvOutputParamsInitials = getA_PvOutputParams();
        createPvoutputConfigFile(tempPvOutputFile, pvOutputParamsInitials);

        PVOutputParams pvOutputParams2Save = getA_PvOutputParams();

        EventBus firstEventBus = new EventBus();
        PvOutputNew PvOutputStore = new PvOutputNew(tempPvOutputFile, firstEventBus);
        Map requestSaveMap = new HashMap<>();
        requestSaveMap.put("opcode", "save");
        requestSaveMap.put("url", pvOutputParams2Save.url);
        requestSaveMap.put("apiKey", pvOutputParams2Save.apiKey);
        requestSaveMap.put("systemId", ""+pvOutputParams2Save.systemId);
        requestSaveMap.put("period", ""+pvOutputParams2Save.period);
        requestSaveMap.put("timeWindowSec", ""+pvOutputParams2Save.timeWindowSec);
        EBPvOutputRequest ebPvOutputRequestSave = new EBPvOutputRequest(requestSaveMap);

        Map requestReadMap = new HashMap<>();
        requestReadMap.put("opcode", "read");
        EBPvOutputRequest ebPvOutputRequestRead = new EBPvOutputRequest(requestReadMap);

        EventBus secondEventBus = new EventBus();


        //Exercise
        firstEventBus.post(ebPvOutputRequestSave);  //save configuration data
        PvOutputNew PvOutputRead = new PvOutputNew(tempPvOutputFile, secondEventBus);
        secondEventBus.post(ebPvOutputRequestRead);  //read configuration data

        //Verify
        EBResponseOK ebResponseOKSave = (EBResponseOK) ebPvOutputRequestSave.response;

        EBResponseOK ebResponseOKRead = (EBResponseOK) ebPvOutputRequestRead.response;
        PVOutputParams result = (PVOutputParams) ebResponseOKRead.data;

        assertEquals(pvOutputParams2Save.url, result.url);
        assertEquals(pvOutputParams2Save.apiKey, result.apiKey);
        assertEquals(pvOutputParams2Save.systemId, result.systemId);
        assertEquals(pvOutputParams2Save.period, result.period, 0.0001);
        assertEquals(pvOutputParams2Save.timeWindowSec, result.timeWindowSec, 0.0001);


    }


    @Test
    public void shouldPerformTest() throws IOException, ConfigurationException, InterruptedException {

        // Setup
        EventBus eventBus = new EventBus();
        PvOutputNew pvOutput = new PvOutputNew(tempPvOutputFile, eventBus);

        Map requestTest = new HashMap<>();
        requestTest.put("opcode", "test");
        EBPvOutputRequest ebPvOutputRequest = new EBPvOutputRequest(requestTest);

        //Exercise
        eventBus.post(ebPvOutputRequest);  //save configuration data

        //verify
        EBResponse ebResponse = ebPvOutputRequest.response;
        assertTrue(ebResponse instanceof EBResponseOK);

    }

    @Test
    public void shouldStartPublication() throws IOException, ConfigurationException, InterruptedException {

        //Setup

        EventBus eventBus = new EventBus();
        PvOutputNew pvOutput = new PvOutputNew(tempPvOutputFile, eventBus);

        Map requestTest = new HashMap<>();
        requestTest.put("opcode", "start");
        EBPvOutputRequest ebPvOutputRequest = new EBPvOutputRequest(requestTest);
        PeriodicInverterTelemetries inverterTelemetries1 = new PeriodicInverterTelemetries();

        //Exercise
        eventBus.post(ebPvOutputRequest);
        Thread.sleep(500);
        eventBus.post(inverterTelemetries1);
        Thread.sleep(1000);


        //verify
        EBResponse ebResponse = ebPvOutputRequest.response;
        assertTrue(ebResponse instanceof EBResponseOK);
        assertNotNull(fakePVOutputServer.getLastRequest());

    }

    @Test
    public void shouldStopPublication() throws IOException, ConfigurationException, InterruptedException {

        //Setup

        EventBus eventBus = new EventBus();
        PvOutputNew pvOutput = new PvOutputNew(tempPvOutputFile, eventBus);

        Map requestTest = new HashMap<>();
        requestTest.put("opcode", "stop");
        EBPvOutputRequest ebPvOutputRequest = new EBPvOutputRequest(requestTest);
        PeriodicInverterTelemetries inverterTelemetries1 = new PeriodicInverterTelemetries();

        //Exercise
        pvOutput.start();
        Thread.sleep(300);
        eventBus.post(ebPvOutputRequest);
        eventBus.post(inverterTelemetries1);


        //verify
        EBResponse ebResponse = ebPvOutputRequest.response;
        assertTrue(ebResponse instanceof EBResponseOK);
        assertNull(fakePVOutputServer.getLastRequest());

    }


    @Test
    public void shouldLoadStatusOff() throws Exception {

        //Setup

        EventBus eventBus = new EventBus();
        PvOutputNew pvOutput = new PvOutputNew(tempPvOutputFile, eventBus);

        Map requestStatus = new HashMap<>();
        requestStatus.put("opcode", "status");
        EBPvOutputRequest ebPvOutputRequest = new EBPvOutputRequest(requestStatus);
        eventBus.post(ebPvOutputRequest);


        //verify
        EBResponse ebResponse = ebPvOutputRequest.response;
        assertTrue(ebResponse instanceof EBResponseOK);
        EBResponseOK  ebResponseOk = (EBResponseOK) ebResponse;
        assertEquals(ebResponseOk.data, "off");

    }

    @Test
    public void shouldLoadStatusOn() throws Exception {

        //Setup

        EventBus eventBus = new EventBus();
        PvOutputNew pvOutput = new PvOutputNew(tempPvOutputFile, eventBus);
        pvOutput.start();
        Thread.sleep(300);

        Map requestStatus = new HashMap<>();
        requestStatus.put("opcode", "status");
        EBPvOutputRequest ebPvOutputRequest = new EBPvOutputRequest(requestStatus);
        eventBus.post(ebPvOutputRequest);


        //verify
        EBResponse ebResponse = ebPvOutputRequest.response;
        assertTrue(ebResponse instanceof EBResponseOK);
        EBResponseOK  ebResponseOk = (EBResponseOK) ebResponse;
        assertEquals(ebResponseOk.data,"on");

    }

    @Test
    public void shouldPublishCorrectAveragedData() throws InterruptedException, ConfigurationException, IOException {

        pvOutputParams.period = 1;
        pvOutputParams.timeWindowSec = 4;

        createPvoutputConfigFile(tempPvOutputFile, pvOutputParams);

        EventBus eventBus = new EventBus();
        PvOutputNew pvOutput = new PvOutputNew(tempPvOutputFile, eventBus);


        //Exercise
        PeriodicInverterTelemetries inverterTelemetries1 = new PeriodicInverterTelemetries();
        PeriodicInverterTelemetries inverterTelemetries2 = new PeriodicInverterTelemetries();
        PeriodicInverterTelemetries inverterTelemetries3 = new PeriodicInverterTelemetries();

        long now = new Date().getTime();
        inverterTelemetries1.setTimestamp(now - 2000);
        inverterTelemetries1.cumulatedEnergy = 3;
        inverterTelemetries1.gridPowerAll = 3;
        inverterTelemetries1.gridVoltageAll = 3;
        inverterTelemetries1.inverterTemp = 3;

        inverterTelemetries2.setTimestamp(now - 1000);
        inverterTelemetries2.cumulatedEnergy = 5;
        inverterTelemetries2.gridPowerAll = 5;
        inverterTelemetries2.gridVoltageAll = 5;
        inverterTelemetries2.inverterTemp = 5;

        inverterTelemetries3.setTimestamp(now);
        inverterTelemetries3.cumulatedEnergy = 7;
        inverterTelemetries3.gridPowerAll = 7;
        inverterTelemetries3.gridVoltageAll = 7;
        inverterTelemetries3.inverterTemp = 7;

        //Exercise
        pvOutput.start();
        Thread.sleep(500);
        eventBus.post(inverterTelemetries3);
        eventBus.post(inverterTelemetries2);
        eventBus.post(inverterTelemetries1);

        //verify
        Thread.sleep((long) ((pvOutputParams.timeWindowSec + 1) * 1000));
        String publishedData1 = fakePVOutputServer.pollLastRequest();
        String publishedData2 = fakePVOutputServer.pollLastRequest();
        String publishedData3 = fakePVOutputServer.pollLastRequest();
        String publishedData4 = fakePVOutputServer.pollLastRequest();


        PeriodicInverterTelemetries expectedTelemetries1 = new PeriodicInverterTelemetries();
        expectedTelemetries1.setTimestamp(now);
        expectedTelemetries1.cumulatedEnergy = 7;
        expectedTelemetries1.gridPowerAll = (3 + 5 + 7) / 3;
        expectedTelemetries1.gridVoltageAll = (3 + 5 + 7) / 3;
        expectedTelemetries1.inverterTemp = (3 + 5 + 7) / 3;

        PeriodicInverterTelemetries expectedTelemetries2 = new PeriodicInverterTelemetries();
        expectedTelemetries2.setTimestamp(now);
        expectedTelemetries2.cumulatedEnergy = 7;
        expectedTelemetries2.gridPowerAll = (5 + 7) / 2;
        expectedTelemetries2.gridVoltageAll = (5 + 7) / 2;
        expectedTelemetries2.inverterTemp = (5 + 7) / 2;

        PeriodicInverterTelemetries expectedTelemetries3 = new PeriodicInverterTelemetries();
        expectedTelemetries3.setTimestamp(now);
        expectedTelemetries3.cumulatedEnergy = 7;
        expectedTelemetries3.gridPowerAll = 7;
        expectedTelemetries3.gridVoltageAll = 7;
        expectedTelemetries3.inverterTemp = 7;

        assertTelemetriesEquals(expectedTelemetries1, publishedData1);
        assertTelemetriesEquals(expectedTelemetries2, publishedData2);
        assertTelemetriesEquals(expectedTelemetries3, publishedData3);
        junit.framework.Assert.assertNull(publishedData4);


    }

    public void assertTelemetriesEquals(PeriodicInverterTelemetries telemetries, String httpString) {
        Map<String, String> queryMap = HttpUtils.getQueryMap(httpString);

        Double dailyEnergy = Double.parseDouble(queryMap.get("v1"));
        Double power = Double.parseDouble(queryMap.get("v2"));
        Double temperature = Double.parseDouble(queryMap.get("v5"));
        Double volt = Double.parseDouble(queryMap.get("v6"));
        String date = queryMap.get("d");
        String time = queryMap.get("t");

        Assert.assertEquals(telemetries.cumulatedEnergy, dailyEnergy, 0.000001);
        Assert.assertEquals(telemetries.gridPowerAll, power, 0.000001);
        Assert.assertEquals(telemetries.inverterTemp, temperature, 0.000001);
        Assert.assertEquals(telemetries.gridVoltageAll, volt, 0.000001);
        assertNotNull(date);
        assertNotNull(time);
    }


}

