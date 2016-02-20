package com.steto.jaurmon.monitor;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.cmd.InverterCommandFactory;
import com.steto.jaurlib.eventbus.*;
import com.steto.jaurlib.request.AuroraCumEnergyEnum;
import com.steto.jaurlib.request.AuroraDspRequestEnum;
import com.steto.jaurlib.request.AuroraRequestFactory;
import com.steto.jaurlib.response.AResp_VersionId;
import com.steto.jaurlib.response.AuroraResponse;
import com.steto.jaurlib.response.AuroraResponseFactory;
import com.steto.jaurlib.response.ResponseErrorEnum;
import com.steto.jaurmon.monitor.cmd.MonCmdReadStatus;
import com.steto.jaurmon.monitor.cmd.MonReqLoadInvSettings;
import com.steto.jaurmon.monitor.cmd.MonReqSaveInvSettings;
import com.steto.jaurmon.monitor.pvoutput.PvOutputNew;
import com.steto.jaurmon.monitor.webserver.AuroraWebServer;
import com.steto.jaurmon.utils.MyUtils;
import jssc.SerialPortException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import static com.steto.jaurlib.response.ResponseErrorEnum.*;


public class AuroraMonitor {


    private static final long INVERTER_QUERY_PERIOD_MS = 30000;
    private final EventBus theEventBus;
    private String pvOutputDataDirectoryPath;
    protected HwSettings hwSettings;
    protected MonitorSettings settings;
    public int pvOutputHttpRequestTimeout = 10000;
    TelemetriesQueue telemetriesQueue = new TelemetriesQueue(2);


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
    private boolean fixEnergyCalcution = false;

