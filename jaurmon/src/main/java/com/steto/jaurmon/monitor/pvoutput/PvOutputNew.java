package com.steto.jaurmon.monitor.pvoutput;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.steto.jaurlib.eventbus.EBResponse;
import com.steto.jaurlib.eventbus.EBResponseNOK;
import com.steto.jaurlib.eventbus.EBResponseOK;
import com.steto.jaurmon.monitor.MonitorMsgInverterStatus;
import com.steto.jaurmon.monitor.PeriodicInverterTelemetries;
import com.steto.jaurmon.monitor.TelemetriesQueue;
import com.steto.jaurmon.utils.FormatStringUtils;
import com.steto.jaurmon.utils.HttpUtils;
import com.steto.jaurmon.utils.MyUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by stefano on 31/01/16.
 */
public class PvOutputNew {


    private final EventBus theEventBus;
    private final String configfileName;
    private String pvOutputDataDirectoryPath = "./csv";
    private PVOutputParams params;
    protected Logger log = Logger.getLogger(getClass().getSimpleName());
    private boolean running = false;
    TelemetriesQueue telemetriesQueue = new TelemetriesQueue();
    private int HTTP_REQUEST_TIMEOUT = 10000;
    private boolean isInverterOnline=true;

    public PvOutputNew(String aFileName, EventBus aEventBus) {
        theEventBus = aEventBus;
        configfileName = aFileName;
        aEventBus.register(this);
        params = loadConfigurationParams(aFileName);
        createPvOutputLogDirectory();

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

    protected void createPvOutputLogDirectory() {
        File dataLogDirectory = new File(pvOutputDataDirectoryPath);
        if (!dataLogDirectory.exists()) {
            if (!dataLogDirectory.mkdirs()) {
                log.warning("Error creating directory: " + pvOutputDataDirectoryPath + " for PVoutput data backup. Assuming working directory.");
                pvOutputDataDirectoryPath = ".";
            } else {
                log.info("Created directory: " + pvOutputDataDirectoryPath + " for PVoutput data backup.");
            }
        } else {
            log.info("Using directory: " + pvOutputDataDirectoryPath + " for PVoutput data backup.");
        }

    }

    @Subscribe
    public void handle(MonitorMsgInverterStatus msg) {

         isInverterOnline = msg.isOnline;
    }


    @Subscribe
    public void handle(PeriodicInverterTelemetries telemetries) {
        try {

            if (running) {
                telemetriesQueue.add(telemetries);
                log.info("Stored telemetries: " + telemetries);
            }

        } catch (Exception ex) {
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
                case "status":
                    request.response = handleStatusRequest(request.paramsMap);
                    break;
                default:
                    request.response = new EBResponseNOK(-1, "Received invalid command: " + request.opcode());
                    break;
            }
        } catch (Exception ex) {
            request.response = new EBResponseNOK(-1, "Runtime Error: " + ex.getMessage());

        }
    }


    protected EBResponse handleStatusRequest(Map paramsMap) {

        String status = running ? "on" : "off";
        return new EBResponseOK(status);

    }

    protected EBResponse handleTestRequest(Map paramsMap) {

        EBResponse result;
        try {
            boolean isOK = testPvOutputServer();
            result = isOK ? new EBResponseOK("") : new EBResponseNOK(-1, "Got Wrong Response form Server");
        } catch (Exception ex) {
            result = new EBResponseNOK(1, ex.getMessage());
        }

        return result;

    }

