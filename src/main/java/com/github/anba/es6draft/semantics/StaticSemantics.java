/**
 * Copyright (c) 2012-2014 André Bargull
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.runtime.internal.SmallArrayList;
import com.github.anba.es6draft.runtime.modules.ExportEntry;
import com.github.anba.es6draft.runtime.modules.ImportEntry;

/**
 * <h1>Static Semantics</h1>
 * <ul>
 * <li>BoundNames</li>
 * <li>ConstructorMethod</li>
 * <li>ContainsExpression</li>
 * <li>DeclaredNames</li>
 * <li>ExpectedArgumentCount</li>
 * <li>ExportedBindings</li>
 * <li>ExportEntries</li>
 * <li>HasInitializer</li>
 * <li>HasName</li>
 * <li>ImportedBindings</li>
 * <li>ImportEntries</li>
 * <li>IsAnonymousFunctionDefinition</li>
 * <li>IsConstantDeclaration</li>
 * <li>IsFunctionDefinition</li>
 * <li>IsIdentifierRef</li>
 * <li>IsSimpleParameterList</li>
 * <li>IsStrict</li>
 * <li>IsValidSimpleAssignmentTarget</li>
 * <li>LexicalDeclarations</li>
 * <li>LexicallyDeclaredNames</li>
 * <li>LexicallyScopedDeclarations</li>
 * <li>ModuleRequests</li>
 * <li>PropName</li>
 * <li>PrototypeMethodDefinitions</li>
 * <li>SpecialMethod</li>
 * <li>StaticMethodDefinitions</li>
 * <li>UnknownExportEntries</li>
 * <li>VarDeclaredNames</li>
 * <li>VarScopedDeclarations</li>
 * </ul>
 */
public final class StaticSemantics {
    private StaticSemantics() {
    }

