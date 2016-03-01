package com.steto.monitor.telegram;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.steto.monitor.MonitorMsgDailyMaxPower;
import com.steto.monitor.MonitorMsgStarted;

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
    private String maxPowerMessage="Picco di Potenza giornaliero";

    public TelegramPlg(EventBus aEventBus) {
        this.eventBus = aEventBus;
        this.eventBus.register(this);
    }

    @Subscribe
    public void handle(MonitorMsgDailyMaxPower maxPower) {

        try {
            String[] command = composeCommand(maxPowerMessage+": "+maxPower.value);

            String result = commandExecutor.execute(command);

            String strCommand = "";

            for (String part :command)
            {
                strCommand+= part  +" ";
            }

            log.info("Executed command: "+strCommand+ ",result: "+result);
        } catch (Exception ex) {
            log.severe("Error handling msg:" + maxPower + ", executing command: "+command+", " + ex.getMessage());
        }
    }

    @Subscribe
    public void handle(MonitorMsgStarted msg) {

        try {
            log.info("Handling MonitorMsgStarted");
            String[] command = composeCommand("System Rebooted.");

            String result = commandExecutor.execute(command);

            String strCommand = "";

            for (String part :command)
            {
                strCommand+= part  +" ";
            }
            result=result.replaceAll("[^\\w]"," ").trim().replaceAll(" +", " ");
            log.info("Executed command: " + strCommand + ",result: " + result);
        } catch (Exception ex) {
            log.severe("Error handling msg:" + msg + ", executing command: "+command+", " + ex.getMessage());
        }
    }


    public void setExePath(String executablePath) {
        this.executablePath = executablePath;
    }

    public void setDestinationContact(String userName) {
        this.destionationUser = userName;
    }


    private String[] composeCommand(String aMessage) {


        String text =  "msg @dest @msg".replace("@dest",destionationUser).replace("@msg",aMessage);
        String[] result =  {executablePath,"-W","-e",text};

        return result;

    }

    public void setMaxPowerMessage(String message) {
        maxPowerMessage = message;
    }
}
