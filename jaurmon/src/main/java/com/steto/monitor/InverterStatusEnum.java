package com.steto.monitor;

/**
 * Created by stefano on 20/12/14.
 */

public enum InverterStatusEnum {
    OFFLINE(0),
    ONLINE(1),
    UNCERTAIN(3);

    int val;

    InverterStatusEnum(int aVal) {

        val = aVal;
    }
}
