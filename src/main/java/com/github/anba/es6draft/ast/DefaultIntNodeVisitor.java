/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.synthetic.*;

/**
 * Default implementation for {@link IntNodeVisitor}.
 */
public abstract class DefaultIntNodeVisitor<V> implements IntNodeVisitor<V> {
    /**
     * Default visit() method to be provided by sub-classes.
     * 
     * @param node
     *            the current node
     * @param value
     *            the value
     * @return the return value
     */
    protected abstract int visit(Node node, V value);

    protected int visit(Binding node, V value) {
        return visit((Node) node, value);
    }

    protected int visit(BindingPattern node, V value) {
        return visit((Binding) node, value);
    }

    protected int visit(ComprehensionQualifier node, V value) {
        return visit((Node) node, value);
    }

    protected int visit(FormalParameter node, V value) {
        return visit((Node) node, value);
    }

    protected int visit(PropertyDefinition node, V value) {
        return visit((Node) node, value);
    }

    protected int visit(Expression node, V value) {
        return visit((Node) node, value);
    }

    protected int visit(Literal node, V value) {
        return visit((Expression) node, value);
    }

    protected int visit(ValueLiteral<?> node, V value) {
        return visit((Literal) node, value);
    }

    protected int visit(AssignmentPattern node, V value) {
        return visit((Expression) node, value);
    }

    protected int visit(ArrayInitializer node, V value) {
        return visit((Expression) node, value);
    }

    protected int visit(ModuleItem node, V value) {
        return visit((Node) node, value);
    }

    protected int visit(StatementListItem node, V value) {
        return visit((ModuleItem) node, value);
    }

    protected int visit(Declaration node, V value) {
        return visit((StatementListItem) node, value);
    }

    protected int visit(HoistableDeclaration node, V value) {
        return visit((Declaration) node, value);
    }

    protected int visit(Statement node, V value) {
        return visit((StatementListItem) node, value);
    }

    protected int visit(Program node, V value) {
        return visit((Node) node, value);
    }

    /* NodeVisitor interface */

    @Override
    public int visit(ArrayAssignmentPattern node, V value) {
        return visit((AssignmentPattern) node, value);
    }

    @Override
    public int visit(ArrayBindingPattern node, V value) {
        return visit((BindingPattern) node, value);
    }

    @Override
    public int visit(ArrayComprehension node, V value) {
        return visit((ArrayInitializer) node, value);
    }

    @Override
    public int visit(ArrayLiteral node, V value) {
        return visit((ArrayInitializer) node, value);
    }

