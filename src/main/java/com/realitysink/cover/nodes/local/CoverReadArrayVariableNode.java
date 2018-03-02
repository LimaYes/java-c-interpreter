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

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.NodeFields;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.realitysink.cover.SingletonGlobalMaterializedFrame;
import com.realitysink.cover.nodes.CoverScope;
import com.realitysink.cover.nodes.CoverType;
import com.realitysink.cover.nodes.CoverTypedExpressionNode;

@NodeFields({
        @NodeField(name = "slot", type = FrameSlot.class),
        @NodeField(name = "scope", type = CoverScope.class)
})
public abstract class CoverReadArrayVariableNode extends CoverTypedExpressionNode {
    protected abstract FrameSlot getSlot();
    protected abstract CoverScope getScope();

    @Specialization
    protected Object readObject(VirtualFrame frame) {
        System.out.println("Reading Variable Node: " + frame.toString() + " ... descriptor = " + frame.getFrameDescriptor());

        Object firstTry = getScope().getHeapObject(getSlot());

        return firstTry;
    }

    
    public CoverType getType() {
        return CoverType.ARRAY;
    }
}
