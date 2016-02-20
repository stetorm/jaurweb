package com.steto.jaurmon.monitor.core.integration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.cmd.InverterCommandFactory;
import com.steto.jaurlib.eventbus.EventBusInverterAdapter;
import com.steto.jaurlib.request.AuroraCumEnergyEnum;
import com.steto.jaurlib.request.AuroraDspRequestEnum;
import com.steto.jaurlib.response.AResp_CumulatedEnergy;
import com.steto.jaurlib.response.AResp_DspData;
import com.steto.jaurlib.response.ResponseErrorEnum;
import com.steto.jaurmon.monitor.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Date;

import static com.steto.jaurmon.monitor.RandomObjectGenerator.getA_HwSettings;
import static com.steto.jaurmon.monitor.RandomObjectGenerator.getA_MonitorSettings;
import static com.steto.jaurmon.monitor.TestUtility.createAuroraConfigFile;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by stefano on 17/01/16.
 */

public class TestInverterDataAcquisitionEngine {


    class TelemetriesReceiver {

        private final Object waitVar;
        private final int cardinality;
        public PeriodicInverterTelemetries telemetries;
        public int counter = 0;
        long deltaTmsec = 0;

        public TelemetriesReceiver(int cardinality, Object waitVar) {
            this.waitVar = waitVar;
            this.cardinality = cardinality;
        }


        @Subscribe
        public void handle(PeriodicInverterTelemetries msg) {
            counter++;
            if (counter == 1) {
                deltaTmsec = new Date().getTime();
            } else if (counter == 2) {
                deltaTmsec = new Date().getTime() - deltaTmsec;
            }
            telemetries = msg;
            synchronized (waitVar) {
                if (counter >= cardinality)
                    waitVar.notifyAll();
            }
        }

    }

    ;

    class MonitorMsgInverterStatusReceiver {

        public MonitorMsgInverterStatus monitorStatusMsg;
        public int counter = 0;
        long deltaTmsec = 0;

        public MonitorMsgInverterStatusReceiver() {
        }


        @Subscribe
        public void handle(MonitorMsgInverterStatus msg) {
            counter++;
            monitorStatusMsg = msg;
        }

    }


    File resourcesDirectory = new File("src/test/resources");

    String configFile = resourcesDirectory.getAbsolutePath() + File.separator + "aurora.cfg";
    AuroraMonitor auroraMonitor = null;
    String pvOutDirPath;
    EventBus theEventBus = new EventBus();
    AuroraDriver auroraDriver = mock(AuroraDriver.class);
    EventBusInverterAdapter eventBusInverterAdapter = new EventBusInverterAdapter(theEventBus, auroraDriver, new InverterCommandFactory());

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private HwSettings hwSettings;
    private MonitorSettings monitorSettings;


    @Before
    public void before() throws Exception {

        hwSettings = getA_HwSettings();
        monitorSettings = getA_MonitorSettings();
        createAuroraConfigFile(configFile, hwSettings, monitorSettings);

        pvOutDirPath = tempFolder.newFolder().getAbsolutePath();
        auroraMonitor = new AuroraMonitor(theEventBus, auroraDriver, configFile, pvOutDirPath);
        Thread.sleep(200);

    }

    @After
    public void after() throws Exception {

        tempFolder.delete();
        auroraMonitor.stop();
        Thread.sleep(200);

    }

