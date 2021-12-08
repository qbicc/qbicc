package org.qbicc.graph.atomic;

/**
 * The set of allowed access modes for various operations.
 * <p>
 * <h2>Access Mode Categories</h2>
 * <p>
 * Access modes each fall into zero or more of the following categories:
 * <ul>
 *     <li><em>Global access modes</em> which implement {@link GlobalAccessMode} or one of its subtypes and may be applied to fence operations</li>
 *     <li><em>Read access modes</em>, which implement {@link ReadAccessMode} or one of its subtypes and may be applied to read operations</li>
 *     <li><em>Write access modes</em>, which implement {@link WriteAccessMode} or one of its subtypes and may be applied to write operations</li>
 * </ul>
 * For any given access mode, it is possible to acquire the equivalent-or-stronger {@linkplain AccessMode#getGlobalAccess() global access mode},
 * the equivalent-or-stronger {@linkplain AccessMode#getReadAccess() read access mode}, and the equivalent-or-stronger
 * {@linkplain AccessMode#getWriteAccess() write access mode}.  The return type of these methods preserve the categorization of the original access mode,
 * so for example, given a read access mode, the corresponding global access mode will also be a valid read access mode.
 * Access modes which do not fall into any of the three basic categories may still be used to derive other usable access modes.
 * <p>
 * Access modes which are not global (i.e. do not implement the {@link GlobalAccessMode} interface) are considered "single".
 * These modes are only guaranteed to affect a single location in memory (some CPUs may strengthen this guarantee to
 * apply to some local memory <em>area</em> or <em>domain</em>).
 * <p>
 * <h2>Global Access Modes</h2>
 * <p>
 * The set of global access modes is based on definitions given {@linkplain java.lang.invoke.VarHandle in the JDK specification}
 * as well as <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html#jls-17.4">in the JLS</a>, with additional
 * references to background material such as <a href="http://gee.cs.oswego.edu/dl/html/j9mm.html">Using JDK 9 Memory Order Modes</a>
 * by Doug Lea and <a href="https://shipilev.net/blog/2014/jmm-pragmatics/">Java Memory Model Pragmatics</a> by Aleksey Shipilëv.
 * <p>
 * Most of the possible global access modes are compositions of these four fundamental modes:
 * <ul>
 *     <li>{@link #GlobalLoadLoad}</li>
 *     <li>{@link #GlobalStoreLoad}</li>
 *     <li>{@link #GlobalLoadStore}</li>
 *     <li>{@link #GlobalStoreStore}</li>
 * </ul>
 * These modes are only usable in fence operations, but since they are included in all other global access modes, they can
 * be strengthened to read or write operations using the {@link AccessMode#getReadAccess()} and {@link AccessMode#getWriteAccess()}
 * methods.
 * <p>
 * The following table illustrates which of the fundamental global access modes are included in which composed global
 * access modes:
 * <table class="striped">
 * <tr><th rowspan=2 align=center>Derived Access Mode</th><th colspan=4 align=center>Fundamental Access Mode</th></tr>
 * <tr><th>{@link #GlobalStoreStore}</th><th>{@link #GlobalLoadStore}</th><th>{@link #GlobalLoadLoad}</th><th>{@link #GlobalStoreLoad}</th></tr>
 * <tr><td align=center>{@link #GlobalAcquire}</td><td align=center></td><td align=center>●</td><td align=center>●</td><td align=center></td>
 * <tr><td align=center>{@link #GlobalRelease}</td><td align=center>●</td><td align=center>●</td><td align=center></td><td align=center></td>
 * <tr><td align=center>{@link #GlobalAcqRel}</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center></td>
 * <tr><td align=center>{@link #GlobalSeqCst}</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center>●</td>
 * </table>
 * <p>
 * Consequently, each fundamental barrier type can be strengthened to any of the modes which include that type.
 * <p>
 * The remaining global access modes — {@link #GlobalUnshared} and {@link #GlobalPlain} — are exactly equivalent to the
 * correspondingly-named single access modes, and exist to prevent useless fences from being emitted when the corresponding
 * single access mode is strengthened to a global access mode.
 * <p>
 * <h2>Single Access Modes</h2>
 * <p>
 * Single access modes only affect the memory directly associated with the operation that utilizes that mode, rather than
 * applying to all program memory operations as global access modes do.  As such, they are not composed of fundamental barrier
 * types like the global modes are, since those types are predicated on global ordering.
 * <p>
 * Instead, the single access types generally compose sequentially with only one exception. The following table illustrates
 * how each mode generally includes the next-weaker mode:
 * <p>
 * <table class="striped">
 * <tr><th rowspan=2 align=center>Access Mode</th><th colspan=7 align=center>Included Access Modes</th></tr>
 * <tr><th>{@link #SingleUnshared}</th><th>{@link #SinglePlain}</th><th>{@link #SingleOpaque}</th><th>{@link #SingleAcquire}</th><th>{@link #SingleRelease}</th><th>{@link #SingleAcqRel}</th><th>{@link #SingleSeqCst}</th></tr>
 * <tr><td align=center>{@link #SingleUnshared}</td><td align=center>●</td><td align=center></td><td align=center></td><td align=center></td><td align=center></td><td align=center></td><td align=center></td>
 * <tr><td align=center>{@link #SinglePlain}</td><td align=center>●</td><td align=center>●</td><td align=center></td><td align=center></td><td align=center></td><td align=center></td><td align=center></td>
 * <tr><td align=center>{@link #SingleOpaque}</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center></td><td align=center></td><td align=center></td><td align=center></td>
 * <tr><td align=center>{@link #SingleAcquire}</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center></td><td align=center></td><td align=center></td>
 * <tr><td align=center>{@link #SingleRelease}</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center></td><td align=center>●</td><td align=center></td><td align=center></td>
 * <tr><td align=center>{@link #SingleAcqRel}</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center></td>
 * <tr><td align=center>{@link #SingleSeqCst}</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center>●</td><td align=center>●</td>
 * </table>
 * <p>
 * Apart from {@link #SingleAcquire} and {@link #SingleRelease} — which do not include one another — every mode includes
 * all of the preceding modes.
 * <p>
 * Additionally, every single-access mode can be strengthened into a corresponding global access mode which includes at
 * least the behavior of the single-access mode when applied to the same memory operation.
 * <p>
 * <table class="striped">
 * <tr><th align=center>Single Access Mode</th><th align=center>Global Access mode</th></tr>
 * <tr><td align=center>{@link #SingleUnshared}</td><td align=center>{@link #GlobalUnshared}</td>
 * <tr><td align=center>{@link #SinglePlain}</td><td align=center>{@link #GlobalPlain}</td>
 * <tr><td align=center>{@link #SingleOpaque}</td><td align=center>{@link #GlobalSeqCst}</td>
 * <tr><td align=center>{@link #SingleAcquire}</td><td align=center>{@link #GlobalAcquire}</td>
 * <tr><td align=center>{@link #SingleRelease}</td><td align=center>{@link #GlobalRelease}</td>
 * <tr><td align=center>{@link #SingleAcqRel}</td><td align=center>{@link #GlobalAcqRel}</td>
 * <tr><td align=center>{@link #SingleSeqCst}</td><td align=center>{@link #GlobalSeqCst}</td>
 * </table>
 * <p>
 */
