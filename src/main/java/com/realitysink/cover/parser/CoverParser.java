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
package com.realitysink.cover.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.realitysink.cover.CoverLanguage;
import com.realitysink.cover.builtins.*;
import com.realitysink.cover.nodes.*;
import com.realitysink.cover.nodes.CoverType.BasicType;
import com.realitysink.cover.nodes.access.*;
import com.realitysink.cover.nodes.call.SLInvokeNode;
import com.realitysink.cover.nodes.controlflow.SLBlockNode;
import com.realitysink.cover.nodes.controlflow.SLBreakNode;
import com.realitysink.cover.nodes.controlflow.SLFunctionBodyNode;
import com.realitysink.cover.nodes.controlflow.SLIfNode;
import com.realitysink.cover.nodes.controlflow.SLReturnNode;
import com.realitysink.cover.nodes.controlflow.SLWhileNode;
import com.realitysink.cover.nodes.expression.*;
import com.realitysink.cover.nodes.local.*;
import com.realitysink.cover.runtime.SLFunction;
import com.realitysink.cover.runtime.SLObjectType;

import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTArrayDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTArraySubscriptExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTBinaryExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTBreakStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCastExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTConditionalExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarationStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDoStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTEqualsInitializer;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTExpressionStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFieldReference;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTForStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTIdExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTIfStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTLiteralExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamedTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNullStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTReturnStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTUnaryExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTVisibilityLabel;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTWhileStatement;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.SavedFilesProvider;
import org.eclipse.cdt.internal.core.parser.scanner.CharArray;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.core.runtime.CoreException;

public class CoverParser {
    private Source source;
    final CoverScope fileScope;
    
    public CoverParser(Source source, CoverScope scope) {
        this.source = source;
        this.fileScope = scope;

        scope.addType("long", CoverType.SIGNED_LONG);
        scope.addType("ulong", CoverType.UNSIGNED_LONG);
        scope.addType("uint", CoverType.UNSIGNED_INT);
    }

    public void parse() throws CoreException {


        parseRaw();

    }
    
    private void parseRaw() throws CoreException {
        //System.err.println("Parsing " + source.getPath());

        FileContent fileContent = FileContent.createForExternalFileLocation(source.getPath());

        Map<String, String> definedSymbols = new HashMap<String, String>();
        String[] includePaths = new String[] {System.getProperty("user.dir") + "/runtime/include"};
        IScannerInfo info = new ScannerInfo(definedSymbols, includePaths);
        IParserLogService log = new DefaultLogService();

        IncludeFileContentProvider fileContentProvider =  new SavedFilesProvider() {
            @Override
            public InternalFileContent getContentForInclusion(String path, IMacroDictionary macroDictionary) {
                // we somehow don't get the contents of included headers parsed, so act on them here
                Source includedSource;
                try {
                    includedSource = Source.fromFileName(path);
                } catch (IOException e) {
                    throw new CoverParseException(null, "could not read included file " + path, e);
                }
                return new InternalFileContent(path, new CharArray(includedSource.getCode()));
            }
        };

        int opts = ILanguage.OPTION_IS_SOURCE_UNIT;
        IASTTranslationUnit translationUnit = GPPLanguage.getDefault().getASTTranslationUnit(fileContent, info,
                fileContentProvider, null, opts, log);

        IASTPreprocessorIncludeStatement[] includes = translationUnit.getIncludeDirectives();
        for (IASTPreprocessorIncludeStatement include : includes) {
            //System.err.println("include - " + include.getName());
        }

        // RootNode
        for (IASTNode node : translationUnit.getChildren()) {
            processStatement(fileScope, node);
        }


    }

    private void printTruffleNodes(Node n2, int level) {
        String spaces = "";
        for (int i = 0; i < level; i++)
            spaces += "  ";
        if (n2 instanceof CoverTypedExpressionNode) {
            //System.err.println(spaces + n2.getClass().getName() + " " + ((CoverTypedExpressionNode)n2).getType());
        } else {
            //System.err.println(spaces + n2.getClass().getName());
        }
        for (Node n : n2.getChildren()) {
            printTruffleNodes(n, level + 1);
        }
    }

    private SLStatementNode processStatement(CoverScope scope, IASTNode node) {
        //info(node, "processStatement for " + node.getClass().getSimpleName());
        SLStatementNode result;
        if (node instanceof CPPASTFunctionDefinition) {
            result = processFunctionDefinition(scope, (CPPASTFunctionDefinition) node);
        } else if (node instanceof CPPASTExpressionStatement) {
            CPPASTExpressionStatement x = (CPPASTExpressionStatement) node;
            result =  processExpression(scope, x.getExpression(), null);
        } else if (node instanceof CPPASTDeclarationStatement) {
            result =  processDeclarationStatement(scope, (CPPASTDeclarationStatement) node);
        } else if (node instanceof CPPASTWhileStatement) {
            result =  processWhile(scope, (CPPASTWhileStatement) node);
            result.addStatementTag();
        } else if (node instanceof CPPASTDoStatement) {
            result =  processDo(scope, (CPPASTDoStatement) node);
            result.addStatementTag();
        } else if (node instanceof CPPASTCompoundStatement) {
            result =  processCompoundStatement(scope, (CPPASTCompoundStatement) node);
        } else if (node instanceof CPPASTReturnStatement) {
            result =  processReturn(scope, (CPPASTReturnStatement) node);
        } else if (node instanceof CPPASTBinaryExpression) {
            result =  processBinaryExpression(scope, (CPPASTBinaryExpression) node);
        } else if (node instanceof CPPASTForStatement) {
            result =  processForStatement(scope, (CPPASTForStatement) node);
            result.addStatementTag();
        } else if (node instanceof CPPASTIfStatement) {
            result =  processIfStatement(scope, (CPPASTIfStatement) node);
        } else if (node instanceof CPPASTSimpleDeclaration) {
            result =  processDeclaration(scope, (CPPASTSimpleDeclaration) node);
        } else if (node instanceof CPPASTBreakStatement) {
            result =  new SLBreakNode();
        } else if (node instanceof CPPASTNullStatement) {
            result =  new CoverNopExpression();
        } else {
            printTree(node, 1);
            throw new CoverParseException(node, "unknown statement type: " + node.getClass().getSimpleName());
        }
        if (result.getSourceSection() == null) {
            result.setSourceSection(createSourceSectionForNode("statement", node));
        }
        result.addStatementTag();
        return result;
    }

    private SLStatementNode processTypedef(CoverScope scope, CPPASTSimpleDeclaration node) {
        IASTDeclSpecifier declSpecifier = node.getDeclSpecifier();
        CoverType oldType = processDeclSpecifier(scope, declSpecifier);
        IASTDeclarator[] declarators = node.getDeclarators();
        for (IASTDeclarator declarator : declarators) {
            String newType = declarator.getName().toString();
            scope.addType(newType, oldType);
        }
        return new CoverNopExpression();
    }

    private SLStatementNode processIfStatement(CoverScope scope, CPPASTIfStatement node) {
        CoverScope ifScope = new CoverScope(scope);
        CoverScope thenScope = new CoverScope(ifScope);
        CoverScope elseScope = new CoverScope(ifScope);
        CoverTypedExpressionNode conditionNode = SLForceBooleanNodeGen.create(SLForceBooleanNodeGen.create(processExpression(ifScope, node.getConditionExpression(), null)));
        SLStatementNode thenPartNode = processStatement(thenScope, node.getThenClause());
        SLStatementNode elsePartNode = null;
        if (node.getElseClause() != null) {
            elsePartNode = processStatement(elseScope, node.getElseClause());
        }
        return new SLIfNode(conditionNode, thenPartNode, elsePartNode);
    }

    private SLStatementNode processDo(CoverScope scope, CPPASTDoStatement node) {
        CoverScope scope1 = new CoverScope(scope);
        CoverScope scope2 = new CoverScope(scope);
        CoverScope conditionScope = new CoverScope(scope);
        // a do {} while() loop is just a while loop with the body prepended
        CoverTypedExpressionNode conditionNode = SLForceBooleanNodeGen.create(processExpression(conditionScope, node.getCondition(), null));
        SLStatementNode bodyNode1 = processStatement(scope1, node.getBody());
        SLStatementNode bodyNode2 = processStatement(scope2, node.getBody());
        final SLWhileNode whileNode = new SLWhileNode(conditionNode, bodyNode2);
        SLBlockNode blockNode = new SLBlockNode(new SLStatementNode[]{bodyNode1, whileNode});
        return blockNode;
    }

