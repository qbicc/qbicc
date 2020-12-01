package cc.quarkus.qcc.graph;

final class Util {
    private Util() {}

    static <T> T throwIndexOutOfBounds(final int index) {
        throw new IndexOutOfBoundsException(index);
    }
}