public final class AccessModes {
    private AccessModes() {}

    /**
     * The <em>global unshared</em> access mode.  This mode is exactly equivalent to {@link #SingleUnshared}, and
     * does nothing when used as a fence argument.
     */
    public static final GlobalReadWriteAccessMode GlobalUnshared = FullFences.GlobalUnshared;
    /**
     * The <em>global plain</em> access mode.  This mode is exactly equivalent to {@link #SinglePlain}, and
     * does nothing when used as a fence argument.
     */
    public static final GlobalReadWriteAccessMode GlobalPlain = FullFences.GlobalPlain;

    /**
     * The <em>load-load</em> global access mode.  When used as a fence, this mode
     * prevents any loads which precede the fence from being moved after the fence, and
     * prevents global loads which succeed the fence from being moved before the fence, as follows:
     * <p>
     * {@code AnyLoad ≺ GlobalLoadLoad ≺ AnyGlobalLoad}
     */
    public static final GlobalAccessMode GlobalLoadLoad = PureFences.GlobalLoadLoad;
    /**
     * The <em>store-load</em> global access mode.  When used as a fence, this mode
     * prevents global stores which precede the fence from being moved after the fence, and
     * prevents global loads which succeed the fence from being moved before the fence, as follows:
     * <p>
     * {@code AnyGlobalStore ≺ GlobalStoreLoad < AnyGlobalLoad}
     */
    public static final GlobalAccessMode GlobalStoreLoad = PureFences.GlobalStoreLoad;
    /**
     * The <em>load-store</em> global access mode.  When used as a fence, this mode
     * prevents any loads which precede the fence from being moved after the fence, and
     * prevents global stores which succeed the fence from being moved before the fence, as follows:
     * <p>
     * {@code AnyLoad ≺ GlobalLoadStore < AnyGlobalStore}
     */
    public static final GlobalAccessMode GlobalLoadStore = PureFences.GlobalLoadStore;
    /**
     * The <em>store-store</em> global access mode.  When used as a fence, this mode
     * prevents any stores which precede the fence from being moved after the fence, and
     * prevents any global stores which succeed the fence from being moved before the fence, as follows:
     * <p>
     * {@code AnyStore ≺ GlobalStoreStore < AnyGlobalStore}
     */
    public static final GlobalAccessMode GlobalStoreStore = PureFences.GlobalStoreStore;

