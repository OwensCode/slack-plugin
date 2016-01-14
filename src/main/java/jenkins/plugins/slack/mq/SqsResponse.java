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

    private String user_name;

    @JsonProperty("channel_name")
    public String channelName;

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

    @Override
    public String toString() {

        return new ToStringBuilder(this)
                .append("text", text)
                .append("channelName", channelName)
                .toString();
    }

    public void setUser_name(String user_name) {

        this.user_name = user_name;
    }

    public String getUser_name() {

        return this.user_name;
    }
}