package com.springai.render;

import java.util.List;

public class BarChartPart extends ChartPart {

    private List<ChartSeries> series;
    private boolean stacked;
    private boolean horizontal;

    public BarChartPart() {
        super("barChart");
    }

    public BarChartPart(String title, List<String> labels, List<Number> values) {
        super("barChart");
        setTitle(title);
        setLabels(labels);
        this.series = List.of(new ChartSeries("", values));
    }

    public BarChartPart( List<String> labels,String title, List<ChartSeries> series) {
        super("barChart");
        setTitle(title);
        setLabels(labels);
        this.series = series;
    }

    public List<ChartSeries> getSeries() {
        return series;
    }

    public void setSeries(List<ChartSeries> series) {
        this.series = series;
    }

    public boolean isStacked() {
        return stacked;
    }

    public void setStacked(boolean stacked) {
        this.stacked = stacked;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
    }
}
