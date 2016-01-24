package com.steto.jaurlib.eventbus;

import java.util.Map;

/**
 * Created by stefano on 28/12/15.
 */
public class EventBusInverterRequest extends EventBusRequest{
    private String opcode;
    private final String subcode;
    private final int address;

    public EventBusInverterRequest(String opcode, String subcode, int inverterAddress) {
        this.opcode=opcode;
        this.subcode=subcode;
        this.address=inverterAddress;

    }

    public EventBusInverterRequest(Map cmdParams) {
        this.opcode = (String) cmdParams.get("opcode");
        this.subcode = (String) cmdParams.get("subcode");
        this.address = Integer.parseInt((String) cmdParams.get("address"));

    }


    public String opcode() {
        return opcode;
    }

    public String subcode() {
        return subcode;
    }

    public int address() {
        return address;

    }
}
