package com.steto.jaurmon.monitor;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.request.AuroraCumEnergyEnum;
import com.steto.jaurlib.request.AuroraDspRequestEnum;
import com.steto.jaurlib.response.*;
import com.steto.jaurmon.monitor.cmd.*;
import com.steto.jaurmon.monitor.pvoutput.PVOutputParams;
import jssc.SerialPort;
import jssc.SerialPortException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;


public class AuroraMonitor {


    private final EventBus theEventBus;
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

    public AuroraMonitor(EventBus aEventBus, AuroraDriver auroraDriver, String configFile, String dataLogDirPath) throws IOException, SerialPortException {

        theEventBus = aEventBus;
        this.auroraDriver = auroraDriver;

        this.configurationFileName = configFile;
        this.pvOutputDataDirectoryPath = dataLogDirPath;

        lastCheckDate = new Date();

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

        theEventBus.register(this);


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
            updateInverterStatus(response.getErrorCode());

            long cumulatedEnergy = response.getLongParam();

            response = auroraDriver.acquireDspValue(hwSettings.inverterAddress, AuroraDspRequestEnum.GRID_POWER_ALL);
            result = result && (response.getErrorCode() == ResponseErrorEnum.NONE);
            updateInverterStatus(response.getErrorCode());

            long actualPower = (long) response.getFloatParam();
            // TODO Substitute 12345 with the time span
            dailyCumulatedEnergy = cumulatedEnergy == 0 ? dailyCumulatedEnergy + (actualPower + allPowerGeneration) * 12345 / (2 * 60 * 60) : cumulatedEnergy;
            allPowerGeneration = actualPower;
            response = auroraDriver.acquireDspValue(hwSettings.inverterAddress, AuroraDspRequestEnum.GRID_VOLTAGE_ALL);
            result = result && (response.getErrorCode() == ResponseErrorEnum.NONE);
            updateInverterStatus(response.getErrorCode());
            allGridVoltage = response.getFloatParam();

            response = auroraDriver.acquireDspValue(hwSettings.inverterAddress, AuroraDspRequestEnum.INVERTER_TEMPERATURE_GRID_TIED);
            result = result && (response.getErrorCode() == ResponseErrorEnum.NONE);
            updateInverterStatus(response.getErrorCode());
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


    public void saveHwSettingsConfiguration() throws IOException {

/*
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
  */
    }



    @Subscribe
    @AllowConcurrentEvents
    public void handleInverterCommand(InvCmdDspData cmd) {
        WebResponse cmdResponse;

        try {
            AResp_DspData auroraResponse  = (AResp_DspData) auroraDriver.acquireDspValue(cmd.invAddress, cmd.magnitude);
            cmdResponse = (auroraResponse.getErrorCode() == ResponseErrorEnum.NONE) ? new WebResponseOK(String.valueOf(auroraResponse.getFloatParam())) : new WebResponseNOK(auroraResponse.getErrorCode().get(), auroraResponse.getErrorCode().toString());
            updateInverterStatus(auroraResponse.getErrorCode() );

        } catch (Exception e) {
            String errorString = e.getMessage();
            cmdResponse = new WebResponseNOK(-1, errorString);
        }


        cmd.response = cmdResponse;

    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleInverterCommand(InvCmdCumEnergy cmd) {
        WebResponse cmdResponse;

        try {
            AResp_CumulatedEnergy auroraResponse  = (AResp_CumulatedEnergy) auroraDriver.acquireCumulatedEnergy(cmd.invAddress, cmd.period);
            cmdResponse = (auroraResponse.getErrorCode() == ResponseErrorEnum.NONE) ? new WebResponseOK(auroraResponse.get().toString()) : new WebResponseNOK(auroraResponse.getErrorCode().get(), auroraResponse.getErrorCode().toString());
            updateInverterStatus(auroraResponse.getErrorCode() );

        } catch (Exception e) {
            String errorString = e.getMessage();
            cmdResponse = new WebResponseNOK(-1, errorString);
        }


        cmd.response = cmdResponse;

    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleInverterCommand(InvCmdProductNumber cmd) {
        WebResponse cmdResponse;

        try {
            AResp_ProductNumber auroraResponse  = (AResp_ProductNumber) auroraDriver.acquireProductNumber(cmd.invAddress);
            cmdResponse = (auroraResponse.getErrorCode() == ResponseErrorEnum.NONE) ? new WebResponseOK(auroraResponse.get()) : new WebResponseNOK(auroraResponse.getErrorCode().get(), auroraResponse.getErrorCode().toString());
            updateInverterStatus(auroraResponse.getErrorCode() );

        } catch (Exception e) {
            String errorString = e.getMessage();
            cmdResponse = new WebResponseNOK(-1, errorString);
        }


        cmd.response = cmdResponse;

    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleInverterCommand(InvCmdSerialNumber cmd) {
        WebResponse cmdResponse;

        try {
            AResp_SerialNumber auroraResponse  = (AResp_SerialNumber) auroraDriver.acquireSerialNumber(cmd.invAddress);
            cmdResponse = (auroraResponse.getErrorCode() == ResponseErrorEnum.NONE) ? new WebResponseOK(auroraResponse.get()) : new WebResponseNOK(auroraResponse.getErrorCode().get(), auroraResponse.getErrorCode().toString());
            updateInverterStatus(auroraResponse.getErrorCode() );

        } catch (Exception e) {
            String errorString = e.getMessage();
            cmdResponse = new WebResponseNOK(-1, errorString);
        }


        cmd.response = cmdResponse;

    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleInverterCommand(InvCmdVersionNumber cmd) {
        WebResponse cmdResponse;

        try {
            AResp_VersionId auroraResponse  = (AResp_VersionId) auroraDriver.acquireVersionId(cmd.invAddress);
            cmdResponse = (auroraResponse.getErrorCode() == ResponseErrorEnum.NONE) ? new WebResponseOK(auroraResponse.toString()) : new WebResponseNOK(auroraResponse.getErrorCode().get(), auroraResponse.getErrorCode().toString());
            updateInverterStatus(auroraResponse.getErrorCode() );

        } catch (Exception e) {
            String errorString = e.getMessage();
            cmdResponse = new WebResponseNOK(-1, errorString);
        }


        cmd.response = cmdResponse;

    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleInverterCommand(InvCmdFirmwareVersion cmd) {
        WebResponse cmdResponse;

        try {
            AResp_FwVersion auroraResponse  = (AResp_FwVersion) auroraDriver.acquireFirmwareVersion(cmd.invAddress);
            cmdResponse = (auroraResponse.getErrorCode() == ResponseErrorEnum.NONE) ? new WebResponseOK(auroraResponse.get()) : new WebResponseNOK(auroraResponse.getErrorCode().get(), auroraResponse.getErrorCode().toString());
            updateInverterStatus(auroraResponse.getErrorCode() );

        } catch (Exception e) {
            String errorString = e.getMessage();
            cmdResponse = new WebResponseNOK(-1, errorString);
        }


        cmd.response = cmdResponse;

    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleInverterCommand(InvCmdSysConfig cmd) {
        WebResponse cmdResponse;

        try {
            AResp_SysConfig auroraResponse  = (AResp_SysConfig) auroraDriver.acquireSystemConfig(cmd.invAddress);
            cmdResponse = (auroraResponse.getErrorCode() == ResponseErrorEnum.NONE) ? new WebResponseOK(String.valueOf(auroraResponse.getConfigCode())) : new WebResponseNOK(auroraResponse.getErrorCode().get(), auroraResponse.getErrorCode().toString());
            updateInverterStatus(auroraResponse.getErrorCode() );

        } catch (Exception e) {
            String errorString = e.getMessage();
            cmdResponse = new WebResponseNOK(-1, errorString);
        }


        cmd.response = cmdResponse;

    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleInverterCommand(InvCmdMFGdate cmd) {
        WebResponse cmdResponse;

        try {
            AResp_MFGdate auroraResponse  = (AResp_MFGdate) auroraDriver.acquireMFGdate(cmd.invAddress);
            cmdResponse = (auroraResponse.getErrorCode() == ResponseErrorEnum.NONE) ? new WebResponseOK(auroraResponse.get().toString()) : new WebResponseNOK(auroraResponse.getErrorCode().get(), auroraResponse.getErrorCode().toString());
            updateInverterStatus(auroraResponse.getErrorCode() );

        } catch (Exception e) {
            String errorString = e.getMessage();
            cmdResponse = new WebResponseNOK(-1, errorString);
        }


        cmd.response = cmdResponse;

    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleInverterCommand(InvCmdTimeCounter cmd) {
        WebResponse cmdResponse;

        try {
            AResp_TimeCounter auroraResponse  = (AResp_TimeCounter) auroraDriver.acquireTimeCounter(cmd.invAddress);
            cmdResponse = (auroraResponse.getErrorCode() == ResponseErrorEnum.NONE) ? new WebResponseOK(auroraResponse.get().toString()) : new WebResponseNOK(auroraResponse.getErrorCode().get(), auroraResponse.getErrorCode().toString());
            updateInverterStatus(auroraResponse.getErrorCode() );

        } catch (Exception e) {
            String errorString = e.getMessage();
            cmdResponse = new WebResponseNOK(-1, errorString);
        }


        cmd.response = cmdResponse;

    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleInverterCommand(InvCmdActualTime cmd) {
        WebResponse cmdResponse;

        try {
            AResp_ActualTime auroraResponse  = (AResp_ActualTime) auroraDriver.acquireActualTime(cmd.invAddress);
            cmdResponse = (auroraResponse.getErrorCode() == ResponseErrorEnum.NONE) ? new WebResponseOK(auroraResponse.get().toString()) : new WebResponseNOK(auroraResponse.getErrorCode().get(), auroraResponse.getErrorCode().toString());
            updateInverterStatus(auroraResponse.getErrorCode() );

        } catch (Exception e) {
            String errorString = e.getMessage();
            cmdResponse = new WebResponseNOK(-1, errorString);
        }


        cmd.response = cmdResponse;

    }

    protected String execDspDataInverterCommand(String subCodeParameter, int addressParameter) {
        AuroraResponse auroraResponse = null;
        String errorString = "UNKNOWN ERROR!";
        AuroraDspRequestEnum dspRequestEnum = mapDspCmd.get(subCodeParameter);
        try {
            auroraResponse = auroraDriver.acquireDspValue(addressParameter, dspRequestEnum);
            updateInverterStatus(auroraResponse.getErrorCode() );

        } catch (Exception e) {
            errorString = "ERROR! " + e.getMessage();
        }
        return auroraResponse == null ? errorString : auroraResponse.toString();
    }

    @Subscribe
    @AllowConcurrentEvents
    public String execCommand(MonCmdLoadConfig cmd) throws IOException {
        String result = "";
        Map<String, Object> mapSettings = getSettingsMap();
        Map<String, Object> allParameters = new HashMap();
        allParameters.putAll(mapSettings);
        Gson gson = new Gson();
        return gson.toJson(allParameters);
    }


    @Subscribe
    @AllowConcurrentEvents
    public void execCommand(MonCmdSaveSettings cmd) throws IOException, SerialPortException {
        String result = "";
        try {
            setSerialPortBaudRate(Integer.valueOf(cmd.paramsMap.get("baudRate")));
            setSerialPortName(cmd.paramsMap.get("serialPort"));
            setInverterAddress(Integer.valueOf(cmd.paramsMap.get("inverterAddress")));
            saveHwSettingsConfiguration();
            init();
            cmd.response = new WebResponseOK("");
        } catch (Exception ex) {
            cmd.response = new WebResponseNOK(-1, ex.getMessage());
            log.severe("Error executing cmd: " + cmd);
        }
    }


    private Map<String, Object> getSettingsMap() {
        Map<String, Object> result = new HashMap();
        result.put("serialPort", getSerialPortName());
        result.put("baudRate", getSerialPortBaudRate());
        result.put("inverterAddress", getInverterAddress());
        return result;
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
        updateInverterStatus(result.getErrorCode() );


    }

    private void updateInverterStatus(ResponseErrorEnum acquisitionOutcome) {

        boolean correct = (acquisitionOutcome == ResponseErrorEnum.NONE);
        switch (inverterStatus) {
            case OFFLINE:
                inverterStatus = correct ? InverterStatusEnum.ONLINE : InverterStatusEnum.OFFLINE;
                break;
            case ONLINE:
                inverterStatus = correct ? InverterStatusEnum.ONLINE : InverterStatusEnum.UNCERTAIN;
                break;
            case UNCERTAIN:
                inverterStatus = correct ? InverterStatusEnum.ONLINE : InverterStatusEnum.OFFLINE;
                break;

        }
        log.info("Inverter Status is :" + inverterStatus);
    }


    public void stop() {
        auroraDriver.stop();
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


    @Subscribe
    @AllowConcurrentEvents
    public void execCommand(MonCmdReadStatus cmd) {
        try {
            Map<String, String> mapResult = new HashMap<>();
            boolean isPvOutRunning = getPvOutputRunningStatus();
            String pvStatus = getPvOutputRunningStatus() ? "on" : "off";
            if (!isPvOutRunning) {
                checkInverterStatus();
            }
            String inverterStatus = isInverterOnline() ? "online" : "offline";
            mapResult.put("pvOutputStatus", pvStatus);
            mapResult.put("inverterStatus", inverterStatus);

        } catch (Exception ex) {
            cmd.response = new WebResponseNOK(-1, ex.getMessage());

        }
    }
}


