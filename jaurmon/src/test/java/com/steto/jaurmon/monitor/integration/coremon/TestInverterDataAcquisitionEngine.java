package com.steto.jaurmon.monitor.integration.coremon;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.cmd.InverterCommandFactory;
import com.steto.jaurlib.eventbus.EventBusInverterAdapter;
import com.steto.jaurlib.request.AuroraCumEnergyEnum;
import com.steto.jaurlib.request.AuroraDspRequestEnum;
import com.steto.jaurlib.response.AResp_CumulatedEnergy;
import com.steto.jaurlib.response.AResp_DspData;
import com.steto.jaurlib.response.AuroraResponse;
import com.steto.jaurlib.response.ResponseErrorEnum;
import com.steto.jaurmon.monitor.AuroraMonitor;
import com.steto.jaurmon.monitor.HwSettings;
import com.steto.jaurmon.monitor.PeriodicInverterTelemetries;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static com.steto.jaurmon.monitor.RandomObjectGenerator.getA_HwSettings;
import static com.steto.jaurmon.monitor.TestUtility.createAuroraConfigFile;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by stefano on 17/01/16.
 */

public class TestInverterDataAcquisitionEngine {


    class  TelemetriesReceiver  {

        private final Object waitVar;
        public PeriodicInverterTelemetries telemetries;

        public TelemetriesReceiver(Object waitVar) {
            this.waitVar = waitVar;
        }


        @Subscribe
        public void handle(PeriodicInverterTelemetries msg) {
            telemetries = msg;
            synchronized (waitVar) {
                waitVar.notifyAll();
            }
        }

    };

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


    @Before
    public void before() throws Exception {

        hwSettings = getA_HwSettings();
        createAuroraConfigFile(configFile,hwSettings);

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
    public void should() throws Exception {


        final Object waitVar = new Object();


        float gridPowerAll = (float) 321.3;
        float gridVoltageAll = (float) 221.5;
        float inverterTemperature = (float) 27.4;
        Float cumulatedEnergy = (float) 1367.0;

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

        TelemetriesReceiver telemetriesReceiver = new TelemetriesReceiver(waitVar);
        theEventBus.register(telemetriesReceiver);

        // Exercise
        auroraMonitor.start();

        synchronized (waitVar) {
            waitVar.wait(2000);
        }


        // Verify
        assertEquals(gridPowerAll,telemetriesReceiver.telemetries.gridPowerAll,0.0001);
        assertEquals(gridVoltageAll,telemetriesReceiver.telemetries.gridVoltageAll,0.0001);
        assertEquals(inverterTemperature,telemetriesReceiver.telemetries.inverterTemp,0.0001);
        assertEquals(cumulatedEnergy,telemetriesReceiver.telemetries.cumulatedEnergy,0.0001);



    }
}
