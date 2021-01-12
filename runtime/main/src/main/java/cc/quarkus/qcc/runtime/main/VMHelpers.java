package cc.quarkus.qcc.runtime.main;

/**
 * Runtime Helpers to support the operation of the compiled code.
 */
public final class VMHelpers {

	// TODO: mark this with a "AlwaysInline" annotation
	public static final int fast_instanceof(Object instance, Class<?> classInstance) {
		if (instance.getClass() == classInstance) {
			return 1;
		}
		return full_instanceof(instance, classInstance);
	}

	// TODO: mark this with a "NoInline" annotation
	public static final int full_instanceof(Object instance, Class<?> classInstance) {
		return 0; // TODO: full instance of support
	}
}
