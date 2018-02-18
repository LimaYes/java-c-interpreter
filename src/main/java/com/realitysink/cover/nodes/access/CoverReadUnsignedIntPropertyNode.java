package com.realitysink.cover.nodes.access;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.NodeFields;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.realitysink.cover.nodes.CoverType;
import com.realitysink.cover.nodes.CoverTypedExpressionNode;
import com.realitysink.cover.nodes.INT32;
import com.realitysink.cover.runtime.CoverRuntimeException;

@NodeFields({
@NodeField(name="type",type=CoverType.class),
@NodeField(name="property",type=String.class)
})
@NodeChild("object")
public abstract class CoverReadUnsignedIntPropertyNode extends CoverTypedExpressionNode {
    @Specialization
    INT32 get(DynamicObject object) {
        Shape shape = object.getShape();
        Property property = shape.getProperty(getProperty());
        Location location = property.getLocation();
        Object data = location.get(object,shape);
        try {
            return (INT32) data;
        } catch (Exception e) {
            throw new CoverRuntimeException(this, e);
        }
    }
    
    protected abstract String getProperty();
}