    /**
     * Static Semantics: BoundNames
     * <ul>
     * <li>12.1.3 Static Semantics: BoundNames
     * <li>13.2.3.1 Static Semantics: BoundNames
     * </ul>
     * 
     * @param node
     *            the binding node
     * @return the bound names
     */
    public static List<String> BoundNames(Binding node) {
        if (node instanceof BindingIdentifier) {
            return singletonList(((BindingIdentifier) node).getName());
        }
        assert node instanceof BindingPattern;
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
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
    public static List<String> BoundNames(FormalParameterList node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
    }

    /**
     * Static Semantics: BoundNames
     * <ul>
     * <li>13.2.1.2 Static Semantics: BoundNames
     * <li>14.1.3 Static Semantics: BoundNames
     * <li>14.4.2 Static Semantics: BoundNames
     * <li>14.5.2 Static Semantics: BoundNames
     * </ul>
     * 
     * @param node
     *            the declaration node
     * @return the bound names
     */
    public static List<String> BoundNames(Declaration node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
    }

    /**
     * 13.2.2.1 Static Semantics: BoundNames
     * 
     * @param node
     *            the variable statement
     * @return the bound names
     */
    public static List<String> BoundNames(VariableStatement node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
    }

    /**
     * 15.2.1.2 Static Semantics: BoundNames
     * 
     * @param node
     *            the import declaration
     * @return the bound names
     */
    public static List<String> BoundNames(ImportDeclaration node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
    }

    /**
     * 15.2.2.1 Static Semantics: BoundNames
     * 
     * @param node
     *            the export declaration
     * @return the bound names
     */
    public static List<String> BoundNames(ExportDeclaration node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
    }

    /**
     * 14.5.3 Static Semantics: ConstructorMethod
     * 
     * @param node
     *            the class definition node
     * @return the constructor method if found, otherwise {@code null}
     */
    public static MethodDefinition ConstructorMethod(ClassDefinition node) {
        for (MethodDefinition m : PrototypeMethodDefinitions(node)) {
            if ("constructor".equals(PropName(m))) {
                return m;
            }
        }
        return null;
    }

    /**
     * Static Semantics: ContainsExpression
     * <ul>
     * <li>13.2.3.2 Static Semantics: ContainsExpression
     * <li>14.1.5 Static Semantics: ContainsExpression
     * <li>14.2.4 Static Semantics: ContainsExpression
     * </ul>
     * 
     * @param formals
     *            the formal parameters
     * @return {@code true} if an expression was found
     */
    public static boolean ContainsExpression(FormalParameterList formals) {
        for (FormalParameter formal : formals) {
            if (formal instanceof BindingElement && ContainsExpression((BindingElement) formal)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 13.2.3.2 Static Semantics: ContainsExpression
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
     * 13.2.3.2 Static Semantics: ContainsExpression
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
     * 13.2.3.2 Static Semantics: ContainsExpression
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
     * 13.2.3.2 Static Semantics: ContainsExpression
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
     * <li>14.2.6 Static Semantics: ExpectedArgumentCount
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
            if (formal instanceof BindingRestElement) {
                break;
            } else if (HasInitializer((BindingElement) formal)) {
                break;
            }
            count += 1;
        }
        return count;
    }

    /**
     * 13.2.3.3 Static Semantics: HasInitializer
     * 
     * @param node
     *            the binding element
     * @return {@code true} if the binding element has an initializer
     */
    private static boolean HasInitializer(BindingElement node) {
        return node.getInitializer() != null;
    }

    /**
     * 14.1.9 Static Semantics: IsAnonymousFunctionDefinition (production) Abstract Operation
     * 
     * @param node
     *            the expression node
     * @return {@code true} if the node is an anonymous function definition
     */
    public static boolean IsAnonymousFunctionDefinition(Expression node) {
        if (!IsFunctionDefinition(node)) {
            return false;
        }
        if (node instanceof ArrowFunction) {
            return !HasName((ArrowFunction) node);
        }
        if (node instanceof FunctionExpression) {
            return !HasName((FunctionExpression) node);
        }
        if (node instanceof GeneratorExpression) {
            return !HasName((GeneratorExpression) node);
        }
        if (node instanceof ClassExpression) {
            return !HasName((ClassExpression) node);
        }
        if (node instanceof AsyncFunctionExpression) {
            return !HasName((AsyncFunctionExpression) node);
        }
        throw new AssertionError();
    }

    /**
     * 14.1.8 Static Semantics: HasName
     * 
     * @param node
     *            the function node
     * @return {@code true} if the function has a binding name
     */
    public static boolean HasName(FunctionExpression node) {
        return node.getIdentifier() != null;
    }

    /**
     * 14.2.9 Static Semantics: HasName
     * 
     * @param node
     *            the function node
     * @return {@code true} if the function has a binding name
     */
    public static boolean HasName(ArrowFunction node) {
        return false;
    }

    /**
     * 14.4.6 Static Semantics: HasName
     * 
     * @param node
     *            the function node
     * @return {@code true} if the function has a binding name
     */
    public static boolean HasName(GeneratorExpression node) {
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
        return node.getName() != null;
    }

    /**
     * Static Semantics: HasName
     * 
     * @param node
     *            the function node
     * @return {@code true} if the function has a binding name
     */
    public static boolean HasName(AsyncFunctionExpression node) {
        return node.getIdentifier() != null;
    }

    /**
     * Static Semantics: IsConstantDeclaration
     * <ul>
     * <li>13.2.1.3 Static Semantics: IsConstantDeclaration
     * <li>14.1.10 Static Semantics: IsConstantDeclaration
     * <li>14.4.7 Static Semantics: IsConstantDeclaration
     * <li>14.5.7 Static Semantics: IsConstantDeclaration
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
     * <li>12.2.0.2 Static Semantics: IsFunctionDefinition
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
     * <li>14.4.8 Static Semantics: IsFunctionDefinition
     * <li>14.5.8 Static Semantics: IsFunctionDefinition
     * </ul>
     * 
     * @param node
     *            the expression node
     * @return {@code true} if the node is a function definition
     */
    public static boolean IsFunctionDefinition(Expression node) {
        if (node instanceof ArrowFunction) {
            return true;
        }
        if (node instanceof FunctionExpression) {
            return true;
        }
        if (node instanceof GeneratorExpression) {
            return true;
        }
        if (node instanceof ClassExpression) {
            return true;
        }
        if (node instanceof AsyncFunctionExpression) {
            return true;
        }
        return false;
    }

    /**
     * Static Semantics: IsIdentifierRef
     * <ul>
     * <li>12.2.0.3 Static Semantics: IsIdentifierRef
     * <li>12.3.1.3 Static Semantics: IsIdentifierRef
     * </ul>
     * 
     * @param node
     *            the left-hand side expression node
     * @return {@code true} if node is an identifier
     */
    public static boolean IsIdentifierRef(LeftHandSideExpression node) {
        return node instanceof Identifier && !node.isParenthesized();
    }

    /**
     * Static Semantics: IsSimpleParameterList
     * <ul>
     * <li>14.1.12 Static Semantics: IsSimpleParameterList
     * <li>14.2.10 Static Semantics: IsSimpleParameterList
     * </ul>
     * 
     * @param formals
     *            the formal parameters list
     * @return {@code true} if the list is a simple formal parameters list
     */
    public static boolean IsSimpleParameterList(FormalParameterList formals) {
        for (FormalParameter formal : formals) {
            if (formal instanceof BindingRestElement) {
                return false;
            } else if (HasInitializer((BindingElement) formal)) {
                return false;
            } else if (((BindingElement) formal).getBinding() instanceof BindingPattern) {
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
     * 15.2.0.7 Static Semantics: IsStrict
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
     * Static Semantics: IsValidSimpleAssignmentTarget
     * <ul>
     * <li>12.2.0.4 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.2.10.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.3.1.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.4.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.5.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.6.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.7.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.8.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.9.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.10.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.11.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.12.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.13.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.14.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.15.2 Static Semantics: IsValidSimpleAssignmentTarget
     * </ul>
     * 
     * @param lhs
     *            the left-hand side expression
     * @param strict
     *            the strict mode flag
     * @return {@code true} if the left-hand side expression is a simple assignment target
     */
    public static boolean IsValidSimpleAssignmentTarget(Expression lhs, boolean strict) {
        if (lhs instanceof Identifier) {
            String name = ((Identifier) lhs).getName();
            if (strict && ("eval".equals(name) || "arguments".equals(name))) {
                return false;
            }
            return true;
        } else if (lhs instanceof ElementAccessor) {
            return true;
        } else if (lhs instanceof PropertyAccessor) {
            return true;
        } else if (lhs instanceof SuperExpression) {
            SuperExpression superExpr = (SuperExpression) lhs;
            if (superExpr.getType() == SuperExpression.Type.ElementAccessor
                    || superExpr.getType() == SuperExpression.Type.PropertyAccessor) {
                return true;
            }
        }
        return false;
    }

    /**
     * 13.1.2 Static Semantics: LexicalDeclarations
     * 
     * @param node
     *            the block statement node
     * @return the list of lexically scoped declarations
     */
    public static List<Declaration> LexicalDeclarations(BlockStatement node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 13.11.2 Static Semantics: LexicalDeclarations
     * 
     * @param node
     *            the switch statement node
     * @return the list of lexically scoped declarations
     */
    public static List<Declaration> LexicalDeclarations(SwitchStatement node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * Static Semantics: LexicalDeclarations
     * <ul>
     * <li>14.1.14 Static Semantics: LexicalDeclarations
     * <li>14.2.11 Static Semantics: LexicalDeclarations
     * </ul>
     * 
     * @param node
     *            the function node
     * @return the list of lexically scoped declarations
     */
    public static List<Declaration> LexicalDeclarations(FunctionNode node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 15.1.4 Static Semantics: LexicallyScopedDeclarations
     * 
     * @param node
     *            the script node
     * @return the list of lexically scoped declarations
     */
    public static List<Declaration> LexicallyScopedDeclarations(Script node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 13.1.3 Static Semantics: LexicallyDeclaredNames
     * 
     * @param node
     *            the block statement node
     * @return the set of lexically declared names
     */
    public static Set<String> LexicallyDeclaredNames(BlockStatement node) {
        return emptyIfNull(node.getScope().lexicallyDeclaredNames());
    }

    /**
     * 13.11.3 Static Semantics: LexicalDeclarations
     * 
     * @param node
     *            the switch statement node
     * @return the list of lexically scoped declarations
     */
    public static Set<String> LexicallyDeclaredNames(SwitchStatement node) {
        return emptyIfNull(node.getScope().lexicallyDeclaredNames());
    }

    /**
     * Static Semantics: LexicallyDeclaredNames
     * 
     * @param scope
     *            the block scope
     * @return the set of lexically declared names
     */
    public static Set<String> LexicallyDeclaredNames(BlockScope scope) {
        return emptyIfNull(scope.lexicallyDeclaredNames());
    }

    /**
     * Static Semantics: LexicallyDeclaredNames
     * <ul>
     * <li>14.1.15 Static Semantics: LexicallyDeclaredNames
     * <li>14.2.12 Static Semantics: LexicallyDeclaredNames
     * </ul>
     * 
     * @param node
     *            the script node
     * @return the set of lexically declared names
     */
    public static Set<String> LexicallyDeclaredNames(FunctionNode node) {
        return emptyIfNull(node.getScope().lexicallyDeclaredNames());
    }

    /**
     * 15.1.3 Static Semantics: LexicallyDeclaredNames
     * 
     * @param node
     *            the script node
     * @return the set of lexically declared names
     */
    public static Set<String> LexicallyDeclaredNames(Script node) {
        return emptyIfNull(node.getScope().lexicallyDeclaredNames());
    }

    /**
     * Static Semantics: VarDeclaredNames
     * <ul>
     * <li>14.1.17 Static Semantics: VarDeclaredNames
     * <li>14.2.14 Static Semantics: VarDeclaredNames
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
    public static Set<String> VarDeclaredNames(FunctionNode node) {
        return emptyIfNull(node.getScope().varDeclaredNames());
    }

    /**
     * 15.1.5 Static Semantics: VarDeclaredNames
     * 
     * @param node
     *            the script node
     * @return the set of variable declared names
     */
    public static Set<String> VarDeclaredNames(Script node) {
        return emptyIfNull(node.getScope().varDeclaredNames());
    }

    /**
     * Static Semantics: VarScopedDeclarations
     * <ul>
     * <li>14.1.18 Static Semantics: VarScopedDeclarations
     * <li>14.2.15 Static Semantics: VarScopedDeclarations
     * </ul>
     * 
     * @param node
     *            the function node
     * @return the list of variable scoped declarations
     */
    public static List<StatementListItem> VarScopedDeclarations(FunctionNode node) {
        return emptyIfNull(node.getScope().varScopedDeclarations());
    }

    /**
     * 15.1.6 Static Semantics: VarScopedDeclarations
     * 
     * @param node
     *            the script node
     * @return the list of variable scoped declarations
     */
    public static List<StatementListItem> VarScopedDeclarations(Script node) {
        return emptyIfNull(node.getScope().varScopedDeclarations());
    }

    /**
     * 15.2.0.2 Static Semantics: DeclaredNames
     * 
     * @param node
     *            the module node
     * @return the set of declared names
     */
    public static Set<String> DeclaredNames(Module node) {
        HashSet<String> names = new HashSet<>();
        names.addAll(LexicallyDeclaredNames(node));
        names.addAll(VarDeclaredNames(node));
        return names;
    }

    /**
     * 15.2.0.3 Static Semantics: ExportedBindings<br>
     * 15.2.2.2 Static Semantics: ExportedBindings
     * 
     * @param node
     *            the module node
     * @return the set of export bindings
     */
    public static Set<String> ExportedBindings(Module node) {
        return emptyIfNull(node.getScope().getExportBindings());
    }

    /**
     * 15.2.0.4 Static Semantics: ExportEntries<br>
     * 15.2.2.3 Static Semantics: ExportEntries
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
                    entries.add(new ExportEntry(module, "<all>", null, null));
                    break;
                }
                case External: {
                    String module = exportDecl.getModuleSpecifier();
                    ExportEntriesForModule(exportDecl.getExportsClause(), module, entries);
                    break;
                }
                case Local:
                    ExportEntriesForModule(exportDecl.getExportsClause(), null, entries);
                    break;
                case Variable:
                    for (String name : BoundNames(exportDecl.getVariableStatement())) {
                        entries.add(new ExportEntry(null, null, name, name));
                    }
                    break;
                case Declaration:
                    for (String name : BoundNames(exportDecl.getDeclaration())) {
                        entries.add(new ExportEntry(null, null, name, name));
                    }
                    break;
                case Default:
                default:
                    entries.add(new ExportEntry(null, null, "default", "default"));
                    break;
                }
            }
        }
        return entries;
    }

    /**
     * 15.2.2.4 Static Semantics: ExportEntriesForModule
     * 
     * @param node
     *            the exports clause node
     * @param module
     *            the module name
     * @param entries
     *            the list of export entries
     */
    private static void ExportEntriesForModule(ExportsClause node, String module,
            List<ExportEntry> entries) {
        for (ExportSpecifier specifier : node.getExports()) {
            entries.add(new ExportEntry(module, specifier.getImportName(),
                    specifier.getLocalName(), specifier.getExportName()));
        }
    }

    /**
     * 15.2.0.5 Static Semantics: ImportedBindings
     * 
     * @param node
     *            the module node
     * @return the list of imported bindings
     */
    public static List<String> ImportedBindings(Module node) {
        ArrayList<String> bindings = new ArrayList<>();
        for (ModuleItem item : node.getStatements()) {
            if (item instanceof ImportDeclaration) {
                item.accept(BoundNames.INSTANCE, bindings);
            }
        }
        return bindings;
    }

    /**
     * 15.2.0.6 Static Semantics: ImportEntries<br>
     * 15.2.1.3 Static Semantics: ImportEntries
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
                case ModuleImport: {
                    ModuleImport moduleImport = importDecl.getModuleImport();
                    String module = moduleImport.getModuleSpecifier();
                    String localName = moduleImport.getImportedBinding().getName();
                    entries.add(new ImportEntry(module, "default", localName));
                    break;
                }
                case ImportModule:
                default:
                    /* empty */
                    break;
                }
            }
        }
        return entries;
    }

    /**
     * 15.2.1.4 Static Semantics: ImportEntriesForModule
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
        if (node.getDefaultEntry() != null) {
            String localName = node.getDefaultEntry().getName();
            entries.add(new ImportEntry(module, "default", localName));
        }
        for (ImportSpecifier specifier : node.getNamedImports()) {
            String importName = specifier.getImportName();
            String localName = specifier.getLocalName().getName();
            entries.add(new ImportEntry(module, importName, localName));
        }
    }

    /**
     * 15.2.0.8 Static Semantics: KnownExportEntries
     * 
     * @param node
     *            the module node
     * @return the list of known export entries
     */
    public static List<ExportEntry> KnownExportEntries(Module node) {
        ArrayList<ExportEntry> knownExports = new ArrayList<>();
        for (ExportEntry entry : ExportEntries(node)) {
            if (!"<all>".equals(entry.getImportName())) {
                knownExports.add(entry);
            }
        }
        return knownExports;
    }

    /**
     * 15.2.0.9 Static Semantics: ModuleRequests<br>
     * 15.2.1.5 Static Semantics: ModuleRequests<br>
     * 15.2.2.5 Static Semantics: ModuleRequests
     * 
     * @param node
     *            the module node
     * @return the ordered set of module requests
     */
    public static Set<String> ModuleRequests(Module node) {
        return emptyIfNull(node.getScope().getModuleRequests());
    }

    /**
     * 15.2.0.10 Static Semantics: LexicallyDeclaredNames
     * 
     * @param node
     *            the module node
     * @return the set of lexically declared names
     */
    public static Set<String> LexicallyDeclaredNames(Module node) {
        return emptyIfNull(node.getScope().lexicallyDeclaredNames());
    }

    /**
     * 15.2.0.11 Static Semantics: LexicalDeclarations
     * 
     * @param node
     *            the module node
     * @return the list of lexically scoped declarations
     */
    public static List<Declaration> LexicalDeclarations(Module node) {
        // FIXME: Does not include ImportDeclaration nodes! May need to change class structure...
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 15.2.0.12 Static Semantics: UnknownExportEntries
     * 
     * @param node
     *            the module node
     * @return the list of unknown export entries
     */
    public static List<ExportEntry> UnknownExportEntries(Module node) {
        ArrayList<ExportEntry> unknownExports = new ArrayList<>();
        for (ExportEntry entry : ExportEntries(node)) {
            if ("<all>".equals(entry.getImportName())) {
                unknownExports.add(entry);
            }
        }
        return unknownExports;
    }

    /**
     * 15.2.0.13 Static Semantics: VarDeclaredNames
     * 
     * @param node
     *            the module node
     * @return the set of variable declared names
     */
    public static Set<String> VarDeclaredNames(Module node) {
        return emptyIfNull(node.getScope().varDeclaredNames());
    }

    /**
     * 15.2.0.14 Static Semantics: VarScopedDeclarations
     * 
     * @param node
     *            the module node
     * @return the list of variable scoped declarations
     */
    public static List<StatementListItem> VarScopedDeclarations(Module node) {
        return emptyIfNull(node.getScope().varScopedDeclarations());
    }

    /**
     * 14.5.11 Static Semantics: PrototypeMethodDefinitions
     * 
     * @param node
     *            the class definition
     * @return the list of prototype class methods
     */
    public static List<MethodDefinition> PrototypeMethodDefinitions(ClassDefinition node) {
        return node.getPrototypeMethods();
    }

    /**
     * 14.5.15 Static Semantics: StaticMethodDefinitions
     * 
     * @param node
     *            the class definition
     * @return the list of static class methods
     */
    public static List<MethodDefinition> StaticMethodDefinitions(ClassDefinition node) {
        return node.getStaticMethods();
    }

    /**
     * 12.2.5.6 Static Semantics: PropName<br>
     * 14.3.5 Static Semantics: PropName
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
     * 14.3.7 Static Semantics: SpecialMethod
     * 
     * @param node
     *            the method node
     * @return {@code true} if <var>node</var> is not a simple function
     */
    public static boolean SpecialMethod(MethodDefinition node) {
        switch (node.getType()) {
        case AsyncFunction:
        case Generator:
        case Getter:
        case Setter:
            return true;
        case Function:
        default:
            return false;
        }
    }

    /**
     * 12.2.9.1.1 Static Semantics: TemplateStrings
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
     * 14.6.1 Static Semantics: Tail Position<br>
     * 14.6.2 Static Semantics: HasProductionInTailPosition
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
        } else if (expr instanceof ConditionalExpression || IsLogicalExpression(expr)) {
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
        } else {
            return emptySet();
        }
    }

    private static boolean IsLogicalExpression(Expression expr) {
        if (expr instanceof BinaryExpression) {
            BinaryExpression.Operator op = ((BinaryExpression) expr).getOperator();
            return op == BinaryExpression.Operator.AND || op == BinaryExpression.Operator.OR;
        }
        return false;
    }

    private static boolean IsTailCallExpression(Expression expr) {
        return expr instanceof CallExpression
                || expr instanceof TemplateCallExpression
                || (expr instanceof SuperExpression && ((SuperExpression) expr).getType() == SuperExpression.Type.CallExpression)
                || expr instanceof NewExpression;
    }

    //

    private static <T> Set<T> emptyIfNull(Set<T> list) {
        return list != null ? list : Collections.<T> emptySet();
    }

    private static <T> List<T> emptyIfNull(List<T> list) {
        return list != null ? list : Collections.<T> emptyList();
    }

    private static <T> T last(List<T> list) {
        assert !list.isEmpty();
        return list.get(list.size() - 1);
    }
}
