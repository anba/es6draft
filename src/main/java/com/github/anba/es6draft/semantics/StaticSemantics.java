/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import static com.github.anba.es6draft.semantics.StaticSemanticsVisitor.forEach;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
     * 13.2.2.1 Static Semantics: BoundNames
     */
    public static List<String> BoundNames(VariableStatement node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
    }

    /**
     * 14.1.2 Static Semantics: BoundNames
     */
    public static List<String> BoundNames(FormalParameterList formals) {
        List<String> result = new SmallArrayList<>();
        forEach(BoundNames.INSTANCE, formals, result);
        return result;
    }

    /**
     * 14.5.3 Static Semantics: ConstructorMethod
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
     * Static Semantics: ExpectedArgumentCount
     * <ul>
     * <li>14.1.4 Static Semantics: ExpectedArgumentCount
     * <li>14.2.5 Static Semantics: ExpectedArgumentCount
     * <li>14.3.2 Static Semantics: ExpectedArgumentCount
     * </ul>
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
     * 13.2.3.3 Static Semantics: HasInitialiser
     */
    static boolean HasInitialiser(BindingElement node) {
        return node.getInitialiser() != null;
    }

    /**
     * Static Semantics: IsAnonymousFunctionDefinition
     * <ul>
     * <li>12.1.0.2 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.1.10.2 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.2.1.2 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.3.2 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.4.2 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.5.1 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.6.1 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.7.1 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.8.1 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.9.1 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.10.1 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.11.1 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.12.1 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.13.2 Static Semantics: IsAnonymousFunctionDefinition
     * <li>12.14.1 Static Semantics: IsAnonymousFunctionDefinition
     * <li>14.1.6 Static Semantics: IsAnonymousFunctionDefinition
     * <li>14.4.5 Static Semantics: IsAnonymousFunctionDefinition
     * <li>14.5.5 Static Semantics: IsAnonymousFunctionDefinition
     * </ul>
     */
    public static boolean IsAnonymousFunctionDefinition(Expression node) {
        if (node instanceof ArrowFunction) {
            return true;
        }
        if (node instanceof FunctionExpression) {
            return ((FunctionExpression) node).getIdentifier() == null;
        }
        if (node instanceof GeneratorExpression) {
            return ((GeneratorExpression) node).getIdentifier() == null;
        }
        if (node instanceof ClassExpression) {
            return ((ClassExpression) node).getName() == null;
        }
        return false;
    }

    /**
     * Static Semantics: IsConstantDeclaration
     * <ul>
     * <li>13.2.1.3 Static Semantics: IsConstantDeclaration
     * <li>14.1.7 Static Semantics: IsConstantDeclaration
     * <li>14.4.6 Static Semantics: IsConstantDeclaration
     * <li>14.5.6 Static Semantics: IsConstantDeclaration
     * </ul>
     */
    public static boolean IsConstantDeclaration(Declaration node) {
        return node.isConstDeclaration();
    }

    /**
     * Static Semantics: IsIdentifierRef
     * <ul>
     * <li>12.1.0.3 Static Semantics: IsIdentifierRef
     * <li>12.2.1.3 Static Semantics: IsIdentifierRef
     * </ul>
     */
    public static boolean IsIdentifierRef(LeftHandSideExpression node) {
        return node instanceof Identifier && !node.isParenthesised();
    }

    /**
     * 14.1.8 Static Semantics: IsSimpleParameterList
     */
    public static boolean IsSimpleParameterList(FormalParameterList formals) {
        for (FormalParameter formal : formals) {
            if (formal instanceof BindingRestElement) {
                return false;
            } else if (HasInitialiser((BindingElement) formal)) {
                return false;
            } else if (((BindingElement) formal).getBinding() instanceof BindingPattern) {
                return false;
            }
        }
        return true;
    }

    /**
     * 15.2.2 Static Semantics: IsStrict
     */
    public static boolean IsStrict(Script node) {
        return node.isStrict();
    }

    /**
     * 14.1.9 Static Semantics: IsStrict
     */
    public static boolean IsStrict(FunctionNode node) {
        return node.getStrictMode() != FunctionNode.StrictMode.NonStrict;
    }

    /**
     * Static Semantics: IsValidSimpleAssignmentTarget
     * <ul>
     * <li>12.1.0.4 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.1.10.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.2.1.4 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.3.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.4.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.5.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.6.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.7.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.8.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.9.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.10.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.11.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.12.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.13.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.14.2 Static Semantics: IsValidSimpleAssignmentTarget
     * </ul>
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
     */
    public static List<Declaration> LexicalDeclarations(BlockStatement node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 13.11.2 Static Semantics: LexicalDeclarations
     */
    public static List<Declaration> LexicalDeclarations(SwitchStatement node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 13.1.3 Static Semantics: LexicallyDeclaredNames
     */
    public static Set<String> LexicallyDeclaredNames(BlockScope scope) {
        return emptyIfNull(scope.lexicallyDeclaredNames());
    }

    /**
     * 15.2.3 Static Semantics: LexicallyDeclaredNames
     */
    public static Set<String> LexicallyDeclaredNames(Script node) {
        return emptyIfNull(node.getScope().lexicallyDeclaredNames());
    }

    /**
     * 14.1 Function Definitions
     * <p>
     * Static Semantics: LexicallyScopedDeclarations (FIXME: missing in spec!)
     */
    public static List<Declaration> LexicallyScopedDeclarations(FunctionNode node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 15.2.4 Static Semantics: LexicallyScopedDeclarations
     */
    public static List<Declaration> LexicallyScopedDeclarations(Script node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 14.1.11 Static Semantics: VarDeclaredNames
     * 
     * <pre>
     * FunctionStatementList :
     *     StatementList
     * </pre>
     */
    public static Set<String> VarDeclaredNames(FunctionNode node) {
        return emptyIfNull(node.getScope().varDeclaredNames());
    }

    /**
     * 15.2.5 Static Semantics: VarDeclaredNames
     */
    public static Set<String> VarDeclaredNames(Script node) {
        return emptyIfNull(node.getScope().varDeclaredNames());
    }

    /**
     * 14.1 Function Definitions
     * <p>
     * Static Semantics: VarScopedDeclarations (FIXME: missing in spec!)
     */
    public static List<StatementListItem> VarScopedDeclarations(FunctionNode node) {
        return emptyIfNull(node.getScope().varScopedDeclarations());
    }

    /**
     * 15.2.6 Static Semantics: VarScopedDeclarations
     */
    public static List<StatementListItem> VarScopedDeclarations(Script node) {
        return emptyIfNull(node.getScope().varScopedDeclarations());
    }

    /**
     * 14.5.9 Static Semantics: PrototypeMethodDefinitions
     */
    public static List<MethodDefinition> PrototypeMethodDefinitions(ClassDefinition node) {
        return node.getPrototypeMethods();
    }

    /**
     * 14.5.13 Static Semantics: StaticMethodDefinitions
     */
    public static List<MethodDefinition> StaticMethodDefinitions(ClassDefinition node) {
        return node.getStaticMethods();
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
     * 12.1.5.5 Static Semantics: PropName<br>
     * 14.3.5 Static Semantics: PropName
     */
    public static String PropName(PropertyDefinition node) {
        return node.getPropertyName().getName();
    }

    /**
     * 12.1.5.5 Static Semantics: PropName
     */
    public static String PropName(PropertyName node) {
        return node.getName();
    }

    /**
     * 14.3.7 Static Semantics: SpecialMethod
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

    /**
     * 12.1.9.1.1 Static Semantics: TemplateStrings
     */
    public static List<TemplateCharacters> TemplateStrings(TemplateLiteral node) {
        List<Expression> elements = node.getElements();
        assert (elements.size() & 1) == 1;
        int numChars = ((elements.size() / 2) + 1);
        List<TemplateCharacters> strings = new ArrayList<>(numChars);
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
     */
    public static List<Expression> Substitutions(TemplateLiteral node) {
        List<Expression> elements = node.getElements();
        assert (elements.size() & 1) == 1;
        int numSubst = (elements.size() / 2);
        List<Expression> substitutions = new ArrayList<>(numSubst);
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
        return (list != null ? list : Collections.<T> emptySet());
    }

    private static <T> List<T> emptyIfNull(List<T> list) {
        return (list != null ? list : Collections.<T> emptyList());
    }

    private static <T> T last(List<T> list) {
        assert !list.isEmpty();
        return list.get(list.size() - 1);
    }
}
