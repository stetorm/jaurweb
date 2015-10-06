package com.steto.jaurmon.utils;


import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by stefano on 27/12/14.
 */
public class FormatStringUtils {

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    public static String jsonResult(boolean isResultOk) {
        Map<String, String> mapResult = new HashMap<>();
        String result = isResultOk ? "OK" : "ERROR";
        mapResult.put("result", result);

        return new Gson().toJson(mapResult);

    }

    public static String fromDate(Date aDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_kk");
        return sdf.format(aDate);
    }
}