    @Test
    public void shouldProvideInverterMeasure() throws Exception {


        final Object waitVar = new Object();

        int NUM_OF_TELEMETRIES = 3;

        float gridPowerAll = (float) 321.3;
        float gridVoltageAll = (float) 221.5;
        float inverterTemperature = (float) 27.4;
        Float cumulatedEnergy = (float) 1367.0;
        float inverterInterrPeriod = (float) 0.5;


        AResp_CumulatedEnergy cumulateEnergyResponse = new AResp_CumulatedEnergy();
        AResp_DspData responseGridPowerAll = new AResp_DspData();
        AResp_DspData responseGridVoltageAll = new AResp_DspData();
        AResp_DspData respInverterTemperature = new AResp_DspData();

        cumulateEnergyResponse.setLongParam(cumulatedEnergy.longValue());
        responseGridPowerAll.setFloatParam(gridPowerAll);
        responseGridVoltageAll.setFloatParam(gridVoltageAll);
        respInverterTemperature.setFloatParam(inverterTemperature);

        when(auroraDriver.acquireCumulatedEnergy(eq(hwSettings.inverterAddress), eq(AuroraCumEnergyEnum.DAILY))).thenReturn(cumulateEnergyResponse);
        when(auroraDriver.acquireDspValue(eq(hwSettings.inverterAddress), eq(AuroraDspRequestEnum.GRID_POWER_ALL))).thenReturn(responseGridPowerAll);
        when(auroraDriver.acquireDspValue(eq(hwSettings.inverterAddress), eq(AuroraDspRequestEnum.GRID_VOLTAGE_ALL))).thenReturn(responseGridVoltageAll);
        when(auroraDriver.acquireDspValue(eq(hwSettings.inverterAddress), eq(AuroraDspRequestEnum.INVERTER_TEMPERATURE_GRID_TIED))).thenReturn(respInverterTemperature);

        TelemetriesReceiver telemetriesReceiver = new TelemetriesReceiver(NUM_OF_TELEMETRIES, waitVar);
        theEventBus.register(telemetriesReceiver);

        // Exercise
        auroraMonitor.setInverterInterrogationPeriod(inverterInterrPeriod);
        auroraMonitor.start();

        synchronized (waitVar) {
            waitVar.wait(2000);
        }


        // Verify
        assertEquals(NUM_OF_TELEMETRIES, telemetriesReceiver.counter);
        assertEquals(inverterInterrPeriod * 1000, telemetriesReceiver.deltaTmsec, 20);
        assertEquals(gridPowerAll, telemetriesReceiver.telemetries.gridPowerAll, 0.0001);
        assertEquals(gridVoltageAll, telemetriesReceiver.telemetries.gridVoltageAll, 0.0001);
        assertEquals(inverterTemperature, telemetriesReceiver.telemetries.inverterTemp, 0.0001);
        assertEquals(cumulatedEnergy, telemetriesReceiver.telemetries.cumulatedEnergy, 0.0001);


    }

    @Test
    public void shouldProvideFixedTelemetries() throws Exception {


        final Object waitVar = new Object();

        int NUM_OF_TELEMETRIES = 3;

        float gridPowerAll = (float) 100;
        float gridVoltageAll = (float) 221.5;
        float inverterTemperature = (float) 27.4;
        Float cumulatedEnergy = (float) 0;
        float inverterInterrPeriod = (float) 0.5;


        AResp_CumulatedEnergy cumulateEnergyResponse = new AResp_CumulatedEnergy();
        AResp_DspData responseGridPowerAll = new AResp_DspData();
        AResp_DspData responseGridVoltageAll = new AResp_DspData();
        AResp_DspData respInverterTemperature = new AResp_DspData();

        cumulateEnergyResponse.setLongParam(cumulatedEnergy.longValue());
        responseGridPowerAll.setFloatParam(gridPowerAll);
        responseGridVoltageAll.setFloatParam(gridVoltageAll);
        respInverterTemperature.setFloatParam(inverterTemperature);

        when(auroraDriver.acquireCumulatedEnergy(eq(hwSettings.inverterAddress), eq(AuroraCumEnergyEnum.DAILY))).thenReturn(cumulateEnergyResponse);
        when(auroraDriver.acquireDspValue(eq(hwSettings.inverterAddress), eq(AuroraDspRequestEnum.GRID_POWER_ALL))).thenReturn(responseGridPowerAll);
        when(auroraDriver.acquireDspValue(eq(hwSettings.inverterAddress), eq(AuroraDspRequestEnum.GRID_VOLTAGE_ALL))).thenReturn(responseGridVoltageAll);
        when(auroraDriver.acquireDspValue(eq(hwSettings.inverterAddress), eq(AuroraDspRequestEnum.INVERTER_TEMPERATURE_GRID_TIED))).thenReturn(respInverterTemperature);

        TelemetriesReceiver telemetriesReceiver = new TelemetriesReceiver(NUM_OF_TELEMETRIES, waitVar);
        theEventBus.register(telemetriesReceiver);

        // Exercise
        auroraMonitor.setInverterInterrogationPeriod(inverterInterrPeriod);
        auroraMonitor.setDailyCumulatedEnergyEstimationFeature(true);
        auroraMonitor.start();

        synchronized (waitVar) {
            waitVar.wait(2000);
        }


        // Verify
        float expectedCumulatedEnergy = (gridPowerAll * inverterInterrPeriod) * 2;
        assertEquals(NUM_OF_TELEMETRIES, telemetriesReceiver.counter);
        assertEquals(inverterInterrPeriod * 1000, telemetriesReceiver.deltaTmsec, 20);
        assertEquals(gridPowerAll, telemetriesReceiver.telemetries.gridPowerAll, 0.0001);
        assertEquals(gridVoltageAll, telemetriesReceiver.telemetries.gridVoltageAll, 0.0001);
        assertEquals(inverterTemperature, telemetriesReceiver.telemetries.inverterTemp, 0.0001);
        assertEquals(expectedCumulatedEnergy, telemetriesReceiver.telemetries.cumulatedEnergy, 5);


    }


