package com.steto.jaurmon.monitor.cmd;

import com.steto.jaurlib.eventbus.EventBusRequest;

import java.util.Map;

/**
 * Created by stefano on 30/01/15.
 */
public class MonReqLoadInvSettings extends EventBusRequest {


    public MonReqLoadInvSettings(Map<String, String> params) {
        super(params);
    }
}
