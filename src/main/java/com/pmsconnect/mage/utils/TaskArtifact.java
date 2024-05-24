package com.pmsconnect.mage.utils;

import java.util.ArrayList;
import java.util.List;

public class TaskArtifact {
    private String taskName;
    private List<Artifact> input;
    private List<Artifact> output;

    public TaskArtifact(String taskName, List<Artifact> input, List<Artifact> output) {
        this.taskName = taskName;
        this.input = input;
        this.output = output;
    }

    public TaskArtifact(String taskName, String[] inputString, String[] outputString, String mode) {
        this.taskName = taskName;

        this.input = new ArrayList<>();
        this.output = new ArrayList<>();

        for (String artifactName: inputString) {
            Artifact artifact = new Artifact(artifactName);
            this.input.add(artifact);
        }

        for (String artifactName: outputString) {
            Artifact artifact = new Artifact(artifactName);
            this.output.add(artifact);
        }
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public List<Artifact> getInput() {
        return input;
    }

    public void setInput(List<Artifact> input) {
        this.input = input;
    }

    public List<Artifact> getOutput() {
        return output;
    }

    public void setOutput(List<Artifact> output) {
        this.output = output;
    }
}
