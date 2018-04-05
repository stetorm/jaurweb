package com.steto.jaurmon.monitor;

/**
 * Created by stefano on 18/02/16.
 */
public class InverterCRCException extends Throwable {
    private String errMsg;

    @Override
    public String getMessage() {
        return super.getMessage()+". "+errMsg;
    }

    public InverterCRCException(String msg) {
        errMsg = msg;
    }
}