    private SLStatementNode processForStatement(CoverScope scope, CPPASTForStatement node) {
        /*
           -CPPASTForStatement (offset: 15,50) -> for (
             -CPPASTDeclarationStatement (offset: 20,8) -> int i=0;
               -CPPASTSimpleDeclaration (offset: 20,8) -> int i=0;
                 -CPPASTSimpleDeclSpecifier (offset: 20,3) -> int
                 -CPPASTDeclarator (offset: 24,3) -> i=0
                   -CPPASTName (offset: 24,1) -> i
                   -CPPASTEqualsInitializer (offset: 25,2) -> =0
                     -CPPASTLiteralExpression (offset: 26,1) -> 0
             -CPPASTBinaryExpression (offset: 28,4) -> i<12
               -CPPASTIdExpression (offset: 28,1) -> i
                 -CPPASTName (offset: 28,1) -> i
               -CPPASTLiteralExpression (offset: 30,2) -> 12
             -CPPASTUnaryExpression (offset: 33,3) -> i++
               -CPPASTIdExpression (offset: 33,1) -> i
                 -CPPASTName (offset: 33,1) -> i
             -CPPASTCompoundStatement (offset: 38,27) -> {          printf("i=%d\n", i);    }
         */
        CoverScope initializerScope = new CoverScope(scope);
        CoverScope conditionScope = new CoverScope(initializerScope);
        CoverScope iterationScope = new CoverScope(conditionScope);
        CoverScope bodyScope = new CoverScope(iterationScope);        
        IASTStatement initializer = node.getInitializerStatement();
        IASTExpression condition = node.getConditionExpression();
        IASTExpression iteration = node.getIterationExpression();
        SLStatementNode initializerNode = processStatement(initializerScope, initializer);
        CoverTypedExpressionNode conditionNode = processExpression(conditionScope, condition, null);
        CoverTypedExpressionNode iterationNode = processExpression(iterationScope, iteration, null);
        SLStatementNode bodyNode = processStatement(bodyScope, node.getBody());
        /*
         * We turn this:
         *   for (int i=0;i<x;i++) {}
         * into:
         *   {
         *     int i = 0;
         *     while (i < x) {
         *       {...}
         *       i++;
         *     }
         *   }
         */
        SLStatementNode[] loopNodes = new SLStatementNode[] {bodyNode, iterationNode};
        SLBlockNode loopBlock = new SLBlockNode(loopNodes);
        
        final SLWhileNode whileNode = new SLWhileNode(conditionNode, loopBlock);

        SLStatementNode[] setupNodes = new SLStatementNode[] {initializerNode, whileNode}; 
        SLBlockNode setupBlock = new SLBlockNode(setupNodes);
        
        return setupBlock;        
    }

    private SLStatementNode processReturn(CoverScope scope, CPPASTReturnStatement node) {
        IASTExpression returnValue = node.getReturnValue();
        final SLReturnNode returnNode = new SLReturnNode(processExpression(scope, returnValue, null));
        return returnNode;
    }

    private SLStatementNode processWhile(CoverScope scope, CPPASTWhileStatement node) {
        /*
       -CPPASTWhileStatement (offset: 27,47) -> while
         -CPPASTBinaryExpression (offset: 34,6) -> i < 10
           -CPPASTIdExpression (offset: 34,1) -> i
             -CPPASTName (offset: 34,1) -> i
           -CPPASTLiteralExpression (offset: 38,2) -> 10
         -CPPASTCompoundStatement (offset: 42,32) -> {
         */
        //CoverTypedExpressionNode conditionNode = SLForceBooleanNodeGen.create(processExpression(scope, node.getCondition(), null));
        CoverTypedExpressionNode conditionNode = processExpression(scope, node.getCondition(), null);
        SLStatementNode bodyNode = processStatement(scope, node.getBody());
        final SLWhileNode whileNode = new SLWhileNode(conditionNode, bodyNode);
        return whileNode;
    }

    private CoverTypedExpressionNode processExpression(CoverScope scope, IASTExpression expression, CoverType type) {
        if (expression == null) {
            // FIXME: do we want to silently do this?
            return new CoverNopExpression();
        }
        //printTree(expression, 1);
        CoverTypedExpressionNode result;
        if (expression instanceof CPPASTBinaryExpression) {
            result = processBinaryExpression(scope, (CPPASTBinaryExpression) expression);
        } else if (expression instanceof CPPASTLiteralExpression) {
            result = processLiteral(scope, (CPPASTLiteralExpression) expression);
        } else if (expression instanceof CPPASTArraySubscriptExpression) {
            result = processArraySubscriptExpression(scope, (CPPASTArraySubscriptExpression) expression);
        } else if (expression instanceof CPPASTIdExpression) {
            result = processId(scope, (CPPASTIdExpression) expression);                    
        } else if (expression instanceof CPPASTFunctionCallExpression) {
            result = processFunctionCall(scope, expression, type);
        } else if (expression instanceof CPPASTUnaryExpression) {
            result = processUnary(scope, (CPPASTUnaryExpression) expression);
        } else if (expression instanceof CPPASTCastExpression) {
            warn(expression, "ignoring cast");
            result = processExpression(scope, ((CPPASTCastExpression)expression).getOperand(), null);
        } else if (expression instanceof CPPASTFieldReference) {
            result = processFieldReference(scope, (CPPASTFieldReference)expression, null);
        } else if (expression instanceof CPPASTConditionalExpression) {
            result = processConditionalExpression(scope, (CPPASTConditionalExpression)expression, null);
        } else {
            throw new CoverParseException(expression, "unknown expression type " + expression.getClass().getSimpleName());
        }
        result.setSourceSection(createSourceSectionForNode("expression", expression));
        return result;
    }

    private CoverTypedExpressionNode processConditionalExpression(CoverScope scope,
            CPPASTConditionalExpression expression, Object object) {
        CoverTypedExpressionNode condition = processExpression(scope, expression.getLogicalConditionExpression(),null);
        CoverTypedExpressionNode positive = processExpression(scope, expression.getPositiveResultExpression(), null);
        CoverTypedExpressionNode negative = processExpression(scope, expression.getNegativeResultExpression(),null);
        CoverType resultType = positive.getType().combine(expression, negative.getType());
        return new CoverObjectConditionalExpressionNode(condition, positive, negative, resultType);
    }

    private CoverTypedExpressionNode processFieldReference(CoverScope scope, CPPASTFieldReference expression,
            Object object) {
        ICPPASTExpression owner = expression.getFieldOwner();
        CoverTypedExpressionNode ownerExpression = processExpression(scope, owner, null);
        String field = expression.getFieldName().toString();
        CoverType memberType = ownerExpression.getType().getObjectMembers().get(field);
        if (memberType.getBasicType() == BasicType.UNSIGNED_LONG) {
            return CoverReadUnsignedLongPropertyNodeGen.create(ownerExpression, memberType, field);
        } else if (memberType.getBasicType() == BasicType.SIGNED_LONG) {
            return CoverReadSignedLongPropertyNodeGen.create(ownerExpression, memberType, field);
        } else if (memberType.getBasicType() == BasicType.UNSIGNED_INT) {
            return CoverReadUnsignedIntPropertyNodeGen.create(ownerExpression, memberType, field);
        } else if (memberType.getBasicType() == BasicType.SIGNED_INT) {
            return CoverReadSignedIntPropertyNodeGen.create(ownerExpression, memberType, field);
        }else if (memberType.getBasicType() == BasicType.DOUBLE) {
            return CoverReadDoublePropertyNodeGen.create(ownerExpression, memberType, field);
        } else if (memberType.getBasicType() == BasicType.FLOAT) {
            return CoverReadFloatPropertyNodeGen.create(ownerExpression, memberType, field);
        } else {
            throw new CoverParseException(expression, "unsupported property type");
        }
    }

