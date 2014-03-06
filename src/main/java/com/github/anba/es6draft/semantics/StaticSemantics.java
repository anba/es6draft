/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

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
import com.github.anba.es6draft.runtime.modules.ExportEntry;
import com.github.anba.es6draft.runtime.modules.ImportEntry;

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
    public static List<String> BoundNames(FormalParameterList node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
    }

    /**
     * 15.2.1.2 Static Semantics: BoundNames
     */
    public static List<String> BoundNames(ImportDeclaration node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
    }

    /**
     * 15.2.2.1 Static Semantics: BoundNames
     */
    public static List<String> BoundNames(ExportDeclaration node) {
        return node.accept(BoundNames.INSTANCE, new SmallArrayList<String>());
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
     * 15.1.2 Static Semantics: IsStrict
     */
    public static boolean IsStrict(Script node) {
        return node.isStrict();
    }

    /**
     * 15.2.0.7 Static Semantics: IsStrict
     */
    public static boolean IsStrict(Module node) {
        return true;
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
     * 15.2.0.2 Static Semantics: DeclaredNames
     */
    public static List<String> DeclaredNames(Module node) {
        List<String> names = new ArrayList<>();
        names.addAll(LexicallyDeclaredNames(node));
        names.addAll(VarDeclaredNames(node));
        return names;
    }

    /**
     * 15.2.0.3 Static Semantics: ExportedBindings<br>
     * 15.2.2.2 Static Semantics: ExportedBindings
     */
    public static Set<String> ExportedBindings(Module node) {
        return emptyIfNull(node.getScope().getExportBindings());
    }

    /**
     * 15.2.0.4 Static Semantics: ExportEntries<br>
     * 15.2.2.3 Static Semantics: ExportEntries
     */
    public static List<ExportEntry> ExportEntries(Module node) {
        List<ExportEntry> entries = new ArrayList<>();
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
     */
    public static List<String> ImportedBindings(Module node) {
        List<String> bindings = new ArrayList<>();
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
     */
    public static List<ImportEntry> ImportEntries(Module node) {
        List<ImportEntry> entries = new ArrayList<>();
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
     */
    public static List<ExportEntry> KnownExportEntries(Module node) {
        List<ExportEntry> knownExports = new ArrayList<>();
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
     */
    public static Set<String> ModuleRequests(Module node) {
        return emptyIfNull(node.getScope().getModuleRequests());
    }

    /**
     * 15.2.0.10 Static Semantics: LexicallyDeclaredNames
     */
    public static Set<String> LexicallyDeclaredNames(Module node) {
        return emptyIfNull(node.getScope().lexicallyDeclaredNames());
    }

    /**
     * 15.2.0.11 Static Semantics: LexicalDeclarations
     */
    public static List<Declaration> LexicalDeclarations(Module node) {
        // TODO: does not include ImportDeclaration nodes
        return emptyIfNull(node.getScope().lexicallyScopedDeclarations());
    }

    /**
     * 15.2.0.12 Static Semantics: UnknownExportEntries
     */
    public static List<ExportEntry> UnknownExportEntries(Module node) {
        List<ExportEntry> unknownExports = new ArrayList<>();
        for (ExportEntry entry : ExportEntries(node)) {
            if ("<all>".equals(entry.getImportName())) {
                unknownExports.add(entry);
            }
        }
        return unknownExports;
    }

    /**
     * 15.2.0.13 Static Semantics: VarDeclaredNames
     */
    public static Set<String> VarDeclaredNames(Module node) {
        return emptyIfNull(node.getScope().varDeclaredNames());
    }

    /**
     * 15.2.0.14 Static Semantics: VarScopedDeclarations
     */
    public static List<StatementListItem> VarScopedDeclarations(Module node) {
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
