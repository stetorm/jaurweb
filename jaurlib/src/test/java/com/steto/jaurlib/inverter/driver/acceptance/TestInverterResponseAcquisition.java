package com.steto.jaurlib.inverter.driver.acceptance;

import com.steto.jaurinv.AuroraInverter;
import com.steto.jaurinv.AuroraVersionData;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.request.AuroraCumEnergyEnum;
import com.steto.jaurlib.request.AuroraDspRequestEnum;
import com.steto.jaurlib.request.AuroraRequestFactory;
import com.steto.jaurlib.response.*;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.junit.*;

import java.text.ParseException;
import java.util.Calendar;

import static org.junit.Assert.*;

/**
 * Created by sbrega on 17/11/2014.
 */
public class TestInverterResponseAcquisition {


    static final String serialPort1 = "/dev/ttys005";
    static final String serialPort2 = "/dev/ttys006";
//      static final String serialPort1 = "COM14";
//      static final String serialPort2 = "COM24";
    static AuroraVersionData auroraVersionData;
    static AuroraInverter auroraInverter;
    AuroraDriver auroraDriver;

    @BeforeClass
    public static void bef() throws ParseException {
        auroraVersionData = new AuroraVersionData(AI_ModelsEnum.PVI_2000, AI_NationEnum.Italy_ENEL_DK_5950, AI_TransformerType.Transformer_Version, AI_Type.Photovoltaic_Version);
        auroraInverter = new AuroraInverter(1, serialPort2, new AuroraResponseFactory(), new AuroraRequestFactory(), auroraVersionData);
        new Thread(auroraInverter).start();
    }

    @Before
    public void before() throws InterruptedException, ParseException, SerialPortException {

        SerialPort serialPort = new SerialPort(serialPort1);
        serialPort.openPort();//Open serial port
        serialPort.setParams(19200, 8, 1, 0);//Set params.

        auroraDriver = new AuroraDriver(serialPort, new AuroraRequestFactory(), new AuroraResponseFactory());


    }

    @After
    public void after() throws InterruptedException, ParseException, SerialPortException {

        auroraDriver.stop();
        Thread.sleep(1000);

    }

    @Test
    public void shouldSendVersionInfo() throws Exception {

        // exercise
        AuroraResponse response = auroraDriver.acquireVersionId(1);

        AResp_VersionId expectedMsg = new AResp_VersionId();
        expectedMsg.setParam1(auroraVersionData.modelName.get());
        expectedMsg.setParam2(auroraVersionData.nation.get());
        expectedMsg.setParam3(auroraVersionData.transformerType.get());
        expectedMsg.setParam4(auroraVersionData.type.get());


        assertArrayEquals(expectedMsg.toByteArray(), response.toByteArray());


    }

