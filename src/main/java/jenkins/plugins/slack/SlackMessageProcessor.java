package jenkins.plugins.slack;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.tasks.Publisher;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.plugins.slack.mq.GlobalConfig;
import jenkins.plugins.slack.mq.SqsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class processes the commands posted from slack.
 */
public class SlackMessageProcessor {

    private static final Logger LOGGER = Logger.getLogger(SlackMessageProcessor.class.getName());

    public String process(SqsResponse slackMessage) {

        String response = "";

        try {
            String command = slackMessage.getText();
            LOGGER.info("processing -" + command);

            String listProjectPattern = "list projects";
            String scheduleJobPattern = "run ([\\p{L}\\p{N}\\p{ASCII}\\W]+)";


            if(isValidCommand(command, listProjectPattern)) {
                response = listProjects(slackMessage.getChannelName());
            }
            if(isValidCommand(command, scheduleJobPattern)) {
                String[] parametersArray = getParamaters(command, scheduleJobPattern);
                response = scheduleJob(parametersArray[0], slackMessage.getUserName(), slackMessage.getChannelName());
            }
            LOGGER.info("response -" + response);

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE,"unable to process message -" + ex.getMessage(),ex);
        }

        return response;
    }

    private Matcher getMatcher(String command, String patternString) {

        GlobalConfig globalConfig = GlobalConfiguration.all().get(GlobalConfig.class);
        Pattern pattern = Pattern.compile("^" + globalConfig.getTriggerWord() + " " + patternString);

        return pattern.matcher(command);
    }

    private boolean isValidCommand(String command, String patternString) {

        return getMatcher(command, patternString).matches();
    }

    private String[] getParamaters(String command, String patternString) {

        String[] parametersArray = null;

        Matcher matcher = getMatcher(command, patternString);

        if (matcher.matches()) {
            List<Object> parameters = new ArrayList<Object>();
            for (int index = 0; index <= matcher.groupCount(); index++) {
                parameters.add(matcher.group(index));
            }

            if (parameters.size() > 1) {
                parameters.remove(0);
                parametersArray = new String[parameters.size()];
                parametersArray = parameters.toArray(parametersArray);
            }
        }

        return parametersArray;
    }

    private void sendSlackNotificationForProject(AbstractProject project, String message, String channelName) {

        Descriptor<Publisher> publisher = Jenkins.getInstance().getPublisher("SlackNotifier");
        SlackNotifier.DescriptorImpl slackNotifier = (SlackNotifier.DescriptorImpl) publisher;

        String token = slackNotifier.getToken();
        String teamDomain = slackNotifier.getTeamDomain();

        SlackService testSlackService = new StandardSlackService(teamDomain, token, "#" + channelName);
        testSlackService.publish(message, "good");
    }

    private String listProjects(String channelName) {

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

                sendSlackNotificationForProject(job, response, channelName);
            }
        }

        if (jobs == null || jobs.size() == 0)
            response += ">_No projects found_";

        return response;
    }

    public String scheduleJob(String projectName, String slackUser, String channelName) {

        ACL.impersonate(ACL.SYSTEM);

        Project project =
                Jenkins.getInstance().getItemByFullName(projectName, Project.class);

        boolean success = false;

        if (project != null) {
            success = project.scheduleBuild(new SlackCause(slackUser));
        } else {
            return "Could not find project (" + projectName + ")\n";
        }

        if (success) {
            String response = "Build scheduled for project " + projectName + "\n";
            sendSlackNotificationForProject(project, response, channelName);
            return response;
        } else {
            return "Build not scheduled due to an issue with Jenkins";
        }
    }
}
