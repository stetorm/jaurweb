package com.steto.jaurmon.monitor;

import com.steto.jaurmon.monitor.pvoutput.PVOutputParams;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

import java.io.File;

/**
 * Created by stefano on 18/01/16.
 */
public class TestUtility {

    public static void createAuroraConfigFile(String configurationFileName,HwSettings hwSettings) throws ConfigurationException {

        HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration(new File(configurationFileName));
        iniConfObj.setProperty("inverter.inverterAddress", hwSettings.inverterAddress);
        iniConfObj.setProperty("inverter.serialPortBaudRate", hwSettings.serialPortBaudRate);
        iniConfObj.setProperty("inverter.serialPort", hwSettings.serialPort);

        iniConfObj.save();

    }

    public static void createPvoutputConfigFile(String configurationFileName,PVOutputParams pvOutputParams) throws ConfigurationException {

        HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration(new File(configurationFileName));
        iniConfObj.setProperty("pvoutput.systemId", pvOutputParams.systemId);
        iniConfObj.setProperty("pvoutput.apiKey", pvOutputParams.apiKey);
        iniConfObj.setProperty("pvoutput.period", pvOutputParams.period);
        iniConfObj.setProperty("pvoutput.url", pvOutputParams.url);

        iniConfObj.save();

    }

}
