package com.pmsconnect.mage.service;

import com.pmsconnect.mage.casestudy.CaseStudy;
import event.TaskEventHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.pmsconnect.mage.connector.PMSConnector;
import com.pmsconnect.mage.connector.CoreBapeConnector;
import com.pmsconnect.mage.repo.Repo;
import com.pmsconnect.mage.repo.UserRepo;
import type.ProcessInstance;
import type.TaskInstance;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//// this class is aimed for detecting possible
public class Mage {
    private List<String> commitList;
    private ProcessInstance processInstance;
    private Map<String, String> artifactMap;
    private Map<String, Integer> artifactSharedMap;
    private TaskEventHandler taskEventHandler;
    private String repoLink;
    private String repoDir;
    private UserRepo userRepo;
    private String path;
    private PMSConnector connector;

    public Mage(String mode, String creatorRole, String processType, String user, String userRealName, String repoOrigin, String token, String repoLink, String repoDir, String pms) {
        if (mode.equals("create"))
            createConnector(creatorRole, processType, user, userRealName, repoOrigin, token, repoLink, repoDir, pms);
        this.repoDir = repoDir;
        this.repoLink = repoLink;
        this.taskEventHandler = new TaskEventHandler(this.processInstance);
        this.commitList = new ArrayList<>();
        this.artifactMap = new HashMap<>();
        this.artifactSharedMap = new HashMap<>();
        this.path = "../processmining/resources/commitlist/" + this.processInstance.getName();
    }

    public Mage(String processId, String processType, String user, String userRealName, String repoOrigin, String token, String repoLink, String repoDir, String pms) {
        emitConnector(processId, processType, user, userRealName, repoOrigin, token, repoLink, repoDir, pms);
        this.repoDir = repoDir;
        this.repoLink = repoLink;
        this.taskEventHandler = new TaskEventHandler(processInstance);
        this.commitList = loadCommitList();
        this.artifactMap = new HashMap<>();
        this.artifactSharedMap = new HashMap<>();
        this.path = "../processmining/resources/commitlist/" + this.processInstance.getName();
    }

    private void createConnector(String creatorRole, String processType, String user, String userRealName, String repoOrigin, String token, String repoLink, String repoDir, String pms) {
        if (pms.equals("core_bape")) {
            this.connector = new CoreBapeConnector();
            this.processInstance = this.connector.createProcessInstance(creatorRole, processType, user, userRealName, repoOrigin, token, repoLink, repoDir);
        }
        Repo projectRepo = new Repo(repoOrigin);
        userRepo = new UserRepo(user, userRealName, token, projectRepo);
        userRepo.addRepoMap(repoLink, repoDir);
    }

    private void emitConnector(String processId, String processType, String user, String userRealName, String repoOrigin, String token, String repoLink, String repoDir, String pms) {
        if (pms.equals("core_bape")) {
            this.connector = new CoreBapeConnector();
            this.processInstance = this.connector.connectProcessInstance(processId, processType, user, userRealName, repoOrigin, token, repoLink, repoDir);
        }
        Repo projectRepo = new Repo(repoOrigin);
        userRepo = new UserRepo(user, userRealName, token, projectRepo);
        userRepo.addRepoMap(repoLink, repoDir);
    }

    public Map<String, String> getArtifactMap() {
        return artifactMap;
    }

    public Map<String, Integer> getArtifactSharedMap() {
        return artifactSharedMap;
    }

    public String getCommitPath() {
        return path;
    }

    public void setCommitPath(String path) {
        this.path = path;
    }

