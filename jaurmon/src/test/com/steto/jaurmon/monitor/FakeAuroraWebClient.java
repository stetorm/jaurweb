package com.steto.jaurmon.monitor;

import com.steto.jaurmon.utils.HttpUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by stefano on 23/12/14.
 */
public class FakeAuroraWebClient {
    private final String serverAddress;

    protected Logger log = Logger.getLogger(getClass().getSimpleName());

    public FakeAuroraWebClient(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String sendLoadInvSettingsRequest() throws IOException {
        String requestUrl = serverAddress + "/cmd/loadInvSettings";
        String result = "";

        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, "UTF-8");
            log.info("Sending 'GET' request: " + requestUrl);
            log.info("Response Code: " + response.getStatusLine().getStatusCode() + " " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public String sendSaveConfigurationRequest(Map mapConfig) {
        String requestUrl = serverAddress + "/cmd/saveCfg/?" + HttpUtils.urlEncodeUTF8(mapConfig);
        String result = "";
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, "UTF-8");
            log.info("Sending 'GET' request: " + requestUrl);
            log.info("Response Code: " + response.getStatusLine().getStatusCode() + " " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public String sendSaveSettingsRequest(Map mapConfig) {
        String requestUrl = serverAddress + "/cmd/saveInvSettings/?" + HttpUtils.urlEncodeUTF8(mapConfig);
        String result = "";
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, "UTF-8");
            log.info("Sending 'GET' request: " + requestUrl);
            log.info("Response Code: " + response.getStatusLine().getStatusCode() + " " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }


    public String sendTestPVOutputRequest(Map mapConfig) {
        String requestUrl = serverAddress + "/cmd/pvOutputTest/?" + HttpUtils.urlEncodeUTF8(mapConfig);
        String result = "";
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, "UTF-8");
            log.info("Sending 'GET' request: " + requestUrl);
            log.info("Response Code: " + response.getStatusLine().getStatusCode() + " " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public String sendStatusRequest() {
        String requestUrl = serverAddress + "/cmd/status";
        String result = "";
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, "UTF-8");
            log.info("Sending 'GET' request: " + requestUrl);
            log.info("Response Code: " + response.getStatusLine().getStatusCode() + " " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public String sendInverterCommand(String address, String opcode, String subcode) {

        Map mapConfig = new HashMap<String,String>();
        mapConfig.put("opcode",opcode);
        mapConfig.put("subcode",subcode);
        mapConfig.put("address",address);
        String queryUrl = HttpUtils.urlEncodeUTF8(mapConfig) ;

        String requestUrl = serverAddress + "/cmd/inv/" + "?" + queryUrl;
        String result = "";
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, "UTF-8");
            log.info("Sending 'GET' request: " + requestUrl);
            log.info("Response Code: " + response.getStatusLine().getStatusCode() + " " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
