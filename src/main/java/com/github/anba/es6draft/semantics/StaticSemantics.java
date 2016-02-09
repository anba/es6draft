/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.BlockScope;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.runtime.internal.InlineArrayList;
import com.github.anba.es6draft.runtime.modules.ExportEntry;
import com.github.anba.es6draft.runtime.modules.ImportEntry;

/**
 * <h1>Static Semantics</h1>
 * <ul>
 * <li>BoundNames
 * <li>ConstructorMethod
 * <li>ContainsExpression
 * <li>ExpectedArgumentCount
 * <li>ExportEntries
 * <li>HasInitializer
 * <li>HasName
 * <li>ImportedLocalNames
 * <li>ImportEntries
 * <li>IsAnonymousFunctionDefinition
 * <li>IsConstantDeclaration
 * <li>IsFunctionDefinition
 * <li>IsIdentifierRef
 * <li>IsSimpleParameterList
 * <li>IsStrict
 * <li>LexicallyDeclaredNames
 * <li>LexicallyScopedDeclarations
 * <li>ModuleRequests
 * <li>PropName
 * <li>SpecialMethod
 * <li>VarDeclaredNames
 * <li>VarScopedDeclarations
 * </ul>
 */
public final class StaticSemantics {
    private StaticSemantics() {
    }

    /**
     * Static Semantics: BoundNames
     * <ul>
     * <li>12.1.2 Static Semantics: BoundNames
     * </ul>
     * 
     * @param node
     *            the binding identifier
     * @return the bound name
     */
    public static Name BoundName(BindingIdentifier node) {
        return node.getName();
    }

    /**
     * Static Semantics: BoundNames
     * <ul>
     * <li>14.1.3 Static Semantics: BoundNames
     * <li>14.4.2 Static Semantics: BoundNames
     * </ul>
     * 
     * @param node
     *            the hoistable declaration
     * @return the bound name
     */
    public static Name BoundName(HoistableDeclaration node) {
        return node.getName();
    }

    /**
     * Static Semantics: BoundNames
     * <ul>
     * <li>14.5.2 Static Semantics: BoundNames
     * </ul>
     * 
     * @param node
     *            the hoistable declaration
     * @return the bound name
     */
    public static Name BoundName(ClassDeclaration node) {
        return node.getName();
    }

    /**
     * Static Semantics: BoundNames
     * <ul>
     * <li>12.1.2 Static Semantics: BoundNames
     * <li>13.3.3.1 Static Semantics: BoundNames
     * </ul>
     * 
     * @param node
     *            the binding node
     * @return the bound names
     */
    public static List<Name> BoundNames(BindingIdentifier node) {
        return singletonList(node.getName());
    }

    /**
     * Static Semantics: BoundNames
     * <ul>
     * <li>12.1.2 Static Semantics: BoundNames
     * <li>13.3.3.1 Static Semantics: BoundNames
     * </ul>
     * 
     * @param node
     *            the binding node
     * @return the bound names
     */
    public static List<Name> BoundNames(Binding node) {
        if (node instanceof BindingIdentifier) {
            return singletonList(((BindingIdentifier) node).getName());
        }
        assert node instanceof BindingPattern;
        return node.accept(BoundNames.INSTANCE, new InlineArrayList<>());
    }

    /**
     * Static Semantics: BoundNames
     * <ul>
     * <li>14.1.3 Static Semantics: BoundNames
     * <li>14.2.2 Static Semantics: BoundNames
     * </ul>
     * 
     * @param node
     *            the formal parameters list
     * @return the bound names
     */
    public static List<Name> BoundNames(FormalParameterList node) {
        return BoundNames(node.getFormals());
    }

    /**
     * Static Semantics: BoundNames
     * <ul>
     * <li>14.1.3 Static Semantics: BoundNames
     * <li>14.2.2 Static Semantics: BoundNames
     * </ul>
     * 
     * @param formals
     *            the formal parameters list
     * @return the bound names
     */
    public static List<Name> BoundNames(List<FormalParameter> formals) {
        // TODO: Some callers expect the returned list to be mutable.
        InlineArrayList<Name> list = new InlineArrayList<>();
        for (FormalParameter formalParameter : formals) {
            formalParameter.accept(BoundNames.INSTANCE, list);
        }
        return list;
    }

    /**
     * Static Semantics: BoundNames
     * <ul>
     * <li>14.1.3 Static Semantics: BoundNames
     * <li>14.2.2 Static Semantics: BoundNames
     * </ul>
     * 
     * @param node
     *            the formal parameter node
     * @return the bound names
     */
    public static List<Name> BoundNames(FormalParameter node) {
        BindingElementItem element = node.getElement();
        if (element instanceof BindingElement) {
            return BoundNames(((BindingElement) element).getBinding());
        }
        return BoundNames(((BindingRestElement) element).getBinding());
    }

