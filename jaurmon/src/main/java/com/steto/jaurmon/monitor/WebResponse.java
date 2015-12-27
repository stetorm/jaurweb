package com.steto.jaurmon.monitor;

import com.google.gson.Gson;

/**
 * Created by stefano on 26/12/15.
 */
public abstract class WebResponse
{

    public String toJson()
    {

       return new Gson().toJson(this)  ;

    }


}