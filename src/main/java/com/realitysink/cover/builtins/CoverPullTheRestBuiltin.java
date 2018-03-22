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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.realitysink.cover.ComputationResult;
import com.realitysink.cover.CoverMain;
import com.realitysink.cover.nodes.CoverType;
import com.realitysink.cover.nodes.CoverTypedExpressionNode;
import com.realitysink.cover.nodes.SLExpressionNode;

@NodeInfo(shortName = "pull_the_rest")
@NodeChildren({@NodeChild("array_m"), @NodeChild("array_s")})
public abstract class CoverPullTheRestBuiltin extends CoverTypedExpressionNode {


    public CoverPullTheRestBuiltin() {

    }
    @Specialization
    public Object pull(long[] array_m, long[] array_s) {
        ComputationResult r = CoverMain.getComputationResult();
        if(r.personalized_ints.length != array_m.length) { System.err.println("M != m in puller"); return null; }
        if(r.storage.length != array_s.length) { System.err.println("Storage != s in puller ... CompRes has " + r.storage.length + ", array_s has " + array_s.length); return null; }
        for(int i=0;i<array_m.length;++i) array_m[i] = r.personalized_ints[i];
        for(int i=0;i<array_s.length;++i) array_s[i] = r.storage[i];
        return null;
    }

    @Override
    public CoverType getType() {
        return CoverType.VOID;
    }    
}