    /**
     * Static Semantics: BoundNames
     * <ul>
     * <li>13.3.1.2 Static Semantics: BoundNames
     * <li>14.1.3 Static Semantics: BoundNames
     * <li>14.4.2 Static Semantics: BoundNames
     * <li>14.5.2 Static Semantics: BoundNames
     * </ul>
     * 
     * @param node
     *            the declaration node
     * @return the bound names
     */
    public static List<Name> BoundNames(Declaration node) {
        if (node instanceof HoistableDeclaration) {
            return singletonList(BoundName((HoistableDeclaration) node));
        }
        if (node instanceof ClassDeclaration) {
            return singletonList(BoundName((ClassDeclaration) node));
        }
        if (node instanceof ExportDefaultExpression) {
            return BoundNames(((ExportDefaultExpression) node).getBinding());
        }
        assert node instanceof LexicalDeclaration;
        return node.accept(BoundNames.INSTANCE, new InlineArrayList<>());
    }

    /**
     * 13.3.2.1 Static Semantics: BoundNames
     * 
     * @param node
     *            the variable statement
     * @return the bound names
     */
    public static List<Name> BoundNames(VariableStatement node) {
        return node.accept(BoundNames.INSTANCE, new InlineArrayList<>());
    }

    /**
     * 13.3.2.1 Static Semantics: BoundNames
     * 
     * @param node
     *            the variable statement
     * @return the bound names
     */
    public static List<Name> BoundNames(VariableDeclaration node) {
        return BoundNames(node.getBinding());
    }

    /**
     * 15.2.2.2 Static Semantics: BoundNames
     * 
     * @param node
     *            the import declaration
     * @return the bound names
     */
    public static List<Name> BoundNames(ImportDeclaration node) {
        return node.accept(BoundNames.INSTANCE, new InlineArrayList<>());
    }

    /**
     * 15.2.3.2 Static Semantics: BoundNames
     * 
     * @param node
     *            the export declaration
     * @return the bound names
     */
    public static List<Name> BoundNames(ExportDeclaration node) {
        return node.accept(BoundNames.INSTANCE, new InlineArrayList<>());
    }

    /**
     * 14.5.3 Static Semantics: ConstructorMethod
     * 
     * @param node
     *            the class definition node
     * @return the constructor method if found, otherwise {@code null}
     */
    public static MethodDefinition ConstructorMethod(ClassDefinition node) {
        return node.getConstructor();
    }

    /**
     * 14.5.3 Static Semantics: ConstructorMethod
     * 
     * @param methods
     *            the methods list
     * @return the constructor method if found, otherwise {@code null}
     */
    public static MethodDefinition ConstructorMethod(List<MethodDefinition> methods) {
        for (MethodDefinition m : methods) {
            if (m.isClassConstructor()) {
                return m;
            }
        }
        return null;
    }

    /**
     * Static Semantics: CallConstructorMethod
     * 
     * @param methods
     *            the methods list
     * @return the call constructor method if found, otherwise {@code null}
     */
    public static MethodDefinition CallConstructorMethod(List<MethodDefinition> methods) {
        for (MethodDefinition m : methods) {
            if (m.isCallConstructor()) {
                return m;
            }
        }
        return null;
    }

    /**
     * Static Semantics: ContainsExpression
     * <ul>
     * <li>13.3.3.2 Static Semantics: ContainsExpression
     * <li>14.1.5 Static Semantics: ContainsExpression
     * <li>14.2.4 Static Semantics: ContainsExpression
     * </ul>
     * 
     * @param formals
     *            the formal parameters
     * @return {@code true} if an expression was found
     */
    public static boolean ContainsExpression(FormalParameterList formals) {
        return formals.containsExpression();
    }

