package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.casestudy.PreDefinedArtifactInstance;
import com.pmsconnect.mage.repo.UserRepo;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ConnectorAsyncService {
    private final ConnectorRepository connectorRepository;

    @Autowired
    public ConnectorAsyncService (ConnectorRepository mageRepository) {
        this.connectorRepository = mageRepository;
    }

    @Async
    public void monitorProcessInstance(String connectorId) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        if (!connector.isMonitoring()) {
            connector.setMonitoring(true);
            connectorRepository.save(connector);
            this.watchProject(connector);
        } else {
            throw new IllegalStateException("This connector is already monitored");
        }
    }


    public void watchProject(Connector connector) {
        // allow process in pms to be run
        this.openProcess(connector);

        // just in case, retrieve all the commits of this project
        this.retrieveAllCommit(connector);

        // then run this as a background service to check commit status
        while(true) {
            try {
                // check last commit every 10 seconds
                Thread.sleep(10000);

                // check if there is a stop monitoring request
                Connector updatedConnector = connectorRepository.findById(connector.getId()).orElseThrow(() -> new IllegalStateException("Connector with id " + connector.getId() + "does not exist."));
                if (!updatedConnector.isMonitoring())
                    return;

                System.out.println("Monitoring connector " + connector.getId() + " of project id" + connector.getPmsProjectId() + " of user " + connector.getUserRepo().getUserName());
                this.retrieveLatestCommit(connector);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void retrieveAllCommit(Connector connector) {
        File directory = new File("/home/ngmkhoi/Documents/workspace/gitlab/processmining/src/main/python/software_heritage");
        File dirRepo = new File(connector.getUserRepo().getRepoDir());
        String controller = "python3 retrieve_latest.py " + connector.getUserRepo().getRepoLink() + " all";
        try {
            String commit = runCommandPython(directory, controller);
            String commitForJSONParser = commit.replace("\"", "\\\"").replace("'", "\"");
            JSONParser jParser = new JSONParser();
            Object obj = jParser.parse(commitForJSONParser);
            JSONArray commitList = (JSONArray) obj;

            for (int i = 0; i < commitList.size(); i++) {
                JSONObject jObject = (JSONObject) commitList.get(i);
                String commitMessage = jObject.get("title").toString();
                String commitId = jObject.get("id").toString();

                // if this commit is already in the history commit log of that connection
                if (!connector.getHistoryCommitList().contains(commitId))
                    connector.addHistoryCommitList(commitId);
                else
                    continue;

                // skip validating the commit if the connector's owner is not the committer
                String commiterName = jObject.get("committer_name").toString();
                if (!commiterName.equals(connector.getUserRepo().getUserName()))
                    continue;

                String taskFound = detectTaskFromCommit(commitMessage);

                // skip revert commit
                if (taskFound.equals(""))
                    continue;

                validateCommit(connector, commitMessage, taskFound, commitId, dirRepo);
            }
            connectorRepository.save(connector);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public void retrieveLatestCommit(Connector connector) {
        File directory = new File("../processmining/src/main/python/software_heritage");
        File dirRepo = new File(connector.getUserRepo().getRepoDir());
        String controller = "python3 retrieve_latest.py " + connector.getUserRepo().getRepoLink();
        try {
            String commit = runCommandPython(directory, controller);
            String commitForJSONParser = commit.replace("\"", "\\\"").replace("'", "\"");
            JSONParser jParser = new JSONParser();
            JSONObject jObject = (JSONObject) jParser.parse(commitForJSONParser);
            String commitMessage = jObject.get("title").toString();
            String commitId = jObject.get("id").toString();

            // if the latest commit is already in the list of retrieved commit, skip ths later work
            if (connector.getHistoryCommitList().contains(commitId)) {
                System.out.println("Commit is up-to-date");
                return;
            }

            connector.addHistoryCommitList(commitId);

            // skip validating the commit if the connector's owner is not the committer
            String commiterName = jObject.get("committer_name").toString();
            if (!commiterName.equals(connector.getUserRepo().getUserName()))
                return;

            String taskFound = detectTaskFromCommit(commitMessage);

            // skip revert commit
            if (taskFound.equals(""))
                return;

            validateCommit(connector, commitMessage, taskFound, commitId, dirRepo);

            connectorRepository.save(connector);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }



    private String runCommandPython(File whereToRun, String command) throws Exception {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        System.out.println("Running in: " + whereToRun);
        System.out.println("Command: " + command);

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(whereToRun);

        if(isWindows) {
            builder.command("cmd.exe", "/c", command);
        }else {
            builder.command("sh", "-c", command);
        }

        Process process = builder.start();

        OutputStream outputStream = process.getOutputStream();
        InputStream inputStream = process.getInputStream();

        String result = getOutputFromCommand(inputStream);

        boolean isFinished = process.waitFor(30, TimeUnit.SECONDS);
        outputStream.flush();
        outputStream.close();

        if(!isFinished) {
            process.destroyForcibly();
        }

        return result;
    }

    private void runCommand(File whereToRun, String command) throws Exception {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        System.out.println("Running in: " + whereToRun);
        System.out.println("Command: " + command);

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(whereToRun);

        if(isWindows) {
            builder.command("cmd.exe", "/c", command);
        }else {
            builder.command("sh", "-c", command);
        }

        Process process = builder.start();

        OutputStream outputStream = process.getOutputStream();

        boolean isFinished = process.waitFor(30, TimeUnit.SECONDS);
        outputStream.flush();
        outputStream.close();

        if(!isFinished) {
            process.destroyForcibly();
        }
    }

    private String getOutputFromCommand(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while((line = bufferedReader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

    public String detectTaskFromCommit(String commitMessage) {
        // TermDetect termDetector = new TermDetect();
        // return caseStudy.checkRelevant(commitMessage, termDetector);
        // cannot use this term detector in this use case, must build another system
        String taskDetect = "";

        // skip revert commit
        if (commitMessage.contains("Revert"))
            return taskDetect;

        if (commitMessage.contains("end task") || commitMessage.contains("finish task")) {
            if (commitMessage.contains("|"))
                taskDetect = commitMessage.substring(commitMessage.indexOf("task") + 5, commitMessage.indexOf("|"));
            else if (commitMessage.contains(";"))
                taskDetect = commitMessage.substring(commitMessage.indexOf("task") + 5,  commitMessage.indexOf(";"));
            else
                taskDetect = commitMessage.substring(commitMessage.indexOf("task") + 5);
        }
        System.out.println(taskDetect);
        return taskDetect;
    }

    public List<PreDefinedArtifactInstance> detectArtifactFromCommit(String commitMessage) {
        List<PreDefinedArtifactInstance> preDefinedArtifactInstanceList = new ArrayList<>();
        if (commitMessage.contains(";")) {
            String importantMessage = commitMessage.contains("|") ? commitMessage.substring(0, commitMessage.indexOf("|")) : commitMessage;
            String[] terms = importantMessage.split(";");
            for (int i = 1; i < terms.length; i++) {
                String[] artifact = terms[i].split(":");
                if (artifact.length < 2)
                    System.out.println("Invalid syntax. Cannot detect artifact.");
                preDefinedArtifactInstanceList.add(new PreDefinedArtifactInstance(artifact[0], artifact[1], Integer.parseInt(artifact[2])));
            }
        }
        return preDefinedArtifactInstanceList;
    }

    public void validateCommit(Connector connector, String commitMessage, String taskDetected, String commitId, File directory) {
        HttpClient client = HttpClients.createDefault();
        URIBuilder builder = null;
        try {
            builder = new URIBuilder(connector.getUrl() + "/" + connector.getPmsProjectId() + "/validate-task");
            builder.addParameter("taskName", taskDetected);
            builder.addParameter("actorName", connector.getUserRepo().getRealName());

            String finalUri = builder.build().toString();
            HttpGet getMethod = new HttpGet(finalUri);
            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode == 200) {
                String responseBody = EntityUtils.toString(getResponse.getEntity());
                Integer isPermitted = Integer.parseInt(responseBody);

                if (isPermitted == -1) {
                    System.out.println("Task corresponding with commit " + commitId + " is not found in this process model. A revert commit is launched");
                    rollbackCommit(commitId, directory, connector.getUserRepo());
                }
                else if (isPermitted == 0) {
                    System.out.println("Commit is invalidated. A revert commit is launched. No task is launched");
                    rollbackCommit(commitId, directory, connector.getUserRepo());
                } else if (isPermitted == 1){
                    System.out.println("Commit is validated. Task is launched");
                    completeTaskCommitted(connector, taskDetected, commitMessage);
                    connectorRepository.save(connector);
                } else if (isPermitted == 2) {
                    System.out.println("Task has been completed.");
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollbackCommit(String commitId, File directory, UserRepo user) {
        try {
            runCommand(directory, "git revert " + commitId);
            String pushCommitCommand = "https://" + user.getUserName() + ":" + user.getPersonalToken() + "@" + user.getRepoLink().replace("https://","") + ".git";
            runCommand(directory, "git push " + pushCommitCommand);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void completeTaskCommitted(Connector connector, String taskDetected, String commitMessage) {
        String newTaskInstanceId = startTaskInstance(connector, taskDetected);
        endTaskInstance(connector, newTaskInstanceId, detectArtifactFromCommit(commitMessage));
    }

    private String startTaskInstance(Connector connector, String taskDetected) {
        HttpClient client = HttpClients.createDefault();
        URIBuilder builder = null;
        try {
            builder = new URIBuilder(connector.getUrl() + "/" + connector.getPmsProjectId() + "/start-task");
            builder.addParameter("taskName", taskDetected);
            builder.addParameter("actorName", connector.getUserRepo().getRealName());

            String finalUri = builder.build().toString();
            HttpPut putMethod = new HttpPut(finalUri);
            HttpResponse getResponse = client.execute(putMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode != 200)
                return "";
            return EntityUtils.toString(getResponse.getEntity());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void endTaskInstance(Connector connector, String newTaskInstanceId, List<PreDefinedArtifactInstance> preDefinedArtifactInstanceList) {
        HttpClient client = HttpClients.createDefault();
        URIBuilder builder = null;
        try {
            builder = new URIBuilder(connector.getUrl() + "/" + connector.getPmsProjectId() + "/end-task");
            builder.addParameter("taskId", newTaskInstanceId);

            String finalUri = builder.build().toString();
            HttpPut putMethod = new HttpPut(finalUri);
            putMethod.addHeader("Content-Type", "application/json");
            StringEntity entity = new StringEntity(preDefinedArtifactInstanceList.toString(), "UTF-8");
            putMethod.setEntity(entity);

            HttpResponse getResponse = client.execute(putMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode != 200)
                throw new IllegalStateException("Cannot end task id " + newTaskInstanceId);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openProcess(Connector connector) {
        HttpClient client = HttpClients.createDefault();
        URIBuilder builder = null;
        try {
            builder = new URIBuilder(connector.getUrl() + "/" + connector.getPmsProjectId() + "/change-state");
            builder.addParameter("processInstanceState", Boolean.toString(false));

            String finalUri = builder.build().toString();
            HttpPut putMethod = new HttpPut(finalUri);
            HttpResponse getResponse = client.execute(putMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode != 200)
                throw new IllegalStateException("Cannot open process instance id " + connector.getPmsProjectId());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void endTaskInstance(String connectorId, String taskId, String commitMessage) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        List<PreDefinedArtifactInstance> preDefinedArtifactInstanceList = detectArtifactFromCommit(commitMessage);

        endTaskInstance(connector, taskId, preDefinedArtifactInstanceList);
    }
}
