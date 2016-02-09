/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.MethodDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadArrayLiteral;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;

/**
 * Default implementation for {@link VoidNodeVisitor}.
 */
public abstract class DefaultVoidNodeVisitor<V> implements VoidNodeVisitor<V> {
    /**
     * Default visit() method to be provided by sub-classes.
     * 
     * @param node
     *            the current node
     * @param value
     *            the value
     */
    protected abstract void visit(Node node, V value);

    protected void visit(Binding node, V value) {
        visit((Node) node, value);
    }

    protected void visit(BindingPattern node, V value) {
        visit((Binding) node, value);
    }

    protected void visit(ComprehensionQualifier node, V value) {
        visit((Node) node, value);
    }

    protected void visit(BindingElementItem node, V value) {
        visit((Node) node, value);
    }

    protected void visit(PropertyDefinition node, V value) {
        visit((Node) node, value);
    }

    protected void visit(Expression node, V value) {
        visit((Node) node, value);
    }

    protected void visit(Literal node, V value) {
        visit((Expression) node, value);
    }

    protected void visit(ValueLiteral<?> node, V value) {
        visit((Literal) node, value);
    }

    protected void visit(AssignmentPattern node, V value) {
        visit((Expression) node, value);
    }

    protected void visit(ArrayInitializer node, V value) {
        visit((Expression) node, value);
    }

    protected void visit(ModuleItem node, V value) {
        visit((Node) node, value);
    }

    protected void visit(StatementListItem node, V value) {
        visit((ModuleItem) node, value);
    }

    protected void visit(Declaration node, V value) {
        visit((StatementListItem) node, value);
    }

    protected void visit(HoistableDeclaration node, V value) {
        visit((Declaration) node, value);
    }

    protected void visit(Statement node, V value) {
        visit((StatementListItem) node, value);
    }

    protected void visit(IterationStatement node, V value) {
        visit((Statement) node, value);
    }

    protected void visit(Program node, V value) {
        visit((Node) node, value);
    }

    /* NodeVisitor interface */

    @Override
    public void visit(ArrayAssignmentPattern node, V value) {
        visit((AssignmentPattern) node, value);
    }

    @Override
    public void visit(ArrayBindingPattern node, V value) {
        visit((BindingPattern) node, value);
    }

    @Override
    public void visit(ArrayComprehension node, V value) {
        visit((ArrayInitializer) node, value);
    }

    @Override
    public void visit(ArrayLiteral node, V value) {
        visit((ArrayInitializer) node, value);
    }

