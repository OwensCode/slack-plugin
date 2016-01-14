package jenkins.plugins.slack;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.util.SequentialExecutionQueue;
import hudson.util.TimeUnit2;
import jenkins.model.GlobalConfiguration;
import jenkins.plugins.slack.mq.GlobalConfig;
import jenkins.plugins.slack.mq.SqsProfile;
import jenkins.plugins.slack.mq.SqsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receives a message from SQS and processes it
 *
 */
@Extension
public class SqsQueueHandler extends PeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(SqsQueueHandler.class.getName());

    private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Executors.newFixedThreadPool(2));

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit2.SECONDS.toMillis(2);
    }

    @Override
    protected void doRun() throws Exception {
        GlobalConfig globalConfig = GlobalConfiguration.all().get(GlobalConfig.class);
        SqsProfile profile = new SqsProfile(globalConfig.getSlackAwsAccessKeyId(), globalConfig.getSlackAwsSecretAccessKey(),
                globalConfig.getSlackSqsQueue());

        if (queue.getInProgress().size() == 0) {
            queue.setExecutors(Executors.newFixedThreadPool(1));
            queue.execute(new SQSQueueReceiver(profile));
        } else {
            LOGGER.fine("Currently Waiting for Messages from Queues");
        }
    }

    public static SqsQueueHandler get() {
        return PeriodicWork.all().get(SqsQueueHandler.class);
    }

    private class SQSQueueReceiver implements Runnable {

        private SqsProfile profile;

        private SQSQueueReceiver(SqsProfile profile) {
            this.profile = profile;
        }

        public void run() {
            LOGGER.info("looking for build triggers on queue: " + profile.sqsQueue);
            AmazonSQS sqs = profile.getSQSClient();
            String queueUrl = profile.getQueueUrl();
            //TriggerProcessor processor = profile.getTriggerProcessor();
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
                    //processor.trigger(message.getBody());
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
}
