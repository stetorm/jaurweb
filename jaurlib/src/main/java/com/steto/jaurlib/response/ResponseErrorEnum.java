package com.steto.jaurlib.response;

/**
 * Created by sbrega on 27/11/2014.
 */


public enum ResponseErrorEnum {

    NONE(0),
    CRC(1),
    TIMEOUT(2), UNKNOWN(3);

    final int value;

    ResponseErrorEnum(int val) {
        value = val;
    }


    public int get() {
        return value;
    }
}
