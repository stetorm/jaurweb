package com.steto.monitor.pvoutput;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.gson.Gson;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.request.AuroraCumEnergyEnum;
import com.steto.jaurlib.request.AuroraDspRequestEnum;
import com.steto.jaurlib.response.AResp_VersionId;
import com.steto.jaurlib.response.AuroraResponse;
import com.steto.jaurlib.response.ResponseErrorEnum;
import com.steto.monitor.HwSettings;
import com.steto.monitor.InverterStatusEnum;
import com.steto.monitor.cmd.*;
import com.steto.utils.FormatStringUtils;
import com.steto.utils.HttpUtils;
import com.steto.utils.MyUtils;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PvOutput {


    private String pvOutputDataDirectoryPath;
    protected PVOutputParams pvOutputParams;
    protected HwSettings hwSettings;
    public int pvOutputHttpRequestTimeout = 10000;


    private final String configurationFileName;
    protected Logger log = Logger.getLogger(getClass().getSimpleName());
    protected float dailyCumulatedEnergy = 0;
    protected long allPowerGeneration = 0;
    protected double inverterTemperature = 0;
    protected double allGridVoltage = 0;
    protected final AuroraDriver auroraDriver;
    private ScheduledFuture<?> pvOutputFeature;
    Map<String, AuroraCumEnergyEnum> mapEnergyCmd = new HashMap<String, AuroraCumEnergyEnum>();
    Map<String, AuroraDspRequestEnum> mapDspCmd = new HashMap<String, AuroraDspRequestEnum>();
    private InverterStatusEnum inverterStatus = InverterStatusEnum.OFFLINE;
    private boolean pvOutputRunning = false;
    private Date lastCheckDate;

    public PvOutput(AuroraDriver auroraDriver, String configFile, String dataLogDirPath) throws IOException, SerialPortException {
        this.auroraDriver = auroraDriver;

        this.configurationFileName = configFile;
        this.pvOutputDataDirectoryPath = dataLogDirPath;

        lastCheckDate = new Date();

        pvOutputParams = loadPvOutputConfiguration();
        hwSettings = loadHwSettings();

        pvOutputParams = pvOutputParams == null ? new PVOutputParams() : pvOutputParams;
        hwSettings = hwSettings == null ? new HwSettings() : hwSettings;

        mapEnergyCmd.put("daily", AuroraCumEnergyEnum.DAILY);
        mapEnergyCmd.put("weekly", AuroraCumEnergyEnum.WEEKLY);
        mapEnergyCmd.put("monthly", AuroraCumEnergyEnum.MONTHLY);
        mapEnergyCmd.put("yearly", AuroraCumEnergyEnum.YEARLY);
        mapEnergyCmd.put("last7days", AuroraCumEnergyEnum.LAST7DAYS);
        mapEnergyCmd.put("partial", AuroraCumEnergyEnum.PARTIAL);
        mapEnergyCmd.put("total", AuroraCumEnergyEnum.TOTAL);

        mapDspCmd.put("freqAll", AuroraDspRequestEnum.FREQUENCY_ALL);
        mapDspCmd.put("gridVoltageAll", AuroraDspRequestEnum.GRID_VOLTAGE_ALL);
        mapDspCmd.put("gridCurrentAll", AuroraDspRequestEnum.GRID_CURRENT_ALL);
        mapDspCmd.put("gridPowerAll", AuroraDspRequestEnum.GRID_POWER_ALL);
        mapDspCmd.put("input1Voltage", AuroraDspRequestEnum.INPUT_1_VOLTAGE);
        mapDspCmd.put("input1Current", AuroraDspRequestEnum.INPUT_1_CURRENT);
        mapDspCmd.put("input2Voltage", AuroraDspRequestEnum.INPUT_2_VOLTAGE);
        mapDspCmd.put("input2Current", AuroraDspRequestEnum.INPUT_2_CURRENT);
        mapDspCmd.put("inverterTemp", AuroraDspRequestEnum.INVERTER_TEMPERATURE_GRID_TIED);
        mapDspCmd.put("boosterTemp", AuroraDspRequestEnum.BOOSTER_TEMPERATURE_GRID_TIED);

        createPvOutputLogDirectory();

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

    public void init() throws SerialPortException {

        log.info("Aurora Monitor Initializing...");
        initInverterDriver(hwSettings.serialPort, hwSettings.serialPortBaudRate);

        checkInverterStatus();
    }

    protected void initInverterDriver(String serialPortName, int serialPortBaudRate) throws SerialPortException {

        SerialPort newSerialPort = new SerialPort(serialPortName);
        newSerialPort.openPort();//Open serial port
        newSerialPort.setParams(serialPortBaudRate, 8, 1, 0);//Set params.
        auroraDriver.setSerialPort(newSerialPort);
        log.info("Serial Port initialized with values: " + serialPortName + ", " + serialPortBaudRate);
    }

    public void setPvOutputUrl(String pvOutputUrl) {
        pvOutputParams.url = pvOutputUrl;
    }

    public void setPvOutputApiKey(String pvOutputApiKey) {
        pvOutputParams.apiKey = pvOutputApiKey;
    }

    public void setPvOutputPeriod(float period) {
        pvOutputParams.period = period;
    }

    public String getPvOutputApiKey() {
        return pvOutputParams.apiKey;
    }

    public int getPvOutputSystemId() {
        return pvOutputParams.systemId;
    }

    public String getPvOutputUrl() {
        return pvOutputParams.url;
    }

    public float getPvOutputPeriod() {
        return pvOutputParams.period;
    }


    public void setPvOutputSystemId(int pvOutputSystemId) {
        pvOutputParams.systemId = pvOutputSystemId;
    }

    public boolean publish2PvOutput() throws IOException {
        boolean executed = false;
        PvOutputRecord pvOutputRecord = getActualPvOutputValues();
        String requestUrl = generatePvOutputLiveUpdateUrl(pvOutputRecord);// put in your url
        int responseCode = -1;

        try {

            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(pvOutputHttpRequestTimeout).build();
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
            savePvOutputRecord(pvOutputRecord);
        } else {
            log.info("Dati inviati a PVOutput: \n" + getActualPvOutputValues());
        }

        return executed;
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

    public PvOutputRecord getActualPvOutputValues() {

        PvOutputRecord result = new PvOutputRecord();


        Date now = new Date();
        result.timestamp = now.getTime();
        result.dailyCumulatedEnergy = dailyCumulatedEnergy;
        result.totalPowerGenerated = allPowerGeneration;
        result.temperature = (float) inverterTemperature;
        result.totalGridVoltage = (float) allGridVoltage;
        return result;


    }

    public boolean testPvOutputServer() throws IOException {

        int responseCode = -1;
        String requestUrl = generatePvOutputTestUrl();

        try {
            URL obj = new URL(requestUrl);
            HttpURLConnection con;
            con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            responseCode = con.getResponseCode();
            log.info("Sending 'GET' request: " + requestUrl);
            log.info("Response Code: " + responseCode + " " + con.getResponseMessage());
        } catch (Exception e) {
            log.severe("Http request for test failed. Response Code: " + responseCode + "," + e.getMessage());
        }

        return responseCode == 200;

    }


    private String generatePvOutputBatchUpdateUrl(String data) {
        Map<String, Object> map = new LinkedHashMap<>();


        map.put("key", pvOutputParams.apiKey);
        map.put("sid", pvOutputParams.systemId);
        map.put("data", data);

        String result = pvOutputParams.url + "/addbatchstatus.jsp?" + HttpUtils.urlEncodeUTF8(map);
        return result;
    }

    private String generatePvOutputLiveUpdateUrl(PvOutputRecord pvOutputRecord) {
        Map<String, Object> map = new LinkedHashMap<>();

        Date now = new Date();
        now.setTime(pvOutputRecord.timestamp);
        String date = convertDate(now);
        String time = convertDayTime(now);
        map.put("key", pvOutputParams.apiKey);
        map.put("sid", pvOutputParams.systemId);
        map.put("d", date);
        map.put("t", time);
        map.put("v1", pvOutputRecord.dailyCumulatedEnergy);
        map.put("v2", pvOutputRecord.totalPowerGenerated);
        map.put("v5", pvOutputRecord.temperature);
        map.put("v6", pvOutputRecord.totalGridVoltage);

        String result = pvOutputParams.url + "/addstatus.jsp?" + HttpUtils.urlEncodeUTF8(map);
        return result;
    }

    private String generatePvOutputTestUrl() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("key", pvOutputParams.apiKey);
        map.put("sid", pvOutputParams.systemId);

        String result = pvOutputParams.url + "/getinsolation.jsp?" + HttpUtils.urlEncodeUTF8(map);
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

    public float getCumulatedEnergyReadout() {
        return dailyCumulatedEnergy;
    }

    public long getInstantPowerReadout() {
        return allPowerGeneration;
    }

    public Double getVoltageReadout() {
        return allGridVoltage;
    }

    public Double getTemperatureReadout() {

        return inverterTemperature;
    }

    public boolean acquireDataToBePublished() {
        boolean result = true;
        AuroraResponse response = null;
        try {
            log.info("Starting data acquisition from inverter");
            response = auroraDriver.acquireCumulatedEnergy(hwSettings.inverterAddress, AuroraCumEnergyEnum.DAILY);
            result = result && (response.getErrorCode() == ResponseErrorEnum.NONE);
            long cumulatedEnergy = response.getLongParam();
            response = auroraDriver.acquireDspValue(hwSettings.inverterAddress, AuroraDspRequestEnum.GRID_POWER_ALL);
            result = result && (response.getErrorCode() == ResponseErrorEnum.NONE);
            long actualPower = (long) response.getFloatParam();
            dailyCumulatedEnergy = cumulatedEnergy == 0 ? dailyCumulatedEnergy + (actualPower + allPowerGeneration) * getPvOutputPeriod() / (2 * 60 * 60) : cumulatedEnergy;
            allPowerGeneration = actualPower;
            response = auroraDriver.acquireDspValue(hwSettings.inverterAddress, AuroraDspRequestEnum.GRID_VOLTAGE_ALL);
            result = result && (response.getErrorCode() == ResponseErrorEnum.NONE);
            allGridVoltage = response.getFloatParam();
            response = auroraDriver.acquireDspValue(hwSettings.inverterAddress, AuroraDspRequestEnum.INVERTER_TEMPERATURE_GRID_TIED);
            result = result && (response.getErrorCode() == ResponseErrorEnum.NONE);
            inverterTemperature = response.getFloatParam();
            log.info("data acquisition from inverter completed");
        } catch (Exception e) {
            result = false;
            String errMsg = "Data acquisition failed: " + e.getMessage();
            if (response != null) {
                errMsg += ", Error Code: " + response.getErrorCode();
            }
            log.severe(errMsg);
        }

        updateInverterStatus(result);
        return result;

    }

    protected HwSettings loadHwSettings() {

        Properties properties = new Properties();
        HwSettings result = new HwSettings();

        String currentProperty = "";
        try {
            InputStream inputStream = new FileInputStream(new File(configurationFileName));
            properties.load(inputStream);
            currentProperty = "inverterAddress";
            result.inverterAddress = Integer.parseInt(properties.getProperty(currentProperty));
            currentProperty = "serialPortBaudRate";
            result.serialPortBaudRate = Integer.parseInt(properties.getProperty(currentProperty));
            currentProperty = "serialPort";
            result.serialPort = properties.getProperty(currentProperty);
        } catch (Exception e) {
            log.severe("Error reading file: " + configurationFileName + ", property: " + currentProperty);
            result = null;
        }

        return result;
    }

    public PVOutputParams loadPvOutputConfiguration() throws IOException {

        Properties properties = new Properties();
        PVOutputParams result = new PVOutputParams();

        String currentProperty = "";
        try {
            InputStream inputStream = new FileInputStream(new File(configurationFileName));
            properties.load(inputStream);
            log.info("Valori letti dal file: " + configurationFileName + ",\n " + properties.toString());
            currentProperty = "pvOutputSystemId";
            result.systemId = Integer.parseInt(properties.getProperty(currentProperty));
            currentProperty = "pvOutputPeriod";
            result.period = Float.parseFloat(properties.getProperty(currentProperty));
            currentProperty = "pvOutputUrl";
            result.url = properties.getProperty(currentProperty);
            currentProperty = "pvOutputApiKey";
            result.apiKey = properties.getProperty(currentProperty);
        } catch (Exception e) {
            log.severe("Error reading file: " + configurationFileName + ", property: " + currentProperty);
            result = null;
        }

        return result;

    }

    public void savePvOutputConfiguration() throws IOException {

        Properties pvOutputParamsProperties = pvOutputParams.toProperties();

        HwSettings fileHwSettings = loadHwSettings();
        Properties merged = new Properties();
        if (fileHwSettings != null) {
            Properties hwSettingsProperties = fileHwSettings.toProperties();
            merged.putAll(hwSettingsProperties);
        }
        merged.putAll(pvOutputParamsProperties);

        OutputStream outputStream = new FileOutputStream(new File(configurationFileName));
        merged.store(outputStream, "");
        outputStream.close();

    }


    public void saveHwSettingsConfiguration() throws IOException {

        Properties hwSettingsProperties = hwSettings.toProperties();

        PVOutputParams filePvOutputParams = loadPvOutputConfiguration();
        Properties merged = new Properties();
        if (filePvOutputParams != null) {
            Properties filePvOutputParamsProperties = filePvOutputParams.toProperties();
            merged.putAll(filePvOutputParamsProperties);
        }
        merged.putAll(hwSettingsProperties);

        OutputStream outputStream = new FileOutputStream(new File(configurationFileName));
        merged.store(outputStream, "");
        outputStream.close();

    }

    protected String execInverterCommand(Map<String, String> map) {

        String opCodeParameter = map.get("opcode");
        String subCodeParameter = map.get("subcode");
        int addressParameter = Integer.parseInt(map.get("address"));
        hwSettings.inverterAddress = addressParameter;
        String responseString = "";
        AuroraResponse auroraResponse = null;
        try {
            switch (opCodeParameter) {
                case "dspData":
                    responseString = execDspDataInverterCommand(subCodeParameter, addressParameter);
                    break;
                case "cumEnergy":
                    responseString = execCumEnergyInverterCommand(subCodeParameter, addressParameter);
                    break;
                case "productNumber":
                    auroraResponse = auroraDriver.acquireProductNumber(addressParameter);
                    break;
                case "serialNumber":
                    auroraResponse = auroraDriver.acquireSerialNumber(addressParameter);
                    break;
                case "versionNumber":
                    auroraResponse = auroraDriver.acquireVersionId(addressParameter);
                    break;
                case "firmwareNumber":
                    auroraResponse = auroraDriver.acquireFirmwareVersion(addressParameter);
                    break;
                case "manufacturingDate":
                    auroraResponse = auroraDriver.acquireMFGdate(addressParameter);
                    break;
                case "sysConfig":
                    auroraResponse = auroraDriver.acquireSystemConfig(addressParameter);
                    break;
                case "timeCounter":
                    auroraResponse = auroraDriver.acquireTimeCounter(addressParameter);
                    break;
                case "actualTime":
                    auroraResponse = auroraDriver.acquireActualTime(addressParameter);
                    break;
                default:
                    responseString = "Error: bad request";
            }

        } catch (Exception e) {
            responseString = "ERROR!: " + e.getMessage();
        }
        if (responseString.isEmpty()) {
            if (auroraResponse == null) {
                responseString = "UNKNOWN ERROR!";
            } else {
                responseString = auroraResponse.getErrorCode() == ResponseErrorEnum.NONE ? auroraResponse.toString() : auroraResponse.getErrorCode().toString() + " ERROR";
                updateInverterStatus(auroraResponse.getErrorCode() != ResponseErrorEnum.TIMEOUT);
            }
        }

        return responseString;
    }


    protected String execCumEnergyInverterCommand(String subCodeParameter, int addressParameter) {
        AuroraResponse auroraResponse = null;
        String errorString = "UNKNOWN ERROR!";
        AuroraCumEnergyEnum cumEnergyEnum = mapEnergyCmd.get(subCodeParameter);
        try {
            auroraResponse = auroraDriver.acquireCumulatedEnergy(addressParameter, cumEnergyEnum);

        } catch (Exception e) {
            errorString = "ERROR! " + e.getMessage();
        }
        return auroraResponse == null ? errorString : auroraResponse.toString();
    }

    protected String execDspDataInverterCommand(String subCodeParameter, int addressParameter) {
        AuroraResponse auroraResponse = null;
        String errorString = "UNKNOWN ERROR!";
        AuroraDspRequestEnum dspRequestEnum = mapDspCmd.get(subCodeParameter);
        try {
            auroraResponse = auroraDriver.acquireDspValue(addressParameter, dspRequestEnum);

        } catch (Exception e) {
            errorString = "ERROR! " + e.getMessage();
        }
        return auroraResponse == null ? errorString : auroraResponse.toString();
    }

    public String execCommand(MonCmdLoadConfig cmd) throws IOException {
        String result = "";
        pvOutputParams = loadPvOutputConfiguration();
        Map<String, Object> mapPvOutParameters = getPvOutputParametersMap();
        Map<String, Object> mapSettings = getSettingsMap();
        Map<String, Object> allParameters = new HashMap();
        allParameters.putAll(mapPvOutParameters);
        allParameters.putAll(mapSettings);
        Gson gson = new Gson();
        return gson.toJson(allParameters);
    }

    public String execCommand(MonCmdSavePvOutputConfig cmd) throws IOException {
        String result = "";
        setPvOutputApiKey(cmd.paramsMap.get("pvOutputApiKey"));
        setPvOutputUrl(cmd.paramsMap.get("pvOutputUrl"));
        //setPvOutputPeriod(Float.valueOf(cmd.command.get("pvOutputPeriod")));
        setPvOutputSystemId(Integer.valueOf(cmd.paramsMap.get("pvOutputSystemId")));
        savePvOutputConfiguration();
        result = "{response : OK}";
        return result;
    }

    public String execCommand(MonReqSaveInvSettings cmd) throws IOException, SerialPortException {
        String result = "";
        setSerialPortBaudRate(Integer.valueOf(cmd.paramsMap.get("baudRate")));
        setSerialPortName(cmd.paramsMap.get("serialPort"));
        setInverterAddress(Integer.valueOf(cmd.paramsMap.get("inverterAddress")));
        saveHwSettingsConfiguration();
        init();
        result = "{response : OK}";
        return result;
    }


    public String execCommand(MonCmdInverter cmd) throws IOException {
        String result = "";

        result = execInverterCommand(cmd.paramsMap);

        return result;
    }

    public String execCommand(MonCmdTestPVoutput cmd) throws IOException {
        String result = "";
        setPvOutputApiKey(cmd.paramsMap.get("pvOutputApiKey"));
        setPvOutputUrl(cmd.paramsMap.get("pvOutputUrl"));
//        setPvOutputPeriod(Integer.valueOf(cmd.command.get("pvOutputPeriod")));
        setPvOutputSystemId(Integer.valueOf(cmd.paramsMap.get("pvOutputSystemId")));
        result = FormatStringUtils.jsonResult(testPvOutputServer());
        return result;
    }

    private Map<String, Object> getSettingsMap() {
        Map<String, Object> result = new HashMap();
        result.put("serialPort", getSerialPortName());
        result.put("baudRate", getSerialPortBaudRate());
        result.put("inverterAddress", getInverterAddress());
        return result;
    }

    private Map<String, Object> getPvOutputParametersMap() {
        Map<String, Object> result = new HashMap();
        result.put("pvOutputApiKey", getPvOutputApiKey());
        result.put("pvOutputPeriod", getPvOutputPeriod());
        result.put("pvOutputSystemId", getPvOutputSystemId());
        result.put("pvOutputUrl", getPvOutputUrl());
        return result;
    }

    public void pvOutputjob() {
        try {
            log.finer("PVoutputJob restarted.");
            Date actualDate = new Date();
            if (!MyUtils.sameDay(actualDate, lastCheckDate)) {
                dailyCumulatedEnergy = 0;
            }
            lastCheckDate = actualDate;
            checkInverterStatus();
            if (isInverterOnline()) {
                boolean dataAcquisitionSuccessful = acquireDataToBePublished();
                if (dataAcquisitionSuccessful) {
                    publish2PvOutput();
                } else {
                    log.warning("Periodic data publication to PVOutput skipped because inverter is OFFLINE.");
                }
            } else {
                log.info("Inverter not online, status: " + inverterStatus);
                String pvOutputFileData = MyUtils.selectFirstFile(pvOutputDataDirectoryPath, ".csv");
                if (pvOutputFileData.isEmpty()) {
                    log.fine("No backup file found to upload to PvOutput in: " + pvOutputDataDirectoryPath);
                } else {
                    batchPublish2PvOutput(pvOutputFileData);
                }
            }
        } catch (Exception e) {
            log.severe("Executing periodic data publication to PVOutput: " + e.getMessage());
        }

    }


    public void startPvOutput() {

        if (pvOutputRunning) {
            return;
        }
        pvOutputRunning = true;

        ScheduledExecutorService serviceExec = Executors.newScheduledThreadPool(1);

        long millisecPeriod = (long) (getPvOutputPeriod() * 1000);
        pvOutputFeature = serviceExec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                log.info("Thread in charge of pvoutput data publishing is: " + Thread.currentThread().getName());
                pvOutputRunning = true;
                pvOutputjob();
                log.finer("Job terminated");
            }
        }, 0, millisecPeriod, TimeUnit.MILLISECONDS);

        log.info("Publication job to PVOutput Started with period (ms): " + millisecPeriod);

    }

    public void checkInverterStatus() {

        AuroraResponse badResult = new AResp_VersionId();
        badResult.setErrorCode(ResponseErrorEnum.UNKNOWN);

        AuroraResponse result;
        try {
            result = auroraDriver.acquireVersionId(hwSettings.inverterAddress);
        } catch (Exception e) {
            result = badResult;
        }

        result = result == null ? badResult : result;

        log.fine("Check Status Result: " + result.getErrorCode());
        updateInverterStatus(result.getErrorCode() == ResponseErrorEnum.CRC || result.getErrorCode() == ResponseErrorEnum.NONE);


    }

    private void updateInverterStatus(boolean acquisitionSuccessfull) {

        switch (inverterStatus) {
            case OFFLINE:
                inverterStatus = acquisitionSuccessfull ? InverterStatusEnum.ONLINE : InverterStatusEnum.OFFLINE;
                break;
            case ONLINE:
                inverterStatus = acquisitionSuccessfull ? InverterStatusEnum.ONLINE : InverterStatusEnum.UNCERTAIN;
                break;
            case UNCERTAIN:
                inverterStatus = acquisitionSuccessfull ? InverterStatusEnum.ONLINE : InverterStatusEnum.OFFLINE;
                break;

        }
        log.info("Inverter Status is :" + inverterStatus);
    }

    public void stopPvOutput() {


        if (pvOutputFeature != null) {
            pvOutputFeature.cancel(true);
            pvOutputRunning = false;
        }
        log.info("Publication job to PVOutput Stopped.");

    }

    public void stop() {
        auroraDriver.stop();
        stopPvOutput();
    }

    public boolean isInverterOnline() {
        return inverterStatus == InverterStatusEnum.ONLINE || inverterStatus == InverterStatusEnum.UNCERTAIN;
    }

    public boolean getPvOutputRunningStatus() {

        return pvOutputRunning;

    }

    public String getSerialPortName() {
        return hwSettings.serialPort;
    }

    public void setSerialPortName(String serialPortName) {
        hwSettings.serialPort = serialPortName;
    }


    public int getSerialPortBaudRate() {
        return hwSettings.serialPortBaudRate;
    }

    public void setSerialPortBaudRate(Integer serialPortBaudRate) {
        hwSettings.serialPortBaudRate = serialPortBaudRate;
    }

    public int getInverterAddress() {
        return hwSettings.inverterAddress;
    }

    public void setInverterAddress(int aInverterAddress) {
        hwSettings.inverterAddress = aInverterAddress;
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

    public String execCommand(MonCmdReadStatus cmd) {
        Map<String, String> mapResult = new HashMap<>();
        boolean isPvOutRunning  = getPvOutputRunningStatus();
        String pvStatus = getPvOutputRunningStatus() ? "on" : "off";
        if (!isPvOutRunning) {
            checkInverterStatus();
        }
        String inverterStatus = isInverterOnline() ? "online" : "offline";
        mapResult.put("pvOutputStatus", pvStatus);
        mapResult.put("inverterStatus", inverterStatus);

        return new Gson().toJson(mapResult);
    }
}


