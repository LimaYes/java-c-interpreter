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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTNode;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.object.Shape;
import com.realitysink.cover.parser.CoverParseException;

/**
 * The type of "something" in the Cover language. This can be a long variable, a function
 * or a Java object. Whatever you want, really.
 */
public class CoverType {
    public enum BasicType {
        UNSIGNED_INT,
        SIGNED_INT,
        UNSIGNED_LONG,
        SIGNED_LONG,
        DOUBLE,
        FLOAT,
        STRING,
        BOOLEAN,
        ARRAY,
        ARRAY_ELEMENT,
        FUNCTION,
        OBJECT,
        JAVA_OBJECT,
        VOID
    }
    
    public static final CoverType VOID = new CoverType(BasicType.VOID);
    public static final CoverType SIGNED_LONG = new CoverType(BasicType.SIGNED_LONG);
    public static final CoverType UNSIGNED_LONG = new CoverType(BasicType.UNSIGNED_LONG);
    public static final CoverType SIGNED_INT = new CoverType(BasicType.SIGNED_INT);
    public static final CoverType UNSIGNED_INT = new CoverType(BasicType.UNSIGNED_INT);
    public static final CoverType DOUBLE = new CoverType(BasicType.DOUBLE);
    public static final CoverType FLOAT = new CoverType(BasicType.FLOAT);
    public static final CoverType BOOLEAN = new CoverType(BasicType.BOOLEAN);
    public static final CoverType FUNCTION = new CoverType(BasicType.FUNCTION);
    public static final CoverType STRING = new CoverType(BasicType.STRING);
    public static final CoverType ARRAY = new CoverType(BasicType.ARRAY);
    public static final CoverType OBJECT = new CoverType(BasicType.OBJECT);
    
    private BasicType basicType;
    
    private CoverType[] functionArguments;
    private CoverType functionReturn;
    
    /*
     * For variables.
     */
    private Map<String, CoverType> objectMembers = new HashMap<String, CoverType>();
    private Shape shape;
    
    private CoverType arrayType;
    
    public CoverType(BasicType basicType) {
        this.basicType = basicType;
    }
    
    public CoverType[] getFunctionArguments() {
        return functionArguments;
    }

    public CoverType setFunctionArguments(CoverType[] functionArguments) {
        this.functionArguments = functionArguments;
        return this;
    }

    public CoverType getFunctionReturn() {
        return functionReturn;
    }

    public CoverType setFunctionReturn(CoverType functionReturn) {
        this.functionReturn = functionReturn;
        return this;
    }

    public Map<String, CoverType> getObjectMembers() {
        return objectMembers;
    }

    public CoverType setObjectMembers(Map<String, CoverType> objectMembers) {
        this.objectMembers = objectMembers;
        return this;
    }

    public FrameSlotKind getFrameSlotKind(IASTNode node) {
        CompilerAsserts.neverPartOfCompilation();
        switch (basicType) {
        case SIGNED_LONG: return FrameSlotKind.Long;
        case UNSIGNED_LONG: return FrameSlotKind.Long;
        case SIGNED_INT: return FrameSlotKind.Int;
        case UNSIGNED_INT: return FrameSlotKind.Int;
        case DOUBLE: return FrameSlotKind.Double;
        case FLOAT: return FrameSlotKind.Float;
        case OBJECT: return FrameSlotKind.Object;
        case STRING: return FrameSlotKind.Object;
        case ARRAY: return FrameSlotKind.Object;
        case JAVA_OBJECT: return FrameSlotKind.Object;
        case ARRAY_ELEMENT: return arrayType.getFrameSlotKind(node);
        default:   throw new CoverParseException(node, "unsupported reference for frameslotkind: " + basicType.toString());
        }
    }

    public BasicType getBasicType() {
        return basicType;
    }

    public CoverType getTypeOfArrayContents() {
        return arrayType;
    }

    public CoverType setArrayType(CoverType typeOfContents) {
        this.arrayType = typeOfContents;
        return this;
    }

