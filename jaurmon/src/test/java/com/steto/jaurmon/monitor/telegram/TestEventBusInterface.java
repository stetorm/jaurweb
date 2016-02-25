package com.steto.jaurmon.monitor.telegram;


import com.google.common.eventbus.EventBus;
import com.steto.jaurmon.monitor.MonitorMsgDailyMaxPower;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
    public void shouldSendMessageUponMaxPowerNotification() {

        float maxpower = 1850;
        String message = "Picco di Potenza giornaliero";

        telegramPlg.setExePath("/usr/bin/telegram-cli");
        telegramPlg.setDestinationContact("Stefano_Brega");
        telegramPlg.setMaxPowerMessage(message);

        String expectedCommand = "/usr/bin/telegram-cli -W -e \"msg Stefano_Brega "+message+": "+maxpower+"\"";

        ArgumentCaptor<String> commandCapture = ArgumentCaptor.forClass(String.class);

        //Exercise
        MonitorMsgDailyMaxPower monitorMsgInverterStatus = new MonitorMsgDailyMaxPower(maxpower);
        theEventBus.post(monitorMsgInverterStatus);

        //Verify
        verify(commandExecutor, times(1)).execute(commandCapture.capture());
        String commandExecuted = commandCapture.getValue();
        assertEquals( expectedCommand,commandExecuted);
        System.out.println(commandExecuted);
    }
}


