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
public interface NodeVisitor<R, V> {
    R visit(ArrayAssignmentPattern node, V value);

    R visit(ArrayBindingPattern node, V value);

    R visit(ArrayComprehension node, V value);

    R visit(ArrayLiteral node, V value);

    R visit(ArrowFunction node, V value);

    R visit(AssignmentElement node, V value);

    R visit(AssignmentExpression node, V value);

    R visit(AssignmentProperty node, V value);

    R visit(AssignmentRestElement node, V value);

    R visit(AssignmentRestProperty node, V value);

    R visit(AsyncArrowFunction node, V value);

    R visit(AsyncFunctionDeclaration node, V value);

    R visit(AsyncFunctionExpression node, V value);

    R visit(AwaitExpression node, V value);

    R visit(BinaryExpression node, V value);

    R visit(BindingElement node, V value);

    R visit(BindingElision node, V value);

    R visit(BindingIdentifier node, V value);

    R visit(BindingProperty node, V value);

    R visit(BindingRestElement node, V value);

    R visit(BindingRestProperty node, V value);

    R visit(BlockStatement node, V value);

    R visit(BooleanLiteral node, V value);

    R visit(BreakStatement node, V value);

    R visit(CallExpression node, V value);

    R visit(CallSpreadElement node, V value);

    R visit(CatchNode node, V value);

    R visit(ClassDeclaration node, V value);

    R visit(ClassExpression node, V value);

    R visit(CommaExpression node, V value);

    R visit(Comprehension node, V value);

    R visit(ComprehensionFor node, V value);

    R visit(ComprehensionIf node, V value);

    R visit(ComputedPropertyName node, V value);

    R visit(ConditionalExpression node, V value);

    R visit(ContinueStatement node, V value);

    R visit(DebuggerStatement node, V value);

    R visit(DoWhileStatement node, V value);

    R visit(ElementAccessor node, V value);

    R visit(ElementAccessorValue node, V value);

    R visit(Elision node, V value);

    R visit(EmptyExpression node, V value);

    R visit(EmptyStatement node, V value);

    R visit(ExportDeclaration node, V value);

    R visit(ExportDefaultExpression node, V value);

    R visit(ExportSpecifier node, V value);

    R visit(ExportClause node, V value);

    R visit(ExpressionMethod node, V value);

    R visit(ExpressionStatement node, V value);

    R visit(ForEachStatement node, V value);

    R visit(ForInStatement node, V value);

    R visit(FormalParameterList node, V value);

    R visit(ForOfStatement node, V value);

    R visit(ForStatement node, V value);

    R visit(FunctionDeclaration node, V value);

    R visit(FunctionExpression node, V value);

    R visit(GeneratorComprehension node, V value);

    R visit(GeneratorDeclaration node, V value);

    R visit(GeneratorExpression node, V value);

    R visit(GuardedCatchNode node, V value);

    R visit(IdentifierName node, V value);

    R visit(IdentifierReference node, V value);

    R visit(IdentifierReferenceValue node, V value);

    R visit(IfStatement node, V value);

    R visit(ImportDeclaration node, V value);

    R visit(ImportSpecifier node, V value);

    R visit(ImportClause node, V value);

    R visit(SpreadArrayLiteral node, V value);

    R visit(SpreadElementMethod node, V value);

    R visit(PropertyDefinitionsMethod node, V value);

    R visit(LabelledFunctionStatement node, V value);

    R visit(LabelledStatement node, V value);

    R visit(LegacyComprehension node, V value);

    R visit(LegacyComprehensionFor node, V value);

    R visit(LegacyGeneratorDeclaration node, V value);

    R visit(LegacyGeneratorExpression node, V value);

    R visit(LetExpression node, V value);

    R visit(LetStatement node, V value);

    R visit(LexicalBinding node, V value);

    R visit(LexicalDeclaration node, V value);

    R visit(MethodDefinition node, V value);

    R visit(MethodDefinitionsMethod node, V value);

    R visit(Module node, V value);

    R visit(NativeCallExpression node, V value);

    R visit(NewExpression node, V value);

    R visit(NewTarget node, V value);

    R visit(NullLiteral node, V value);

    R visit(NumericLiteral node, V value);

    R visit(ObjectAssignmentPattern node, V value);

    R visit(ObjectBindingPattern node, V value);

    R visit(ObjectLiteral node, V value);

    R visit(PropertyAccessor node, V value);

    R visit(PropertyAccessorValue node, V value);

    R visit(PropertyNameDefinition node, V value);

    R visit(PropertyValueDefinition node, V value);

    R visit(RegularExpressionLiteral node, V value);

    R visit(ReturnStatement node, V value);

    R visit(Script node, V value);

    R visit(SpreadElement node, V value);

    R visit(SpreadProperty node, V value);

    R visit(StatementListMethod node, V value);

    R visit(StringLiteral node, V value);

    R visit(SuperCallExpression node, V value);

    R visit(SuperElementAccessor node, V value);

    R visit(SuperElementAccessorValue node, V value);

    R visit(SuperNewExpression node, V value);

    R visit(SuperPropertyAccessor node, V value);

    R visit(SuperPropertyAccessorValue node, V value);

    R visit(SwitchClause node, V value);

    R visit(SwitchStatement node, V value);

    R visit(TemplateCallExpression node, V value);

    R visit(TemplateCharacters node, V value);

    R visit(TemplateLiteral node, V value);

    R visit(ThisExpression node, V value);

    R visit(ThrowStatement node, V value);

    R visit(TryStatement node, V value);

    R visit(UnaryExpression node, V value);

    R visit(VariableDeclaration node, V value);

    R visit(VariableStatement node, V value);

    R visit(WhileStatement node, V value);

    R visit(WithStatement node, V value);

    R visit(YieldExpression node, V value);
}
