package org.qbicc.tests;

import org.qbicc.tests.snippets.ArithmeticCompare;
import org.qbicc.tests.snippets.ArithmeticNegation;
import org.qbicc.tests.snippets.Arrays;
import org.qbicc.tests.snippets.BadHashCode;
import org.qbicc.tests.snippets.ClassInit;
import org.qbicc.tests.snippets.ClassLiteralTests;
import org.qbicc.tests.snippets.DynamicTypeTests;
import org.qbicc.tests.snippets.InvokeInterface;
import org.qbicc.tests.snippets.InvokeVirtual;
import org.qbicc.tests.snippets.LoopTests;
import org.qbicc.tests.snippets.MathMinMax;
import org.qbicc.tests.snippets.MethodHandle;
import org.qbicc.tests.snippets.Reflection;
import org.qbicc.tests.snippets.ResourceLoading;
import org.qbicc.tests.snippets.RuntimeChecks;
import org.qbicc.tests.snippets.SelectorTest;
import org.qbicc.tests.snippets.ServiceLoading;
import org.qbicc.tests.snippets.Synchronized;
import org.qbicc.tests.snippets.TryCatch;

/**
 * The main test coordinator.
 */
public final class TestRunner {
    private TestRunner() {}

    public static void main(String[] args) throws Throwable {
        if (args.length == 0) {
            System.err.println("Expected an argument to indicate which test to run");
            System.exit(1);
            return; // unreachable
        }
        String test = args[0];
        String[] testArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
        switch (test) {
            case "snippet-ArithmeticCompare" -> ArithmeticCompare.main(testArgs);
            case "snippet-ArithmeticNegation" -> ArithmeticNegation.main(testArgs);
            case "snippet-Arrays" -> Arrays.main(testArgs);
            case "snippet-BadHashCode" -> BadHashCode.main(testArgs);
            case "snippet-ClassInit" -> ClassInit.main(testArgs);
            case "snippet-DynamicTypeTests" -> DynamicTypeTests.main(testArgs);
            case "snippet-InvokeInterface" -> InvokeInterface.main(testArgs);
            case "snippet-InvokeVirtual" -> InvokeVirtual.main(testArgs);
            case "snippet-LoopTests" -> LoopTests.main(testArgs);
            case "snippet-MathMinMax" -> MathMinMax.main(testArgs);
            case "snippet-MethodHandle" -> MethodHandle.main(testArgs);
            case "snippet-Reflection" -> Reflection.main(testArgs);
            case "snippet-ResourceLoading" -> ResourceLoading.main(testArgs);
            case "snippet-RuntimeChecks" -> RuntimeChecks.main(testArgs);
            case "snippet-SelectorTest" -> SelectorTest.main(testArgs);
            case "snippet-ServiceLoading" -> ServiceLoading.main(testArgs);
            case "snippet-TryCatch" -> TryCatch.main(testArgs);
            case "snippet-ClassLiteralTests" -> ClassLiteralTests.main(testArgs);
            case "snippet-Synchronized" -> Synchronized.main(testArgs);
            default -> {
                System.err.printf("Unknown test name \"%s\"%n", test);
                System.exit(1);
            }
        }
    }
}
