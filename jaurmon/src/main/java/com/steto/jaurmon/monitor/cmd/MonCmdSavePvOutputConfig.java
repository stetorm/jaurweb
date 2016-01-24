package com.steto.jaurmon.monitor.cmd;

import com.steto.jaurlib.eventbus.EventBusRequest;

import java.util.Map;

/**
 * Created by stefano on 23/12/14.
 */
public class MonCmdSavePvOutputConfig extends EventBusRequest {

    public MonCmdSavePvOutputConfig(Map<String, String> params) {
        super(params);
    }
}
