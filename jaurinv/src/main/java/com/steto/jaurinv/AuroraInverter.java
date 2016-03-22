package com.steto.jaurinv;

import com.steto.jaurlib.request.*;
import com.steto.jaurlib.response.*;
import jssc.SerialPort;
import jssc.SerialPortException;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.steto.jaurlib.request.AuroraDspRequestEnum.*;


/**
 * Created by sbrega on 17/11/2014.
 */
public class AuroraInverter implements Runnable, AuroraResponseBuilder {

    Logger log = Logger.getLogger(getClass().getSimpleName());
    private final AuroraRequestFactory auroraRequestFactory;
    private final AuroraVersionData auroraVersionData;
    final SerialPort serialPort;
    final String comName;
    final int address;
    final AuroraResponseFactory auroraResponseFactory;
    private char modelId;
    public float dspGridCurrentAll;
    public float dspGridPowerAll;
    public float dspGridVoltageAll;
    public float dspFrequencyAll;
    public float dspInput1Voltage;
    public float dspInput2Voltage;
    public float dspInput1Current;
    public float dspInput2Current;
    public float dspInverterTempGridTied;
    public float dspBoosterTempGridTied;
    public float dspDummyValue = 333;
    public byte alarmState;
    public byte globalState;
    public byte channel1DcDcState;
    public byte channel2DcDcState;
    public byte inverterState;
    public String firmwareVersion;
    public final Date manufactoringDate;
    public String serialNumber;
    public String productNumber;
    public int systemConfiguration;
    public int timeCounter;
    public long TIMEORIGIN;
    public long cumulatedLast7Days;
    public long cumulatedEnergyDaily;
    public long cumulatedEnergyWeekly;
    public long cumulatedEnergyMonthly;
    public long cumulatedEnergyYearly;
    public long cumulatedEnergyPartial;
    public long cumulatedEnergyTotal;
    public int time = -1;

    public int[] lastAlarms= new int[]{0,1,2,3};
    private boolean running = false;


    public AuroraInverter(int address, String com, AuroraResponseFactory auroraResponseFactory, AuroraRequestFactory auroraRequestFactory, AuroraVersionData auroraVersionData) throws ParseException {

        comName = com;
        serialPort = new SerialPort(com);
        this.address = address;

        this.auroraResponseFactory = auroraResponseFactory;
        this.auroraRequestFactory = auroraRequestFactory;
        this.auroraVersionData = auroraVersionData;

        dspGridCurrentAll = 22;
        dspGridVoltageAll = 580;
        dspFrequencyAll = 49;
        dspInput1Voltage = 247;
        dspInput2Voltage = 230;
        dspInput1Current = 5;
        dspInput2Current = 6;
        dspInverterTempGridTied = 47;
        dspBoosterTempGridTied = 35;
        dspGridPowerAll = dspInput1Current * dspInput1Voltage + dspInput2Current + dspInput2Voltage;

        alarmState = 0;
        globalState = 6; // run
        channel1DcDcState = 2;
        channel2DcDcState = 2;
        inverterState = 2;

        serialNumber = "123456";
        systemConfiguration = 1;
        productNumber = "65432";
        timeCounter = (int) Math.round(new Date().getTime() / 1000.0);

        cumulatedEnergyDaily = 0; //wh
        // cumulatedEnergyDaily = 896; //wh
        cumulatedEnergyWeekly = 5210; //wh
        cumulatedEnergyMonthly = 170000; //wh
        cumulatedEnergyYearly = 1999000;  //wh
        cumulatedEnergyPartial = 525000; //wh
        cumulatedEnergyTotal = 7947000; //wh
        cumulatedLast7Days = cumulatedEnergyWeekly + 1;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        TIMEORIGIN = sdf.parse("01/01/2000").getTime();

        manufactoringDate = sdf.parse("01/04/2010");

        firmwareVersion = "1927";

    }

    public void setModelId(char serialNumber) {
        this.modelId = serialNumber;
    }

