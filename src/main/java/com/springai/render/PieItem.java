package com.springai.render;

public class PieItem {

    private String name;
    private Number value;

    public PieItem() {
    }

    public PieItem(String name, Number value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }
}
