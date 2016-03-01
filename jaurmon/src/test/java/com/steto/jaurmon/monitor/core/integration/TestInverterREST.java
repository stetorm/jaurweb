package com.steto.jaurmon.monitor.core.integration;

import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.cmd.InverterCommandFactory;
import com.steto.jaurlib.eventbus.EBResponseNOK;
import com.steto.jaurlib.eventbus.EBResponseOK;
import com.steto.jaurlib.eventbus.EventBusInverterAdapter;
import com.steto.jaurlib.request.AuroraCumEnergyEnum;
import com.steto.jaurlib.request.AuroraDspRequestEnum;
import com.steto.jaurlib.response.*;
import com.steto.jaurmon.monitor.AuroraMonitor;
import com.steto.jaurmon.monitor.FakeAuroraWebClient;
import com.steto.jaurmon.monitor.webserver.AuroraWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by stefano on 23/12/14.
 */
public class TestInverterREST {

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


    @Test
    public void shouldExecuteInverterCommandEnergyCumulated() throws Exception {

        // Setup Command
        int inverterAddress = 5;
        String opcde = "cumEnergy";
        String subcode = "daily";

        // Setup
        final float expectedCumulateEnergyValue = (float) 1001.0;

        AResp_CumulatedEnergy expectedCumulateEnergyResponse = new AResp_CumulatedEnergy();
        expectedCumulateEnergyResponse.setLongParam((long) expectedCumulateEnergyValue);
        when(auroraDriver.acquireCumulatedEnergy(eq(inverterAddress), eq(AuroraCumEnergyEnum.DAILY))).thenReturn(expectedCumulateEnergyResponse);

        // Setup FakeClient
        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendInverterCommand(String.valueOf(inverterAddress), opcde, subcode);

        //verify
        System.out.println(jsonResult);

        EBResponseOK result = new Gson().fromJson(jsonResult, EBResponseOK.class);
        float energyReadout = Float.parseFloat((String) result.data);

        assertEquals(expectedCumulateEnergyValue, energyReadout, 0.00001);


    }

    @Test
    public void shouldExecuteInverterCommandDspData() throws Exception {

        // Setup Command
        int inverterAddress = 5;
        String opcde = "dspData";
        String subcode = "gridVoltageAll";

        // Setup
        final float expectedVoltageAll = (float) 253.2;

        AResp_DspData expectedResponse = new AResp_DspData();
        expectedResponse.setFloatParam(expectedVoltageAll);
        when(auroraDriver.acquireDspValue(eq(inverterAddress), eq(AuroraDspRequestEnum.GRID_VOLTAGE_ALL))).thenReturn(expectedResponse);

        // Setup FakeClient
        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendInverterCommand(String.valueOf(inverterAddress), opcde, subcode);

        //verify
        System.out.println(jsonResult);

        EBResponseOK result = new Gson().fromJson(jsonResult, EBResponseOK.class);
        float voltageReadout = Float.parseFloat((String) result.data);

        assertEquals(expectedVoltageAll, voltageReadout, 0.00001);


    }

    @Test
    public void shouldExecuteInverterCommandProductNumber() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "productNumber";
        String subcode = "";


        String productNumber = "012345";

        AResp_ProductNumber expectedProductNumberResponse = new AResp_ProductNumber();
        expectedProductNumberResponse.set(productNumber);
        when(auroraDriver.acquireProductNumber(eq(inverterAddress))).thenReturn(expectedProductNumberResponse);

        // Setup FakeClient
        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendInverterCommand(String.valueOf(inverterAddress), opcde, subcode);

        //verify
        System.out.println(jsonResult);

        EBResponseOK result = new Gson().fromJson(jsonResult, EBResponseOK.class);

