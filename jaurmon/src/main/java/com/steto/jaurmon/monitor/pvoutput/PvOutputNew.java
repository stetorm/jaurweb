package com.steto.jaurmon.monitor.pvoutput;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.steto.jaurlib.eventbus.EBResponseOK;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.util.logging.Logger;

/**
 * Created by stefano on 31/01/16.
 */
public class PvOutputNew {


    private final EventBus theEventBus;
    private final String configfileName;
    private final PVOutputParams params;
    protected Logger log = Logger.getLogger(getClass().getSimpleName());

    public PvOutputNew(String aFileName, EventBus aEventBus) {
        theEventBus = aEventBus;
        configfileName = aFileName;
        aEventBus.register(this);
        params = loadConfigurationParams(aFileName);
    }

    private PVOutputParams loadConfigurationParams(String fileName) {

        PVOutputParams result = null;

        try {
            result = new PVOutputParams();
            HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration(fileName);
            SubnodeConfiguration inverterParams = iniConfObj.getSection("pvoutput");

            result.url = inverterParams.getString("url");
            result.period = inverterParams.getFloat("period");
            result.systemId = inverterParams.getInt("systemId");
            result.apiKey = inverterParams.getString("apiKey");

        } catch (Exception e) {
            String errMsg = "Error reading file: " + fileName + ", " + e.getMessage();
            log.severe(errMsg);
        }

        return result;
    }

    @Subscribe
    public void handle(EBPvOutputRequest request) {

        request.response = new EBResponseOK(params);
    }


}
