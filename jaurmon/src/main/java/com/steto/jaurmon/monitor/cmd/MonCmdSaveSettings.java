package com.steto.jaurmon.monitor.cmd;

import com.steto.jaurlib.eventbus.EventBusRequest;

import java.util.Map;

/**
 * Created by stefano on 30/01/15.
 */
public class MonCmdSaveSettings extends EventBusRequest {


    public MonCmdSaveSettings(Map<String, String> params) {
        super(params);
    }
}
