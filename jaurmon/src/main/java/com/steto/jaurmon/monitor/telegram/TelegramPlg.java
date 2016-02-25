package com.steto.jaurmon.monitor.telegram;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.steto.jaurmon.monitor.MonitorMsgDailyMaxPower;

import java.util.logging.Logger;

/**
 * Created by stefano on 24/02/16.
 */
public class TelegramPlg {

    Logger log = Logger.getLogger(getClass().getSimpleName());
    private EventBus eventBus;
    private CommandExecutor commandExecutor = new CommandExecutor();
    private String executablePath = "./telegram-cli";
    private String destionationUser = "";
    private String command = "";
    private String maxPowerMessage="";

    public TelegramPlg(EventBus aEventBus) {
        this.eventBus = aEventBus;
        System.out.println(commandExecutor);
        this.eventBus.register(this);
    }

    @Subscribe
    public void handle(MonitorMsgDailyMaxPower maxPower) {

        try {
            String command = composeCommand(maxPowerMessage+": "+maxPower.value);
            System.out.print(commandExecutor.execute(command));
        } catch (Exception ex) {
            log.severe("Error handling msg:" + maxPower + ", executing command: " + ex.getMessage());
        }
    }


    public void setExePath(String executablePath) {
        this.executablePath = executablePath;
    }

    public void setDestinationContact(String userName) {
        this.destionationUser = userName;
    }


    private String composeCommand(String aMessage) {
        return executablePath + " -W -e \"msg " + destionationUser + " " + aMessage+"\"";

    }

    public void setMaxPowerMessage(String message) {
        maxPowerMessage = message;
    }
}
