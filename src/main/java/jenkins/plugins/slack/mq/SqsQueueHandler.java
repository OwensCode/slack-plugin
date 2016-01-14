package jenkins.plugins.slack.mq;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.util.SequentialExecutionQueue;
import hudson.util.TimeUnit2;
import jenkins.model.GlobalConfiguration;

import java.util.concurrent.Executors;
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
        GlobalConfig globalConfig = getGlobalConfiguration();

        if (isQueueEnabled(globalConfig) && !getQueueInProgress()) {
            SqsProfile profile = getSqsProfile(globalConfig);

            queue.setExecutors(Executors.newFixedThreadPool(1));
            queue.execute(new SqsQueueReceiver(profile));
        } else {
            LOGGER.fine("Currently Waiting for Messages from Queues");
        }
    }

    /**
     * Return the Jenkins global configuration
     * @return GlobalConfig globalConfig
     */
    protected GlobalConfig getGlobalConfiguration() {
        return GlobalConfiguration.all().get(GlobalConfig.class);
    }

    /**
     * Create a new SqsProfile based upon the global configuration
     * @param globalConfig the Jenkins global configuration
     * @return SqsProfile the populated profile
     */
    protected SqsProfile getSqsProfile(GlobalConfig globalConfig) {
        return new SqsProfile(globalConfig.getSlackAwsAccessKeyId(), globalConfig.getSlackAwsSecretAccessKey(),
                globalConfig.getSlackSqsQueue());
    }

    /**
     * Determine if the queue has an in progress jobs
     * @return boolean true if jobs are in progress, false if no jobs in progress
     */
    protected boolean getQueueInProgress() {
        return queue.getInProgress().size() != 0;
    }

    /**
     * Determine if SQS is enabled
     * @param globalConfig the Jenkins global configuration
     * @return true if enabled, false if not
     */
    protected boolean isQueueEnabled(GlobalConfig globalConfig) {
        return globalConfig.isEnabled();
    }


    public static SqsQueueHandler get() {
        return PeriodicWork.all().get(SqsQueueHandler.class);
    }
}
