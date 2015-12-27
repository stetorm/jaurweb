package com.steto.jaurmon.monitor.cmd;

import com.steto.jaurlib.request.AuroraCumEnergyEnum;

/**
 * Created by stefano on 27/12/15.
 */
public class InvCmdCumEnergy extends MonCmdInverter {
    public AuroraCumEnergyEnum period;

    public InvCmdCumEnergy(int addressParameter, AuroraCumEnergyEnum aPeriod) {
        super(addressParameter);
        period = aPeriod;

    }
}