    @Override
    public void run() {

        try {
            running = true;
            serialPort.openPort();//Open serial port
            serialPort.setParams(19200, 8, 1, 0);//Set params.
            serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
            log.info("Serial Port: "+serialPort.getPortName()+" opened successfully");
            log.info("Running and waiting for a request");
            while (running) {

                AuroraRequest request = null;
                try {
                    request = getRequest();
                    if (request != null) {
                        log.info("Received request: " + request);
                        sendResponse(request);
                    } else {
                        log.severe("Error: received invalid request ");
                    }
                } catch (Exception e) {
                    log.severe("Error: " + e.getMessage() + " trying to reply to: " + request);
                    serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
                }
            }
        } catch (SerialPortException e) {
            log.severe("Fatal Error: " + e.getMessage());
        }


    }

    private void sendResponse(AuroraRequest auroraRequest) throws SerialPortException, IllegalAccessException, InstantiationException {
        AuroraResponse auroraResponse = null;


        auroraResponse = auroraRequest.create(this);

        if (auroraResponse != null) {
            sendMsgOnBus(auroraResponse);
            log.info("Sent msg: " + auroraResponse + " as response to msg: " + auroraRequest);
        } else {
            log.info("No msg sent: as response to msg: " + auroraRequest);
        }

    }


    private void sendMsgOnBus(AuroraResponse inverterMsg) throws SerialPortException {
        AuroraResponsePacket pkt = new AuroraResponsePacket(inverterMsg);
        serialPort.writeBytes(pkt.toByteArray());
    }


    private AuroraRequest getRequest() throws Exception {
        byte[] bytes = serialPort.readBytes(10);
        log.info("Received bytes: " + printByteArray(bytes));
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        AuroraRequestPacket pkt = new AuroraRequestPacket();
        pkt.read(byteArrayInputStream);
        return (AuroraRequest) pkt.getPdu();
    }

    private String printByteArray(byte[] bytes) {
        String result = "";
        for (byte b : bytes) {
            result += String.format("%02X ", b);

        }
        return result;
    }


    public AuroraResponseFactory getResponseFactory() {
        return auroraResponseFactory;
    }

    public AuroraRequestFactory getRequestFactory() {
        return auroraRequestFactory;
    }


    @Override
    public AuroraResponse createResponse(AReq_VersionId auroraRequest) {

        AuroraResponse result = new AResp_VersionId();
        result.setParam1(auroraVersionData.modelName.get());
        result.setParam2(auroraVersionData.nation.get());
        result.setParam3(auroraVersionData.transformerType.get());
        result.setParam4(auroraVersionData.type.get());


        return result;
    }

    @Override
    public AuroraResponse createResponse(AReq_State auroraRequest) {

        AResp_State result = new AResp_State();
        result.setAlarmState((char) alarmState);
        result.setGlobalState((char) globalState);
        result.setCh1DcDcState((char) channel1DcDcState);
        result.setCh2DcDcState((char) channel2DcDcState);
        result.setInverterState((char) inverterState);

        return result;
    }

    @Override
    public AuroraResponse createResponse(AReq_FwVersion auroraRequest) {
        AResp_FwVersion result = new AResp_FwVersion();
        result.set(firmwareVersion);

        return result;

    }

    @Override
    public AuroraResponse createResponse(AReq_MFGdate auroraRequest) {
        AResp_MFGdate result = new AResp_MFGdate();
        result.setDate(manufactoringDate);

        return result;
    }

    @Override
    public AuroraResponse createResponse(AReq_SerialNumber request) {
        AResp_SerialNumber result = new AResp_SerialNumber();
        result.set(this.serialNumber);
        return result;
    }

    @Override
    public AuroraResponse createResponse(AReq_SystemConfig request) {
        AResp_SysConfig result = new AResp_SysConfig();
        result.setConfigCode(this.systemConfiguration);

        return result;
    }

    @Override
    public AuroraResponse createResponse(AReq_ProductNumber request) {
        AResp_ProductNumber result = new AResp_ProductNumber();
        result.set(this.productNumber);

        return result;
    }

    @Override
    public AuroraResponse createResponse(AReq_TimeCounter request) {
        AResp_TimeCounter result = new AResp_TimeCounter();
        result.set(this.timeCounter);

        String description = "Time Counter";
        result.setDescription(description);
        return result;
    }

    @Override
    public AuroraResponse createResponse(AReq_ActualTime request) {
        AResp_ActualTime result = new AResp_ActualTime();
        Date now = new Date();
        time = (int) ((now.getTime() - TIMEORIGIN) / 1000);
        result.set(time);
        String description = "Inverter Time";
        result.setDescription(description);

        return result;
    }

