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
    private final Type type;
    private final String moduleSpecifier;
    private final ExportsClause exportsClause;
    private final Expression expression;
    private final VariableStatement variableStatement;
    private final Declaration declaration;

    public enum Type {
        All, Local, External, DefaultDeclaration, DefaultExpression, Variable, Declaration
    }

    public ExportDeclaration(long beginPosition, long endPosition, String moduleSpecifier) {
        super(beginPosition, endPosition);
        this.type = Type.All;
        this.moduleSpecifier = moduleSpecifier;
        this.exportsClause = null;
        this.expression = null;
        this.variableStatement = null;
        this.declaration = null;
    }

    public ExportDeclaration(long beginPosition, long endPosition, ExportsClause exportsClause,
            String moduleSpecifier) {
        super(beginPosition, endPosition);
        this.type = moduleSpecifier != null ? Type.External : Type.Local;
        this.exportsClause = exportsClause;
        this.moduleSpecifier = moduleSpecifier;
        this.expression = null;
        this.variableStatement = null;
        this.declaration = null;
    }

    public ExportDeclaration(long beginPosition, long endPosition, Expression expression) {
        super(beginPosition, endPosition);
        this.type = Type.DefaultExpression;
        this.expression = expression;
        this.exportsClause = null;
        this.moduleSpecifier = null;
        this.variableStatement = null;
        this.declaration = null;
    }

    public ExportDeclaration(long beginPosition, long endPosition, HoistableDeclaration declaration) {
        super(beginPosition, endPosition);
        this.type = Type.DefaultDeclaration;
        this.declaration = declaration;
        this.exportsClause = null;
        this.moduleSpecifier = null;
        this.expression = null;
        this.variableStatement = null;
    }

    public ExportDeclaration(long beginPosition, long endPosition,
            VariableStatement variableStatement) {
        super(beginPosition, endPosition);
        this.type = Type.Variable;
        this.variableStatement = variableStatement;
        this.exportsClause = null;
        this.moduleSpecifier = null;
        this.expression = null;
        this.declaration = null;
    }

    public ExportDeclaration(long beginPosition, long endPosition, Declaration declaration) {
        super(beginPosition, endPosition);
        this.type = Type.Declaration;
        this.declaration = declaration;
        this.exportsClause = null;
        this.moduleSpecifier = null;
        this.expression = null;
        this.variableStatement = null;
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

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}
