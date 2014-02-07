/**
 * Copyright (c) 2012-2014 André Bargull
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
final class BoundNames extends DefaultNodeVisitor<List<String>, List<String>> {
    static final NodeVisitor<List<String>, List<String>> INSTANCE = new BoundNames();

    @Override
    protected List<String> visit(Node node, List<String> value) {
        throw new IllegalStateException();
    }

    private List<String> forEach(Iterable<? extends Node> list, List<String> value) {
        for (Node node : list) {
            node.accept(this, value);
        }
        return value;
    }

    /**
     * <pre>
     * LexicalDeclaration : LetOrConst BindingList ;
     * BindingList : BindingList , LexicalBinding
     * </pre>
     */
    @Override
    public List<String> visit(LexicalDeclaration node, List<String> value) {
        return forEach(node.getElements(), value);
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
        return forEach(node.getElements(), value);
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
        return forEach(node.getElements(), value);
    }

    /**
     * <pre>
     * ObjectBindingPattern: { }
     * BindingPropertyList : BindingPropertyList , BindingProperty
     * </pre>
     */
    @Override
    public List<String> visit(ObjectBindingPattern node, List<String> value) {
        return forEach(node.getProperties(), value);
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
     * GeneratorDeclaration : function * BindingIdentifier ( FormalParameterList ) { FunctionBody }
     * </pre>
     */
    @Override
    public List<String> visit(LegacyGeneratorDeclaration node, List<String> value) {
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

    /**
     * <pre>
     * StrictFormalParameters :
     *     FormalParameters
     * FormalParameters :
     *     [empty]
     *     FormalParameterList
     * FormalParameterList :
     *     FunctionRestParameter
     *     FormalsList
     *     FormalsList , FunctionRestParameter
     * FormalsList :
     *     FormalParameter
     *     FormalsList , FormalParameter
     * </pre>
     */
    @Override
    public List<String> visit(FormalParameterList node, List<String> value) {
        return forEach(node.getFormals(), value);
    }

    /**
     * <pre>
     * ImportDeclaration : ModuleImport
     * ImportDeclaration : import ImportClause FromClause ;
     * ImportDeclaration : import ModuleSpecifier ;
     * </pre>
     */
    @Override
    public List<String> visit(ImportDeclaration node, List<String> value) {
        switch (node.getType()) {
        case ModuleImport:
            return node.getModuleImport().accept(this, value);
        case ImportFrom:
            return node.getImportClause().accept(this, value);
        case ImportModule:
        default:
            return value;
        }
    }

    /**
     * <pre>
     * ModuleImport : module ImportedBinding FromClause ;
     * </pre>
     */
    @Override
    public List<String> visit(ModuleImport node, List<String> value) {
        return node.getImportedBinding().accept(this, value);
    }

    /**
     * <pre>
     * ImportClause : ImportedBinding , NamedImports
     * ImportsList : ImportsList , ImportSpecifier
     * </pre>
     */
    @Override
    public List<String> visit(ImportClause node, List<String> value) {
        if (node.getDefaultEntry() != null) {
            node.getDefaultEntry().accept(this, value);
        }
        return forEach(node.getNamedImports(), value);
    }

    /**
     * <pre>
     * ImportSpecifier : IdentifierName as ImportedBinding
     * </pre>
     */
    @Override
    public List<String> visit(ImportSpecifier node, List<String> value) {
        return node.getLocalName().accept(this, value);
    }

    /**
     * <pre>
     * ExportDeclaration :
     *     export * FromClause ;
     *     export ExportClause FromClause ;
     *     export ExportClause ;
     * ExportDeclaration : export VariableStatement ;
     * ExportDeclaration : export Declaration ;
     * ExportDeclaration : export default AssignmentExpression ;
     * </pre>
     */
    @Override
    public List<String> visit(ExportDeclaration node, List<String> value) {
        switch (node.getType()) {
        case Variable:
            return node.getVariableStatement().accept(this, value);
        case Declaration:
            return node.getDeclaration().accept(this, value);
        case Default:
            value.add("default");
            return value;
        case All:
        case External:
        case Local:
        default:
            return value;
        }
    }
}
