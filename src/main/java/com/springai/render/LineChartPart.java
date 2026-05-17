package com.springai.render;

import java.util.List;

public class LineChartPart extends ChartPart {

    private List<ChartSeries> series;
    private boolean smooth;
    private boolean area;
    private boolean showSymbol = true;

    public LineChartPart() {
        super("lineChart");
    }

    public LineChartPart(String title, List<String> labels, List<Number> values) {
        super("lineChart");
        setTitle(title);
        setLabels(labels);
        this.series = List.of(new ChartSeries("", values));
    }

    public LineChartPart(List<ChartSeries> series,String title, List<String> labels) {
        super("lineChart");
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

    public boolean isSmooth() {
        return smooth;
    }

    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }

    public boolean isArea() {
        return area;
    }

    public void setArea(boolean area) {
        this.area = area;
    }

    public boolean isShowSymbol() {
        return showSymbol;
    }

    public void setShowSymbol(boolean showSymbol) {
        this.showSymbol = showSymbol;
    }
}
