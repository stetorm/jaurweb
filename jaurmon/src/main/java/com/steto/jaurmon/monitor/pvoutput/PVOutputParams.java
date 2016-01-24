package com.steto.jaurmon.monitor.pvoutput;

import java.util.Properties;

/**
 * Created by stefano on 24/12/15.
 */
public class PVOutputParams {
    public int systemId;
    public float period;
    public String url;
    public String apiKey;

    public Properties toProperties() {
        Properties result = new Properties();

        result.setProperty("pvOutputSystemId", String.valueOf(systemId));
        result.setProperty("pvOutputPeriod", String.valueOf(period));
        result.setProperty("pvOutputUrl", url);
        result.setProperty("pvOutputApiKey", apiKey);

        return result;
    }

}
