package com.steto.monitor;

import java.util.Properties;

/**
 * Created by stefano on 24/12/15.
 */
public class HwSettings {
    public String serialPort = "/dev/tty";
    public int serialPortBaudRate = 19200;
    public int inverterAddress = 2;

    public Properties toProperties() {
        Properties result = new Properties();
        result.setProperty("serialPort", serialPort);
        result.setProperty("serialPortBaudRate", String.valueOf(serialPortBaudRate));
        result.setProperty("inverterAddress", String.valueOf(inverterAddress));

        return result;
    }



}
