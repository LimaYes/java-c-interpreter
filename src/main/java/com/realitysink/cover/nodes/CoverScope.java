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
package com.realitysink.cover.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.realitysink.cover.SingletonGlobalMaterializedFrame;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.realitysink.cover.nodes.CoverType.BasicType;
import com.realitysink.cover.parser.CoverParseException;
import com.realitysink.cover.runtime.SLFunction;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;

public class CoverScope {
    private FrameDescriptor frameDescriptor = new FrameDescriptor();
    private Map<String,CoverReference> definitions = new HashMap<>();
    private Map<String,CoverType> types = new HashMap<>();
    private CoverScope parent;
    private Map<FrameSlot,Object> arrays_heap = new HashMap<>();


    public CoverScope(CoverScope parent) {
        this.parent = parent;
        if (parent != null) {
            this.frameDescriptor = parent.frameDescriptor;
        }
    }

    public Object getHeapObject(FrameSlot sl){
        Object ret = arrays_heap.get(sl);
        if(ret==null && parent != null)
            return parent.getHeapObject(sl);
        else
            return ret;
    }

    public Object setHeapObject(FrameSlot sl, Object obj){
        return arrays_heap.put(sl, obj);
    }

    public FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }
    
    public CoverReference findReference(String identifier) {
        CoverReference definition = definitions.get(identifier);
        if (definition != null) {
            return definition;
        } else if (parent != null) {
            return parent.findReference(identifier);
        }
        return null;
    }
    
    public CoverReference define(IASTNode node, String identifier, CoverType type) {
        if (definitions.containsKey(identifier)) {
            throw new CoverParseException(node, "identifier " + identifier + " already exists in this scope");
        }
        CoverReference ref = new CoverReference(type); 
        if (type.getBasicType() != BasicType.FUNCTION) {
            // function references don't use frameslots, they use the SLFunction object itself
            FrameSlot slot = frameDescriptor.addFrameSlot(new Object());
            FrameSlotKind frameSlotKind = type.getFrameSlotKind(node);
            slot.setKind(frameSlotKind);
            //System.err.println("added " + slot);
            ref.setFrameSlot(slot);


        }
        definitions.put(identifier, ref);
        //System.err.println("defined " + identifier + " as " + type.getBasicType());
        return ref;
    }
    
    public void addType(String name, CoverType type) {
        types.put(name, type);
    }
    
    public CoverType findType(String name) {
        CoverType type = types.get(name);
        if (type != null) {
            return type;
        } else if (parent != null) {
            return parent.findType(name);
        } else {
            return null;
        }
    }

    public CoverScope getParent() {
        return parent;
    }

}
