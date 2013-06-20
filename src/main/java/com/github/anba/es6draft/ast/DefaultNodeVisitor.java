/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.synthetic.ElementAccessorValue;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.IdentifierValue;
import com.github.anba.es6draft.ast.synthetic.SpreadArrayLiteral;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyAccessorValue;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.ast.synthetic.SuperExpressionValue;

/**
 * Default implementation for {@link NodeVisitor}
 */
public abstract class DefaultNodeVisitor<R, V> implements NodeVisitor<R, V> {
    /**
     * Default visit() method to be provided by sub-classes
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

    protected R visit(ArrayInitialiser node, V value) {
        return visit((Expression) node, value);
    }

    protected R visit(StatementListItem node, V value) {
        return visit((Node) node, value);
    }

    protected R visit(Declaration node, V value) {
        return visit((StatementListItem) node, value);
    }

    protected R visit(Statement node, V value) {
        return visit((StatementListItem) node, value);
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
        return visit((ArrayInitialiser) node, value);
    }

    @Override
    public R visit(ArrayLiteral node, V value) {
        return visit((ArrayInitialiser) node, value);
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
        return visit((StatementListItem) node, value);
    }

    @Override
    public R visit(ExportSpecifier node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(ExportSpecifierSet node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(ExpressionMethod node, V value) {
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
        return visit((Declaration) node, value);
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
        return visit((Declaration) node, value);
    }

    @Override
    public R visit(GeneratorExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(Identifier node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(IdentifierValue node, V value) {
        return visit((Identifier) node, value);
    }

    @Override
    public R visit(IfStatement node, V value) {
        return visit((Statement) node, value);
    }

    @Override
    public R visit(ImportDeclaration node, V value) {
        return visit((StatementListItem) node, value);
    }

    @Override
    public R visit(ImportSpecifier node, V value) {
        return visit((Node) node, value);
    }

    @Override
    public R visit(ImportSpecifierSet node, V value) {
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
    public R visit(LabelledStatement node, V value) {
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
    public R visit(ModuleDeclaration node, V value) {
        return visit((StatementListItem) node, value);
    }

    @Override
    public R visit(NewExpression node, V value) {
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
        return visit((Node) node, value);
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
    public R visit(SuperExpression node, V value) {
        return visit((Expression) node, value);
    }

    @Override
    public R visit(SuperExpressionValue node, V value) {
        return visit((SuperExpression) node, value);
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
