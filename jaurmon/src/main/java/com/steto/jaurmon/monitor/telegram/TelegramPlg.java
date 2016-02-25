package com.steto.jaurmon.monitor.telegram;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.steto.jaurmon.monitor.MonitorMsgDailyMaxPower;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by stefano on 24/02/16.
 */
public class TelegramPlg {
    private  EventBus eventBus;
    private  CommandExecutor commandExecutor= new CommandExecutor();

    public TelegramPlg(EventBus aEventBus) {
        this.eventBus= aEventBus;
        System.out.println(commandExecutor);
        this.eventBus.register(this);
    }

    @Subscribe
    public void handle(MonitorMsgDailyMaxPower maxPower)
    {

      System.out.print(commandExecutor.execute("comando"));
    }

    private String executeCommand(String command) {

        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();

    }
}
