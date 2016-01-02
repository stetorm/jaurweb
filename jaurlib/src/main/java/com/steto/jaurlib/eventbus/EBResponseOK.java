package com.steto.jaurlib.eventbus;

/**
 * Created by stefano on 26/12/15.
 */
public class EBResponseOK extends EBResponse {

    public class _Data {
        public Object value;
    }

    public _Data data = new _Data();


    public EBResponseOK(Object payload) {
        data.value = payload;
    }

    public static void main(String[] args) {

        EBResponseOK webResponseOK = new EBResponseOK("400");
        System.out.print(webResponseOK.toJson());
    }


}