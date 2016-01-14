package jenkins.plugins.slack;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.security.ACL;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.plugins.slack.mq.GlobalConfig;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This class processes the commands posted from slack.
 */
public class SlackMessageProcessor {

    private static final Logger LOGGER = Logger.getLogger(SlackMessageProcessor.class.getName());

    public String process(String message) {

        String response = "";

        try {
            SlackMessage slackTextMessage =
                new ObjectMapper().readValue(message, SlackMessage.class);

            String command = slackTextMessage.getText();

            LOGGER.info("processing -" + command);
            if(isValidCommand(command, "list projects")) {
                response = listProjects();
            }
            LOGGER.info("response -" + response);

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE,"unable to process message -" + ex.getMessage(),ex);
        }

        return response;
    }

    private boolean isValidCommand(String command, String patternString) {

        GlobalConfig globalConfig = GlobalConfiguration.all().get(GlobalConfig.class);

        Pattern pattern = Pattern.compile("^" + globalConfig.getTriggerWord() + " " + patternString);
        Matcher matcher = pattern.matcher(command);

        return matcher.matches();
    }

    private String listProjects() {

        ACL.impersonate(ACL.SYSTEM);
        String response = "*Projects:*\n";

        List<AbstractProject> jobs =
                Jenkins.getInstance().getAllItems(AbstractProject.class);

        for (AbstractProject job : jobs) {
            if (job.isBuildable()) {
                AbstractBuild lastBuild = job.getLastBuild();
                String buildNumber = "TBD";
                String status = "TBD";
                if (lastBuild != null) {

                    buildNumber = Integer.toString(lastBuild.getNumber());

                    if (lastBuild.isBuilding()) {
                        status = "BUILDING";
                    }

                    Result result = lastBuild.getResult();

                    if (result != null) {
                        status = result.toString();
                    }
                }
                response += ">*"+job.getDisplayName() + "*\n>*Last Build:* #"+buildNumber+"\n>*Status:* "+status;
                response += "\n\n\n";
            }
        }

        if (jobs == null || jobs.size() == 0)
            response += ">_No projects found_";

        return response;
    }

}
