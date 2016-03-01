package com.steto.monitor.core.integration;

import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.cmd.InverterCommandFactory;
import com.steto.jaurlib.eventbus.EBResponseNOK;
import com.steto.jaurlib.eventbus.EBResponseOK;
import com.steto.jaurlib.eventbus.EventBusInverterAdapter;
import com.steto.monitor.AuroraMonitor;
import com.steto.monitor.FakeAuroraWebClient;

import com.steto.monitor.webserver.AuroraWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

/**
 * Created by stefano on 23/12/14.
 */
public class TestCoreMonitorREST {

    File resourcesDirectory = new File("src/test/resources/aurora.cfg");

    String configFile = resourcesDirectory.getAbsolutePath();
    AuroraMonitor auroraMonitor = null;
    AuroraWebServer auroraWebServer = null;
    int auroraServicePort = 8085;
    String pvOutDirPath;
    EventBus theEventBus = new EventBus();
    AuroraDriver auroraDriver = mock(AuroraDriver.class);
    EventBusInverterAdapter eventBusInverterAdapter = new EventBusInverterAdapter(theEventBus,auroraDriver,new InverterCommandFactory());

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    @Before
    public void before() throws Exception {

        pvOutDirPath = tempFolder.newFolder().getAbsolutePath();
        auroraMonitor = new AuroraMonitor(theEventBus, auroraDriver, configFile, pvOutDirPath);
        Thread.sleep(500);
        auroraWebServer = new AuroraWebServer(auroraServicePort, "./html", theEventBus);
        new Thread(auroraWebServer).start();
        Thread.sleep(200);

    }

    @After
    public void after() throws Exception {

        tempFolder.delete();
        auroraWebServer.stop();
        auroraMonitor.stop();
        Thread.sleep(200);

    }


    @Test
    public void shouldSendStatus() throws Exception {

        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);

        // Exercise
        String jsonResult = fakeAuroraWebClient.sendStatusRequest();


        // Verify
        System.out.println(jsonResult);
        EBResponseOK ebResponseOK = new Gson().fromJson(jsonResult, EBResponseOK.class);


        String invStatus  = (String) ebResponseOK.data;


        Gson gson = new Gson();
        Map result = gson.fromJson(jsonResult, Map.class);
        assertEquals(auroraMonitor.isInverterOnline() ? "online" : "offline", invStatus);


    }


    @Test
    public void shouldApplyInverterSettings() throws Exception {

        String serialPort = "/TEST/COM";
        Integer baudRate = 1122;
        Integer inverterAddress = 5;
        Map<String, Object> mapConfig = new HashMap<>();
        mapConfig.put("serialPort", serialPort);
        mapConfig.put("serialPortBaudRate", baudRate);
        mapConfig.put("inverterAddress", inverterAddress);


        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);
        // exercise
        String jsonResult = fakeAuroraWebClient.sendSaveSettingsRequest(mapConfig);


        //verify
        System.out.println(jsonResult);
        EBResponseOK ebResponseOK = new Gson().fromJson(jsonResult, EBResponseOK.class);

        // verify json answer, gli interi vengono considerati come float
        Map responseMap  = new Gson().fromJson(jsonResult, Map.class);
        Map dataMap = (Map) responseMap.get("data");
        assertEquals(serialPort,  dataMap.get("serialPort"));
        assertEquals(String.valueOf(baudRate.floatValue()), dataMap.get("serialPortBaudRate").toString().trim());
        assertEquals(String.valueOf(inverterAddress.floatValue()),  dataMap.get("inverterAddress").toString().trim());

        // verify effects on auroraMonitorObject
        assertEquals(serialPort, auroraMonitor.getSerialPortName());
        assertEquals(baudRate.intValue(), auroraMonitor.getSerialPortBaudRate());
        assertEquals(inverterAddress.intValue(), auroraMonitor.getInverterAddress());

    }


    @Test
    public void shouldLoadInverterSettings() throws Exception {

        String serialPort = "/TEST/COM";
        Integer baudRate = 1122;
        Integer inverterAddress = 5;

        auroraMonitor.setInverterAddress(inverterAddress);
        auroraMonitor.setSerialPortBaudRate(baudRate);
        auroraMonitor.setSerialPortName(serialPort);

        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);
        // exercise
        String jsonResult = fakeAuroraWebClient.sendLoadInvSettingsRequest();


        //verify
        System.out.println(jsonResult);
        EBResponseOK ebResponseOK = new Gson().fromJson(jsonResult, EBResponseOK.class);

        // verify json answer, gli interi vengono considerati come float
        Map responseMap  = new Gson().fromJson(jsonResult, Map.class);
        Map dataMap = (Map) responseMap.get("data");
        assertEquals(serialPort,  dataMap.get("serialPort"));
        assertEquals(String.valueOf(baudRate.floatValue()), dataMap.get("serialPortBaudRate").toString().trim());
        assertEquals(String.valueOf(inverterAddress.floatValue()),  dataMap.get("inverterAddress").toString().trim());

    }

    @Test
    public void shouldReplyToBadCommand() throws Exception {

        // Setup Command
        int inverterAddress = 5;
        String opcde = "badcommand";
        String subcode = "wrong";


        // Setup FakeClient
        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendInverterCommand(String.valueOf(inverterAddress), opcde, subcode);

        //verify
        System.out.println(jsonResult);

        EBResponseNOK result = new Gson().fromJson(jsonResult, EBResponseNOK.class);

        assertEquals(1,result.error.code.intValue());
        assertTrue(result.error.message.length() > 0);


    }


}