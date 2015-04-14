/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import static java.util.Collections.singletonList;

import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.*;

/**
 * Returns the estimated byte code size for a {@link Node}.
 */
final class CodeSizeVisitor implements IntNodeVisitor<CodeSizeHandler> {
    public int startAnalyze(Node node, List<? extends Node> children, CodeSizeHandler handler) {
        return analyze(node, children, 0, 0, handler);
    }

    public int startAnalyze(Node node, CodeSizeHandler handler) {
        return node.accept(this, handler);
    }

    private void submit(TopLevelNode<?> node, List<? extends Node> children, CodeSizeHandler handler) {
        handler.submit(node, children);
    }

    private int reportSize(Node node, int size, CodeSizeHandler handler) {
        return handler.reportSize(node, size);
    }

    private int analyze(Node node, Node child, int nodeSize, CodeSizeHandler handler) {
        int size = nodeSize;
        if (child != null) {
            size += child.accept(this, handler);
        }
        return reportSize(node, size, handler);
    }

    private int analyze(Node node, Node left, Node right, int nodeSize, CodeSizeHandler handler) {
        int size = nodeSize;
        if (left != null) {
            size += left.accept(this, handler);
        }
        if (right != null) {
            size += right.accept(this, handler);
        }
        return reportSize(node, size, handler);
    }

    private int analyze(Node node, Node left, Node middle, Node right, int nodeSize,
            CodeSizeHandler handler) {
        int size = nodeSize;
        if (left != null) {
            size += left.accept(this, handler);
        }
        if (middle != null) {
            size += middle.accept(this, handler);
        }
        if (right != null) {
            size += right.accept(this, handler);
        }
        return reportSize(node, size, handler);
    }

    private int analyze(Node node, Node left, Node middle, Node right, Node extra, int nodeSize,
            CodeSizeHandler handler) {
        int size = nodeSize;
        if (left != null) {
            size += left.accept(this, handler);
        }
        if (middle != null) {
            size += middle.accept(this, handler);
        }
        if (right != null) {
            size += right.accept(this, handler);
        }
        if (extra != null) {
            size += extra.accept(this, handler);
        }
        return reportSize(node, size, handler);
    }

    private int analyze(Node node, Node left, Node middle, Node right,
            List<? extends Node> children, int nodeSize, int childFactor, CodeSizeHandler handler) {
        int size = nodeSize + childFactor * children.size();
        if (left != null) {
            size += left.accept(this, handler);
        }
        if (middle != null) {
            size += middle.accept(this, handler);
        }
        if (right != null) {
            size += right.accept(this, handler);
        }
        for (Node child : children) {
            size += child.accept(this, handler);
        }
        return reportSize(node, size, handler);
    }

    private int analyze(Node node, List<? extends Node> children, int nodeSize, int childFactor,
            CodeSizeHandler handler) {
        int size = nodeSize + childFactor * children.size();
        for (Node child : children) {
            size += child.accept(this, handler);
        }
        return reportSize(node, size, handler);
    }

    private int analyze(Node node, List<? extends Node> children, Node extra, int nodeSize,
            int childFactor, CodeSizeHandler handler) {
        int size = nodeSize + childFactor * children.size();
        for (Node child : children) {
            size += child.accept(this, handler);
        }
        if (extra != null) {
            size += extra.accept(this, handler);
        }
        return reportSize(node, size, handler);
    }

    private int analyze(Node node, List<? extends Node> children,
            List<? extends Node> moreChildren, Node extra, int nodeSize, int childFactor,
            CodeSizeHandler handler) {
        int size = nodeSize + childFactor * children.size() + childFactor * moreChildren.size();
        for (Node child : children) {
            size += child.accept(this, handler);
        }
        for (Node child : moreChildren) {
            size += child.accept(this, handler);
        }
        if (extra != null) {
            size += extra.accept(this, handler);
        }
        return reportSize(node, size, handler);
    }

    private int analyze(MethodDefinition node, int nodeSize, CodeSizeHandler handler) {
        int size = nodeSize;
        size += node.getPropertyName().accept(this, handler);
        for (Node child : node.getDecorators()) {
            size += child.accept(this, handler);
        }
        // TODO: reportSize() problematic
        // return reportSize(node, size, handler);
        return size;
    }

    private int stringSize(String s) {
        if (s.length() <= 32768) {
            return 5;
        }
        return ((s.length() / 32768) + 1) * 10;
    }

