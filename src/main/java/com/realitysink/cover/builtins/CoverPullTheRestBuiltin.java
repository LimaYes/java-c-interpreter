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
package com.realitysink.cover.builtins;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.realitysink.cover.ComputationResult;
import com.realitysink.cover.nodes.CoverType;
import com.realitysink.cover.nodes.CoverTypedExpressionNode;
import com.realitysink.cover.nodes.SLExpressionNode;

@NodeInfo(shortName = "pull_the_rest")
@NodeField(name = "compres", type = ComputationResult.class)

public abstract class CoverPullTheRestBuiltin extends CoverTypedExpressionNode {
    private final FrameSlot frameSlot;

    public CoverPullTheRestBuiltin(FrameSlot frameSlot) {
        this.frameSlot = frameSlot;
    }
    @Specialization
    public int pull() {
    return 0;
    }

    @Override
    public CoverType getType() {
        return CoverType.UNSIGNED_INT;
    }    
}
