package com.steto.jaurmon.monitor;

import com.steto.jaurmon.utils.HttpUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by stefano on 19/12/14.
 */

class PvOutputHttpRequestHandler extends AbstractHandler {

    private final FakePVOutputServer fakePVOutputServer;


    public PvOutputHttpRequestHandler(FakePVOutputServer fakePVOutputServer) {
        this.fakePVOutputServer = fakePVOutputServer;
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {

        response.setContentType("text/html;charset=utf-8");

        fakePVOutputServer.requestQueue.add(request.getQueryString());
        System.out.println("request: " + request);
        try {
            if (fakePVOutputServer.getResponseDelay()>0)

            Thread.sleep(fakePVOutputServer.getResponseDelay());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Map<String,String> queyMap = HttpUtils.getQueryMap(request.getQueryString());
        String a =  queyMap.get("key");
        String b =  queyMap.get("sid");
        if (queyMap.get("key").equals(fakePVOutputServer.key) &&
                queyMap.get("sid").equals(fakePVOutputServer.systemId.toString())) {

            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }


        baseRequest.setHandled(true);

    }


}

public class FakePVOutputServer implements Runnable {


    private Integer servicePort;
    public Integer systemId;
    public String key;
    public String pvOutUrl;
    String lastRequest = null;
    Queue<String> requestQueue = new LinkedList<>();
    private long responseDelay;
    private Server server;


    public FakePVOutputServer(Integer port, String pvOutKey, Integer pvOutSystemId, String pvOutServiceUrl) {
        this.pvOutUrl = pvOutServiceUrl;
        this.key = pvOutKey;
        this.systemId = pvOutSystemId;
        this.servicePort = port;

    }


    public String getLastRequest() {

        return requestQueue.peek();
    }

    public String pollLastRequest() {

        return requestQueue.poll();
    }


    public void stop() throws Exception {
        server.stop();
        requestQueue.clear();
    }
    @Override
    public void run() {

        server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(servicePort);
        server.addConnector(connector);


        // Http get comandi
        ContextHandler cmdContext = new ContextHandler();
        cmdContext.setContextPath(pvOutUrl);
        cmdContext.setHandler(new PvOutputHttpRequestHandler(this));


        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[]{
                        cmdContext
                }
        );

        server.setHandler(contexts);


        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws Exception {

 /* Configure the http server */
        new Thread(new FakePVOutputServer(8080, "aKey", 1234, "/")).start();
    }


    public void setResponseDelay(long responseDelay) {
        this.responseDelay = responseDelay;
    }
    public long getResponseDelay() {
        return this.responseDelay ;
    }

    public String waitForRequest(long wait) throws InterruptedException {
        long timestamp = new Date().getTime();
        while( null==getLastRequest() && ((new Date().getTime()-timestamp)<wait) )
        {
            Thread.sleep(200);
        }
        return getLastRequest();
    }
}