package com.steto.jaurinv.unit;

import com.steto.jaurlib.response.AResp_ProductNumber;
import com.steto.jaurlib.response.AResp_String6;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by sbrega on 12/12/2014.
 */
public class TestResponse {

    @Test
    public void should() throws Exception {
        String number= "12345";
        AResp_String6 response = new AResp_ProductNumber();
        response.set(number);
        assertEquals(number,response.get());

        number= "1";
        response.set(number);
        assertEquals(number,response.get());

        number= "1234567";
        response.set(number);
        assertEquals("123456",response.get());
    }

}
