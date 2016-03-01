package com.steto.jaurmon.monitor;

/**
 * Created by stefano on 14/02/16.
 */
public class MonitorMsgDailyMaxPower {
    public long timestamp=0;
    public float value = 0;

    public MonitorMsgDailyMaxPower(float value, long time) {
        this.value = value;
        this.timestamp = time;
    }

    @Override
    public String toString() {
        return "MonitorMsgDailyMaxPower{" +
                "timestamp=" + timestamp +
                ", value=" + value +
                '}';
    }
}
