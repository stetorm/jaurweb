package com.steto.jaurmon.monitor.webserver;

import com.google.common.eventbus.EventBus;
import com.steto.jaurlib.eventbus.EBResponseNOK;
import com.steto.jaurlib.eventbus.EBInverterRequest;
import com.steto.jaurkit.EventBusRequest;

import com.steto.jaurmon.monitor.cmd.*;
import com.steto.jaurmon.monitor.pvoutput.EBPvOutputRequest;
import com.steto.jaurmon.utils.HttpUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by stefano on 27/12/15.
 */
class InverterCmdHandler extends AbstractHandler {
    private EventBus theEventBus;
    protected Logger log = Logger.getLogger(getClass().getSimpleName());


    public InverterCmdHandler(EventBus aEventBus) {
        this.theEventBus = aEventBus;

    }


    protected EventBusRequest createMonitorCommand(String cmdUrl, Map cmdParams) {

        EventBusRequest result = null;
        switch (cmdUrl) {
            case "saveInvSettings":
                result = new MonReqSaveInvSettings(cmdParams);
                break;
            case "saveCfg":
                result = new MonCmdSavePvOutputConfig(cmdParams);
                break;
            case "loadInvSettings":
                result = new MonReqLoadInvSettings(cmdParams);
                break;
            case "status":
                result = new MonCmdReadStatus();
                break;
            case "inv":
                result = new EBInverterRequest(cmdParams);
                break;
            case "pvoutput":
                result = new EBPvOutputRequest(cmdParams);
                break;

        }

        return result;
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {
        // elimina lo / iniziale
        String responseString = "";
        String commandReceived = "";
        try {
            String[] pathList = request.getRequestURI().split("/");
            commandReceived = pathList[pathList.length - 1];

            log.info("Handling request: " + commandReceived);
            Map<String, String> queryMap = null;
            if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
                queryMap = HttpUtils.getQueryMap(request.getQueryString());
            }

            EventBusRequest eventBusRequest = createMonitorCommand(commandReceived, queryMap);
            if (eventBusRequest != null) {
                theEventBus.post(eventBusRequest);
                responseString = eventBusRequest.response.toJson();
                log.info("Command :" + commandReceived + " decoded as :" + eventBusRequest + ", response: " + responseString);

            } else {
                String errMsg = "Received UNKNOWN Command: " + commandReceived;
                responseString = new EBResponseNOK(1,errMsg).toJson();
                log.severe(errMsg);

            }


        } catch (Exception e) {
            String errMsg = "Error elaborating command: " + commandReceived + ", " + e.getMessage();
            responseString = errMsg;
            log.severe(errMsg);
        } finally {
            log.info("Response to request: " + commandReceived + ", is: " + responseString);
            response.setContentType("text/html;charset=utf-8");
            response.getWriter().println(responseString);
            baseRequest.setHandled(true);
            response.setStatus(HttpServletResponse.SC_OK);

        }
    }


}
