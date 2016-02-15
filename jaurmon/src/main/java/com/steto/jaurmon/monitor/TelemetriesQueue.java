package com.steto.jaurmon.monitor;

import java.util.*;

/**
 * Created by stefano on 07/02/16.
 */
public class TelemetriesQueue {
    float cumulatedEnergy = 0;
    List<PeriodicInverterTelemetries> dataList = new ArrayList<PeriodicInverterTelemetries>();

    protected static Comparator<PeriodicInverterTelemetries> Timestamp = new Comparator<PeriodicInverterTelemetries>() {
        @Override
        public int compare(PeriodicInverterTelemetries o1, PeriodicInverterTelemetries o2) {
            int i = Long.valueOf(o1.timestamp).compareTo(Long.valueOf(o2.timestamp));
            return i;
        }
    };

    public void add(PeriodicInverterTelemetries inverterTelemetries1) {

        dataList.add(inverterTelemetries1);
        Collections.sort(dataList, Timestamp);

    }

    public PeriodicInverterTelemetries average() {

        return average(0);
    }

    public PeriodicInverterTelemetries average(long sinceTime) {
        PeriodicInverterTelemetries result = new PeriodicInverterTelemetries();
        int count = 0;
        for (PeriodicInverterTelemetries telemetry : dataList) {
            if (telemetry.timestamp >= sinceTime) {
                count++;
                result.gridPowerAll += telemetry.gridPowerAll;
                result.gridVoltageAll += telemetry.gridVoltageAll;
                result.inverterTemp += telemetry.inverterTemp;
            }
        }
        if (count == 0)
            return null;

        result.gridPowerAll /= count;
        result.gridVoltageAll /= count;
        result.inverterTemp /= count;

        result.timestamp = dataList.get(dataList.size() - 1).timestamp;
        result.cumulatedEnergy = dataList.get(dataList.size() - 1).cumulatedEnergy;

        return result;
    }

    public PeriodicInverterTelemetries fixedAverage() {
        PeriodicInverterTelemetries result = average(0);
        if (cumulatedEnergy == 0 && dataList.size() >0 && dataList.get(dataList.size() - 1).cumulatedEnergy == 0) {
            float estimatedEnergy = estimateEnergy();
            cumulatedEnergy += estimatedEnergy;
            result.cumulatedEnergy = cumulatedEnergy;

        }
        return result;
    }

    private float estimateEnergy() {

        float energy = 0;

        if (dataList.size() > 0) {
            for (int i = 1; i < dataList.size(); i++) {
                float powMed = (float) ((dataList.get(i).gridPowerAll + dataList.get(i - 1).gridPowerAll)/2.0);
                float deltaT = (float) ((dataList.get(i).timestamp - dataList.get(i - 1).timestamp) / 1000.0);
                energy += powMed * deltaT;
            }
        }

        return energy;
    }

    public void removeOlderThan(long timestamp) {
        ListIterator<PeriodicInverterTelemetries> iterator = dataList.listIterator();
        while (iterator.hasNext() && iterator.next().timestamp < timestamp) {
            iterator.remove();
        }
    }

    public void reset() {
        cumulatedEnergy=0;
        dataList.clear();
    }
}