    public boolean isPrimitiveType(IASTNode node) {
        CompilerAsserts.neverPartOfCompilation();
        switch (basicType) {
        case SIGNED_LONG: return true;
        case UNSIGNED_LONG: return true;
        case SIGNED_INT: return false;
        case UNSIGNED_INT: return false;
        case DOUBLE: return true;
        case FLOAT: return true;
        case BOOLEAN: return true;
        case OBJECT: return false;
        case STRING: return false;
        case JAVA_OBJECT: return false;
        case ARRAY: return false;
        case ARRAY_ELEMENT: return arrayType.isPrimitiveType(node);
        default:   throw new CoverParseException(node, "unsupported reference for isUnboxed: " + basicType.toString());
        }
    }

    public boolean canAccept(CoverType type) {
        CompilerAsserts.neverPartOfCompilation();
        if (this.equals(type)) {
            return true;
        }
        if (isPrimitiveType(null) && getBasicType() == type.getBasicType()) {
            return true; // FIXME, array types!
        }

        // Just allow free conversion between these types
        BasicType[] primoTypes = new BasicType[]{BasicType.SIGNED_INT, BasicType.UNSIGNED_INT, BasicType.SIGNED_LONG, BasicType.UNSIGNED_LONG, BasicType.FLOAT, BasicType.DOUBLE  };
        for(int i=0; i<primoTypes.length; ++i)
            for(int j=0; j<primoTypes.length; ++j) {
                if(i==j) continue;
                if (getBasicType() == primoTypes[i] && type.getBasicType() == primoTypes[j])
                    return true;
            }

        if (basicType == BasicType.ARRAY_ELEMENT && getTypeOfArrayContents().canAccept(type)) {
            return true;
        }
        return false;
    }

    public CoverType combine(IASTNode node, CoverType other) {
        CompilerAsserts.neverPartOfCompilation();
        if (this.equals(other)) {
            return this;
        }

        // Convert based on "precedence" which is equivalent with a higher position in array
        BasicType[] primoTypes = new BasicType[]{BasicType.SIGNED_INT, BasicType.UNSIGNED_INT, BasicType.SIGNED_LONG, BasicType.UNSIGNED_LONG, BasicType.FLOAT, BasicType.DOUBLE  };
        for(int i=0; i<primoTypes.length; ++i)
            for(int j=0; j<primoTypes.length; ++j) {
                if(i==j) continue;
                if (getBasicType() == primoTypes[i] && other.getBasicType() == primoTypes[j])
                    return (i>j)?this:other;
            }

            // TODO FIXME: add conversion exception when higher rank is signed https://www.safaribooksonline.com/library/view/c-in-a/0596006977/ch04.html



        throw new CoverParseException(node, "incompatible types: " + basicType + " and " + other.basicType);
    }

    @Override
    public int hashCode() {
        CompilerAsserts.neverPartOfCompilation();
        final int prime = 31;
        int result = 1;
        result = prime * result + ((arrayType == null) ? 0 : arrayType.hashCode());
        result = prime * result + ((basicType == null) ? 0 : basicType.hashCode());
        result = prime * result + Arrays.hashCode(functionArguments);
        result = prime * result + ((functionReturn == null) ? 0 : functionReturn.hashCode());
        result = prime * result + ((objectMembers == null) ? 0 : objectMembers.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        CompilerAsserts.neverPartOfCompilation();
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CoverType other = (CoverType) obj;
        if (arrayType == null) {
            if (other.arrayType != null)
                return false;
        } else if (!arrayType.equals(other.arrayType))
            return false;
        if (basicType != other.basicType)
            return false;
        if (!Arrays.equals(functionArguments, other.functionArguments))
            return false;
        if (functionReturn == null) {
            if (other.functionReturn != null)
                return false;
        } else if (!functionReturn.equals(other.functionReturn))
            return false;
        if (objectMembers == null) {
            if (other.objectMembers != null)
                return false;
        } else if (!objectMembers.equals(other.objectMembers))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return "CoverType [basicType=" + basicType + ", functionArguments=" + Arrays.toString(functionArguments)
                + ", functionReturn=" + functionReturn + ", objectMembers=" + objectMembers + ", arrayType="
                + arrayType + "]";
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }

}
