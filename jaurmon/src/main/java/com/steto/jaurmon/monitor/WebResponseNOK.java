package com.steto.jaurmon.monitor;

import com.google.gson.Gson;

/**
 * Created by stefano on 26/12/15.
 */
public class WebResponseNOK extends WebResponse
{
    public class _Error
    {
        public Integer code;
        public String message;
    }
    public _Error error = new _Error();

    public WebResponseNOK(int errorCode, String errorString) {
        error.code=errorCode;
        error.message = errorString;
    }


    public static void  main(String[] args)
    {

        WebResponseNOK webResponseNOK = new WebResponseNOK(400, "messaggio");


        System.out.print(webResponseNOK.toJson())     ;
    }


}