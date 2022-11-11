/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8138583
 * @summary Add C2 AArch64 Superword support for scalar sum reduction optimizations : double abs & neg test
 * @requires os.arch=="aarch64" | os.arch=="riscv64"
 * @library /test/lib /
 * @run driver compiler.loopopts.superword.RedTest_int
*/

package compiler.loopopts.superword;

import compiler.lib.ir_framework.*;

public class RedTest_AbsNeg_double { 

    static final int NUM = 256 * 1024;
    static final int ITER = 8000;
    public static void main(String[] args) throws Exception {
        TestFramework framework = new TestFramework();
        framework.addFlags("-XX:+IgnoreUnrecognizedVMOptions",
                           "-XX:LoopUnrollLimit=250",
                           "-XX:CompileThresholdScaling=0.1",
                           "-XX:-TieredCompilation",
                           "-XX:+RecomputeReductions");
        int i = 0;
        Scenario[] scenarios = new Scenario[8];
        for (String reductionSign : new String[] {"+", "-"}) {
            for (int maxUnroll : new int[] {2, 4, 8, 16}) {
                // REMOVE
                scenarios[i] = new Scenario(i, "-XX:" + reductionSign + "SuperWordReductions",
				               "-XX:LoopUnrollLimit=" + 250,
                                               "-XX:LoopMaxUnroll=" + maxUnroll);
                i++;
            }
        }
        framework.addScenarios(scenarios);
        framework.start();
    }

    @Run(test = {"sumReductionImplement"},
        mode = RunMode.STANDALONE)
    public void runTests() throws Exception {
        double[] a = new double[NUM];
        double[] b = new double[NUM];
        double[] c = new double[NUM];
        double[] d = new double[NUM];
        sumReductionInit(a, b, c);
        double total = 0;
        double valid = 0;
        for (int j = 0; j < ITER; j++) {
            total = sumReductionImplement(a, b, c, d, total);
        }
        for (int j = 0; j < d.length; j++) {
            valid += d[j];
        }
        testCorrectness(total, valid, "Add Reduction");
    }
	
    public static void sumReductionInit(
            double[] a,
            double[] b,
            double[] c) {
        for (int j = 0; j < 1; j++) {
            for (int i = 0; i < a.length; i++) {
                a[i] = i * 1 + j;
                b[i] = i * 1 - j;
                c[i] = i + j;
            }
        }
    }

    /*@Test
    @IR(applyIfCPUFeature = {"ssse3", "true"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", ">= 8"},
        counts = {IRNode.ADD_REDUCTION_VD, ">= 1"})*/
    // Test not applicable for x64
    @IR(applyIfCPUFeature = {"sve", "true"},
        applyIfAnd = {"SuperWordReductions", "true", "LoopMaxUnroll", ">= 8"},
        counts = {IRNode.ADD_REDUCTION_VD, ">= 1"})
    public static double sumReductionImplement(
            double[] a,
            double[] b,
            double[] c,
            double[] d,
            double total) {
        for (int i = 0; i < a.length; i++) {
            d[i] = Math.abs(-a[i] * -b[i]) + Math.abs(-a[i] * -c[i]) + Math.abs(-b[i] * -c[i]);
            total += d[i];
        }
        return total;
    }

    public static void testCorrectness(
            double total,
            double valid,
            String op) throws Exception {
        if (total == valid) {
            System.out.println(op + ": Success");
        } else {
            System.out.println("Invalid total: " + total);
            System.out.println("Expected value = " + valid);
            throw new Exception(op + ": Failed");
        }
    }

}
