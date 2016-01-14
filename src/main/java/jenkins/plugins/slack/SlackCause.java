package jenkins.plugins.slack;

import hudson.model.Cause;

/**
 * Created by ivanvojinovic on 1/14/16.
 */
public class SlackCause extends Cause {
    private String username;

    public SlackCause(String username) {
        this.username = username;
    }

    @Override
    public String getShortDescription() {
        return "Build started by Slack user @"+username+ " via SlackPlugin";
    }
}
