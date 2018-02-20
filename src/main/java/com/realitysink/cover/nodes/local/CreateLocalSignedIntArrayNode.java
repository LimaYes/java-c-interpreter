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
import com.realitysink.cover.nodes.SLExpressionNode;
import com.realitysink.cover.nodes.SLStatementNode;
import com.realitysink.cover.runtime.CoverRuntimeException;

import java.math.BigInteger;

public class CreateLocalSignedIntArrayNode extends SLStatementNode {
    private final FrameSlot frameSlot;
    @Child
    private SLExpressionNode size;

    public CreateLocalSignedIntArrayNode(FrameSlot frameSlot, SLExpressionNode size) {
        this.frameSlot = frameSlot;
        this.size = size;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        int s;

        // A rather risky thing but we have no automatic implicit casting of array argument
        try {
            s = (int) size.executeLong(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new CoverRuntimeException(this, e);
        }
        frame.setObject(frameSlot, new long[s]);
    }
}
