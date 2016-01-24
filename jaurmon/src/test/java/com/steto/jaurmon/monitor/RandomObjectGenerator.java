package com.steto.jaurmon.monitor;

import com.steto.jaurmon.monitor.pvoutput.PvOutputRecord;

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

}
