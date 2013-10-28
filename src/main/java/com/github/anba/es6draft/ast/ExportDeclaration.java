/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.3 Modules</h2>
 */
public class ExportDeclaration extends StatementListItem {
    private ExportSpecifierSet exportSpecifierSet;
    private Expression expression;
    private VariableStatement variableStatement;
    private Declaration declaration;

    public ExportDeclaration(long beginPosition, long endPosition,
            ExportSpecifierSet exportSpecifierSet) {
        super(beginPosition, endPosition);
        this.exportSpecifierSet = exportSpecifierSet;
    }

    public ExportDeclaration(long beginPosition, long endPosition, Expression expression) {
        super(beginPosition, endPosition);
        this.expression = expression;
    }

    public ExportDeclaration(long beginPosition, long endPosition,
            VariableStatement variableStatement) {
        super(beginPosition, endPosition);
        this.variableStatement = variableStatement;
    }

    public ExportDeclaration(long beginPosition, long endPosition, Declaration declaration) {
        super(beginPosition, endPosition);
        this.declaration = declaration;
    }

    public ExportSpecifierSet getExportSpecifierSet() {
        return exportSpecifierSet;
    }

    public Expression getExpression() {
        return expression;
    }

    public VariableStatement getVariableStatement() {
        return variableStatement;
    }

    public Declaration getDeclaration() {
        return declaration;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
