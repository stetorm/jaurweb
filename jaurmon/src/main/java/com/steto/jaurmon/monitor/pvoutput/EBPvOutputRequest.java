package com.steto.jaurmon.monitor.pvoutput;

import com.steto.jaurkit.EventBusRequest;

import java.util.Map;

/**
 * Created by stefano on 28/12/15.
 */
public class EBPvOutputRequest extends EventBusRequest {
    private String opcode;



    public EBPvOutputRequest(Map cmdParams) {
        super(cmdParams);
        this.opcode = (String) cmdParams.get("opcode");

    }


    public String opcode() {
        return opcode;
    }


}
