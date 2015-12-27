package com.steto.jaurmon.monitor.webserver;

import com.google.common.eventbus.EventBus;
import com.steto.jaurlib.request.AuroraCumEnergyEnum;
import com.steto.jaurmon.monitor.cmd.*;
import com.steto.jaurmon.utils.HttpUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
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

    protected MonCmdInverter createMonitorInverterCommand(Map map) {

        Map<String, AuroraCumEnergyEnum> mapEnergyCmd = new HashMap<>();

        mapEnergyCmd.put("daily", AuroraCumEnergyEnum.DAILY);
        mapEnergyCmd.put("weekly", AuroraCumEnergyEnum.WEEKLY);
        mapEnergyCmd.put("monthly", AuroraCumEnergyEnum.MONTHLY);
        mapEnergyCmd.put("yearly", AuroraCumEnergyEnum.YEARLY);
        mapEnergyCmd.put("last7days", AuroraCumEnergyEnum.LAST7DAYS);
        mapEnergyCmd.put("partial", AuroraCumEnergyEnum.PARTIAL);
        mapEnergyCmd.put("total", AuroraCumEnergyEnum.TOTAL);

        String opCodeParameter = (String) map.get("opcode");
        String subCodeParameter = (String) map.get("subcode");
        int addressParameter = Integer.parseInt((String) map.get("address"));
        MonCmdInverter result = null;
        switch (opCodeParameter) {
            case "cumEnergy":
                AuroraCumEnergyEnum period = mapEnergyCmd.get(subCodeParameter);
                result = new InvCmdCumEnergy(addressParameter, period);
                break;
            case "productNumber":
                result = new InvCmdProductNumber(addressParameter);
                break;
            case "serialNumber":
                result = new InvCmdSerialNumber(addressParameter);
                break;
            case "versionNumber":
                result = new InvCmdVersionNumber(addressParameter);
                break;
            case "firmwareNumber":
                result = new InvCmdFirmwareVersion(addressParameter);
                break;
            case "manufacturingDate":
                result = new InvCmdMFGdate(addressParameter);
                break;
            case "sysConfig":
                result = new InvCmdSysConfig(addressParameter);
                break;
            case "timeCounter":
                result = new InvCmdTimeCounter(addressParameter);
                break;
            case "actualTime":
                result = new InvCmdActualTime(addressParameter);
                break;
            default:
                log.severe("Received unknown inverter command, with opcode: "+opCodeParameter);

        }


        return result;

    }

    protected MonitorCommand createMonitorCommand(String cmdUrl, Map cmdParams) {

        MonitorCommand result = null;
        switch (cmdUrl) {
            case "saveSettings":
                result = new MonCmdSaveSettings(cmdParams);
                break;
            case "saveCfg":
                result = new MonCmdSavePvOutputConfig(cmdParams);
                break;
            case "loadCfg":
                result = new MonCmdLoadConfig();
                break;
            case "inv":
                result = createMonitorInverterCommand(cmdParams);
                break;
            case "status":
                result = new MonCmdReadStatus();
                break;
            case "pvOutputTest":
                // TODO
                // responseString = theEventBus.execCommand(new MonCmdTestPVoutput(queryMap));
                break;
            case "pvOutputStart":
                // TODO
                //theEventBus.startPvOutput();
                break;
            case "pvOutputStop":
                // TODO
                // theEventBus.stopPvOutput();
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

            MonitorCommand monitorCommand = createMonitorCommand(commandReceived, queryMap);
            if (monitorCommand != null) {
                theEventBus.post(monitorCommand);
                responseString = monitorCommand.response.toJson();
                log.info("Command :" + commandReceived + " decoded as :" + monitorCommand + ", response: " + responseString);

            } else {
                String errMsg = "Received UNKNOWN Command: " + commandReceived;
                responseString = errMsg;
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
