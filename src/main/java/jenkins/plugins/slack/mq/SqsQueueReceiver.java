package jenkins.plugins.slack.mq;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jenkins.plugins.slack.SlackMessageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Check the queue for messages and process them.
 */

public class SqsQueueReceiver implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(SqsQueueReceiver.class.getName());

    private SqsProfile profile;

    private AmazonSQS sqs;

    public SqsQueueReceiver(SqsProfile profile) {
        this.profile = profile;
    }

    protected void setSqs(AmazonSQS sqs) {

        this.sqs = sqs;
    }

    public void run() {
        LOGGER.info("looking for build triggers on queue: " + profile.sqsQueue);
        sqs = profile.getSQSClient();
        String queueUrl = profile.getQueueUrl();
        SlackMessageProcessor processor = new SlackMessageProcessor();
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        receiveMessageRequest.setWaitTimeSeconds(20);
        List<Message> messages = new ArrayList<Message>();
        // Try to pick up the messages from SQS, and log if an error was encountered,
        // for example a 403 Access to the resource is denied.
        try {
            messages = sqs.receiveMessage(receiveMessageRequest).getMessages();

        } catch (Exception ex) {
            LOGGER.warning("Unable to retrieve messages from the queue. " + ex.getMessage());
        }

        for (Message message : messages) {
            //Process the message payload
            try {
                LOGGER.info("got payload -" + message.getBody());
                SqsResponse response = new ObjectMapper().readValue(message.getBody(), SqsResponse.class);
                LOGGER.info("parsed json -" + response);
                processor.process(message.getBody());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE,"unable to trigger builds " + ex.getMessage(),ex);
            } finally {
                //delete the message even if it failed
                sqs.deleteMessage(new DeleteMessageRequest()
                        .withQueueUrl(queueUrl)
                        .withReceiptHandle(message.getReceiptHandle()));
            }
        }
    }
}
