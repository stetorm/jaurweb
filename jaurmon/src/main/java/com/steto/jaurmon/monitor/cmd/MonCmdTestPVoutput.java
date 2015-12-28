package com.steto.jaurmon.monitor.cmd;

import com.steto.jaurlib.eventbus.EventBusRequest;

import java.util.Map;

/**
 * Created by sbrega on 07/01/2015.
 */


public class MonCmdTestPVoutput extends EventBusRequest {

    protected MonCmdTestPVoutput(Map<String, String> params) {
        super(params);
    }
}