package com.steto.jaurmon.monitor.cmd;

import java.util.Map;

/**
 * Created by stefano on 23/12/14.
 */
public class MonCmdLoadConfig extends MonitorCommand {
    public MonCmdLoadConfig(Map<String, String> params) {
        super(params);
    }

    public MonCmdLoadConfig() {
        super(null);
    }
}
