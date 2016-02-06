package com.steto.jaurmon.monitor.pvoutput;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.steto.jaurlib.eventbus.EBResponse;
import com.steto.jaurlib.eventbus.EBResponseNOK;
import com.steto.jaurlib.eventbus.EBResponseOK;
import com.steto.jaurmon.utils.HttpUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by stefano on 31/01/16.
 */
public class PvOutputNew {


    private final EventBus theEventBus;
    private final String configfileName;
    private PVOutputParams params;
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

        try {
            switch (request.opcode()) {
                case "read":
                    request.response = handleReadRequest(request.paramsMap);
                    break;
                case "save":
                    request.response = handleSaveRequest(request.paramsMap);
                    break;
                case "test":
                    request.response = handleTestRequest(request.paramsMap);
                    break;
                default:
                    request.response = new EBResponseNOK(-1, "Received invalid command: " + request.opcode());
                    break;
            }
        } catch (Exception ex) {
            request.response = new EBResponseNOK(-1, "Runtime Error: " + ex.getMessage());

        }
    }

    private EBResponse handleTestRequest(Map paramsMap) {

        EBResponse result;
        try {
            boolean isOK = testPvOutputServer();
            result = isOK ? new EBResponseOK("") : new EBResponseNOK(-1,"Got Wrong Response form Server");
        } catch (Exception ex) {
            result = new EBResponseNOK(1, ex.getMessage());
        }

        return result;

    }

    protected EBResponse handleReadRequest(Map paramsMap) {
        return new EBResponseOK(params);
    }

    protected EBResponse handleSaveRequest(Map paramsMap) {
        EBResponse result = new EBResponseNOK();
        try {
            PVOutputParams newParams = new PVOutputParams();
            newParams.apiKey = (String) paramsMap.get("apiKey");
            newParams.url = (String) paramsMap.get("url");
            newParams.systemId = (int) paramsMap.get("systemId");
            newParams.period = (float) paramsMap.get("period");
            saveParams(newParams);
            params = newParams;
            result = new EBResponseOK("");
        } catch (Exception ex) {
            result = new EBResponseNOK(1, "Error saving data: " + ex.getMessage());
        }

        return result;

    }

    private void saveParams(PVOutputParams newParams) throws ConfigurationException {
        HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration(new File(configfileName));
        iniConfObj.setProperty("pvoutput.systemId", newParams.systemId);
        iniConfObj.setProperty("pvoutput.apiKey", newParams.apiKey);
        iniConfObj.setProperty("pvoutput.period", newParams.period);
        iniConfObj.setProperty("pvoutput.url", newParams.url);

        iniConfObj.save();

    }

    public boolean testPvOutputServer() throws IOException {

        int responseCode = -1;
        String requestUrl = generatePvOutputTestUrl();

        URL obj = new URL(requestUrl);
        HttpURLConnection con;
        con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        responseCode = con.getResponseCode();
        log.info("Sending 'GET' request: " + requestUrl);
        log.info("Response Code: " + responseCode + " " + con.getResponseMessage());

        return responseCode == 200;

    }

    private String generatePvOutputTestUrl() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("key", params.apiKey);
        map.put("sid", params.systemId);

        String result = params.url + "/getinsolation.jsp?" + HttpUtils.urlEncodeUTF8(map);
        return result;
    }


}
