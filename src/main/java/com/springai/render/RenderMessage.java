package com.springai.render;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RenderMessage {

    private String jsonrpc = "2.0";
    private String id;
    private List<RenderPart> parts;
    private boolean streaming;
    private boolean done;

    public RenderMessage() {
        this.id = UUID.randomUUID().toString();
        this.parts = new ArrayList<>();
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<RenderPart> getParts() {
        return parts;
    }

    public void setParts(List<RenderPart> parts) {
        this.parts = parts;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    // ==================== 便捷构建方法 ====================

    public static RenderMessage complete(List<RenderPart> parts) {
        RenderMessage msg = new RenderMessage();
        msg.parts = parts;
        msg.streaming = false;
        msg.done = true;
        return msg;
    }

    public static RenderMessage chunk(List<RenderPart> parts) {
        RenderMessage msg = new RenderMessage();
        msg.parts = parts;
        msg.streaming = true;
        msg.done = false;
        return msg;
    }

    public static RenderMessage finish(List<RenderPart> parts) {
        RenderMessage msg = new RenderMessage();
        msg.parts = parts;
        msg.streaming = true;
        msg.done = true;
        return msg;
    }

    // ==================== 便捷添加方法 ====================

    public RenderMessage addText(String text) {
        this.parts.add(new TextPart(text));
        return this;
    }

    public RenderMessage addTable(List<String> headers, List<List<String>> rows) {
        this.parts.add(new TablePart(headers, rows));
        return this;
    }

    /** 添加柱状图（单系列简化） */
    public RenderMessage addBarChart(String title, List<String> labels, List<Number> values) {
        this.parts.add(new BarChartPart(title, labels, values));
        return this;
    }

    /** 添加柱状图（多系列） */
    public RenderMessage addBarCharts(String title, List<String> labels, List<ChartSeries> series) {
        this.parts.add(new BarChartPart(labels,title,  series));
        return this;
    }

    /** 添加饼图 */
    public RenderMessage addPieChart(String title, List<PieItem> items) {
        this.parts.add(new PieChartPart(title, items));
        return this;
    }

    /** 添加饼图（环形图） */
    public RenderMessage addPieChart(String title, List<PieItem> items, String radius) {
        PieChartPart part = new PieChartPart(title, items);
        part.setRadius(radius);
        this.parts.add(part);
        return this;
    }

    /** 添加折线图（单系列简化） */
    public RenderMessage addLineChart(String title, List<String> labels, List<Number> values) {
        this.parts.add(new LineChartPart(title, labels, values));
        return this;
    }

    /** 添加折线图（多系列） */
    public RenderMessage addLineCharts(String title, List<String> labels, List<ChartSeries> series) {
        this.parts.add(new LineChartPart(series,title, labels));
        return this;
    }

    public RenderMessage addImage(String url, String alt) {
        this.parts.add(new ImagePart(url, alt));
        return this;
    }
}
