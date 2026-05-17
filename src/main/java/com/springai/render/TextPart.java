package com.springai.render;

public class TextPart extends RenderPart {

    private String text;

    public TextPart() {
        super("text");
    }

    public TextPart(String text) {
        super("text");
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
