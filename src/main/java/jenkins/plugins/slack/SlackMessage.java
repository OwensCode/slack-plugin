package jenkins.plugins.slack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by ivanvojinovic on 1/14/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackMessage {

    private String text;

    private String user_name;

    public SlackMessage() { }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_name() {
        return this.user_name;
    }
}
