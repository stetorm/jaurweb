package com.steto.monitor.webserver;

/**
 * Created by stefano on 12/12/14.
 */

import com.google.common.eventbus.EventBus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;

import java.util.logging.Logger;


public class AuroraWebServer implements Runnable {

    protected Logger log = Logger.getLogger(getClass().getSimpleName());

    private final EventBus theEventBus;
    private final int port;
    private final String resourcePath;
    private Server server;

    public AuroraWebServer(int port, String path, EventBus eventBus) {
        this.theEventBus = eventBus;
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
        cmdContext.setHandler(new InverterCmdHandler(theEventBus));

        ContextHandler pvoutputContext = new ContextHandler();
        pvoutputContext.setContextPath("/pvoutput");
        pvoutputContext.setHandler(new InverterCmdHandler(theEventBus));

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[]{
                        resourceContext, cmdContext, pvoutputContext
                }
        );

        server.setHandler(contexts);


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




    public void stop() throws Exception {
        server.stop();
    }
}