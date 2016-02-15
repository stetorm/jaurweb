package com.steto.jaurmon.monitor.pvoutput.unit;

import com.steto.jaurmon.monitor.PeriodicInverterTelemetries;
import com.steto.jaurmon.monitor.TelemetriesQueue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by stefano on 07/02/16.
 */
public class TestDataStatistics {


    PeriodicInverterTelemetries inverterTelemetries1;
    PeriodicInverterTelemetries inverterTelemetries2;
    PeriodicInverterTelemetries inverterTelemetries3;
    TelemetriesQueue telemetriesQueue = new TelemetriesQueue();

    @Before
    public void before() {

        inverterTelemetries1 = new PeriodicInverterTelemetries();
        inverterTelemetries2 = new PeriodicInverterTelemetries();
        inverterTelemetries3 = new PeriodicInverterTelemetries();

        inverterTelemetries1.setTimestamp(100);
        inverterTelemetries1.cumulatedEnergy = 3;
        inverterTelemetries1.gridPowerAll = 3;
        inverterTelemetries1.gridVoltageAll = 3;
        inverterTelemetries1.inverterTemp = 3;

        inverterTelemetries2.setTimestamp(200);
        inverterTelemetries2.cumulatedEnergy = 5;
        inverterTelemetries2.gridPowerAll = 5;
        inverterTelemetries2.gridVoltageAll = 5;
        inverterTelemetries2.inverterTemp = 5;

        inverterTelemetries3.setTimestamp(300);
        inverterTelemetries3.cumulatedEnergy = 7;
        inverterTelemetries3.gridPowerAll = 7;
        inverterTelemetries3.gridVoltageAll = 7;
        inverterTelemetries3.inverterTemp = 7;

        telemetriesQueue.add(inverterTelemetries1);
        telemetriesQueue.add(inverterTelemetries2);
        telemetriesQueue.add(inverterTelemetries3);

    }

    @Test
    public void shouldFixAverageWhenZeroEnergy() {
        inverterTelemetries1.cumulatedEnergy = 0;
        inverterTelemetries2.cumulatedEnergy = 0;
        inverterTelemetries3.cumulatedEnergy = 0;

        PeriodicInverterTelemetries media = telemetriesQueue.fixedAverage();

        float deltaT1 = (float) ((inverterTelemetries2.timestamp - inverterTelemetries1.timestamp) / 1000.0);
        float deltaT2 = (float) ((inverterTelemetries3.timestamp - inverterTelemetries2.timestamp) / 1000.0);
        float estimatedEnergy = (5 + 3) / 2 * deltaT1 + (7 + 5) / 2 * deltaT2;
        assertEquals(media.gridPowerAll, (3 + 5 + 7) / 3, 0.00001);
        assertEquals(media.cumulatedEnergy, estimatedEnergy, 0.01);
        assertEquals(media.gridVoltageAll, (3 + 5 + 7) / 3, 0.00001);
        assertEquals(media.inverterTemp, (3 + 5 + 7) / 3, 0.00001);
        assertEquals(media.timestamp, 300, 0.00001);

    }

    @Test
    public void shouldResetAverage() {

        inverterTelemetries1.cumulatedEnergy = 0;
        inverterTelemetries2.cumulatedEnergy = 0;
        inverterTelemetries3.cumulatedEnergy = 0;

        PeriodicInverterTelemetries fixedMedia1 = telemetriesQueue.fixedAverage();

        telemetriesQueue.reset();

        telemetriesQueue.add(inverterTelemetries1);
        telemetriesQueue.add(inverterTelemetries2);
        telemetriesQueue.add(inverterTelemetries3);

        PeriodicInverterTelemetries fixedMedia2 = telemetriesQueue.fixedAverage();

        assertEquals(fixedMedia1.gridPowerAll, fixedMedia2.gridVoltageAll, 0.00001);
        assertEquals(fixedMedia1.cumulatedEnergy, fixedMedia2.cumulatedEnergy, 0.00001);
        assertEquals(fixedMedia1.inverterTemp, fixedMedia2.inverterTemp, 0.00001);
        assertEquals(fixedMedia1.timestamp, fixedMedia2.timestamp, 0.00001);

    }

    @Test
    public void shouldComputeAverage() {

        PeriodicInverterTelemetries media = telemetriesQueue.average();

        assertEquals(media.gridPowerAll, (3 + 5 + 7) / 3, 0.00001);
        assertEquals(media.cumulatedEnergy, 7, 0.00001);
        assertEquals(media.gridVoltageAll, (3 + 5 + 7) / 3, 0.00001);
        assertEquals(media.inverterTemp, (3 + 5 + 7) / 3, 0.00001);
        assertEquals(media.timestamp, 300, 0.00001);


    }

    @Test
    public void shouldNotFixAverage() {

        PeriodicInverterTelemetries media = telemetriesQueue.average();
        PeriodicInverterTelemetries fixedMedia = telemetriesQueue.fixedAverage();

        assertEquals(media.gridPowerAll, fixedMedia.gridVoltageAll, 0.00001);
        assertEquals(media.cumulatedEnergy, fixedMedia.cumulatedEnergy, 0.00001);
        assertEquals(media.inverterTemp, fixedMedia.inverterTemp, 0.00001);
        assertEquals(media.timestamp, fixedMedia.timestamp, 0.00001);

    }

    @Test
    public void shouldComputeAverageSince() {

        PeriodicInverterTelemetries average = telemetriesQueue.average(200);

        assertEquals((5 + 7) / 2, average.gridPowerAll, 0.00001);
        assertEquals(7, average.cumulatedEnergy, 0.00001);
        assertEquals((5 + 7) / 2, average.gridVoltageAll, 0.00001);
        assertEquals((5 + 7) / 2, average.inverterTemp, 0.00001);
        assertEquals(300, average.timestamp, 0.00001);

    }

    @Test
    public void shouldRemoveOlderItems() {

        PeriodicInverterTelemetries average200 = telemetriesQueue.average(200);
        telemetriesQueue.removeOlderThan(200);
        PeriodicInverterTelemetries averageAll = telemetriesQueue.average();

        assertEquals(average200.gridPowerAll, averageAll.gridPowerAll, 0.00001);
        assertEquals(average200.cumulatedEnergy, averageAll.cumulatedEnergy, 0.00001);
        assertEquals(average200.gridVoltageAll, averageAll.gridVoltageAll, 0.00001);
        assertEquals(average200.inverterTemp, averageAll.inverterTemp, 0.00001);
        assertEquals(average200.timestamp, averageAll.timestamp, 0.00001);

    }


    @Test
    public void shouldHandleEmptyQueue() {

        telemetriesQueue.removeOlderThan(1000);
        PeriodicInverterTelemetries averageAll = telemetriesQueue.average();
        assertNull(averageAll);


    }


}
