package cc.quarkus.qcc.machine.tool;

/**
 *
 */
public abstract class Linker extends Tool {
    protected Linker() {
    }

    public String getToolName() {
        return "Linker";
    }
}
