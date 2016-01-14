package jenkins.plugins.slack.mq;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class GlobalConfig extends GlobalConfiguration {
	
	private static final Logger logger = Logger.getLogger(GlobalConfig.class.getName());

	private boolean enabled;
    private String slackSqsQueue;
    private String slackAwsAccessKeyId;
    private Secret slackAwsSecretAccessKey;
    private String slackTokenForSqsIntegration;
    private String slackChannelsStr;
    private String triggerWord = "jenkins";
    
    public GlobalConfig() {
        load();
        slackChannelsStr = cleanupChannelList(slackChannelsStr);
    }
    
    @Override
    public boolean configure(StaplerRequest req, JSONObject json)
            throws FormException {
    	
    	if (json.containsKey("slackEnableSqsIntegration")) {
    		JSONObject config = json.getJSONObject("slackEnableSqsIntegration");
    		req.bindJSON(this, config);
    		enabled = true;
    	} else {
    		enabled = false;
    	}
    	
    	slackChannelsStr = cleanupChannelList(slackChannelsStr);
        
        save();
        
        return true;
    }
    
    public FormValidation doCheckSlackSqsQueue(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.warning("Please set a queue name or URL");
        }
        return FormValidation.ok();
    }
    
    public FormValidation doCheckSlackAwsAccessKeyId(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.warning("Please set an access key id");
        }
        return FormValidation.ok();
    }
    
    public FormValidation doCheckSlackAwsSecretAccessKey(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.warning("Please set a secret access key");
        }
        return FormValidation.ok();
    }
    
    public FormValidation doCheckSlackTokenForSqsIntegration(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.warning("Please set a token from Slack integration");
        }
        return FormValidation.ok();
    }
    
    public FormValidation doCheckTriggerWord(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.warning("You must set a trigger word or phrase");
        }
        return FormValidation.ok();
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

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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
	
	public String getSlackChannelsStr() {
		return slackChannelsStr;
	}

	public void setSlackChannelsStr(String slackChannelsStr) {
		this.slackChannelsStr = slackChannelsStr;
	}

	public String getTriggerWord() {
		return triggerWord;
	}

	public void setTriggerWord(String triggerWord) {
		this.triggerWord = triggerWord;
	}

	public List<String> getSlackChannels() {
		List<String> slackChannels = new ArrayList<String>();
    	
    	if (slackChannelsStr != null) {
    		String[] items = slackChannelsStr.split("[,; ]+");
    		
    		for (int i = 0; i < items.length; ++i) {
    			String item = StringUtils.trimToNull(items[i]);
    			if (item != null) {
    				item = StringUtils.removeStart(item, "#");
    				slackChannels.add(item);
    			}
    		}
    	}
    	
    	return slackChannels;
	}

	private String cleanupChannelList(String delimitedList) {
    	return StringUtils.join(getSlackChannels(), ",");
    }
}