    /**
     * The <em>acquire</em> global access mode.  This mode includes {@link #GlobalLoadLoad} and {@link #GlobalLoadStore} and
     * may be used with read and fence operations.
     */
    public static final GlobalReadAccessMode GlobalAcquire = ReadFences.GlobalAcquire;
    /**
     * The <em>release</em> global access mode.  This mode includes {@link #GlobalStoreStore} and {@link #GlobalLoadStore} and
     * may be used with write and fence operations.
     */
    public static final GlobalWriteAccessMode GlobalRelease = WriteFences.GlobalRelease;
    /**
     * The <em>acquire-release</em> global access mode.  This mode includes both {@link #GlobalAcquire} and {@link #GlobalRelease}
     * and may be used only with fence operations.
     */
    public static final GlobalAccessMode GlobalAcqRel = PureFences.GlobalAcqRel;
    /**
     * The <em>sequentially-consistent</em> global access mode, also known as {@code volatile}.
     * This mode includes both {@link #GlobalAcqRel} and {@link #GlobalStoreLoad}
     * and may be used with read, write, and fence operations.
     */
    public static final GlobalReadWriteAccessMode GlobalSeqCst = FullFences.GlobalSeqCst;

    /**
     * The <em>unshared</em> single access mode.  This is a non-atomic mode which provides no guarantees against
     * tearing or invalid values when not protected with an appropriate synchronization structure.
     * This mode may be used for read and write operations.
     */
    public static final ReadWriteAccessMode SingleUnshared = ReadWriteModes.SingleUnshared;
    /**
     * The <em>plain</em> single access mode, also known as <em>unordered</em>.  This is a mode which only guarantees
     * that the value will not be invalid in the presence of data races.
     * This mode may be used for read and write operations.
     */
    public static final ReadWriteAccessMode SinglePlain = ReadWriteModes.SinglePlain;
    /**
     * The <em>opaque</em> single access mode, also known as <em>relaxed</em> or <em>monotonic</em>.
     * This is a mode has all of the guarantees of {@link #SinglePlain} but also enforces a single total order
     * for all operations on a given memory address.
     * This mode may be used for read and write operations.
     */
    public static final ReadWriteAccessMode SingleOpaque = ReadWriteModes.SingleOpaque;
    /**
     * The <em>acquire</em> single access mode.
     * This is a mode has all of the guarantees of {@link #SingleOpaque} but also <em>synchronizes-with</em>
     * any <em>release</em> operation on the same memory address.
     * This mode may only be used for read operations.
     */
    public static final ReadAccessMode SingleAcquire = ReadModes.SingleAcquire;
    /**
     * The <em>release</em> single access mode.
     * This is a mode has all of the guarantees of {@link #SingleOpaque} but also <em>synchronizes-with</em>
     * any <em>acquire</em> operation on the same memory address.
     * This mode may only be used for write operations.
     */
    public static final WriteAccessMode SingleRelease = WriteModes.SingleRelease;
    /**
     * The <em>acquire-release</em> single access mode.
     * This mode combines {@link #SingleAcquire} with {@link #SingleRelease}.  It is not usable by itself
     * but is useful for compatibility with the previous scheme.
     */
    public static final AccessMode SingleAcqRel = PureModes.SingleAcqRel;
    /**
     * The <em>sequentially-consistent</em> single access mode.
     * This is a mode has all of the guarantees of {@link #SingleAcquire} and {@link #SingleRelease}
     * but also guarantees a global total order on all of the memory affected by this operation.
     * This mode may be used for read and write operations.
     */
    public static final ReadWriteAccessMode SingleSeqCst = ReadWriteModes.SingleSeqCst;
}
