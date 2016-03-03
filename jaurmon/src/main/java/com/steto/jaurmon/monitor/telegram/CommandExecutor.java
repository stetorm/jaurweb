package com.steto.jaurmon.monitor.telegram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.*;

/**
 * Created by stefano on 24/02/16.
 */
public class CommandExecutor {

    String outcome="";
    int errorCode=0;
    public int execute(String[] command) throws IOException, InterruptedException {
        StringBuffer output = new StringBuffer();

        Process p;
        p = Runtime.getRuntime().exec(command);
        p.waitFor();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = "";
        while ((line = reader.readLine()) != null) {
            output.append(line + "\n");
        }


        errorCode  = p.waitFor();
        outcome = output.toString();
        return errorCode;

    }

    public int execute(String[] command, long timeoutMsec) throws IOException, ExecutionException, InterruptedException, TimeoutException {

        final Process p = Runtime.getRuntime().exec(command);
        Callable<Integer> call = new Callable<Integer>() {
            public Integer call() throws Exception {
                p.waitFor();
                StringBuffer output = new StringBuffer();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(p.getInputStream()));

                String line = "";
                while ((line = reader.readLine()) != null) {
                    output.append(line + "\n");
                }

                outcome = output.toString();
                return p.exitValue();
            }
        };
        Future<Integer> ft = Executors.newSingleThreadExecutor().submit(call);
        try {

            errorCode = ft.get(timeoutMsec, TimeUnit.MILLISECONDS);
            return errorCode;
        } catch (TimeoutException to) {
            p.destroy();
            throw to;
        }
    }

    public String getOutputString() {
        return outcome.replaceAll("[^\\w]", " ").trim().replaceAll(" +", " ");
    }
}
