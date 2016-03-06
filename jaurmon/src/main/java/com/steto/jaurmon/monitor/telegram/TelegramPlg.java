package com.steto.jaurmon.monitor.telegram;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.steto.jaurmon.monitor.MonitorMsgDailyMaxPower;
import com.steto.jaurmon.monitor.MonitorMsgStarted;

import java.util.logging.Level;
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
    private String[] command ;
    private String maxPowerMessageDescription = "Picco di Potenza giornaliero";

    public TelegramPlg(EventBus aEventBus) {
        this.eventBus = aEventBus;
        this.eventBus.register(this);
    }

    @Subscribe
    public void handle(final MonitorMsgDailyMaxPower maxPowerMsg) {

        log.fine("Handling:  " + maxPowerMsg);
        new Thread(new Runnable() {
            @Override

            public void run() {
                String strCommand = "";
                for (String part : command) {
                    strCommand += part + " ";
                }
                try {


                   command = composeCommand(maxPowerMessageDescription + ": " + maxPowerMsg.value);

                    int result = commandExecutor.execute(command,60000);
                    String outputString = commandExecutor.getOutputString();



                    log.info("Executed command: " + strCommand + ",result: " + result + ", output: " + outputString);
                } catch (Exception ex) {
                    log.log(Level.SEVERE,"Error handling msg:" + maxPowerMsg + ", executing command: " + strCommand + ", " + ex.getMessage(), ex);

                }
            }
        }).start();

    }

    @Subscribe
    public void handle(final MonitorMsgStarted msg) {

        log.fine("Handling:  " + msg);
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    log.info("Handling MonitorMsgStarted");
                    String[] command = composeCommand("System Rebooted.");

                    int result = commandExecutor.execute(command,60000);
                    String output = commandExecutor.getOutputString();

                    String strCommand = "";

                    for (String part : command) {
                        strCommand += part + " ";
                    }

                    log.info("Executed command: " + strCommand + ",result: " + result+ ",output: "+output);
                } catch (Exception ex) {
                    log.severe("Error handling msg:" + msg + ", executing command: " + command + ", " + ex.getMessage());
                }

            }
        }).start();

    }


    public void setExePath(String executablePath) {
        this.executablePath = executablePath;
    }

    public void setDestinationContact(String userName) {
        this.destionationUser = userName;
    }


    private String[] composeCommand(String aMessage) {


        String text = "msg @dest @msg".replace("@dest", destionationUser).replace("@msg", aMessage);
        String[] result = {executablePath, "-W", "-e", text};

        return result;

    }

    public void setMaxPowerMessage(String message) {
        maxPowerMessageDescription = message;
    }
}
