package com.steto.jaurmon.monitor.core.unit;

import com.google.common.eventbus.EventBus;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.request.AuroraCumEnergyEnum;
import com.steto.jaurlib.request.AuroraDspRequestEnum;
import com.steto.jaurlib.response.AResp_CumulatedEnergy;
import com.steto.jaurlib.response.AResp_DspData;
import com.steto.jaurlib.response.AResp_VersionId;
import com.steto.jaurlib.response.ResponseErrorEnum;
import com.steto.jaurmon.monitor.AuroraMonitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by stefano on 21/12/14.
 */
public class TestDataAcquisitionFromInverter {

    AuroraDriver auroraDriver;
    AResp_CumulatedEnergy expectedCumulateEnergy;
    AResp_DspData expectedPower;
    AResp_DspData expectedVoltage;
    AResp_DspData expectedTemperature;
    AuroraMonitor auroraMonitor ;
    long cumulatedEnergy= 1201;
    Double powerAll=1840.0;
    Double voltageAll=437.9;
    Double temperature=44.7;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private String pvOutDirPath;


    @After
    public void after() throws IOException {
        tempFolder.delete();

    }
    @Before
    public void before() throws Exception {
        pvOutDirPath =  tempFolder.newFolder().getAbsolutePath();
        auroraDriver= mock(AuroraDriver.class);
        expectedCumulateEnergy = mock(AResp_CumulatedEnergy.class);
        expectedVoltage = mock(AResp_DspData.class);
        expectedTemperature = mock(AResp_DspData.class);
        expectedPower = mock(AResp_DspData.class);
        when(auroraDriver.acquireCumulatedEnergy(anyInt(),any(AuroraCumEnergyEnum.class))).thenReturn(expectedCumulateEnergy);
        when(auroraDriver.acquireDspValue(anyInt(), eq(AuroraDspRequestEnum.GRID_POWER_ALL))).thenReturn(expectedPower);
        when(auroraDriver.acquireDspValue(anyInt(), eq(AuroraDspRequestEnum.GRID_VOLTAGE_ALL))).thenReturn(expectedVoltage);
        when(auroraDriver.acquireDspValue(anyInt(), eq(AuroraDspRequestEnum.INVERTER_TEMPERATURE_GRID_TIED))).thenReturn(expectedTemperature);

        AResp_VersionId aRespVersionIdTimeout = new AResp_VersionId();
        aRespVersionIdTimeout.setErrorCode(ResponseErrorEnum.TIMEOUT);
        AResp_VersionId aRespVersionIdGood= new AResp_VersionId();
        aRespVersionIdGood.setErrorCode(ResponseErrorEnum.NONE);

        doReturn(aRespVersionIdTimeout)
                .doReturn(aRespVersionIdGood)
                .doReturn(aRespVersionIdGood)
                .doReturn(aRespVersionIdTimeout)
                .doReturn(aRespVersionIdGood)
                .doReturn(aRespVersionIdTimeout)
                .doReturn(aRespVersionIdTimeout)
                .when(auroraDriver).acquireVersionId(anyInt());

        auroraMonitor = new AuroraMonitor(mock(EventBus.class),auroraDriver, " afilename",pvOutDirPath);

    }

    @Test
    public void shouldAcquireDailyEnergy() throws Exception {
        when(expectedCumulateEnergy.getLongParam()).thenReturn(cumulatedEnergy);
        when(expectedPower.getFloatParam()).thenReturn(powerAll.floatValue());
        when(expectedVoltage.getFloatParam()).thenReturn(voltageAll.floatValue());
        when(expectedTemperature.getFloatParam()).thenReturn(temperature.floatValue());

        when(expectedCumulateEnergy.getErrorCode()).thenReturn(ResponseErrorEnum.NONE);
        when(expectedPower.getErrorCode()).thenReturn(ResponseErrorEnum.NONE);
        when(expectedVoltage.getErrorCode()).thenReturn(ResponseErrorEnum.NONE);
        when(expectedTemperature.getErrorCode()).thenReturn(ResponseErrorEnum.NONE);

        auroraMonitor.acquireDataToBePublished();

        assertEquals(cumulatedEnergy, auroraMonitor.getCumulatedEnergyReadout(),0001);
        assertEquals(powerAll, auroraMonitor.getInstantPowerReadout(), 0.0001);
        assertEquals(voltageAll, auroraMonitor.getVoltageReadout(), 0.0001);
        assertEquals(temperature,auroraMonitor.getTemperatureReadout(), 0.0001);
        assertTrue(auroraMonitor.isInverterOnline());

    }



    @Test
    public void shouldInitializeInvStatusOnStartup() throws Exception {

        assertFalse(auroraMonitor.isInverterOnline());


    }

    @Test
    public void shouldUpdateInverterStatus() throws Exception {


        auroraMonitor.checkInverterStatus();   // success
        assertTrue(auroraMonitor.isInverterOnline());

        auroraMonitor.checkInverterStatus();  // success
        assertTrue(auroraMonitor.isInverterOnline());

        auroraMonitor.checkInverterStatus(); // timeout
        assertTrue(auroraMonitor.isInverterOnline());

        auroraMonitor.checkInverterStatus(); // success
        assertTrue(auroraMonitor.isInverterOnline());

        auroraMonitor.checkInverterStatus(); // timeout
        assertTrue(auroraMonitor.isInverterOnline());

        auroraMonitor.checkInverterStatus(); // timeout
        assertFalse(auroraMonitor.isInverterOnline());

    }


}
