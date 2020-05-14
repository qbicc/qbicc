package cc.quarkus.qcc.machine.probe;

import java.util.function.UnaryOperator;

final class ProbeUtil {
    private ProbeUtil() {}

    static UnaryOperator<StringBuilder> literal(String str) {
        return sb -> sb.append(str);
    }

    static UnaryOperator<StringBuilder> assocList(UnaryOperator<StringBuilder> type, UnaryOperator<StringBuilder> expr) {
        return sb -> expr.apply(type.apply(sb).append(':').append(' '));
    }

    static UnaryOperator<StringBuilder> csv(UnaryOperator<StringBuilder> i1, UnaryOperator<StringBuilder> i2) {
        return sb -> i2.apply(i1.apply(sb).append(','));
    }

    static UnaryOperator<StringBuilder> csv(UnaryOperator<StringBuilder> i1, UnaryOperator<StringBuilder> i2, UnaryOperator<StringBuilder> i3) {
        return csv(csv(i1, i2), i3);
    }

    static UnaryOperator<StringBuilder> csv(UnaryOperator<StringBuilder> i1, UnaryOperator<StringBuilder> i2, UnaryOperator<StringBuilder> i3, UnaryOperator<StringBuilder> i4) {
        return csv(csv(i1, i2), csv(i3, i4));
    }

    static UnaryOperator<StringBuilder> csv(UnaryOperator<StringBuilder> i1, UnaryOperator<StringBuilder> i2, UnaryOperator<StringBuilder> i3, UnaryOperator<StringBuilder> i4, UnaryOperator<StringBuilder> i5) {
        return csv(csv(i1, i2), csv(i3, i4), i5);
    }

    static UnaryOperator<StringBuilder> csv(UnaryOperator<StringBuilder> i1, UnaryOperator<StringBuilder> i2, UnaryOperator<StringBuilder> i3, UnaryOperator<StringBuilder> i4, UnaryOperator<StringBuilder> i5, UnaryOperator<StringBuilder> i6) {
        return csv(csv(i1, i2), csv(i3, i4), csv(i5, i6));
    }

    static UnaryOperator<StringBuilder> csv(UnaryOperator<StringBuilder> i1, UnaryOperator<StringBuilder> i2, UnaryOperator<StringBuilder> i3, UnaryOperator<StringBuilder> i4, UnaryOperator<StringBuilder> i5, UnaryOperator<StringBuilder> i6, UnaryOperator<StringBuilder> i7) {
        return csv(csv(i1, i2), csv(i3, i4), csv(i5, i6), i7);
    }

    static UnaryOperator<StringBuilder> generic(UnaryOperator<StringBuilder> object, UnaryOperator<StringBuilder> csv) {
        return sb -> csv.apply(object.apply(sb.append("_Generic(")).append(',')).append(')');
    }

    static UnaryOperator<StringBuilder> decl(UnaryOperator<StringBuilder> type, String name,
        UnaryOperator<StringBuilder> value) {
        return sb -> value.apply(type.apply(sb).append(' ').append(name).append(" = ")).append(";\n");
    }

    static UnaryOperator<StringBuilder> deref(UnaryOperator<StringBuilder> ptrExpr) {
        return sb -> ptrExpr.apply(sb.append('*'));
    }

    static UnaryOperator<StringBuilder> zeroPtrTo(UnaryOperator<StringBuilder> type) {
        return sb -> type.apply(sb.append("((")).append(" *) 0)");
    }

    static UnaryOperator<StringBuilder> memberOf(UnaryOperator<StringBuilder> type, String memberName) {
        return memberOfPtr(zeroPtrTo(type), memberName);
    }

    static UnaryOperator<StringBuilder> memberOfPtr(UnaryOperator<StringBuilder> ptrExpr, String memberName) {
        return sb -> ptrExpr.apply(sb).append("->").append(memberName);
    }

    static UnaryOperator<StringBuilder> union(String name, UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("union ").append(name));
    }

    static UnaryOperator<StringBuilder> struct(String name, UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("struct ").append(name));
    }

    static UnaryOperator<StringBuilder> sizeof(UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("sizeof(")).append(')');
    }

    static UnaryOperator<StringBuilder> alignof(UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("_Alignof(")).append(')');
    }

    static UnaryOperator<StringBuilder> offsetof(UnaryOperator<StringBuilder> type, String memberName) {
        return sb -> type.apply(sb.append("offsetof(")).append(',').append(memberName).append(')');
    }

    static UnaryOperator<StringBuilder> cast(UnaryOperator<StringBuilder> type, UnaryOperator<StringBuilder> expr) {
        return sb -> expr.apply(type.apply(sb.append("((")).append(')')).append(')');
    }

    static UnaryOperator<StringBuilder> isFloating(final UnaryOperator<StringBuilder> expr) {
        return generic(expr, csv(
            assocList(literal("float"), literal("1")),
            assocList(literal("double"), literal("1")),
            assocList(literal("long double"), literal("1")),
            assocList(literal("default"), literal("0")))
        );
    }

    static UnaryOperator<StringBuilder> isUnsigned(final UnaryOperator<StringBuilder> expr) {
        return generic(expr, csv(
            assocList(literal("unsigned char"), literal("1")),
            assocList(literal("unsigned short"), literal("1")),
            assocList(literal("unsigned int"), literal("1")),
            assocList(literal("unsigned long"), literal("1")),
            assocList(literal("unsigned long long"), literal("1")),
            assocList(literal("default"), literal("0")))
        );
    }

    static UnaryOperator<StringBuilder> isSigned(final UnaryOperator<StringBuilder> expr) {
        return generic(expr, csv(
            assocList(literal("signed char"), literal("1")),
            assocList(literal("signed short"), literal("1")),
            assocList(literal("signed int"), literal("1")),
            assocList(literal("signed long"), literal("1")),
            assocList(literal("signed long long"), literal("1")),
            assocList(literal("default"), literal("0")))
        );
    }

    static UnaryOperator<StringBuilder> definedRaw(String name, UnaryOperator<StringBuilder> ifTrue, UnaryOperator<StringBuilder> ifFalse) {
        return sb -> ifFalse.apply(ifTrue.apply(sb
                    .append(System.lineSeparator())
                    .append("#if defined(")
                    .append(name)
                    .append(")")
                    .append(System.lineSeparator())
                )
                .append(System.lineSeparator())
                .append("#else")
                .append(System.lineSeparator())
            )
            .append(System.lineSeparator())
            .append("#endif")
            .append(System.lineSeparator());
    }

    static UnaryOperator<StringBuilder> defined(String name) {
        return definedRaw(name, literal("1"), literal("0"));
    }

    static UnaryOperator<StringBuilder> definedValue(String name, UnaryOperator<StringBuilder> type) {
        return definedRaw(name, cast(type, literal(name)), cast(type, literal("{ 0 }")));
    }
}
