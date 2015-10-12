package com.steto.jaurmon.monitor;

/**
 * Created by stefano on 12/12/14.
 */

import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.request.AuroraRequestFactory;
import com.steto.jaurlib.response.AuroraResponseFactory;
import com.steto.jaurmon.utils.HttpUtils;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;


class InverterCmdHandler extends AbstractHandler {
    private AuroraMonitor auroraMonitor;
    protected Logger log = Logger.getLogger(getClass().getSimpleName());


    public InverterCmdHandler(AuroraMonitor auroraMonitor) {
        this.auroraMonitor = auroraMonitor;

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
            MonitorCommand monitorCommand = null;
            Map<String, String> queryMap = null;
            if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
                queryMap = HttpUtils.getQueryMap(request.getQueryString());
            }
            switch (commandReceived) {
                case "saveSettings":
                    responseString = auroraMonitor.execCommand(new MonCmdSaveSettings(queryMap));
                    break;
                case "saveCfg":
                    responseString = auroraMonitor.execCommand(new MonCmdSavePvOutputConfig(queryMap));
                    break;
                case "loadCfg":
                    responseString = auroraMonitor.execCommand(new MonCmdLoadConfig());
                    break;
                case "inv":
                    responseString = auroraMonitor.execCommand(new MonCmdInverter(queryMap));
                    break;
                case "status":
                    responseString = auroraMonitor.execCommand(new MonCmdReadStatus());
                    break;
                case "pvOutputTest":
                    responseString = auroraMonitor.execCommand(new MonCmdTestPVoutput(queryMap));
                    break;
                case "pvOutputStart":
                    auroraMonitor.startPvOutput();
                    break;
                case "pvOutputStop":
                    auroraMonitor.stopPvOutput();
                    break;
                default:
                    String errMsg = "Received UNKNOWN Command: " + commandReceived;
                    responseString = errMsg;
                    log.severe(errMsg);

            }

        } catch (Exception e) {
            String errMsg = "Error elaborating command: " + commandReceived + ", " + e.getMessage();
            responseString = errMsg;
            log.severe(errMsg);
        } finally {
            log.info("Response to request: "+commandReceived+", is: " + responseString);
            response.setContentType("text/html;charset=utf-8");
            response.getWriter().println(responseString);
            baseRequest.setHandled(true);
            response.setStatus(HttpServletResponse.SC_OK);

        }
    }


}

public class AuroraWebServer implements Runnable {

    protected Logger log = Logger.getLogger(getClass().getSimpleName());

    private final AuroraMonitor auroraMonitor;
    private final int port;
    private final String resourcePath;
    private Server server;

    public AuroraWebServer(int port, String path, AuroraMonitor auroraMonitor) {
        this.auroraMonitor = auroraMonitor;
        this.port = port;
        resourcePath = path;
    }

    public void run() {

 /* Configure the http server */
        server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(port);
        server.addConnector(connector);

    /* Resources */
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setAliases(true);
        resourceHandler.setWelcomeFiles(new String[]{
                        "index.html"
                }
        );

        resourceHandler.setResourceBase(resourcePath);
        ContextHandler resourceContext = new ContextHandler();
        resourceContext.setContextPath("/");
        resourceContext.setHandler(resourceHandler);
        resourceContext.setAliases(true);

        // Http get comandi
        ContextHandler cmdContext = new ContextHandler();
        cmdContext.setContextPath("/cmd");
        cmdContext.setHandler(new InverterCmdHandler(auroraMonitor));


        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[]{
                        resourceContext, cmdContext
                }
        );

        server.setHandler(contexts);

        try {
            auroraMonitor.init();
            auroraMonitor.startPvOutput();
            log.info("Web Server Started...");
        } catch (Exception e) {
            log.severe("Problems initializing Aurora Inverter Driver: " + e.getMessage());
        }
        finally
        {
            try {
                server.start();
                server.join();
                log.info("Web Server Started...");
                log.info("Web Server Joined...");
            } catch (Exception e) {
                log.severe("Problems starting Web Server...");
                e.printStackTrace();
            }

        }

    }

    public static SerialPort createSerialPort(String com, int baudRate) throws SerialPortException {
        SerialPort result = new SerialPort(com);
        result.openPort();//Open serial port
        result.setParams(baudRate, 8, 1, 0);//Set params.
        return result;
    }


    public static void main(String[] args) throws Exception {

        Logger log = Logger.getLogger("mainLogger");
        String configurationFileName = "aurora.cfg";
        String logDirectoryPath = "log";
        String workingDirectory = ".";
        String webDirectory = "html/build/web";
        if (args.length > 0) {
            workingDirectory = args[0];
            webDirectory = args[1];
        }

        configurationFileName =  workingDirectory + File.separator + "config" + File.separator + configurationFileName;
        logDirectoryPath =  workingDirectory + File.separator + logDirectoryPath;


        try {
//        String serialPort = "/dev/ttys002";
            String webDirectoryPath = workingDirectory + File.separator + webDirectory;
//        String serialPort = "/dev/ttys001";

            log.info("Creating Aurora Driver...");
            AuroraDriver auroraDriver = new AuroraDriver(null, new AuroraRequestFactory(), new AuroraResponseFactory());


            log.info("Creating Aurora Monitor...");
            AuroraMonitor auroraMonitor = new AuroraMonitor(auroraDriver, configurationFileName, logDirectoryPath);

            log.info("Creating Web Server...");
            AuroraWebServer auroraWebServer = new AuroraWebServer(8080, webDirectoryPath, auroraMonitor);
            log.info("Starting Web Server...");
            new Thread(auroraWebServer).start();
            Thread.sleep(1000);
        } catch (Exception ex) {
            System.out.println("Error at startup: " + ex.getMessage());
            log.severe("Fatal error at startup: " + ex.getMessage());
        }

    }

    public void stop() throws Exception {
        server.stop();
    }
}