/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import static com.github.anba.es6draft.semantics.StaticSemanticsVisitor.forEach;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.ast.*;

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
        return node.accept(BoundNames.INSTANCE, new ArrayList<String>());
    }

    /**
     * Static Semantics: BoundNames
     */
    public static List<String> BoundNames(FormalParameter node) {
        return node.accept(BoundNames.INSTANCE, new ArrayList<String>());
    }

    /**
     * Static Semantics: BoundNames
     */
    public static List<String> BoundNames(StatementListItem node) {
        return node.accept(BoundNames.INSTANCE, new ArrayList<String>());
    }

    /**
     * Static Semantics: BoundNames<br>
     */
    public static List<String> BoundNames(List<FormalParameter> formals) {
        List<String> result = new ArrayList<>();
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
     * Static Semantics: Elision Width
     */
    public static int ElisionWidth(List<? extends Node> elements, int startIndex) {
        int width = 0;
        for (int i = startIndex, size = elements.size(); i < size; ++i) {
            if (!(elements.get(i) instanceof Elision)) {
                break;
            }
            width += 1;
        }
        return width;
    }

    /**
     * Static Semantics: ExpectedArgumentCount
     */
    public static int ExpectedArgumentCount(List<FormalParameter> formals) {
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
        return node.accept(IsConstantDeclaration.INSTANCE, null);
    }

    /**
     * Static Semantics: IsSimpleParameterList
     */
    public static boolean IsSimpleParameterList(List<FormalParameter> formals) {
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
     * Static Semantics: IsValidSimpleAssignmentTarget
     */
    public static boolean IsValidSimpleAssignmentTarget(Expression node, boolean strict) {
        return node.accept(IsValidSimpleAssignmentTarget.INSTANCE, strict);
    }

    // LexicalDeclarations

    /**
     * Static Semantics: LexicalDeclarations
     */
    public static List<Declaration> LexicalDeclarations(BlockStatement node) {
        return LexicalDeclarations(node.getStatements());
    }

    /**
     * Static Semantics: LexicalDeclarations
     */
    public static List<Declaration> LexicalDeclarations(SwitchStatement node) {
        return caseBlock(LexicalDeclarations.INSTANCE, node.getClauses(),
                new ArrayList<Declaration>());
    }

    /**
     * Static Semantics: LexicalDeclarations
     */
    public static List<Declaration> LexicalDeclarations(FunctionNode node) {
        return LexicalDeclarations(node.getStatements());
    }

    /**
     * Static Semantics: LexicalDeclarations
     */
    public static List<Declaration> LexicalDeclarations(Script node) {
        return LexicalDeclarations(node.getStatements());
    }

    /**
     * Static Semantics: LexicalDeclarations
     */
    private static List<Declaration> LexicalDeclarations(List<StatementListItem> statementlist) {
        return statementList(LexicalDeclarations.INSTANCE, statementlist,
                new ArrayList<Declaration>());
    }

    // LexicallyDeclaredNames

    /**
     * Static Semantics: LexicallyDeclaredNames
     */
    public static List<String> LexicallyDeclaredNames(BlockStatement node) {
        return LexicallyDeclaredNames(node.getStatements());
    }

    /**
     * Static Semantics: LexicallyDeclaredNames
     */
    public static List<String> LexicallyDeclaredNames(SwitchStatement node) {
        return caseBlock(LexicallyDeclaredNames.INSTANCE, node.getClauses(),
                new ArrayList<String>());
    }

    /**
     * 13.1 Function Definitions<br>
     * 13.2 Arrow Function Definitions
     * <p>
     * Static Semantics: LexicallyDeclaredNames (FunctionBody -> StatementList)<br>
     * Static Semantics: LexicallyDeclaredNames (FunctionBody -> StatementList)<br>
     * Static Semantics: LexicallyDeclaredNames ([LA &#x2209; { <b>{</b> }] AssignmentExpression)
     */
    public static List<String> LexicallyDeclaredNames(FunctionNode node) {
        return TopLevelLexicallyDeclaredNames(node.getStatements());
    }

    /**
     * 14.1 Script
     * <p>
     * Static Semantics: LexicallyDeclaredNames
     */
    public static List<String> LexicallyDeclaredNames(Script node) {
        // TODO: ModuleDeclaration | ImportDeclaration
        return TopLevelLexicallyDeclaredNames(node.getStatements());
    }

    /**
     * Static Semantics: LexicallyDeclaredNames
     */
    private static List<String> LexicallyDeclaredNames(List<StatementListItem> statementlist) {
        return statementList(LexicallyDeclaredNames.INSTANCE, statementlist,
                new ArrayList<String>());
    }

    /**
     * Static Semantics: TopLevelLexicallyDeclaredNames
     */
    private static List<String> TopLevelLexicallyDeclaredNames(List<StatementListItem> statementlist) {
        return statementList(TopLevelLexicallyDeclaredNames.INSTANCE, statementlist,
                new ArrayList<String>());
    }

    // LexicallyScopedDeclarations

    /**
     * 13.1 Function Definitions
     * <p>
     * Static Semantics: LexicallyScopedDeclarations (FIXME: missing in spec!)
     */
    public static List<Declaration> LexicallyScopedDeclarations(FunctionNode node) {
        // FIXME: spec uses LexicalDeclarations on FunctionBody -> nonsense?!
        return TopLevelLexicallyScopedDeclarations(node.getStatements());
    }

    /**
     * 14.1 Script
     * <p>
     * Static Semantics: LexicallyScopedDeclarations
     */
    public static List<Declaration> LexicallyScopedDeclarations(Script node) {
        // TODO: ModuleDeclaration | ImportDeclaration
        return TopLevelLexicallyScopedDeclarations(node.getStatements());
    }

    /**
     * Static Semantics: TopLevelLexicallyScopedDeclarations
     */
    private static List<Declaration> TopLevelLexicallyScopedDeclarations(
            List<StatementListItem> statementlist) {
        return statementList(TopLevelLexicallyScopedDeclarations.INSTANCE, statementlist,
                new ArrayList<Declaration>());
    }

    // VarDeclaredNames

    /**
     * Static Semantics: VarDeclaredNames
     */
    public static List<String> VarDeclaredNames(BlockStatement node) {
        return VarDeclaredNames(node.getStatements());
    }

    /**
     * Static Semantics: VarDeclaredNames
     */
    public static List<String> VarDeclaredNames(SwitchStatement node) {
        return caseBlock(VarDeclaredNames.INSTANCE, node.getClauses(), new ArrayList<String>());
    }

    /**
     * 13.1 Function Definitions
     * <p>
     * Static Semantics: VarDeclaredNames (FunctionBody -> StatementList)
     */
    public static List<String> VarDeclaredNames(FunctionNode node) {
        return TopLevelVarDeclaredNames(node.getStatements());
    }

    /**
     * 14.1 Script
     * <p>
     * Static Semantics: VarDeclaredNames
     */
    public static List<String> VarDeclaredNames(Script node) {
        // TODO: ModuleDeclaration | ImportDeclaration
        return TopLevelVarDeclaredNames(node.getStatements());
    }

    /**
     * Static Semantics: VarDeclaredNames
     */
    private static List<String> VarDeclaredNames(List<StatementListItem> statementlist) {
        return statementList(VarDeclaredNames.INSTANCE, statementlist, new ArrayList<String>());
    }

    /**
     * Static Semantics: TopLevelVarDeclaredNames
     */
    private static List<String> TopLevelVarDeclaredNames(List<StatementListItem> statementlist) {
        return statementList(TopLevelVarDeclaredNames.INSTANCE, statementlist,
                new ArrayList<String>());
    }

    // VarScopedDeclarations

    /**
     * 13.1 Function Definitions
     * <p>
     * Static Semantics: VarScopedDeclarations (FIXME: missing in spec!)
     */
    public static List<StatementListItem> VarScopedDeclarations(FunctionNode node) {
        return TopLevelVarScopedDeclarations(node.getStatements());
    }

    /**
     * 14.1 Script
     * <p>
     * Static Semantics: VarScopedDeclarations
     */
    public static List<StatementListItem> VarScopedDeclarations(Script node) {
        // TODO: ModuleDeclaration | ImportDeclaration
        return TopLevelVarScopedDeclarations(node.getStatements());
    }

    /**
     * Static Semantics: TopLevelVarScopedDeclarations
     */
    private static List<StatementListItem> TopLevelVarScopedDeclarations(
            List<StatementListItem> statementlist) {
        return statementList(TopLevelVarScopedDeclarations.INSTANCE, statementlist,
                new ArrayList<StatementListItem>());
    }

    //

    /**
     * Static Semantics: MethodDefinitions
     */
    public static List<MethodDefinition> MethodDefinitions(ClassDefinition node) {
        return node.getBody();
    }

    /**
     * Static Semantics: NumberOfParameters (FIXME: not yet defined in spec!)
     */
    public static int NumberOfParameters(List<FormalParameter> formals) {
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
     * Static Semantics: PropertyNameList
     */
    public static List<String> PropertyNameList(List<PropertyDefinition> list) {
        List<String> result = new ArrayList<>();
        for (PropertyDefinition def : list) {
            result.add(PropName(def));
        }
        return result;
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

    private static <T> T statementList(NodeVisitor<T, T> visitor,
            List<StatementListItem> statementlist, T result) {
        if (statementlist != null) {
            forEach(visitor, statementlist, result);
        }
        return result;
    }

    private static <T> T caseBlock(NodeVisitor<T, T> visitor, List<SwitchClause> clauses, T result) {
        forEach(visitor, clauses, result);
        return result;
    }

}
