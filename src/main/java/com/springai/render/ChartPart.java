package com.springai.render;

import java.util.List;

public abstract class ChartPart extends RenderPart {

    private String title;
    private List<String> labels;
    private List<String> colors;

    protected ChartPart(String kind) {
        super(kind);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }
}
