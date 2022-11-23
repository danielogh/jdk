/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8240248
 * @summary Add C2 x86 Superword support for scalar logical reduction optimizations : float test
 * @library /test/lib /
 * @run driver compiler.loopopts.superword.RedTest_float
*/

package compiler.loopopts.superword;

import compiler.lib.ir_framework.*;

public class RedTest_float {
    static final int NUM = 512;
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
                scenarios[i] = new Scenario(i, "-XX:" + reductionSign + "SuperWordReductions",
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
	float[] a = new float[NUM];
        float[] b = new float[NUM];
        float[] c = new float[NUM];
        float[] d = new float[NUM];
        sumReductionInit(a, b, c);
        float total = 0;
        float valid = 0;
        for (int j = 0; j < ITER; j++) {
            total = sumReductionImplement(a, b, c, d);
        }
        for (int j = 0; j < d.length; j++) {
            valid += d[j];
        }
        testCorrectness(total, valid, "Add Reduction");
    }

    public static void sumReductionInit(
            float[] a,
            float[] b,
            float[] c) {
        for (int j = 0; j < 1; j++) {
            for (int i = 0; i < a.length; i++) {
                a[i] = i * 1 + j;
                b[i] = i * 1 - j;
                c[i] = i + j;
            }
        }
    }
 
    @Test
    @IR(applyIf = {"SuperWordReductions", "false"},
        failOn = {IRNode.ADD_REDUCTION_VF})
    @IR(applyIfCPUFeature = {"sse", "true"},
        applyIfAnd = {"SuperWordReductions", "true", "UseSSE", ">= 1", "LoopMaxUnroll", ">= 8"},
        counts = {IRNode.ADD_REDUCTION_VF, ">= 1"})
    @IR(applyIfCPUFeature = {"sve", "true"},
        applyIfAnd = {"SuperWordReductions", "true", "UseSVE", ">= 1", "LoopMaxUnroll", ">= 8"},
        counts = {IRNode.ADD_REDUCTION_VF, ">= 1"})
    public static float sumReductionImplement(
            float[] a,
            float[] b,
            float[] c,
            float[] d) {
        float total = 0;
        for (int i = 0; i < a.length; i++) {
            d[i] = (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
            total += d[i];
        }
        return total;
    }

    public static void testCorrectness(
            float total,
            float valid,
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

