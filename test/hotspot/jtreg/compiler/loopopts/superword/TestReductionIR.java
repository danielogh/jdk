/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/*
 * @test
 * @bug 8302139
 * @summary Test various reductions, verify results and IR
 * @requires vm.compiler2.enabled
 * @library /test/lib /
 * @run driver compiler.loopopts.superword.TestReductionIR
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::test*
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::verify*
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::fill*
   -XX:LoopUnrollLimit=250
   -XX:-TieredCompilation
   -XX:-SuperWordReductions
   -XX:LoopMaxUnroll=4
 * @run driver compiler.loopopts.superword.TestReductionIR
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::test*
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::verify*
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::fill*
   -XX:LoopUnrollLimit=250
   -XX:-TieredCompilation
   -XX:+SuperWordReductions
   -XX:LoopMaxUnroll=4
 * @run driver compiler.loopopts.superword.TestReductionIR
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::test*
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::verify*
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::fill*
   -XX:LoopUnrollLimit=250
   -XX:-TieredCompilation
   -XX:-SuperWordReductions
   -XX:LoopMaxUnroll=8
 * @run driver compiler.loopopts.superword.TestReductionIR
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::test*
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::verify*
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::fill*
   -XX:LoopUnrollLimit=250
   -XX:-TieredCompilation
   -XX:+SuperWordReductions
   -XX:LoopMaxUnroll=8
 * @run driver compiler.loopopts.superword.TestReductionIR
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::test*
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::verify*
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::fill*
   -XX:LoopUnrollLimit=250
   -XX:-TieredCompilation
   -XX:-SuperWordReductions
   -XX:LoopMaxUnroll=16
 * @run driver compiler.loopopts.superword.TestReductionIR
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::test*
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::verify*
   -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestReductionIR::fill*
   -XX:LoopUnrollLimit=250
   -XX:-TieredCompilation
   -XX:+SuperWordReductions
   -XX:LoopMaxUnroll=16
*/

// TODO:
// int / long: min, max - seems to be broken? -- externally blocked
// SumRedAbsNeg_Double.java, SumRedAbsNeg_Float.java -- Double not working in original test
// Vec_MulAddS2I.java ? -- added
// Lower than SVE? Rules for other platforms? -- cant test, skipping
// Remove the unroll 2 scenario, add another one instead? -- removed
// Extend randomize inputs? -- no action, would be nice to try different formulas for reductions.
// Separate test descriptions instead of scenarios? -- done.
// Test methods not using shared fields / data structures -- mostly done, kept finals
// more types (byte, char, short) -- not working good, skipping
// tweak SSE/AVX values -- done

/* Tested reductions:
   - Add
     + (DoubleSqrt)
     + AbsNeg (not working for double)
   - Mul
   - Xor
   - And
   - Or
   - Min/Max : broken for int, not working for Long
   byte, char, short not working good. Skipping for now.
*/


package compiler.loopopts.superword;
import compiler.lib.ir_framework.*;

public class TestReductionIR {
    static final int RANGE = 512;
    static final int REPETITIONS = 100;

    public static void main(String args[]) {
        TestFramework framework = new TestFramework();
        framework.start();
    }

