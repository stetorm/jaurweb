package com.steto.jaurmon.monitor;

import java.util.Map;

/**
 * Created by sbrega on 07/01/2015.
 */


public class MonCmdTestPVoutput extends MonitorCommand {
    public final Map<String,String> command;

    public MonCmdTestPVoutput(Map mapCommand) {
        super();
        this.command = mapCommand;
    }
}