    @Override
    public AuroraResponse createResponse(AReq_CumulatedEnergy request) {
        AuroraResponse result = new AResp_CumulatedEnergy();
        Map<Integer, Long> mapCERequest2Value = new HashMap<Integer, Long>();
        mapCERequest2Value.put(AuroraCumEnergyEnum.DAILY.get(), cumulatedEnergyDaily);
        mapCERequest2Value.put(AuroraCumEnergyEnum.WEEKLY.get(), cumulatedEnergyWeekly);
        mapCERequest2Value.put(AuroraCumEnergyEnum.LAST7DAYS.get(), cumulatedEnergyWeekly);
        mapCERequest2Value.put(AuroraCumEnergyEnum.MONTHLY.get(), cumulatedEnergyMonthly);
        mapCERequest2Value.put(AuroraCumEnergyEnum.YEARLY.get(), cumulatedEnergyYearly);
        mapCERequest2Value.put(AuroraCumEnergyEnum.PARTIAL.get(), cumulatedEnergyPartial);
        mapCERequest2Value.put(AuroraCumEnergyEnum.TOTAL.get(), cumulatedEnergyTotal);


        Long val = mapCERequest2Value.get((int) request.getParam1());


        if (val == null) {
            result = null;
        } else {
            result.setLongParam(val);
        }


        return result;
    }

