import static org.qbicc.runtime.CNative.*;

public class MiscompileGC {
    @extern
    public static native int putchar(int arg);

    static class Item { 
        Thread current;
        public int i; 
        Item(Thread c) { current = c; }
    }

    static Item items[];

    public static void main(String[] args) {
        items = new Item[10];
        Item i = getItem(Thread.currentThread(), 5);
        if (i != null) {
            reportSuccess();
        } else {
            reportFailure();
        }
    }

    static Item getItem(Thread currentThread, int typeid) {
        int typeid_value = typeid;
        Item arrayState = items[typeid_value];
        if (arrayState != null) {
            return arrayState;
        }
        Item state = new Item(currentThread);
        arrayState = items[typeid_value];
        if (arrayState == null) {
            items[typeid_value] = state;
        } else {
            state = arrayState;
        }
        return state;
    }

    static void reportSuccess() {
        putchar('P');
        putchar('A');
        putchar('S');
        putchar('S');
        putchar('\n');
    }

    static void reportFailure() {
        putchar('F');
        putchar('A');
        putchar('I');
        putchar('L');
        putchar('\n');
    }
}
