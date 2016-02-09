/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.function.Consumer;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.MethodDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadArrayLiteral;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;

/**
 * 
 */
final class FindYieldOrAwait implements VoidNodeVisitor<Consumer<Expression>> {
    private <NODE extends Node> void acceptIfPresent(NODE node, Consumer<Expression> value) {
        if (node != null) {
            node.accept(this, value);
        }
    }

    @Override
    public void visit(ArrayAssignmentPattern node, Consumer<Expression> value) {
        node.getElements().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(ArrayBindingPattern node, Consumer<Expression> value) {
        node.getElements().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(ArrayComprehension node, Consumer<Expression> value) {
        node.getComprehension().accept(this, value);
    }

    @Override
    public void visit(ArrayLiteral node, Consumer<Expression> value) {
        node.getElements().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(ArrowFunction node, Consumer<Expression> value) {
        // Don't visit nested function nodes.
    }

    @Override
    public void visit(AssignmentElement node, Consumer<Expression> value) {
        node.getTarget().accept(this, value);
        acceptIfPresent(node.getInitializer(), value);
    }

    @Override
    public void visit(AssignmentExpression node, Consumer<Expression> value) {
        node.getLeft().accept(this, value);
        node.getRight().accept(this, value);
    }

    @Override
    public void visit(AssignmentProperty node, Consumer<Expression> value) {
        acceptIfPresent(node.getPropertyName(), value);
        node.getTarget().accept(this, value);
        acceptIfPresent(node.getInitializer(), value);
    }

    @Override
    public void visit(AssignmentRestElement node, Consumer<Expression> value) {
        node.getTarget().accept(this, value);
    }

    @Override
    public void visit(AssignmentRestProperty node, Consumer<Expression> value) {
        node.getTarget().accept(this, value);
    }

    @Override
    public void visit(AsyncArrowFunction node, Consumer<Expression> value) {
        // Don't visit nested function nodes.
    }

    @Override
    public void visit(AsyncFunctionDeclaration node, Consumer<Expression> value) {
        // Don't visit nested function nodes.
    }

    @Override
    public void visit(AsyncFunctionExpression node, Consumer<Expression> value) {
        // Don't visit nested function nodes.
    }

    @Override
    public void visit(AwaitExpression node, Consumer<Expression> value) {
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(BinaryExpression node, Consumer<Expression> value) {
        node.getLeft().accept(this, value);
        node.getRight().accept(this, value);
    }

    @Override
    public void visit(BindingElement node, Consumer<Expression> value) {
        node.getBinding().accept(this, value);
        acceptIfPresent(node.getInitializer(), value);
    }

    @Override
    public void visit(BindingElision node, Consumer<Expression> value) {
    }

    @Override
    public void visit(BindingIdentifier node, Consumer<Expression> value) {
    }

    @Override
    public void visit(BindingProperty node, Consumer<Expression> value) {
        acceptIfPresent(node.getPropertyName(), value);
        node.getBinding().accept(this, value);
        acceptIfPresent(node.getInitializer(), value);
    }

    @Override
    public void visit(BindingRestElement node, Consumer<Expression> value) {
        node.getBinding().accept(this, value);
    }

    @Override
    public void visit(BindingRestProperty node, Consumer<Expression> value) {
        node.getBindingIdentifier().accept(this, value);
    }

    @Override
    public void visit(BlockStatement node, Consumer<Expression> value) {
        node.getStatements().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(BooleanLiteral node, Consumer<Expression> value) {
    }

    @Override
    public void visit(BreakStatement node, Consumer<Expression> value) {
    }

    @Override
    public void visit(CallExpression node, Consumer<Expression> value) {
        node.getBase().accept(this, value);
        node.getArguments().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(CallSpreadElement node, Consumer<Expression> value) {
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(CatchNode node, Consumer<Expression> value) {
        node.getCatchParameter().accept(this, value);
        node.getCatchBlock().accept(this, value);
    }

    @Override
    public void visit(ClassDeclaration node, Consumer<Expression> value) {
        acceptIfPresent(node.getHeritage(), value);
        node.getDecorators().forEach(v -> v.accept(this, value));
        node.getProperties().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(ClassExpression node, Consumer<Expression> value) {
        acceptIfPresent(node.getHeritage(), value);
        node.getDecorators().forEach(v -> v.accept(this, value));
        node.getProperties().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(CommaExpression node, Consumer<Expression> value) {
        node.getOperands().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(Comprehension node, Consumer<Expression> value) {
        node.getList().forEach(v -> v.accept(this, value));
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(ComprehensionFor node, Consumer<Expression> value) {
        node.getBinding().accept(this, value);
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(ComprehensionIf node, Consumer<Expression> value) {
        node.getTest().accept(this, value);
    }

    @Override
    public void visit(ComputedPropertyName node, Consumer<Expression> value) {
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(ConditionalExpression node, Consumer<Expression> value) {
        node.getTest().accept(this, value);
        node.getThen().accept(this, value);
        node.getOtherwise().accept(this, value);
    }

    @Override
    public void visit(ContinueStatement node, Consumer<Expression> value) {
    }

    @Override
    public void visit(DebuggerStatement node, Consumer<Expression> value) {
    }

    @Override
    public void visit(DoExpression node, Consumer<Expression> value) {
        node.getStatement().accept(this, value);
    }

    @Override
    public void visit(DoWhileStatement node, Consumer<Expression> value) {
        node.getTest().accept(this, value);
        node.getStatement().accept(this, value);
    }

    @Override
    public void visit(ElementAccessor node, Consumer<Expression> value) {
        node.getBase().accept(this, value);
        node.getElement().accept(this, value);
    }

    @Override
    public void visit(Elision node, Consumer<Expression> value) {
    }

    @Override
    public void visit(EmptyExpression node, Consumer<Expression> value) {
    }

    @Override
    public void visit(EmptyStatement node, Consumer<Expression> value) {
    }

    @Override
    public void visit(ExportClause node, Consumer<Expression> value) {
        throw new IllegalStateException();
    }

    @Override
    public void visit(ExportDeclaration node, Consumer<Expression> value) {
        throw new IllegalStateException();
    }

    @Override
    public void visit(ExportDefaultExpression node, Consumer<Expression> value) {
        throw new IllegalStateException();
    }

    @Override
    public void visit(ExportSpecifier node, Consumer<Expression> value) {
        throw new IllegalStateException();
    }

    @Override
    public void visit(ExpressionMethod node, Consumer<Expression> value) {
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(ExpressionStatement node, Consumer<Expression> value) {
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(ForEachStatement node, Consumer<Expression> value) {
        node.getHead().accept(this, value);
        node.getExpression().accept(this, value);
        node.getStatement().accept(this, value);
    }

    @Override
    public void visit(ForInStatement node, Consumer<Expression> value) {
        node.getHead().accept(this, value);
        node.getExpression().accept(this, value);
        node.getStatement().accept(this, value);
    }

    @Override
    public void visit(FormalParameter node, Consumer<Expression> value) {
        throw new IllegalStateException();
    }

    @Override
    public void visit(FormalParameterList node, Consumer<Expression> value) {
        throw new IllegalStateException();
    }

    @Override
    public void visit(ForOfStatement node, Consumer<Expression> value) {
        node.getHead().accept(this, value);
        node.getExpression().accept(this, value);
        node.getStatement().accept(this, value);
    }

    @Override
    public void visit(ForStatement node, Consumer<Expression> value) {
        acceptIfPresent(node.getHead(), value);
        acceptIfPresent(node.getTest(), value);
        acceptIfPresent(node.getStep(), value);
        node.getStatement().accept(this, value);
    }

    @Override
    public void visit(FunctionDeclaration node, Consumer<Expression> value) {
        // Don't visit nested function nodes.
    }

    @Override
    public void visit(FunctionExpression node, Consumer<Expression> value) {
        // Don't visit nested function nodes.
    }

    @Override
    public void visit(FunctionSent node, Consumer<Expression> value) {
    }

    @Override
    public void visit(GeneratorComprehension node, Consumer<Expression> value) {
        // Don't visit nested function nodes.
    }

    @Override
    public void visit(GeneratorDeclaration node, Consumer<Expression> value) {
        // Don't visit nested function nodes.
    }

    @Override
    public void visit(GeneratorExpression node, Consumer<Expression> value) {
        // Don't visit nested function nodes.
    }

    @Override
    public void visit(GuardedCatchNode node, Consumer<Expression> value) {
        node.getCatchParameter().accept(this, value);
        node.getGuard().accept(this, value);
        node.getCatchBlock().accept(this, value);
    }

    @Override
    public void visit(IdentifierName node, Consumer<Expression> value) {
    }

    @Override
    public void visit(IdentifierReference node, Consumer<Expression> value) {
    }

    @Override
    public void visit(IfStatement node, Consumer<Expression> value) {
        node.getTest().accept(this, value);
        node.getThen().accept(this, value);
        acceptIfPresent(node.getOtherwise(), value);
    }

    @Override
    public void visit(ImportClause node, Consumer<Expression> value) {
        throw new IllegalStateException();
    }

    @Override
    public void visit(ImportDeclaration node, Consumer<Expression> value) {
        throw new IllegalStateException();
    }

    @Override
    public void visit(ImportSpecifier node, Consumer<Expression> value) {
        throw new IllegalStateException();
    }

    @Override
    public void visit(LabelledFunctionStatement node, Consumer<Expression> value) {
        node.getFunction().accept(this, value);
    }

    @Override
    public void visit(LabelledStatement node, Consumer<Expression> value) {
        node.getStatement().accept(this, value);
    }

    @Override
    public void visit(LegacyComprehension node, Consumer<Expression> value) {
        node.getList().forEach(v -> v.accept(this, value));
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(LegacyComprehensionFor node, Consumer<Expression> value) {
        node.getBinding().accept(this, value);
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(LegacyGeneratorDeclaration node, Consumer<Expression> value) {
        // Don't visit nested function nodes.
    }

    @Override
    public void visit(LegacyGeneratorExpression node, Consumer<Expression> value) {
        // Don't visit nested function nodes.
    }

    @Override
    public void visit(LetExpression node, Consumer<Expression> value) {
        node.getBindings().forEach(v -> v.accept(this, value));
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(LetStatement node, Consumer<Expression> value) {
        node.getBindings().forEach(v -> v.accept(this, value));
        node.getStatement().accept(this, value);
    }

    @Override
    public void visit(LexicalBinding node, Consumer<Expression> value) {
        node.getBinding().accept(this, value);
        acceptIfPresent(node.getInitializer(), value);
    }

    @Override
    public void visit(LexicalDeclaration node, Consumer<Expression> value) {
        node.getElements().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(MethodDefinition node, Consumer<Expression> value) {
        node.getPropertyName().accept(this, value);
        node.getDecorators().forEach(v -> v.accept(this, value));
        // Don't visit nested function nodes.
    }

    @Override
    public void visit(MethodDefinitionsMethod node, Consumer<Expression> value) {
        node.getProperties().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(Module node, Consumer<Expression> value) {
        throw new IllegalStateException();
    }

    @Override
    public void visit(NativeCallExpression node, Consumer<Expression> value) {
        node.getBase().accept(this, value);
        node.getArguments().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(NewExpression node, Consumer<Expression> value) {
        node.getExpression().accept(this, value);
        node.getArguments().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(NewTarget node, Consumer<Expression> value) {
    }

    @Override
    public void visit(NullLiteral node, Consumer<Expression> value) {
    }

    @Override
    public void visit(NumericLiteral node, Consumer<Expression> value) {
    }

    @Override
    public void visit(ObjectAssignmentPattern node, Consumer<Expression> value) {
        node.getProperties().forEach(v -> v.accept(this, value));
        acceptIfPresent(node.getRest(), value);
    }

    @Override
    public void visit(ObjectBindingPattern node, Consumer<Expression> value) {
        node.getProperties().forEach(v -> v.accept(this, value));
        acceptIfPresent(node.getRest(), value);
    }

    @Override
    public void visit(ObjectLiteral node, Consumer<Expression> value) {
        node.getProperties().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(PropertyAccessor node, Consumer<Expression> value) {
        node.getBase().accept(this, value);
    }

    @Override
    public void visit(PropertyDefinitionsMethod node, Consumer<Expression> value) {
        node.getProperties().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(PropertyNameDefinition node, Consumer<Expression> value) {
        node.getPropertyName().accept(this, value);
    }

    @Override
    public void visit(PropertyValueDefinition node, Consumer<Expression> value) {
        node.getPropertyName().accept(this, value);
        node.getPropertyValue().accept(this, value);
    }

    @Override
    public void visit(RegularExpressionLiteral node, Consumer<Expression> value) {
    }

    @Override
    public void visit(ReturnStatement node, Consumer<Expression> value) {
        acceptIfPresent(node.getExpression(), value);
    }

    @Override
    public void visit(Script node, Consumer<Expression> value) {
        throw new IllegalStateException();
    }

    @Override
    public void visit(SpreadArrayLiteral node, Consumer<Expression> value) {
        node.getElements().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(SpreadElement node, Consumer<Expression> value) {
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(SpreadElementMethod node, Consumer<Expression> value) {
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(SpreadProperty node, Consumer<Expression> value) {
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(StatementListMethod node, Consumer<Expression> value) {
        node.getStatements().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(StringLiteral node, Consumer<Expression> value) {
    }

    @Override
    public void visit(SuperCallExpression node, Consumer<Expression> value) {
        node.getArguments().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(SuperElementAccessor node, Consumer<Expression> value) {
        node.getElement().accept(this, value);
    }

    @Override
    public void visit(SuperNewExpression node, Consumer<Expression> value) {
        node.getArguments().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(SuperPropertyAccessor node, Consumer<Expression> value) {
    }

    @Override
    public void visit(SwitchClause node, Consumer<Expression> value) {
        acceptIfPresent(node.getExpression(), value);
        node.getStatements().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(SwitchStatement node, Consumer<Expression> value) {
        node.getExpression().accept(this, value);
        node.getClauses().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(TemplateCallExpression node, Consumer<Expression> value) {
        node.getBase().accept(this, value);
        node.getTemplate().accept(this, value);
    }

    @Override
    public void visit(TemplateCharacters node, Consumer<Expression> value) {
    }

    @Override
    public void visit(TemplateLiteral node, Consumer<Expression> value) {
        node.getElements().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(ThisExpression node, Consumer<Expression> value) {
    }

    @Override
    public void visit(ThrowStatement node, Consumer<Expression> value) {
        node.getExpression().accept(this, value);
    }

    @Override
    public void visit(TryStatement node, Consumer<Expression> value) {
        node.getTryBlock().accept(this, value);
        acceptIfPresent(node.getCatchNode(), value);
        node.getGuardedCatchNodes().forEach(v -> v.accept(this, value));
        acceptIfPresent(node.getFinallyBlock(), value);
    }

    @Override
    public void visit(UnaryExpression node, Consumer<Expression> value) {
        node.getOperand().accept(this, value);
    }

    @Override
    public void visit(VariableDeclaration node, Consumer<Expression> value) {
        node.getBinding().accept(this, value);
        acceptIfPresent(node.getInitializer(), value);
    }

    @Override
    public void visit(VariableStatement node, Consumer<Expression> value) {
        node.getElements().forEach(v -> v.accept(this, value));
    }

    @Override
    public void visit(WhileStatement node, Consumer<Expression> value) {
        node.getTest().accept(this, value);
        node.getStatement().accept(this, value);
    }

    @Override
    public void visit(WithStatement node, Consumer<Expression> value) {
        node.getExpression().accept(this, value);
        node.getStatement().accept(this, value);
    }

    @Override
    public void visit(YieldExpression node, Consumer<Expression> value) {
        value.accept(node);
        acceptIfPresent(node.getExpression(), value);
    }
}
