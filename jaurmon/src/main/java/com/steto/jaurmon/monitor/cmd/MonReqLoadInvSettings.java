package com.steto.jaurmon.monitor.cmd;

import com.steto.jaurkit.EventBusRequest;


import java.util.Map;

/**
 * Created by stefano on 30/01/15.
 */
public class MonReqLoadInvSettings extends EventBusRequest {


    public MonReqLoadInvSettings(Map<String, String> params) {
        super(params);
    }
}