    @Override
    public AuroraResponse createResponse(AReq_DspData auroraRequest) {

        AuroraResponse result = new AResp_DspData();
        Map<Integer, Float> mapDspRequest2Value = new HashMap<Integer, Float>();
        mapDspRequest2Value.put(FREQUENCY_ALL.get(), dspFrequencyAll);
        mapDspRequest2Value.put(GRID_VOLTAGE_ALL.get(), dspGridVoltageAll);
        mapDspRequest2Value.put(GRID_CURRENT_ALL.get(), dspGridCurrentAll);
        mapDspRequest2Value.put(GRID_POWER_ALL.get(), dspGridPowerAll);

        mapDspRequest2Value.put(INPUT_1_VOLTAGE.get(), dspInput1Voltage);
        mapDspRequest2Value.put(INPUT_1_CURRENT.get(), dspInput1Current);
        mapDspRequest2Value.put(INPUT_2_VOLTAGE.get(), dspInput2Voltage);
        mapDspRequest2Value.put(INPUT_2_CURRENT.get(), dspInput2Current);
        mapDspRequest2Value.put(INVERTER_TEMPERATURE_GRID_TIED.get(), dspInverterTempGridTied);
        mapDspRequest2Value.put(BOOSTER_TEMPERATURE_GRID_TIED.get(), dspBoosterTempGridTied);

        mapDspRequest2Value.put(VBULK_ILEAK_DCDC.get(), dspDummyValue);
        mapDspRequest2Value.put(ILEAK_DCDC.get(), dspDummyValue);
        mapDspRequest2Value.put(ILEAK_INVERTER.get(), dspDummyValue);
        mapDspRequest2Value.put(PIN_1.get(), dspDummyValue);
        mapDspRequest2Value.put(PIN_2.get(), dspDummyValue);
        mapDspRequest2Value.put(GRID_VOLTAGE.get(), dspDummyValue);
        mapDspRequest2Value.put(GRID_FREQUENCY.get(), dspDummyValue);
        mapDspRequest2Value.put(ISOLATION_RESISTANCE_ALL.get(), dspDummyValue);
        mapDspRequest2Value.put(VBULK_GRID.get(), dspDummyValue);
        mapDspRequest2Value.put(AVERAGE_GRID_VOLTAGE.get(), dspDummyValue);
        mapDspRequest2Value.put(VBULK_MID_GRID_TIED.get(), dspDummyValue);
        mapDspRequest2Value.put(POWER_PEAK_ALL.get(), dspDummyValue);
        mapDspRequest2Value.put(POWER_PEAK_TODAY_AL.get(), dspDummyValue);
        mapDspRequest2Value.put(GRID_VOLTAGE_NEUTRAL_GRID_TIED.get(), dspDummyValue);
        mapDspRequest2Value.put(WIND_GENERATOR_FREQUENCY.get(), dspDummyValue);
        mapDspRequest2Value.put(GRID_VOLTAGE_NEUTRAL_PHASE_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(GRID_CURRENT_PHASE_R_CENTRAL_AND_3_PHASE.get(), dspDummyValue);
        mapDspRequest2Value.put(GRID_CURRENT_PHASE_S_CENTRAL_AND_3_PHASE.get(), dspDummyValue);
        mapDspRequest2Value.put(GRID_CURRENT_PHASE_T_CENTRAL_AND_3_PHASE.get(), dspDummyValue);
        mapDspRequest2Value.put(FREQUENCY_PHASE_R_CENTRAL_AND_3_PHASE.get(), dspDummyValue);
        mapDspRequest2Value.put(FREQUENCY_PHASE_S_CENTRAL_AND_3_PHASE.get(), dspDummyValue);
        mapDspRequest2Value.put(FREQUENCY_PHASE_T_CENTRAL_AND_3_PHASE.get(), dspDummyValue);
        mapDspRequest2Value.put(VBULK_PLUS_CENTRAL_AND_3_PHASE.get(), dspDummyValue);
        mapDspRequest2Value.put(VBULK_MINUS_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(SUPERVISOR_TEMPERATURE_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(ALIM_TEMPERATURE_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(HEAK_SINK_TEMPERATURE_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(TEMPERATURE_1_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(TEMPERATURE_2_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(TEMPERATURE_3_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(FAN_1_SPEED_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(FAN_2_SPEED_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(FAN_3_SPEED_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(FAN_4_SPEED_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(FAN_5_SPEED_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(POWER_SATURATION_LIMIT_DER_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(REFERENCE_RING_BULK_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(VPANEL_MICRO_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(GRID_VOLTAGE_PHASE_R_CENTRAL_AND_3_PHASE.get(), dspDummyValue);
        mapDspRequest2Value.put(GRID_VOLTAGE_PHASE_S_CENTRAL_AND_3_PHASE.get(), dspDummyValue);
        mapDspRequest2Value.put(GRID_VOLTAGE_PHASE_T_CENTRAL_AND_3_PHASE.get(), dspDummyValue);
        mapDspRequest2Value.put(FAN_1_SPEED_RPM_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(FAN_2_SPEED_RPM_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(FAN_3_SPEED_RPM_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(FAN_4_SPEED_RPM_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(FAN_5_SPEED_RPM_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(FAN_6_SPEED_RPM_CENTRAL.get(), dspDummyValue);
        mapDspRequest2Value.put(FAN_7_SPEED_RPM_CENTRAL.get(), dspDummyValue);


        Float val = mapDspRequest2Value.get((int) auroraRequest.getParam1());


        if (val == null) {
            result = null;
        } else {
            result.setFloatParam(val);
        }


        return result;
    }

    @Override
    public AuroraResponse createResponse(AReq_LastAlarms request) {
        AResp_LastAlarms result = new AResp_LastAlarms();
        result.setAlarm1(lastAlarms[0]);
        result.setAlarm2(lastAlarms[1]);
        result.setAlarm3(lastAlarms[2]);
        result.setAlarm4(lastAlarms[3]);
        String description = "Last Alarms";
        result.setDescription(description);

        return result;
    }


    public static void main(String[] args) throws InterruptedException, ParseException {

        String serialPort = "COM14";
//        String serialPort = "/dev/ttys001";
        if (args.length > 0) {
            serialPort = args[0];
        }
        AuroraVersionData auroraVersionData = new AuroraVersionData(AI_ModelsEnum.PVI_2000, AI_NationEnum.Italy_ENEL_DK_5950, AI_TransformerType.Transformer_Version, AI_Type.Photovoltaic_Version);
        AuroraInverter auroraInverter = new AuroraInverter(1, serialPort, new AuroraResponseFactory(), new AuroraRequestFactory(), auroraVersionData);


        String serial = "PVI2000";
        byte b = '1';
        auroraInverter.setModelId('1');
        new Thread(auroraInverter).start();
        Thread.sleep(10000);
    }


    public void stop() throws SerialPortException {
        running = false;
        serialPort.closePort();
    }
}


