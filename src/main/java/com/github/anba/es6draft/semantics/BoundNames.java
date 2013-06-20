/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import java.util.List;

import com.github.anba.es6draft.ast.*;

/**
 * Static Semantics: BoundNames
 */
class BoundNames extends StaticSemanticsVisitor<List<String>, List<String>> {
    static final NodeVisitor<List<String>, List<String>> INSTANCE = new BoundNames();

    /**
     * <pre>
     * LexicalDeclaration : LetOrConst BindingList ;
     * BindingList : BindingList , LexicalBinding
     * </pre>
     */
    @Override
    public List<String> visit(LexicalDeclaration node, List<String> value) {
        forEach(this, node.getElements(), value);
        return value;
    }

    /**
     * <pre>
     * LexicalBinding : BindingIdentifier Initialiser<sub>opt</sub>
     * LexicalBinding: BindingPattern Initialiser
     * </pre>
     */
    @Override
    public List<String> visit(LexicalBinding node, List<String> value) {
        return node.getBinding().accept(this, value);
    }

    /**
     * <pre>
     * BindingIdentifier : Identifier
     * </pre>
     */
    @Override
    public List<String> visit(BindingIdentifier node, List<String> value) {
        value.add(node.getName());
        return value;
    }

    /**
     * <pre>
     * VariableDeclarationList : VariableDeclarationList , VariableDeclaration
     * </pre>
     */
    @Override
    public List<String> visit(VariableStatement node, List<String> value) {
        forEach(this, node.getElements(), value);
        return value;
    }

    /**
     * <pre>
     * VariableDeclaration : BindingIdentifier Initialiser<sub>opt</sub>
     * VariableDeclaration: BindingPattern Initialiser
     * </pre>
     */
    @Override
    public List<String> visit(VariableDeclaration node, List<String> value) {
        return node.getBinding().accept(this, value);
    }

    /**
     * <pre>
     * ArrayBindingPattern : [ Elision<sub>opt</sub> ]
     * ArrayBindingPattern : [ Elision<sub>opt</sub> BindingRestElement ]
     * ArrayBindingPattern : [ BindingElementList , Elision<sub>opt</sub> ]
     * ArrayBindingPattern : [ BindingElementList , Elision<sub>opt</sub> BindingRestElement ]
     * BindingElementList : Elision<sub>opt</sub> BindingElement
     * BindingElementList : BindingElementList , Elision<sub>opt</sub> BindingElement
     * </pre>
     */
    @Override
    public List<String> visit(ArrayBindingPattern node, List<String> value) {
        forEach(this, node.getElements(), value);
        return value;
    }

    /**
     * <pre>
     * ObjectBindingPattern: { }
     * BindingPropertyList : BindingPropertyList , BindingProperty
     * </pre>
     */
    @Override
    public List<String> visit(ObjectBindingPattern node, List<String> value) {
        forEach(this, node.getProperties(), value);
        return value;
    }

    /**
     * <pre>
     * BindingProperty : SingleNameBinding
     * BindingProperty : PropertyName : BindingElement
     * </pre>
     */
    @Override
    public List<String> visit(BindingProperty node, List<String> value) {
        return node.getBinding().accept(this, value);
    }

    /**
     * <pre>
     * BindingElement : SingleNameBinding
     * BindingElement : BindingPattern Initialiser<sub>opt</sub>
     * SingleNameBinding : BindingIdentifier Initialiser<sub>opt</sub>
     * </pre>
     */
    @Override
    public List<String> visit(BindingElement node, List<String> value) {
        return node.getBinding().accept(this, value);
    }

    /**
     * <pre>
     * BindingRestElement : ... BindingIdentifier
     * </pre>
     */
    @Override
    public List<String> visit(BindingRestElement node, List<String> value) {
        return node.getBindingIdentifier().accept(this, value);
    }

    /**
     * <pre>
     * Elision : ,
     * Elision : Elision ,
     * </pre>
     */
    @Override
    public List<String> visit(BindingElision node, List<String> value) {
        return value;
    }

    /**
     * <pre>
     * for each (ForDeclaration in Expression ) Statement
     * ForDeclaration : LetOrConst ForBinding
     * </pre>
     */
    @Override
    public List<String> visit(ForEachStatement node, List<String> value) {
        if (node.getHead() instanceof LexicalDeclaration) {
            return node.getHead().accept(this, value);
        }
        return value;
    }

    /**
     * <pre>
     * for (ForDeclaration in Expression ) Statement
     * ForDeclaration : LetOrConst ForBinding
     * </pre>
     */
    @Override
    public List<String> visit(ForInStatement node, List<String> value) {
        if (node.getHead() instanceof LexicalDeclaration) {
            return node.getHead().accept(this, value);
        }
        return value;
    }

    /**
     * <pre>
     * for (ForDeclaration of Expression ) Statement
     * ForDeclaration : LetOrConst ForBinding
     * </pre>
     */
    @Override
    public List<String> visit(ForOfStatement node, List<String> value) {
        if (node.getHead() instanceof LexicalDeclaration) {
            return node.getHead().accept(this, value);
        }
        return value;
    }

    /**
     * <pre>
     * FunctionDeclaration : function BindingIdentifier ( FormalParameterList ) { FunctionBody }
     * </pre>
     */
    @Override
    public List<String> visit(FunctionDeclaration node, List<String> value) {
        return node.getIdentifier().accept(this, value);
    }

    /**
     * <pre>
     * GeneratorDeclaration : function * BindingIdentifier ( FormalParameterList ) { FunctionBody }
     * </pre>
     */
    @Override
    public List<String> visit(GeneratorDeclaration node, List<String> value) {
        return node.getIdentifier().accept(this, value);
    }

    /**
     * <pre>
     * ClassDeclaration: class BindingIdentifier ClassTail
     * </pre>
     */
    @Override
    public List<String> visit(ClassDeclaration node, List<String> value) {
        return node.getName().accept(this, value);
    }
}