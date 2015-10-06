package com.steto.jaurmon.monitor;

import java.util.Map;

/**
 * Created by sbrega on 22/01/2015.
 */


public class MonCmdApplySettings extends MonitorCommand {
    public final Map<String,String> command;

    public MonCmdApplySettings(Map mapCommand) {
        super();
        this.command = mapCommand;
    }
}
