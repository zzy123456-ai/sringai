package com.springai.render;

public class ImagePart extends RenderPart {

    private String url;
    private String alt;

    public ImagePart() {
        super("image");
    }

    public ImagePart(String url, String alt) {
        super("image");
        this.url = url;
        this.alt = alt;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }
}
