package com.steto.jaurmon.monitor.cmd;

import com.steto.jaurmon.monitor.WebResponse;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Created by stefano on 23/12/14.
 */
public abstract class MonitorCommand {
    public final Map<String, String> paramsMap;
    public WebResponse response = null;

    protected MonitorCommand(Map<String, String> params) {
        this.paramsMap = params;
    }

    public MonitorCommand() {
        paramsMap=null;
    }
}
