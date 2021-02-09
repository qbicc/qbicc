package cc.quarkus.qcc.type.definition.classfile;

import java.nio.ByteBuffer;
import java.util.Arrays;

final class LineNumberTable {
    private final short[] lineNumbers; // format: start_pc line_number (alternating), sorted uniquely by start_pc (ascending)

    public LineNumberTable(final short[] lineNumbers) {
        this.lineNumbers = lineNumbers;
    }

    private static int findLineNumber(short[] table, int size, int bci) {
        if (table == null) {
            return -1;
        }
        int low = 0;
        int high = size - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = table[mid << 1] & 0xffff;
            if (midVal < bci) {
                low = mid + 1;
            } else if (midVal > bci) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -low - 1;
    }

    int getLineNumber(int bci) {
        int low = 0;
        int high = (lineNumbers.length >>> 1) - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = lineNumbers[mid << 1] & 0xffff;
            if (midVal < bci) {
                low = mid + 1;
            } else if (midVal > bci) {
                high = mid - 1;
            } else {
                // exact match
                return lineNumbers[(mid << 1) + 1];
            }
        }
        // return previous entry
        if (low == 0) {
            return 1;
        } else {
            return lineNumbers[((low - 1) << 1) + 1];
        }
    }

    int getMinimumLineNumber() {
        if (this.lineNumbers.length != 0) {
            int minimumLineNumber = Integer.MAX_VALUE;

            for (int i = 1; i < this.lineNumbers.length; i += 2) {
                minimumLineNumber = Math.min(minimumLineNumber, this.lineNumbers[i]);
            }

            return minimumLineNumber;
        } else {
            return 1;
        }
    }

    int getMaximumLineNumber() {
        if (this.lineNumbers.length != 0) {
            int maximumLineNumber = 1;

            for (int i = 1; i < this.lineNumbers.length; i += 2) {
                maximumLineNumber = Math.max(maximumLineNumber, this.lineNumbers[i]);
            }

            return maximumLineNumber;
        } else {
            return 1;
        }
    }

    static LineNumberTable createForCodeAttribute(final ClassFileImpl classFile, final ByteBuffer codeAttr) {
        // Skip max_stack and max_locals
        codeAttr.getShort();
        codeAttr.getShort();

        // Skip code
        int codeLen = codeAttr.getInt();
        codeAttr.position(codeAttr.position() + codeLen);

        // Skip exception_table
        short exTableLen = codeAttr.getShort();
        codeAttr.position(codeAttr.position() + exTableLen * 8);

        int attrCnt = codeAttr.getShort() & 0xffff;
        LineNumberTable.Builder b = new LineNumberTable.Builder();

        for (int i = 0; i < attrCnt; i++) {
            int nameIdx = codeAttr.getShort() & 0xffff;
            int len = codeAttr.getInt();

            if (classFile.utf8ConstantEquals(nameIdx, "LineNumberTable")) {
                b.appendTableFromAttribute(codeAttr.duplicate().limit(codeAttr.position() + len).slice());
            }

            codeAttr.position(codeAttr.position() + len);
        }

        return b.build();
    }

    static final class Builder {
        private static final short[] NO_SHORTS = new short[0];

        private short[] lineNumberTable = NO_SHORTS;
        private int lineNumberTableLen;

        Builder appendTableFromAttribute(ByteBuffer lntAttr) {
            int cnt = lntAttr.getShort() & 0xffff;

            // sanity check the length
            if (cnt << 2 != lntAttr.limit() - 2) {
                throw new InvalidAttributeLengthException();
            }

            // add some line numbers; this attribute can appear more than once
            if ((lineNumberTable.length >>> 1) - lineNumberTableLen < cnt) {
                lineNumberTable = Arrays.copyOf(lineNumberTable, lineNumberTableLen + (cnt << 1));
            }
            for (int j = 0; j < cnt; j ++) {
                int startPc = lntAttr.getShort() & 0xffff;
                int lineNumber = lntAttr.getShort() & 0xffff;
                if (lineNumberTable.length == 0) {
                    lineNumberTable = new short[cnt];
                    lineNumberTable[0] = (short) startPc;
                    lineNumberTable[1] = (short) lineNumber;
                } else {
                    int idx = findLineNumber(lineNumberTable, lineNumberTableLen, startPc);
                    if (idx < 0) {
                        idx = -idx - 1;
                        if (idx < lineNumberTableLen) {
                            // make a hole
                            System.arraycopy(lineNumberTable, idx << 1, lineNumberTable, 1 + (idx << 1), (lineNumberTableLen - idx) << 1);
                        }
                        // add list entry
                        lineNumberTable[idx << 1] = (short) startPc;
                        lineNumberTable[(idx << 1) + 1] = (short) lineNumber;
                        lineNumberTableLen++;
                    }
                }
            }

            return this;
        }

        LineNumberTable build() {
            short[] lineNumbers = (lineNumberTableLen << 1) == lineNumberTable.length ? lineNumberTable : Arrays.copyOf(lineNumberTable, lineNumberTableLen << 1);
            return new LineNumberTable(lineNumbers);
        }
    }
}
