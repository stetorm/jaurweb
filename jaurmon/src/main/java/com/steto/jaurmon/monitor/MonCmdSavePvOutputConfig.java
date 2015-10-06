package com.steto.jaurmon.monitor;

import java.util.Map;

/**
 * Created by stefano on 23/12/14.
 */
public class MonCmdSavePvOutputConfig extends MonitorCommand {
    public final Map<String,String> command;

    public MonCmdSavePvOutputConfig(Map mapCommand) {
        super();
        this.command = mapCommand;
    }
}