    private CoverTypedExpressionNode processArraySubscriptExpression(CoverScope scope,
            CPPASTArraySubscriptExpression expression) {
        ICPPASTExpression array = expression.getArrayExpression();
        IASTExpression subscript = expression.getSubscriptExpression();
        
        CoverReference ref = scope.findReference(array.getRawSignature());
        if (ref == null) {
            throw new CoverParseException(expression, "identifier not found");
        }
        if (ref.getType().getBasicType() != BasicType.ARRAY) {
            throw new CoverParseException(expression, "does not reference an array");
        }
        if (ref.getType().getTypeOfArrayContents().getBasicType() == BasicType.UNSIGNED_LONG) {
            return CoverReadUnsignedLongArrayValueNodeGen.create(CoverReadArrayVariableNodeGen.create(ref.getFrameSlot()), processExpression(scope, subscript, null));
        } else if (ref.getType().getTypeOfArrayContents().getBasicType() == BasicType.SIGNED_LONG) {
            return CoverReadSignedLongArrayValueNodeGen.create(CoverReadArrayVariableNodeGen.create(ref.getFrameSlot()), processExpression(scope, subscript, null));
        } else if (ref.getType().getTypeOfArrayContents().getBasicType() == BasicType.UNSIGNED_INT) {
            return CoverReadUnsignedIntArrayValueNodeGen.create(CoverReadArrayVariableNodeGen.create(ref.getFrameSlot()), processExpression(scope, subscript, null));
        } else if (ref.getType().getTypeOfArrayContents().getBasicType() == BasicType.SIGNED_INT) {
            return CoverReadSignedIntArrayValueNodeGen.create(CoverReadArrayVariableNodeGen.create(ref.getFrameSlot()), processExpression(scope, subscript, null));
        } else if (ref.getType().getTypeOfArrayContents().getBasicType() == BasicType.DOUBLE) {
            return CoverReadDoubleArrayValueNodeGen.create(CoverReadArrayVariableNodeGen.create(ref.getFrameSlot()), processExpression(scope, subscript, null));
        } else if (ref.getType().getTypeOfArrayContents().getBasicType() == BasicType.FLOAT) {
            return CoverReadFloatArrayValueNodeGen.create(CoverReadArrayVariableNodeGen.create(ref.getFrameSlot()), processExpression(scope, subscript, null));
        } else {
            throw new CoverParseException(expression, "unsupported array type " + ref.getType().getTypeOfArrayContents().getBasicType());
        }
    }

    private CoverTypedExpressionNode processBinaryExpression(CoverScope scope, CPPASTBinaryExpression expression) {
        int operator = expression.getOperator();
        CoverTypedExpressionNode result = null;
        if (operator == CPPASTBinaryExpression.op_lessThan) {
            CoverTypedExpressionNode leftNode = processExpression(scope, expression.getOperand1(), null);
            CoverTypedExpressionNode rightNode = processExpression(scope, expression.getOperand2(), null);
            result = createLessThanNode(expression, leftNode, rightNode);
        } else if (operator == CPPASTBinaryExpression.op_lessEqual) {
            CoverTypedExpressionNode leftNode = processExpression(scope, expression.getOperand1(), null);
            CoverTypedExpressionNode rightNode = processExpression(scope, expression.getOperand2(), null);
            result = createLessOrEqualNode(expression, leftNode, rightNode);
        } else if (operator == CPPASTBinaryExpression.op_equals) {
            CoverTypedExpressionNode leftNode = processExpression(scope, expression.getOperand1(), null);
            CoverTypedExpressionNode rightNode = processExpression(scope, expression.getOperand2(), null);
            result = SLEqualNodeGen.create(leftNode, rightNode);
        } else if (operator == CPPASTBinaryExpression.op_greaterThan) {
            CoverTypedExpressionNode leftNode = processExpression(scope, expression.getOperand1(), null);
            CoverTypedExpressionNode rightNode = processExpression(scope, expression.getOperand2(), null);
            result = createLessThanNode(expression, rightNode, leftNode);
        } else if (operator == CPPASTBinaryExpression.op_greaterEqual) {
            CoverTypedExpressionNode leftNode = processExpression(scope, expression.getOperand1(), null);
            CoverTypedExpressionNode rightNode = processExpression(scope, expression.getOperand2(), null);
            result = createLessOrEqualNode(expression, rightNode, leftNode);
        } else if (operator == CPPASTBinaryExpression.op_plusAssign) {
            CoverTypedExpressionNode change = processExpression(scope, expression.getOperand2(), null);
            CoverTypedExpressionNode source = processExpression(scope, expression.getOperand1(), null);
            result = createWriteVariableNode(scope, expression.getOperand1(), createAddNode(expression, source, change));
        } else if (operator == CPPASTBinaryExpression.op_minusAssign) {
            CoverTypedExpressionNode change = processExpression(scope, expression.getOperand2(), null);
            CoverTypedExpressionNode source = processExpression(scope, expression.getOperand1(), null);
            result = createWriteVariableNode(scope, expression.getOperand1(), createSubNode(expression, source, change));
        } else if (operator == CPPASTBinaryExpression.op_assign) {
            CoverTypedExpressionNode value = processExpression(scope, expression.getOperand2(), null);
            result = createWriteVariableNode(scope, expression.getOperand1(), value);
        } else if (operator == CPPASTBinaryExpression.op_multiply) {
            CoverTypedExpressionNode leftNode = processExpression(scope, expression.getOperand1(), null);
            CoverTypedExpressionNode rightNode = processExpression(scope, expression.getOperand2(), null);
            result = createMulNode(expression, leftNode, rightNode);
        } else if (operator == CPPASTBinaryExpression.op_divide) {
            CoverTypedExpressionNode leftNode = processExpression(scope, expression.getOperand1(), null);
            CoverTypedExpressionNode rightNode = processExpression(scope, expression.getOperand2(), null);
            result = createDivNode(expression, leftNode, rightNode);
        } else if (operator == CPPASTBinaryExpression.op_plus) {
            CoverTypedExpressionNode leftNode = processExpression(scope, expression.getOperand1(), null);
            CoverTypedExpressionNode rightNode = processExpression(scope, expression.getOperand2(), null);
            result = createAddNode(expression, leftNode, rightNode);
        } else if (operator == CPPASTBinaryExpression.op_minus) {
            CoverTypedExpressionNode leftNode = processExpression(scope, expression.getOperand1(), null);
            CoverTypedExpressionNode rightNode = processExpression(scope, expression.getOperand2(), null);
            result = createSubNode(expression, leftNode, rightNode);
        } else if (operator == CPPASTBinaryExpression.op_logicalAnd) {
            CoverTypedExpressionNode leftNode = processExpression(scope, expression.getOperand1(), null);
            CoverTypedExpressionNode rightNode = processExpression(scope, expression.getOperand2(), null);
            result = SLLogicalAndNodeGen.create(leftNode, rightNode);
        } else if (operator == CPPASTBinaryExpression.op_binaryAndAssign) {
            CoverTypedExpressionNode change = processExpression(scope, expression.getOperand2(), null);
            CoverTypedExpressionNode source = processExpression(scope, expression.getOperand1(), null);
            if(source.getType() == CoverType.UNSIGNED_INT || source.getType() == CoverType.SIGNED_INT)
                result = createWriteVariableNode(scope, expression.getOperand1(), SLBinaryIntOrNodeGen.create(source, change));
            else
                result = createWriteVariableNode(scope, expression.getOperand1(), SLBinaryLongOrNodeGen.create(source, change));
        } else if (operator == CPPASTBinaryExpression.op_binaryOr) {
            CoverTypedExpressionNode change = processExpression(scope, expression.getOperand2(), null);
            CoverTypedExpressionNode source = processExpression(scope, expression.getOperand1(), null);
            if(source.getType() == CoverType.UNSIGNED_INT || source.getType() == CoverType.SIGNED_INT)
                result = SLBinaryIntOrNodeGen.create(source, change);
            else
                result = SLBinaryLongOrNodeGen.create(source, change);
        } else if (operator == CPPASTBinaryExpression.op_binaryAnd) {
            CoverTypedExpressionNode change = processExpression(scope, expression.getOperand2(), null);
            CoverTypedExpressionNode source = processExpression(scope, expression.getOperand1(), null);
            if(source.getType() == CoverType.UNSIGNED_INT || source.getType() == CoverType.SIGNED_INT)
                result = SLBinaryIntAndNodeGen.create(source, change);
            else
                result = SLBinaryLongAndNodeGen.create(source, change);
        }   else if (operator == CPPASTBinaryExpression.op_shiftRightAssign) {
            CoverTypedExpressionNode change = processExpression(scope, expression.getOperand2(), null);
            CoverTypedExpressionNode source = processExpression(scope, expression.getOperand1(), null);
            if(source.getType() == CoverType.UNSIGNED_INT || source.getType() == CoverType.SIGNED_INT)
                result = createWriteVariableNode(scope, expression.getOperand1(), SLBinaryIntShiftRightNodeGen.create(source, change));
            else
                result = createWriteVariableNode(scope, expression.getOperand1(), SLBinaryLongShiftRightNodeGen.create(source, change));
        } else if (operator == CPPASTBinaryExpression.op_shiftLeftAssign) {
            CoverTypedExpressionNode change = processExpression(scope, expression.getOperand2(), null);
            CoverTypedExpressionNode source = processExpression(scope, expression.getOperand1(), null);
            if(source.getType() == CoverType.UNSIGNED_INT || source.getType() == CoverType.SIGNED_INT)
                result = createWriteVariableNode(scope, expression.getOperand1(), SLBinaryIntShiftLeftNodeGen.create(source, change));
            else
                result = createWriteVariableNode(scope, expression.getOperand1(), SLBinaryLongShiftLeftNodeGen.create(source, change));        } else if (operator == CPPASTBinaryExpression.op_binaryOrAssign) {
        } else if (operator == CPPASTBinaryExpression.op_binaryOrAssign) {
            CoverTypedExpressionNode change = processExpression(scope, expression.getOperand2(), null);
            CoverTypedExpressionNode source = processExpression(scope, expression.getOperand1(), null);
            if(source.getType() == CoverType.UNSIGNED_INT || source.getType() == CoverType.SIGNED_INT)
                result = createWriteVariableNode(scope, expression.getOperand1(), SLBinaryIntOrNodeGen.create(source, change));
            else
                result = createWriteVariableNode(scope, expression.getOperand1(), SLBinaryLongOrNodeGen.create(source, change));
        } else if (operator == CPPASTBinaryExpression.op_binaryXorAssign) {
            CoverTypedExpressionNode change = processExpression(scope, expression.getOperand2(), null);
            CoverTypedExpressionNode source = processExpression(scope, expression.getOperand1(), null);
            if(source.getType() == CoverType.UNSIGNED_INT || source.getType() == CoverType.SIGNED_INT)
                result = createWriteVariableNode(scope, expression.getOperand1(), SLBinaryIntXorNodeGen.create(source, change));
            else
                result = createWriteVariableNode(scope, expression.getOperand1(), SLBinaryLongXorNodeGen.create(source, change));
        } else if (operator == CPPASTBinaryExpression.op_binaryXor) {
            CoverTypedExpressionNode change = processExpression(scope, expression.getOperand2(), null);
            CoverTypedExpressionNode source = processExpression(scope, expression.getOperand1(), null);
            if(source.getType() == CoverType.UNSIGNED_INT || source.getType() == CoverType.SIGNED_INT)
                result = SLBinaryIntXorNodeGen.create(source, change);
            else
                result = SLBinaryLongXorNodeGen.create(source, change);
        } else if (operator == CPPASTBinaryExpression.op_shiftLeft) {
            CoverTypedExpressionNode change = processExpression(scope, expression.getOperand2(), null);
            CoverTypedExpressionNode source = processExpression(scope, expression.getOperand1(), null);
            if(source.getType() == CoverType.UNSIGNED_INT || source.getType() == CoverType.SIGNED_INT)
                result = SLBinaryIntShiftLeftNodeGen.create(source, change);
            else
                result = SLBinaryLongShiftLeftNodeGen.create(source, change);
        } else if (operator == CPPASTBinaryExpression.op_shiftRight) {
            CoverTypedExpressionNode change = processExpression(scope, expression.getOperand2(), null);
            CoverTypedExpressionNode source = processExpression(scope, expression.getOperand1(), null);
            if(source.getType() == CoverType.UNSIGNED_INT || source.getType() == CoverType.SIGNED_INT)
                result = SLBinaryIntOrNodeGen.create(source, change);
            else
                result = SLBinaryLongOrNodeGen.create(source, change);
        }  else if (operator == CPPASTBinaryExpression.op_modulo) {
            CoverTypedExpressionNode leftNode = processExpression(scope, expression.getOperand1(), null);
            CoverTypedExpressionNode rightNode = processExpression(scope, expression.getOperand2(), null);
            result = createModNode(expression, leftNode, rightNode);
        }  else if (operator == CPPASTBinaryExpression.op_notequals) {
            CoverTypedExpressionNode leftNode = processExpression(scope, expression.getOperand1(), null);
            CoverTypedExpressionNode rightNode = processExpression(scope, expression.getOperand2(), null);
            result = SLNotEqualNodeGen.create(leftNode, rightNode);
        } else {
            throw new CoverParseException(expression, "unknown operator type " + operator);
        }
        result.setSourceSection(createSourceSectionForNode("binary", expression));
        return result;
    }

