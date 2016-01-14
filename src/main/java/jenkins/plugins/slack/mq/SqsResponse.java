package jenkins.plugins.slack.mq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * The response from Amazon SQS
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SqsResponse {

    public String text;

    @JsonProperty("channel_name")
    public String channelName;

    @JsonProperty("user_name")
    private String userName;

    public String getText() {

        return text;
    }

    public void setText(String text) {

        this.text = text;
    }

    public String getChannelName() {

        return channelName;
    }

    public void setChannelName(String channelName) {

        this.channelName = channelName;
    }

    public String getUserName() {

        return userName;
    }

    public void setUserName(String user_name) {

        this.userName = user_name;
    }

    @Override
    public String toString() {

        return new ToStringBuilder(this)
                .append("text", text)
                .append("channelName", channelName)
                .append("userName", userName)
                .toString();
    }
}