package org.qbicc.type.definition.classfile;

import java.nio.ByteBuffer;

import org.qbicc.context.ClassContext;

/**
 * A BootstrapMethod contains the cpIndex data from a single
 * entry in the bootstrap_methods[] of a BootstrapMethod attribute.
 */
public class BootstrapMethod {
  private final int bootstrapMethod;
  private final int[] bootstrapArgs;

  BootstrapMethod(int bootstrapMethod, int[] bootstrapArgs) {
    this.bootstrapMethod = bootstrapMethod;
    this.bootstrapArgs = bootstrapArgs;
  }

  public int getBootstrapMethod() {
    return bootstrapMethod;
  }

  public int getBootstrapArgCount() {
    return bootstrapArgs.length;
  }

  public int getBootstrapArg(int index) {
    return bootstrapArgs[index];
  }

  public static BootstrapMethod parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
    int method = buf.getShort() & 0xffff;
    int argCount = buf.getShort() & 0xffff;
    int[] args = new int[argCount];
    for (int i = 0; i < argCount; i++) {
      args[i] = buf.getShort() & 0xffff;
    }
    return new BootstrapMethod(method, args);
  }
}
