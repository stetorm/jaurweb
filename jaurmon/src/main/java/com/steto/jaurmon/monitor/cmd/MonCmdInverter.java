package com.steto.jaurmon.monitor.cmd;

import com.steto.jaurlib.eventbus.EventBusRequest;

/**
 * Created by stefano on 26/12/14.
 */
public class MonCmdInverter extends EventBusRequest {


    public int invAddress ;


    public MonCmdInverter(int aInverterAddress) {
        this.invAddress = aInverterAddress;
    }
}
