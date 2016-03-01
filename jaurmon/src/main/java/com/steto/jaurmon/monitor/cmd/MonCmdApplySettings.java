package com.steto.jaurmon.monitor.cmd;

import com.steto.jaurkit.EventBusRequest;

import java.util.Map;

/**
 * Created by sbrega on 22/01/2015.
 */


public class MonCmdApplySettings extends EventBusRequest {
    protected MonCmdApplySettings(Map<String, String> params) {
        super(params);
    }
}
