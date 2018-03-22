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
import com.realitysink.cover.CoverMain;
import com.realitysink.cover.nodes.CoverReference;
import com.realitysink.cover.nodes.CoverScope;
import com.realitysink.cover.nodes.CoverType;
import com.realitysink.cover.nodes.CoverTypedExpressionNode;
import com.realitysink.cover.parser.CoverParseException;
@NodeFields({
        @NodeField(name = "scope", type = CoverScope.class)
})
@NodeInfo(shortName = "sync_r")
public abstract class CoverSyncRBuiltin extends CoverTypedExpressionNode {

    protected abstract CoverScope getScope();

    public CoverSyncRBuiltin() {

    }
    @Specialization
    public Object sync() {
        ComputationResult r = CoverMain.getComputationResult();
        if(r==null) return null;

        CoverReference ref_r = getScope().findReference("r");
        if (ref_r == null) return null;
        FrameSlot frameSlot_r = ref_r.getFrameSlot();
        long[] obj = (long[]) getScope().getHeapObject(frameSlot_r);
        r.isPow = obj[0]!=0;
        r.isBounty = obj[1]!=0;
        return null;
    }

    @Override
    public CoverType getType() {
        return CoverType.VOID;
    }    
}
