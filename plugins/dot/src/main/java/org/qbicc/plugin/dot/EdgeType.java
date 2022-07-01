package org.qbicc.plugin.dot;

public enum EdgeType implements DotAttributes{
    PHI_INCOMING ("green", "dashed"),
    PHI_INCOMING_UNREACHABLE ("brown", "dashed"),
    PHI_PINNED_NODE ("red", "dashed"),
    VALUE_DEPENDENCY ("blue", "solid"),
    COND_DEPENDENCY ("blueviolet", "solid"),
    ORDER_DEPENDENCY ("black", "dotted"),
    CONTROL_FLOW ("black", "bold"),
    COND_TRUE_FLOW ("brown", "bold"),
    COND_FALSE_FLOW ("darkgreen", "bold"),
    RET_RESUME_FLOW ("darkgreen", "dashed, bold");

    private final String color;
    private final String style;

    EdgeType(String color, String style) {
        this.color = color;
        this.style = style;
    }

    public String color() {
        return this.color;
    }

    public String style() {
        return this.style;
    }

    public String portPos() {
        return "";
    }
}