    @Override
    public int visit(ArrowFunction node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(AssignmentElement node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(AssignmentExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(AssignmentProperty node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(AssignmentRestElement node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(AsyncArrowFunction node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(AsyncFunctionDeclaration node, V value) {
        return visit((HoistableDeclaration) node, value);
    }

    @Override
    public int visit(AsyncFunctionExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(AwaitExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(BinaryExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(BindingElement node, V value) {
        return visit((FormalParameter) node, value);
    }

    @Override
    public int visit(BindingElision node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(BindingIdentifier node, V value) {
        return visit((Binding) node, value);
    }

    @Override
    public int visit(BindingProperty node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(BindingRestElement node, V value) {
        return visit((FormalParameter) node, value);
    }

    @Override
    public int visit(BlockStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(BooleanLiteral node, V value) {
        return visit((ValueLiteral<?>) node, value);
    }

    @Override
    public int visit(BreakStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(CallExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(CallSpreadElement node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(CatchNode node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(ClassDeclaration node, V value) {
        return visit((Declaration) node, value);
    }

    @Override
    public int visit(ClassExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(CommaExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(Comprehension node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(ComprehensionFor node, V value) {
        return visit((ComprehensionQualifier) node, value);
    }

    @Override
    public int visit(ComprehensionIf node, V value) {
        return visit((ComprehensionQualifier) node, value);
    }

    @Override
    public int visit(ComputedPropertyName node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(ConditionalExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(ContinueStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(DebuggerStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(DoWhileStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(ElementAccessor node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(ElementAccessorValue node, V value) {
        return visit((ElementAccessor) node, value);
    }

    @Override
    public int visit(Elision node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(EmptyStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(ExportDeclaration node, V value) {
        return visit((ModuleItem) node, value);
    }

    @Override
    public int visit(ExportDefaultExpression node, V value) {
        return visit((Declaration) node, value);
    }

    @Override
    public int visit(ExportSpecifier node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(ExportClause node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(ExpressionMethod node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(EmptyExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(ExpressionStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(ForEachStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(ForInStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(ForOfStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(FormalParameterList node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(ForStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(FunctionDeclaration node, V value) {
        return visit((HoistableDeclaration) node, value);
    }

    @Override
    public int visit(FunctionExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(GeneratorComprehension node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(GeneratorDeclaration node, V value) {
        return visit((HoistableDeclaration) node, value);
    }

    @Override
    public int visit(GeneratorExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(GuardedCatchNode node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(IdentifierName node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(IdentifierReference node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(IdentifierReferenceValue node, V value) {
        return visit((IdentifierReference) node, value);
    }

    @Override
    public int visit(IfStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(ImportDeclaration node, V value) {
        return visit((ModuleItem) node, value);
    }

    @Override
    public int visit(ImportSpecifier node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(ImportClause node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(SpreadArrayLiteral node, V value) {
        return visit((ArrayLiteral) node, value);
    }

    @Override
    public int visit(SpreadElementMethod node, V value) {
        return visit((SpreadElement) node, value);
    }

    @Override
    public int visit(PropertyDefinitionsMethod node, V value) {
        return visit((PropertyDefinition) node, value);
    }

    @Override
    public int visit(LabelledFunctionStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(LabelledStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(LegacyComprehension node, V value) {
        return visit((Comprehension) node, value);
    }

    @Override
    public int visit(LegacyComprehensionFor node, V value) {
        return visit((ComprehensionQualifier) node, value);
    }

    @Override
    public int visit(LegacyGeneratorDeclaration node, V value) {
        return visit((GeneratorDeclaration) node, value);
    }

    @Override
    public int visit(LegacyGeneratorExpression node, V value) {
        return visit((GeneratorExpression) node, value);
    }

    @Override
    public int visit(LetExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(LetStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(LexicalBinding node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(LexicalDeclaration node, V value) {
        return visit((Declaration) node, value);
    }

    @Override
    public int visit(MethodDefinition node, V value) {
        return visit((PropertyDefinition) node, value);
    }

    @Override
    public int visit(MethodDefinitionsMethod node, V value) {
        return visit((PropertyDefinition) node, value);
    }

    @Override
    public int visit(Module node, V value) {
        return visit((Program) node, value);
    }

    @Override
    public int visit(NativeCallExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(NewExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(NewTarget node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(NullLiteral node, V value) {
        return visit((Literal) node, value);
    }

    @Override
    public int visit(NumericLiteral node, V value) {
        return visit((ValueLiteral<?>) node, value);
    }

    @Override
    public int visit(ObjectAssignmentPattern node, V value) {
        return visit((AssignmentPattern) node, value);
    }

    @Override
    public int visit(ObjectBindingPattern node, V value) {
        return visit((BindingPattern) node, value);
    }

    @Override
    public int visit(ObjectLiteral node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(PropertyAccessor node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(PropertyAccessorValue node, V value) {
        return visit((PropertyAccessor) node, value);
    }

    @Override
    public int visit(PropertyNameDefinition node, V value) {
        return visit((PropertyDefinition) node, value);
    }

    @Override
    public int visit(PropertyValueDefinition node, V value) {
        return visit((PropertyDefinition) node, value);
    }

    @Override
    public int visit(RegularExpressionLiteral node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(ReturnStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(Script node, V value) {
        return visit((Program) node, value);
    }

    @Override
    public int visit(SpreadElement node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(StatementListMethod node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(StringLiteral node, V value) {
        return visit((ValueLiteral<?>) node, value);
    }

    @Override
    public int visit(SuperCallExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(SuperElementAccessor node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(SuperElementAccessorValue node, V value) {
        return visit((SuperElementAccessor) node, value);
    }

    @Override
    public int visit(SuperNewExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(SuperPropertyAccessor node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(SuperPropertyAccessorValue node, V value) {
        return visit((SuperPropertyAccessor) node, value);
    }

    @Override
    public int visit(SwitchClause node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(SwitchStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(TemplateCallExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(TemplateCharacters node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(TemplateLiteral node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(ThisExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(ThrowStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(TryStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(UnaryExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public int visit(VariableDeclaration node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public int visit(VariableStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(WhileStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(WithStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public int visit(YieldExpression node, V value) {
        return visit((Expression) node, value);
    }
}