    public AuroraMonitor(EventBus aEventBus, AuroraDriver auroraDriver, String configFile, String dataLogDirPath) throws Exception {

        theEventBus = aEventBus;
        this.auroraDriver = auroraDriver;

        this.configurationFileName = configFile;
        this.pvOutputDataDirectoryPath = dataLogDirPath;

        lastCheckDate = new Date();

        hwSettings = loadHwSettings();
        settings = loadSettings();


        hwSettings = hwSettings == null ? new HwSettings() : hwSettings;
        settings = settings == null ? new MonitorSettings() : settings;

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

        auroraDriver.setSerialPort(serialPortName, serialPortBaudRate);
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


    protected HwSettings loadHwSettings() throws Exception {

        HwSettings result = new HwSettings();

        try {
            HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration(configurationFileName);
            SubnodeConfiguration inverterParams = iniConfObj.getSection("inverter");

            result.inverterAddress = inverterParams.getInt("inverterAddress");
            result.serialPortBaudRate = inverterParams.getInt("serialPortBaudRate");
            result.serialPort = inverterParams.getString("serialPort");
        } catch (Exception e) {
            String errMsg = "Error reading file: " + configurationFileName + ", " + e.getMessage();
//            log.severe("Error reading file: " + configurationFileName + ", " + e.getMessage());
            throw new Exception(errMsg);
        }

        return result;
    }

    protected MonitorSettings loadSettings() throws Exception {

        MonitorSettings result = new MonitorSettings();

        try {
            HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration(configurationFileName);
            SubnodeConfiguration inverterParams = iniConfObj.getSection("monitor");

            result.inverterInterrogationPeriodSec = inverterParams.getFloat("inverterInterrogationPeriodSec");
        } catch (Exception e) {
            String errMsg = "Error reading file: " + configurationFileName + ", " + e.getMessage();
//            log.severe("Error reading file: " + configurationFileName + ", " + e.getMessage());
            throw new Exception(errMsg);
        }

        return result;
    }


    public void saveHwSettingsConfiguration() throws IOException, ConfigurationException {


        HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration(configurationFileName);
        iniConfObj.setProperty("inverter.inverterAddress", hwSettings.inverterAddress);
        iniConfObj.setProperty("inverter.serialPortBaudRate", hwSettings.serialPortBaudRate);
        iniConfObj.setProperty("inverter.serialPort", hwSettings.serialPort);

        iniConfObj.save();


    }

    public void saveConfiguration() throws IOException, ConfigurationException {

        HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration(configurationFileName);
        iniConfObj.setProperty("monitor.inverterInterrogationPeriodSec", settings.inverterInterrogationPeriodSec);

        iniConfObj.save();


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
        badResult.setErrorCode(UNKNOWN);

        AuroraResponse result;
        try {
            result = auroraDriver.acquireVersionId(hwSettings.inverterAddress);
        } catch (Exception e) {
            result = badResult;
        }

        result = result == null ? badResult : result;

        log.fine("Check Status Result: " + result.getErrorCode());
        updateInverterStatus(result.getErrorCode());


    }

    private void updateInverterStatus(ResponseErrorEnum acquisitionOutcome) {

        boolean correct = (acquisitionOutcome == NONE);
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

    public void setInverterInterrogationPeriod(float inverterQueryPeriodSec) {
        settings.inverterInterrogationPeriodSec = inverterQueryPeriodSec;
    }

    public void setDailyCumulatedEnergyFixing(boolean value) {
        fixEnergyCalcution = value;
    }

    public float getInverterInterrogationPeriod() {
        return settings.inverterInterrogationPeriodSec;
    }


    public float acquireInverterMeasure(String cmdCode, String cmdOpCode) throws InverterTimeoutException, InverterCRCException {

        float measure = 0;
        EBInverterRequest ebInverterRequest = new EBInverterRequest(cmdCode, cmdOpCode, hwSettings.inverterAddress);
        theEventBus.post(ebInverterRequest);
        if (ebInverterRequest.getResponse() instanceof EBResponseOK) {
            EBResponseOK ebResponse = (EBResponseOK) ebInverterRequest.getResponse();
            measure = Float.parseFloat((String) ebResponse.data);
            return measure;
        } else {
            EBResponseNOK ebResponseNOK = (EBResponseNOK) ebInverterRequest.getResponse();
            ResponseErrorEnum error = fromCode(ebResponseNOK.error.code);

            throw new InverterCRCException();


        }

    }

    public PeriodicInverterTelemetries acquireDataToBePublished() throws InverterCRCException, InverterTimeoutException {

        PeriodicInverterTelemetries result = new PeriodicInverterTelemetries();
        log.info("Starting data acquisition from inverter");
        result.cumulatedEnergy = acquireInverterMeasure("cumEnergy", "daily");

        result.gridPowerAll = acquireInverterMeasure("dspData", "gridPowerAll");

        result.gridVoltageAll = acquireInverterMeasure("dspData", "gridVoltageAll");

        result.inverterTemp = acquireInverterMeasure("dspData", "inverterTemp");


        log.info("data acquisition from inverter completed: " + result);


        return result;

    }


    public void start() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Date actualDate = new Date();
                        if (!MyUtils.sameDay(actualDate, lastCheckDate)) {
                            dailyCumulatedEnergy = 0;
                            log.info("It's a new day: Cumulated Energy RESET!");
                        }
                        lastCheckDate = actualDate;

                        PeriodicInverterTelemetries telemetries = acquireDataToBePublished();
                        updateInverterStatus(NONE);
                        if (fixEnergyCalcution) {
                            // fix energy calcutation when 0
                            telemetriesQueue.add(telemetries);
                            PeriodicInverterTelemetries fixedTelemetries = telemetriesQueue.fixedAverage();
                            dailyCumulatedEnergy += fixedTelemetries.cumulatedEnergy;
                            telemetries.cumulatedEnergy = dailyCumulatedEnergy;
                            log.info("Fixed Energy calculation, last one: " + fixedTelemetries.cumulatedEnergy + ", day total: " + dailyCumulatedEnergy);
                        }

                        theEventBus.post(telemetries);
                    } catch (InverterCRCException e) {
                        updateInverterStatus(TIMEOUT);
                    } catch (InverterTimeoutException e) {
                        updateInverterStatus(CRC);
                        e.printStackTrace();
                    } finally {
                        try {
                            long time2wait = (long) (settings.inverterInterrogationPeriodSec * 1000);
                            switch (inverterStatus) {
                                case ONLINE:
                                    theEventBus.post(new MonitorMsgInverterStatus(true));
                                    break;
                                case OFFLINE:
                                    theEventBus.post(new MonitorMsgInverterStatus(false));
                                    break;
                            }
                            Thread.sleep(time2wait);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        }).start();

    }

