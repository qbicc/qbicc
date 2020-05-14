package cc.quarkus.qcc.graph.invoke;

import cc.quarkus.qcc.graph.AbstractNodeTestCase;
import cc.quarkus.qcc.graph.MockNode;
import cc.quarkus.qcc.graph.node.InvokeNode;
import cc.quarkus.qcc.graph.type.InvokeToken;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.definition.MethodDefinition;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class InvokeTest extends AbstractNodeTestCase {

    @Test
    public void testInvokeDirectlyImplements() {
        TypeDefinition animalClass = getTypeDefinition(Animal.class);
        MethodDefinition<ObjectReference> animalSpeak = (MethodDefinition<ObjectReference>) animalClass.findMethod("speak", "()Ljava/lang/String;");

        TypeDefinition dogClass = getTypeDefinition(Dog.class);
        ObjectReference dogObj = dogClass.newInstance();

        TypeDefinition catClass = getTypeDefinition(Cat.class);
        ObjectReference catObj = catClass.newInstance();

        MockNode<ObjectReference> dogNode = set(dogObj);
        InvokeNode<ObjectReference> dogInvoke = new InvokeNode<>(graph(), control(), animalSpeak, InvokeNode.InvocationType.VIRTUAL);
        dogInvoke.addArgument(dogNode);
        InvokeToken dogResult = dogInvoke.getValue(context());
        assertThat(dogResult.getReturnValue().toString() ).isEqualTo("woof");

        MockNode<ObjectReference> catNode = set(catObj);
        InvokeNode<ObjectReference> catInvoke = new InvokeNode<>(graph(), control(), animalSpeak, InvokeNode.InvocationType.VIRTUAL);
        catInvoke.addArgument(catNode);
        InvokeToken catResult = catInvoke.getValue(context());
        assertThat(catResult.getReturnValue().toString() ).isEqualTo("meow");
    }

    @Test
    public void testInvokeOverrides() {
        TypeDefinition animalClass = getTypeDefinition(Animal.class);
        MethodDefinition<ObjectReference> animalSpeak = (MethodDefinition<ObjectReference>) animalClass.findMethod("speak", "()Ljava/lang/String;");

        TypeDefinition lionClass = getTypeDefinition(Lion.class);
        ObjectReference lionObj = lionClass.newInstance();

        MockNode<ObjectReference> lionNode = set(lionObj);
        InvokeNode<ObjectReference> lionInvoke = new InvokeNode<>(graph(), control(), animalSpeak, InvokeNode.InvocationType.VIRTUAL);
        lionInvoke.addArgument(lionNode);

        InvokeToken lionResult = lionInvoke.getValue(context());
        assertThat(lionResult.getReturnValue().toString() ).isEqualTo("roar");
    }

    @Test
    public void testInvokeDoesNotOverride() {
        TypeDefinition animalClass = getTypeDefinition(Animal.class);
        MethodDefinition<ObjectReference> animalSpeak = (MethodDefinition<ObjectReference>) animalClass.findMethod("speak", "()Ljava/lang/String;");

        TypeDefinition ligerClass = getTypeDefinition(Liger.class);
        ObjectReference ligerObj = ligerClass.newInstance();

        MockNode<ObjectReference> ligerNode = set(ligerObj);
        InvokeNode<ObjectReference> ligerInvoke = new InvokeNode<>(graph(), control(), animalSpeak, InvokeNode.InvocationType.VIRTUAL);
        ligerInvoke.addArgument(ligerNode);

        InvokeToken ligerResult = ligerInvoke.getValue(context());
        assertThat(ligerResult.getReturnValue().toString() ).isEqualTo("roar");
    }
}
