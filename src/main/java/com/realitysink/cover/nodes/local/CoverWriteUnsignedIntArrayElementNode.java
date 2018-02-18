/*
 * Copyright (c) 2016 Gerard Krol
 * Copyright (c) 2018 Tyler Durden (GPG AAB252C6)
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
package com.realitysink.cover.nodes.local;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.realitysink.cover.nodes.CoverType;
import com.realitysink.cover.nodes.CoverTypedExpressionNode;
import com.realitysink.cover.nodes.INT32;
import com.realitysink.cover.runtime.CoverRuntimeException;

@NodeChildren({@NodeChild("array"), @NodeChild("index"), @NodeChild("value")})
@NodeInfo(shortName="=")
public abstract class CoverWriteUnsignedIntArrayElementNode extends CoverTypedExpressionNode {
    @Specialization
    protected INT32 writeUnsignedIntArrayElement(INT32[] array, long index, INT32 value) {
        try {
            array[(int) index] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            CompilerDirectives.transferToInterpreter();
            throw new CoverRuntimeException(this, "index " + index + " out of bounds");
        }
        return value;
    }

    public CoverType getType() {
        return CoverType.UNSIGNED_INT;
    }
}