    private CoverTypedExpressionNode createSubNode(CPPASTBinaryExpression expression, CoverTypedExpressionNode leftNode, CoverTypedExpressionNode rightNode) {
        CoverType newType = leftNode.getType().combine(expression, rightNode.getType());
        if (newType.equals(CoverType.SIGNED_LONG)) {
            return CoverSubSignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_LONG)) {
            return CoverSubUnsignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.SIGNED_INT)) {
            return CoverSubSignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_INT)) {
            return CoverSubUnsignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.DOUBLE)) {
            return CoverSubDoubleNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.FLOAT)) {
            return CoverSubFloatNodeGen.create(leftNode, rightNode);
        } else {
            throw new CoverParseException(expression, "cannot subtract type " + newType);
        }
    }

    private CoverTypedExpressionNode createMulNode(CPPASTBinaryExpression expression,
            CoverTypedExpressionNode leftNode, CoverTypedExpressionNode rightNode) {
        CoverType newType = leftNode.getType().combine(expression, rightNode.getType());
        if (newType.equals(CoverType.SIGNED_LONG)) {
            return CoverMulSignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_LONG)) {
            return CoverMulUnsignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.SIGNED_INT)) {
            return CoverMulSignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_INT)) {
            return CoverMulUnsignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.DOUBLE)) {
            return CoverMulDoubleNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.FLOAT)) {
            return CoverMulFloatNodeGen.create(leftNode, rightNode);
        } else {
            throw new CoverParseException(expression, "cannot multiply type " + newType);
        }
    }

    private CoverTypedExpressionNode createModNode(CPPASTBinaryExpression expression,
            CoverTypedExpressionNode leftNode, CoverTypedExpressionNode rightNode) {
        CoverType newType = leftNode.getType().combine(expression, rightNode.getType());
        if (newType.equals(CoverType.SIGNED_LONG)) {
            return CoverModSignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_LONG)) {
            return CoverModUnsignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.SIGNED_INT)) {
            return CoverModSignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_INT)) {
            return CoverModUnsignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.DOUBLE)) {
            return CoverModDoubleNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.FLOAT)) {
            return CoverModFloatNodeGen.create(leftNode, rightNode);
        } else {
            throw new CoverParseException(expression, "cannot mod type " + newType);
        }
    }

    private CoverTypedExpressionNode createLessOrEqualNode(CPPASTBinaryExpression expression,
            CoverTypedExpressionNode leftNode, CoverTypedExpressionNode rightNode) {
        CoverType newType = leftNode.getType().combine(expression, rightNode.getType());
        if (newType.equals(CoverType.SIGNED_LONG)) {
            return CoverLessOrEqualSignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_LONG)) {
            return CoverLessOrEqualUnsignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.SIGNED_INT)) {
            return CoverLessOrEqualSignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_INT)) {
            return CoverLessOrEqualUnsignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.DOUBLE)) {
            return CoverLessOrEqualDoubleNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.FLOAT)) {
            return CoverLessOrEqualFloatNodeGen.create(leftNode, rightNode);
        } else {
            throw new CoverParseException(expression, "cannot <= type " + newType);
        }
    }

    private CoverTypedExpressionNode createLessThanNode(CPPASTBinaryExpression expression,
            CoverTypedExpressionNode leftNode, CoverTypedExpressionNode rightNode) {
        CoverType newType = leftNode.getType().combine(expression, rightNode.getType());
        if (newType.equals(CoverType.SIGNED_LONG)) {
            return CoverLessThanSignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_LONG)) {
            return CoverLessThanUnsignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.SIGNED_INT)) {
            return CoverLessThanSignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_INT)) {
            return CoverLessThanUnsignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.DOUBLE)) {
            return CoverLessThanDoubleNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.FLOAT)) {
            return CoverLessThanFloatNodeGen.create(leftNode, rightNode);
        } else {
            throw new CoverParseException(expression, "cannot < type " + newType);
        }
    }

    private CoverTypedExpressionNode createDivNode(CPPASTBinaryExpression node,
            CoverTypedExpressionNode leftNode, CoverTypedExpressionNode rightNode) {
        CoverType newType = leftNode.getType().combine(node, rightNode.getType());
        if (newType.equals(CoverType.SIGNED_LONG)) {
            return CoverDivSignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_LONG)) {
            return CoverDivUnsignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.SIGNED_INT)) {
            return CoverDivSignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_INT)) {
            return CoverDivUnsignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.DOUBLE)) {
            return CoverDivDoubleNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.FLOAT)) {
            return CoverDivFloatNodeGen.create(leftNode, rightNode);
        } else {
            throw new CoverParseException(node, "cannot div type " + newType);
        }
    }

    private CoverTypedExpressionNode createAddNode(CPPASTBinaryExpression node, CoverTypedExpressionNode leftNode,
            CoverTypedExpressionNode rightNode) {
        CoverType newType = leftNode.getType().combine(node, rightNode.getType());
        if (newType.equals(CoverType.SIGNED_LONG)) {
            return CoverAddSignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_LONG)) {
            return CoverAddUnsignedLongNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.SIGNED_INT)) {
            return CoverAddSignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.UNSIGNED_INT)) {
            return CoverAddUnsignedIntNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.DOUBLE)) {
            return CoverAddDoubleNodeGen.create(leftNode, rightNode);
        } else if (newType.equals(CoverType.FLOAT)) {
            return CoverAddFloatNodeGen.create(leftNode, rightNode);
        } else {
            throw new CoverParseException(node, "cannot add type " + newType);
        }
    }

    private CoverTypedExpressionNode createWriteVariableNode(CoverScope scope, IASTExpression node, CoverTypedExpressionNode value) {
        // We parse the "left" expression, and then we add an assignment
        // Types of assignments:
        // LocalVariable (frameSlot)
        // ArrayMember (Object, index)
        // ObjectMember (TODO)
        
        if (node instanceof CPPASTIdExpression) {
            CPPASTIdExpression x = (CPPASTIdExpression) node;
            CoverReference ref = scope.findReference(x.getName().toString());
            if (ref == null) {
                throw new CoverParseException(node, "not found");
            }
            return createSimpleAssignmentNode(node, ref, value);
        } else if (node instanceof CPPASTArraySubscriptExpression) {
            CPPASTArraySubscriptExpression x = (CPPASTArraySubscriptExpression) node;
            ICPPASTExpression array = x.getArrayExpression();
            IASTExpression argument = (IASTExpression) x.getArgument();
            CoverTypedExpressionNode indexExpression = processExpression(scope, argument, null);
            
            CoverReference ref = scope.findReference(array.getRawSignature());
            if (ref == null) throw new CoverParseException(node, "not found");
            FrameSlot frameSlot = ref.getFrameSlot();
            if (frameSlot == null) throw new CoverParseException(node, "no frameslot");
            if (ref.getType().getBasicType() != BasicType.ARRAY)
                throw new CoverParseException(node, "is not an array");
            CoverReadArrayVariableNode arrayExpression = CoverReadArrayVariableNodeGen.create(frameSlot);
            BasicType elementType = ref.getType().getTypeOfArrayContents().getBasicType();
            if (elementType == BasicType.UNSIGNED_LONG) {
                return CoverWriteUnsignedLongArrayElementNodeGen.create(arrayExpression, indexExpression, value);
            } else if (elementType == BasicType.SIGNED_LONG) {
                return CoverWriteSignedLongArrayElementNodeGen.create(arrayExpression, indexExpression, value);
            } else if (elementType == BasicType.UNSIGNED_INT) {
                return CoverWriteUnsignedIntArrayElementNodeGen.create(arrayExpression, indexExpression, value);
            } else if (elementType == BasicType.SIGNED_INT) {
                return CoverWriteSignedIntArrayElementNodeGen.create(arrayExpression, indexExpression, value);
            } else if (elementType == BasicType.DOUBLE) {
                return CoverWriteDoubleArrayElementNodeGen.create(arrayExpression, indexExpression, value);
            } else if (elementType == BasicType.FLOAT) {
                return CoverWriteFloatArrayElementNodeGen.create(arrayExpression, indexExpression, value);
            } else {
                throw new CoverParseException(node, "unsupported array type for assignment " + elementType);
            }
             
        } else if (node instanceof CPPASTFieldReference) {
            CPPASTFieldReference fieldReference = (CPPASTFieldReference) node;
            CoverTypedExpressionNode owner = processExpression(scope, fieldReference.getFieldOwner(), null);
            String field = fieldReference.getFieldName().toString();
            CoverType memberType = owner.getType().getObjectMembers().get(field);
            if (memberType == null) {
                throw new CoverParseException(node, "field does not exist");
            }
            return CoverWritePropertyNodeGen.create(owner, value, field, memberType);
        }
        throw new CoverParseException(node, "unknown destination type: " + node.getClass().getSimpleName());
    }

    private CoverTypedExpressionNode createSimpleAssignmentNode(IASTNode node, CoverReference ref,
            CoverTypedExpressionNode value) {
        if (!ref.getType().canAccept(value.getType())) {
            throw new CoverParseException(node, "cannot assign "+value.getType()+" to " + ref.getType());
        }
        if (ref.getType().getBasicType() == BasicType.UNSIGNED_LONG) {
            return CoverWriteUnsignedLongNodeGen.create(value, ref.getFrameSlot());
        } else if (ref.getType().getBasicType() == BasicType.SIGNED_LONG) {
            return CoverWriteSignedLongNodeGen.create(value, ref.getFrameSlot());
        } else if (ref.getType().getBasicType() == BasicType.UNSIGNED_INT) {
            return CoverWriteUnsignedIntNodeGen.create(value, ref.getFrameSlot());
        } else if (ref.getType().getBasicType() == BasicType.SIGNED_INT) {
            return CoverWriteSignedIntNodeGen.create(value, ref.getFrameSlot());
        } else if (ref.getType().getBasicType() == BasicType.DOUBLE) {
            return CoverWriteDoubleNodeGen.create(value, ref.getFrameSlot());
        } else if (ref.getType().getBasicType() == BasicType.FLOAT) {
            return CoverWriteFloatNodeGen.create(value, ref.getFrameSlot());
        } else if (ref.getType().getBasicType() == BasicType.OBJECT) {
            return CoverWriteObjectNodeGen.create(value, ref.getFrameSlot());
        } else {
            throw new CoverParseException(node, "unsupported variable write");
        }
    }

    private CoverTypedExpressionNode processUnary(CoverScope scope, CPPASTUnaryExpression node) {
        int operator = node.getOperator();
        final int change;
        CoverTypedExpressionNode readNode = processExpression(scope, node.getOperand(), null);
        CoverType type = readNode.getType();

        if (operator == IASTUnaryExpression.op_postFixIncr || operator == IASTUnaryExpression.op_prefixIncr) {
            change = 1;
        } else if (operator == IASTUnaryExpression.op_postFixDecr || operator == IASTUnaryExpression.op_prefixDecr) {
            change = -1;
        } else if (operator == IASTUnaryExpression.op_bracketedPrimary) {
            return readNode;
        } else if (operator == IASTUnaryExpression.op_tilde) {
            if(type == CoverType.SIGNED_INT || type == CoverType.UNSIGNED_INT)
                return SLBinaryIntNotNodeGen.create(readNode);
            else
                return SLBinaryLongNotNodeGen.create(readNode);
        } else if (operator == IASTUnaryExpression.op_minus) {
            if(type == CoverType.SIGNED_INT || type == CoverType.UNSIGNED_INT)
                return SLIntFlipsignNodeGen.create(readNode);
            else
                return SLLongFlipsignNodeGen.create(readNode);
        } else if (operator == IASTUnaryExpression.op_plus) {
            return readNode; // This operator is not really needed
        } else {
            throw new CoverParseException(node, "Unsupported operator type " + operator);
        }

        // FIXME THIS IS WRONG HERE
        // turn (++i) into (i=i+1)
        CoverAddUnsignedLongNode addNode = CoverAddUnsignedLongNodeGen.create(readNode, new SLUnsignedLongLiteralNode(change));
        CoverTypedExpressionNode writeNode = createWriteVariableNode(scope, node.getOperand(), addNode);
        if (operator == IASTUnaryExpression.op_postFixIncr || operator == IASTUnaryExpression.op_postFixDecr) {
            // Use the "inverse comma" operator to return the old value of i
            return CoverInverseCommaUnsignedLongNodeGen.create(readNode, writeNode);
        } else {
            return writeNode;
        }
    }

    private SLStatementNode processDeclarationStatement(CoverScope scope, CPPASTDeclarationStatement node) {
        CPPASTSimpleDeclaration s = (CPPASTSimpleDeclaration) node.getDeclaration();
        return processDeclaration(scope, s);
    }

    private SLStatementNode processDeclaration(CoverScope scope, CPPASTSimpleDeclaration node) {
        //printTree(node, 1);
        
        // NOTE: this can also be a typedef!
        
        // Part 1: find the type
        IASTDeclSpecifier declSpecifier = node.getDeclSpecifier();
        CoverType type = processDeclSpecifier(scope, declSpecifier);
        
        // Part 2: declare variables
        IASTDeclarator[] declarators = node.getDeclarators();
        List<SLStatementNode> nodes = new ArrayList<SLStatementNode>();
        for (int i=0;i<declarators.length;i++) {
            IASTDeclarator declarator = declarators[i];
            String name = declarator.getName().toString();
            
            // throw away "const"
            String rawTypeName = declSpecifier.getRawSignature();
            String parts[] = rawTypeName.split(" ");
            if (parts.length > 1) {
                if (parts[0].equals("typedef")) {
                    // retry as typedef!
                    return processTypedef(scope, node);
                }
                if (!parts[0].equals("const")) {
                    throw new CoverParseException(node, "unknown declaration type: " + parts[0]);
                } else {
                    warn(node, "ignoring const");
                }
            }
            
            if (declarator instanceof CPPASTArrayDeclarator) {
                // we don't support initializers yet, so keep it empty
                CPPASTArrayDeclarator arrayDeclarator = (CPPASTArrayDeclarator) declarator;
                CoverTypedExpressionNode size = processExpression(scope, arrayDeclarator.getArrayModifiers()[0].getConstantExpression(), null);
                //System.err.println(name+" declared as array of " + type.getBasicType());
                CoverType arrayType = new CoverType(BasicType.ARRAY).setArrayType(type);
                CoverReference ref = scope.define(node, name, arrayType);
                if (type.getBasicType() == BasicType.DOUBLE) {
                    nodes.add(new CreateLocalDoubleArrayNode(ref.getFrameSlot(), size));
                } else if (type.getBasicType() == BasicType.FLOAT) {
                    nodes.add(new CreateLocalFloatArrayNode(ref.getFrameSlot(), size));
                } else if (type.getBasicType() == BasicType.UNSIGNED_LONG) {
                    nodes.add(new CreateLocalUnsignedLongArrayNode(ref.getFrameSlot(), size));
                } else if (type.getBasicType() == BasicType.SIGNED_LONG) {
                    nodes.add(new CreateLocalSignedLongArrayNode(ref.getFrameSlot(), size));
                } else if (type.getBasicType() == BasicType.UNSIGNED_INT) {
                    nodes.add(new CreateLocalUnsignedIntArrayNode(ref.getFrameSlot(), size));
                } else if (type.getBasicType() == BasicType.SIGNED_INT) {
                    nodes.add(new CreateLocalSignedIntArrayNode(ref.getFrameSlot(), size));
                } else {
                    throw new CoverParseException(node, "unsupported array type " + type.getBasicType());
                }
                if(scope.getParent()==null){
                    scope.addGlobalDef(nodes.get(0));
                }
            } else if (declarator instanceof CPPASTDeclarator) {
                CPPASTDeclarator d = (CPPASTDeclarator) declarators[i];
                //System.err.println(name+" declared as " + frameSlot.getKind());
                CoverReference ref = scope.define(node, name, type);
                CPPASTEqualsInitializer initializer = (CPPASTEqualsInitializer) d.getInitializer();
                if (initializer != null) {
                    CoverTypedExpressionNode expression = processExpression(scope, (IASTExpression) initializer.getInitializerClause(), type);
                    nodes.add(createSimpleAssignmentNode(node, ref, expression));
                } else {
                    // FIXME: initialize according to type
                    if (type.getBasicType() == BasicType.UNSIGNED_LONG || type.getBasicType() == BasicType.SIGNED_LONG || type.getBasicType() == BasicType.UNSIGNED_INT || type.getBasicType() == BasicType.SIGNED_INT || // TODO FIXME JUST WRONG HERE
                            type.getBasicType() == BasicType.DOUBLE ||
                            type.getBasicType() == BasicType.FLOAT) {
                        nodes.add(createSimpleAssignmentNode(d, ref, new SLUnsignedLongLiteralNode(0)));
                    } else if (type.getBasicType() == BasicType.OBJECT) {
                        nodes.add(createSimpleAssignmentNode(d, ref, new CoverCreateObjectNode(type)));
                    } else {
                        warn(node, "unknown type; not initialized");
                    }
                }
            } else {
                throw new CoverParseException(node, "unknown declarator type: " + declarators[i].getClass().getSimpleName());
            }
        }
        if (nodes.isEmpty()) {
            warn(node, "no declarators found");
        }
        return new SLBlockNode(nodes.stream().toArray(SLStatementNode[]::new));
    }

    private CoverType processDeclSpecifier(CoverScope scope, IASTDeclSpecifier node) {
        if (node instanceof CPPASTCompositeTypeSpecifier) {
            info(node, "found class/struct");
            CoverType newType = new CoverType(BasicType.OBJECT);
            CPPASTCompositeTypeSpecifier c = (CPPASTCompositeTypeSpecifier) node;
            String className = c.getName().toString();
            IASTDeclaration[] declarations = c.getDeclarations(false);
            // GK 2016-07-30: I'm not sure what's going on with layouts and shapes but
            // let's give this a try.
            Layout layout = Layout.createLayout();
            Shape shape = layout.createShape(SLObjectType.SINGLETON);
            for (IASTDeclaration d : declarations) {
                
                if (d instanceof CPPASTSimpleDeclaration) {
                    CPPASTSimpleDeclaration x = (CPPASTSimpleDeclaration) d;
                    CoverType memberType = processDeclSpecifier(scope, x.getDeclSpecifier());
                    for (IASTDeclarator declarator : x.getDeclarators()) {
                        String name = declarator.getName().toString();
                        newType.getObjectMembers().put(name, memberType);
                        // FIXME: also process initializers?
                        Object initialValue = null;
                        if (memberType.getBasicType() == BasicType.UNSIGNED_LONG || memberType.getBasicType() == BasicType.SIGNED_LONG) {
                            initialValue = (long) 0;
                        } else if (memberType.getBasicType() == BasicType.UNSIGNED_INT || memberType.getBasicType() == BasicType.SIGNED_INT) {
                            initialValue = (long) 0;
                        } else if (memberType.getBasicType() == BasicType.DOUBLE) {
                            initialValue = (double) 0.0;
                        } else if (memberType.getBasicType() == BasicType.FLOAT) {
                            initialValue = (float) 0.0;
                        } else {
                            warn(declarator, "unknown member type " + memberType + ", not initialized");
                        }
                        shape = shape.defineProperty(name, initialValue, 0);
                    }
                } else if (d instanceof CPPASTVisibilityLabel) {
                    warn(d, "ignoring visibility label");
                } else {
                    throw new CoverParseException(d, "usupported member type " + d.getClass().getSimpleName());
                }
            }
            newType.setShape(shape);
            scope.addType(className, newType);
            return newType;
        } else if (node instanceof CPPASTSimpleDeclSpecifier) {
            // int i;
            CPPASTSimpleDeclSpecifier d = (CPPASTSimpleDeclSpecifier) node;
            switch (d.getType()) {
            case CPPASTSimpleDeclSpecifier.t_unspecified:
                if (!d.isLong()) throw new CoverParseException(node, "unspecified is not a long!");
                return CoverType.SIGNED_LONG;
            case CPPASTSimpleDeclSpecifier.t_char:
                return CoverType.SIGNED_LONG;
            case CPPASTSimpleDeclSpecifier.t_int:
                return CoverType.SIGNED_INT;
            case CPPASTSimpleDeclSpecifier.t_double:
                return CoverType.DOUBLE;
            case CPPASTSimpleDeclSpecifier.t_float:
                return CoverType.FLOAT;
            // More types are initialized in Constructor

            default:
                throw new CoverParseException(node, "unsupported basic type: " + d.getType());
            }
        } else if (node instanceof CPPASTNamedTypeSpecifier) {
            // Test test;
            CPPASTNamedTypeSpecifier d = (CPPASTNamedTypeSpecifier) node;
            String oldTypeName = d.getName().toString();
            CoverType type = scope.findType(oldTypeName);            
            if (type == null) {
                throw new CoverParseException(node, "type not found");
            }
            return type;
        } else {
            throw new CoverParseException(node, "cannot process declaration with type specifier " + node.getClass().getSimpleName());
        }
    }

    private CPPASTSimpleDeclaration declarator(CPPASTSimpleDeclaration d) {
        // TODO Auto-generated method stub
        return null;
    }

    private void warn(IASTNode node, String message) {
        System.err.println(nodeMessage(node, "warning: " + message));
    }

    private void info(IASTNode node, String message) {
        System.err.println(nodeMessage(node, "info: " + message));
    }

    // TODO FIXME: Make sure to support all the signed unsigned literals here
    private CoverTypedExpressionNode processLiteral(CoverScope scope, CPPASTLiteralExpression y) {
        if (y.getKind() == IASTLiteralExpression.lk_string_literal) {
            String v = new String(y.getValue());
            String noQuotes = v.substring(1, v.length() - 1).replace("\\n", "\n");
            return new SLStringLiteralNode(noQuotes);
        } else if (y.getKind() == IASTLiteralExpression.lk_integer_constant) {
            String stringValue = new String(y.getValue());
            final long longValue;
            if (stringValue.startsWith("0x")) {
                longValue = Long.parseLong(stringValue.substring(2), 16);
            } else {
                longValue = Long.parseLong(stringValue);
            }
            return new SLUnsignedLongLiteralNode(longValue);
        } else if (y.getKind() == IASTLiteralExpression.lk_float_constant) { // fixme special care
            return new CoverDoubleLiteralNode(Double.parseDouble(new String(y.getValue())));
        } else {
            throw new CoverParseException(y, "unsupported literal type: " + y.getKind());
        }
    }

    private CoverTypedExpressionNode processFunctionCall(CoverScope scope, IASTNode node, CoverType type) {
        CPPASTFunctionCallExpression functionCall = (CPPASTFunctionCallExpression) node;
        String rawName = functionCall.getFunctionNameExpression().getRawSignature();

        List<CoverTypedExpressionNode> coverArguments = new ArrayList<>();
        for (IASTInitializerClause x : functionCall.getArguments()) {
            if (x instanceof IASTExpression) {
                coverArguments.add(processExpression(scope, (IASTExpression) x, null));
            } else {
                throw new CoverParseException(node, "Unknown function argument type: " + x.getClass());
            }
        }
        CoverTypedExpressionNode[] argumentArray = coverArguments.toArray(new CoverTypedExpressionNode[coverArguments.size()]);
        
        if ("puts".equals(rawName)) {
            NodeFactory<SLPrintlnBuiltin> printlnBuiltinFactory = SLPrintlnBuiltinFactory.getInstance();
            return printlnBuiltinFactory.createNode(argumentArray, CoverLanguage.INSTANCE.findContext());
        } else if ("printf".equals(rawName)) {
            return new CoverPrintfBuiltin(argumentArray);
        } else if ("fwrite".equals(rawName)) {
            return CoverFWriteBuiltinNodeGen.create(argumentArray[0], argumentArray[1], argumentArray[2], argumentArray[3]);
        } else if ("putc".equals(rawName)) {
            return CoverPutcBuiltinNodeGen.create(argumentArray[0], argumentArray[1]);
        } else if ("sin".equals(rawName)) {
            return CoverSinBuiltinNodeGen.create(argumentArray[0]);
        } else if ("sinh".equals(rawName)) {
            return CoverSinhBuiltinNodeGen.create(argumentArray[0]);
        } else if ("sqrt".equals(rawName)) {
            return CoverSqrtBuiltinNodeGen.create(argumentArray[0]);
        } else if ("tan".equals(rawName)) {
            return CoverTanBuiltinNodeGen.create(argumentArray[0]);
        } else if ("tanh".equals(rawName)) {
            return CoverTanhBuiltinNodeGen.create(argumentArray[0]);
        } else if ("acos".equals(rawName)) {
            return CoverAcosBuiltinNodeGen.create(argumentArray[0]);
        } else if ("atan".equals(rawName)) {
            return CoverAtanBuiltinNodeGen.create(argumentArray[0]);
        } else if ("atan2".equals(rawName)) {
            return CoverAtan2BuiltinNodeGen.create(argumentArray[0], argumentArray[1]);
        } else if ("ceil".equals(rawName)) {
            return CoverCeilBuiltinNodeGen.create(argumentArray[0]);
        } else if ("cos".equals(rawName)) {
            return CoverCosBuiltinNodeGen.create(argumentArray[0]);
        } else if ("cosh".equals(rawName)) {
            return CoverCoshBuiltinNodeGen.create(argumentArray[0]);
        } else if ("exp".equals(rawName)) {
            return CoverExpBuiltinNodeGen.create(argumentArray[0]);
        } else if ("exp2".equals(rawName)) {
            return CoverExp2BuiltinNodeGen.create(argumentArray[0]);
        } else if ("log".equals(rawName)) {
            return CoverLogBuiltinNodeGen.create(argumentArray[0]);
        } else if ("log2".equals(rawName)) {
            return CoverLog2BuiltinNodeGen.create(argumentArray[0]);
        } else if ("log10".equals(rawName)) {
            return CoverLog10BuiltinNodeGen.create(argumentArray[0]);
        } else if ("fabs".equals(rawName)) {
            return CoverFabsBuiltinNodeGen.create(argumentArray[0]);
        } else if ("abs".equals(rawName)) {
            return CoverAbsBuiltinNodeGen.create(argumentArray[0]);
        } else if ("floor".equals(rawName)) {
            return CoverFloorBuiltinNodeGen.create(argumentArray[0]);
        } else if ("gcd".equals(rawName)) {
            return CoverGcdBuiltinNodeGen.create(argumentArray[0], argumentArray[1]);
        } else if ("pow".equals(rawName)) {
            return CoverPowBuiltinNodeGen.create(argumentArray[0], argumentArray[1]);
        } else if ("fmod".equals(rawName)) {
            return CoverFmodBuiltinNodeGen.create(argumentArray[0], argumentArray[1]);
        } else if ("rotl32".equals(rawName)) {
            return CoverRotl32BuiltinNodeGen.create(argumentArray[0], argumentArray[1]);
        } else if ("rotr32".equals(rawName)) {
            return CoverRotl32BuiltinNodeGen.create(argumentArray[0], argumentArray[1]);
        } else if ("rotl64".equals(rawName)) {
            return CoverRotl32BuiltinNodeGen.create(argumentArray[0], argumentArray[1]);
        } else if ("rotr64".equals(rawName)) {
            return CoverRotl32BuiltinNodeGen.create(argumentArray[0], argumentArray[1]);
        } else {
            CoverTypedExpressionNode function = processExpression(scope, functionCall.getFunctionNameExpression(), CoverType.DOUBLE);
            return new SLInvokeNode(function, argumentArray);
        }
    }

    private CoverTypedExpressionNode processId(CoverScope scope, CPPASTIdExpression id) {
        String name = id.getName().getRawSignature();
        CoverReference ref = scope.findReference(name);
        if (ref != null) {
            if (ref.getFrameSlot() != null) {
                if (ref.getType().getBasicType().equals(BasicType.UNSIGNED_LONG)) {
                    return CoverReadUnsignedLongVariableNodeGen.create(ref.getFrameSlot());
                } else if (ref.getType().getBasicType().equals(BasicType.SIGNED_LONG)) {
                    return CoverReadSignedLongVariableNodeGen.create(ref.getFrameSlot());
                } else if (ref.getType().getBasicType().equals(BasicType.UNSIGNED_INT)) {
                    return CoverReadUnsignedIntVariableNodeGen.create(ref.getFrameSlot());
                } else if (ref.getType().getBasicType().equals(BasicType.SIGNED_INT)) {
                    return CoverReadSignedIntVariableNodeGen.create(ref.getFrameSlot());
                } else if (ref.getType().getBasicType().equals(BasicType.DOUBLE)) {
                    return CoverReadDoubleVariableNodeGen.create(ref.getFrameSlot());
                } else if (ref.getType().getBasicType().equals(BasicType.FLOAT)) {
                    return CoverReadFloatVariableNodeGen.create(ref.getFrameSlot());
                } else if (ref.getType().getBasicType().equals(BasicType.ARRAY)) {
                    return CoverReadArrayVariableNodeGen.create(ref.getFrameSlot());
                } else if (ref.getType().getBasicType().equals(BasicType.OBJECT)) {
                    return CoverReadObjectVariableNodeGen.create(ref.getType(), ref.getFrameSlot());
                } else {
                    throw new CoverParseException(id, "unsupported variable read " + ref.getType());
                }
            } else if (ref.getFunction() != null){
                return new CoverFunctionLiteralNode(ref.getFunction());
            } else {
                throw new CoverParseException(id, "not a variable or function");
            }
        } else {
            throw new CoverParseException(id, "not found in local scope");
        }
    }

    private CoverTypedExpressionNode processFunctionDefinition(CoverScope scope, CPPASTFunctionDefinition node) {
        /*
           -CPPASTFunctionDefinition (offset: 1,81) -> void 
             -CPPASTSimpleDeclSpecifier (offset: 1,4) -> void
             -CPPASTFunctionDeclarator (offset: 6,18) -> doStuff(int count)
               -CPPASTName (offset: 6,7) -> doStuff
               -CPPASTParameterDeclaration (offset: 14,9) -> int count
                 -CPPASTSimpleDeclSpecifier (offset: 14,3) -> int
                 -CPPASTDeclarator (offset: 18,5) -> count
                   -CPPASTName (offset: 18,5) -> count
             -CPPASTCompoundStatement (offset: 25,57) -> {
         */
        CoverScope newScope = new CoverScope(scope);
        CPPASTFunctionDeclarator declarator = (CPPASTFunctionDeclarator) node.getDeclarator();
        ICPPASTParameterDeclaration[] parameters = declarator.getParameters();
        SLStatementNode[] readArgumentsStatements = new SLStatementNode[parameters.length];
        for (int i = 0;i<parameters.length;i++) {
            ICPPASTParameterDeclaration parameter = parameters[i];
            String name = parameter.getDeclarator().getName().getRawSignature();
            CoverType type = processDeclSpecifier(scope, parameter.getDeclSpecifier());
            CoverReference ref = newScope.define(node, name, type);
            
            // copy to local var in the prologue
            final CoverTypedExpressionNode readArg;
            if (type.getBasicType() == BasicType.UNSIGNED_LONG) {
                readArg = CoverReadUnsignedLongArgumentNodeGen.create(i);
            } else if (type.getBasicType() == BasicType.SIGNED_LONG) {
                readArg = CoverReadSignedLongArgumentNodeGen.create(i);
            } else if (type.getBasicType() == BasicType.UNSIGNED_INT) {
                readArg = CoverReadUnsignedIntArgumentNodeGen.create(i);
            } else if (type.getBasicType() == BasicType.SIGNED_INT) {
                readArg = CoverReadSignedIntArgumentNodeGen.create(i);
            } else {
                throw new CoverParseException(node, "unsupported argument type");
            }
            CoverTypedExpressionNode assignment = createSimpleAssignmentNode(node, ref, readArg);
            readArgumentsStatements[i] = assignment;
        }
        
        SLBlockNode readArgumentsNode = new SLBlockNode(readArgumentsStatements);
        
        IASTStatement s = node.getBody();
        SLStatementNode blockNode = processStatement(newScope, s);
        SLBlockNode wrappedBodyNode = new SLBlockNode(new SLStatementNode[] {readArgumentsNode, blockNode});
        final SLFunctionBodyNode functionBodyNode = new SLFunctionBodyNode(wrappedBodyNode);
        functionBodyNode.addRootTag();
        
        // we will now add code to read the arguments into the frame
        // load local variables from arguments
        
        String functionName = declarator.getName().toString();
        // for int main() {} we create a main = (int)(){} assignment
        SLFunction function = new SLFunction(functionName);
        SLRootNode rootNode = new SLRootNode(newScope.getFrameDescriptor(), functionBodyNode, null, functionName);
        RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        function.setCallTarget(callTarget);
        
        CoverReference reference = scope.define(node, functionName, new CoverType(BasicType.FUNCTION));
        reference.setFunction(function);
        return new CoverNopExpression();
    }

    private SourceSection createSourceSectionForNode(String identifier, IASTNode expression) {
        IASTFileLocation fileLocation = expression.getFileLocation();
        int charIndex = fileLocation.getNodeOffset();
        int length = fileLocation.getNodeLength();
        return source.createSection(identifier, charIndex, length);
    }

    private SLBlockNode processCompoundStatement(CoverScope scope, IASTStatement s) {
        IASTCompoundStatement compound = (IASTCompoundStatement) s;
        List<SLStatementNode> statements = new ArrayList<SLStatementNode>();
        for (IASTStatement statement : compound.getStatements()) {
            statements.add(processStatement(scope, statement));
        }
        SLBlockNode blockNode = new SLBlockNode(statements.toArray(new SLStatementNode[statements.size()]));
        return blockNode;
    }

    static String nodeMessage(IASTNode node, String message) {
        if (node == null) {
            return "<unknown>: " + message;
        }
        IASTFileLocation f = node.getFileLocation();
        return f.getFileName() + ":" + f.getStartingLineNumber() + ": " + message + ": '" + node.getRawSignature() + "'";
    }

    public static void printTree(IASTNode node, int index) {
        IASTNode[] children = node.getChildren();

        boolean printContents = true;

        if ((node instanceof CPPASTTranslationUnit)) {
            printContents = false;
        }

        String offset = "";
        try {
            offset = node.getSyntax() != null ? " (offset: " + node.getFileLocation().getNodeOffset() + ","
                    + node.getFileLocation().getNodeLength() + ")" : "";
            printContents = node.getFileLocation().getNodeLength() < 30;
        } catch (ExpansionOverlapsBoundaryException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            offset = "UnsupportedOperationException";
        }

        System.err.println(String.format(new StringBuilder("%1$").append(index * 2).append("s").toString(),
                new Object[] { "-" })
                + node.getClass().getSimpleName()
                + offset
                + " -> "
                + (printContents ? node.getRawSignature().replaceAll("\n", " \\ ") : node.getRawSignature()
                        .subSequence(0, 5)));

        for (IASTNode iastNode : children)
            printTree(iastNode, index + 1);
    }
}
