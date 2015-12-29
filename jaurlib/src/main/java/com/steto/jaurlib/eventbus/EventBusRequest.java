package com.steto.jaurlib.eventbus;


import java.util.Map;

/**
 * Created by stefano on 23/12/14.
 */
public abstract class EventBusRequest {
    public final Map<String, String> paramsMap;
    public EBResponse response=null;

    protected EventBusRequest(Map<String, String> params) {
        this.paramsMap = params;
    }

    public EventBusRequest() {
        paramsMap=null;
    }

    public EBResponse getResponse()
    {
        return response;
    }
}
