package com.steto.jaurmon.monitor.cmd;

/**
 * Created by stefano on 26/12/14.
 */
public class MonCmdInverter extends MonitorCommand {


    public int invAddress ;


    public MonCmdInverter(int aInverterAddress) {
        this.invAddress = aInverterAddress;
    }
}
