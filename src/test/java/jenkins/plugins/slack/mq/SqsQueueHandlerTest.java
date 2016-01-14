package jenkins.plugins.slack.mq;

import hudson.util.SequentialExecutionQueue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * JUnit test for the SqsQueueHandler class
 */
@RunWith(MockitoJUnitRunner.class)
public class SqsQueueHandlerTest {

    @Mock
    private SequentialExecutionQueue queue;

    @Spy
    @InjectMocks
    private SqsQueueHandler sqsQueueHandler;


    @Test
    public void testReceiverGetsPutOntoQueueSuccessfully() throws Exception {

        SqsProfile sqsProfile = new SqsProfile("123456", null, "Test-Queue");

        doReturn(null).when(sqsQueueHandler).getGlobalConfiguration();

        doReturn(false).when(sqsQueueHandler).getQueueInProgress();

        doReturn(true).when(sqsQueueHandler).isQueueEnabled(null);

        doReturn(sqsProfile).when(sqsQueueHandler).getSqsProfile(null);

        given(queue.getInProgress()).willReturn(new HashSet<Runnable>());

        sqsQueueHandler.doRun();

        verify(sqsQueueHandler, times(1)).getGlobalConfiguration();
        verify(sqsQueueHandler, times(1)).getSqsProfile(null);
    }


    @Test
    public void testReceiverGetsPutOntoQueueNotEnabledSuccessfully() throws Exception {

        SqsProfile sqsProfile = new SqsProfile("123456", null, "Test-Queue");

        doReturn(null).when(sqsQueueHandler).getGlobalConfiguration();

        doReturn(false).when(sqsQueueHandler).getQueueInProgress();

        doReturn(false).when(sqsQueueHandler).isQueueEnabled(null);

        doReturn(sqsProfile).when(sqsQueueHandler).getSqsProfile(null);

        given(queue.getInProgress()).willReturn(new HashSet<Runnable>());

        sqsQueueHandler.doRun();

        verify(sqsQueueHandler, times(1)).getGlobalConfiguration();
        verify(sqsQueueHandler, times(0)).getSqsProfile(null);
    }

    @Test
    public void testReceiverNotPutOntoQueueSuccessfully() throws Exception {

        Set<Runnable> runnable = new HashSet<Runnable>();

        runnable.add(new Runnable() {
            public void run() {

            }
        });

        runnable.add(new Runnable() {
            public void run() {

            }
        });

        SqsProfile sqsProfile = new SqsProfile("123456", null, "Test-Queue");

        doReturn(null).when(sqsQueueHandler).getGlobalConfiguration();

        doReturn(true).when(sqsQueueHandler).getQueueInProgress();

        doReturn(true).when(sqsQueueHandler).isQueueEnabled(null);

        doReturn(sqsProfile).when(sqsQueueHandler).getSqsProfile(null);

        given(queue.getInProgress()).willReturn(runnable);

        sqsQueueHandler.doRun();

        verify(sqsQueueHandler, times(1)).getGlobalConfiguration();
        verify(sqsQueueHandler, times(0)).getSqsProfile(null);
    }
}