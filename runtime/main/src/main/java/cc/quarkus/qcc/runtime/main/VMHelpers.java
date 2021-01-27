package cc.quarkus.qcc.runtime.main;

/**
 * Runtime Helpers to support the operation of the compiled code.
 */
public final class VMHelpers {

	// TODO: mark this with a "AlwaysInline" annotation
	public static final int fast_instanceof(Object instance, Class<?> castClass) {
		if (instance == null) return 0;	// null isn't an instance of anything
		Class<?> instanceClass = instance.getClass();
		if (instanceClass == castClass) {
			return 1;
		}
		return full_instanceof(instance, castClass);
	}

	// TODO: mark this with a "NoInline" annotation
	public static final int full_instanceof(Object instance, Class<?> castClass) {
		return 0; // TODO: full instance of support
	}
}
