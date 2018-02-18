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
package com.realitysink.cover.builtins;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.realitysink.cover.nodes.CoverType;
import com.realitysink.cover.nodes.CoverTypedExpressionNode;

@NodeInfo(shortName = "exp")
@NodeChildren({@NodeChild("a")})
public abstract class CoverExp2Builtin extends CoverTypedExpressionNode {

    public static final double loge2 = Math.log(2);

    public double exp2i(double a) {
        //return Math.exp(a);
        return Math.exp(a * loge2);
    }

    @Specialization
    public double exp2(double j) {
        return exp2i(j);
    }

    @Override
    public CoverType getType() {
        return CoverType.DOUBLE;
    }    
}
