package com.steto.monitor;

import com.steto.monitor.pvoutput.PVOutputParams;
import com.steto.monitor.pvoutput.PvOutputRecord;

import java.util.Date;
import java.util.Random;

/**
 * Created by sbrega on 05/02/2015.
 */

public class RandomObjectGenerator {
    public static PvOutputRecord getPvOutputRecord() {
        PvOutputRecord pvOutputRecord = new PvOutputRecord();
        pvOutputRecord.dailyCumulatedEnergy= getFloat(10000000);
        pvOutputRecord.temperature = getFloat(200)-50;
        pvOutputRecord.totalGridVoltage = getFloat(1000);
        pvOutputRecord.totalPowerGenerated = getFloat(1000);
        pvOutputRecord.timestamp = new Date().getTime();
        return pvOutputRecord;
    }

    public static HwSettings getA_HwSettings() {
        HwSettings hwSettings = new HwSettings();
        hwSettings.inverterAddress = getInt(32);
        hwSettings.serialPortBaudRate = getInt(105000);
        hwSettings.serialPort = getString("/dev/myserialPort");
        return hwSettings;
    }

    public static MonitorSettings getA_MonitorSettings() {
        MonitorSettings monitorSettings = new MonitorSettings();
        monitorSettings.inverterInterrogationPeriodSec = getInt(1000);
        return monitorSettings;
    }

    public static PVOutputParams getA_PvOutputParams() {
        PVOutputParams pvOutParams = new PVOutputParams();
        pvOutParams.systemId = getInt(32);
        pvOutParams.period= getInt(100);
        pvOutParams.timeWindowSec= getInt(1000);
        pvOutParams.apiKey = getString("api");
        pvOutParams.url = getString("http://url?");
        return pvOutParams;
    }



    public static int getInt(int maxNum) {
        Random random = new Random();
        float f = random.nextFloat();
        return (int) (f * maxNum);
    }





    public static boolean getBoolean() {
        int val = getInt(10000);
        boolean res = ((val % 2) == 0);
        return res;
    }



    public static long getRandomLongNumber(long maxNum) {
        Random random = new Random();
        float f = random.nextFloat();
        return (long) (f * maxNum);
    }

    public static float getFloat(float maxNum) {
        Random random = new Random();
        float f = random.nextFloat();
        return  (f * maxNum);
    }


    public static boolean getRandomBoolean() {
        Random random = new Random();
        return random.nextBoolean();
    }

    public static String getString(String baseString) {
        return baseString + getInt(100000);
    }

    public static PeriodicInverterTelemetries getA_PeriodicInverterTelemetries() {
        PeriodicInverterTelemetries result = new PeriodicInverterTelemetries();
        result.gridVoltageAll = getFloat(500);
        result.cumulatedEnergy = getFloat(50000);
        result.inverterTemp = getFloat(100);
        result.gridPowerAll = getFloat(10000);
        return result;
    }
}
