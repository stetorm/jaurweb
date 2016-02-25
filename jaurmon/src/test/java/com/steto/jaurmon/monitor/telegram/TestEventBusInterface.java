package com.steto.jaurmon.monitor.telegram;


import com.google.common.eventbus.EventBus;
import com.steto.jaurmon.monitor.MonitorMsgDailyMaxPower;
import com.steto.jaurmon.monitor.MonitorMsgInverterStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import javax.security.auth.login.LoginContext;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by stefano on 24/02/16.
 */

public class TestEventBusInterface {


    EventBus theEventBus ;

    @Mock
    private CommandExecutor commandExecutor = mock(CommandExecutor.class);

    @InjectMocks
    private TelegramPlg telegramPlg ;

    @Before
    public void setUp() throws Exception {

        theEventBus = new EventBus();
        telegramPlg = new TelegramPlg(theEventBus);
        // va re-inizializzato con l'EventBus vero
        MockitoAnnotations.initMocks(this);

    }
    @Test
    public void should()
    {

        ArgumentCaptor<String> commandCapture = ArgumentCaptor.forClass(String.class);


        MonitorMsgDailyMaxPower monitorMsgInverterStatus = new MonitorMsgDailyMaxPower(1850);
        theEventBus.post(monitorMsgInverterStatus);

        verify(commandExecutor,times(1)).execute(commandCapture.capture());
        String commandExecuted =  commandCapture.getValue();
        assertEquals(commandExecuted, "ok");
    }
}


