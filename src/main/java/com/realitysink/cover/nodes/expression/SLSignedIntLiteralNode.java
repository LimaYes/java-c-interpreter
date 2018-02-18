/*
 * Copyright (c) 2016 Gerard Krol
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.realitysink.cover.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.realitysink.cover.nodes.CoverType;
import com.realitysink.cover.nodes.CoverTypedExpressionNode;
import com.realitysink.cover.nodes.INT32;

// TODO FIXME: why dont we have such classes for FLOAT and double? Do we need this stuff at all here?
@NodeInfo(shortName = "const")
public final class SLSignedIntLiteralNode extends CoverTypedExpressionNode {

    private final INT32 value;

    public SLSignedIntLiteralNode(INT32 value) {
        this.value = value;
    }


    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return value.value;
    }
    @Override
    public INT32 executeINT32(VirtualFrame frame) throws UnexpectedResultException {
        return value;
    }


    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return value.value;
    }

    @Override
    public float executeFloat(VirtualFrame frame) throws UnexpectedResultException {
        return value.value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }

    @Override
    public CoverType getType() {
        return CoverType.SIGNED_INT;
    }
}
