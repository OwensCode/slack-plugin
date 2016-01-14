package jenkins.plugins.slack.mq;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * JUnit test for the SqsQueueReceiver class
 */
@RunWith(MockitoJUnitRunner.class)
public class SqsQueueReceiverTest {

    @Mock
    private SqsProfile profile;

    @Mock
    private AmazonSQS sqs;

    @Spy
    @InjectMocks
    private SqsQueueReceiver sqsQueueReceiver;


    @Test
    public void testReceiverCallsSqsWithNoMessagesSuccessfully() throws Exception {

        given(profile.getQueueUrl()).willReturn("http://amazon.test.url");

        given(sqs.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(new ReceiveMessageResult());

        sqsQueueReceiver.run();

        verify(sqs, times(0)).deleteMessage(any(DeleteMessageRequest.class));
    }


    @Test
    public void testReceiverCallsSqsWith1MessageSuccessfully() throws Exception {

        ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult();
        Message message = new Message();
        message.setBody("{\"text\":\"test body\"}");
        List<Message> messages = new ArrayList<Message>();
        messages.add(message);
        receiveMessageResult.setMessages(messages);

        AmazonSQSClient amazonSQSClient = mock(AmazonSQSClient.class);

        given(profile.getQueueUrl()).willReturn("http://amazon.test.url");

        given(profile.getSQSClient()).willReturn(amazonSQSClient);

        given(amazonSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(receiveMessageResult);

        sqsQueueReceiver.run();

        verify(amazonSQSClient, times(1)).deleteMessage(any(DeleteMessageRequest.class));
    }
}