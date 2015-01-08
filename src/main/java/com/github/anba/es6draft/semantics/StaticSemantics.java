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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
 * <li>BoundNames</li>
 * <li>ConstructorMethod</li>
 * <li>ContainsExpression</li>
 * <li>ExpectedArgumentCount</li>
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
 * <li>LexicallyDeclaredNames</li>
 * <li>LexicallyScopedDeclarations</li>
 * <li>MethodDefinitions</li>
 * <li>ModuleRequests</li>
 * <li>PropName</li>
 * <li>SpecialMethod</li>
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
     * <li>12.1.3 Static Semantics: BoundNames
     * <li>13.2.3.1 Static Semantics: BoundNames
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
        return node.accept(BoundNames.INSTANCE, new InlineArrayList<Name>());
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
        return node.accept(BoundNames.INSTANCE, new InlineArrayList<Name>());
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
        InlineArrayList<Name> list = new InlineArrayList<Name>();
        node.accept(BoundNames.INSTANCE, list);
        return list;
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
        InlineArrayList<Name> list = new InlineArrayList<Name>();
        for (FormalParameter formalParameter : formals) {
            formalParameter.accept(BoundNames.INSTANCE, list);
        }
        return list;
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
    public static List<Name> BoundNames(Declaration node) {
        return node.accept(BoundNames.INSTANCE, new InlineArrayList<Name>());
    }

    /**
     * 13.2.2.1 Static Semantics: BoundNames
     * 
     * @param node
     *            the variable statement
     * @return the bound names
     */
    public static List<Name> BoundNames(VariableStatement node) {
        return node.accept(BoundNames.INSTANCE, new InlineArrayList<Name>());
    }

    /**
     * 15.2.1.2 Static Semantics: BoundNames
     * 
     * @param node
     *            the import declaration
     * @return the bound names
     */
    public static List<Name> BoundNames(ImportDeclaration node) {
        return node.accept(BoundNames.INSTANCE, new InlineArrayList<Name>());
    }

    /**
     * 15.2.2.1 Static Semantics: BoundNames
     * 
     * @param node
     *            the export declaration
     * @return the bound names
     */
    public static List<Name> BoundNames(ExportDeclaration node) {
        return node.accept(BoundNames.INSTANCE, new InlineArrayList<Name>());
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
            if (!m.isStatic() && "constructor".equals(m.getPropertyName().getName())) {
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
        return formals.containsExpression();
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
    public static boolean ContainsExpression(List<FormalParameter> formals) {
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
            }
            if (HasInitializer((BindingElement) formal)) {
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
        if (node instanceof AsyncArrowFunction) {
            return !HasName((AsyncArrowFunction) node);
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
        return node.getIdentifier() != null;
    }

    /**
     * Static Semantics: HasName
     * 
     * @param node
     *            the function node
     * @return {@code true} if the function has a binding name
     */
    public static boolean HasName(AsyncArrowFunction node) {
        return false;
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
        if (node instanceof AsyncArrowFunction) {
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
        return node instanceof IdentifierReference && !node.isParenthesized();
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
        return formals.isSimpleParameterList();
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
    public static boolean IsSimpleParameterList(List<FormalParameter> formals) {
        for (FormalParameter formal : formals) {
            if (formal instanceof BindingRestElement) {
                return false;
            }
            if (HasInitializer((BindingElement) formal)) {
                return false;
            }
            if (((BindingElement) formal).getBinding() instanceof BindingPattern) {
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
        if (lhs instanceof IdentifierReference) {
            String name = ((IdentifierReference) lhs).getName();
            if (strict && ("eval".equals(name) || "arguments".equals(name))) {
                return false;
            }
            return true;
        }
        if (lhs instanceof ElementAccessor) {
            return true;
        }
        if (lhs instanceof PropertyAccessor) {
            return true;
        }
        if (lhs instanceof SuperElementAccessor) {
            return true;
        }
        if (lhs instanceof SuperPropertyAccessor) {
            return true;
        }
        return false;
    }

    /**
     * 13.1.2 Static Semantics: LexicallyScopedDeclarations
     * 
     * @param node
     *            the block statement node
     * @return the list of lexically scoped declarations
     */
    public static List<Declaration> LexicallyScopedDeclarations(BlockStatement node) {
        return node.getScope().lexicallyScopedDeclarations();
    }

    /**
     * 13.11.2 Static Semantics: LexicallyScopedDeclarations
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
     * <li>14.1.14 Static Semantics: LexicallyScopedDeclarations
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
     * 13.1.3 Static Semantics: LexicallyDeclaredNames
     * 
     * @param node
     *            the block statement node
     * @return the set of lexically declared names
     */
    public static Set<Name> LexicallyDeclaredNames(BlockStatement node) {
        return node.getScope().lexicallyDeclaredNames();
    }

    /**
     * 13.11.3 Static Semantics: LexicalDeclarations
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
     * <li>14.1.15 Static Semantics: LexicallyDeclaredNames
     * <li>14.2.12 Static Semantics: LexicallyDeclaredNames
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
     * <li>14.1.18 Static Semantics: VarScopedDeclarations
     * <li>14.2.15 Static Semantics: VarScopedDeclarations
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
                    entries.add(new ExportEntry(module, "*", null, null, item.getBeginPosition()));
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
                    for (Name name : BoundNames(exportDecl.getVariableStatement())) {
                        String id = name.getIdentifier();
                        entries.add(new ExportEntry(null, null, id, id, item.getBeginPosition()));
                    }
                    break;
                case Declaration:
                    for (Name name : BoundNames(exportDecl.getDeclaration())) {
                        String id = name.getIdentifier();
                        entries.add(new ExportEntry(null, null, id, id, item.getBeginPosition()));
                    }
                    break;
                case DefaultHoistableDeclaration: {
                    Name localName = BoundName(exportDecl.getHoistableDeclaration());
                    entries.add(new ExportEntry(null, null, localName.getIdentifier(), "default",
                            item.getBeginPosition()));
                    break;
                }
                case DefaultClassDeclaration: {
                    Name localName = BoundName(exportDecl.getClassDeclaration());
                    entries.add(new ExportEntry(null, null, localName.getIdentifier(), "default",
                            item.getBeginPosition()));
                    break;
                }
                case DefaultExpression: {
                    Name localName = BoundName(exportDecl.getExpression().getBinding());
                    entries.add(new ExportEntry(null, null, localName.getIdentifier(), "default",
                            item.getBeginPosition()));
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
     * 15.2.3.5 Static Semantics: ExportEntriesForModule
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
            for (ExportSpecifier specifier : node.getExports()) {
                String localName = specifier.getSourceName();
                String importName = null;
                String exportName = specifier.getExportName();
                long position = specifier.getBeginPosition();
                entries.add(new ExportEntry(module, importName, localName, exportName, position));
            }
        } else {
            for (ExportSpecifier specifier : node.getExports()) {
                String localName = null;
                String importName = specifier.getSourceName();
                String exportName = specifier.getExportName();
                long position = specifier.getBeginPosition();
                entries.add(new ExportEntry(module, importName, localName, exportName, position));
            }
        }
    }

    /**
     * 15.2.1.6 Static Semantics: ImportEntries<br>
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
        if (node.getDefaultEntry() != null) {
            String localName = node.getDefaultEntry().getName().getIdentifier();
            entries.add(new ImportEntry(module, "default", localName, node.getBeginPosition()));
        }
        if (node.getNameSpace() != null) {
            String localName = node.getNameSpace().getName().getIdentifier();
            entries.add(new ImportEntry(module, "*", localName, node.getBeginPosition()));
        }
        for (ImportSpecifier specifier : node.getNamedImports()) {
            String importName = specifier.getImportName();
            String localName = specifier.getLocalName().getName().getIdentifier();
            entries.add(new ImportEntry(module, importName, localName, node.getBeginPosition()));
        }
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
        LinkedHashSet<String> requests = new LinkedHashSet<>();
        for (ModuleItem item : node.getStatements()) {
            if (item instanceof ExportDeclaration) {
                ExportDeclaration export = (ExportDeclaration) item;
                switch (export.getType()) {
                case All:
                case External: {
                    String moduleSpecifier = export.getModuleSpecifier();
                    assert moduleSpecifier != null;
                    requests.add(moduleSpecifier);
                    break;
                }
                case Local:
                case Variable:
                case Declaration:
                case DefaultHoistableDeclaration:
                case DefaultClassDeclaration:
                case DefaultExpression:
                default:
                    break;
                }
            } else if (item instanceof ImportDeclaration) {
                String moduleSpecifier = ((ImportDeclaration) item).getModuleSpecifier();
                assert moduleSpecifier != null;
                requests.add(moduleSpecifier);
            }
        }
        return requests;
    }

    /**
     * 15.2.0.10 Static Semantics: LexicallyDeclaredNames
     * 
     * @param node
     *            the module node
     * @return the set of lexically declared names
     */
    public static Set<Name> LexicallyDeclaredNames(Module node) {
        return node.getScope().lexicallyDeclaredNames();
    }

    /**
     * 15.2.0.11 Static Semantics: LexicallyScopedDeclarations
     * 
     * @param node
     *            the module node
     * @return the list of lexically scoped declarations
     */
    public static List<Declaration> LexicallyScopedDeclarations(Module node) {
        return node.getScope().lexicallyScopedDeclarations();
    }

    /**
     * 15.2.0.13 Static Semantics: VarDeclaredNames
     * 
     * @param node
     *            the module node
     * @return the set of variable declared names
     */
    public static Set<Name> VarDeclaredNames(Module node) {
        return node.getScope().varDeclaredNames();
    }

    /**
     * 15.2.0.14 Static Semantics: VarScopedDeclarations
     * 
     * @param node
     *            the module node
     * @return the list of variable scoped declarations
     */
    public static List<StatementListItem> VarScopedDeclarations(Module node) {
        return node.getScope().varScopedDeclarations();
    }

    /**
     * 14.5.x Static Semantics: MethodDefinitions
     * 
     * @param node
     *            the class definition
     * @return the list of class methods
     */
    public static List<MethodDefinition> MethodDefinitions(ClassDefinition node) {
        return node.getMethods();
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
     * 14.6 Tail Position Calls
     * <ul>
     * <li>14.6.1 Static Semantics: IsInTailPosition(nonterminal) Abstract Operation
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
        return expr instanceof CallExpression || expr instanceof NewExpression
                || expr instanceof TemplateCallExpression || expr instanceof SuperCallExpression
                || expr instanceof SuperNewExpression;
    }

    private static <T> T last(List<T> list) {
        assert !list.isEmpty();
        return list.get(list.size() - 1);
    }
}
