package com.steto.monitor.cmd;

import com.steto.jaurkit.EventBusRequest;


import java.util.Map;

/**
 * Created by stefano on 23/12/14.
 */
public class MonCmdSavePvOutputConfig extends EventBusRequest {

    public MonCmdSavePvOutputConfig(Map<String, String> params) {
        super(params);
    }
}
