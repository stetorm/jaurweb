package com.steto.jaurmon.monitor;

/**
 * Created by stefano on 26/12/15.
 */
public class WebResponseOK extends WebResponse {

    public class _Data {
        public String value;
    }

    public _Data data = new _Data();


    public WebResponseOK(String payload) {
        data.value = payload;
    }

    public static void main(String[] args) {

        WebResponseOK webResponseOK = new WebResponseOK("400");
        System.out.print(webResponseOK.toJson());
    }


}