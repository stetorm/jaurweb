package com.steto.jaurinv;

import com.steto.jaurlib.modbus.MB_PDU;

/**
 * Created by sbrega on 02/12/2014.
 */
public abstract class Inverter {
    public abstract MB_PDU createResponse(MB_PDU auroraRequest);


}
