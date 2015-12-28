package com.steto.jaurlib.eventbus;

/**
 * Created by stefano on 28/12/15.
 */
public class EventBusInverterRequest {
    protected EBResponse response=null;
    private String opcode;
    private final String subcode;
    private final int address;

    public EventBusInverterRequest(String opcode, String subcode, int inverterAddress) {
        this.opcode=opcode;
        this.subcode=subcode;
        this.address=inverterAddress;

    }


    public EBResponse getResponse()
    {
        return response;
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
