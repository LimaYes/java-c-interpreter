package com.realitysink.cover.nodes.expression;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.realitysink.cover.nodes.CoverType;
import com.realitysink.cover.nodes.CoverTypedExpressionNode;
import com.realitysink.cover.runtime.CoverRuntimeException;

@NodeChildren({@NodeChild("condition"),@NodeChild("positive"),@NodeChild("negative")})
public class CoverConditionalExpressionNode extends CoverTypedExpressionNode {
    @Child
    private CoverTypedExpressionNode condition;
    @Child
    private CoverTypedExpressionNode positive;
    @Child
    private CoverTypedExpressionNode negative;
    
    private final CoverType type;
    
    public CoverConditionalExpressionNode(CoverTypedExpressionNode condition, CoverTypedExpressionNode positive,
            CoverTypedExpressionNode negative, CoverType type) {
        this.condition = condition;
        this.positive = positive;
        this.negative = negative;
        this.type = type;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        boolean result;
        try {
            result = condition.executeBoolean(frame);
        } catch (UnexpectedResultException e) {
            throw new CoverRuntimeException(this, e);
        }
        if (result) {
            return positive.executeGeneric(frame);
        } else {
            return negative.executeGeneric(frame);
        }
    }

    @Override
    public CoverType getType() {
        return type;
    }
}
