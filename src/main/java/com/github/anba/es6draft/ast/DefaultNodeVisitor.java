/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.synthetic.*;

/**
 * Default implementation for {@link NodeVisitor}.
 */
public abstract class DefaultNodeVisitor<R, V> implements NodeVisitor<R, V> {
    /**
     * Default visit() method to be provided by sub-classes.
     * 
     * @param node
     *            the current node
     * @param value
     *            the value
     * @return the return value
     */
    protected abstract R visit(Node node, V value);

    protected R visit(Binding node, V value) {
        return visit((Node) node, value);
    }

    protected R visit(BindingPattern node, V value) {
        return visit((Binding) node, value);
    }

    protected R visit(ComprehensionQualifier node, V value) {
        return visit((Node) node, value);
    }

    protected R visit(FormalParameter node, V value) {
        return visit((Node) node, value);
    }

    protected R visit(PropertyDefinition node, V value) {
        return visit((Node) node, value);
    }

    protected R visit(Expression node, V value) {
        return visit((Node) node, value);
    }

    protected R visit(Literal node, V value) {
        return visit((Expression) node, value);
    }

    protected R visit(ValueLiteral<?> node, V value) {
        return visit((Literal) node, value);
    }

    protected R visit(AssignmentPattern node, V value) {
        return visit((Expression) node, value);
    }

    protected R visit(ArrayInitializer node, V value) {
        return visit((Expression) node, value);
    }

    protected R visit(ModuleItem node, V value) {
        return visit((Node) node, value);
    }

    protected R visit(StatementListItem node, V value) {
        return visit((ModuleItem) node, value);
    }

    protected R visit(Declaration node, V value) {
        return visit((StatementListItem) node, value);
    }

    protected R visit(HoistableDeclaration node, V value) {
        return visit((Declaration) node, value);
    }

    protected R visit(Statement node, V value) {
        return visit((StatementListItem) node, value);
    }

    protected R visit(Program node, V value) {
        return visit((Node) node, value);
    }

    /* NodeVisitor interface */

    @Override
    public R visit(ArrayAssignmentPattern node, V value) {
        return visit((AssignmentPattern) node, value);
    }

    @Override
    public R visit(ArrayBindingPattern node, V value) {
        return visit((BindingPattern) node, value);
    }

    @Override
    public R visit(ArrayComprehension node, V value) {
        return visit((ArrayInitializer) node, value);
    }

    @Override
    public R visit(ArrayLiteral node, V value) {
        return visit((ArrayInitializer) node, value);
    }

