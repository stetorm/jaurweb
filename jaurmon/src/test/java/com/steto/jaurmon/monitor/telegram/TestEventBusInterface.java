package com.steto.jaurmon.monitor.telegram;


import com.google.common.eventbus.EventBus;
import com.steto.jaurmon.monitor.MonitorMsgDailyMaxPower;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Created by stefano on 24/02/16.
 */

public class TestEventBusInterface {


    EventBus theEventBus;

    @Mock
    private CommandExecutor commandExecutor = mock(CommandExecutor.class);

    @InjectMocks
    private TelegramPlg telegramPlg;

    @Before
    public void setUp() throws Exception {

        theEventBus = new EventBus();
        telegramPlg = new TelegramPlg(theEventBus);
        // inizializza i mock e gli oggetti iniettati
        MockitoAnnotations.initMocks(this);
        telegramPlg.setDestinationContact("Stefano_Brega");

    }

    @Test
    public void shouldSendMessageUponMaxPowerNotification() throws Exception {

        float maxpower = 1850;
        long timestamp= new Date().getTime();
        String message = "Picco di Potenza giornaliero";

        telegramPlg.setExePath("/usr/bin/telegram-cli");
        telegramPlg.setDestinationContact("Stefano_Brega");
        telegramPlg.setMaxPowerMessage(message);

        String expectedCommand = "/usr/bin/telegram-cli -W -e \"msg Stefano_Brega "+message+": "+maxpower+"\"";

        ArgumentCaptor<String[]> commandCapture = ArgumentCaptor.forClass(String[].class);

        //Exercise
        MonitorMsgDailyMaxPower monitorMsgInverterStatus = new MonitorMsgDailyMaxPower(maxpower, timestamp);
        theEventBus.post(monitorMsgInverterStatus);

        //Verify
        verify(commandExecutor, times(1)).execute(commandCapture.capture());
        String[] commandExecuted = commandCapture.getValue();
        String strCommandExecuted = commandExecuted[0]+" "+commandExecuted[1]+" "+commandExecuted[2]+" \""+commandExecuted[3]+"\"";

        assertEquals( expectedCommand,strCommandExecuted);
        System.out.println(strCommandExecuted);
    }
}


