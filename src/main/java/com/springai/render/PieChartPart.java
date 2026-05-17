package com.springai.render;

import java.util.List;

public class PieChartPart extends ChartPart {

    private List<PieItem> items;
    private String radius;
    private String roseType;
    private boolean showLabel = true;

    public PieChartPart() {
        super("pieChart");
    }

    public PieChartPart(String title, List<PieItem> items) {
        super("pieChart");
        setTitle(title);
        this.items = items;
    }

    public List<PieItem> getItems() {
        return items;
    }

    public void setItems(List<PieItem> items) {
        this.items = items;
    }

    public String getRadius() {
        return radius;
    }

    /** @param radius 如 "70%" 为标准饼图，"50%,70%" 为环形图 */
    public void setRadius(String radius) {
        this.radius = radius;
    }

    public String getRoseType() {
        return roseType;
    }

    /** @param roseType "radius" 或 "area"，null 为普通饼图 */
    public void setRoseType(String roseType) {
        this.roseType = roseType;
    }

    public boolean isShowLabel() {
        return showLabel;
    }

    public void setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
    }
}
