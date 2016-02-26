package com.steto.jaurmon.monitor.telegram;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by stefano on 24/02/16.
 */
public class CommandExecutor {
    public String execute(String[] command) {
        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();

    }
}
