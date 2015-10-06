package com.steto.jaurmon.monitor;

import java.util.Map;

/**
 * Created by stefano on 30/01/15.
 */
public class MonCmdSaveSettings extends MonitorCommand {
    public final Map<String,String> command;

    public MonCmdSaveSettings(Map mapCommand) {
        super();
        this.command = mapCommand;
    }
}