    /* ***************************************************************************************** */

    @Override
    public int visit(ArrayAssignmentPattern node, CodeSizeHandler handler) {
        return analyze(node, node.getElements(), 30, 5, handler);
    }

    @Override
    public int visit(ArrayBindingPattern node, CodeSizeHandler handler) {
        return analyze(node, node.getElements(), 30, 5, handler);
    }

    @Override
    public int visit(ArrayComprehension node, CodeSizeHandler handler) {
        return analyze(node, node.getComprehension(), 25, handler);
    }

    @Override
    public int visit(ArrayLiteral node, CodeSizeHandler handler) {
        return analyze(node, node.getElements(), 25, 5, handler);
    }

    @Override
    public int visit(ArrowFunction node, CodeSizeHandler handler) {
        if (node.getExpression() != null) {
            submit(node, singletonList(node.getExpression()), handler);
        } else {
            submit(node, node.getStatements(), handler);
        }
        return 10;
    }

    @Override
    public int visit(AssignmentElement node, CodeSizeHandler handler) {
        return analyze(node, node.getTarget(), node.getInitializer(), 25, handler);
    }

    @Override
    public int visit(AssignmentExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getLeft(), node.getRight(), 10, handler);
    }

    @Override
    public int visit(AssignmentProperty node, CodeSizeHandler handler) {
        return analyze(node, node.getPropertyName(), node.getTarget(), node.getInitializer(), 25,
                handler);
    }

    @Override
    public int visit(AssignmentRestElement node, CodeSizeHandler handler) {
        return analyze(node, node.getTarget(), 25, handler);
    }

    @Override
    public int visit(AsyncArrowFunction node, CodeSizeHandler handler) {
        if (node.getExpression() != null) {
            submit(node, singletonList(node.getExpression()), handler);
        } else {
            submit(node, node.getStatements(), handler);
        }
        return 10;
    }

    @Override
    public int visit(AsyncFunctionDeclaration node, CodeSizeHandler handler) {
        submit(node, node.getStatements(), handler);
        return 0;
    }

    @Override
    public int visit(AsyncFunctionExpression node, CodeSizeHandler handler) {
        submit(node, node.getStatements(), handler);
        return 10;
    }

    @Override
    public int visit(AwaitExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getExpression(), 150, handler);
    }

    @Override
    public int visit(BinaryExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getLeft(), node.getRight(), 20, handler);
    }

    @Override
    public int visit(BindingElement node, CodeSizeHandler handler) {
        return analyze(node, node.getBinding(), node.getInitializer(), 25, handler);
    }

    @Override
    public int visit(BindingElision node, CodeSizeHandler handler) {
        return 0;
    }

    @Override
    public int visit(BindingIdentifier node, CodeSizeHandler handler) {
        return 15;
    }

    @Override
    public int visit(BindingProperty node, CodeSizeHandler handler) {
        return analyze(node, node.getPropertyName(), node.getBinding(), node.getInitializer(), 25,
                handler);
    }

    @Override
    public int visit(BindingRestElement node, CodeSizeHandler handler) {
        return analyze(node, node.getBindingIdentifier(), 25, handler);
    }

    @Override
    public int visit(BlockStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getStatements(), 15, 0, handler);
    }

    @Override
    public int visit(BooleanLiteral node, CodeSizeHandler handler) {
        return 5;
    }

    @Override
    public int visit(BreakStatement node, CodeSizeHandler handler) {
        return 5;
    }

    @Override
    public int visit(CallExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getArguments(), node.getBase(), 30, 5, handler);
    }

    @Override
    public int visit(CallSpreadElement node, CodeSizeHandler handler) {
        return analyze(node, node.getExpression(), 10, handler);
    }

    @Override
    public int visit(CatchNode node, CodeSizeHandler handler) {
        return analyze(node, node.getCatchParameter(), node.getCatchBlock(), 35, handler);
    }

    @Override
    public int visit(ClassDeclaration node, CodeSizeHandler handler) {
        return analyze(node, node.getDecorators(), node.getProperties(), node.getHeritage(), 50,
                10, handler);
    }

    @Override
    public int visit(ClassExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getDecorators(), node.getProperties(), node.getHeritage(), 50,
                10, handler);
    }

    @Override
    public int visit(CommaExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getOperands(), 5, 5, handler);
    }

    @Override
    public int visit(Comprehension node, CodeSizeHandler handler) {
        return analyze(node, node.getList(), node.getExpression(), 50, 0, handler);
    }

    @Override
    public int visit(ComprehensionFor node, CodeSizeHandler handler) {
        return analyze(node, node.getBinding(), node.getExpression(), 40, handler);
    }

    @Override
    public int visit(ComprehensionIf node, CodeSizeHandler handler) {
        return analyze(node, node.getTest(), 15, handler);
    }

    @Override
    public int visit(ComputedPropertyName node, CodeSizeHandler handler) {
        return analyze(node, node.getExpression(), 15, handler);
    }

    @Override
    public int visit(ConditionalExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getTest(), node.getThen(), node.getOtherwise(), 20, handler);
    }

    @Override
    public int visit(ContinueStatement node, CodeSizeHandler handler) {
        return 5;
    }

    @Override
    public int visit(DebuggerStatement node, CodeSizeHandler handler) {
        return 0;
    }

    @Override
    public int visit(DoWhileStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getTest(), node.getStatement(), 25, handler);
    }

    @Override
    public int visit(ElementAccessor node, CodeSizeHandler handler) {
        return analyze(node, node.getBase(), node.getElement(), 10, handler);
    }

    @Override
    public int visit(ElementAccessorValue node, CodeSizeHandler handler) {
        return analyze(node, node.getBase(), node.getElement(), 10, handler);
    }

    @Override
    public int visit(Elision node, CodeSizeHandler handler) {
        return 0;
    }

    @Override
    public int visit(EmptyExpression node, CodeSizeHandler handler) {
        return 0;
    }

    @Override
    public int visit(EmptyStatement node, CodeSizeHandler handler) {
        return 0;
    }

    @Override
    public int visit(ExportDeclaration node, CodeSizeHandler handler) {
        switch (node.getType()) {
        case All:
        case External:
        case Local:
            return 0;
        case Variable:
            return analyze(node, node.getVariableStatement(), 0, handler);
        case Declaration:
            return analyze(node, node.getDeclaration(), 0, handler);
        case DefaultHoistableDeclaration:
            return analyze(node, node.getHoistableDeclaration(), 0, handler);
        case DefaultClassDeclaration:
            return analyze(node, node.getClassDeclaration(), 0, handler);
        case DefaultExpression:
            return analyze(node, node.getExpression(), 0, handler);
        default:
            throw new AssertionError();
        }
    }

    @Override
    public int visit(ExportDefaultExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getExpression(), 15, handler);
    }

    @Override
    public int visit(ExportSpecifier node, CodeSizeHandler handler) {
        return 0;
    }

    @Override
    public int visit(ExportClause node, CodeSizeHandler handler) {
        return 0;
    }

    @Override
    public int visit(ExpressionMethod node, CodeSizeHandler handler) {
        return 5;
    }

    @Override
    public int visit(ExpressionStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getExpression(), 5, handler);
    }

    @Override
    public int visit(ForEachStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getHead(), node.getExpression(), node.getStatement(), 150,
                handler);
    }

    @Override
    public int visit(ForInStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getHead(), node.getExpression(), node.getStatement(), 150,
                handler);
    }

    @Override
    public int visit(FormalParameterList node, CodeSizeHandler handler) {
        return 0;
    }

    @Override
    public int visit(ForOfStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getHead(), node.getExpression(), node.getStatement(), 150,
                handler);
    }

    @Override
    public int visit(ForStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getHead(), node.getTest(), node.getStep(), node.getStatement(),
                30, handler);
    }

    @Override
    public int visit(FunctionDeclaration node, CodeSizeHandler handler) {
        submit(node, node.getStatements(), handler);
        return 0;
    }

    @Override
    public int visit(FunctionExpression node, CodeSizeHandler handler) {
        submit(node, node.getStatements(), handler);
        return 10;
    }

    @Override
    public int visit(GeneratorComprehension node, CodeSizeHandler handler) {
        submit(node, singletonList(node.getComprehension()), handler);
        return 10;
    }

    @Override
    public int visit(GeneratorDeclaration node, CodeSizeHandler handler) {
        submit(node, node.getStatements(), handler);
        return 0;
    }

    @Override
    public int visit(GeneratorExpression node, CodeSizeHandler handler) {
        submit(node, node.getStatements(), handler);
        return 10;
    }

    @Override
    public int visit(GuardedCatchNode node, CodeSizeHandler handler) {
        return analyze(node, node.getCatchParameter(), node.getGuard(), node.getCatchBlock(), 45,
                handler);
    }

    @Override
    public int visit(IdentifierName node, CodeSizeHandler handler) {
        return 5;
    }

    @Override
    public int visit(IdentifierReference node, CodeSizeHandler handler) {
        return 10;
    }

    @Override
    public int visit(IdentifierReferenceValue node, CodeSizeHandler handler) {
        return 10;
    }

    @Override
    public int visit(IfStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getTest(), node.getOtherwise(), node.getThen(), 10, handler);
    }

    @Override
    public int visit(ImportDeclaration node, CodeSizeHandler handler) {
        return 0;
    }

    @Override
    public int visit(ImportSpecifier node, CodeSizeHandler handler) {
        throw new IllegalStateException();
    }

    @Override
    public int visit(ImportClause node, CodeSizeHandler handler) {
        throw new IllegalStateException();
    }

    @Override
    public int visit(LabelledFunctionStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getFunction(), 0, handler);
    }

    @Override
    public int visit(LabelledStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getStatement(), 15, handler);
    }

    @Override
    public int visit(LegacyComprehension node, CodeSizeHandler handler) {
        return analyze(node, node.getList(), node.getExpression(), 50, 0, handler);
    }

    @Override
    public int visit(LegacyComprehensionFor node, CodeSizeHandler handler) {
        return analyze(node, node.getBinding(), node.getExpression(), 40, handler);
    }

    @Override
    public int visit(LegacyGeneratorDeclaration node, CodeSizeHandler handler) {
        return visit((GeneratorDeclaration) node, handler);
    }

    @Override
    public int visit(LegacyGeneratorExpression node, CodeSizeHandler handler) {
        return visit((GeneratorExpression) node, handler);
    }

    @Override
    public int visit(LetExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getBindings(), node.getExpression(), 25, 5, handler);
    }

    @Override
    public int visit(LetStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getBindings(), node.getStatement(), 25, 5, handler);
    }

    @Override
    public int visit(LexicalBinding node, CodeSizeHandler handler) {
        return analyze(node, node.getBinding(), node.getInitializer(), 20, handler);
    }

    @Override
    public int visit(LexicalDeclaration node, CodeSizeHandler handler) {
        return analyze(node, node.getElements(), 0, 0, handler);
    }

    @Override
    public int visit(MethodDefinition node, CodeSizeHandler handler) {
        submit(node, node.getStatements(), handler);
        return analyze(node, 10, handler);
    }

    @Override
    public int visit(MethodDefinitionsMethod node, CodeSizeHandler handler) {
        // don't descend into synthetic nodes
        return 10;
    }

    @Override
    public int visit(Module node, CodeSizeHandler handler) {
        return 0;
    }

    @Override
    public int visit(NativeCallExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getArguments(), node.getBase(), 30, 5, handler);
    }

    @Override
    public int visit(NewExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getArguments(), node.getExpression(), 15, 5, handler);
    }

    @Override
    public int visit(NewTarget node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public int visit(NullLiteral node, CodeSizeHandler handler) {
        return 5;
    }

    @Override
    public int visit(NumericLiteral node, CodeSizeHandler handler) {
        return 5;
    }

    @Override
    public int visit(ObjectAssignmentPattern node, CodeSizeHandler handler) {
        return analyze(node, node.getProperties(), 20, 5, handler);
    }

    @Override
    public int visit(ObjectBindingPattern node, CodeSizeHandler handler) {
        return analyze(node, node.getProperties(), 20, 5, handler);
    }

    @Override
    public int visit(ObjectLiteral node, CodeSizeHandler handler) {
        return analyze(node, node.getProperties(), 15, 5, handler);
    }

    @Override
    public int visit(PropertyAccessor node, CodeSizeHandler handler) {
        return analyze(node, node.getBase(), 10, handler);
    }

    @Override
    public int visit(PropertyAccessorValue node, CodeSizeHandler handler) {
        return analyze(node, node.getBase(), 10, handler);
    }

    @Override
    public int visit(PropertyDefinitionsMethod node, CodeSizeHandler handler) {
        // don't descend into synthetic nodes
        return 10;
    }

    @Override
    public int visit(PropertyNameDefinition node, CodeSizeHandler handler) {
        return 15;
    }

    @Override
    public int visit(PropertyValueDefinition node, CodeSizeHandler handler) {
        return analyze(node, node.getPropertyName(), node.getPropertyValue(), 5, handler);
    }

    @Override
    public int visit(RegularExpressionLiteral node, CodeSizeHandler handler) {
        return 10;
    }

    @Override
    public int visit(ReturnStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getExpression(), 10, handler);
    }

    @Override
    public int visit(Script node, CodeSizeHandler handler) {
        throw new IllegalStateException();
    }

    @Override
    public int visit(SpreadArrayLiteral node, CodeSizeHandler handler) {
        // don't descend into synthetic nodes
        return 10;
    }

    @Override
    public int visit(SpreadElement node, CodeSizeHandler handler) {
        return analyze(node, node.getExpression(), 10, handler);
    }

    @Override
    public int visit(SpreadElementMethod node, CodeSizeHandler handler) {
        // don't descend into synthetic nodes
        return 10;
    }

    @Override
    public int visit(StatementListMethod node, CodeSizeHandler handler) {
        // don't descend into synthetic nodes
        return 15;
    }

    @Override
    public int visit(StringLiteral node, CodeSizeHandler handler) {
        return stringSize(node.getValue());
    }

    @Override
    public int visit(SuperCallExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getArguments(), 25, 5, handler);
    }

    @Override
    public int visit(SuperElementAccessor node, CodeSizeHandler handler) {
        return analyze(node, node.getExpression(), 15, handler);
    }

    @Override
    public int visit(SuperElementAccessorValue node, CodeSizeHandler handler) {
        return visit((SuperElementAccessor) node, handler);
    }

    @Override
    public int visit(SuperNewExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getArguments(), 25, 5, handler);
    }

    @Override
    public int visit(SuperPropertyAccessor node, CodeSizeHandler handler) {
        return 10;
    }

    @Override
    public int visit(SuperPropertyAccessorValue node, CodeSizeHandler handler) {
        return visit((SuperPropertyAccessor) node, handler);
    }

    @Override
    public int visit(SwitchClause node, CodeSizeHandler handler) {
        return analyze(node, node.getStatements(), node.getExpression(), 15, 0, handler);
    }

    @Override
    public int visit(SwitchStatement node, CodeSizeHandler handler) {
        // TODO: Doesn't take optimized switches (int,char,string) into account.
        return analyze(node, node.getClauses(), node.getExpression(), 100, 0, handler);
    }

    @Override
    public int visit(TemplateCallExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getBase(), node.getTemplate(), 30, handler);
    }

    @Override
    public int visit(TemplateCharacters node, CodeSizeHandler handler) {
        return stringSize(node.getValue()) + stringSize(node.getRawValue());
    }

    @Override
    public int visit(TemplateLiteral node, CodeSizeHandler handler) {
        return analyze(node, node.getElements(), 25, 10, handler);
    }

    @Override
    public int visit(ThisExpression node, CodeSizeHandler handler) {
        return 10;
    }

    @Override
    public int visit(ThrowStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getExpression(), 10, handler);
    }

    @Override
    public int visit(TryStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getTryBlock(), node.getCatchNode(), node.getFinallyBlock(),
                node.getGuardedCatchNodes(), 40, 20, handler);
    }

    @Override
    public int visit(UnaryExpression node, CodeSizeHandler handler) {
        return analyze(node, node.getOperand(), 20, handler);
    }

    @Override
    public int visit(VariableDeclaration node, CodeSizeHandler handler) {
        return analyze(node, node.getBinding(), node.getInitializer(), 15, handler);
    }

    @Override
    public int visit(VariableStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getElements(), 0, 0, handler);
    }

    @Override
    public int visit(WhileStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getTest(), node.getStatement(), 25, handler);
    }

    @Override
    public int visit(WithStatement node, CodeSizeHandler handler) {
        return analyze(node, node.getExpression(), node.getStatement(), 25, handler);
    }

    @Override
    public int visit(YieldExpression node, CodeSizeHandler handler) {
        if (node.isDelegatedYield()) {
            return analyze(node, node.getExpression(), 300, handler);
        }
        return analyze(node, node.getExpression(), 150, handler);
    }
}
