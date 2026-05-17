package com.springai.render;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextPart.class, name = "text"),
        @JsonSubTypes.Type(value = TablePart.class, name = "table"),
        @JsonSubTypes.Type(value = BarChartPart.class, name = "barChart"),
        @JsonSubTypes.Type(value = PieChartPart.class, name = "pieChart"),
        @JsonSubTypes.Type(value = LineChartPart.class, name = "lineChart"),
        @JsonSubTypes.Type(value = ImagePart.class, name = "image"),
})
public abstract class RenderPart {

    private String kind;

    protected RenderPart(String kind) {
        this.kind = kind;
    }

    public String getKind() {
        return kind;
    }
}
