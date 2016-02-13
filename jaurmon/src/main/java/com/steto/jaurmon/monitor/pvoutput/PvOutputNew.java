package com.steto.jaurmon.monitor.pvoutput;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.steto.jaurlib.eventbus.EBResponse;
import com.steto.jaurlib.eventbus.EBResponseNOK;
import com.steto.jaurlib.eventbus.EBResponseOK;
import com.steto.jaurmon.monitor.PeriodicInverterTelemetries;
import com.steto.jaurmon.monitor.TelemetriesQueue;
import com.steto.jaurmon.utils.HttpUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Created by stefano on 31/01/16.
 */
public class PvOutputNew {


    private final EventBus theEventBus;
    private final String configfileName;
    private PVOutputParams params;
    protected Logger log = Logger.getLogger(getClass().getSimpleName());
    private  boolean running=false;
    TelemetriesQueue telemetriesQueue = new TelemetriesQueue();
    private int HTTP_REQUEST_TIMEOUT =10000;

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
            SubnodeConfiguration params = iniConfObj.getSection("pvoutput");

            result.url = params.getString("url");
            result.period = params.getFloat("period");
            result.systemId = params.getInt("systemId");
            result.apiKey = params.getString("apiKey");
            result.timeWindowSec = params.getFloat("timeWindowSec");

        } catch (Exception e) {
            String errMsg = "Error reading file: " + fileName + ", " + e.getMessage();
            log.severe(errMsg);
        }

        return result;
    }

    @Subscribe
    public void handle(PeriodicInverterTelemetries telemetries) {
        try{

            if (running) {
                telemetriesQueue.add(telemetries);
                log.info("Stored telemetries: " + telemetries);
            }

        }
        catch (Exception ex)
        {
            log.severe(ex.getMessage());
        }
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
                case "start":
                    request.response = handleStartRequest(request.paramsMap);
                    break;
                case "stop":
                    request.response = handleStopRequest(request.paramsMap);
                    break;
                default:
                    request.response = new EBResponseNOK(-1, "Received invalid command: " + request.opcode());
                    break;
            }
        } catch (Exception ex) {
            request.response = new EBResponseNOK(-1, "Runtime Error: " + ex.getMessage());

        }
    }

    protected EBResponse handleTestRequest(Map paramsMap) {

        EBResponse result;
        try {
            boolean isOK = testPvOutputServer();
            result = isOK ? new EBResponseOK("") : new EBResponseNOK(-1,"Got Wrong Response form Server");
        } catch (Exception ex) {
            result = new EBResponseNOK(1, ex.getMessage());
        }

        return result;

    }

    protected EBResponse handleStartRequest(Map paramsMap) {

        EBResponse result;
        try {
            if (!running)
            {
                start();
            }
            result = new EBResponseOK("") ;
        } catch (Exception ex) {
            result = new EBResponseNOK(1, ex.getMessage());
        }

        return result;

    }

    protected EBResponse handleStopRequest(Map paramsMap) {

        stop();
        return new EBResponseOK("");

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
            newParams.timeWindowSec = (float) paramsMap.get("timeWindowSec");
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
        iniConfObj.setProperty("pvoutput.timeWindowSec", newParams.timeWindowSec);

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


    private void stop() {
        running=false;
        log.info("Main Loop Stopped");
    }

    public void start(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                mainLoop();
            }
        }).start();
    }

    protected void mainLoop() {
        final long PERIODICITY  = (long) (params.period*1000);
        final long WINDOW_MS  = (long)(params.timeWindowSec*1000);
        log.info("Main Loop Started");
        running=true;
        while (running)
        {
            try {

                Long now = new Date().getTime();
                Long since = now-WINDOW_MS;
                telemetriesQueue.removeOlderThan(since);
                PeriodicInverterTelemetries dataPublished = telemetriesQueue.average();
                if (dataPublished!=null)
                {
                    publish2PvOutput(dataPublished);
                }
                else
                {
                  log.fine("No data available for publication");
                }
                Thread.sleep(PERIODICITY);
            } catch (Exception e) {
                log.severe(e.getMessage());
                e.printStackTrace();
            }
        }

    }


    public boolean publish2PvOutput(PeriodicInverterTelemetries telemetries) throws IOException {
        boolean executed = false;
        String requestUrl = generatePvOutputLiveUpdateUrl(telemetries);// put in your url
        int responseCode = -1;

        try {

            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(HTTP_REQUEST_TIMEOUT).build();
            HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

            HttpGet request = new HttpGet(requestUrl);

            org.apache.http.HttpResponse response = httpClient.execute(request);
            log.fine("Response code:" + response.getStatusLine());

            responseCode = response.getStatusLine().getStatusCode();
            log.info("Sending 'GET' request: " + requestUrl);


        } catch (Exception e) {
            log.severe("Error publishing data to PVOutput: " + e.getMessage());
        }
        executed = (responseCode == 200);
        if (!executed) {
            log.info("Errore nell'invio dei dati: \n" + telemetries);
//            savePvOutputRecord(pvOutputRecord);
        } else {
            log.info("Dati inviati a PVOutput: \n" + telemetries);
        }

        return executed;
    }

    private String generatePvOutputLiveUpdateUrl(PeriodicInverterTelemetries tele) {
        Map<String, Object> map = new LinkedHashMap<>();

        Date now = new Date();
        now.setTime(tele.timestamp);
        String date = convertDate(now);
        String time = convertDayTime(now);
        map.put("key", params.apiKey);
        map.put("sid", params.systemId);
        map.put("d", date);
        map.put("t", time);
        map.put("v1", tele.cumulatedEnergy);
        map.put("v2", tele.gridPowerAll);
        map.put("v5", tele.inverterTemp);
        map.put("v6", tele.gridVoltageAll);

        String result = params.url + "/addstatus.jsp?" + HttpUtils.urlEncodeUTF8(map);
        return result;
    }

    public static String convertDate(Date aDate) {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

        return DATE_FORMAT.format(aDate);
    }

    public static String convertDayTime(Date aDate) {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm");
        DATE_FORMAT.setTimeZone(TimeZone.getDefault());

        return DATE_FORMAT.format(aDate);
    }


}
