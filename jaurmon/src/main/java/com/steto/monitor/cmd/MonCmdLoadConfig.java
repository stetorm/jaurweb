package com.steto.monitor.cmd;

import com.steto.jaurkit.EventBusRequest;

import java.util.Map;

/**
 * Created by stefano on 23/12/14.
 */
public class MonCmdLoadConfig extends EventBusRequest {
    public MonCmdLoadConfig(Map<String, String> params) {
        super(params);
    }

    public MonCmdLoadConfig() {
        super(null);
    }
}