    // ------------------------------------ ReductionAddInt --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.ADD_VI, "= 0", IRNode.MUL_VI, "= 0",
                  IRNode.ADD_REDUCTION_VI, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.ADD_VI, "> 0", IRNode.MUL_VI, "> 0",
                  IRNode.ADD_REDUCTION_VI, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"sse4.1", "true", "sve", "true"})
    public int testReductionAddInt(int[] a, int[] b, int[] c, int sum) {
        for (int i = 0; i < RANGE; i++) {
            sum += (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
        }
        return sum;
    }

    // Not compiled.
    public int referenceReductionAddInt(int[] a, int[] b, int[] c, int sum) {
        for (int i = 0; i < RANGE; i++) {
            sum += (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
        }
        return sum;
    }

    @Run(test = "testReductionAddInt")
    @Warmup(0)
    public void runTestReductionAddInt() {
        int[] iArrA = new int[RANGE];
        int[] iArrB = new int[RANGE];
        int[] iArrC = new int[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(iArrA, iArrB, iArrC);
            int init = RunInfo.getRandom().nextInt();
            int s0 = testReductionAddInt(iArrA, iArrB, iArrC, init);
            int s1 = referenceReductionAddInt(iArrA, iArrB, iArrC, init);
            verify("testReductionAddInt sum", s0, s1);
        }
    }

    // ------------------------------------ ReductionMulInt --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.SUB_VI, "= 0", IRNode.MUL_REDUCTION_VI, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.SUB_VI, "> 0", IRNode.MUL_REDUCTION_VI, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"sse4.1", "true", "sve", "true"})
    public int testReductionMulInt(int[] a, int[] b, int mul) {
        for (int i = 0; i < RANGE; i++) {
            mul *= a[i] - b[i];
        }
        return mul;
    }

    // Not compiled.
    public int referenceReductionMulInt(int[] a, int[] b, int mul) {
        for (int i = 0; i < RANGE; i++) {
            mul *= a[i] - b[i];
        }
        return mul;
    }

    @Run(test = "testReductionMulInt")
    @Warmup(0)
    public void runTestReductionMulInt() {
        int[] iArrA = new int[RANGE];
        int[] iArrB = new int[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillSmallPrimeDiff(iArrA, iArrB);
            int init = fillSmallPrime();
            int s0 = testReductionMulInt(iArrA, iArrB, init);
            int s1 = referenceReductionMulInt(iArrA, iArrB, init);
            verify("testReductionMulInt sum", s0, s1);
            if (s0 == 0) {
                throw new RuntimeException("Primes should not multiply to zero in int-ring.");
            }
        }
    }

    // ------------------------------------ ReductionXorInt --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.ADD_VI, "= 0", IRNode.XOR_REDUCTION_V, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.ADD_VI, "> 0", IRNode.XOR_REDUCTION_V, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"sse4.1", "true", "sve", "true"})
    public int testReductionXorInt(int[] a, int[] b, int sum) {
        for (int i = 0; i < RANGE; i++) {
            sum ^= a[i] + b[i];
        }
        return sum;
    }

    // Not compiled.
    public int referenceReductionXorInt(int[] a, int[] b, int sum) {
        for (int i = 0; i < RANGE; i++) {
            sum ^= a[i] + b[i];
        }
        return sum;
    }

    @Run(test = "testReductionXorInt")
    @Warmup(0)
    public void runTestReductionXorInt() {
        int[] iArrA = new int[RANGE];
        int[] iArrB = new int[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(iArrA, iArrB);
            int init = RunInfo.getRandom().nextInt();
            int s0 = testReductionXorInt(iArrA, iArrB, init);
            int s1 = referenceReductionXorInt(iArrA, iArrB, init);
            verify("testReductionXorInt sum", s0, s1);
        }
    }

    // ------------------------------------ ReductionAndInt --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.SUB_VI, "= 0", IRNode.AND_REDUCTION_V, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.SUB_VI, "> 0", IRNode.AND_REDUCTION_V, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"sse4.1", "true", "sve", "true"})
    public int testReductionAndInt(int[] a, int[] b, int sum) {
        for (int i = 0; i < RANGE; i++) {
            sum &= a[i] - b[i];
        }
        return sum;
    }

    // Not compiled.
    public int referenceReductionAndInt(int[] a, int[] b, int sum) {
        for (int i = 0; i < RANGE; i++) {
            sum &= a[i] - b[i];
        }
        return sum;
    }

    @Run(test = "testReductionAndInt")
    @Warmup(0)
    public void runTestReductionAndInt() {
        int[] iArrA = new int[RANGE];
        int[] iArrB = new int[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillSpecialBytes(iArrA, iArrB, 0xFFFFFFFF);
            int init = 0xFFFFFFFF; // start with all bits
            int s0 = testReductionAndInt(iArrA, iArrB, init);
            int s1 = referenceReductionAndInt(iArrA, iArrB, init);
            verify("testReductionAndInt sum", s0, s1);
            if (s0 == 0 || s0 == 0xFFFFFFFF) {
                throw new RuntimeException("Test should not collapse. " + s0);
            }
	}
    }

    // ------------------------------------ ReductionOrInt --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.SUB_VI, "= 0", IRNode.OR_REDUCTION_V, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.SUB_VI, "> 0", IRNode.OR_REDUCTION_V, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"sse4.1", "true", "sve", "true"})
    public int testReductionOrInt(int[] a, int[] b, int sum) {
        for (int i = 0; i < RANGE; i++) {
            sum |= a[i] - b[i];
        }
        return sum;
    }

    // Not compiled.
    public int referenceReductionOrInt(int[] a, int[] b, int sum) {
        for (int i = 0; i < RANGE; i++) {
            sum |= a[i] - b[i];
        }
        return sum;
    }

    @Run(test = "testReductionOrInt")
    @Warmup(0)
    public void runTestReductionOrInt() {
        int[] iArrA = new int[RANGE];
        int[] iArrB = new int[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillSpecialBytes(iArrA, iArrB, 0);
            int init = 0; // start with no bits
            int s0 = testReductionOrInt(iArrA, iArrB, init);
            int s1 = referenceReductionOrInt(iArrA, iArrB, init);
            verify("testReductionOrInt sum", s0, s1);
            if (s0 == 0 || s0 == 0xFFFFFFFF) {
                throw new RuntimeException("Test should not collapse. " + s0);
            }
	    }
    }

// TODO Add once it works
//     // ------------------------------------ ReductionMinInt --------------------------------------------------
// 
//     @Test
//     @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.MUL_VI, "= 0",
//                   IRNode.MIN_REDUCTION_VI, "= 0"},
//         applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
//     @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.MUL_VI, "> 0",
//                   IRNode.MIN_REDUCTION_VI, "> 0"},
//         applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
//         applyIfCPUFeatureOr = {"sse4.1", "true", "sve", "true"})
//     public int testReductionMinInt(int[] a, int sum) {
//         for (int i = 0; i < RANGE; i++) {
//             sum = Math.min(sum, a[i] * 11);
//         }
//         return sum;
//     }
// 
//     // Not compiled.
//     public int referenceReductionMinInt(int[] a, int sum) {
//         for (int i = 0; i < RANGE; i++) {
//             sum = Math.min(sum, a[i] * 11);
//         }
//         return sum;
//     }
// 
//     @Run(test = "testReductionMinInt")
//     @Warmup(0)
//     public void runTestReductionMinInt() {
//         for (int j = 0; j < REPETITIONS; j++) {
//             fillRandom(iArrA);
//             int init = RunInfo.getRandom().nextInt();
//             int s0 = testReductionMinInt(iArrA, init);
//             int s1 = referenceReductionMinInt(iArrA, init);
//             verify("testReductionMinInt sum", s0, s1);
//         }
//     }

//     // ------------------------------------ ReductionMaxInt --------------------------------------------------
//
//     @Test
//     @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.MUL_VI, "= 0",
//                   IRNode.MAX_REDUCTION_VI, "= 0"},
//         applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
//     @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.MUL_VI, "> 0",
//                   IRNode.MAX_REDUCTION_VI, "> 0"},
//         applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
//         applyIfCPUFeatureOr = {"sse4.1", "true", "sve", "true"})
//     public int testReductionMaxInt(int[] a, int sum) {
//         for (int i = 0; i < RANGE; i++) {
//             sum = Math.max(sum, a[i] * 11);
//         }
//         return sum;
//     }
//
//     // Not compiled.
//     public int referenceReductionMaxInt(int[] a, int sum) {
//         for (int i = 0; i < RANGE; i++) {
//             sum = Math.max(sum, a[i] * 11);
//         }
//         return sum;
//     }
//
//     @Run(test = "testReductionMaxInt")
//     @Warmup(0)
//     public void runTestReductionMaxInt() {
//         for (int j = 0; j < REPETITIONS; j++) {
//             fillRandom(iArrA);
//             int init = RunInfo.getRandom().nextInt();
//             int s0 = testReductionMaxInt(iArrA, init);
//             int s1 = referenceReductionMaxInt(iArrA, init);
//             verify("testReductionMaxInt sum", s0, s1);
//         }
//     }


    // ------------------------------------ ReductionAddLong --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.ADD_VL, "= 0", IRNode.MUL_VL, "= 0",
                  IRNode.ADD_REDUCTION_VL, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.ADD_VL, "> 0", IRNode.MUL_VL, "> 0",
                  IRNode.ADD_REDUCTION_VL, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"avx2", "true", "sve", "true"},
        applyIfPlatformFeature = {"32-bit", "false"})
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0"},
        applyIfCPUFeatureAnd = {"sse4.1", "true", "avx2", "false"})
    public long testReductionAddLong(long[] a, long[] b, long[] c, long sum) {
        for (int i = 0; i < RANGE; i++) {
            // Note: need at least AVX2 to have more than 2 longs in vector.
            sum += (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
        }
        return sum;
    }

    // Not compiled.
    public long referenceReductionAddLong(long[] a, long[] b, long[] c, long sum) {
        for (int i = 0; i < RANGE; i++) {
            sum += (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
        }
        return sum;
    }

    @Run(test = "testReductionAddLong")
    @Warmup(0)
    public void runTestReductionAddLong() {
        long[] lArrA = new long[RANGE];
        long[] lArrB = new long[RANGE];
        long[] lArrC = new long[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(lArrA, lArrB, lArrC);
            long init = RunInfo.getRandom().nextLong();
            long s0 = testReductionAddLong(lArrA, lArrB, lArrC, init);
            long s1 = referenceReductionAddLong(lArrA, lArrB, lArrC, init);
            verify("testReductionAddLong sum", s0, s1);
        }
    }

    // ------------------------------------ ReductionMulLong --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.SUB_VL, "= 0",
                  IRNode.MUL_REDUCTION_VL, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.SUB_VL, "> 0",
                  IRNode.MUL_REDUCTION_VL, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"avx512dq", "true", "sve", "true"},
        applyIfPlatformFeature = {"32-bit", "false"})
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0"},
        applyIfCPUFeatureAnd = {"sse4.1", "true", "avx2", "false"})
    public long testReductionMulLong(long[] a, long[] b, long[] c, long mul) {
        for (int i = 0; i < RANGE; i++) {
            // Note: requires AVX3 to vectorize.
            mul *= a[i] - b[i];
        }
        return mul;
    }

    // Not compiled.
    public long referenceReductionMulLong(long[] a, long[] b, long[] c, long mul) {
        for (int i = 0; i < RANGE; i++) {
            mul *= a[i] - b[i];
        }
        return mul;
    }

    @Run(test = "testReductionMulLong")
    @Warmup(0)
    public void runTestReductionMulLong() {
        long[] lArrA = new long[RANGE];
        long[] lArrB = new long[RANGE];
        long[] lArrC = new long[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillSmallPrimeDiff(lArrA, lArrB);
            long init = fillSmallPrime();
            long s0 = testReductionMulLong(lArrA, lArrB, lArrC, init);
            long s1 = referenceReductionMulLong(lArrA, lArrB, lArrC, init);
            verify("testReductionMulLong mul", s0, s1);
            if (s0 == 0) {
                throw new RuntimeException("Primes should not multiply to zero in long-ring.");
            }
	}
    }

    // ------------------------------------ ReductionXorLong --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.ADD_VL, "= 0",
                  IRNode.XOR_REDUCTION_V, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.ADD_VL, "> 0",
                  IRNode.XOR_REDUCTION_V, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"avx2", "true", "sve", "true"},
        applyIfPlatformFeature = {"32-bit", "false"})
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0"},
        applyIfCPUFeatureAnd = {"sse4.1", "true", "avx2", "false"})
    public long testReductionXorLong(long[] a, long[] b, long sum) {
        for (int i = 0; i < RANGE; i++) {
            // Note: need at least AVX2 to have more than 2 longs in vector.
            sum ^= a[i] + b[i];
        }
        return sum;
    }

    // Not compiled.
    public long referenceReductionXorLong(long[] a, long[] b, long sum) {
        for (int i = 0; i < RANGE; i++) {
            sum ^= a[i] + b[i];
        }
        return sum;
    }

    @Run(test = "testReductionXorLong")
    @Warmup(0)
    public void runTestReductionXorLong() {
        long[] lArrA = new long[RANGE];
        long[] lArrB = new long[RANGE];
        long[] lArrC = new long[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(lArrA, lArrB, lArrC);
            long init = RunInfo.getRandom().nextLong();
            long s0 = testReductionXorLong(lArrA, lArrB, init);
            long s1 = referenceReductionXorLong(lArrA, lArrB, init);
            verify("testReductionXorLong sum", s0, s1);
        }
    }

    // ------------------------------------ ReductionAndLong --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.SUB_VL, "= 0",
                  IRNode.AND_REDUCTION_V, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.SUB_VL, "> 0",
                  IRNode.AND_REDUCTION_V, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"avx2", "true", "sve", "true"},
        applyIfPlatformFeature = {"32-bit", "false"})
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0"},
        applyIfCPUFeatureAnd = {"sse4.1", "true", "avx2", "false"})
    public long testReductionAndLong(long[] a, long[] b, long sum) {
        for (int i = 0; i < RANGE; i++) {
            // Note: need at least AVX2 to have more than 2 longs in vector.
            sum &= a[i] - b[i];
        }
        return sum;
    }

    // Not compiled.
    public long referenceReductionAndLong(long[] a, long[] b, long sum) {
        for (int i = 0; i < RANGE; i++) {
            sum &= a[i] - b[i];
        }
        return sum;
    }

    @Run(test = "testReductionAndLong")
    @Warmup(0)
    public void runTestReductionAndLong() {
        long[] lArrA = new long[RANGE];
        long[] lArrB = new long[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillSpecialBytes(lArrA, lArrB, 0xFFFFFFFFFFFFFFFFL);
            long init = 0xFFFFFFFFFFFFFFFFL; // start with all bits
            long s0 = testReductionAndLong(lArrA, lArrB, init);
            long s1 = referenceReductionAndLong(lArrA, lArrB, init);
            verify("testReductionAndLong sum", s0, s1);
            if (s0 == 0 || s0 == 0xFFFFFFFFFFFFFFFFL) {
                throw new RuntimeException("Test should not collapse. " + s0);
            }
        }
    }

    // ------------------------------------ ReductionOrLong --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.SUB_VL, "= 0",
                  IRNode.OR_REDUCTION_V, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.SUB_VL, "> 0",
                  IRNode.OR_REDUCTION_V, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"avx2", "true", "sve", "true"},
        applyIfPlatformFeature = {"32-bit", "false"})
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0"},
        applyIfCPUFeatureAnd = {"sse4.1", "true", "avx2", "false"})
    public long testReductionOrLong(long[] a, long[] b, long sum) {
        for (int i = 0; i < RANGE; i++) {
            // Note: need at least AVX2 to have more than 2 longs in vector.
            sum |= a[i] - b[i];
        }
        return sum;
    }

    // Not compiled.
    public long referenceReductionOrLong(long[] a, long[] b, long sum) {
        for (int i = 0; i < RANGE; i++) {
            sum |= a[i] - b[i];
        }
        return sum;
    }

    @Run(test = "testReductionOrLong")
    @Warmup(0)
    public void runTestReductionOrLong() {
        long[] lArrA = new long[RANGE];
        long[] lArrB = new long[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillSpecialBytes(lArrA, lArrB, 0);
            long init = 0; // start with no bits
            long s0 = testReductionOrLong(lArrA, lArrB, init);
            long s1 = referenceReductionOrLong(lArrA, lArrB, init);
            verify("testReductionOrLong sum", s0, s1);
            if (s0 == 0 || s0 == 0xFFFFFFFFFFFFFFFFL) {
                throw new RuntimeException("Test should not collapse. " + s0);
            }
        }
    }

