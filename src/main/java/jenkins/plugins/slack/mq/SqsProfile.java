/**
 * Code originally from https://github.com/jenkinsci/github-sqs-plugin
 */
package jenkins.plugins.slack.mq;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import hudson.model.AbstractDescribableImpl;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SqsProfile to access SQS
 *
 * @author aaronwalker
 */
public class SqsProfile extends AbstractDescribableImpl<SqsProfile> implements AWSCredentials{

    public final String awsAccessKeyId;
    public final Secret awsSecretAccessKey;
    public final String sqsQueue;
    public final List<String> channels;

    static final String queueUrlRegex = "^https://sqs\\.(.+?)\\.amazonaws\\.com/(.+?)/(.+)$";
    static final Pattern endpointPattern = Pattern.compile("(sqs\\..+?\\.amazonaws\\.com)");
    private final boolean urlSpecified;
    private AmazonSQS client;


    @DataBoundConstructor
    public SqsProfile(String awsAccessKeyId, Secret awsSecretAccessKey, String sqsQueue, List<String> channels) {
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretAccessKey = awsSecretAccessKey;
        this.sqsQueue = sqsQueue;
        this.urlSpecified = Pattern.matches(queueUrlRegex, sqsQueue);
        this.channels = channels;
        this.client = null;
    }

    public String getAWSAccessKeyId() {
        return awsAccessKeyId;
    }

    public String getAWSSecretKey() {
        return awsSecretAccessKey.getPlainText();
    }

    public List<String> getChannels() {

        return channels;
    }

    public AmazonSQS getSQSClient() {
        if(client == null) {
            client =  new AmazonSQSClient(this);
            if(urlSpecified) {
                Matcher endpointMatcher = endpointPattern.matcher(getSqsQueue());
                if(endpointMatcher.find()) {
                    String endpoint = endpointMatcher.group(1);
                    client.setEndpoint(endpoint);
                }
            }
        }
        return client;
    }

    public String getSqsQueue() {
        return sqsQueue;
    }

    public String getQueueUrl() {
        return urlSpecified ? sqsQueue
                            : createQueue(getSQSClient(), sqsQueue);
    }

    /**
     * Create a Amazon SQS queue if it does already exists
     * @param sqs  Amazon SQS client
     * @param queue the name of the queue
     * @return  the queue url
     */
    private String createQueue(AmazonSQS sqs, String queue) {
        for(String url : sqs.listQueues().getQueueUrls()) {
            if(url.endsWith("/" + queue)) {
                return url;
            }
        }
        //The queue wasn't found so we will create it
        return sqs.createQueue(new CreateQueueRequest(queue)).getQueueUrl();
    }
}
