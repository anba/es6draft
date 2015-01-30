/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.synthetic.*;

/**
 * Visitor interface for {@link Node} subclasses
 */
public interface VoidNodeVisitor<V> {
    void visit(ArrayAssignmentPattern node, V value);

    void visit(ArrayBindingPattern node, V value);

    void visit(ArrayComprehension node, V value);

    void visit(ArrayLiteral node, V value);

    void visit(ArrowFunction node, V value);

    void visit(AssignmentElement node, V value);

    void visit(AssignmentExpression node, V value);

    void visit(AssignmentProperty node, V value);

    void visit(AssignmentRestElement node, V value);

    void visit(AsyncArrowFunction node, V value);

    void visit(AsyncFunctionDeclaration node, V value);

    void visit(AsyncFunctionExpression node, V value);

    void visit(AwaitExpression node, V value);

    void visit(BinaryExpression node, V value);

    void visit(BindingElement node, V value);

    void visit(BindingElision node, V value);

    void visit(BindingIdentifier node, V value);

    void visit(BindingProperty node, V value);

    void visit(BindingRestElement node, V value);

    void visit(BlockStatement node, V value);

    void visit(BooleanLiteral node, V value);

    void visit(BreakStatement node, V value);

    void visit(CallExpression node, V value);

    void visit(CallSpreadElement node, V value);

    void visit(CatchNode node, V value);

    void visit(ClassDeclaration node, V value);

    void visit(ClassExpression node, V value);

    void visit(CommaExpression node, V value);

    void visit(Comprehension node, V value);

    void visit(ComprehensionFor node, V value);

    void visit(ComprehensionIf node, V value);

    void visit(ComputedPropertyName node, V value);

    void visit(ConditionalExpression node, V value);

    void visit(ContinueStatement node, V value);

    void visit(DebuggerStatement node, V value);

    void visit(DoWhileStatement node, V value);

    void visit(ElementAccessor node, V value);

    void visit(ElementAccessorValue node, V value);

    void visit(Elision node, V value);

    void visit(EmptyExpression node, V value);

    void visit(EmptyStatement node, V value);

    void visit(ExportDeclaration node, V value);

    void visit(ExportDefaultExpression node, V value);

    void visit(ExportSpecifier node, V value);

    void visit(ExportClause node, V value);

    void visit(ExpressionMethod node, V value);

    void visit(ExpressionStatement node, V value);

    void visit(ForEachStatement node, V value);

    void visit(ForInStatement node, V value);

    void visit(FormalParameterList node, V value);

    void visit(ForOfStatement node, V value);

    void visit(ForStatement node, V value);

    void visit(FunctionDeclaration node, V value);

    void visit(FunctionExpression node, V value);

    void visit(GeneratorComprehension node, V value);

    void visit(GeneratorDeclaration node, V value);

    void visit(GeneratorExpression node, V value);

    void visit(GuardedCatchNode node, V value);

    void visit(IdentifierName node, V value);

    void visit(IdentifierReference node, V value);

    void visit(IdentifierReferenceValue node, V value);

    void visit(IfStatement node, V value);

    void visit(ImportDeclaration node, V value);

    void visit(ImportSpecifier node, V value);

    void visit(ImportClause node, V value);

    void visit(SpreadArrayLiteral node, V value);

    void visit(SpreadElementMethod node, V value);

    void visit(PropertyDefinitionsMethod node, V value);

    void visit(LabelledFunctionStatement node, V value);

    void visit(LabelledStatement node, V value);

    void visit(LegacyComprehension node, V value);

    void visit(LegacyComprehensionFor node, V value);

    void visit(LegacyGeneratorDeclaration node, V value);

    void visit(LegacyGeneratorExpression node, V value);

    void visit(LetExpression node, V value);

    void visit(LetStatement node, V value);

    void visit(LexicalBinding node, V value);

    void visit(LexicalDeclaration node, V value);

    void visit(MethodDefinition node, V value);

    void visit(MethodDefinitionsMethod node, V value);

    void visit(Module node, V value);

    void visit(NativeCallExpression node, V value);

    void visit(NewExpression node, V value);

    void visit(NewTarget node, V value);

    void visit(NullLiteral node, V value);

    void visit(NumericLiteral node, V value);

    void visit(ObjectAssignmentPattern node, V value);

    void visit(ObjectBindingPattern node, V value);

    void visit(ObjectLiteral node, V value);

    void visit(PropertyAccessor node, V value);

    void visit(PropertyAccessorValue node, V value);

    void visit(PropertyNameDefinition node, V value);

    void visit(PropertyValueDefinition node, V value);

    void visit(RegularExpressionLiteral node, V value);

    void visit(ReturnStatement node, V value);

    void visit(Script node, V value);

    void visit(SpreadElement node, V value);

    void visit(StatementListMethod node, V value);

    void visit(StringLiteral node, V value);

    void visit(SuperCallExpression node, V value);

    void visit(SuperElementAccessor node, V value);

    void visit(SuperElementAccessorValue node, V value);

    void visit(SuperNewExpression node, V value);

    void visit(SuperPropertyAccessor node, V value);

    void visit(SuperPropertyAccessorValue node, V value);

    void visit(SwitchClause node, V value);

    void visit(SwitchStatement node, V value);

    void visit(TemplateCallExpression node, V value);

    void visit(TemplateCharacters node, V value);

    void visit(TemplateLiteral node, V value);

    void visit(ThisExpression node, V value);

    void visit(ThrowStatement node, V value);

    void visit(TryStatement node, V value);

    void visit(UnaryExpression node, V value);

    void visit(VariableDeclaration node, V value);

    void visit(VariableStatement node, V value);

    void visit(WhileStatement node, V value);

    void visit(WithStatement node, V value);

    void visit(YieldExpression node, V value);
}