    @Subscribe

    public void handle(MonReqSaveInvSettings cmd) {

        EBResponse ebResponse = null;

        try {

            HwSettings newSettings = new Gson().fromJson(cmd.jsonParams, HwSettings.class);

            setSerialPortBaudRate(newSettings.serialPortBaudRate);
            setSerialPortName(newSettings.serialPort);
            setInverterAddress(newSettings.inverterAddress);
            init();
            saveHwSettingsConfiguration();

            ebResponse = new EBResponseOK(hwSettings);


        } catch (Exception e) {
            String errorString = "Could not execute Request: " + cmd.getClass().getCanonicalName() + ", " + e.getMessage();
            ebResponse = new EBResponseNOK(-1, errorString);
        }

        cmd.response = ebResponse;

    }

    @Subscribe

    public void handle(MonReqLoadInvSettings cmd) {

        EBResponse ebResponse = null;

        try {


            ebResponse = new EBResponseOK(hwSettings);


        } catch (Exception e) {
            String errorString = "Could not execute Request: " + cmd.getClass().getCanonicalName() + ", " + e.getMessage();
            ebResponse = new EBResponseNOK(-1, errorString);
        }

        cmd.response = ebResponse;

    }

    @Subscribe
    public void execCommand(MonCmdReadStatus cmd) {
        EBResponse ebResponse = null;

        try {
            checkInverterStatus();
            String inverterStatus = isInverterOnline() ? "online" : "offline";
            ebResponse = new EBResponseOK(inverterStatus);

        } catch (Exception ex) {
            cmd.response = new EBResponseNOK(-1, ex.getMessage());

        }
        cmd.response = ebResponse;

    }

    public static void main(String[] args) throws Exception {

        Logger log = Logger.getLogger("mainLogger");
        String configurationFileName = "aurora.cfg";
        String logDirectoryPath = "log";
        String workingDirectory = ".";
        String webDirectory = "html/";
        if (args.length > 0) {
            workingDirectory = args[0];
            webDirectory = args[1];
        }

        configurationFileName = workingDirectory + File.separator + "config" + File.separator + configurationFileName;
        logDirectoryPath = workingDirectory + File.separator + logDirectoryPath;


        try {
//        String serialPort = "/dev/ttys002";
            String webDirectoryPath = workingDirectory + File.separator + webDirectory;
//        String serialPort = "/dev/ttys001";

            log.info("Creating Aurora Driver...");
            AuroraDriver auroraDriver = new AuroraDriver(null, new AuroraRequestFactory(), new AuroraResponseFactory());


            log.info("Creating Aurora Monitor...");
            EventBus theEventBus = new EventBus();
            AuroraMonitor auroraMonitor = new AuroraMonitor(theEventBus, auroraDriver, configurationFileName, logDirectoryPath);
            EventBusInverterAdapter eventBusInverterAdapter = new EventBusInverterAdapter(theEventBus, auroraDriver, new InverterCommandFactory());
            auroraMonitor.init();
            auroraMonitor.start();
            PvOutputNew pvOutput = new PvOutputNew(configurationFileName, theEventBus);
            pvOutput.start();

            log.info("Creating Web Server...");
            AuroraWebServer auroraWebServer = new AuroraWebServer(8080, webDirectoryPath, theEventBus);
            log.info("Starting Web Server...");
            new Thread(auroraWebServer).start();
            Thread.sleep(1000);
        } catch (Exception ex) {
            System.out.println("Error at startup: " + ex.getMessage());
            log.severe("Fatal error at startup: " + ex.getMessage());
        }

    }

}


