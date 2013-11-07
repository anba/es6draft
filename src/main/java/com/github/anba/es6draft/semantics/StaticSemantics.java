/**
 * Copyright (c) 2012-2013 André Bargull
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
        for (MethodDefinition m : PrototypeMethodDefinitions(node)) {
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
     * Static Semantics: IsAnonymousFunctionDefinition
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
     */
    public static boolean IsConstantDeclaration(Declaration node) {
        return node.isConstDeclaration();
    }

    /**
     * Static Semantics: IsIdentifierRef
     */
    public static boolean IsIdentifierRef(Expression node) {
        return node instanceof Identifier && !node.isParenthesised();
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
        return node.getStrictMode() != FunctionNode.StrictMode.NonStrict;
    }

    /**
     * Static Semantics: IsValidSimpleAssignmentTarget
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
     * Static Semantics: LexicallyDeclaredNames
     */
    public static Set<String> LexicallyDeclaredNames(BlockScope scope) {
        return emptyIfNull(scope.lexicallyDeclaredNames());
    }

    /**
     * 15.1 Script
     * <p>
     * Static Semantics: LexicallyDeclaredNames
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
     * 15.1 Script
     * <p>
     * Static Semantics: LexicallyScopedDeclarations
     */
    public static List<Declaration> LexicallyScopedDeclarations(Script node) {
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 14.1 Function Definitions
     * <p>
     * Static Semantics: VarDeclaredNames (FunctionBody -> StatementList)
     */
    public static Set<String> VarDeclaredNames(FunctionNode node) {
        return emptyIfNull(node.getScope().varDeclaredNames());
    }

    /**
     * 15.1 Script
     * <p>
     * Static Semantics: VarDeclaredNames
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
     * 15.1 Script
     * <p>
     * Static Semantics: VarScopedDeclarations
     */
    public static List<StatementListItem> VarScopedDeclarations(Script node) {
        return emptyIfNull(node.getScope().varScopedDeclarations());
    }

    /**
     * Static Semantics: PrototypeMethodDefinitions
     */
    public static List<MethodDefinition> PrototypeMethodDefinitions(ClassDefinition node) {
        return node.getPrototypeMethods();
    }

    /**
     * Static Semantics: StaticMethodDefinitions
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

    /**
     * Static Semantics: TemplateStrings
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
     */
    public static Set<Expression> TailCallNodes(Expression expr) {
        while (expr instanceof CommaExpression) {
            expr = last(((CommaExpression) expr).getOperands());
        }
        if (IsCallExpression(expr)) {
            return singleton(expr);
        } else if (expr instanceof ConditionalExpression) {
            HashSet<Expression> tail = new HashSet<>(8);
            for (ArrayDeque<Expression> queue = new ArrayDeque<>(singleton(expr)); !queue.isEmpty();) {
                Expression e = queue.remove();
                while (e instanceof CommaExpression) {
                    e = last(((CommaExpression) e).getOperands());
                }
                if (IsCallExpression(e)) {
                    tail.add(e);
                } else if (e instanceof ConditionalExpression) {
                    queue.add(((ConditionalExpression) e).getThen());
                    queue.add(((ConditionalExpression) e).getOtherwise());
                }
            }
            return tail;
        } else {
            return emptySet();
        }
    }

    //

    private static boolean IsCallExpression(Expression expr) {
        return expr instanceof CallExpression
                || expr instanceof TemplateCallExpression
                || (expr instanceof SuperExpression && ((SuperExpression) expr).getType() == SuperExpression.Type.CallExpression);
    }

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
