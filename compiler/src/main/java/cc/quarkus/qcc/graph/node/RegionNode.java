package cc.quarkus.qcc.graph.node;

import javax.naming.ldap.Control;

import cc.quarkus.qcc.graph.type.ControlType;
import cc.quarkus.qcc.graph.type.ControlValue;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class RegionNode extends ControlNode<ControlType>  {

    public RegionNode(int maxLocals, int maxStack) {
        super(ControlType.INSTANCE, maxLocals, maxStack);
    }

    @Override
    public String label() {
        return getId() + ": <region>";
    }

    @Override
    public Value<?> getValue(Context context) {
        //return super.getValue(context);
        return new ControlValue();
    }
}

