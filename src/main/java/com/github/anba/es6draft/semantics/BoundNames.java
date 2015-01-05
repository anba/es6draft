/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.Name;

/**
 * Static Semantics: BoundNames
 */
final class BoundNames extends DefaultNodeVisitor<List<Name>, List<Name>> {
    static final BoundNames INSTANCE = new BoundNames();

    @Override
    protected List<Name> visit(Node node, List<Name> value) {
        throw new IllegalStateException();
    }

    private List<Name> forEach(Iterable<? extends Node> list, List<Name> value) {
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
    public List<Name> visit(LexicalDeclaration node, List<Name> value) {
        return forEach(node.getElements(), value);
    }

    /**
     * <pre>
     * LexicalBinding : BindingIdentifier Initializer<span><sub>opt</sub></span>
     * LexicalBinding: BindingPattern Initializer
     * </pre>
     */
    @Override
    public List<Name> visit(LexicalBinding node, List<Name> value) {
        return node.getBinding().accept(this, value);
    }

    /**
     * <pre>
     * BindingIdentifier : Identifier
     * </pre>
     */
    @Override
    public List<Name> visit(BindingIdentifier node, List<Name> value) {
        value.add(node.getName());
        return value;
    }

    /**
     * <pre>
     * VariableDeclarationList : VariableDeclarationList , VariableDeclaration
     * </pre>
     */
    @Override
    public List<Name> visit(VariableStatement node, List<Name> value) {
        return forEach(node.getElements(), value);
    }

    /**
     * <pre>
     * VariableDeclaration : BindingIdentifier Initializer<span><sub>opt</sub></span>
     * VariableDeclaration: BindingPattern Initializer
     * </pre>
     */
    @Override
    public List<Name> visit(VariableDeclaration node, List<Name> value) {
        return node.getBinding().accept(this, value);
    }

    /**
     * <pre>
     * ArrayBindingPattern : [ Elision<span><sub>opt</sub></span> ]
     * ArrayBindingPattern : [ Elision<span><sub>opt</sub></span> BindingRestElement ]
     * ArrayBindingPattern : [ BindingElementList , Elision<span><sub>opt</sub></span> ]
     * ArrayBindingPattern : [ BindingElementList , Elision<span><sub>opt</sub></span> BindingRestElement ]
     * BindingElementList : Elision<span><sub>opt</sub></span> BindingElement
     * BindingElementList : BindingElementList , Elision<span><sub>opt</sub></span> BindingElement
     * </pre>
     */
    @Override
    public List<Name> visit(ArrayBindingPattern node, List<Name> value) {
        return forEach(node.getElements(), value);
    }

    /**
     * <pre>
     * ObjectBindingPattern: { }
     * BindingPropertyList : BindingPropertyList , BindingProperty
     * </pre>
     */
    @Override
    public List<Name> visit(ObjectBindingPattern node, List<Name> value) {
        return forEach(node.getProperties(), value);
    }

    /**
     * <pre>
     * BindingProperty : SingleNameBinding
     * BindingProperty : PropertyName : BindingElement
     * </pre>
     */
    @Override
    public List<Name> visit(BindingProperty node, List<Name> value) {
        return node.getBinding().accept(this, value);
    }

    /**
     * <pre>
     * BindingElement : SingleNameBinding
     * BindingElement : BindingPattern Initializer<span><sub>opt</sub></span>
     * SingleNameBinding : BindingIdentifier Initializer<span><sub>opt</sub></span>
     * </pre>
     */
    @Override
    public List<Name> visit(BindingElement node, List<Name> value) {
        return node.getBinding().accept(this, value);
    }

    /**
     * <pre>
     * BindingRestElement : ... BindingIdentifier
     * </pre>
     */
    @Override
    public List<Name> visit(BindingRestElement node, List<Name> value) {
        return node.getBindingIdentifier().accept(this, value);
    }

    /**
     * <pre>
     * Elision : ,
     * Elision : Elision ,
     * </pre>
     */
    @Override
    public List<Name> visit(BindingElision node, List<Name> value) {
        return value;
    }

    /**
     * <pre>
     * for each (ForDeclaration in Expression ) Statement
     * ForDeclaration : LetOrConst ForBinding
     * </pre>
     */
    @Override
    public List<Name> visit(ForEachStatement node, List<Name> value) {
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
    public List<Name> visit(ForInStatement node, List<Name> value) {
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
    public List<Name> visit(ForOfStatement node, List<Name> value) {
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
    public List<Name> visit(FunctionDeclaration node, List<Name> value) {
        return node.getIdentifier().accept(this, value);
    }

    /**
     * <pre>
     * GeneratorDeclaration : function * BindingIdentifier ( FormalParameterList ) { FunctionBody }
     * </pre>
     */
    @Override
    public List<Name> visit(GeneratorDeclaration node, List<Name> value) {
        return node.getIdentifier().accept(this, value);
    }

    /**
     * <pre>
     * GeneratorDeclaration : function * BindingIdentifier ( FormalParameterList ) { FunctionBody }
     * </pre>
     */
    @Override
    public List<Name> visit(LegacyGeneratorDeclaration node, List<Name> value) {
        return node.getIdentifier().accept(this, value);
    }

    /**
     * <pre>
     * ClassDeclaration: class BindingIdentifier ClassTail
     * </pre>
     */
    @Override
    public List<Name> visit(ClassDeclaration node, List<Name> value) {
        return node.getIdentifier().accept(this, value);
    }

    /**
     * <pre>
     * AsyncFunctionDeclaration : async function BindingIdentifier ( FormalParameterList ) { FunctionBody }
     * </pre>
     */
    @Override
    public List<Name> visit(AsyncFunctionDeclaration node, List<Name> value) {
        return node.getIdentifier().accept(this, value);
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
    public List<Name> visit(FormalParameterList node, List<Name> value) {
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
    public List<Name> visit(ImportDeclaration node, List<Name> value) {
        switch (node.getType()) {
        case ImportFrom:
            return node.getImportClause().accept(this, value);
        case ImportModule:
            return value;
        default:
            throw new AssertionError();
        }
    }

    /**
     * <pre>
     * ImportClause : ImportedBinding , NamedImports
     * ImportsList : ImportsList , ImportSpecifier
     * </pre>
     */
    @Override
    public List<Name> visit(ImportClause node, List<Name> value) {
        if (node.getDefaultEntry() != null) {
            node.getDefaultEntry().accept(this, value);
        }
        if (node.getNameSpace() != null) {
            node.getNameSpace().accept(this, value);
        }
        return forEach(node.getNamedImports(), value);
    }

    /**
     * <pre>
     * ImportSpecifier : IdentifierName as ImportedBinding
     * </pre>
     */
    @Override
    public List<Name> visit(ImportSpecifier node, List<Name> value) {
        return node.getLocalName().accept(this, value);
    }

    /**
     * <pre>
     * ExportDeclaration :
     *     export * FromClause ;
     *     export ExportClause FromClause ;
     *     export ExportClause ;
     *     export VariableStatement
     *     export Declaration
     *     export default HoistableDeclaration
     *     export default ClassDeclaration
     *     export default AssignmentExpression ;
     * </pre>
     */
    @Override
    public List<Name> visit(ExportDeclaration node, List<Name> value) {
        switch (node.getType()) {
        case All:
        case External:
        case Local:
            return value;
        case Variable:
            return node.getVariableStatement().accept(this, value);
        case Declaration:
            return node.getDeclaration().accept(this, value);
        case DefaultHoistableDeclaration:
            // TODO: Necessary to include "*default*" if not present?
            return node.getHoistableDeclaration().accept(this, value);
        case DefaultClassDeclaration:
            // TODO: Necessary to include "*default*" if not present?
            return node.getClassDeclaration().accept(this, value);
        case DefaultExpression:
            return node.getExpression().getBinding().accept(this, value);
        default:
            throw new AssertionError();
        }
    }
}
