/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.synthetic.ClassFieldInitializer;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.MethodDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;

/**
 * Visitor interface for {@link Node} subclasses
 */
public interface IntNodeVisitor<V> {
    int visit(ArrayAssignmentPattern node, V value);

    int visit(ArrayBindingPattern node, V value);

    int visit(ArrayComprehension node, V value);

    int visit(ArrayLiteral node, V value);

    int visit(ArrowFunction node, V value);

    int visit(AssignmentElement node, V value);

    int visit(AssignmentExpression node, V value);

    int visit(AssignmentProperty node, V value);

    int visit(AssignmentRestElement node, V value);

    int visit(AssignmentRestProperty node, V value);

    int visit(AsyncArrowFunction node, V value);

    int visit(AsyncFunctionDeclaration node, V value);

    int visit(AsyncFunctionExpression node, V value);

    int visit(AsyncGeneratorDeclaration node, V value);

    int visit(AsyncGeneratorExpression node, V value);

    int visit(AwaitExpression node, V value);

    int visit(BigIntegerLiteral node, V value);

    int visit(BinaryExpression node, V value);

    int visit(BindingElement node, V value);

    int visit(BindingElision node, V value);

    int visit(BindingIdentifier node, V value);

    int visit(BindingProperty node, V value);

    int visit(BindingRestElement node, V value);

    int visit(BindingRestProperty node, V value);

    int visit(BlockStatement node, V value);

    int visit(BooleanLiteral node, V value);

    int visit(BreakStatement node, V value);

    int visit(CallExpression node, V value);

    int visit(CallSpreadElement node, V value);

    int visit(CatchNode node, V value);

    int visit(ClassDeclaration node, V value);

    int visit(ClassExpression node, V value);

    int visit(ClassFieldDefinition node, V value);

    int visit(ClassFieldInitializer node, V value);

    int visit(CommaExpression node, V value);

    int visit(Comprehension node, V value);

    int visit(ComprehensionFor node, V value);

    int visit(ComprehensionIf node, V value);

    int visit(ComputedPropertyName node, V value);

    int visit(ConditionalExpression node, V value);

    int visit(ContinueStatement node, V value);

    int visit(DebuggerStatement node, V value);

    int visit(DoExpression node, V value);

    int visit(DoWhileStatement node, V value);

    int visit(ElementAccessor node, V value);

    int visit(Elision node, V value);

    int visit(EmptyExpression node, V value);

    int visit(EmptyStatement node, V value);

    int visit(ExportClause node, V value);

    int visit(ExportDeclaration node, V value);

    int visit(ExportDefaultExpression node, V value);

    int visit(ExportSpecifier node, V value);

    int visit(ExpressionMethod node, V value);

    int visit(ExpressionStatement node, V value);

    int visit(ForAwaitStatement node, V value);

    int visit(ForInStatement node, V value);

    int visit(FormalParameter node, V value);

    int visit(FormalParameterList node, V value);

    int visit(ForOfStatement node, V value);

    int visit(ForStatement node, V value);

    int visit(FunctionDeclaration node, V value);

    int visit(FunctionExpression node, V value);

    int visit(FunctionSent node, V value);

    int visit(GeneratorComprehension node, V value);

    int visit(GeneratorDeclaration node, V value);

    int visit(GeneratorExpression node, V value);

    int visit(IdentifierName node, V value);

    int visit(IdentifierReference node, V value);

    int visit(IfStatement node, V value);

    int visit(ImportCallExpression node, V value);

    int visit(ImportClause node, V value);

    int visit(ImportDeclaration node, V value);

    int visit(ImportMeta node, V value);

    int visit(ImportSpecifier node, V value);

    int visit(LabelledFunctionStatement node, V value);

    int visit(LabelledStatement node, V value);

    int visit(LexicalBinding node, V value);

    int visit(LexicalDeclaration node, V value);

    int visit(MethodDefinition node, V value);

    int visit(MethodDefinitionsMethod node, V value);

    int visit(Module node, V value);

    int visit(NativeCallExpression node, V value);

    int visit(NewExpression node, V value);

    int visit(NewTarget node, V value);

    int visit(NullLiteral node, V value);

    int visit(NumericLiteral node, V value);

    int visit(ObjectAssignmentPattern node, V value);

    int visit(ObjectBindingPattern node, V value);

    int visit(ObjectLiteral node, V value);

    int visit(PrivateNameProperty node, V value);

    int visit(PrivatePropertyAccessor node, V value);

    int visit(PropertyAccessor node, V value);

    int visit(PropertyDefinitionsMethod node, V value);

    int visit(PropertyNameDefinition node, V value);

    int visit(PropertyValueDefinition node, V value);

    int visit(RegularExpressionLiteral node, V value);

    int visit(ReturnStatement node, V value);

    int visit(Script node, V value);

    int visit(SpreadElement node, V value);

    int visit(SpreadElementMethod node, V value);

    int visit(SpreadProperty node, V value);

    int visit(StatementListMethod node, V value);

    int visit(StringLiteral node, V value);

    int visit(SuperCallExpression node, V value);

    int visit(SuperElementAccessor node, V value);

    int visit(SuperNewExpression node, V value);

    int visit(SuperPropertyAccessor node, V value);

    int visit(SwitchClause node, V value);

    int visit(SwitchStatement node, V value);

    int visit(TemplateCallExpression node, V value);

    int visit(TemplateCharacters node, V value);

    int visit(TemplateLiteral node, V value);

    int visit(ThisExpression node, V value);

    int visit(ThrowExpression node, V value);

    int visit(ThrowStatement node, V value);

    int visit(TryStatement node, V value);

    int visit(UnaryExpression node, V value);

    int visit(UpdateExpression node, V value);

    int visit(VariableDeclaration node, V value);

    int visit(VariableStatement node, V value);

    int visit(WhileStatement node, V value);

    int visit(WithStatement node, V value);

    int visit(YieldExpression node, V value);
}
