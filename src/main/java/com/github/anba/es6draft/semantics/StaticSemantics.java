/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import static com.github.anba.es6draft.semantics.StaticSemanticsVisitor.forEach;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.runtime.internal.SmallArrayList;

/**
 * <h1>Static Semantics</h1>
 * <ul>
 * <li>BoundNames</li>
 * <li>ConstructorMethod</li>
 * <li>Elision Width</li>
 * <li>ExpectedArgumentCount</li>
 * <li>HasInitialiser</li>
 * <li>IsConstantDeclaration</li>
 * <li>IsInvalidAssignmentPattern</li>
 * <li>IsValidSimpleAssignmentTarget</li>
 * <li>LexicalDeclarations</li>
 * <li>LexicallyDeclaredNames</li>
 * <li>MethodDefinitions</li>
 * <li>PropName</li>
 * <li>PropNameList</li>
 * <li>SpecialMethod</li>
 * <li>VarDeclaredNames</li>
 * </ul>
 */
public final class StaticSemantics {
    private StaticSemantics() {
    }

    /**
     * Static Semantics: BoundNames
     */
    public static List<String> BoundNames(Binding node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
    }

    /**
     * Static Semantics: BoundNames
     */
    public static List<String> BoundNames(FormalParameter node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
    }

    /**
     * Static Semantics: BoundNames
     */
    public static List<String> BoundNames(Declaration node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
    }

    /**
     * Static Semantics: BoundNames
     */
    public static List<String> BoundNames(StatementListItem node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
    }

    /**
     * Static Semantics: BoundNames<br>
     */
    public static List<String> BoundNames(FormalParameterList formals) {
        List<String> result = new SmallArrayList<>();
        forEach(BoundNames.INSTANCE, formals, result);
        return result;
    }

    /**
     * Static Semantics: ConstructorMethod
     */
    public static MethodDefinition ConstructorMethod(ClassDefinition node) {
        for (MethodDefinition m : node.getBody()) {
            if ("constructor".equals(PropName(m))) {
                return m;
            }
        }
        return null;
    }

    /**
     * Static Semantics: ExpectedArgumentCount
     */
    public static int ExpectedArgumentCount(FormalParameterList formals) {
        int count = 0;
        for (FormalParameter formal : formals) {
            if (formal instanceof BindingRestElement) {
                break;
            } else if (HasInitialiser((BindingElement) formal)) {
                break;
            }
            count += 1;
        }
        return count;
    }

    /**
     * Static Semantics: HasInitialiser
     */
    static boolean HasInitialiser(BindingElement node) {
        return node.getInitialiser() != null;
    }

    /**
     * Static Semantics: IsConstantDeclaration
     */
    public static boolean IsConstantDeclaration(Declaration node) {
        return node.isConstDeclaration();
    }

    /**
     * Static Semantics: IsSimpleParameterList
     */
    public static boolean IsSimpleParameterList(FormalParameterList formals) {
        return StaticSemanticsVisitor.every(IsSimpleParameterList.INSTANCE, formals, null);
    }

    /**
     * Static Semantics: IsStrict
     */
    public static boolean IsStrict(Script node) {
        return node.isStrict();
    }

    /**
     * Static Semantics: IsStrict
     */
    public static boolean IsStrict(FunctionNode node) {
        return node.isStrict();
    }

    /**
     * Static Semantics: LexicalDeclarations
     */
    public static List<Declaration> LexicalDeclarations(BlockStatement node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * Static Semantics: LexicalDeclarations
     */
    public static List<Declaration> LexicalDeclarations(SwitchStatement node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 14.1 Script
     * <p>
     * Static Semantics: LexicallyDeclaredNames
     */
    public static Set<String> LexicallyDeclaredNames(Script node) {
        return emptyIfNull(node.getScope().lexicallyDeclaredNames());
    }

    /**
     * 13.1 Function Definitions
     * <p>
     * Static Semantics: LexicallyScopedDeclarations (FIXME: missing in spec!)
     */
    public static List<Declaration> LexicallyScopedDeclarations(FunctionNode node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 14.1 Script
     * <p>
     * Static Semantics: LexicallyScopedDeclarations
     */
    public static List<Declaration> LexicallyScopedDeclarations(Script node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 13.1 Function Definitions
     * <p>
     * Static Semantics: VarDeclaredNames (FunctionBody -> StatementList)
     */
    public static Set<String> VarDeclaredNames(FunctionNode node) {
        return emptyIfNull(node.getScope().varDeclaredNames());
    }

    /**
     * 14.1 Script
     * <p>
     * Static Semantics: VarDeclaredNames
     */
    public static Set<String> VarDeclaredNames(Script node) {
        return emptyIfNull(node.getScope().varDeclaredNames());
    }

    /**
     * 13.1 Function Definitions
     * <p>
     * Static Semantics: VarScopedDeclarations (FIXME: missing in spec!)
     */
    public static List<StatementListItem> VarScopedDeclarations(FunctionNode node) {
        return emptyIfNull(node.getScope().varScopedDeclarations());
    }

    /**
     * 14.1 Script
     * <p>
     * Static Semantics: VarScopedDeclarations
     */
    public static List<StatementListItem> VarScopedDeclarations(Script node) {
        return emptyIfNull(node.getScope().varScopedDeclarations());
    }

    /**
     * Static Semantics: MethodDefinitions
     */
    public static List<MethodDefinition> MethodDefinitions(ClassDefinition node) {
        return node.getBody();
    }

    /**
     * Static Semantics: NumberOfParameters (FIXME: not yet defined in spec!)
     */
    public static int NumberOfParameters(FormalParameterList formals) {
        int count = 0;
        for (FormalParameter formal : formals) {
            if (formal instanceof BindingRestElement) {
                break;
            }
            count += 1;
        }
        return count;
    }

    /**
     * Static Semantics: PropName
     */
    public static String PropName(PropertyDefinition node) {
        return node.getPropertyName().getName();
    }

    /**
     * Static Semantics: PropName
     */
    public static String PropName(PropertyName node) {
        return node.getName();
    }

    /**
     * Static Semantics: SpecialMethod
     */
    public static boolean SpecialMethod(MethodDefinition node) {
        switch (node.getType()) {
        case Generator:
        case Getter:
        case Setter:
            return true;
        case Function:
        default:
            return false;
        }
    }

    //

    private static <T> Set<T> emptyIfNull(Set<T> list) {
        return (list != null ? list : Collections.<T> emptySet());
    }

    private static <T> List<T> emptyIfNull(List<T> list) {
        return (list != null ? list : Collections.<T> emptyList());
    }
}