//    // ------------------------------------ ReductionMinLong --------------------------------------------------
//
//    @Test
//    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.MUL_VL, "= 0",
//                  IRNode.MIN_REDUCTION_V, "= 0"},
//        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
//    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.MUL_VL, "> 0",
//                  IRNode.MIN_REDUCTION_V, "> 0"},
//        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 8"},
//        applyIfCPUFeatureAnd = {"avx512dq", "true", "avx512vl", "true", "avx512bw", "true"},
//        applyIfPlatformFeature = {"32-bit", "false"})
//    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.MUL_VL, "> 0",
//                  IRNode.MIN_REDUCTION_V, "> 0"},
//        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 8"},
//        applyIfCPUFeature = {"sve", "true"},
//        applyIfPlatformFeature = {"32-bit", "false"})
//    public long testReductionMinLong(long[] a, long sum) {
//        // Min/Max Long require avx512vlbwdq
//        for (int i = 0; i < RANGE; i++) {
//            sum = Math.min(sum, a[i] * 11L);
//        }
//        return sum;
//    }
//
//    // Not compiled.
//    public long referenceReductionMinLong(long[] a, long sum) {
//        for (int i = 0; i < RANGE; i++) {
//            sum = Math.min(sum, a[i] * 11L);
//        }
//        return sum;
//    }
//
//    @Run(test = "testReductionMinLong")
//    @Warmup(0)
//    public void runTestReductionMinLong() {
//        long[] lArrA = new long[RANGE];
//        for (int j = 0; j < REPETITIONS; j++) {
//            fillRandom(lArrA);
//            long init = RunInfo.getRandom().nextLong();
//            long s0 = testReductionMinLong(lArrA, init);
//            long s1 = referenceReductionMinLong(lArrA, init);
//            verify("testReductionMinLong sum", s0, s1);
//        }
//    }
//
//    // ------------------------------------ ReductionMaxLong --------------------------------------------------
//
//    @Test
//    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.MUL_VL, "= 0",
//                  IRNode.MAX_REDUCTION_V, "= 0"},
//        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
//    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.MUL_VL, "> 0",
//                  IRNode.MAX_REDUCTION_V, "> 0"},
//        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 8"},
//        applyIfCPUFeatureAnd = {"avx512dq", "true", "avx512vl", "true", "avx512bw", "true"},
//        applyIfPlatformFeature = {"32-bit", "false"})
//    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.MUL_VL, "> 0",
//                  IRNode.MAX_REDUCTION_V, "> 0"},
//        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 8"},
//        applyIfCPUFeature = {"sve", "true"},
//        applyIfPlatformFeature = {"32-bit", "false"})
//    public long testReductionMaxLong(long[] a, long sum) {
//        for (int i = 0; i < RANGE; i++) {
//            sum = Math.max(sum, a[i] * 123456789L);
//        }
//        return sum;
//    }
//
//    // Not compiled.
//    public long referenceReductionMaxLong(long[] a, long sum) {
//        for (int i = 0; i < RANGE; i++) {
//            sum = Math.max(sum, a[i] * 123456789L);
//        }
//        return sum;
//    }
//
//    @Run(test = "testReductionMaxLong")
//    @Warmup(0)
//    public void runTestReductionMaxLong() {
//        long[] lArrA = new long[RANGE];
//        for (int j = 0; j < REPETITIONS; j++) {
//            fillRandom(lArrA);
//            long init = RunInfo.getRandom().nextLong();
//            long s0 = testReductionMaxLong(lArrA, init);
//            long s1 = referenceReductionMaxLong(lArrA, init);
//            verify("testReductionMaxLong sum", s0, s1);
//        }
//    }

    // ------------------------------------ ReductionAddFloat --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.ADD_VF, "= 0", IRNode.MUL_VF, "= 0",
                  IRNode.ADD_REDUCTION_VF, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.ADD_VF, "> 0", IRNode.MUL_VF, "> 0",
                  IRNode.ADD_REDUCTION_VF, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfPlatformFeature = {"32-bit", "false"},
        applyIfCPUFeatureOr = {"sse2", "true", "sve", "true"})
    public float testReductionAddFloat(float[] a, float[] b, float[] c, float sum) {
        for (int i = 0; i < RANGE; i++) {
            sum += (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
        }
        return sum;
    }

    // Not compiled.
    public float referenceReductionAddFloat(float[] a, float[] b, float[] c, float sum) {
        for (int i = 0; i < RANGE; i++) {
            sum += (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
        }
        return sum;
    }

    @Run(test = "testReductionAddFloat")
    @Warmup(0)
    public void runTestReductionAddFloat() {
        float[] fArrA = new float[RANGE];
        float[] fArrB = new float[RANGE];
        float[] fArrC = new float[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(fArrA, fArrB, fArrC);
            float init = RunInfo.getRandom().nextFloat();
            float s0 = testReductionAddFloat(fArrA, fArrB, fArrC, init);
            // // Comment: reduction order for floats matters. Swapping order leads to wrong results.
            // // To verify: uncomment code below.
            // float tmpA = fArrA[50];
            // float tmpB = fArrB[50];
            // float tmpC = fArrC[50];
            // fArrA[50] = fArrA[51];
            // fArrB[50] = fArrB[51];
            // fArrC[50] = fArrC[51];
            // fArrA[51] = tmpA;
            // fArrB[51] = tmpB;
            // fArrC[51] = tmpC;
            float s1 = referenceReductionAddFloat(fArrA, fArrB, fArrC, init);
            verify("testReductionAddFloat sum", s0, s1);
            if (s0 == 0.0f || Float.isNaN(s0) || Float.isInfinite(s0)) {
                throw new RuntimeException("Test should not collapse. " + s0);
            }
        }
    }

    // ------------------------------------ ReductionMinFloat --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.MUL_VF, "= 0", IRNode.MIN_REDUCTION_V, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.MUL_VF, "> 0", IRNode.MIN_REDUCTION_V, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"avx", "true", "sve", "true"},
        applyIfPlatformFeature = {"32-bit", "false"})
    public float testReductionMinFloat(float[] a, float sum) {
        for (int i = 0; i < RANGE; i++) {
            // Note: MinReductionV requires at least AVX for float.
            sum = Math.min(sum, a[i] * 5.5f);
        }
        return sum;
    }

    // Not compiled.
    public float referenceReductionMinFloat(float[] a, float sum) {
        for (int i = 0; i < RANGE; i++) {
            sum = Math.min(sum, a[i] * 5.5f);
        }
        return sum;
    }

    @Run(test = "testReductionMinFloat")
    @Warmup(0)
    public void runTestReductionMinFloat() {
        float[] fArrA = new float[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(fArrA);
            float init = RunInfo.getRandom().nextFloat();
            float s0 = testReductionMinFloat(fArrA, init);
            float s1 = referenceReductionMinFloat(fArrA, init);
            verify("testReductionMinFloat sum", s0, s1);
        }
    }

    // ------------------------------------ ReductionMaxFloat --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.MUL_VF, "= 0", IRNode.MAX_REDUCTION_V, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.MUL_VF, "> 0", IRNode.MAX_REDUCTION_V, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"avx", "true", "sve", "true"},
        applyIfPlatformFeature = {"32-bit", "false"})
    public float testReductionMaxFloat(float[] a, float sum) {
        for (int i = 0; i < RANGE; i++) {
            // Note: MaxReductionV requires at least AVX for float.
            sum = Math.max(sum, a[i] * 5.5f);
        }
        return sum;
    }

    // Not compiled.
    public float referenceReductionMaxFloat(float[] a, float sum) {
        for (int i = 0; i < RANGE; i++) {
            sum = Math.max(sum, a[i] * 5.5f);
        }
        return sum;
    }

    @Run(test = "testReductionMaxFloat")
    @Warmup(0)
    public void runTestReductionMaxFloat() {
        float[] fArrA = new float[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(fArrA);
            float init = RunInfo.getRandom().nextFloat();
            float s0 = testReductionMaxFloat(fArrA, init);
            float s1 = referenceReductionMaxFloat(fArrA, init);
            verify("testReductionMaxFloat sum", s0, s1);
        }
    }

    // ------------------------------------ ReductionAddAbsNegFloat --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.ADD_VF, "= 0", IRNode.MUL_VF, "= 0",
                  IRNode.ADD_REDUCTION_VF, "= 0", IRNode.ABS_V, "= 0", IRNode.NEG_V, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.ADD_VF, "> 0", IRNode.MUL_VF, "> 0",
                  IRNode.ADD_REDUCTION_VF, "> 0", IRNode.ABS_V, "> 0", IRNode.NEG_V, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"sse4.1", "true", "sve", "true"})
    public float testReductionAddAbsNegFloat(float[] a, float[] b, float[] c, float sum) {
        for (int i = 0; i < RANGE; i++) {
            sum += Math.abs(-a[i] * -b[i]) + Math.abs(-a[i] * -c[i]) + Math.abs(-b[i] * -c[i]);
        }
        return sum;
    }

    // Not compiled.
    public float referenceReductionAddAbsNegFloat(float[] a, float[] b, float[] c, float sum) {
        for (int i = 0; i < RANGE; i++) {
            sum += Math.abs(-a[i] * -b[i]) + Math.abs(-a[i] * -c[i]) + Math.abs(-b[i] * -c[i]);
        }
        return sum;
    }

    @Run(test = "testReductionAddAbsNegFloat")
    @Warmup(0)
    public void runTestReductionAddAbsNegFloat() {
        float[] fArrA = new float[RANGE];
        float[] fArrB = new float[RANGE];
        float[] fArrC = new float[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(fArrA, fArrB, fArrC);
            float init = RunInfo.getRandom().nextFloat();
            float s0 = testReductionAddAbsNegFloat(fArrA, fArrB, fArrC, init);
            float s1 = referenceReductionAddAbsNegFloat(fArrA, fArrB, fArrC, init);
            verify("testReductionAddAbsNegFloat sum", s0, s1);
        }
    }

    // ------------------------------------ ReductionAddDouble --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.ADD_VD, "= 0", IRNode.MUL_VD, "= 0", IRNode.STORE_VECTOR, "= 0",
                  IRNode.ADD_REDUCTION_VD, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.ADD_VD, "> 0", IRNode.MUL_VD, "> 0", IRNode.STORE_VECTOR, "> 0",
                  IRNode.ADD_REDUCTION_VD, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"sse2", "true", "sve", "true"})
    public double testReductionAddDouble(double[] a, double[] b, double[] c, double[] r, double sum) {
        for (int i = 0; i < RANGE; i++) {
            r[i] = (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
            sum += r[i]; // TODO: Store is required for double reduction.
        }
        return sum;
    }

    // Not compiled.
    public double referenceReductionAddDouble(double[] a, double[] b, double[] c, double[] r, double sum) {
        for (int i = 0; i < RANGE; i++) {
            r[i] = (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
            sum += r[i]; // TODO: Store is required for double reduction.
        }
        return sum;
    }

    @Run(test = "testReductionAddDouble")
    @Warmup(0)
    public void runTestReductionAddDouble() {
        double[] dArrA = new double[RANGE];
        double[] dArrB = new double[RANGE];
        double[] dArrC = new double[RANGE];
        double[] dArrR0 = new double[RANGE];
        double[] dArrR1 = new double[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(dArrA, dArrB, dArrC);
            double init = RunInfo.getRandom().nextDouble();
            double s0 = testReductionAddDouble(dArrA, dArrB, dArrC, dArrR0, init);
            // // Comment: reduction order for doubles matters. Swapping order leads to wrong results.
            // // To verify: uncomment code below, and comment out the r verification.
            // double tmpA = dArrA[50];
            // double tmpB = dArrB[50];
            // double tmpC = dArrC[50];
            // dArrA[50] = dArrA[51];
            // dArrB[50] = dArrB[51];
            // dArrC[50] = dArrC[51];
            // dArrA[51] = tmpA;
            // dArrB[51] = tmpB;
            // dArrC[51] = tmpC;
            double s1 = referenceReductionAddDouble(dArrA, dArrB, dArrC, dArrR1, init);
            verify("testReductionAddDouble sum", s0, s1);
            verify("testReductionAddDouble r", dArrR0, dArrR1);
            if (s0 == 0.0f || Double.isNaN(s0) || Double.isInfinite(s0)) {
                throw new RuntimeException("Test should not collapse. " + s0);
            }
	}
    }

    // ------------------------------------ ReductionAddDoubleSqrt --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.ADD_VD, "= 0", IRNode.MUL_VD, "= 0", IRNode.STORE_VECTOR, "= 0",
                  IRNode.ADD_REDUCTION_VD, "= 0", IRNode.SQRT_VD, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.ADD_VD, "> 0", IRNode.MUL_VD, "> 0", IRNode.STORE_VECTOR, "> 0",
                  IRNode.ADD_REDUCTION_VD, "> 0", IRNode.SQRT_VD, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"avx", "true", "sve", "true"})
    public double testReductionAddDoubleSqrt(double[] a, double[] b, double[] c, double[] r, double sum) {
        for (int i = 0; i < RANGE; i++) {
            r[i] = Math.sqrt(a[i] * b[i]) + Math.sqrt(a[i] * c[i]) + Math.sqrt(b[i] * c[i]);
            sum += r[i]; // TODO: Store is required for double reduction.
        }
        return sum;
    }

    // Not compiled.
    public double referenceReductionAddDoubleSqrt(double[] a, double[] b, double[] c, double[] r, double sum) {
        for (int i = 0; i < RANGE; i++) {
            r[i] = Math.sqrt(a[i] * b[i]) + Math.sqrt(a[i] * c[i]) + Math.sqrt(b[i] * c[i]);
            sum += r[i]; // TODO: Store is required for double reduction.
        }
        return sum;
    }

    @Run(test = "testReductionAddDoubleSqrt")
    @Warmup(0)
    public void runTestReductionAddDoubleSqrt() {
        double[] dArrA = new double[RANGE];
        double[] dArrB = new double[RANGE];
        double[] dArrC = new double[RANGE];
        double[] dArrR0 = new double[RANGE];
        double[] dArrR1 = new double[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(dArrA, dArrB, dArrC);
            double init = RunInfo.getRandom().nextDouble();
            double s0 = testReductionAddDoubleSqrt(dArrA, dArrB, dArrC, dArrR0, init);
            // // Comment: reduction order for doubles matters. Swapping order leads to wrong results.
            // // To verify: uncomment code below, and comment out the r verification.
            // double tmpA = dArrA[50];
            // double tmpB = dArrB[50];
            // double tmpC = dArrC[50];
            // dArrA[50] = dArrA[51];
            // dArrB[50] = dArrB[51];
            // dArrC[50] = dArrC[51];
            // dArrA[51] = tmpA;
            // dArrB[51] = tmpB;
            // dArrC[51] = tmpC;
            double s1 = referenceReductionAddDoubleSqrt(dArrA, dArrB, dArrC, dArrR1, init);
            verify("testReductionAddDoubleSqrt sum", s0, s1);
            verify("testReductionAddDoubleSqrt r", dArrR0, dArrR1);
            if (s0 == 0.0f || Double.isNaN(s0) || Double.isInfinite(s0)) {
                throw new RuntimeException("Test should not collapse. " + s0);
            }
	}
    }

    // ------------------------------------ ReductionMulDouble --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.ADD_VD, "= 0", IRNode.MUL_VD, "= 0", IRNode.STORE_VECTOR, "= 0",
                  IRNode.MUL_REDUCTION_VD, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.ADD_VD, "> 0", IRNode.MUL_VD, "> 0", IRNode.STORE_VECTOR, "> 0",
                  IRNode.MUL_REDUCTION_VD, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"sse2", "true", "sve", "true"})
    public double testReductionMulDouble(double[] a, double[] b, double[] c, double[] r, double mul) {
        for (int i = 0; i < RANGE; i++) {
            r[i] = a[i] * 0.05 + b[i] * 0.07 + c[i] * 0.08 + 0.9; // [0.9..1.1]
            mul *= r[i]; // TODO: Store is required for double reduction.
        }
        return mul;
    }

    // Not compiled.
    public double referenceReductionMulDouble(double[] a, double[] b, double[] c, double[] r, double mul) {
        for (int i = 0; i < RANGE; i++) {
            r[i] = a[i] * 0.05 + b[i] * 0.07 + c[i] * 0.08 + 0.9; // [0.9..1.1]
            mul *= r[i]; // TODO: Store is required for double reduction.
        }
        return mul;
    }

    @Run(test = "testReductionMulDouble")
    @Warmup(0)
    public void runTestReductionMulDouble() {
        double[] dArrA = new double[RANGE];
        double[] dArrB = new double[RANGE];
        double[] dArrC = new double[RANGE];
        double[] dArrR0 = new double[RANGE];
        double[] dArrR1 = new double[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(dArrA, dArrB, dArrC);
            double init = RunInfo.getRandom().nextDouble() + 1.0; // avoid zero
            double s0 = testReductionMulDouble(dArrA, dArrB, dArrC, dArrR0, init);
            // // Comment: reduction order for doubles matters. Swapping order leads to wrong results.
            // // To verify: uncomment code below.
            // double tmpA = dArrA[50];
            // double tmpB = dArrB[50];
            // double tmpC = dArrC[50];
            // dArrA[50] = dArrA[51];
            // dArrB[50] = dArrB[51];
            // dArrC[50] = dArrC[51];
            // dArrA[51] = tmpA;
            // dArrB[51] = tmpB;
            // dArrC[51] = tmpC;
            double s1 = referenceReductionMulDouble(dArrA, dArrB, dArrC, dArrR1, init);
            verify("testReductionMulDouble mul", s0, s1);
            verify("testReductionMulDouble r", dArrR0, dArrR1);
            if (s0 == 0.0f || Double.isNaN(s0) || Double.isInfinite(s0)) {
                throw new RuntimeException("Test should not collapse. " + s0);
            }
        }
    }

    // ------------------------------------ ReductionMinDouble --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.ADD_VD, "= 0", IRNode.MUL_VD, "= 0", IRNode.STORE_VECTOR, "= 0",
                  IRNode.MIN_REDUCTION_V, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.ADD_VD, "> 0", IRNode.MUL_VD, "> 0", IRNode.STORE_VECTOR, "> 0",
                  IRNode.MIN_REDUCTION_V, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"avx", "true", "sve", "true"},
        applyIfPlatformFeature = {"32-bit", "false"})
    public double testReductionMinDouble(double[] a, double[] b, double[] c, double[] r, double sum) {
        for (int i = 0; i < RANGE; i++) {
            // Note: AVX required for MinReductionV for double.
            r[i] = (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
            sum = Math.min(sum, r[i]); // TODO: Store is required for double reduction.
        }
        return sum;
    }

    // Not compiled.
    public double referenceReductionMinDouble(double[] a, double[] b, double[] c, double[] r, double sum) {
        for (int i = 0; i < RANGE; i++) {
            r[i] = (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
            sum = Math.min(sum, r[i]); // TODO: Store is required for double reduction.
        }
        return sum;
    }

    @Run(test = "testReductionMinDouble")
    @Warmup(0)
    public void runTestReductionMinDouble() {
        double[] dArrA = new double[RANGE];
        double[] dArrB = new double[RANGE];
        double[] dArrC = new double[RANGE];
        double[] dArrR0 = new double[RANGE];
        double[] dArrR1 = new double[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(dArrA, dArrB, dArrC);
            double init = RunInfo.getRandom().nextDouble();
            double s0 = testReductionMinDouble(dArrA, dArrB, dArrC, dArrR0, init);
            double s1 = referenceReductionMinDouble(dArrA, dArrB, dArrC, dArrR1, init);
            verify("testReductionMinDouble sum", s0, s1);
            verify("testReductionMinDouble r", dArrR0, dArrR1);
	}
    }

    // ------------------------------------ ReductionMaxDouble --------------------------------------------------

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.ADD_VD, "= 0", IRNode.MUL_VD, "= 0", IRNode.STORE_VECTOR, "= 0",
                  IRNode.MAX_REDUCTION_V, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.ADD_VD, "> 0", IRNode.MUL_VD, "> 0", IRNode.STORE_VECTOR, "> 0",
                  IRNode.MAX_REDUCTION_V, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"avx", "true", "sve", "true"})
    public double testReductionMaxDouble(double[] a, double[] b, double[] c, double[] r, double sum) {
        for (int i = 0; i < RANGE; i++) {
            // Note: AVX required for MaxReductionV for double.
            r[i] = (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
            sum = Math.max(sum, r[i]); // TODO: Store is required for double reduction.
        }
        return sum;
    }

    // Not compiled.
    public double referenceReductionMaxDouble(double[] a, double[] b, double[] c, double[] r, double sum) {
        for (int i = 0; i < RANGE; i++) {
            r[i] = (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
            sum = Math.max(sum, r[i]); // TODO: Store is required for double reduction.
        }
        return sum;
    }

    @Run(test = "testReductionMaxDouble")
    @Warmup(0)
    public void runTestReductionMaxDouble() {
        double[] dArrA = new double[RANGE];
        double[] dArrB = new double[RANGE];
        double[] dArrC = new double[RANGE];
        double[] dArrR0 = new double[RANGE];
        double[] dArrR1 = new double[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(dArrA, dArrB, dArrC);
            double init = RunInfo.getRandom().nextDouble();
            double s0 = testReductionMaxDouble(dArrA, dArrB, dArrC, dArrR0, init);
            double s1 = referenceReductionMaxDouble(dArrA, dArrB, dArrC, dArrR1, init);
            verify("testReductionMaxDouble sum", s0, s1);
            verify("testReductionMaxDouble r", dArrR0, dArrR1);
	    }
    }

// TODO fix it
//    // ------------------------------------ ReductionAddAbsNegDouble --------------------------------------------------
//
//    @Test
//    @IR(counts = {IRNode.LOAD_VECTOR, "= 0", IRNode.ADD_VF, "= 0", IRNode.MUL_VF, "= 0", IRNode.STORE_VECTOR, "= 0",
//                  IRNode.ADD_REDUCTION_VF, "= 0", IRNode.ABS_V, "= 0", IRNode.NEG_V, "= 0"},
//        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
//    @IR(counts = {IRNode.LOAD_VECTOR, "> 0", IRNode.ADD_VF, "> 0", IRNode.MUL_VF, "> 0", IRNode.STORE_VECTOR, "> 0",
//                  IRNode.ADD_REDUCTION_VF, "> 0", IRNode.ABS_V, "> 0", IRNode.NEG_V, "> 0"},
//        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
//        applyIfCPUFeatureOr = {"sse4.1", "true", "sve", "true"})
//    public double testReductionAddAbsNegDouble(double[] a, double[] b, double[] c, double[] r, double sum) {
//        for (int i = 0; i < RANGE; i++) {
//            r[i] = Math.abs(-a[i] * -b[i]) + Math.abs(-a[i] * -c[i]) + Math.abs(-b[i] * -c[i]);
//            sum += r[i]; // TODO: Store is required for double reduction.
//        }
//        return sum;
//    }
//
//    // Not compiled.
//    public double referenceReductionAddAbsNegDouble(double[] a, double[] b, double[] c, double[] r, double sum) {
//        for (int i = 0; i < RANGE; i++) {
//            r[i] = Math.abs(-a[i] * -b[i]) + Math.abs(-a[i] * -c[i]) + Math.abs(-b[i] * -c[i]);
//            sum += r[i]; // TODO: Store is required for double reduction.
//        }
//        return sum;
//    }
//
//    @Run(test = "testReductionAddAbsNegDouble")
//    @Warmup(0)
//    public void runTestReductionAddAbsNegDouble() {
//        for (int j = 0; j < REPETITIONS; j++) {
//            fillRandom(dArrA, dArrB, dArrC);
//            double init = RunInfo.getRandom().nextDouble();
//            double s0 = testReductionAddAbsNegDouble(dArrA, dArrB, dArrC, dArrR0, init);
//            double s1 = referenceReductionAddAbsNegDouble(dArrA, dArrB, dArrC, dArrR1, init);
//            verify("testReductionAddAbsNegDouble sum", s0, s1);
//            verify("testReductionAddAbsNegDouble r", dArrR0, dArrR1);
//        }
//    }


    // ------------------------------------ ReductionAddMulShort2Int --------------------------------------------------

    @Test
    @IR(counts = {IRNode.MUL_ADDS2I, "= 0"},
        applyIfOr = {"SuperWordReductions", "false", "LoopMaxUnroll", "<= 4"})
    @IR(counts = {IRNode.MUL_ADDS2I, "> 0"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", "> 4"},
        applyIfCPUFeatureOr = {"sse2", "true", "sve", "true"})
    public int testReductionAddMulShort2Int(short[] a, short[] b, int sum) {
        for (int i = 0; i < RANGE / 2; i++) {
            sum += ((a[2*i] * b[2*i]) + (a[2*i+1] * b[2*i+1]));
        }
        return sum;
    }

    public int referenceReductionAddMulShort2Int(short[] a, short[] b, int sum) {
        for (int i = 0; i < RANGE / 2; i++) {
            sum += ((a[2*i] * b[2*i]) + (a[2*i+1] * b[2*i+1]));
        }
        return sum;
    }

    @Run(test = "testReductionAddMulShort2Int")
    @Warmup(0)
    public void runTestReductionAddMulShort2Int() {
        short[] sArrA = new short[RANGE];
        short[] sArrB = new short[RANGE];
        for (int j = 0; j < REPETITIONS; j++) {
            fillRandom(sArrA, sArrB);
            int init = RunInfo.getRandom().nextInt();
            int s0 = testReductionAddMulShort2Int(sArrA, sArrB, init);
            int s1 = referenceReductionAddMulShort2Int(sArrA, sArrB, init);
            verify("runTestReductionAddMulShort2Int sum", s0, s1);
        }
    }

    // ------------------------------------ VERIFICATION --------------------------------------------------

    static void verify(String name, int v0, int v1) {
        if (v0 != v1) {
            throw new RuntimeException(" Invalid " + name + " result: " + v0 + " != " + v1);
        }
    }

    static void verify(String name, int[] a0, int[] a1) {
        for (int i = 0; i < RANGE; i++) {
            if (a0[i] != a1[i]) {
                throw new RuntimeException(" Invalid " + name + " result: array[" + i + "]: " + a0[i] + " != " + a1[i]);
            }
        }
    }

    static void verify(String name, long v0, long v1) {
        if (v0 != v1) {
            throw new RuntimeException(" Invalid " + name + " result: " + v0 + " != " + v1);
        }
    }

    static void verify(String name, long[] a0, long[] a1) {
        for (int i = 0; i < RANGE; i++) {
            if (a0[i] != a1[i]) {
                throw new RuntimeException(" Invalid " + name + " result: array[" + i + "]: " + a0[i] + " != " + a1[i]);
            }
        }
    }

    static void verify(String name, float v0, float v1) {
        if (v0 != v1) {
            throw new RuntimeException(" Invalid " + name + " result: " + v0 + " != " + v1);
        }
    }

    static void verify(String name, float[] a0, float[] a1) {
        for (int i = 0; i < RANGE; i++) {
            if (a0[i] != a1[i]) {
                throw new RuntimeException(" Invalid " + name + " result: array[" + i + "]: " + a0[i] + " != " + a1[i]);
            }
        }
    }

    static void verify(String name, double v0, double v1) {
        if (v0 != v1) {
            throw new RuntimeException(" Invalid " + name + " result: " + v0 + " != " + v1);
        }
    }

    static void verify(String name, double[] a0, double[] a1) {
        for (int i = 0; i < RANGE; i++) {
            if (a0[i] != a1[i]) {
                throw new RuntimeException(" Invalid " + name + " result: array[" + i + "]: " + a0[i] + " != " + a1[i]);
            }
        }
    }

    static void verify(String name, short v0, short v1) {
        if (v0 != v1) {
            throw new RuntimeException(" Invalid " + name + " result: " + v0 + " != " + v1);
        }
    }

    static void verify(String name, short[] a0, short[] a1) {
        for (int i = 0; i < RANGE; i++) {
            if (a0[i] != a1[i]) {
                throw new RuntimeException(" Invalid " + name + " result: array[" + i + "]: " + a0[i] + " != " + a1[i]);
            }
        }
    }


    // ------------------------------------ INITIALIZATION --------------------------------------------------

    static void fillRandom(int[]... arrs) {
        for (int[] a : arrs) {
            for (int i = 0; i < RANGE; i++) {
                a[i] = RunInfo.getRandom().nextInt();
            }
        }
    }

    static int fillSmallPrime() {
        int[] primes = {3, 5, 7, 11, 13, 17, 23, 29};
        return primes[RunInfo.getRandom().nextInt(8)];
    }

    static byte fillSmallPrimeByte() {
        byte[] primes = {3, 5, 7, 11, 13, 17, 23, 29};
        return primes[RunInfo.getRandom().nextInt(8)];
    }

    // Fill such that subtraction reveals small prime numbers
    static void fillSmallPrimeDiff(int[] a, int[] b) {
        for (int i = 0; i < RANGE; i++) {
            int r = RunInfo.getRandom().nextInt();
            a[i] = r;
            b[i] = r + fillSmallPrime();
        }
    }

    // Fill such that subtraction reveals base, except for a few bits flipped
    static void fillSpecialBytes(int[] a, int[] b, int base) {
        for (int i = 0; i < RANGE; i++) {
            a[i] = base;
        }
        // set at least 1 bit, but at most 31
        for (int i = 0; i < 31; i++) {
            int pos = RunInfo.getRandom().nextInt(32);
            int bit = 1 << pos;
            int j = RunInfo.getRandom().nextInt(RANGE);
            a[j] ^= bit; // set (xor / flip) the bit
	}
        for (int i = 0; i < RANGE; i++) {
            int r = RunInfo.getRandom().nextInt();
            a[i] += r;
            b[i] = r;
        }
    }

    static void fillRandom(long[]... arrs) {
        for (long[] a : arrs) {
            for (int i = 0; i < RANGE; i++) {
                a[i] = RunInfo.getRandom().nextLong();
            }
        }
    }

    static void fillSmallPrimeDiff(long[] a, long[] b) {
        for (int i = 0; i < RANGE; i++) {
            long r = RunInfo.getRandom().nextLong();
            a[i] = r;
            b[i] = r + fillSmallPrime();
        }
    }

    // Fill such that subtraction reveals base, except for a few bits flipped
    static void fillSpecialBytes(long[] a, long[] b, long base) {
        for (int i = 0; i < RANGE; i++) {
            a[i] = base;
        }
        // set at least 1 bit, but at most 63
        for (int i = 0; i < 63; i++) {
            long pos = RunInfo.getRandom().nextInt(64);
            long bit = 1L << pos;
            int j = RunInfo.getRandom().nextInt(RANGE);
            a[j] ^= bit; // set (xor / flip) the bit
	}
        for (int i = 0; i < RANGE; i++) {
            long r = RunInfo.getRandom().nextLong();
            a[i] += r;
            b[i] = r;
        }
    }

    static void fillRandom(float[]... arrs) {
        for (float[] a : arrs) {
            for (int i = 0; i < RANGE; i++) {
                a[i] = RunInfo.getRandom().nextFloat();
            }
        }
    } 

    static void fillRandom(double[]... arrs) {
        for (double[] a : arrs) {
            for (int i = 0; i < RANGE; i++) {
                a[i] = RunInfo.getRandom().nextDouble();
            }
        }
    }

    static void fillRandom(short[]... arrs) {
        for (short[] a : arrs) {
            for (int i = 0; i < RANGE; i++) {
                a[i] = (short) RunInfo.getRandom().nextInt(-32768, 32767 + 1);
            }
        }
    }

}
