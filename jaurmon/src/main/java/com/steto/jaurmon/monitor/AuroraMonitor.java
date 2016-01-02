package com.steto.jaurmon.monitor;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.cmd.InverterCommandFactory;
import com.steto.jaurlib.eventbus.EBResponseNOK;
import com.steto.jaurlib.eventbus.EventBusAdapter;
import com.steto.jaurlib.request.AuroraCumEnergyEnum;
import com.steto.jaurlib.request.AuroraDspRequestEnum;
import com.steto.jaurlib.request.AuroraRequestFactory;
import com.steto.jaurlib.response.*;
import com.steto.jaurmon.monitor.cmd.*;
import com.steto.jaurmon.monitor.pvoutput.PVOutputParams;
import com.steto.jaurmon.monitor.webserver.AuroraWebServer;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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
        log.info("Serial Port: "+newSerialPort.getPortName()+" initialized with values: " + serialPortName + ", " + serialPortBaudRate);
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

    protected HwSettings loadHwSettings()  {

        HwSettings result = new HwSettings();

        try {
            HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration(configurationFileName);
            SubnodeConfiguration inverterParams = iniConfObj.getSection("inverter");

            result.inverterAddress = inverterParams.getInt("address");
            result.serialPortBaudRate = inverterParams.getInt("serialPortBaudRate");
            result.serialPort = inverterParams.getString("serialPort");
        } catch (Exception e) {
            log.severe("Error reading file: " + configurationFileName + ", " + e.getMessage());
            result = null;
        }

        return result;
    }


    public void saveHwSettingsConfiguration() throws IOException, ConfigurationException {



        HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration(configurationFileName);
        iniConfObj.setProperty("inverter.address", hwSettings.inverterAddress);
        iniConfObj.setProperty("inverter.serialPortBaudRate", hwSettings.serialPortBaudRate);
        iniConfObj.setProperty("inverter.serialPort", hwSettings.serialPort);

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
            cmd.response = new EBResponseNOK(-1, ex.getMessage());

        }
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

        configurationFileName =  workingDirectory + File.separator + "config" + File.separator + configurationFileName;
        logDirectoryPath =  workingDirectory + File.separator + logDirectoryPath;


        try {
//        String serialPort = "/dev/ttys002";
            String webDirectoryPath = workingDirectory + File.separator + webDirectory;
//        String serialPort = "/dev/ttys001";

            log.info("Creating Aurora Driver...");
            AuroraDriver auroraDriver = new AuroraDriver(null, new AuroraRequestFactory(), new AuroraResponseFactory());


            log.info("Creating Aurora Monitor...");
            EventBus theEventBus = new EventBus();
            AuroraMonitor auroraMonitor = new AuroraMonitor(theEventBus,auroraDriver, configurationFileName, logDirectoryPath);
            EventBusAdapter eventBusAdapter = new EventBusAdapter(theEventBus,auroraDriver, new InverterCommandFactory());
            auroraMonitor.init();

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


