package cc.quarkus.qcc.graph2;

/**
 *
 */
final class ParameterValueImpl extends ValueImpl implements ParameterValue {
    int index = -1;
    String name;

    public int getIndex() {
        return index;
    }

    public void setIndex(final int idx) {
        index = idx;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getLabelForGraph() {
        String name = this.name;
        if (name != null) {
            return "param[" + index + "]:" + name;
        } else {
            return "param[" + index + "]";
        }
    }
}
