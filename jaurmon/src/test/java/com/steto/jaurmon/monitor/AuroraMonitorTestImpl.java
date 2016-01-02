package com.steto.jaurmon.monitor;

import com.google.common.eventbus.EventBus;
import com.steto.jaurlib.AuroraDriver;
import jssc.SerialPortException;

import java.io.IOException;

/**
 * Created by stefano on 20/12/14.
 */


public class AuroraMonitorTestImpl extends AuroraMonitor {


    public String lastPvOutputDataPublished="";

    public AuroraMonitorTestImpl(AuroraDriver mock, String configFile, String datalogPath) throws IOException, SerialPortException {
        super(new EventBus(),mock, configFile,datalogPath);
    }

    public void setDailyCumulatedEnergy(long v) {
        dailyCumulatedEnergy = v;
    }

    public void setAllPowerGeneration(long v) {
        allPowerGeneration = v;
    }

    public void setInverterTemperature(double v) {
        inverterTemperature = v;
    }

    public void setAllGridVoltage(double v) {
        allGridVoltage = v;
    }


    public double getDailyCumulatedEnergy() {
        return dailyCumulatedEnergy;
    }

    public double getAllPowerGeneration() {
        return allPowerGeneration;
    }

    public double getInverterTemperature() {
        return inverterTemperature;
    }

    public double getAllGridVoltage() {
        return allGridVoltage;
    }

}
