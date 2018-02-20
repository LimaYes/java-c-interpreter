/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.realitysink.cover.nodes.expression;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.realitysink.cover.nodes.CoverType;
import com.realitysink.cover.nodes.CoverTypedExpressionNode;
import com.realitysink.cover.nodes.INT32;
import com.realitysink.cover.runtime.SLFunction;
import com.realitysink.cover.runtime.SLNull;

import java.math.BigInteger;

/**
 * The {@code ==} operator of SL is defined on all types. Therefore, we need a
 * {@link #equal(Object, Object) implementation} that can handle all possible types. But since
 * {@code ==} can only return {@code true} when the type of the left and right operand are the same,
 * the specializations already cover all possible cases that can return {@code true} and the generic
 * case is trivial.
 * <p>
 * Note that we do not need the analogous {@code !=} operator, because we can just
 * {@link SLLogicalNotNode negate} the {@code ==} operator.
 */
@NodeInfo(shortName = "!=")
@NodeChildren({@NodeChild("leftNode"), @NodeChild("rightNode")})
public abstract class SLNotEqualNode extends CoverTypedExpressionNode {


    @Specialization
    protected boolean nequal(long left, long right) {
        return left != right;
    }

    @Specialization
    @TruffleBoundary
    protected boolean nequal(BigInteger left, BigInteger right) {
        return !left.equals(right);
    }

    @Specialization
    protected boolean nequal(boolean left, boolean right) {
        return left != right;
    }

    @Specialization
    protected boolean nequal(String left, String right) {
        return !left.equals(right);
    }

    @Specialization
    protected boolean equal(SLFunction left, SLFunction right) {
        /*
         * Our function registry maintains one canonical SLFunction object per function name, so we
         * do not need equals().
         */
        return left != right;
    }

    @Specialization
    protected boolean nequal(SLNull left, SLNull right) {
        /* There is only the singleton instance of SLNull, so we do not need equals(). */
        return left != right;
    }

    /**
     * We covered all the cases that can return true in the type specializations above. If we
     * compare two values with different types, the result is known to be false.
     * <p>
     * Note that the guard is essential for correctness: without the guard, the specialization would
     * also match when the left and right value have the same type. The following scenario would
     * return a wrong value: First, the node is executed with the left value 42 (type long) and the
     * right value "abc" (String). This specialization matches, and since it is the first execution
     * it is also the only specialization. Then, the node is executed with the left value "42" (type
     * long) and the right value "42" (type long). Since this specialization is already present, and
     * (without the guard) also matches (long values can be boxed to Object), it is executed. The
     * wrong return value is "false".
     */
    @Specialization(guards = "left.getClass() != right.getClass()")
    protected boolean nequal(Object left, Object right) {
        assert !left.equals(right);
        return true;
    }
    
    public CoverType getType() {
        return CoverType.BOOLEAN;
    }
}