    protected EBResponse handleStartRequest(Map paramsMap) {

        EBResponse result;
        try {
            if (!running) {
                start();
            }
            result = new EBResponseOK("");
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


    public void stop() {
        running = false;
        log.info("Main Loop Stopped");
    }

    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mainLoop();
            }
        }).start();
    }

    protected void mainLoop() {
        final long PERIODICITY = (long) (params.period * 1000);
        final long WINDOW_MS = (long) (params.timeWindowSec * 1000);
        log.info("Main Loop Started");
        running = true;
        while (running) {
            try {
                if (isInverterOnline) {
                    Long now = new Date().getTime();
                    Long since = now - WINDOW_MS;
                    telemetriesQueue.removeOlderThan(since);
                    PeriodicInverterTelemetries dataPublished = telemetriesQueue.average();
                    if (dataPublished != null) {
                        publish2PvOutput(dataPublished);
                    } else {
                        log.fine("No data available for publication");
                    }
                }
                else {
                    log.info("Inverter is not online, examining data backup files");
                    String pvOutputFileData = MyUtils.selectFirstFile(pvOutputDataDirectoryPath, ".csv");
                    if (pvOutputFileData.isEmpty()) {
                        log.fine("No backup file found to upload to PvOutput in: " + pvOutputDataDirectoryPath);
                    } else {
                        batchPublish2PvOutput(pvOutputFileData);
                    }
                }

                Thread.sleep(PERIODICITY);
            } catch (Exception e) {
                log.severe(e.getMessage());
                e.printStackTrace();
            }
        }

    }


    public String savePvOutputRecord(PvOutputRecord pvData) throws IOException {

        String[] values = new String[5];
        values[0] = Long.toString(pvData.timestamp);
        values[1] = Float.toString(pvData.dailyCumulatedEnergy);
        values[2] = Float.toString(pvData.totalPowerGenerated);
        values[3] = Float.toString(pvData.temperature);
        values[4] = Float.toString(pvData.totalGridVoltage);

        Date actualDate = new Date(pvData.timestamp);
        String fileName = FormatStringUtils.fromDate(actualDate);
        fileName = pvOutputDataDirectoryPath + File.separator + fileName + ".csv";
        BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
        CSVWriter writer = new CSVWriter(out, ',', CSVWriter.NO_QUOTE_CHARACTER);
        writer.writeNext(values);
        out.close();
        log.info("PVOutput data saved in: " + fileName);
        return fileName;
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
            savePvOutputRecord(new PvOutputRecord(telemetries));
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

    public void batchPublish2PvOutput(String dataStorageFileName) throws IOException {
        int responseCode = -1;
        String data = "";
        String requestUrl = "";
        boolean toBeDeleted = false;
        try {
            List<PvOutputRecord> savedData2Send = readPvOutputRecordSet(dataStorageFileName);
            if (!savedData2Send.isEmpty()) {
                data = pvOutputRecordList2String(savedData2Send);
                requestUrl = generatePvOutputBatchUpdateUrl(data);
                URL obj = new URL(requestUrl);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                // optional default is GET
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.setRequestMethod("GET");
                responseCode = con.getResponseCode();
                log.info("Sending 'GET' request: " + requestUrl);
                log.info("Response Code: " + responseCode + " " + con.getResponseMessage());
                toBeDeleted = (responseCode == 200);
            } else {
                log.warning("File: " + dataStorageFileName + " is empty or has no valid data");
            }
        } catch (Exception e) {
            log.severe("Error publishing batch data  to PVOutput: " + e.getMessage());
            log.severe("Data: " + data + "\n requestUrl" + requestUrl);
        }
        if (toBeDeleted) {
            boolean deleted = new File(dataStorageFileName).delete();
            log.info("Data contained in: " + dataStorageFileName + " where successfully updated to PvOutput. The file was deleted? " + deleted);
        }


    }

    public static List<PvOutputRecord> readPvOutputRecordSet(String filePath) throws IOException {

        List<PvOutputRecord> pvOutputRecordList = new ArrayList<>();

        CSVReader reader = new CSVReader(new FileReader(filePath), ',');
        List<String[]> allRows = reader.readAll();

        for (String[] rec : allRows) {
            if (rec.length == 5) {
                PvOutputRecord pvOutputRecord = new PvOutputRecord();
                pvOutputRecord.timestamp = Long.decode(rec[0]);
                pvOutputRecord.dailyCumulatedEnergy = Float.parseFloat(rec[1]);
                pvOutputRecord.totalPowerGenerated = Float.parseFloat(rec[2]);
                pvOutputRecord.temperature = Float.parseFloat(rec[3]);
                pvOutputRecord.totalGridVoltage = Float.parseFloat(rec[4]);
                pvOutputRecordList.add(pvOutputRecord);
            }
        }

        return pvOutputRecordList;

    }

    public String pvOutputRecordList2String(List<PvOutputRecord> dataList) {
        String charSep = ",";
        String recordSep = ";";
        String data = "";
        for (PvOutputRecord pvRecord : dataList) {
            Date date = new Date();
            date.setTime(pvRecord.timestamp);
            data += convertDate(date) + charSep + convertDayTime(date) + charSep + pvRecord.dailyCumulatedEnergy + charSep + pvRecord.totalPowerGenerated + charSep;
            data += "-1" + charSep + "-1" + charSep;
            data += pvRecord.temperature + charSep + pvRecord.totalGridVoltage + recordSep;
        }
        data = data.substring(0, data.length() - 1);

        return data;

    }

    private String generatePvOutputBatchUpdateUrl(String data) {
        Map<String, Object> map = new LinkedHashMap<>();


        map.put("key", params.apiKey);
        map.put("sid", params.systemId);
        map.put("data", data);

        String result = params.url + "/addbatchstatus.jsp?" + HttpUtils.urlEncodeUTF8(map);
        return result;
    }


}