    @Override
    public void visit(ArrowFunction node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(AssignmentElement node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(AssignmentExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(AssignmentProperty node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(AssignmentRestElement node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(AssignmentRestProperty node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(AsyncArrowFunction node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(AsyncFunctionDeclaration node, V value) {
        visit((HoistableDeclaration) node, value);
    }

    @Override
    public void visit(AsyncFunctionExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(AwaitExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(BinaryExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(BindingElement node, V value) {
        visit((BindingElementItem) node, value);
    }

    @Override
    public void visit(BindingElision node, V value) {
        visit((BindingElementItem) node, value);
    }

    @Override
    public void visit(BindingIdentifier node, V value) {
        visit((Binding) node, value);
    }

    @Override
    public void visit(BindingProperty node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(BindingRestElement node, V value) {
        visit((BindingElementItem) node, value);
    }

    @Override
    public void visit(BindingRestProperty node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(BlockStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(BooleanLiteral node, V value) {
        visit((ValueLiteral<?>) node, value);
    }

    @Override
    public void visit(BreakStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(CallExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(CallSpreadElement node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(CatchNode node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(ClassDeclaration node, V value) {
        visit((Declaration) node, value);
    }

    @Override
    public void visit(ClassExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(CommaExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(Comprehension node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(ComprehensionFor node, V value) {
        visit((ComprehensionQualifier) node, value);
    }

    @Override
    public void visit(ComprehensionIf node, V value) {
        visit((ComprehensionQualifier) node, value);
    }

    @Override
    public void visit(ComputedPropertyName node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(ConditionalExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(ContinueStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(DebuggerStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(DoExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(DoWhileStatement node, V value) {
        visit((IterationStatement) node, value);
    }

    @Override
    public void visit(ElementAccessor node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(Elision node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(EmptyExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(EmptyStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(ExportClause node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(ExportDeclaration node, V value) {
        visit((ModuleItem) node, value);
    }

    @Override
    public void visit(ExportDefaultExpression node, V value) {
        visit((Declaration) node, value);
    }

    @Override
    public void visit(ExportSpecifier node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(ExpressionMethod node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(ExpressionStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(ForEachStatement node, V value) {
        visit((IterationStatement) node, value);
    }

    @Override
    public void visit(ForInStatement node, V value) {
        visit((IterationStatement) node, value);
    }

    @Override
    public void visit(FormalParameter node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(FormalParameterList node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(ForOfStatement node, V value) {
        visit((IterationStatement) node, value);
    }

    @Override
    public void visit(ForStatement node, V value) {
        visit((IterationStatement) node, value);
    }

    @Override
    public void visit(FunctionDeclaration node, V value) {
        visit((HoistableDeclaration) node, value);
    }

    @Override
    public void visit(FunctionExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(FunctionSent node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(GeneratorComprehension node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(GeneratorDeclaration node, V value) {
        visit((HoistableDeclaration) node, value);
    }

    @Override
    public void visit(GeneratorExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(GuardedCatchNode node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(IdentifierName node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(IdentifierReference node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(IfStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(ImportClause node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(ImportDeclaration node, V value) {
        visit((ModuleItem) node, value);
    }

    @Override
    public void visit(ImportSpecifier node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(LabelledFunctionStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(LabelledStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(LegacyComprehension node, V value) {
        visit((Comprehension) node, value);
    }

    @Override
    public void visit(LegacyComprehensionFor node, V value) {
        visit((ComprehensionQualifier) node, value);
    }

    @Override
    public void visit(LegacyGeneratorDeclaration node, V value) {
        visit((GeneratorDeclaration) node, value);
    }

    @Override
    public void visit(LegacyGeneratorExpression node, V value) {
        visit((GeneratorExpression) node, value);
    }

    @Override
    public void visit(LetExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(LetStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(LexicalBinding node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(LexicalDeclaration node, V value) {
        visit((Declaration) node, value);
    }

    @Override
    public void visit(MethodDefinition node, V value) {
        visit((PropertyDefinition) node, value);
    }

    @Override
    public void visit(MethodDefinitionsMethod node, V value) {
        visit((PropertyDefinition) node, value);
    }

    @Override
    public void visit(Module node, V value) {
        visit((Program) node, value);
    }

    @Override
    public void visit(NativeCallExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(NewExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(NewTarget node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(NullLiteral node, V value) {
        visit((Literal) node, value);
    }

    @Override
    public void visit(NumericLiteral node, V value) {
        visit((ValueLiteral<?>) node, value);
    }

    @Override
    public void visit(ObjectAssignmentPattern node, V value) {
        visit((AssignmentPattern) node, value);
    }

    @Override
    public void visit(ObjectBindingPattern node, V value) {
        visit((BindingPattern) node, value);
    }

    @Override
    public void visit(ObjectLiteral node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(PropertyAccessor node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(PropertyDefinitionsMethod node, V value) {
        visit((PropertyDefinition) node, value);
    }

    @Override
    public void visit(PropertyNameDefinition node, V value) {
        visit((PropertyDefinition) node, value);
    }

    @Override
    public void visit(PropertyValueDefinition node, V value) {
        visit((PropertyDefinition) node, value);
    }

    @Override
    public void visit(RegularExpressionLiteral node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(ReturnStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(Script node, V value) {
        visit((Program) node, value);
    }

    @Override
    public void visit(SpreadArrayLiteral node, V value) {
        visit((ArrayLiteral) node, value);
    }

    @Override
    public void visit(SpreadElement node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(SpreadElementMethod node, V value) {
        visit((SpreadElement) node, value);
    }

    @Override
    public void visit(SpreadProperty node, V value) {
        visit((PropertyDefinition) node, value);
    }

    @Override
    public void visit(StatementListMethod node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(StringLiteral node, V value) {
        visit((ValueLiteral<?>) node, value);
    }

    @Override
    public void visit(SuperCallExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(SuperElementAccessor node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(SuperNewExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(SuperPropertyAccessor node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(SwitchClause node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(SwitchStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(TemplateCallExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(TemplateCharacters node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(TemplateLiteral node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(ThisExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(ThrowStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(TryStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(UnaryExpression node, V value) {
        visit((Expression) node, value);
    }

    @Override
    public void visit(VariableDeclaration node, V value) {
        visit((Node) node, value);
    }

    @Override
    public void visit(VariableStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(WhileStatement node, V value) {
        visit((IterationStatement) node, value);
    }

    @Override
    public void visit(WithStatement node, V value) {
        visit((Statement) node, value);
    }

    @Override
    public void visit(YieldExpression node, V value) {
        visit((Expression) node, value);
    }
}