    /**
     * Static Semantics: ContainsExpression
     * <ul>
     * <li>13.3.3.2 Static Semantics: ContainsExpression
     * <li>14.1.5 Static Semantics: ContainsExpression
     * <li>14.2.4 Static Semantics: ContainsExpression
     * </ul>
     * 
     * @param formals
     *            the formal parameters
     * @return {@code true} if an expression was found
     */
    public static boolean ContainsExpression(List<FormalParameter> formals) {
        for (FormalParameter formal : formals) {
            BindingElementItem element = formal.getElement();
            if (element instanceof BindingElement && ContainsExpression((BindingElement) element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 13.3.3.2 Static Semantics: ContainsExpression
     * 
     * @param element
     *            the binding element node
     * @return {@code true} if an expression was found
     */
    private static boolean ContainsExpression(BindingElement element) {
        if (element.getInitializer() != null) {
            return true;
        }
        return ContainsExpression(element.getBinding());
    }

    /**
     * 13.3.3.2 Static Semantics: ContainsExpression
     * 
     * @param binding
     *            the binding node
     * @return {@code true} if an expression was found
     */
    private static boolean ContainsExpression(Binding binding) {
        if (binding instanceof ArrayBindingPattern) {
            return ContainsExpression((ArrayBindingPattern) binding);
        }
        if (binding instanceof ObjectBindingPattern) {
            return ContainsExpression((ObjectBindingPattern) binding);
        }
        assert binding instanceof BindingIdentifier;
        return false;
    }

    /**
     * 13.3.3.2 Static Semantics: ContainsExpression
     * 
     * @param pattern
     *            the array binding pattern node
     * @return {@code true} if an expression was found
     */
    private static boolean ContainsExpression(ArrayBindingPattern pattern) {
        for (BindingElementItem item : pattern.getElements()) {
            if (item instanceof BindingElement && ContainsExpression((BindingElement) item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 13.3.3.2 Static Semantics: ContainsExpression
     * 
     * @param pattern
     *            the object binding pattern node
     * @return {@code true} if an expression was found
     */
    private static boolean ContainsExpression(ObjectBindingPattern pattern) {
        for (BindingProperty property : pattern.getProperties()) {
            if (property.getPropertyName() instanceof ComputedPropertyName) {
                return true;
            }
            if (property.getInitializer() != null) {
                return true;
            }
            if (ContainsExpression(property.getBinding())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Static Semantics: ExpectedArgumentCount
     * <ul>
     * <li>14.1.6 Static Semantics: ExpectedArgumentCount
     * <li>14.2.5 Static Semantics: ExpectedArgumentCount
     * <li>14.3.3 Static Semantics: ExpectedArgumentCount
     * </ul>
     * 
     * @param formals
     *            the formal parameters list
     * @return the expected arguments count
     */
    public static int ExpectedArgumentCount(FormalParameterList formals) {
        int count = 0;
        for (FormalParameter formal : formals) {
            BindingElementItem element = formal.getElement();
            if (element instanceof BindingRestElement) {
                break;
            }
            if (HasInitializer((BindingElement) element)) {
                break;
            }
            count += 1;
        }
        return count;
    }

    /**
     * 13.3.3.3 Static Semantics: HasInitializer
     * 
     * @param node
     *            the binding element
     * @return {@code true} if the binding element has an initializer
     */
    private static boolean HasInitializer(BindingElement node) {
        return node.getInitializer() != null;
    }

    /**
     * 14.1.9 Static Semantics: IsAnonymousFunctionDefinition (production)
     * 
     * @param node
     *            the expression node
     * @return {@code true} if the node is an anonymous function definition
     */
    public static boolean IsAnonymousFunctionDefinition(Expression node) {
        if (node instanceof FunctionNode) {
            return ((FunctionNode) node).getIdentifier() == null;
        }
        if (node instanceof ClassExpression) {
            return ((ClassExpression) node).getIdentifier() == null;
        }
        return false;
    }

    /**
     * Static Semantics: HasName
     * <ul>
     * <li>14.1.8 Static Semantics: HasName
     * <li>14.2.7 Static Semantics: HasName
     * <li>14.4.7 Static Semantics: HasName
     * </ul>
     * 
     * @param <FUNCTION>
     *            the function type
     * @param node
     *            the function node
     * @return {@code true} if the function has a binding name
     */
    public static <FUNCTION extends Expression & FunctionNode> boolean HasName(FUNCTION node) {
        return node.getIdentifier() != null;
    }

    /**
     * 14.5.6 Static Semantics: HasName
     * 
     * @param node
     *            the function node
     * @return {@code true} if the function has a binding name
     */
    public static boolean HasName(ClassExpression node) {
        return node.getIdentifier() != null;
    }

    /**
     * Static Semantics: IsConstantDeclaration
     * <ul>
     * <li>13.3.1.3 Static Semantics: IsConstantDeclaration
     * <li>14.1.10 Static Semantics: IsConstantDeclaration
     * <li>14.4.8 Static Semantics: IsConstantDeclaration
     * <li>14.5.7 Static Semantics: IsConstantDeclaration
     * <li>15.2.3.7 Static Semantics: IsConstantDeclaration
     * </ul>
     * 
     * @param node
     *            the declaration node
     * @return {@code true} if the node is a constant declaration
     */
    public static boolean IsConstantDeclaration(Declaration node) {
        return node.isConstDeclaration();
    }

    /**
     * Static Semantics: IsFunctionDefinition
     * <ul>
     * <li>12.2.1.3 Static Semantics: IsFunctionDefinition
     * <li>12.2.10.2 Static Semantics: IsFunctionDefinition
     * <li>12.3.1.2 Static Semantics: IsFunctionDefinition
     * <li>12.4.2 Static Semantics: IsFunctionDefinition
     * <li>12.5.2 Static Semantics: IsFunctionDefinition
     * <li>12.6.1 Static Semantics: IsFunctionDefinition
     * <li>12.7.1 Static Semantics: IsFunctionDefinition
     * <li>12.8.1 Static Semantics: IsFunctionDefinition
     * <li>12.9.1 Static Semantics: IsFunctionDefinition
     * <li>12.10.1 Static Semantics: IsFunctionDefinition
     * <li>12.11.1 Static Semantics: IsFunctionDefinition
     * <li>12.12.1 Static Semantics: IsFunctionDefinition
     * <li>12.13.1 Static Semantics: IsFunctionDefinition
     * <li>12.14.2 Static Semantics: IsFunctionDefinition
     * <li>12.15.1 Static Semantics: IsFunctionDefinition
     * <li>14.1.11 Static Semantics: IsFunctionDefinition
     * <li>14.4.9 Static Semantics: IsFunctionDefinition
     * <li>14.5.8 Static Semantics: IsFunctionDefinition
     * </ul>
     * 
     * @param node
     *            the expression node
     * @return {@code true} if the node is a function definition
     */
    public static boolean IsFunctionDefinition(Expression node) {
        return node instanceof FunctionNode || node instanceof ClassExpression;
    }

    /**
     * Static Semantics: IsIdentifierRef
     * <ul>
     * <li>12.2.1.4 Static Semantics: IsIdentifierRef
     * <li>12.3.1.4 Static Semantics: IsIdentifierRef
     * </ul>
     * 
     * @param node
     *            the left-hand side expression node
     * @return {@code true} if node is an identifier
     */
    public static boolean IsIdentifierRef(LeftHandSideExpression node) {
        return node instanceof IdentifierReference && !node.isParenthesized();
    }

    /**
     * Static Semantics: IsSimpleParameterList
     * <ul>
     * <li>14.1.12 Static Semantics: IsSimpleParameterList
     * <li>14.2.8 Static Semantics: IsSimpleParameterList
     * </ul>
     * 
     * @param formals
     *            the formal parameters list
     * @return {@code true} if the list is a simple formal parameters list
     */
    public static boolean IsSimpleParameterList(FormalParameterList formals) {
        return formals.isSimpleParameterList();
    }

    /**
     * Static Semantics: IsSimpleParameterList
     * <ul>
     * <li>14.1.12 Static Semantics: IsSimpleParameterList
     * <li>14.2.8 Static Semantics: IsSimpleParameterList
     * </ul>
     * 
     * @param formals
     *            the formal parameters list
     * @return {@code true} if the list is a simple formal parameters list
     */
    public static boolean IsSimpleParameterList(List<FormalParameter> formals) {
        for (FormalParameter formal : formals) {
            BindingElementItem element = formal.getElement();
            if (element instanceof BindingRestElement) {
                return false;
            }
            if (HasInitializer((BindingElement) element)) {
                return false;
            }
            if (((BindingElement) element).getBinding() instanceof BindingPattern) {
                return false;
            }
        }
        return true;
    }

    /**
     * 15.1.2 Static Semantics: IsStrict
     * 
     * @param node
     *            the script node
     * @return {@code true} if the node is strict mode script
     */
    public static boolean IsStrict(Script node) {
        return node.isStrict();
    }

    /**
     * 15.2.1.9 Static Semantics: IsStrict
     * 
     * @param node
     *            the module node
     * @return {@code true} if the node is strict mode module
     */
    public static boolean IsStrict(Module node) {
        return true;
    }

    /**
     * 14.1.13 Static Semantics: IsStrict
     * 
     * @param node
     *            the function node
     * @return {@code true} if the node is strict mode function
     */
    public static boolean IsStrict(FunctionNode node) {
        return node.getStrictMode() != FunctionNode.StrictMode.NonStrict;
    }

    /**
     * 13.1.6 Static Semantics: LexicallyScopedDeclarations
     * 
     * @param node
     *            the block statement node
     * @return the list of lexically scoped declarations
     */
    public static List<Declaration> LexicallyScopedDeclarations(BlockStatement node) {
        return node.getScope().lexicallyScopedDeclarations();
    }

    /**
     * 13.11.6 Static Semantics: LexicallyScopedDeclarations
     * 
     * @param node
     *            the switch statement node
     * @return the list of lexically scoped declarations
     */
    public static List<Declaration> LexicallyScopedDeclarations(SwitchStatement node) {
        return node.getScope().lexicallyScopedDeclarations();
    }

    /**
     * Static Semantics: LexicallyScopedDeclarations
     * <ul>
     * <li>14.1.15 Static Semantics: LexicallyScopedDeclarations
     * <li>14.2.11 Static Semantics: LexicallyScopedDeclarations
     * </ul>
     * 
     * @param node
     *            the function node
     * @return the list of lexically scoped declarations
     */
    public static List<Declaration> LexicallyScopedDeclarations(FunctionNode node) {
        return node.getScope().lexicallyScopedDeclarations();
    }

    /**
     * 15.1.4 Static Semantics: LexicallyScopedDeclarations
     * 
     * @param node
     *            the script node
     * @return the list of lexically scoped declarations
     */
    public static List<Declaration> LexicallyScopedDeclarations(Script node) {
        return node.getScope().lexicallyScopedDeclarations();
    }

    /**
     * 13.1.5 Static Semantics: LexicallyDeclaredNames
     * 
     * @param node
     *            the block statement node
     * @return the set of lexically declared names
     */
    public static Set<Name> LexicallyDeclaredNames(BlockStatement node) {
        return node.getScope().lexicallyDeclaredNames();
    }

    /**
     * 13.11.5 Static Semantics: LexicallyDeclaredNames
     * 
     * @param node
     *            the switch statement node
     * @return the list of lexically scoped declarations
     */
    public static Set<Name> LexicallyDeclaredNames(SwitchStatement node) {
        return node.getScope().lexicallyDeclaredNames();
    }

    /**
     * Static Semantics: LexicallyDeclaredNames
     * 
     * @param scope
     *            the block scope
     * @return the set of lexically declared names
     */
    public static Set<Name> LexicallyDeclaredNames(BlockScope scope) {
        return scope.lexicallyDeclaredNames();
    }

    /**
     * Static Semantics: LexicallyDeclaredNames
     * <ul>
     * <li>14.1.14 Static Semantics: LexicallyDeclaredNames
     * <li>14.2.10 Static Semantics: LexicallyDeclaredNames
     * </ul>
     * 
     * @param node
     *            the script node
     * @return the set of lexically declared names
     */
    public static Set<Name> LexicallyDeclaredNames(FunctionNode node) {
        return node.getScope().lexicallyDeclaredNames();
    }

    /**
     * 15.1.3 Static Semantics: LexicallyDeclaredNames
     * 
     * @param node
     *            the script node
     * @return the set of lexically declared names
     */
    public static Set<Name> LexicallyDeclaredNames(Script node) {
        return node.getScope().lexicallyDeclaredNames();
    }

    /**
     * Static Semantics: VarDeclaredNames
     * <ul>
     * <li>14.1.16 Static Semantics: VarDeclaredNames
     * <li>14.2.12 Static Semantics: VarDeclaredNames
     * </ul>
     * 
     * <pre>
     * FunctionStatementList :
     *     StatementList
     * </pre>
     * 
     * @param node
     *            the function node
     * @return the set of variable declared names
     */
    public static Set<Name> VarDeclaredNames(FunctionNode node) {
        return node.getScope().varDeclaredNames();
    }

    /**
     * 15.1.5 Static Semantics: VarDeclaredNames
     * 
     * @param node
     *            the script node
     * @return the set of variable declared names
     */
    public static Set<Name> VarDeclaredNames(Script node) {
        return node.getScope().varDeclaredNames();
    }

    /**
     * Static Semantics: VarScopedDeclarations
     * <ul>
     * <li>14.1.17 Static Semantics: VarScopedDeclarations
     * <li>14.2.13 Static Semantics: VarScopedDeclarations
     * </ul>
     * 
     * @param node
     *            the function node
     * @return the list of variable scoped declarations
     */
    public static List<StatementListItem> VarScopedDeclarations(FunctionNode node) {
        return node.getScope().varScopedDeclarations();
    }

    /**
     * 15.1.6 Static Semantics: VarScopedDeclarations
     * 
     * @param node
     *            the script node
     * @return the list of variable scoped declarations
     */
    public static List<StatementListItem> VarScopedDeclarations(Script node) {
        return node.getScope().varScopedDeclarations();
    }

    /**
     * 15.2.1.7 Static Semantics: ExportEntries<br>
     * 15.2.3.5 Static Semantics: ExportEntries
     * 
     * @param node
     *            the module node
     * @return the list of export entries
     */
    public static List<ExportEntry> ExportEntries(Module node) {
        ArrayList<ExportEntry> entries = new ArrayList<>();
        for (ModuleItem item : node.getStatements()) {
            if (item instanceof ExportDeclaration) {
                ExportDeclaration exportDecl = (ExportDeclaration) item;
                switch (exportDecl.getType()) {
                case All: {
                    String module = exportDecl.getModuleSpecifier();
                    entries.add(new ExportEntry(item, module, "*", null, null));
                    break;
                }
                case External: {
                    String module = exportDecl.getModuleSpecifier();
                    ExportEntriesForModule(exportDecl.getExportClause(), module, entries);
                    break;
                }
                case Local:
                    ExportEntriesForModule(exportDecl.getExportClause(), null, entries);
                    break;
                case Variable:
                    for (Name name : BoundNames(exportDecl.getVariableStatement())) {
                        String id = name.getIdentifier();
                        entries.add(new ExportEntry(item, null, null, id, id));
                    }
                    break;
                case Declaration:
                    for (Name name : BoundNames(exportDecl.getDeclaration())) {
                        String id = name.getIdentifier();
                        entries.add(new ExportEntry(item, null, null, id, id));
                    }
                    break;
                case DefaultHoistableDeclaration: {
                    Name localName = BoundName(exportDecl.getHoistableDeclaration());
                    entries.add(new ExportEntry(item, null, null, localName.getIdentifier(),
                            "default"));
                    break;
                }
                case DefaultClassDeclaration: {
                    Name localName = BoundName(exportDecl.getClassDeclaration());
                    entries.add(new ExportEntry(item, null, null, localName.getIdentifier(),
                            "default"));
                    break;
                }
                case DefaultExpression: {
                    Name localName = BoundName(exportDecl.getExpression().getBinding());
                    entries.add(new ExportEntry(item, null, null, localName.getIdentifier(),
                            "default"));
                    break;
                }
                default:
                    throw new AssertionError();
                }
            }
        }
        return entries;
    }

    /**
     * 15.2.3.6 Static Semantics: ExportEntriesForModule
     * 
     * @param node
     *            the exports clause node
     * @param module
     *            the module name
     * @param entries
     *            the list of export entries
     */
    private static void ExportEntriesForModule(ExportClause node, String module,
            List<ExportEntry> entries) {
        if (module == null) {
            assert node.getDefaultEntry() == null;
            assert node.getNameSpace() == null;
            for (ExportSpecifier specifier : node.getExports()) {
                String localName = specifier.getSourceName();
                String importName = null;
                String exportName = specifier.getExportName();
                entries.add(new ExportEntry(specifier, module, importName, localName, exportName));
            }
        } else {
            IdentifierName defaultEntry = node.getDefaultEntry();
            if (defaultEntry != null) {
                String exportName = defaultEntry.getName();
                entries.add(new ExportEntry(defaultEntry, module, "default", null, exportName));
            }
            IdentifierName nameSpace = node.getNameSpace();
            if (nameSpace != null) {
                String exportName = nameSpace.getName();
                entries.add(new ExportEntry(nameSpace, module, "*", null, exportName));
            }
            for (ExportSpecifier specifier : node.getExports()) {
                String localName = null;
                String importName = specifier.getSourceName();
                String exportName = specifier.getExportName();
                entries.add(new ExportEntry(specifier, module, importName, localName, exportName));
            }
        }
    }

    /**
     * 15.2.1.8 Static Semantics: ImportEntries<br>
     * 15.2.2.3 Static Semantics: ImportEntries
     * 
     * @param node
     *            the module node
     * @return the list of import entries
     */
    public static List<ImportEntry> ImportEntries(Module node) {
        ArrayList<ImportEntry> entries = new ArrayList<>();
        for (ModuleItem item : node.getStatements()) {
            if (item instanceof ImportDeclaration) {
                ImportDeclaration importDecl = (ImportDeclaration) item;
                switch (importDecl.getType()) {
                case ImportFrom: {
                    String module = importDecl.getModuleSpecifier();
                    ImportEntriesForModule(importDecl.getImportClause(), module, entries);
                    break;
                }
                case ImportModule:
                    /* empty */
                    break;
                default:
                    throw new AssertionError();
                }
            }
        }
        return entries;
    }

    /**
     * 15.2.2.4 Static Semantics: ImportEntriesForModule
     * 
     * @param node
     *            the import clause node
     * @param module
     *            the module name
     * @param entries
     *            the list of import entries
     */
    private static void ImportEntriesForModule(ImportClause node, String module,
            List<ImportEntry> entries) {
        BindingIdentifier defaultEntry = node.getDefaultEntry();
        if (defaultEntry != null) {
            String localName = defaultEntry.getName().getIdentifier();
            entries.add(new ImportEntry(defaultEntry, module, "default", localName));
        }
        BindingIdentifier nameSpace = node.getNameSpace();
        if (nameSpace != null) {
            String localName = nameSpace.getName().getIdentifier();
            entries.add(new ImportEntry(nameSpace, module, "*", localName));
        }
        for (ImportSpecifier specifier : node.getNamedImports()) {
            String importName = specifier.getImportName();
            String localName = specifier.getLocalName().getName().getIdentifier();
            entries.add(new ImportEntry(specifier, module, importName, localName));
        }
    }

    /**
     * 15.2.1.9 Static Semantics: ImportedLocalNames ( importEntries )
     * 
     * @param importEntries
     *            the list of import entry records
     * @return the list of local names
     */
    public static Map<String, ImportEntry> ImportedLocalNames(List<ImportEntry> importEntries) {
        HashMap<String, ImportEntry> localNames = new HashMap<>();
        for (ImportEntry importEntry : importEntries) {
            localNames.put(importEntry.getLocalName(), importEntry);
        }
        return localNames;
    }

    /**
     * 15.2.1.10 Static Semantics: ModuleRequests<br>
     * 15.2.2.5 Static Semantics: ModuleRequests<br>
     * 15.2.3.9 Static Semantics: ModuleRequests
     * 
     * @param node
     *            the module node
     * @return the ordered set of module requests
     */
    public static Set<String> ModuleRequests(Module node) {
        LinkedHashSet<String> requests = new LinkedHashSet<>();
        for (ModuleItem item : node.getStatements()) {
            String moduleSpecifier;
            if (item instanceof ExportDeclaration) {
                ExportDeclaration export = (ExportDeclaration) item;
                switch (export.getType()) {
                case All:
                case External:
                    moduleSpecifier = export.getModuleSpecifier();
                    break;
                case Local:
                case Variable:
                case Declaration:
                case DefaultHoistableDeclaration:
                case DefaultClassDeclaration:
                case DefaultExpression:
                    continue;
                default:
                    throw new AssertionError();
                }
            } else if (item instanceof ImportDeclaration) {
                moduleSpecifier = ((ImportDeclaration) item).getModuleSpecifier();
            } else {
                continue;
            }
            assert moduleSpecifier != null;
            requests.add(moduleSpecifier);
        }
        return requests;
    }

    /**
     * 15.2.1.11 Static Semantics: LexicallyDeclaredNames
     * 
     * @param node
     *            the module node
     * @return the set of lexically declared names
     */
    public static Set<Name> LexicallyDeclaredNames(Module node) {
        return node.getScope().lexicallyDeclaredNames();
    }

    /**
     * 15.2.1.12 Static Semantics: LexicallyScopedDeclarations
     * 
     * @param node
     *            the module node
     * @return the list of lexically scoped declarations
     */
    public static List<Declaration> LexicallyScopedDeclarations(Module node) {
        return node.getScope().lexicallyScopedDeclarations();
    }

    /**
     * 15.2.1.13 Static Semantics: VarDeclaredNames
     * 
     * @param node
     *            the module node
     * @return the set of variable declared names
     */
    public static Set<Name> VarDeclaredNames(Module node) {
        return node.getScope().varDeclaredNames();
    }

    /**
     * 15.2.1.14 Static Semantics: VarScopedDeclarations
     * 
     * @param node
     *            the module node
     * @return the list of variable scoped declarations
     */
    public static List<StatementListItem> VarScopedDeclarations(Module node) {
        return node.getScope().varScopedDeclarations();
    }

    /**
     * 12.2.5.6 Static Semantics: PropName<br>
     * 14.3.6 Static Semantics: PropName
     * 
     * @param node
     *            the property definition
     * @return the property name string
     */
    public static String PropName(PropertyDefinition node) {
        return node.getPropertyName().getName();
    }

    /**
     * 12.2.5.6 Static Semantics: PropName
     * 
     * @param node
     *            the property name node
     * @return the property name string
     */
    public static String PropName(PropertyName node) {
        return node.getName();
    }

    /**
     * 14.3.8 Static Semantics: SpecialMethod
     * 
     * @param node
     *            the method node
     * @return {@code true} if <var>node</var> is not a simple function
     */
    public static boolean SpecialMethod(MethodDefinition node) {
        switch (node.getType()) {
        case AsyncFunction:
        case AsyncGenerator:
        case ConstructorGenerator:
        case Generator:
        case Getter:
        case Setter:
            return true;
        case BaseConstructor:
        case DerivedConstructor:
        case CallConstructor:
        case Function:
            return false;
        default:
            throw new AssertionError();
        }
    }

    /**
     * Returns {@code true} if any method definition of {@code node} has a decorator.
     * 
     * @param node
     *            the object literal
     * @return {@code true} if a decorator was found
     */
    public static boolean HasDecorators(ObjectLiteral node) {
        for (PropertyDefinition property : node.getProperties()) {
            if (!(property instanceof MethodDefinition)) {
                continue;
            }
            if (!((MethodDefinition) property).getDecorators().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if any method definition of {@code node} has a decorator.
     * 
     * @param node
     *            the class definition
     * @return {@code true} if a decorator was found
     */
    public static boolean HasDecorators(ClassDefinition node) {
        for (PropertyDefinition property : node.getProperties()) {
            if (property instanceof MethodDefinition
                    && !((MethodDefinition) property).getDecorators().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 12.2.8.1 Static Semantics: TemplateStrings
     * 
     * @param node
     *            the template literal
     * @return the list of template literal strings
     */
    public static List<TemplateCharacters> TemplateStrings(TemplateLiteral node) {
        List<Expression> elements = node.getElements();
        assert (elements.size() & 1) == 1;
        int numChars = ((elements.size() / 2) + 1);
        ArrayList<TemplateCharacters> strings = new ArrayList<>(numChars);
        for (int i = 0, size = elements.size(); i < size; ++i) {
            if ((i & 1) == 1) {
                assert !(elements.get(i) instanceof TemplateCharacters);
                continue;
            }
            strings.add((TemplateCharacters) elements.get(i));
        }
        return strings;
    }

    /**
     * Static Semantics: Substitutions (not in spec)
     * 
     * @param node
     *            the template literal
     * @return the list of template literal substitutions
     */
    public static List<Expression> Substitutions(TemplateLiteral node) {
        List<Expression> elements = node.getElements();
        assert (elements.size() & 1) == 1;
        int numSubst = (elements.size() / 2);
        ArrayList<Expression> substitutions = new ArrayList<>(numSubst);
        for (int i = 0, size = elements.size(); i < size; ++i) {
            if ((i & 1) == 0) {
                assert (elements.get(i) instanceof TemplateCharacters);
                continue;
            }
            substitutions.add(elements.get(i));
        }
        return substitutions;
    }

    /**
     * Static Semantics: TailCallNodes (not in spec)
     * <p>
     * 14.6 Tail Position Calls
     * <ul>
     * <li>14.6.1 Static Semantics: IsInTailPosition(nonterminal)
     * <li>14.6.2 Static Semantics: HasProductionInTailPosition
     * </ul>
     * 
     * @param expr
     *            the return expression
     * @return the set of tail call nodes
     */
    public static Set<Expression> TailCallNodes(Expression expr) {
        while (expr instanceof CommaExpression) {
            expr = last(((CommaExpression) expr).getOperands());
        }
        if (IsTailCallExpression(expr)) {
            return singleton(expr);
        }
        if (expr instanceof ConditionalExpression || IsLogicalExpression(expr)) {
            HashSet<Expression> tail = new HashSet<>(8);
            for (ArrayDeque<Expression> queue = new ArrayDeque<>(singleton(expr)); !queue.isEmpty();) {
                Expression e = queue.remove();
                while (e instanceof CommaExpression) {
                    e = last(((CommaExpression) e).getOperands());
                }
                if (IsTailCallExpression(e)) {
                    tail.add(e);
                } else if (e instanceof ConditionalExpression) {
                    queue.add(((ConditionalExpression) e).getThen());
                    queue.add(((ConditionalExpression) e).getOtherwise());
                } else if (IsLogicalExpression(e)) {
                    queue.add(((BinaryExpression) e).getRight());
                }
            }
            return tail;
        }
        return emptySet();
    }

    private static boolean IsLogicalExpression(Expression expr) {
        if (expr instanceof BinaryExpression) {
            BinaryExpression.Operator op = ((BinaryExpression) expr).getOperator();
            return op == BinaryExpression.Operator.AND || op == BinaryExpression.Operator.OR;
        }
        return false;
    }

    private static boolean IsTailCallExpression(Expression expr) {
        return expr instanceof CallExpression || expr instanceof TemplateCallExpression;
    }

    private static <T> T last(List<T> list) {
        assert !list.isEmpty();
        return list.get(list.size() - 1);
    }
}
