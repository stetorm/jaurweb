package com.steto.jaurmon.monitor;

/**
 * Created by stefano on 18/01/16.
 */
public class PeriodicInverterTelemetries {

    public long timestamp=0;
    public float cumulatedEnergy=0;
    public float gridPowerAll=0;
    public float gridVoltageAll=0;
    public float inverterTemp=0;

    public void setTimestamp(long time) {
        timestamp=time;
    }
}
