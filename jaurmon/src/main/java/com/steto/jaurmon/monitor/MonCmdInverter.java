package com.steto.jaurmon.monitor;

import java.util.Map;

/**
 * Created by stefano on 26/12/14.
 */
public class MonCmdInverter extends MonitorCommand {
    public final Map<String,String> command;

    public MonCmdInverter(Map mapCommand) {
        super();
        this.command = mapCommand;
    }

}
