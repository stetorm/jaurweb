package com.steto.jaurmon.monitor.pvoutput;

import com.google.common.eventbus.EventBus;
import com.steto.jaurlib.eventbus.EBResponseOK;
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

/**
 * Created by stefano on 31/01/16.
 */
public class TestSomething {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    @Test
    public void shouldAnswer2ConfigurationRequest() throws IOException, SerialPortException, ConfigurationException {

        //Setup
        String tempPvOutputFile = tempFolder.newFile().getAbsolutePath();
        PVOutputParams pvOutputParams = getA_PvOutputParams();
        createPvoutputConfigFile(tempPvOutputFile, pvOutputParams);

        EventBus theEventBus = new EventBus();
        PvOutputNew pvOutput = new PvOutputNew(tempPvOutputFile, theEventBus);
        Map requestMap = new HashMap<>();
        requestMap.put("code", "read");
        EBPvOutputRequest ebPvOutputRequest = new EBPvOutputRequest(requestMap);
        //Exercise
        theEventBus.post(ebPvOutputRequest);

        //Verify
        EBResponseOK ebResponseOK = (EBResponseOK) ebPvOutputRequest.response;
        PVOutputParams result = (PVOutputParams) ebResponseOK.data;

        assertEquals(pvOutputParams.url, result.url);


    }

}
