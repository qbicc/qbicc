package org.qbicc.runtime.stackwalker;

import org.qbicc.runtime.CNative;
import org.qbicc.runtime.methoddata.MethodData;
import org.qbicc.runtime.stringpool.StringPoolAccessor;

public class JavaStackFrameVisitor implements StackFrameVisitor {
    private static int INITIAL_SIZE = 10;
    private int sourceCodeIndexList[];
    private int depth;

    JavaStackFrameVisitor() {
        sourceCodeIndexList = new int[INITIAL_SIZE];
        depth = 0;
    }

    private int findInstructionInMethodData(long ip) {
        // do a binary search in instruction table
        int upper = MethodData.getInstructionListSize();
        int lower = 0;
        while (upper >= lower) {
            int mid = (upper+lower)/2;
            long addr = MethodData.getInstructionAddress(mid).longValue();
            if (ip == addr) {
                return mid;
            } else if (ip > addr) {
                lower = mid+1;
            } else {
                upper = mid-1;
            }
        }
        return -1;
    }

    public boolean visitFrame(long ip, long sp) {
        int index = findInstructionInMethodData(ip);
        if (index != -1) {
            if (depth >= sourceCodeIndexList.length) {
                // reallocate larger array
                int[] previous = sourceCodeIndexList;
                sourceCodeIndexList = new int[depth + INITIAL_SIZE];
                System.arraycopy(previous, 0, sourceCodeIndexList, 0, previous.length);
            }
            sourceCodeIndexList[depth] = index;
            depth += 1;
        } else {
            // skip this frame; probably a native frame?
        }
        return true;
    }

    // Used in intrinsic for Throwable#fillInStackTrace
    public int getDepth() {
        return depth;
    }

    // Used in intrinsic for Throwable#fillInStackTrace
    public Object getBacktrace() {
        return sourceCodeIndexList;
    }

    private static native void fillStackTraceElement(StackTraceElement element, String className, String methodName, String fileName, int lineNumber);

    @CNative.extern
    public static native int putchar(int arg);

    // helper to print stack trace for debugging
    private static void printString(String string) {
        char[] contents = string.toCharArray();
        for (char ch: contents) {
            putchar((byte)ch);
        }
        putchar('\n');
    }

    // Used in intrinsic for StackTraceElement#initStackTraceElements
    public static void fillStackTraceElements(StackTraceElement[] steArray, Object backtrace, int depth) {
        int sourceCodeIndexList[] = (int[]) backtrace;
        for (int i = 0; i < depth; i++) {
            int scIndex = MethodData.getSourceCodeInfoIndex(sourceCodeIndexList[i]).intValue();
            int lineNumber = MethodData.getLineNumber(scIndex);
            int minfoIndex = MethodData.getMethodInfoIndex(scIndex);
            String className = StringPoolAccessor.getString(MethodData.getClassNameIndex(minfoIndex));
            String methodName = StringPoolAccessor.getString(MethodData.getMethodNameIndex(minfoIndex));
            String fileName = StringPoolAccessor.getString(MethodData.getFileNameIndex(minfoIndex));
            //printString(className + "#" + methodName + "(" + fileName + ":" + lineNumber + ")");
            fillStackTraceElement(steArray[i], className, methodName, fileName, lineNumber);
        }
    }
}
