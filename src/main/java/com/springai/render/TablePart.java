package com.springai.render;

import java.util.List;

public class TablePart extends RenderPart {

    private List<String> headers;
    private List<List<String>> rows;

    public TablePart() {
        super("table");
    }

    public TablePart(List<String> headers, List<List<String>> rows) {
        super("table");
        this.headers = headers;
        this.rows = rows;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public void setRows(List<List<String>> rows) {
        this.rows = rows;
    }
}
