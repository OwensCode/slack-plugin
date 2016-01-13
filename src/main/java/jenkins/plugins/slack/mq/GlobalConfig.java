package jenkins.plugins.slack.mq;

import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.plugins.slack.SlackListener;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class GlobalConfig extends GlobalConfiguration {
	
	private static final Logger logger = Logger.getLogger(GlobalConfig.class.getName());

	private boolean slackEnableSqsIntegration = false;
    private String slackSqsQueue;
    private String slackAwsAccessKeyId;
    private Secret slackAwsSecretAccessKey;
    private String slackTokenForSqsIntegration;

    public GlobalConfig() {
        load();
    }

    public FormValidation doCheckSlackSqsQueue(@QueryParameter String value) {
        if (slackEnableSqsIntegration && StringUtils.isBlank(value)) {
            return FormValidation.warning("Please set a queue name or URL");
        }
        return FormValidation.ok();
    }
    
    public FormValidation doCheckSlackAwsAccessKeyId(@QueryParameter String value) {
        if (slackEnableSqsIntegration && StringUtils.isBlank(value)) {
            return FormValidation.warning("Please set an access key id");
        }
        return FormValidation.ok();
    }
    
    public FormValidation doCheckSlackAwsSecretAccessKey(@QueryParameter String value) {
        if (slackEnableSqsIntegration && StringUtils.isBlank(value)) {
            return FormValidation.warning("Please set a secret access key");
        }
        return FormValidation.ok();
    }
    
    public FormValidation doCheckSlackTokenForSqsIntegration(@QueryParameter String value) {
        if (slackEnableSqsIntegration && StringUtils.isBlank(value)) {
            return FormValidation.warning("Please set a token from Slack integration");
        }
        return FormValidation.ok();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json)
            throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    public FormValidation doTestConnection(
            @QueryParameter("slackSqsQueue") final String sqsQueue,
            @QueryParameter("slackAwsAccessKeyId") final String awsAccessKeyId,
            @QueryParameter("slackAwsSecretAccessKey") final Secret awsSecretAccessKey)
            throws FormException {
    	
		if (StringUtils.isBlank(sqsQueue)
				|| StringUtils.isBlank(awsAccessKeyId)
				|| StringUtils.isBlank(Secret.toString(awsSecretAccessKey))) {
			return FormValidation
					.error("Please provide all configuration values");
		}
        
    	try {
            SqsProfile profile = new SqsProfile(awsAccessKeyId, awsSecretAccessKey, sqsQueue);
            String queue = profile.getQueueUrl();
            if(queue != null) {
                return FormValidation.ok("Verified SQS Queue " + queue);
            } else {
                return FormValidation.error("Failed to validate the account");
            }
        } catch (RuntimeException ex) {
        	logger.log(Level.WARNING, "Failed to validate the Amazon SQS configuration: " + ex.getMessage());
            return FormValidation.error("Failed to validate the account. Check the Jenkins log files.");
        }
    }

	public String getSlackSqsQueue() {
		return slackSqsQueue;
	}

	public void setSlackSqsQueue(String slackSqsQueue) {
		this.slackSqsQueue = slackSqsQueue;
	}

	public String getSlackAwsAccessKeyId() {
		return slackAwsAccessKeyId;
	}

	public void setSlackAwsAccessKeyId(String slackAwsAccessKeyId) {
		this.slackAwsAccessKeyId = slackAwsAccessKeyId;
	}

	public Secret getSlackAwsSecretAccessKey() {
		return slackAwsSecretAccessKey;
	}

	public void setSlackAwsSecretAccessKey(Secret slackAwsSecretAccessKey) {
		this.slackAwsSecretAccessKey = slackAwsSecretAccessKey;
	}

	public String getSlackTokenForSqsIntegration() {
		return slackTokenForSqsIntegration;
	}

	public void setSlackTokenForSqsIntegration(
			String slackTokenForSqsIntegration) {
		this.slackTokenForSqsIntegration = slackTokenForSqsIntegration;
	}
	
	public boolean isSlackEnableSqsIntegration() {
		return slackEnableSqsIntegration;
	}
	
	public void setSlackEnableSqsIntegration(boolean slackEnableSqsIntegration) {
		this.slackEnableSqsIntegration = slackEnableSqsIntegration;
	}
}
