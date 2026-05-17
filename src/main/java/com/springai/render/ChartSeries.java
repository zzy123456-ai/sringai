package com.springai.render;

import java.util.List;

public class ChartSeries {

    private String name;
    private List<Number> data;

    public ChartSeries() {
    }

    public ChartSeries(String name, List<Number> data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Number> getData() {
        return data;
    }

    public void setData(List<Number> data) {
        this.data = data;
    }
}
