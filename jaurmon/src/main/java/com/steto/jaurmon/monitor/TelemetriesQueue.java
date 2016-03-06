package com.steto.jaurmon.monitor;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by stefano on 07/02/16.
 */
public class TelemetriesQueue {

    Logger log = Logger.getLogger(getClass().getSimpleName());
    private final int maxDim;
    List<PeriodicInverterTelemetries> dataList = Collections.synchronizedList(new ArrayList<PeriodicInverterTelemetries>());

    protected static Comparator<PeriodicInverterTelemetries> Timestamp = new Comparator<PeriodicInverterTelemetries>() {
        @Override
        public int compare(PeriodicInverterTelemetries o1, PeriodicInverterTelemetries o2) {
            int i = Long.valueOf(o1.timestamp).compareTo(Long.valueOf(o2.timestamp));
            return i;
        }
    };

    public TelemetriesQueue(int maxDim) {
        this.maxDim = maxDim;
    }

    public TelemetriesQueue() {
        this(1000);
    }

    public void add(PeriodicInverterTelemetries inverterTelemetries1) {

        if (dataList.size() >= maxDim) {
            dataList.remove(0);
        }
        dataList.add(inverterTelemetries1);
        Collections.sort(dataList, Timestamp);

    }

    public PeriodicInverterTelemetries average() {

        return average(0);
    }

    public PeriodicInverterTelemetries average(long sinceTime) {
        log.info("Averaging telemetries since: " + new Date(sinceTime)) ;
        PeriodicInverterTelemetries result = new PeriodicInverterTelemetries();
        int count = 0;
        for (PeriodicInverterTelemetries telemetry : dataList) {
            if (telemetry.timestamp >= sinceTime) {
                log.fine("Adding telemetry "+telemetry);
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

        log.fine("Average result of "+count+" telemetries:" + result);
        return result;
    }

    public PeriodicInverterTelemetries fixedAverage() {
        PeriodicInverterTelemetries result = average(0);
        if (dataList.size() > 0) {
            float estimatedEnergy = estimateEnergy();
            result.cumulatedEnergy = estimatedEnergy;

        }
        return result;
    }

    private float estimateEnergy() {

        float energy = 0;

        if (dataList.size() > 0) {
            for (int i = 1; i < dataList.size(); i++) {
                float powMed = (float) ((dataList.get(i).gridPowerAll + dataList.get(i - 1).gridPowerAll) / 2.0);
                float deltaT = (float) ((dataList.get(i).timestamp - dataList.get(i - 1).timestamp) / 1000.0);
                float deltaHours = (float) (deltaT / 3600.0);
                float partialEnergy = powMed * deltaHours;
                energy += partialEnergy;
                log.finer("partial energy (Wh): " + partialEnergy + ", powMed: " + powMed + ", deltaT: " + deltaT);
            }
        }

        log.fine("Estimated Energy: " + energy);
        return energy;
    }

    public void removeOlderThan(long timestamp) {
        log.fine("Discarding telemetries since: "+new Date(timestamp));
        ListIterator<PeriodicInverterTelemetries> iterator = dataList.listIterator();
//        while (iterator.hasNext() && iterator.next().timestamp < timestamp) {
        while (iterator.hasNext()) {
            PeriodicInverterTelemetries telem = iterator.next();
            if (telem.timestamp < timestamp) {
                log.fine("Telemetry discarded: " + telem);
                iterator.remove();
            }
        }
    }

    public int length() {
        return dataList.size();
    }

    public void clear() {
        dataList.clear();
    }
}
