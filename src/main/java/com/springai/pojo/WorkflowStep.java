package com.springai.pojo;

import java.util.List;

public class WorkflowStep {
    private int order;
    private String agent;
    private String task;
    private List<Integer> dependsOn;

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public List<Integer> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<Integer> dependsOn) {
        this.dependsOn = dependsOn;
    }
}
