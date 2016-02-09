/**
 * Copyright (c) 2012-2016 André Bargull
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
    protected List<Name> visit(Node node, List<Name> names) {
        throw new IllegalStateException();
    }

    private List<Name> forEach(Iterable<? extends Node> list, List<Name> names) {
        for (Node node : list) {
            node.accept(this, names);
        }
        return names;
    }

    /**
     * <pre>
     * LexicalDeclaration : LetOrConst BindingList ;
     * BindingList : BindingList , LexicalBinding
     * </pre>
     */
    @Override
    public List<Name> visit(LexicalDeclaration node, List<Name> names) {
        return forEach(node.getElements(), names);
    }

    /**
     * <pre>
     * LexicalBinding : BindingIdentifier Initializer<span><sub>opt</sub></span>
     * LexicalBinding: BindingPattern Initializer
     * </pre>
     */
    @Override
    public List<Name> visit(LexicalBinding node, List<Name> names) {
        return node.getBinding().accept(this, names);
    }

    /**
     * <pre>
     * BindingIdentifier : Identifier
     * </pre>
     */
    @Override
    public List<Name> visit(BindingIdentifier node, List<Name> names) {
        names.add(node.getName());
        return names;
    }

    /**
     * <pre>
     * VariableDeclarationList : VariableDeclarationList , VariableDeclaration
     * </pre>
     */
    @Override
    public List<Name> visit(VariableStatement node, List<Name> names) {
        return forEach(node.getElements(), names);
    }

    /**
     * <pre>
     * VariableDeclaration : BindingIdentifier Initializer<span><sub>opt</sub></span>
     * VariableDeclaration: BindingPattern Initializer
     * </pre>
     */
    @Override
    public List<Name> visit(VariableDeclaration node, List<Name> names) {
        return node.getBinding().accept(this, names);
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
    public List<Name> visit(ArrayBindingPattern node, List<Name> names) {
        return forEach(node.getElements(), names);
    }

    /**
     * <pre>
     * ObjectBindingPattern: { }
     * BindingPropertyList : BindingPropertyList , BindingProperty
     * </pre>
     */
    @Override
    public List<Name> visit(ObjectBindingPattern node, List<Name> names) {
        forEach(node.getProperties(), names);
        if (node.getRest() != null) {
            node.getRest().accept(this, names);
        }
        return names;
    }

    /**
     * <pre>
     * BindingProperty : SingleNameBinding
     * BindingProperty : PropertyName : BindingElement
     * </pre>
     */
    @Override
    public List<Name> visit(BindingProperty node, List<Name> names) {
        return node.getBinding().accept(this, names);
    }

    /**
     * <pre>
     * BindingRestProperty : ... BindingIdentifier
     * </pre>
     */
    @Override
    public List<Name> visit(BindingRestProperty node, List<Name> names) {
        return node.getBindingIdentifier().accept(this, names);
    }

    /**
     * <pre>
     * BindingElement : SingleNameBinding
     * BindingElement : BindingPattern Initializer<span><sub>opt</sub></span>
     * SingleNameBinding : BindingIdentifier Initializer<span><sub>opt</sub></span>
     * </pre>
     */
    @Override
    public List<Name> visit(BindingElement node, List<Name> names) {
        return node.getBinding().accept(this, names);
    }

    /**
     * <pre>
     * BindingRestElement : ... BindingIdentifier
     * </pre>
     */
    @Override
    public List<Name> visit(BindingRestElement node, List<Name> names) {
        return node.getBinding().accept(this, names);
    }

    /**
     * <pre>
     * Elision : ,
     * Elision : Elision ,
     * </pre>
     */
    @Override
    public List<Name> visit(BindingElision node, List<Name> names) {
        return names;
    }

    /**
     * <pre>
     * for each (ForDeclaration in Expression ) Statement
     * ForDeclaration : LetOrConst ForBinding
     * </pre>
     */
    @Override
    public List<Name> visit(ForEachStatement node, List<Name> names) {
        if (node.getHead() instanceof LexicalDeclaration) {
            return node.getHead().accept(this, names);
        }
        return names;
    }

    /**
     * <pre>
     * for (ForDeclaration in Expression ) Statement
     * ForDeclaration : LetOrConst ForBinding
     * </pre>
     */
    @Override
    public List<Name> visit(ForInStatement node, List<Name> names) {
        if (node.getHead() instanceof LexicalDeclaration) {
            return node.getHead().accept(this, names);
        }
        return names;
    }

    /**
     * <pre>
     * for (ForDeclaration of Expression ) Statement
     * ForDeclaration : LetOrConst ForBinding
     * </pre>
     */
    @Override
    public List<Name> visit(ForOfStatement node, List<Name> names) {
        if (node.getHead() instanceof LexicalDeclaration) {
            return node.getHead().accept(this, names);
        }
        return names;
    }

    /**
     * <pre>
     * FunctionDeclaration : function BindingIdentifier ( FormalParameterList ) { FunctionBody }
     * FunctionDeclaration : function ( FormalParameters ) { FunctionBody }
     * </pre>
     */
    @Override
    public List<Name> visit(FunctionDeclaration node, List<Name> names) {
        names.add(node.getName());
        return names;
    }

    /**
     * <pre>
     * GeneratorDeclaration : function * BindingIdentifier ( FormalParameterList ) { FunctionBody }
     * GeneratorDeclaration : function * ( FormalParameters ) { GeneratorBody }
     * </pre>
     */
    @Override
    public List<Name> visit(GeneratorDeclaration node, List<Name> names) {
        names.add(node.getName());
        return names;
    }

    /**
     * <pre>
     * GeneratorDeclaration : function * BindingIdentifier ( FormalParameterList ) { FunctionBody }
     * </pre>
     */
    @Override
    public List<Name> visit(LegacyGeneratorDeclaration node, List<Name> names) {
        names.add(node.getName());
        return names;
    }

    /**
     * <pre>
     * ClassDeclaration : class BindingIdentifier ClassTail
     * ClassDeclaration : class ClassTail
     * </pre>
     */
    @Override
    public List<Name> visit(ClassDeclaration node, List<Name> names) {
        names.add(node.getName());
        return names;
    }

    /**
     * <pre>
     * AsyncFunctionDeclaration : async function BindingIdentifier ( FormalParameterList ) { FunctionBody }
     * AsyncFunctionDeclaration : async function ( FormalParameterList ) { FunctionBody }
     * </pre>
     */
    @Override
    public List<Name> visit(AsyncFunctionDeclaration node, List<Name> names) {
        names.add(node.getName());
        return names;
    }

    @Override
    public List<Name> visit(FormalParameter node, List<Name> names) {
        return node.getElement().accept(this, names);
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
    public List<Name> visit(FormalParameterList node, List<Name> names) {
        return forEach(node.getFormals(), names);
    }

    /**
     * <pre>
     * ImportDeclaration : ModuleImport
     * ImportDeclaration : import ImportClause FromClause ;
     * ImportDeclaration : import ModuleSpecifier ;
     * </pre>
     */
    @Override
    public List<Name> visit(ImportDeclaration node, List<Name> names) {
        switch (node.getType()) {
        case ImportFrom:
            return node.getImportClause().accept(this, names);
        case ImportModule:
            return names;
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
    public List<Name> visit(ImportClause node, List<Name> names) {
        if (node.getDefaultEntry() != null) {
            node.getDefaultEntry().accept(this, names);
        }
        if (node.getNameSpace() != null) {
            node.getNameSpace().accept(this, names);
        }
        return forEach(node.getNamedImports(), names);
    }

    /**
     * <pre>
     * ImportSpecifier : IdentifierName as ImportedBinding
     * </pre>
     */
    @Override
    public List<Name> visit(ImportSpecifier node, List<Name> names) {
        return node.getLocalName().accept(this, names);
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
    public List<Name> visit(ExportDeclaration node, List<Name> names) {
        switch (node.getType()) {
        case All:
        case External:
        case Local:
            return names;
        case Variable:
            return node.getVariableStatement().accept(this, names);
        case Declaration:
            return node.getDeclaration().accept(this, names);
        case DefaultHoistableDeclaration:
            if (node.getHoistableDeclaration().getIdentifier() != null) {
                names.add(new Name(Name.DEFAULT_EXPORT));
            }
            return node.getHoistableDeclaration().accept(this, names);
        case DefaultClassDeclaration:
            if (node.getClassDeclaration().getIdentifier() != null) {
                names.add(new Name(Name.DEFAULT_EXPORT));
            }
            return node.getClassDeclaration().accept(this, names);
        case DefaultExpression:
            return node.getExpression().accept(this, names);
        default:
            throw new AssertionError();
        }
    }

    /**
     * <pre>
     * ExportDeclaration :
     *     export default AssignmentExpression ;
     * </pre>
     */
    @Override
    public List<Name> visit(ExportDefaultExpression node, List<Name> names) {
        return node.getBinding().accept(this, names);
    }
}
