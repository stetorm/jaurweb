package com.steto.jaurmon.monitor.cmd;

import com.steto.jaurkit.EventBusRequest;

import java.util.Map;

/**
 * Created by stefano on 30/01/15.
 */
public class MonReqSaveInvSettings extends EventBusRequest {


    public MonReqSaveInvSettings(Map<String, String> params) {
        super(params);
    }
}