    public List<String> loadCommitList() {
        List<String> tmp = new ArrayList<>();
        try {
            File savePath = new File(this.path + "/" + this.processInstance.getId() + ".txt");
            if (!savePath.exists()) {
                return tmp;
            }

            BufferedReader reader = new BufferedReader(new FileReader(savePath));
            String line;
            while ((line = reader.readLine()) != null) {
                tmp.add(line.replaceAll("\r", "").replaceAll("\n", ""));
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("An error occurred while loading commit list");
            e.printStackTrace();
        }
        return tmp;
    }

    public void saveCommitList() {
        try {
            File savePath = new File(this.path);
            if (!savePath.isDirectory()) {
                savePath.mkdir();
            }

            StringBuilder content = new StringBuilder();
            for (String commit : this.commitList) {
                content.append(commit).append("\n");
            }

            FileWriter myWriter = new FileWriter(this.path + "/" + this.processInstance.getId() + ".txt");
            myWriter.write(content.toString());
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred while saving commit list");
            e.printStackTrace();
        }
    }

    public void retrieveAllCommit(CaseStudy caseStudy, UserRepo user, String repoLink, String repoDir) {
        File directory = new File("../processmining/src/main/python/software_heritage");
        File dirRepo = new File(repoDir);
        String controller = "python3 retrieve_latest.py " + repoLink + " all";
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

                if (!this.commitList.contains(commitId))
                    this.commitList.add(commitId);
                else
                    continue;
                saveCommitList();

                String taskFound = detectTaskFromCommit(commitMessage, caseStudy);

                // skip revert commit
                if (taskFound.equals(""))
                    continue;

                int isPermitted = validateCommit(taskFound);
                if (isPermitted == -1) {
                    System.out.println("Task corresponding with commit \"" + commitMessage + "\" not found in this process model");
                }
                else if (isPermitted == 0) {
                    System.out.println("Commit is invalidated. A revert commit is launched. No task is launched");
                    rollbackCommit(commitId, dirRepo, user, repoLink);
                } else if (isPermitted == 1){
                    System.out.println("Commit is validated. Task is launched");
                    completeTaskCommitted(taskFound);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public void retrieveLatestCommit(CaseStudy caseStudy, UserRepo user, String repoLink, String repoDir) {
        File directory = new File("../processmining/src/main/python/software_heritage");
        File dirRepo = new File(repoDir);
        String controller = "python3 retrieve_latest.py " + repoLink;
        try {
            String commit = runCommandPython(directory, controller);
            String commitForJSONParser = commit.replace("\"", "\\\"").replace("'", "\"");
            JSONParser jParser = new JSONParser();
            JSONObject jObject = (JSONObject) jParser.parse(commitForJSONParser);
            String commitMessage = jObject.get("title").toString();
            String commitId = jObject.get("id").toString();

            // if the latest commit is already in the list of retrieved commit, skip ths later work
            if (this.commitList.contains(commitId)) {
                System.out.println("Commit is up-to-date");
                return;
            }

            this.commitList.add(commitId);

            String taskFound = detectTaskFromCommit(commitMessage, caseStudy);

            // skip revert commit
            if (taskFound.equals(""))
                return;

            int isPermitted = validateCommit(taskFound);
            if (isPermitted == -1) {
                System.out.println("Task corresponding with commit \"" + commitMessage + "\" is not found in this process model");
            }
            else if (isPermitted == 0) {
                System.out.println("Commit is invalidated. A revert commit is launched. No task is launched");
                rollbackCommit(commitId, dirRepo, user, repoLink);
            } else if (isPermitted == 1){
                System.out.println("Commit is validated. Task is launched");
                completeTaskCommitted(taskFound);
            }
            saveCommitList();
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

    public String detectTaskFromCommit(String commitMessage, CaseStudy caseStudy) {
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

    public void detectArtifactFromCommit(String commitMessage) {
        if (commitMessage.contains(";")) {
            artifactMap.clear();
            artifactSharedMap.clear();
            String importantMessage = commitMessage.contains("|") ? commitMessage.substring(0, commitMessage.indexOf("|")) : commitMessage;
            String[] terms = importantMessage.split(";");
            for (int i = 1; i < terms.length; i++) {
                String[] artifact = terms[i].split(":");
                if (artifact.length < 2)
                    System.out.println("Invalid syntax. Cannot detect artifact.");
                artifactMap.put(artifact[0], artifact[1]);
                artifactSharedMap.put(artifact[0], Integer.parseInt(artifact[2]));
            }
        }
    }

    public int validateCommit(String taskDetected) {
        TaskInstance task = processInstance.getTaskByName(taskDetected, userRepo.getRealName());
        if (task == null) {
            return -1;
        }

        return taskEventHandler.taskReady(task) ? 1 : 0;
    }

    public void rollbackCommit(String commitId, File directory, UserRepo user, String repoLink) {
        try {
            runCommand(directory, "git revert " + commitId);
            String pushCommitCommand = "https://" + user.getUserName() + ":" + user.getPersonalToken() + "@" + repoLink.replace("https://","") + ".git";
            runCommand(directory, "git push " + pushCommitCommand);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void completeTaskCommitted(String taskDetected) {
        TaskInstance task = processInstance.getTaskByName(taskDetected, userRepo.getRealName());
        taskEventHandler.startTask(task);
        taskEventHandler.endTask(task, artifactMap, artifactSharedMap);
    }

    public void watchProject() {
        // allow process in pms to be run
        processInstance.openProcess();

        // just in case, retrieve all the commits of this project
        this.retrieveAllCommit(this.connector.getCaseStudyMage(), this.userRepo, this.repoLink, this.repoDir);

        // then run this as a background service to check commit status
        while(true) {
            try {
                // check last commit every 10 seconds
                Thread.sleep(5000);
                this.retrieveLatestCommit(this.connector.getCaseStudyMage(), this.userRepo, this.repoLink, this.repoDir);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}