    @Test
    public void shouldProvideInverterStatus() throws Exception {


        final Object waitVar = new Object();


        float inverterInterrPeriod = (float) 0.5;


        AResp_CumulatedEnergy cumulateEnergyResponse = new AResp_CumulatedEnergy();
        cumulateEnergyResponse.setLongParam(0);
        cumulateEnergyResponse.setErrorCode(ResponseErrorEnum.TIMEOUT);

        AResp_DspData responseGridPowerAll = new AResp_DspData();
        responseGridPowerAll.setFloatParam(0);
        responseGridPowerAll.setErrorCode(ResponseErrorEnum.TIMEOUT);

        AResp_DspData responseGridVoltageAll = new AResp_DspData();
        responseGridVoltageAll.setFloatParam(0);
        responseGridVoltageAll.setErrorCode(ResponseErrorEnum.TIMEOUT);

        AResp_DspData respInverterTemperature = new AResp_DspData();
        respInverterTemperature.setErrorCode(ResponseErrorEnum.TIMEOUT);
        respInverterTemperature.setFloatParam(0);

        when(auroraDriver.acquireCumulatedEnergy(eq(hwSettings.inverterAddress), eq(AuroraCumEnergyEnum.DAILY))).thenReturn(cumulateEnergyResponse);
        when(auroraDriver.acquireDspValue(eq(hwSettings.inverterAddress), eq(AuroraDspRequestEnum.GRID_POWER_ALL))).thenReturn(responseGridPowerAll);
        when(auroraDriver.acquireDspValue(eq(hwSettings.inverterAddress), eq(AuroraDspRequestEnum.GRID_VOLTAGE_ALL))).thenReturn(responseGridVoltageAll);
        when(auroraDriver.acquireDspValue(eq(hwSettings.inverterAddress), eq(AuroraDspRequestEnum.INVERTER_TEMPERATURE_GRID_TIED))).thenReturn(respInverterTemperature);

        MonitorMsgInverterStatusReceiver inverterStatusReceiver = new MonitorMsgInverterStatusReceiver();
        TelemetriesReceiver telemetriesReceiver = new TelemetriesReceiver(1, waitVar);
        theEventBus.register(telemetriesReceiver);
        theEventBus.register(inverterStatusReceiver);

        // Exercise
        auroraMonitor.setInverterInterrogationPeriod(inverterInterrPeriod);
        auroraMonitor.start();

        synchronized (waitVar) {
            waitVar.wait(1000);
        }


        // Verify
        assertEquals(0, telemetriesReceiver.counter);
        assertEquals(2, inverterStatusReceiver.counter);
        assertEquals(false, inverterStatusReceiver.monitorStatusMsg.isOnline);


    }

}
