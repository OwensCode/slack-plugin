package jenkins.plugins.slack.mq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The response from Amazon SQS
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SqsResponse {

    public String text;

    public String getText() {

        return text;
    }

    public void setText(String text) {

        this.text = text;
    }

    @Override
    public String toString() {

        return new org.apache.commons.lang3.builder.ToStringBuilder(this)
                .append("text", text)
                .toString();
    }
}