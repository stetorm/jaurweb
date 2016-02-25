package com.steto.jaurmon.monitor;

import java.util.Date;

/**
 * Created by stefano on 18/01/16.
 */
public class PeriodicInverterTelemetries {

    public long timestamp= new Date().getTime();
    public float cumulatedEnergy=0;
    public float gridPowerAll=0;
    public float gridVoltageAll=0;
    public float inverterTemp=0;

    public void setTimestamp(long time) {
        timestamp=time;
    }

    @Override
    public String toString() {
        return "PeriodicInverterTelemetries{" +
                "date=" + new Date(timestamp) +
                ", timestamp=" + timestamp +
                ", cumulatedEnergy=" + cumulatedEnergy +
                ", gridPowerAll=" + gridPowerAll +
                ", gridVoltageAll=" + gridVoltageAll +
                ", inverterTemp=" + inverterTemp +
                '}';
    }
}
