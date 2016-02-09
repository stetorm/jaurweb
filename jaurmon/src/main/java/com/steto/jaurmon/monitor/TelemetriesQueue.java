package com.steto.jaurmon.monitor;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by stefano on 07/02/16.
 */
public class TelemetriesQueue {
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
        result.gridPowerAll /= count;
        result.gridVoltageAll /= count;
        result.inverterTemp /= count;

        result.timestamp = dataList.get(dataList.size() - 1).timestamp;
        result.cumulatedEnergy = dataList.get(dataList.size() - 1).cumulatedEnergy;

        return result;
    }

    public void removeOlderThan(long timestamp) {
        ListIterator<PeriodicInverterTelemetries> iterator = dataList.listIterator();
        while (iterator.hasNext() && iterator.next().timestamp < timestamp) {
            iterator.remove();
        }
    }
}
