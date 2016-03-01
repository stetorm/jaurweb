package com.steto.monitor.pvoutput;

import com.steto.monitor.PeriodicInverterTelemetries;
import com.steto.utils.MyUtils;

import java.util.Date;

/**
 * Created by sbrega on 05/02/2015.
 */
public class PvOutputRecord {

    public Long timestamp = new Date().getTime();
    public float dailyCumulatedEnergy = 0;
    public float totalPowerGenerated = 0;
    public float temperature = 0;
    public float totalGridVoltage = 0;

    public PvOutputRecord(PeriodicInverterTelemetries telemetries) {
        timestamp = telemetries.timestamp;
        dailyCumulatedEnergy = telemetries.cumulatedEnergy;
        totalPowerGenerated = telemetries.gridPowerAll;
        temperature = telemetries.inverterTemp;
        totalGridVoltage = telemetries.gridVoltageAll;

    }

    public PvOutputRecord() {


    }

    @Override
    public boolean equals(Object other) {

        boolean result = false;
        if (!(other instanceof PvOutputRecord)) {
            result = false;
        } else {
            PvOutputRecord otherRecord = (PvOutputRecord) other;
            result = (other == this) || MyUtils.epsEquals(dailyCumulatedEnergy, otherRecord.dailyCumulatedEnergy)
                    && MyUtils.epsEquals(totalPowerGenerated, otherRecord.totalPowerGenerated)
                    && MyUtils.epsEquals(temperature, otherRecord.temperature)
                    && MyUtils.epsEquals(totalGridVoltage, otherRecord.totalGridVoltage);
        }

        return result;
    }

    public Date getDate() {
        Date date = new Date();
        date.setTime(timestamp);
        return date;

    }

    @Override
    public String toString() {
        return "PvOutputRecord{" +
                "timestamp=" + timestamp +
                ", dailyCumulatedEnergy=" + dailyCumulatedEnergy +
                ", totalPowerGenerated=" + totalPowerGenerated +
                ", temperature=" + temperature +
                ", totalGridVoltage=" + totalGridVoltage +
                '}';
    }
}
