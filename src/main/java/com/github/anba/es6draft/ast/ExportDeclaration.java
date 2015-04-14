/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2>
 */
public final class ExportDeclaration extends ModuleItem {
    private final Type type;
    private final String moduleSpecifier;
    private final ExportClause exportClause;
    private final ExportDefaultExpression expression;
    private final VariableStatement variableStatement;
    private final Declaration declaration;

    public enum Type {
        All, External, Local, Variable, Declaration, DefaultHoistableDeclaration,
        DefaultClassDeclaration, DefaultExpression
    }

    /**
     * <pre>
     * ExportDeclaration :
     *     export * FromClause ;
     * </pre>
     * 
     * @param beginPosition
     *            the source begin position
     * @param endPosition
     *            the source end position
     * @param moduleSpecifier
     *            the requested module name
     */
    public ExportDeclaration(long beginPosition, long endPosition, String moduleSpecifier) {
        super(beginPosition, endPosition);
        this.type = Type.All;
        this.moduleSpecifier = moduleSpecifier;
        this.exportClause = null;
        this.expression = null;
        this.variableStatement = null;
        this.declaration = null;
    }

    /**
     * <pre>
     * ExportDeclaration :
     *     export ExportClause FromClause ;
     *     export ExportClause ;
     * </pre>
     * 
     * @param beginPosition
     *            the source begin position
     * @param endPosition
     *            the source end position
     * @param exportClause
     *            the export clause
     * @param moduleSpecifier
     *            the requested module name or {@code null}
     */
    public ExportDeclaration(long beginPosition, long endPosition, ExportClause exportClause,
            String moduleSpecifier) {
        super(beginPosition, endPosition);
        this.type = moduleSpecifier != null ? Type.External : Type.Local;
        this.moduleSpecifier = moduleSpecifier;
        this.exportClause = exportClause;
        this.expression = null;
        this.variableStatement = null;
        this.declaration = null;
    }

    /**
     * <pre>
     * ExportDeclaration :
     *     export VariableStatement
     * </pre>
     * 
     * @param beginPosition
     *            the source begin position
     * @param endPosition
     *            the source end position
     * @param variableStatement
     *            the variable statement node
     */
    public ExportDeclaration(long beginPosition, long endPosition,
            VariableStatement variableStatement) {
        super(beginPosition, endPosition);
        this.type = Type.Variable;
        this.moduleSpecifier = null;
        this.exportClause = null;
        this.expression = null;
        this.variableStatement = variableStatement;
        this.declaration = null;
    }

    /**
     * <pre>
     * ExportDeclaration :
     *     export Declaration
     * </pre>
     * 
     * @param beginPosition
     *            the source begin position
     * @param endPosition
     *            the source end position
     * @param declaration
     *            the declaration node
     */
    public ExportDeclaration(long beginPosition, long endPosition, Declaration declaration) {
        super(beginPosition, endPosition);
        this.type = Type.Declaration;
        this.moduleSpecifier = null;
        this.exportClause = null;
        this.expression = null;
        this.variableStatement = null;
        this.declaration = declaration;
    }

    /**
     * <pre>
     * ExportDeclaration :
     *     export default HoistableDeclaration<span><sub>[Default]</sub></span>
     * </pre>
     * 
     * @param beginPosition
     *            the source begin position
     * @param endPosition
     *            the source end position
     * @param declaration
     *            the hoistable declaration node
     */
    public ExportDeclaration(long beginPosition, long endPosition, HoistableDeclaration declaration) {
        super(beginPosition, endPosition);
        this.type = Type.DefaultHoistableDeclaration;
        this.moduleSpecifier = null;
        this.exportClause = null;
        this.expression = null;
        this.variableStatement = null;
        this.declaration = declaration;
    }

    /**
     * <pre>
     * ExportDeclaration :
     *     export default ClassDeclaration<span><sub>[Default]</sub></span>
     * </pre>
     * 
     * @param beginPosition
     *            the source begin position
     * @param endPosition
     *            the source end position
     * @param declaration
     *            the class declaration node
     */
    public ExportDeclaration(long beginPosition, long endPosition, ClassDeclaration declaration) {
        super(beginPosition, endPosition);
        this.type = Type.DefaultClassDeclaration;
        this.moduleSpecifier = null;
        this.exportClause = null;
        this.expression = null;
        this.variableStatement = null;
        this.declaration = declaration;
    }

    /**
     * <pre>
     * ExportDeclaration :
     *     export default [lookahead &#x2209; { <b>function</b>, <b>class</b> }] AssignmentExpression<span><sub>[In]</sub></span> ;
     * </pre>
     * 
     * @param beginPosition
     *            the source begin position
     * @param endPosition
     *            the source end position
     * @param expression
     *            the default export expression node
     */
    public ExportDeclaration(long beginPosition, long endPosition,
            ExportDefaultExpression expression) {
        super(beginPosition, endPosition);
        this.type = Type.DefaultExpression;
        this.moduleSpecifier = null;
        this.exportClause = null;
        this.expression = expression;
        this.variableStatement = null;
        this.declaration = null;
    }

    public Type getType() {
        return type;
    }

    public String getModuleSpecifier() {
        assert type == Type.All || type == Type.External : "Type=" + type;
        return moduleSpecifier;
    }

    public ExportClause getExportClause() {
        assert type == Type.Local || type == Type.External : "Type=" + type;
        return exportClause;
    }

    public ExportDefaultExpression getExpression() {
        assert type == Type.DefaultExpression : "Type=" + type;
        return expression;
    }

    public VariableStatement getVariableStatement() {
        assert type == Type.Variable : "Type=" + type;
        return variableStatement;
    }

    public Declaration getDeclaration() {
        assert type == Type.Declaration : "Type=" + type;
        return declaration;
    }

    public HoistableDeclaration getHoistableDeclaration() {
        assert type == Type.DefaultHoistableDeclaration : "Type=" + type;
        return (HoistableDeclaration) declaration;
    }

    public ClassDeclaration getClassDeclaration() {
        assert type == Type.DefaultClassDeclaration : "Type=" + type;
        return (ClassDeclaration) declaration;
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
