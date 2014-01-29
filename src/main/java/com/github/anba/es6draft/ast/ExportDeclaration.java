/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.3 Modules</h2>
 */
public final class ExportDeclaration extends ModuleItem {
    private Type type;
    private String moduleSpecifier;
    private ExportsClause exportsClause;
    private Expression expression;
    private VariableStatement variableStatement;
    private Declaration declaration;

    public enum Type {
        All, Local, External, Default, Variable, Declaration
    }

    public ExportDeclaration(long beginPosition, long endPosition, String moduleSpecifier) {
        super(beginPosition, endPosition);
        this.type = Type.All;
        this.moduleSpecifier = moduleSpecifier;
    }

    public ExportDeclaration(long beginPosition, long endPosition, ExportsClause exportsClause) {
        super(beginPosition, endPosition);
        this.type = Type.Local;
        this.exportsClause = exportsClause;
    }

    public ExportDeclaration(long beginPosition, long endPosition, ExportsClause exportsClause,
            String moduleSpecifier) {
        super(beginPosition, endPosition);
        this.type = Type.External;
        this.exportsClause = exportsClause;
        this.moduleSpecifier = moduleSpecifier;
    }

    public ExportDeclaration(long beginPosition, long endPosition, Expression expression) {
        super(beginPosition, endPosition);
        this.type = Type.Default;
        this.expression = expression;
    }

    public ExportDeclaration(long beginPosition, long endPosition,
            VariableStatement variableStatement) {
        super(beginPosition, endPosition);
        this.type = Type.Variable;
        this.variableStatement = variableStatement;
    }

    public ExportDeclaration(long beginPosition, long endPosition, Declaration declaration) {
        super(beginPosition, endPosition);
        this.type = Type.Declaration;
        this.declaration = declaration;
    }

    public Type getType() {
        return type;
    }

    public String getModuleSpecifier() {
        return moduleSpecifier;
    }

    public ExportsClause getExportsClause() {
        return exportsClause;
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