    @Test
    public void shouldSendCumulatedEnergy() throws Exception {

        //setup
        AResp_CumulatedEnergy expectedMsg = new AResp_CumulatedEnergy();
        expectedMsg.setType(AuroraCumEnergyEnum.PARTIAL);

        // exercise
        AResp_CumulatedEnergy response = (AResp_CumulatedEnergy) auroraDriver.acquireCumulatedEnergy(1, AuroraCumEnergyEnum.PARTIAL);

        // verify
        Assert.assertEquals((long) response.get(), auroraInverter.cumulatedEnergyPartial);

        // exercise
        response = (AResp_CumulatedEnergy) auroraDriver.acquireCumulatedEnergy(1, AuroraCumEnergyEnum.TOTAL);

        // verify
        expectedMsg = new AResp_CumulatedEnergy();
        expectedMsg.setType(AuroraCumEnergyEnum.TOTAL);
        Assert.assertEquals((long) response.get(), auroraInverter.cumulatedEnergyTotal);

        // exercise
        response = (AResp_CumulatedEnergy) auroraDriver.acquireCumulatedEnergy(1, AuroraCumEnergyEnum.DAILY);

        // verify
        expectedMsg = new AResp_CumulatedEnergy();
        expectedMsg.setType(AuroraCumEnergyEnum.DAILY);
        Assert.assertEquals((long) response.get(), auroraInverter.cumulatedEnergyDaily);

        // exercise
        response = (AResp_CumulatedEnergy) auroraDriver.acquireCumulatedEnergy(1, AuroraCumEnergyEnum.WEEKLY);

        // verify
        expectedMsg = new AResp_CumulatedEnergy();
        expectedMsg.setType(AuroraCumEnergyEnum.WEEKLY);
        Assert.assertEquals((long) response.get(), auroraInverter.cumulatedEnergyWeekly);

        // exercise
        response = (AResp_CumulatedEnergy) auroraDriver.acquireCumulatedEnergy(1, AuroraCumEnergyEnum.MONTHLY);

        // verify
        expectedMsg = new AResp_CumulatedEnergy();
        expectedMsg.setType(AuroraCumEnergyEnum.MONTHLY);
        Assert.assertEquals((long) response.get(), auroraInverter.cumulatedEnergyMonthly);

        // exercise
        response = (AResp_CumulatedEnergy) auroraDriver.acquireCumulatedEnergy(1, AuroraCumEnergyEnum.YEARLY);

        // verify
        expectedMsg = new AResp_CumulatedEnergy();
        expectedMsg.setType(AuroraCumEnergyEnum.YEARLY);
        Assert.assertEquals((long) response.get(), auroraInverter.cumulatedEnergyYearly);


    }

    @Test
    public void shouldAcquireDSPData() throws Exception {


        //exercise
        AuroraResponse response = auroraDriver.acquireDspValue(1, AuroraDspRequestEnum.GRID_VOLTAGE_ALL);
        //verify
        assertTrue(Math.abs(response.getFloatParam() - auroraInverter.dspGridVoltageAll) < 0.000001);

        //exercise
        response = auroraDriver.acquireDspValue(1, AuroraDspRequestEnum.GRID_CURRENT_ALL);
        //verify
        assertTrue(Math.abs(response.getFloatParam() - auroraInverter.dspGridCurrentAll) < 0.000001);

        //exercise
        response = auroraDriver.acquireDspValue(1, AuroraDspRequestEnum.GRID_POWER_ALL);
        //verify
        assertTrue(Math.abs(response.getFloatParam() - auroraInverter.dspGridPowerAll) < 0.000001);

        //exercise
        response = auroraDriver.acquireDspValue(1, AuroraDspRequestEnum.INPUT_1_VOLTAGE);
        //verify
        assertTrue(Math.abs(response.getFloatParam() - auroraInverter.dspInput1Voltage) < 0.000001);

        //exercise
        response = auroraDriver.acquireDspValue(1, AuroraDspRequestEnum.INPUT_1_CURRENT);
        //verify
        assertTrue(Math.abs(response.getFloatParam() - auroraInverter.dspInput1Current) < 0.000001);

        //exercise
        response = auroraDriver.acquireDspValue(1, AuroraDspRequestEnum.INPUT_2_VOLTAGE);
        //verify
        assertTrue(Math.abs(response.getFloatParam() - auroraInverter.dspInput2Voltage) < 0.000001);

        //exercise
        response = auroraDriver.acquireDspValue(1, AuroraDspRequestEnum.INPUT_2_CURRENT);
        //verify
        assertTrue(Math.abs(response.getFloatParam() - auroraInverter.dspInput2Current) < 0.000001);

        //exercise
        response = auroraDriver.acquireDspValue(1, AuroraDspRequestEnum.BOOSTER_TEMPERATURE_GRID_TIED);
        //verify
        assertTrue(Math.abs(response.getFloatParam() - auroraInverter.dspBoosterTempGridTied) < 0.000001);

        //exercise
        response = auroraDriver.acquireDspValue(1, AuroraDspRequestEnum.INVERTER_TEMPERATURE_GRID_TIED);
        //verify
        assertTrue(Math.abs(response.getFloatParam() - auroraInverter.dspInverterTempGridTied) < 0.000001);

        //exercise
        response = auroraDriver.acquireDspValue(1, AuroraDspRequestEnum.FREQUENCY_ALL);
        //verify
        assertTrue(Math.abs(response.getFloatParam() - auroraInverter.dspFrequencyAll) < 0.000001);


    }