    @Override
    public R visit(ArrowFunction node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(AssignmentElement node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(AssignmentExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(AssignmentProperty node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(AssignmentRestElement node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(AsyncArrowFunction node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(AsyncFunctionDeclaration node, V value) {
        return visit((HoistableDeclaration) node, value);
    }

    @Override
    public R visit(AsyncFunctionExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(AwaitExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(BinaryExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(BindingElement node, V value) {
        return visit((FormalParameter) node, value);
    }

    @Override
    public R visit(BindingElision node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(BindingIdentifier node, V value) {
        return visit((Binding) node, value);
    }

    @Override
    public R visit(BindingProperty node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(BindingRestElement node, V value) {
        return visit((FormalParameter) node, value);
    }

    @Override
    public R visit(BlockStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(BooleanLiteral node, V value) {
        return visit((ValueLiteral<?>) node, value);
    }

    @Override
    public R visit(BreakStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(CallExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(CallSpreadElement node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(CatchNode node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(ClassDeclaration node, V value) {
        return visit((Declaration) node, value);
    }

    @Override
    public R visit(ClassExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(CommaExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(Comprehension node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(ComprehensionFor node, V value) {
        return visit((ComprehensionQualifier) node, value);
    }

    @Override
    public R visit(ComprehensionIf node, V value) {
        return visit((ComprehensionQualifier) node, value);
    }

    @Override
    public R visit(ComputedPropertyName node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(ConditionalExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(ContinueStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(DebuggerStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(DoWhileStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(ElementAccessor node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(ElementAccessorValue node, V value) {
        return visit((ElementAccessor) node, value);
    }

    @Override
    public R visit(Elision node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(EmptyStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(ExportDeclaration node, V value) {
        return visit((ModuleItem) node, value);
    }

    @Override
    public R visit(ExportDefaultExpression node, V value) {
        return visit((Declaration) node, value);
    }

    @Override
    public R visit(ExportSpecifier node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(ExportClause node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(ExpressionMethod node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(EmptyExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(ExpressionStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(ForEachStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(ForInStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(ForOfStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(FormalParameterList node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(ForStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(FunctionDeclaration node, V value) {
        return visit((HoistableDeclaration) node, value);
    }

    @Override
    public R visit(FunctionExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(GeneratorComprehension node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(GeneratorDeclaration node, V value) {
        return visit((HoistableDeclaration) node, value);
    }

    @Override
    public R visit(GeneratorExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(GuardedCatchNode node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(IdentifierName node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(IdentifierReference node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(IdentifierReferenceValue node, V value) {
        return visit((IdentifierReference) node, value);
    }

    @Override
    public R visit(IfStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(ImportDeclaration node, V value) {
        return visit((ModuleItem) node, value);
    }

    @Override
    public R visit(ImportSpecifier node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(ImportClause node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(SpreadArrayLiteral node, V value) {
        return visit((ArrayLiteral) node, value);
    }

    @Override
    public R visit(SpreadElementMethod node, V value) {
        return visit((SpreadElement) node, value);
    }

    @Override
    public R visit(PropertyDefinitionsMethod node, V value) {
        return visit((PropertyDefinition) node, value);
    }

    @Override
    public R visit(LabelledFunctionStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(LabelledStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(LegacyComprehension node, V value) {
        return visit((Comprehension) node, value);
    }

    @Override
    public R visit(LegacyComprehensionFor node, V value) {
        return visit((ComprehensionQualifier) node, value);
    }

    @Override
    public R visit(LegacyGeneratorDeclaration node, V value) {
        return visit((GeneratorDeclaration) node, value);
    }

    @Override
    public R visit(LegacyGeneratorExpression node, V value) {
        return visit((GeneratorExpression) node, value);
    }

    @Override
    public R visit(LetExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(LetStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(LexicalBinding node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(LexicalDeclaration node, V value) {
        return visit((Declaration) node, value);
    }

    @Override
    public R visit(MethodDefinition node, V value) {
        return visit((PropertyDefinition) node, value);
    }

    @Override
    public R visit(MethodDefinitionsMethod node, V value) {
        return visit((PropertyDefinition) node, value);
    }

    @Override
    public R visit(Module node, V value) {
        return visit((Program) node, value);
    }

    @Override
    public R visit(NativeCallExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(NewExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(NewTarget node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(NullLiteral node, V value) {
        return visit((Literal) node, value);
    }

    @Override
    public R visit(NumericLiteral node, V value) {
        return visit((ValueLiteral<?>) node, value);
    }

    @Override
    public R visit(ObjectAssignmentPattern node, V value) {
        return visit((AssignmentPattern) node, value);
    }

    @Override
    public R visit(ObjectBindingPattern node, V value) {
        return visit((BindingPattern) node, value);
    }

    @Override
    public R visit(ObjectLiteral node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(PropertyAccessor node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(PropertyAccessorValue node, V value) {
        return visit((PropertyAccessor) node, value);
    }

    @Override
    public R visit(PropertyNameDefinition node, V value) {
        return visit((PropertyDefinition) node, value);
    }

    @Override
    public R visit(PropertyValueDefinition node, V value) {
        return visit((PropertyDefinition) node, value);
    }

    @Override
    public R visit(RegularExpressionLiteral node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(ReturnStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(Script node, V value) {
        return visit((Program) node, value);
    }

    @Override
    public R visit(SpreadElement node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(StatementListMethod node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(StringLiteral node, V value) {
        return visit((ValueLiteral<?>) node, value);
    }

    @Override
    public R visit(SuperCallExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(SuperElementAccessor node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(SuperElementAccessorValue node, V value) {
        return visit((SuperElementAccessor) node, value);
    }

    @Override
    public R visit(SuperNewExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(SuperPropertyAccessor node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(SuperPropertyAccessorValue node, V value) {
        return visit((SuperPropertyAccessor) node, value);
    }

    @Override
    public R visit(SwitchClause node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(SwitchStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(TemplateCallExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(TemplateCharacters node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(TemplateLiteral node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(ThisExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(ThrowStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(TryStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(UnaryExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(VariableDeclaration node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(VariableStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(WhileStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(WithStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(YieldExpression node, V value) {
        return visit((Expression) node, value);
    }
}
