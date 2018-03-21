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
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.realitysink.cover.SingletonGlobalMaterializedFrame;
import com.realitysink.cover.nodes.CoverScope;
import com.realitysink.cover.nodes.SLExpressionNode;
import com.realitysink.cover.nodes.SLStatementNode;
import com.realitysink.cover.nodes.expression.SLUnsignedLongLiteralNode;
import com.realitysink.cover.runtime.CoverRuntimeException;

public class CreateLocalUnsignedIntArrayNode extends SLStatementNode {
    private final FrameSlot frameSlot;
    @Child
    private SLExpressionNode size;
    private CoverScope scope;

    public CreateLocalUnsignedIntArrayNode(FrameSlot frameSlot, CoverScope scope, SLExpressionNode size) {
        this.frameSlot = frameSlot;
        this.size = size;
        this.scope = scope;

        SLUnsignedLongLiteralNode sizeNode = (SLUnsignedLongLiteralNode) size;
        scope.setHeapObject(frameSlot, new long[(int)sizeNode.getValue()]);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        // pass
        // TODO FIXME: This is not the correct way to introduce arrays
    }
}
