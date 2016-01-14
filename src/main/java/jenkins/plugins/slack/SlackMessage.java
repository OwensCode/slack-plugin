package jenkins.plugins.slack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by ivanvojinovic on 1/14/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackMessage {

    private String text;

    public SlackMessage() { }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }
}
