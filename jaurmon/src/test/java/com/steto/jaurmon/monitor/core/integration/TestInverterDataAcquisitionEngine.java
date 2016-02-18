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
        public MonitorMsgInverterStatus inverterStatus;
        public int telemCounter = 0;
        long deltaTmsec =0;

        public int monitorStatusMsgCounter = 0;

        public TelemetriesReceiver(int cardinality, Object waitVar) {
            this.waitVar = waitVar;
            this.cardinality = cardinality;
        }


        @Subscribe
        public void handle(PeriodicInverterTelemetries msg) {
            telemCounter++;
            if (telemCounter ==1)
            {
                deltaTmsec = new Date().getTime();
            }
            else
            if (telemCounter ==2)
            {
                deltaTmsec =  new Date().getTime() - deltaTmsec;
            }
            telemetries = msg;
            synchronized (waitVar) {
                if (telemCounter >= cardinality)
                    waitVar.notifyAll();
            }
        }

        @Subscribe
        public void handle(MonitorMsgInverterStatus msg) {
            inverterStatus = msg;
        }


    }

    ;

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
        assertEquals(NUM_OF_TELEMETRIES, telemetriesReceiver.telemCounter);
        assertEquals(true, telemetriesReceiver.inverterStatus.isOnline);
        assertEquals(gridPowerAll, telemetriesReceiver.telemetries.gridPowerAll, 0.0001);
        assertEquals(gridVoltageAll, telemetriesReceiver.telemetries.gridVoltageAll, 0.0001);
        assertEquals(inverterTemperature, telemetriesReceiver.telemetries.inverterTemp, 0.0001);
        assertEquals(cumulatedEnergy, telemetriesReceiver.telemetries.cumulatedEnergy, 0.0001);


    }

    @Test
    public void should() throws Exception {


        final Object waitVar = new Object();

        int NUM_OF_TELEMETRIES = 1;

        float gridPowerAll = 0;
        float gridVoltageAll = 0;
        float inverterTemperature = 0;
        float cumulatedEnergy = 0;
        float inverterInterrPeriod = 0;


        AResp_CumulatedEnergy cumulateEnergyResponse = new AResp_CumulatedEnergy();
        cumulateEnergyResponse.setErrorCode(ResponseErrorEnum.TIMEOUT);

        AResp_DspData responseGridPowerAll = new AResp_DspData();
        responseGridPowerAll.setErrorCode(ResponseErrorEnum.TIMEOUT);

        AResp_DspData responseGridVoltageAll = new AResp_DspData();
        responseGridVoltageAll.setErrorCode(ResponseErrorEnum.TIMEOUT);

        AResp_DspData respInverterTemperature = new AResp_DspData();
        respInverterTemperature.setErrorCode(ResponseErrorEnum.TIMEOUT);


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
        assertEquals(NUM_OF_TELEMETRIES, 0);
        assertEquals(false, telemetriesReceiver.inverterStatus.isOnline);


    }

}
