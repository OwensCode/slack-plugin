package jenkins.plugins.slack.mq;

import static org.junit.Assert.*;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.mockito.Mockito;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;

public class GlobalConfigTest {
	
	@Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
	public void testDefaultConfiguration() {
    	GlobalConfig config = GlobalConfiguration.all().get(GlobalConfig.class);
    	
    	assertFalse(config.isEnabled());
    	
    	assertNull(config.getSlackSqsQueue());
    	assertNull(config.getSlackAwsAccessKeyId());
    	assertNull(config.getSlackAwsSecretAccessKey());
    	assertNull(config.getSlackTokenForSqsIntegration());
    	assertNotNull(config.getSlackChannels());
    	assertEquals("", config.getSlackChannelsStr());
    	assertEquals("jenkins", config.getTriggerWord());
	}
    
    @Test
    @LocalData
    public void testLoadConfig() throws Exception {
    	GlobalConfig config = GlobalConfiguration.all().get(GlobalConfig.class);
    	
    	assertTrue(config.isEnabled());
    	assertEquals("Jenkins-Slack-Plugin-UT", config.getSlackSqsQueue());
    	assertEquals("ACCESS_KEY", config.getSlackAwsAccessKeyId());
    	assertEquals("SECRET_KEY", config.getSlackAwsSecretAccessKey().getPlainText());
    	assertEquals("SLACK_TOKEN", config.getSlackTokenForSqsIntegration());
    	assertNotNull(config.getSlackChannels());
    	assertEquals(3, config.getSlackChannels().size());
    	assertTrue(config.getSlackChannels().contains("general"));
    	assertTrue(config.getSlackChannels().contains("build"));
    	assertTrue(config.getSlackChannels().contains("random"));
    	assertEquals("james", config.getTriggerWord());
    }
    
    @Test
    public void testDefaultFormBehavior() throws Exception {
    	JenkinsRule.WebClient client = j.createWebClient();
    	
    	HtmlForm form = client.goTo("configure").getFormByName("config");
    	
    	// Functionality disabled by default
    	HtmlInput enabledField = form.getInputByName("_.slackEnableSqsIntegration");
    	assertFalse(enabledField.isChecked());
    	
    	// And all the fields are hidden
    	assertFieldAttributes(form, "_.slackSqsQueue", "", true);
    	assertFieldAttributes(form, "_.slackAwsAccessKeyId", "", true);
    	assertFieldAttributes(form, "_.slackAwsSecretAccessKey", "", true);
    	assertFieldAttributes(form, "_.slackTokenForSqsIntegration", "", true);
    	assertFieldAttributes(form, "_.slackChannelsStr", "", true);
    	assertFieldAttributes(form, "_.triggerWord", "jenkins", true);
    	
    	// Enable it
    	enabledField.click();
    	assertTrue(enabledField.isChecked());
    	
    	// Fields should now be shown
    	assertFieldAttributes(form, "_.slackSqsQueue", "", false);
    	assertFieldAttributes(form, "_.slackAwsAccessKeyId", "", false);
    	assertFieldAttributes(form, "_.slackAwsSecretAccessKey", "", false);
    	assertFieldAttributes(form, "_.slackTokenForSqsIntegration", "", false);
    	assertFieldAttributes(form, "_.slackChannelsStr", "", false);
    	assertFieldAttributes(form, "_.triggerWord", "jenkins", false);
    	
    	// Update the form values and submit
    	setFormFieldValue(form, "_.slackSqsQueue", "Test_Queue_345345");
    	setFormFieldValue(form, "_.slackAwsAccessKeyId", "Acess_Key_sldfj");
    	setFormFieldValue(form, "_.slackAwsSecretAccessKey", "Secret_SDFSDFX");
    	setFormFieldValue(form, "_.slackTokenForSqsIntegration", "Token.345erfwa");
    	setFormFieldValue(form, "_.slackChannelsStr", "#perform");
    	setFormFieldValue(form, "_.triggerWord", "Test This");
    	
    	j.submit(form);

    	// Check that the configuration was updated
    	GlobalConfig config = GlobalConfiguration.all().get(GlobalConfig.class);
    	assertTrue(config.isEnabled());
    	assertEquals("Test_Queue_345345", config.getSlackSqsQueue());
    	assertEquals("Acess_Key_sldfj", config.getSlackAwsAccessKeyId());
    	assertEquals("Secret_SDFSDFX", config.getSlackAwsSecretAccessKey().getPlainText());
    	assertEquals("Token.345erfwa", config.getSlackTokenForSqsIntegration());
    	assertEquals("perform", config.getSlackChannelsStr());
    	assertEquals(1, config.getSlackChannels().size());
    	assertEquals("perform", config.getSlackChannels().get(0));
    	assertEquals("Test This", config.getTriggerWord());
    	
    	// Disable everything again
    	enabledField.click();
    	assertFalse(enabledField.isChecked());
    	j.submit(form);
    	
    	// Check that the configuration was updated, but just disabled
    	config = GlobalConfiguration.all().get(GlobalConfig.class);
    	assertFalse(config.isEnabled());
    	assertEquals("Test_Queue_345345", config.getSlackSqsQueue());
    	assertEquals("Acess_Key_sldfj", config.getSlackAwsAccessKeyId());
    	assertEquals("Secret_SDFSDFX", config.getSlackAwsSecretAccessKey().getPlainText());
    	assertEquals("Token.345erfwa", config.getSlackTokenForSqsIntegration());
    	assertEquals("perform", config.getSlackChannelsStr());
    	assertEquals(1, config.getSlackChannels().size());
    	assertEquals("perform", config.getSlackChannels().get(0));
    	assertEquals("Test This", config.getTriggerWord());
    }
    
