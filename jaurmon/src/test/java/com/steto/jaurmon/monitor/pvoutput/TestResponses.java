package com.steto.jaurmon.monitor.pvoutput;

import com.google.common.eventbus.EventBus;
import com.steto.jaurlib.eventbus.EBResponse;
import com.steto.jaurlib.eventbus.EBResponseOK;
import com.steto.jaurmon.monitor.FakePVOutputServer;
import jssc.SerialPortException;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.steto.jaurmon.monitor.RandomObjectGenerator.getA_PvOutputParams;
import static com.steto.jaurmon.monitor.TestUtility.createPvoutputConfigFile;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by stefano on 31/01/16.
 */
public class TestResponses {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    @Test
    public void shouldReadAndSaveConfigurationData() throws IOException, SerialPortException, ConfigurationException {


        //Setup
        String tempPvOutputFile = tempFolder.newFile().getAbsolutePath();
        PVOutputParams pvOutputParamsInitials = getA_PvOutputParams();
        createPvoutputConfigFile(tempPvOutputFile, pvOutputParamsInitials);

        PVOutputParams pvOutputParams2Save = getA_PvOutputParams();

        EventBus firstEventBus = new EventBus();
        PvOutputNew PvOutputStore = new PvOutputNew(tempPvOutputFile, firstEventBus);
        Map requestSaveMap = new HashMap<>();
        requestSaveMap.put("opcode", "save");
        requestSaveMap.put("url", pvOutputParams2Save.url);
        requestSaveMap.put("apiKey", pvOutputParams2Save.apiKey);
        requestSaveMap.put("systemId", pvOutputParams2Save.systemId);
        requestSaveMap.put("period", pvOutputParams2Save.period);
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


    }

    @Test
    public void should() throws IOException, ConfigurationException, InterruptedException {

        Integer pvOutputPort = 8082;
        String pvOutServiceUrl = "/pvoutputservice";
        String pvOutUrl = "http://localhost:" + pvOutputPort + pvOutServiceUrl;
        String pvOutKey = "a908fds653";
        Integer systemId = 1223;
        Integer period = 5;


        //Setup
        String tempPvOutputFile = tempFolder.newFile().getAbsolutePath();
        PVOutputParams pvOutputParams = getA_PvOutputParams();
        pvOutputParams.url = pvOutUrl;
        pvOutputParams.apiKey = pvOutKey;
        pvOutputParams.systemId = systemId;
        pvOutputParams.period = period;

        createPvoutputConfigFile(tempPvOutputFile, pvOutputParams);

        EventBus eventBus = new EventBus();
        PvOutputNew pvOutput = new PvOutputNew(tempPvOutputFile, eventBus);


        FakePVOutputServer fakePVOutputServer = new FakePVOutputServer(pvOutputPort, pvOutKey, systemId, pvOutServiceUrl);
        new Thread(fakePVOutputServer).start();
        Thread.sleep(1000);

        Map requestTest = new HashMap<>();
        requestTest.put("opcode", "test");
        EBPvOutputRequest ebPvOutputRequest = new EBPvOutputRequest(requestTest);

        //Exercise
        eventBus.post(ebPvOutputRequest);  //save configuration data

        //verify
        EBResponse ebResponse= ebPvOutputRequest.response;
        assertTrue(ebResponse instanceof EBResponseOK);

    }


}