    @Test
    public void shouldAcquireState() throws Exception {


        // exercise
        AResp_State response = (AResp_State) auroraDriver.acquireState(1);

        // verify
        AResp_State expectedMsg = new AResp_State();
        expectedMsg.setAlarmState((char) auroraInverter.alarmState);
        expectedMsg.setGlobalState((char) auroraInverter.globalState);
        expectedMsg.setCh1DcDcState((char) auroraInverter.channel1DcDcState);
        expectedMsg.setCh2DcDcState((char) auroraInverter.channel2DcDcState);
        expectedMsg.setInverterState((char) auroraInverter.inverterState);

        Assert.assertEquals(auroraInverter.alarmState, response.getAlarmState());
        Assert.assertEquals(auroraInverter.globalState, response.getGlobalState());
        Assert.assertEquals(auroraInverter.channel1DcDcState, response.getCh1DcDcState());
        Assert.assertEquals(auroraInverter.channel2DcDcState, response.getCh2DcDcState());
        Assert.assertEquals(auroraInverter.inverterState, response.getInverterState());


    }

    @Test
    public void shouldSendFirmwareRequest() throws Exception {


        // exercise
        AResp_FwVersion response = (AResp_FwVersion) auroraDriver.acquireFirmwareVersion(1);

        // verify

        Assert.assertEquals(auroraInverter.firmwareVersion, response.get().replace(".", ""));

    }

    @Test
    public void shouldSendManufacturingDate() throws Exception {


        // exercise
        AResp_MFGdate response = (AResp_MFGdate) auroraDriver.acquireMFGdate(1);

        // verify

        Calendar expectedCal= Calendar.getInstance();
        expectedCal.setTime(auroraInverter.manufactoringDate);
        Calendar responseCal= Calendar.getInstance();
        responseCal.setTime(response.get());
        assertEquals(expectedCal.get(Calendar.YEAR), responseCal.get(Calendar.YEAR));
        assertEquals(expectedCal.get(Calendar.WEEK_OF_YEAR), responseCal.get(Calendar.WEEK_OF_YEAR));

    }

    @Test
    public void shouldSendSystemConfiguration() throws Exception {


        // exercise
        AResp_SysConfig response = (AResp_SysConfig) auroraDriver.acquireSystemConfig(1);

        // verify

        Assert.assertEquals(response.getConfigCode(), auroraInverter.systemConfiguration);

    }

    @Test
    public void shouldSendSerialNumber() throws Exception {


        // exercise
        AResp_SerialNumber response = (AResp_SerialNumber) auroraDriver.acquireSerialNumber(2);

        // verify


        Assert.assertEquals(auroraInverter.serialNumber, response.get());

    }

    @Test
    public void shouldSendProductNumberRequest() throws Exception {


        // exercise
        AResp_ProductNumber response = (AResp_ProductNumber) auroraDriver.acquireProductNumber(1);

        // verify

        Assert.assertEquals(response.get(), auroraInverter.productNumber);

    }

    @Test
    public void shouldSendTimeCounter() throws Exception {


        // exercise
        AResp_TimeCounter response = (AResp_TimeCounter) auroraDriver.acquireTimeCounter(1);

        // verify

        Assert.assertEquals(auroraInverter.timeCounter, response.get().intValue());

    }

    @Test
    public void shouldSendActualTime() throws Exception {


        // exercise
        AResp_ActualTime response = (AResp_ActualTime) auroraDriver.acquireActualTime(1);

        // verify
        Thread.sleep(1000);

        System.out.println(auroraInverter.time);
        Assert.assertEquals(auroraInverter.time, (long) response.get());

    }

}