    @Test
    public void testDoTestConnectionWithIncompleteParameters() throws Exception {
    	GlobalConfig config = new GlobalConfig();
    	
    	FormValidation validation = config.doTestConnection("", "sdfdf", Secret.fromString("sdfsdf"));
    	assertEquals(FormValidation.Kind.ERROR, validation.kind);
    	assertEquals("Please provide all configuration values", validation.getMessage());
    	
    	validation = config.doTestConnection("r42wff3r", "", Secret.fromString("sdfsdf"));
    	assertEquals(FormValidation.Kind.ERROR, validation.kind);
    	assertEquals("Please provide all configuration values", validation.getMessage());
    	
    	validation = config.doTestConnection("r42wff3r", "", Secret.fromString(""));
    	assertEquals(FormValidation.Kind.ERROR, validation.kind);
    	assertEquals("Please provide all configuration values", validation.getMessage());
    }
    
    @Test
    public void testDoTestConnectionWithValidParameters() throws Exception {
    	SqsProfile profile = Mockito.mock(SqsProfile.class);
    	Mockito.doReturn("fooled-yah").when(profile).getQueueUrl();
    	
    	GlobalConfig config = new GlobalConfigWithSqsProfile(profile);
    	
    	FormValidation validation = config.doTestConnection("asdasdas", "zxcasd2", Secret.fromString("sdfsdf"));
    	assertEquals(FormValidation.Kind.OK, validation.kind);
    }
    
    @Test
    public void testDoTestConnectionWhenQueueCannotBeFound() throws Exception {
    	SqsProfile profile = Mockito.mock(SqsProfile.class);
    	Mockito.doReturn(null).when(profile).getQueueUrl();
    	
    	GlobalConfig config = new GlobalConfigWithSqsProfile(profile);
    	
    	FormValidation validation = config.doTestConnection("asdasdas", "zxcasd2", Secret.fromString("sdfsdf"));
    	assertEquals(FormValidation.Kind.ERROR, validation.kind);
    }
    
    @Test
    public void testDoTestConnectionWhenConfigurationErrorOccurs() throws Exception {
    	SqsProfile profile = Mockito.mock(SqsProfile.class);
    	Mockito.doThrow(new RuntimeException("Bad account")) .when(profile).getQueueUrl();
    	
    	GlobalConfig config = new GlobalConfigWithSqsProfile(profile);
    	
    	FormValidation validation = config.doTestConnection("asdasdas", "zxcasd2", Secret.fromString("sdfsdf"));
    	assertEquals(FormValidation.Kind.ERROR, validation.kind);
    }
    
    private void assertFieldAttributes(HtmlForm form, String name, String expectedValue, boolean shouldBeHidden) {
    	HtmlInput field = form.getInputByName(name);
    	assertEquals(expectedValue, field.getValueAttribute());
    	if (shouldBeHidden) {
    		assertTrue(getParentTableRowStyle(field).contains("display: none"));
    	} else {
    		assertFalse(getParentTableRowStyle(field).contains("display: none"));
    	}
    }
    
    private String getParentTableRowStyle(HtmlInput field) {
    	DomNode tr = field.getParentNode().getParentNode();
    	NamedNodeMap attr = tr.getAttributes();
    	Node style = attr.getNamedItem("style");
    	return style.getNodeValue();
    }
    
    private void setFormFieldValue(HtmlForm form, String name, String newValue) {
    	HtmlInput field = form.getInputByName(name);
    	field.setValueAttribute(newValue);
    }
    
    private static final class GlobalConfigWithSqsProfile extends GlobalConfig {
    	
    	private SqsProfile profile;
    	
    	public GlobalConfigWithSqsProfile(SqsProfile profile) {
    		super();
    		this.profile = profile;
    	}
    	
    	@Override
    	protected SqsProfile getSqsProfile(String awsAccessKeyId,
    			Secret awsSecretAccessKey, String sqsQueue) {
    		return profile;
    	}
    }
}
