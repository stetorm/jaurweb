package com.steto.monitor.cmd;

import com.steto.jaurkit.EventBusRequest;


import java.util.Map;

/**
 * Created by sbrega on 02/03/2015.
 */
public class MonCmdReadStatus extends EventBusRequest {


    public MonCmdReadStatus(Map<String, String> params) {
        super(params);
    }

    public MonCmdReadStatus() {
        super(null);
    }
}