        assertEquals(productNumber, result.data);


    }

    @Test
    public void shouldExecuteInverterCommandSerialNumber() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "serialNumber";
        String subcode = "";


        String serialNumber = "654321";

        AResp_SerialNumber expectedSerialNumberResponse = new AResp_SerialNumber();
        expectedSerialNumberResponse.set(serialNumber);
        when(auroraDriver.acquireSerialNumber(eq(inverterAddress))).thenReturn(expectedSerialNumberResponse);

        // Setup FakeClient
        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendInverterCommand(String.valueOf(inverterAddress), opcde, subcode);

        //verify
        System.out.println(jsonResult);

        EBResponseOK result = new Gson().fromJson(jsonResult, EBResponseOK.class);

        assertEquals(serialNumber, result.data);

    }

    @Test
    public void shouldExecuteInverterVersionNumber() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "versionNumber";
        String subcode = "";


        int versionNumber = 0xAABBCCDD;

        AResp_VersionId expectedVersionId = new AResp_VersionId();
        expectedVersionId.setLongParam(versionNumber);
        String versionDescription = expectedVersionId.getValue();
        when(auroraDriver.acquireVersionId(eq(inverterAddress))).thenReturn(expectedVersionId);

        // Setup FakeClient
        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendInverterCommand(String.valueOf(inverterAddress), opcde, subcode);

        //verify
        System.out.println(jsonResult);

        EBResponseOK result = new Gson().fromJson(jsonResult, EBResponseOK.class);

        assertEquals(versionDescription, result.data);

    }

    @Test
    public void shouldExecuteInverterFirmwareVersion() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "firmwareNumber";
        String subcode = "";

        AResp_FwVersion expectedFirmwareVersionResponse = new AResp_FwVersion();
        expectedFirmwareVersionResponse.set("1234");
        String firmareVersion = expectedFirmwareVersionResponse.get();
        when(auroraDriver.acquireFirmwareVersion(eq(inverterAddress))).thenReturn(expectedFirmwareVersionResponse);

        // Setup FakeClient
        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendInverterCommand(String.valueOf(inverterAddress), opcde, subcode);

        //verify
        System.out.println(jsonResult);

        EBResponseOK result = new Gson().fromJson(jsonResult, EBResponseOK.class);

        assertEquals(firmareVersion, result.data);

    }

    @Test
    public void shouldExecuteInverterManufacturingDate() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "manufacturingDate";
        String subcode = "";

        AResp_MFGdate expectedResponse = new AResp_MFGdate();
        expectedResponse.setDate(new Date());
        String maufactoringDate = expectedResponse.getValue();
        when(auroraDriver.acquireMFGdate(eq(inverterAddress))).thenReturn(expectedResponse);

        // Setup FakeClient
        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendInverterCommand(String.valueOf(inverterAddress), opcde, subcode);

        //verify
        System.out.println(jsonResult);

        EBResponseOK result = new Gson().fromJson(jsonResult, EBResponseOK.class);

        assertEquals(maufactoringDate, result.data);

    }

    @Test
    public void shouldExecuteSystemConfig() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "sysConfig";
        String subcode = "";

        AResp_SysConfig expectedResponse = new AResp_SysConfig();
        expectedResponse.setConfigCode(125);
        int systemConfig = expectedResponse.getConfigCode();
        when(auroraDriver.acquireSystemConfig(eq(inverterAddress))).thenReturn(expectedResponse);

        // Setup FakeClient
        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendInverterCommand(String.valueOf(inverterAddress), opcde, subcode);

        //verify
        System.out.println(jsonResult);

        EBResponseOK result = new Gson().fromJson(jsonResult, EBResponseOK.class);

        assertEquals(systemConfig, Integer.parseInt((String) result.data));

    }

    @Test
    public void shouldExecuteInverterTimeCounter() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "timeCounter";
        String subcode = "";

        AResp_TimeCounter expectedResponse = new AResp_TimeCounter();
        expectedResponse.set(new Date().getTime()/1000);
        String timeCounter = expectedResponse.get().toString();
        when(auroraDriver.acquireTimeCounter(eq(inverterAddress))).thenReturn(expectedResponse);

        // Setup FakeClient
        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendInverterCommand(String.valueOf(inverterAddress), opcde, subcode);

        //verify
        System.out.println(jsonResult);

        EBResponseOK result = new Gson().fromJson(jsonResult, EBResponseOK.class);

        assertEquals(timeCounter, result.data);

    }

    @Test
    public void shouldExecuteInverterActualTime() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "actualTime";
        String subcode = "";

        AResp_ActualTime expectedResponse = new AResp_ActualTime();
        expectedResponse.set(new Date().getTime()/1000);
        String timeCounter = expectedResponse.get().toString();
        when(auroraDriver.acquireActualTime(eq(inverterAddress))).thenReturn(expectedResponse);

        // Setup FakeClient
        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendInverterCommand(String.valueOf(inverterAddress), opcde, subcode);

        //verify
        System.out.println(jsonResult);

        EBResponseOK result = new Gson().fromJson(jsonResult, EBResponseOK.class);

        assertEquals(timeCounter, result.data);

    }

}