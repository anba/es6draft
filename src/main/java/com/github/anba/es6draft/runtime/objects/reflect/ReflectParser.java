/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.objects.text.RegExpConstructor.RegExpCreate;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Type.isUndefinedOrNull;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicallyScopedDeclarations;
import static com.github.anba.es6draft.semantics.StaticSemantics.Substitutions;
import static com.github.anba.es6draft.semantics.StaticSemantics.TemplateStrings;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.ast.synthetic.ClassFieldInitializer;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.MethodDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.objects.text.RegExpObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * https://developer.mozilla.org/en-US/docs/SpiderMonkey/Parser_API
 */
public final class ReflectParser implements NodeVisitor<Object, Void> {
    private final ExecutionContext cx;
    private final boolean location;
    private final String sourceInfo;
    private final EnumMap<Type, Callable> builder;

    private enum Type {
        /* @formatter:off */
        // Programs
        Program("program"),

        // Miscellaneous
        Identifier("identifier"),
        Literal("literal"),
        Property("property"),
        PrototypeMutation("prototypeMutation"),

        // Declarations
        ModuleDeclaration("moduleDeclaration"),
        FunctionDeclaration("functionDeclaration"),
        VariableDeclaration("variableDeclaration"),
        VariableDeclarator("variableDeclarator"),

        // Expressions
        SequenceExpression("sequenceExpression"),
        ConditionalExpression("conditionalExpression"),
        UnaryExpression("unaryExpression"),
        BinaryExpression("binaryExpression"),
        AssignmentExpression("assignmentExpression"),
        LogicalExpression("logicalExpression"),
        UpdateExpression("updateExpression"),
        NewExpression("newExpression"),
        CallExpression("callExpression"),
        MemberExpression("memberExpression"),
        FunctionExpression("functionExpression"),
        ArrowFunctionExpression("arrowFunctionExpression"),
        ArrayExpression("arrayExpression"),
        SpreadExpression("spreadExpression"),
        ObjectExpression("objectExpression"),
        ThisExpression("thisExpression"),
        ComprehensionExpression("comprehensionExpression"),
        GeneratorExpression("generatorExpression"),
        YieldExpression("yieldExpression"),
        ClassExpression("classExpression"),
        MetaProperty("metaProperty"),
        Super("super"),

        // Statements
        EmptyStatement("emptyStatement"),
        BlockStatement("blockStatement"),
        ExpressionStatement("expressionStatement"),
        LabeledStatement("labeledStatement"),
        IfStatement("ifStatement"),
        SwitchStatement("switchStatement"),
        WhileStatement("whileStatement"),
        DoWhileStatement("doWhileStatement"),
        ForStatement("forStatement"),
        ForInStatement("forInStatement"),
        ForOfStatement("forOfStatement"),
        BreakStatement("breakStatement"),
        ContinueStatement("continueStatement"),
        WithStatement("withStatement"),
        ReturnStatement("returnStatement"),
        TryStatement("tryStatement"),
        ThrowStatement("throwStatement"),
        DebuggerStatement("debuggerStatement"),
        LetStatement("letStatement"),

        // Modules
        ImportDeclaration("importDeclaration"),
        ImportSpecifier("importSpecifier"),
        ExportDeclaration("exportDeclaration"),
        ExportSpecifier("exportSpecifier"),
        ExportBatchSpecifier("exportBatchSpecifier"),

        // Clauses
        SwitchCase("switchCase"),
        CatchClause("catchClause"),
        ComprehensionBlock("comprehensionBlock"),
        ComprehensionIf("comprehensionIf"),

        // Patterns
        ArrayPattern("arrayPattern"),
        ObjectPattern("objectPattern"),
        PropertyPattern("propertyPattern"),

        // Template strings
        TemplateLiteral("templateLiteral"),
        TaggedTemplate("taggedTemplate"),
        CallSiteObject("callSiteObject"),

        // Computed property name
        ComputedName("computedName"),

        // Classes
        ClassStatement("classStatement"),
        ClassMethod("classMethod"),

        // New node types
        DoExpression(),
        ForAwaitStatement(),
        ImportCall(),
        PrivateName(),
        ThrowExpression(),

        // Removed node types
        LetExpression("letExpression"),

        ;
        /* @formatter:on */

        private final String name;

        private Type() {
            // <no custom builder>
            this(null);
        }

        private Type(String name) {
            this.name = name;
        }
    }

    private enum GeneratorStyle {
        ES6("es6"), Legacy("legacy");

        final String name;

        private GeneratorStyle(String name) {
            this.name = name;
        }
    }

    private enum Target {
        Script, Module
    }

    private static EnumMap<Type, Callable> toBuilderMap(ExecutionContext cx, ScriptObject builderObject) {
        EnumMap<Type, Callable> map = new EnumMap<>(Type.class);
        for (Type builder : Type.values()) {
            String methodName = builder.name;
            if (methodName == null) {
                continue;
            }
            if (!HasProperty(cx, builderObject, methodName)) {
                continue;
            }
            Callable method = GetMethod(cx, builderObject, methodName);
            if (method == null) {
                continue;
            }
            map.put(builder, method);
        }
        return map;
    }

    private boolean hasBuilder(Type type) {
        return builder.containsKey(type);
    }

    private Object call(Type type, Node node, Object... arguments) {
        if (location && node != null) {
            Object[] args = Arrays.copyOf(arguments, arguments.length + 1);
            args[arguments.length] = createSourceLocation(node);
            arguments = args;
        }
        return builder.get(type).call(cx, NULL, arguments);
    }

    private ReflectParser(ExecutionContext cx, boolean location, String sourceInfo, EnumMap<Type, Callable> builder) {
        this.cx = cx;
        this.location = location;
        this.sourceInfo = sourceInfo;
        this.builder = builder;
    }

    /**
     * Parses the given script code and returns the matching Reflect AST nodes.
     * 
     * @param cx
     *            the execution context
     * @param source
     *            the source string
     * @param options
     *            the options object
     * @return the parsed node
     */
    public static Object parse(ExecutionContext cx, String source, ScriptObject options) {
        boolean location = true;
        String sourceInfo = null;
        int line = 1;
        EnumMap<Type, Callable> builder = new EnumMap<>(Type.class);
        Target target = Target.Script;
        if (options != null) {
            if (HasProperty(cx, options, "loc")) {
                location = ToBoolean(Get(cx, options, "loc"));
            }
            if (HasProperty(cx, options, "source")) {
                sourceInfo = ToFlatString(cx, Get(cx, options, "source"));
            }
            if (HasProperty(cx, options, "line")) {
                line = ToInt32(cx, Get(cx, options, "line"));
            }
            if (HasProperty(cx, options, "builder")) {
                Object value = Get(cx, options, "builder");
                if (!isUndefinedOrNull(value)) {
                    builder = toBuilderMap(cx, ToObject(cx, value));
                }
            }
            if (HasProperty(cx, options, "target")) {
                Object t = Get(cx, options, "target");
                if (!com.github.anba.es6draft.runtime.types.Type.isString(t)) {
                    throw newTypeError(cx, Messages.Key.NotString);
                }
                CharSequence str = ToString(cx, t);
                if ("script".equals(str)) {
                    target = Target.Script;
                } else if ("module".equals(str)) {
                    target = Target.Module;
                } else {
                    throw newError(cx, "Bad value");
                }
            }
        }
        return parse(cx, source, location, sourceInfo, line, builder, target);
    }

    /**
     * Parses the given script code and returns the matching Reflect AST nodes.
     * 
     * @param cx
     *            the execution context
     * @param sourceCode
     *            the source string
     * @param location
     *            if set to {@code true} node locations will be recorded
     * @param sourceInfo
     *            the source info string
     * @param line
     *            the start line offset
     * @param builder
     *            map to customize AST node processing
     * @param target
     *            the parse target
     * @return the parsed node
     */
    private static Object parse(ExecutionContext cx, String sourceCode, boolean location, String sourceInfo, int line,
            EnumMap<Type, Callable> builder, Target target) {
        Realm realm = cx.getRealm();
        ReflectParser reflect = new ReflectParser(cx, location, sourceInfo, builder);
        Source source = new Source("<parse>", line);
        TopLevelNode<?> parsedNode;
        try {
            if (target == Target.Script) {
                parsedNode = realm.getScriptLoader().parseScript(source, sourceCode);
            } else {
                assert target == Target.Module;
                parsedNode = realm.getScriptLoader().parseModule(source, sourceCode);
            }
        } catch (ParserException e) {
            throw e.toScriptException(cx);
        }
        return parsedNode.accept(reflect, null);
    }

    private OrdinaryObject createEmptyNode() {
        return ObjectCreate(cx, Intrinsics.ObjectPrototype);
    }

    private void addProperty(OrdinaryObject holder, String key, Object value) {
        CreateDataProperty(cx, holder, key, value);
    }

    private void addNodeInfo(OrdinaryObject holder, Node node, Type type) {
        Object loc = location ? createSourceLocation(node) : NULL;
        addSourceLocation(holder, loc);
        addType(holder, type);
    }

    private void addNodeInfo(OrdinaryObject holder, Type type) {
        addSourceLocation(holder, NULL);
        addType(holder, type);
    }

    private void addSourceLocation(OrdinaryObject holder, Object loc) {
        addProperty(holder, "loc", loc);
    }

    private void addType(OrdinaryObject holder, Type type) {
        addProperty(holder, "type", type.name());
    }

    private OrdinaryObject createSourceLocation(Node node) {
        OrdinaryObject loc = createEmptyNode();
        addProperty(loc, "start", createPosition(node.getBeginLine(), node.getBeginColumn()));
        addProperty(loc, "end", createPosition(node.getEndLine(), node.getEndColumn()));
        addProperty(loc, "source", sourceInfo != null ? sourceInfo : NULL);
        return loc;
    }

    private OrdinaryObject createPosition(int line, int column) {
        // subtract one to make columns 0-indexed
        OrdinaryObject pos = createEmptyNode();
        addProperty(pos, "line", line);
        addProperty(pos, "column", column - 1);
        return pos;
    }

    private OrdinaryObject createNode(Node node, Type type) {
        OrdinaryObject object = createEmptyNode();
        addNodeInfo(object, node, type);
        return object;
    }

    private OrdinaryObject createNode(Type type) {
        OrdinaryObject object = createEmptyNode();
        addNodeInfo(object, type);
        return object;
    }

    private OrdinaryObject createModuleItem(ModuleItem node, Type type) {
        return createNode(node, type);
    }

    private OrdinaryObject createStatement(Statement node, Type type) {
        return createNode(node, type);
    }

    private OrdinaryObject createDeclaration(LexicalDeclaration node, Type type) {
        return createNode(node, type);
    }

    private OrdinaryObject createDeclaration(VariableStatement node, Type type) {
        return createNode(node, type);
    }

    private OrdinaryObject createExpression(Expression node, Type type) {
        return createNode(node, type);
    }

    private OrdinaryObject createPattern(AssignmentPattern node, Type type) {
        return createNode(node, type);
    }

    private OrdinaryObject createPattern(BindingPattern node, Type type) {
        return createNode(node, type);
    }

    private OrdinaryObject createBinding(Binding node, Type type) {
        return createNode(node, type);
    }

    private OrdinaryObject createFunction(FunctionNode node, Type type) {
        return createNode(node, type);
    }

    private OrdinaryObject createClass(ClassDefinition node, Type type) {
        return createNode(node, type);
    }

    private Object createLiteral(ValueLiteral<?> node) {
        return createLiteral(node, node.getValue());
    }

    private Object createLiteral(TemplateCharacters node) {
        assert node.getValue() != null;
        return createLiteral(node, node.getValue());
    }

    private Object createLiteral(Node node, Object value) {
        if (hasBuilder(Type.Literal)) {
            return call(Type.Literal, node, value);
        }
        OrdinaryObject literal = createNode(node, Type.Literal);
        addProperty(literal, "value", value);
        return literal;
    }

    private Object createLiteral(String value) {
        if (hasBuilder(Type.Literal)) {
            return call(Type.Literal, null, value);
        }
        OrdinaryObject literal = createNode(Type.Literal);
        addProperty(literal, "value", value);
        return literal;
    }

    private Object createIdentifierOrNull(String name) {
        return name != null ? createIdentifier(name) : NULL;
    }

    private Object createDeclarationIdentifier(HoistableDeclaration declaration, Void value) {
        if (declaration.getIdentifier() != null) {
            return declaration.getIdentifier().accept(this, value);
        }
        return createIdentifier("default");
    }

    private Object createDeclarationIdentifier(ClassDeclaration declaration, Void value) {
        if (declaration.getIdentifier() != null) {
            return declaration.getIdentifier().accept(this, value);
        }
        return createIdentifier("default");
    }

    private Object createIdentifier(String name) {
        if (hasBuilder(Type.Identifier)) {
            return call(Type.Identifier, null, name);
        }
        OrdinaryObject identifier = createNode(Type.Identifier);
        addProperty(identifier, "name", name);
        return identifier;
    }

    private ArrayObject createList(List<? extends Node> nodes, Void value) {
        return CreateArrayFromList(cx, nodes.stream().map(node -> node.accept(this, value)));
    }

    private ArrayObject createListWithNull(List<? extends Node> nodes, Void value) {
        return CreateArrayFromList(cx, nodes.stream().map(node -> acceptOrNull(node, value)));
    }

    private ArrayObject createListFromValues(List<? extends Object> values) {
        return CreateArrayFromList(cx, values);
    }

    private Object acceptOrNull(Node node, Void value) {
        return node != null ? node.accept(this, value) : NULL;
    }

    private static <T> T lastElement(List<T> list) {
        return !list.isEmpty() ? list.get(list.size() - 1) : null;
    }

    private List<Binding> getParameterBindings(FormalParameterList formals) {
        ArrayList<Binding> bindings = new ArrayList<>();
        for (FormalParameter formalParameter : formals) {
            BindingElementItem element = formalParameter.getElement();
            if (element instanceof BindingElement) {
                bindings.add(((BindingElement) element).getBinding());
            }
        }
        return bindings;
    }

    private List<Expression> getParameterDefaults(FormalParameterList formals) {
        boolean hasDefault = false;
        ArrayList<Expression> defaults = new ArrayList<>();
        for (FormalParameter formalParameter : formals) {
            BindingElementItem element = formalParameter.getElement();
            if (element instanceof BindingElement) {
                Expression initializer = ((BindingElement) element).getInitializer();
                hasDefault |= initializer != null;
                defaults.add(initializer != null ? initializer : null);
            } else {
                defaults.add(null);
            }
        }
        return hasDefault ? defaults : Collections.<Expression> emptyList();
    }

    private Binding getRestParameter(FormalParameterList formals) {
        FormalParameter last = lastElement(formals.getFormals());
        if (last != null && last.getElement() instanceof BindingRestElement) {
            return ((BindingRestElement) last.getElement()).getBinding();
        }
        return null;
    }

    private <STATEMENT extends Statement & AbruptNode> Object createLabelledStatement(STATEMENT node, Object body) {
        if (node.getLabelSet().isEmpty()) {
            return body;
        }
        Iterator<String> labels = new ArrayDeque<>(node.getLabelSet()).descendingIterator();
        while (labels.hasNext()) {
            Object label = createIdentifier(labels.next());
            if (hasBuilder(Type.LabeledStatement)) {
                body = call(Type.LabeledStatement, node, label, body);
            } else {
                OrdinaryObject statement = createStatement(node, Type.LabeledStatement);
                addProperty(statement, "label", label);
                addProperty(statement, "body", body);
                body = statement;
            }
        }
        return body;
    }

    private Object createFunctionBody(FunctionNode node, Void value) {
        // FunctionBody is materalized as BlockStatement
        ArrayObject body = createList(node.getStatements(), value);
        if (hasBuilder(Type.BlockStatement)) {
            return call(Type.BlockStatement, node, body);
        }
        OrdinaryObject statement = createNode(node, Type.BlockStatement);
        addProperty(statement, "body", body);
        return statement;
    }

    private OrdinaryObject createClassBody(ClassDefinition node, Void value) {
        ArrayList<OrdinaryObject> methods = new ArrayList<>();
        for (PropertyDefinition property : node.getProperties()) {
            if (property instanceof MethodDefinition) {
                MethodDefinition method = (MethodDefinition) property;
                if (method.isSynthetic()) {
                    continue;
                }
                methods.add(createClassMethod(node, method, value));
            } else {
                // TODO: Implement
            }
        }
        return createListFromValues(methods);
    }

    private OrdinaryObject createClassMethod(ClassDefinition classDef, MethodDefinition node, Void value) {
        Object name = node.getPropertyName().accept(this, null);
        Object body = toFunctionExpression(classDef, node, value);
        String kind = methodKind(node, "method");
        OrdinaryObject property = createNode(node, Type.ClassMethod);
        addProperty(property, "name", name);
        addProperty(property, "body", body);
        addProperty(property, "kind", kind);
        addProperty(property, "static", node.isStatic());
        return property;
    }

    private Object toFunctionExpression(ClassDefinition classDef, MethodDefinition node, Void value) {
        Object id;
        if (node.getPropertyName() instanceof ComputedPropertyName) {
            id = NULL;
        } else {
            switch (node.getType()) {
            case Getter:
                id = createIdentifier("get " + node.getPropertyName().getName());
                break;
            case Setter:
                id = createIdentifier("set " + node.getPropertyName().getName());
                break;
            case ClassConstructor:
                assert classDef != null;
                id = acceptOrNull(classDef.getIdentifier(), value);
                break;
            case AsyncFunction:
            case AsyncGenerator:
            case Function:
            case Generator:
            case CallConstructor:
            default:
                id = node.getPropertyName().accept(this, value);
                break;
            }
        }
        ArrayObject params = createList(getParameterBindings(node.getParameters()), value);
        ArrayObject defaults = createListWithNull(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        boolean generator = false;
        boolean async = false;
        switch (node.getType()) {
        case AsyncFunction:
            async = true;
            break;
        case AsyncGenerator:
            // TODO: async generators
            break;
        case Generator:
            generator = true;
            break;
        default:
        }
        boolean expression = false;
        if (hasBuilder(Type.FunctionExpression)) {
            return call(Type.FunctionExpression, node, id, params, body, generator, expression);
        }
        OrdinaryObject function = createFunction(node, Type.FunctionExpression);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "async", async);
        if (generator) {
            addProperty(function, "style", GeneratorStyle.ES6.name);
        }
        addProperty(function, "expression", expression);
        return function;
    }

    private static String methodKind(MethodDefinition method, String defaultKind) {
        switch (method.getType()) {
        case Getter:
            return "get";
        case Setter:
            return "set";
        default:
            return defaultKind;
        }
    }

    private static boolean isGetterOrSetter(MethodDefinition method) {
        switch (method.getType()) {
        case Getter:
        case Setter:
            return true;
        default:
            return false;
        }
    }

    private Object toAssignmentExpression(Node left, Expression right, Void value) {
        EmptyExpression empty = new EmptyExpression(left.getBeginPosition(), right.getEndPosition());
        Object left_ = left.accept(this, value);
        Object right_ = right.accept(this, value);
        String operator = AssignmentExpression.Operator.ASSIGN.toString();
        if (hasBuilder(Type.AssignmentExpression)) {
            return call(Type.AssignmentExpression, empty, operator, left_, right_);
        }
        OrdinaryObject expression = createExpression(empty, Type.AssignmentExpression);
        addProperty(expression, "left", left_);
        addProperty(expression, "right", right_);
        addProperty(expression, "operator", operator);
        return expression;
    }

    @Override
    public Object visit(ArrayAssignmentPattern node, Void value) {
        ArrayObject elements = createList(node.getElements(), value);
        if (hasBuilder(Type.ArrayPattern)) {
            return call(Type.ArrayPattern, node, elements);
        }
        OrdinaryObject pattern = createPattern(node, Type.ArrayPattern);
        addProperty(pattern, "elements", elements);
        return pattern;
    }

    @Override
    public Object visit(ArrayBindingPattern node, Void value) {
        ArrayObject elements = createList(node.getElements(), value);
        if (hasBuilder(Type.ArrayPattern)) {
            return call(Type.ArrayPattern, node, elements);
        }
        OrdinaryObject pattern = createPattern(node, Type.ArrayPattern);
        addProperty(pattern, "elements", elements);
        return pattern;
    }

    @Override
    public Object visit(ArrayComprehension node, Void value) {
        // Comprehension/LegacyComprehension already created a partial result
        OrdinaryObject expression = (OrdinaryObject) node.getComprehension().accept(this, value);
        if (hasBuilder(Type.ComprehensionExpression)) {
            Object body = Get(cx, expression, "body");
            Object blocks = Get(cx, expression, "blocks");
            Object filter = Get(cx, expression, "filter");
            return call(Type.ComprehensionExpression, node, body, blocks, filter);
        }
        addNodeInfo(expression, node, Type.ComprehensionExpression);
        return expression;
    }

    @Override
    public Object visit(ArrayLiteral node, Void value) {
        ArrayObject elements = createList(node.getElements(), value);
        if (hasBuilder(Type.ArrayExpression)) {
            return call(Type.ArrayExpression, node, elements);
        }
        OrdinaryObject expression = createExpression(node, Type.ArrayExpression);
        addProperty(expression, "elements", elements);
        return expression;
    }

    @Override
    public Object visit(ArrowFunction node, Void value) {
        Object id = NULL;
        ArrayObject params = createList(getParameterBindings(node.getParameters()), value);
        ArrayObject defaults = createListWithNull(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body;
        if (node.getExpression() == null) {
            body = createFunctionBody(node, value);
        } else {
            body = node.getExpression().accept(this, value);
        }
        boolean generator = false;
        boolean async = false;
        boolean expression = node.getExpression() != null;
        if (hasBuilder(Type.ArrowFunctionExpression)) {
            return call(Type.ArrowFunctionExpression, node, id, params, body, generator, expression);
        }
        OrdinaryObject function = createFunction(node, Type.ArrowFunctionExpression);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "async", async);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(AssignmentElement node, Void value) {
        if (node.getInitializer() == null) {
            return node.getTarget().accept(this, value);
        }
        return toAssignmentExpression(node.getTarget(), node.getInitializer(), value);
    }

    @Override
    public Object visit(AssignmentExpression node, Void value) {
        Object left = node.getLeft().accept(this, value);
        Object right = node.getRight().accept(this, value);
        String operator = node.getOperator().toString();
        if (hasBuilder(Type.AssignmentExpression)) {
            return call(Type.AssignmentExpression, node, operator, left, right);
        }
        OrdinaryObject expression = createExpression(node, Type.AssignmentExpression);
        addProperty(expression, "left", left);
        addProperty(expression, "right", right);
        addProperty(expression, "operator", operator);
        return expression;
    }

    @Override
    public Object visit(AssignmentProperty node, Void value) {
        if (node.getPropertyName() != null) {
            String propertyName = node.getPropertyName().getName();
            if ("__proto__".equals(propertyName)) {
                Object key = createLiteral(node.getPropertyName(), "__proto__");
                Object _value;
                if (node.getInitializer() == null) {
                    _value = node.getTarget().accept(this, value);
                } else {
                    _value = toAssignmentExpression(node.getTarget(), node.getInitializer(), value);
                }
                if (hasBuilder(Type.PropertyPattern)) {
                    return call(Type.PropertyPattern, node, "init", key, _value);
                }
                OrdinaryObject property = createNode(node, Type.Property); // not PropertyPattern!
                addProperty(property, "key", key);
                addProperty(property, "value", _value);
                addProperty(property, "kind", "init");
                addProperty(property, "method", false);
                addProperty(property, "shorthand", false);
                addProperty(property, "computed", false);
                return property;
            }
        }

        Object key;
        if (node.getPropertyName() == null) {
            key = node.getTarget().accept(this, value);
        } else {
            key = node.getPropertyName().accept(this, value);
        }
        Object _value;
        if (node.getInitializer() == null) {
            _value = node.getTarget().accept(this, value);
        } else {
            _value = toAssignmentExpression(node.getTarget(), node.getInitializer(), value);
        }
        String kind = "init";
        boolean method = false;
        boolean shorthand = node.getPropertyName() == null;
        boolean computed = node.getPropertyName() instanceof ComputedPropertyName;
        if (hasBuilder(Type.PropertyPattern)) {
            return call(Type.PropertyPattern, node, kind, key, _value);
        }
        OrdinaryObject property = createNode(node, Type.Property); // not PropertyPattern!
        addProperty(property, "key", key);
        addProperty(property, "value", _value);
        addProperty(property, "kind", kind);
        addProperty(property, "method", method);
        addProperty(property, "shorthand", shorthand);
        addProperty(property, "computed", computed);
        return property;
    }

    @Override
    public Object visit(AssignmentRestElement node, Void value) {
        Object expr = node.getTarget().accept(this, value);
        if (hasBuilder(Type.SpreadExpression)) {
            return call(Type.SpreadExpression, node, expr);
        }
        OrdinaryObject expression = createNode(node, Type.SpreadExpression);
        addProperty(expression, "expression", expr);
        return expression;
    }

    @Override
    public Object visit(AssignmentRestProperty node, Void value) {
        Object expr = node.getTarget().accept(this, value);
        if (hasBuilder(Type.SpreadExpression)) {
            return call(Type.SpreadExpression, node, expr);
        }
        OrdinaryObject expression = createNode(node, Type.SpreadExpression);
        addProperty(expression, "expression", expr);
        return expression;
    }

    @Override
    public Object visit(AsyncArrowFunction node, Void value) {
        Object id = NULL;
        ArrayObject params = createList(getParameterBindings(node.getParameters()), value);
        ArrayObject defaults = createListWithNull(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body;
        if (node.getExpression() == null) {
            body = createFunctionBody(node, value);
        } else {
            body = node.getExpression().accept(this, value);
        }
        boolean generator = false;
        boolean async = true;
        boolean expression = node.getExpression() != null;
        if (hasBuilder(Type.ArrowFunctionExpression)) {
            return call(Type.ArrowFunctionExpression, node, id, params, body, generator, expression);
        }
        OrdinaryObject function = createFunction(node, Type.ArrowFunctionExpression);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "async", async);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(AsyncFunctionDeclaration node, Void value) {
        Object id = createDeclarationIdentifier(node, value);
        ArrayObject params = createList(getParameterBindings(node.getParameters()), value);
        ArrayObject defaults = createListWithNull(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        boolean generator = false;
        boolean async = true;
        boolean expression = false;
        if (hasBuilder(Type.FunctionDeclaration)) {
            return call(Type.FunctionDeclaration, node, id, params, body, generator, expression);
        }
        OrdinaryObject function = createFunction(node, Type.FunctionDeclaration);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "async", async);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(AsyncFunctionExpression node, Void value) {
        Object id = acceptOrNull(node.getIdentifier(), value);
        ArrayObject params = createList(getParameterBindings(node.getParameters()), value);
        ArrayObject defaults = createListWithNull(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        boolean generator = false;
        boolean async = true;
        boolean expression = false;
        if (hasBuilder(Type.FunctionExpression)) {
            return call(Type.FunctionExpression, node, id, params, body, generator, expression);
        }
        OrdinaryObject function = createFunction(node, Type.FunctionExpression);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "async", async);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(AsyncGeneratorDeclaration node, Void value) {
        Object id = createDeclarationIdentifier(node, value);
        ArrayObject params = createList(getParameterBindings(node.getParameters()), value);
        ArrayObject defaults = createListWithNull(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        // TODO: flag for async generator
        boolean generator = false;
        boolean async = false;
        boolean expression = false;
        if (hasBuilder(Type.FunctionDeclaration)) {
            return call(Type.FunctionDeclaration, node, id, params, body, generator, expression);
        }
        OrdinaryObject function = createFunction(node, Type.FunctionDeclaration);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "async", async);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(AsyncGeneratorExpression node, Void value) {
        Object id = acceptOrNull(node.getIdentifier(), value);
        ArrayObject params = createList(getParameterBindings(node.getParameters()), value);
        ArrayObject defaults = createListWithNull(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        // TODO: flag for async generator
        boolean generator = false;
        boolean async = false;
        boolean expression = false;
        if (hasBuilder(Type.FunctionExpression)) {
            return call(Type.FunctionExpression, node, id, params, body, generator, expression);
        }
        OrdinaryObject function = createFunction(node, Type.FunctionExpression);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "async", async);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(AwaitExpression node, Void value) {
        Object argument = acceptOrNull(node.getExpression(), value);
        OrdinaryObject expression = createExpression(node, Type.UnaryExpression);
        addProperty(expression, "argument", argument);
        addProperty(expression, "operator", "await");
        addProperty(expression, "prefix", true);
        return expression;
    }

    @Override
    public Object visit(BigIntegerLiteral node, Void value) {
        return createLiteral(node);
    }

    @Override
    public Object visit(BinaryExpression node, Void value) {
        Object left = node.getLeft().accept(this, value);
        Object right = node.getRight().accept(this, value);
        String operator = node.getOperator().toString();
        if (hasBuilder(type(node))) {
            return call(type(node), node, operator, left, right);
        }
        OrdinaryObject expression = createExpression(node, type(node));
        addProperty(expression, "left", left);
        addProperty(expression, "right", right);
        addProperty(expression, "operator", operator);
        return expression;
    }

    private static Type type(BinaryExpression node) {
        switch (node.getOperator()) {
        case AND:
        case OR:
            return Type.LogicalExpression;
        default:
            return Type.BinaryExpression;
        }
    }

    @Override
    public Object visit(BindingElement node, Void value) {
        if (node.getInitializer() == null) {
            return node.getBinding().accept(this, value);
        }
        return toAssignmentExpression(node.getBinding(), node.getInitializer(), value);
    }

    @Override
    public Object visit(BindingElision node, Void value) {
        return NULL;
    }

    @Override
    public Object visit(BindingIdentifier node, Void value) {
        String name = node.getName().getIdentifier();
        if (hasBuilder(Type.Identifier)) {
            return call(Type.Identifier, node, name);
        }
        OrdinaryObject binding = createBinding(node, Type.Identifier);
        addProperty(binding, "name", name);
        return binding;
    }

    @Override
    public Object visit(BindingProperty node, Void value) {
        Object key;
        if (node.getPropertyName() == null) {
            // BindingProperty : SingleNameBinding
            key = node.getBinding().accept(this, value);
        } else {
            key = node.getPropertyName().accept(this, value);
        }
        Object _value;
        if (node.getInitializer() == null) {
            _value = node.getBinding().accept(this, value);
        } else {
            _value = toAssignmentExpression(node.getBinding(), node.getInitializer(), value);
        }
        String kind = "init";
        boolean method = false;
        boolean shorthand = node.getPropertyName() == null;
        boolean computed = node.getPropertyName() instanceof ComputedPropertyName;
        if (hasBuilder(Type.PropertyPattern)) {
            return call(Type.PropertyPattern, node, kind, key, _value);
        }
        OrdinaryObject property = createNode(node, Type.Property); // not PropertyPattern!
        addProperty(property, "key", key);
        addProperty(property, "value", _value);
        addProperty(property, "kind", kind);
        addProperty(property, "method", method);
        addProperty(property, "shorthand", shorthand);
        addProperty(property, "computed", computed);
        return property;
    }

    @Override
    public Object visit(BindingRestElement node, Void value) {
        Object expr = node.getBinding().accept(this, value);
        if (hasBuilder(Type.SpreadExpression)) {
            return call(Type.SpreadExpression, node, expr);
        }
        OrdinaryObject expression = createNode(node, Type.SpreadExpression);
        addProperty(expression, "expression", expr);
        return expression;
    }

    @Override
    public Object visit(BindingRestProperty node, Void value) {
        Object expr = node.getBinding().accept(this, value);
        if (hasBuilder(Type.SpreadExpression)) {
            return call(Type.SpreadExpression, node, expr);
        }
        OrdinaryObject expression = createNode(node, Type.SpreadExpression);
        addProperty(expression, "expression", expr);
        return expression;
    }

    @Override
    public Object visit(BlockStatement node, Void value) {
        ArrayObject body = createList(node.getStatements(), value);
        if (hasBuilder(Type.BlockStatement)) {
            return call(Type.BlockStatement, node, body);
        }
        OrdinaryObject statement = createStatement(node, Type.BlockStatement);
        addProperty(statement, "body", body);
        return statement;
    }

    @Override
    public Object visit(BooleanLiteral node, Void value) {
        return createLiteral(node);
    }

    @Override
    public Object visit(BreakStatement node, Void value) {
        Object label = createIdentifierOrNull(node.getLabel());
        if (hasBuilder(Type.BreakStatement)) {
            return call(Type.BreakStatement, node, label);
        }
        OrdinaryObject statement = createStatement(node, Type.BreakStatement);
        addProperty(statement, "label", label);
        return statement;
    }

    @Override
    public Object visit(CallExpression node, Void value) {
        Object callee = node.getBase().accept(this, value);
        ArrayObject arguments = createList(node.getArguments(), value);
        if (hasBuilder(Type.CallExpression)) {
            return call(Type.CallExpression, node, callee, arguments);
        }
        OrdinaryObject expression = createExpression(node, Type.CallExpression);
        addProperty(expression, "callee", callee);
        addProperty(expression, "arguments", arguments);
        return expression;
    }

    @Override
    public Object visit(CallSpreadElement node, Void value) {
        Object expr = node.getExpression().accept(this, value);
        if (hasBuilder(Type.SpreadExpression)) {
            return call(Type.SpreadExpression, node, expr);
        }
        OrdinaryObject expression = createExpression(node, Type.SpreadExpression);
        addProperty(expression, "expression", expr);
        return expression;
    }

    @Override
    public Object visit(CatchNode node, Void value) {
        Object param = acceptOrNull(node.getCatchParameter(), value);
        Object guard = NULL;
        Object body = node.getCatchBlock().accept(this, value);
        if (hasBuilder(Type.CatchClause)) {
            return call(Type.CatchClause, node, param, guard, body);
        }
        OrdinaryObject catchClause = createNode(node, Type.CatchClause);
        addProperty(catchClause, "param", param);
        addProperty(catchClause, "guard", guard);
        addProperty(catchClause, "body", body);
        return catchClause;
    }

    @Override
    public Object visit(ClassDeclaration node, Void value) {
        Object id = createDeclarationIdentifier(node, value);
        Object superClass = acceptOrNull(node.getHeritage(), value);
        Object body = createClassBody(node, value);
        OrdinaryObject classDef = createClass(node, Type.ClassStatement);
        addProperty(classDef, "id", id);
        addProperty(classDef, "superClass", superClass);
        addProperty(classDef, "body", body);
        return classDef;
    }

    @Override
    public Object visit(ClassExpression node, Void value) {
        Object id = acceptOrNull(node.getIdentifier(), value);
        Object superClass = acceptOrNull(node.getHeritage(), value);
        Object body = createClassBody(node, value);
        OrdinaryObject classDef = createClass(node, Type.ClassExpression);
        addProperty(classDef, "id", id);
        addProperty(classDef, "superClass", superClass);
        addProperty(classDef, "body", body);
        return classDef;
    }

    @Override
    public Object visit(ClassFieldDefinition node, Void value) {
        Object key = node.getPropertyName().accept(this, value);
        Object _value = acceptOrNull(node.getInitializer(), value);
        OrdinaryObject property = createNode(node, Type.Property);
        addProperty(property, "key", key);
        addProperty(property, "value", _value);
        addProperty(property, "kind", "init");
        addProperty(property, "method", false);
        addProperty(property, "shorthand", false);
        addProperty(property, "computed", node.getPropertyName() instanceof ComputedPropertyName);
        return property;
    }

    @Override
    public Object visit(ClassFieldInitializer node, Void value) {
        throw new IllegalStateException();
    }

    @Override
    public Object visit(CommaExpression node, Void value) {
        ArrayObject expressions = createList(node.getOperands(), value);
        if (hasBuilder(Type.SequenceExpression)) {
            return call(Type.SequenceExpression, node, expressions);
        }
        OrdinaryObject expression = createExpression(node, Type.SequenceExpression);
        addProperty(expression, "expressions", expressions);
        return expression;
    }

    @Override
    public Object visit(Comprehension node, Void value) {
        // multiple filters possible in Comprehension, single element 'filter' useless here...
        Object body = node.getExpression().accept(this, value);
        ArrayObject blocks = createList(node.getList(), value);
        Object filter = NULL;
        OrdinaryObject expression = createEmptyNode();
        addProperty(expression, "body", body);
        addProperty(expression, "blocks", blocks);
        addProperty(expression, "filter", filter);
        addProperty(expression, "style", "modern");
        return expression;
    }

    @Override
    public Object visit(ComprehensionFor node, Void value) {
        Object left = node.getBinding().accept(this, value);
        Object right = node.getExpression().accept(this, value);
        boolean each = false;
        boolean of = true;
        if (hasBuilder(Type.ComprehensionBlock)) {
            return call(Type.ComprehensionBlock, node, left, right, each);
        }
        OrdinaryObject comprehensionBlock = createNode(node, Type.ComprehensionBlock);
        addProperty(comprehensionBlock, "left", left);
        addProperty(comprehensionBlock, "right", right);
        addProperty(comprehensionBlock, "each", each);
        addProperty(comprehensionBlock, "of", of);
        return comprehensionBlock;
    }

    @Override
    public Object visit(ComprehensionIf node, Void value) {
        Object test = node.getTest().accept(this, value);
        if (hasBuilder(Type.ComprehensionBlock)) {
            return call(Type.ComprehensionBlock, node, test);
        }
        OrdinaryObject comprehensionIf = createNode(node, Type.ComprehensionIf);
        addProperty(comprehensionIf, "test", test);
        return comprehensionIf;
    }

    @Override
    public Object visit(ComputedPropertyName node, Void value) {
        Object expr = node.getExpression().accept(this, value);
        if (hasBuilder(Type.ComputedName)) {
            return call(Type.ComputedName, node, expr);
        }
        OrdinaryObject propertyName = createNode(node, Type.ComputedName);
        addProperty(propertyName, "name", expr);
        return propertyName;
    }

    @Override
    public Object visit(ConditionalExpression node, Void value) {
        Object test = node.getTest().accept(this, value);
        Object consequent = node.getThen().accept(this, value);
        Object alternate = node.getOtherwise().accept(this, value);
        if (hasBuilder(Type.ConditionalExpression)) {
            return call(Type.ConditionalExpression, node, test, consequent, alternate);
        }
        OrdinaryObject expression = createExpression(node, Type.ConditionalExpression);
        addProperty(expression, "test", test);
        addProperty(expression, "consequent", consequent);
        addProperty(expression, "alternate", alternate);
        return expression;
    }

    @Override
    public Object visit(ContinueStatement node, Void value) {
        Object label = createIdentifierOrNull(node.getLabel());
        if (hasBuilder(Type.ContinueStatement)) {
            return call(Type.ContinueStatement, node, label);
        }
        OrdinaryObject statement = createStatement(node, Type.ContinueStatement);
        addProperty(statement, "label", label);
        return statement;
    }

    @Override
    public Object visit(DebuggerStatement node, Void value) {
        if (hasBuilder(Type.DebuggerStatement)) {
            return call(Type.DebuggerStatement, node);
        }
        return createStatement(node, Type.DebuggerStatement);
    }

    @Override
    public Object visit(DoExpression node, Void value) {
        OrdinaryObject expression = createExpression(node, Type.DoExpression);
        addProperty(expression, "statement", node.getStatement().accept(this, value));
        return expression;
    }

    @Override
    public Object visit(DoWhileStatement node, Void value) {
        Object doWhileStatement;
        Object test = node.getTest().accept(this, value);
        Object body = node.getStatement().accept(this, value);
        if (hasBuilder(Type.DoWhileStatement)) {
            doWhileStatement = call(Type.DoWhileStatement, node, test, body);
        } else {
            OrdinaryObject statement = createStatement(node, Type.DoWhileStatement);
            addProperty(statement, "test", test);
            addProperty(statement, "body", body);
            doWhileStatement = statement;
        }
        return createLabelledStatement(node, doWhileStatement);
    }

    @Override
    public Object visit(ElementAccessor node, Void value) {
        Object object = node.getBase().accept(this, value);
        Object property = node.getElement().accept(this, value);
        boolean computed = true;
        if (hasBuilder(Type.MemberExpression)) {
            return call(Type.MemberExpression, node, object, property, computed);
        }
        OrdinaryObject expression = createExpression(node, Type.MemberExpression);
        addProperty(expression, "object", object);
        addProperty(expression, "property", property);
        addProperty(expression, "computed", computed);
        return expression;
    }

    @Override
    public Object visit(Elision node, Void value) {
        return NULL;
    }

    @Override
    public Object visit(EmptyExpression node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(EmptyStatement node, Void value) {
        if (hasBuilder(Type.EmptyStatement)) {
            return call(Type.EmptyStatement, node);
        }
        return createStatement(node, Type.EmptyStatement);
    }

    @Override
    public Object visit(ExportDeclaration node, Void value) {
        Object declaration = NULL;
        Object specifiers = NULL;
        Object source = NULL;
        boolean isDefault = false;
        switch (node.getType()) {
        case All:
            specifiers = createListFromValues(Collections.singletonList(createNode(Type.ExportBatchSpecifier)));
            source = createLiteral(node.getModuleSpecifier());
            break;
        case External:
            // TODO: default entry and namespace export
            specifiers = node.getExportClause().accept(this, value);
            source = createLiteral(node.getModuleSpecifier());
            break;
        case Local:
            specifiers = node.getExportClause().accept(this, value);
            break;
        case Variable:
            declaration = node.getVariableStatement().accept(this, value);
            break;
        case Declaration:
            declaration = node.getDeclaration().accept(this, value);
            break;
        case DefaultHoistableDeclaration:
            declaration = node.getHoistableDeclaration().accept(this, value);
            isDefault = true;
            break;
        case DefaultClassDeclaration:
            declaration = node.getClassDeclaration().accept(this, value);
            isDefault = true;
            break;
        case DefaultExpression:
            declaration = node.getExpression().accept(this, value);
            isDefault = true;
            break;
        default:
            throw new AssertionError();
        }

        OrdinaryObject exportDecl = createModuleItem(node, Type.ExportDeclaration);
        addProperty(exportDecl, "declaration", declaration);
        addProperty(exportDecl, "specifiers", specifiers);
        addProperty(exportDecl, "source", source);
        addProperty(exportDecl, "isDefault", isDefault);
        return exportDecl;
    }

    @Override
    public Object visit(ExportDefaultExpression node, Void value) {
        return node.getExpression().accept(this, value);
    }

    @Override
    public Object visit(ExportSpecifier node, Void value) {
        Object id = createIdentifier(node.getSourceName());
        Object name = createIdentifier(node.getExportName());
        OrdinaryObject exportSpec = createNode(node, Type.ExportSpecifier);
        addProperty(exportSpec, "id", id);
        addProperty(exportSpec, "name", name);
        return exportSpec;
    }

    @Override
    public Object visit(ExportClause node, Void value) {
        return createList(node.getExports(), value);
    }

    @Override
    public Object visit(ExpressionMethod node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(ExpressionStatement node, Void value) {
        Object expression = node.getExpression().accept(this, value);
        if (hasBuilder(Type.ExpressionStatement)) {
            return call(Type.ExpressionStatement, node, expression);
        }
        OrdinaryObject statement = createStatement(node, Type.ExpressionStatement);
        addProperty(statement, "expression", expression);
        return statement;
    }

    @Override
    public Object visit(ForAwaitStatement node, Void value) {
        Object forAwaitStatement;
        Object left = node.getHead().accept(this, value);
        Object right = node.getExpression().accept(this, value);
        Object body = node.getStatement().accept(this, value);
        if (hasBuilder(Type.ForAwaitStatement)) {
            forAwaitStatement = call(Type.ForAwaitStatement, node, left, right, body);
        } else {
            OrdinaryObject statement = createStatement(node, Type.ForAwaitStatement);
            addProperty(statement, "left", left);
            addProperty(statement, "right", right);
            addProperty(statement, "body", body);
            forAwaitStatement = statement;
        }
        return createLabelledStatement(node, forAwaitStatement);
    }

    @Override
    public Object visit(ForInStatement node, Void value) {
        Object forInStatement;
        Object left = node.getHead().accept(this, value);
        Object right = node.getExpression().accept(this, value);
        Object body = node.getStatement().accept(this, value);
        boolean each = false;
        if (hasBuilder(Type.ForInStatement)) {
            forInStatement = call(Type.ForInStatement, node, left, right, body, each);
        } else {
            OrdinaryObject statement = createStatement(node, Type.ForInStatement);
            addProperty(statement, "left", left);
            addProperty(statement, "right", right);
            addProperty(statement, "body", body);
            addProperty(statement, "each", each);
            forInStatement = statement;
        }
        return createLabelledStatement(node, forInStatement);
    }

    @Override
    public Object visit(FormalParameter node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(FormalParameterList node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(ForOfStatement node, Void value) {
        Object forOfStatement;
        Object left = node.getHead().accept(this, value);
        Object right = node.getExpression().accept(this, value);
        Object body = node.getStatement().accept(this, value);
        if (hasBuilder(Type.ForOfStatement)) {
            forOfStatement = call(Type.ForOfStatement, node, left, right, body);
        } else {
            OrdinaryObject statement = createStatement(node, Type.ForOfStatement);
            addProperty(statement, "left", left);
            addProperty(statement, "right", right);
            addProperty(statement, "body", body);
            forOfStatement = statement;
        }
        return createLabelledStatement(node, forOfStatement);
    }

    @Override
    public Object visit(ForStatement node, Void value) {
        Object forStatement;
        Object init = acceptOrNull(node.getHead(), value);
        Object test = acceptOrNull(node.getTest(), value);
        Object update = acceptOrNull(node.getStep(), value);
        Object body = node.getStatement().accept(this, value);
        if (hasBuilder(Type.ForStatement)) {
            forStatement = call(Type.ForStatement, node, init, test, update, body);
        } else {
            OrdinaryObject statement = createStatement(node, Type.ForStatement);
            addProperty(statement, "init", init);
            addProperty(statement, "test", test);
            addProperty(statement, "update", update);
            addProperty(statement, "body", body);
            forStatement = statement;
        }
        return createLabelledStatement(node, forStatement);
    }

    @Override
    public Object visit(FunctionDeclaration node, Void value) {
        Object id = createDeclarationIdentifier(node, value);
        ArrayObject params = createList(getParameterBindings(node.getParameters()), value);
        ArrayObject defaults = createListWithNull(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        boolean generator = false;
        boolean async = false;
        boolean expression = false;
        if (hasBuilder(Type.FunctionDeclaration)) {
            return call(Type.FunctionDeclaration, node, id, params, body, generator, expression);
        }
        OrdinaryObject function = createFunction(node, Type.FunctionDeclaration);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "async", async);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(FunctionExpression node, Void value) {
        Object id = acceptOrNull(node.getIdentifier(), value);
        ArrayObject params = createList(getParameterBindings(node.getParameters()), value);
        ArrayObject defaults = createListWithNull(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        boolean generator = false;
        boolean async = false;
        boolean expression = false;
        if (hasBuilder(Type.FunctionExpression)) {
            return call(Type.FunctionExpression, node, id, params, body, generator, expression);
        }
        OrdinaryObject function = createFunction(node, Type.FunctionExpression);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "async", async);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(FunctionSent node, Void value) {
        // TODO: Attach location information.
        Object meta = createIdentifier("function");
        Object property = createIdentifier("sent");
        if (hasBuilder(Type.MetaProperty)) {
            return call(Type.MetaProperty, node, meta, property);
        }
        OrdinaryObject expression = createExpression(node, Type.MetaProperty);
        addProperty(expression, "meta", meta);
        addProperty(expression, "property", property);
        return expression;
    }

    @Override
    public Object visit(GeneratorComprehension node, Void value) {
        // Comprehension/LegacyComprehension already created a partial result
        OrdinaryObject expression = (OrdinaryObject) node.getComprehension().accept(this, value);
        if (hasBuilder(Type.GeneratorExpression)) {
            Object body = Get(cx, expression, "body");
            Object blocks = Get(cx, expression, "blocks");
            Object filter = Get(cx, expression, "filter");
            return call(Type.GeneratorExpression, node, body, blocks, filter);
        }
        addNodeInfo(expression, node, Type.GeneratorExpression);
        return expression;
    }

    @Override
    public Object visit(GeneratorDeclaration node, Void value) {
        Object id = createDeclarationIdentifier(node, value);
        ArrayObject params = createList(getParameterBindings(node.getParameters()), value);
        ArrayObject defaults = createListWithNull(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        boolean generator = true;
        boolean async = false;
        boolean expression = false;
        if (hasBuilder(Type.FunctionDeclaration)) {
            return call(Type.FunctionDeclaration, node, id, params, body, generator, expression);
        }
        GeneratorStyle style = GeneratorStyle.ES6;
        OrdinaryObject function = createFunction(node, Type.FunctionDeclaration);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "async", async);
        addProperty(function, "style", style.name);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(GeneratorExpression node, Void value) {
        Object id = acceptOrNull(node.getIdentifier(), value);
        ArrayObject params = createList(getParameterBindings(node.getParameters()), value);
        ArrayObject defaults = createListWithNull(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        boolean generator = true;
        boolean async = false;
        boolean expression = false;
        if (hasBuilder(Type.FunctionExpression)) {
            return call(Type.FunctionExpression, node, id, params, body, generator, expression);
        }
        GeneratorStyle style = GeneratorStyle.ES6;
        OrdinaryObject function = createFunction(node, Type.FunctionExpression);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "async", async);
        addProperty(function, "style", style.name);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(IdentifierName node, Void value) {
        String name = node.getName();
        if (hasBuilder(Type.Identifier)) {
            return call(Type.Identifier, node, name);
        }
        OrdinaryObject expression = createNode(node, Type.Identifier);
        addProperty(expression, "name", name);
        return expression;
    }

    @Override
    public Object visit(IdentifierReference node, Void value) {
        String name = node.getName();
        if (hasBuilder(Type.Identifier)) {
            return call(Type.Identifier, node, name);
        }
        OrdinaryObject expression = createExpression(node, Type.Identifier);
        addProperty(expression, "name", name);
        return expression;
    }

    @Override
    public Object visit(IfStatement node, Void value) {
        Object test = node.getTest().accept(this, value);
        Object consequent = node.getThen().accept(this, value);
        Object alternate = acceptOrNull(node.getOtherwise(), value);
        if (hasBuilder(Type.IfStatement)) {
            return call(Type.IfStatement, node, test, consequent, alternate);
        }
        OrdinaryObject statement = createStatement(node, Type.IfStatement);
        addProperty(statement, "test", test);
        addProperty(statement, "consequent", consequent);
        addProperty(statement, "alternate", alternate);
        return statement;
    }

    @Override
    public Object visit(ImportCallExpression node, Void value) {
        Object expr = node.getArgument().accept(this, value);
        OrdinaryObject expression = createExpression(node, Type.ImportCall);
        addProperty(expression, "expression", expr);
        return expression;
    }

    @Override
    public Object visit(ImportDeclaration node, Void value) {
        Object specifiers = NULL;
        Object source = NULL;
        switch (node.getType()) {
        case ImportFrom:
            specifiers = node.getImportClause().accept(this, value);
            source = createLiteral(node.getModuleSpecifier());
            break;
        case ImportModule:
            specifiers = createList(Collections.<Node> emptyList(), value);
            source = createLiteral(node.getModuleSpecifier());
            break;
        default:
            throw new AssertionError();
        }
        OrdinaryObject importDecl = createModuleItem(node, Type.ImportDeclaration);
        addProperty(importDecl, "specifiers", specifiers);
        addProperty(importDecl, "source", source);
        return importDecl;
    }

    @Override
    public Object visit(ImportClause node, Void value) {
        ArrayList<Object> specifiers = new ArrayList<>();
        if (node.getDefaultEntry() != null) {
            specifiers.add(createImportSpecifier(node, "default", node.getDefaultEntry(), value));
        }
        if (node.getNameSpace() != null) {
            specifiers.add(createImportSpecifier(node, "*", node.getNameSpace(), value));
        }
        for (ImportSpecifier specifier : node.getNamedImports()) {
            specifiers.add(specifier.accept(this, value));
        }
        return createListFromValues(specifiers);
    }

    @Override
    public Object visit(ImportMeta node, Void value) {
        // TODO: Attach location information.
        Object meta = createIdentifier("import");
        Object property = createIdentifier("meta");
        if (hasBuilder(Type.MetaProperty)) {
            return call(Type.MetaProperty, node, meta, property);
        }
        OrdinaryObject expression = createExpression(node, Type.MetaProperty);
        addProperty(expression, "meta", meta);
        addProperty(expression, "property", property);
        return expression;
    }

    @Override
    public Object visit(ImportSpecifier node, Void value) {
        return createImportSpecifier(node, node.getImportName(), node.getLocalName(), value);
    }

    private OrdinaryObject createImportSpecifier(Node node, String importName, BindingIdentifier localName,
            Void value) {
        Object id = createIdentifier(importName);
        Object name = localName.accept(this, value);
        OrdinaryObject importSpec = createNode(node, Type.ImportSpecifier);
        addProperty(importSpec, "id", id);
        addProperty(importSpec, "name", name);
        return importSpec;
    }

    @Override
    public Object visit(SpreadElementMethod node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(PropertyDefinitionsMethod node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(LabelledFunctionStatement node, Void value) {
        Object body = node.getFunction().accept(this, value);
        return createLabelledStatement(node, body);
    }

    @Override
    public Object visit(LabelledStatement node, Void value) {
        Object body = node.getStatement().accept(this, value);
        return createLabelledStatement(node, body);
    }

    @Override
    public Object visit(LexicalBinding node, Void value) {
        Object id = node.getBinding().accept(this, value);
        Object init = acceptOrNull(node.getInitializer(), value);
        if (hasBuilder(Type.VariableDeclarator)) {
            return call(Type.VariableDeclarator, node, id, init);
        }
        OrdinaryObject declarator = createNode(node, Type.VariableDeclarator);
        addProperty(declarator, "id", id);
        addProperty(declarator, "init", init);
        return declarator;
    }

    @Override
    public Object visit(LexicalDeclaration node, Void value) {
        ArrayObject declarations = createList(node.getElements(), value);
        String kind = node.isConstDeclaration() ? "const" : "let";
        if (hasBuilder(Type.VariableDeclaration)) {
            return call(Type.VariableDeclaration, node, kind, declarations);
        }
        OrdinaryObject declaration = createDeclaration(node, Type.VariableDeclaration);
        addProperty(declaration, "declarations", declarations);
        addProperty(declaration, "kind", kind);
        return declaration;
    }

    @Override
    public Object visit(MethodDefinition node, Void value) {
        Object key = node.getPropertyName().accept(this, value);
        Object _value = toFunctionExpression(null, node, value);
        String kind = methodKind(node, "init");
        boolean method = !isGetterOrSetter(node);
        boolean shorthand = false;
        boolean computed = node.getPropertyName() instanceof ComputedPropertyName;
        if (hasBuilder(Type.Property)) {
            return call(Type.Property, node, kind, key, _value);
        }
        OrdinaryObject property = createNode(node, Type.Property);
        addProperty(property, "key", key);
        addProperty(property, "value", _value);
        addProperty(property, "kind", kind);
        addProperty(property, "method", method);
        addProperty(property, "shorthand", shorthand);
        addProperty(property, "computed", computed);
        return property;
    }

    @Override
    public Object visit(MethodDefinitionsMethod node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(Module node, Void value) {
        ArrayObject body = createList(node.getStatements(), value);
        if (hasBuilder(Type.Program)) {
            return call(Type.Program, node, body);
        }
        OrdinaryObject program = createNode(node, Type.Program);
        addProperty(program, "body", body);
        addProperty(program, "sourceType", "module");
        return program;
    }

    @Override
    public Object visit(NativeCallExpression node, Void value) {
        Object callee = node.getBase().accept(this, value);
        ArrayObject arguments = createList(node.getArguments(), value);
        if (hasBuilder(Type.CallExpression)) {
            return call(Type.CallExpression, node, callee, arguments);
        }
        OrdinaryObject expression = createExpression(node, Type.CallExpression);
        addProperty(expression, "callee", callee);
        addProperty(expression, "arguments", arguments);
        addProperty(expression, "native", true);
        return expression;
    }

    @Override
    public Object visit(NewExpression node, Void value) {
        Object callee = node.getExpression().accept(this, value);
        ArrayObject arguments = createList(node.getArguments(), value);
        if (hasBuilder(Type.NewExpression)) {
            return call(Type.NewExpression, node, callee, arguments);
        }
        OrdinaryObject expression = createExpression(node, Type.NewExpression);
        addProperty(expression, "callee", callee);
        addProperty(expression, "arguments", arguments);
        return expression;
    }

    @Override
    public Object visit(NewTarget node, Void value) {
        // TODO: Attach location information.
        Object meta = createIdentifier("new");
        Object property = createIdentifier("target");
        if (hasBuilder(Type.MetaProperty)) {
            return call(Type.MetaProperty, node, meta, property);
        }
        OrdinaryObject expression = createExpression(node, Type.MetaProperty);
        addProperty(expression, "meta", meta);
        addProperty(expression, "property", property);
        return expression;
    }

    @Override
    public Object visit(NullLiteral node, Void value) {
        return createLiteral(node, NULL);
    }

    @Override
    public Object visit(NumericLiteral node, Void value) {
        return createLiteral(node);
    }

    @Override
    public Object visit(ObjectAssignmentPattern node, Void value) {
        ArrayList<Node> propertiesWithRest = new ArrayList<>(node.getProperties());
        if (node.getRest() != null) {
            propertiesWithRest.add(node.getRest());
        }
        ArrayObject properties = createList(propertiesWithRest, value);
        if (hasBuilder(Type.ObjectPattern)) {
            return call(Type.ObjectPattern, node, properties);
        }
        OrdinaryObject pattern = createPattern(node, Type.ObjectPattern);
        addProperty(pattern, "properties", properties);
        return pattern;
    }

    @Override
    public Object visit(ObjectBindingPattern node, Void value) {
        ArrayList<Node> propertiesWithRest = new ArrayList<>(node.getProperties());
        if (node.getRest() != null) {
            propertiesWithRest.add(node.getRest());
        }
        ArrayObject properties = createList(propertiesWithRest, value);
        if (hasBuilder(Type.ObjectPattern)) {
            return call(Type.ObjectPattern, node, properties);
        }
        OrdinaryObject pattern = createPattern(node, Type.ObjectPattern);
        addProperty(pattern, "properties", properties);
        return pattern;
    }

    @Override
    public Object visit(ObjectLiteral node, Void value) {
        ArrayObject properties = createList(node.getProperties(), value);
        if (hasBuilder(Type.ObjectExpression)) {
            return call(Type.ObjectExpression, node, properties);
        }
        OrdinaryObject expression = createExpression(node, Type.ObjectExpression);
        addProperty(expression, "properties", properties);
        return expression;
    }

    @Override
    public Object visit(PrivateNameProperty node, Void value) {
        Object name = createIdentifier(node.toPropertyName().getName());
        OrdinaryObject privateName = createNode(node, Type.PrivateName);
        addProperty(privateName, "name", name);
        return privateName;
    }

    @Override
    public Object visit(PrivatePropertyAccessor node, Void value) {
        Object object = node.getBase().accept(this, value);
        Object property = createIdentifier(node.getPrivateName().getName());
        boolean computed = false;
        if (hasBuilder(Type.MemberExpression)) {
            return call(Type.MemberExpression, node, object, property, computed);
        }
        OrdinaryObject expression = createExpression(node, Type.MemberExpression);
        addProperty(expression, "object", object);
        addProperty(expression, "property", property);
        addProperty(expression, "computed", computed);
        return expression;
    }

    @Override
    public Object visit(PropertyAccessor node, Void value) {
        Object object = node.getBase().accept(this, value);
        Object property = createIdentifier(node.getName());
        boolean computed = false;
        if (hasBuilder(Type.MemberExpression)) {
            return call(Type.MemberExpression, node, object, property, computed);
        }
        OrdinaryObject expression = createExpression(node, Type.MemberExpression);
        addProperty(expression, "object", object);
        addProperty(expression, "property", property);
        addProperty(expression, "computed", computed);
        return expression;
    }

    @Override
    public Object visit(PropertyNameDefinition node, Void value) {
        Object key = node.getPropertyName().accept(this, value);
        Object _value = key;
        String kind = "init";
        boolean method = false;
        boolean shorthand = true;
        boolean computed = false;
        if (hasBuilder(Type.Property)) {
            return call(Type.Property, node, kind, key, _value);
        }
        OrdinaryObject property = createNode(node, Type.Property);
        addProperty(property, "key", key);
        addProperty(property, "value", _value);
        addProperty(property, "kind", kind);
        addProperty(property, "method", method);
        addProperty(property, "shorthand", shorthand);
        addProperty(property, "computed", computed);
        return property;
    }

    @Override
    public Object visit(PropertyValueDefinition node, Void value) {
        String propertyName = node.getPropertyName().getName();
        if ("__proto__".equals(propertyName)) {
            Object _value = node.getPropertyValue().accept(this, value);
            if (hasBuilder(Type.PrototypeMutation)) {
                return call(Type.PrototypeMutation, node, _value);
            }
            OrdinaryObject property = createNode(node, Type.PrototypeMutation);
            addProperty(property, "value", _value);
            return property;
        }
        Object key = node.getPropertyName().accept(this, value);
        Object _value = node.getPropertyValue().accept(this, value);
        String kind = "init";
        boolean method = false;
        boolean shorthand = false;
        boolean computed = node.getPropertyName() instanceof ComputedPropertyName;
        if (hasBuilder(Type.Property)) {
            return call(Type.Property, node, kind, key, _value);
        }
        OrdinaryObject property = createNode(node, Type.Property);
        addProperty(property, "key", key);
        addProperty(property, "value", _value);
        addProperty(property, "kind", kind);
        addProperty(property, "method", method);
        addProperty(property, "shorthand", shorthand);
        addProperty(property, "computed", computed);
        return property;
    }

    @Override
    public Object visit(RegularExpressionLiteral node, Void value) {
        RegExpObject _value = RegExpCreate(cx, node.getRegexp(), node.getFlags());
        if (hasBuilder(Type.Literal)) {
            return call(Type.Literal, node, _value);
        }
        OrdinaryObject expression = createExpression(node, Type.Literal);
        addProperty(expression, "value", _value);
        return expression;
    }

    @Override
    public Object visit(ReturnStatement node, Void value) {
        Object argument = acceptOrNull(node.getExpression(), value);
        if (hasBuilder(Type.ReturnStatement)) {
            return call(Type.ReturnStatement, node, argument);
        }
        OrdinaryObject statement = createStatement(node, Type.ReturnStatement);
        addProperty(statement, "argument", argument);
        return statement;
    }

    @Override
    public Object visit(Script node, Void value) {
        ArrayObject body = createList(node.getStatements(), value);
        if (hasBuilder(Type.Program)) {
            return call(Type.Program, node, body);
        }
        OrdinaryObject program = createNode(node, Type.Program);
        addProperty(program, "body", body);
        addProperty(program, "sourceType", "script");
        return program;
    }

    @Override
    public Object visit(SpreadElement node, Void value) {
        Object expr = node.getExpression().accept(this, value);
        if (hasBuilder(Type.SpreadExpression)) {
            return call(Type.SpreadExpression, node, expr);
        }
        OrdinaryObject expression = createExpression(node, Type.SpreadExpression);
        addProperty(expression, "expression", expr);
        return expression;
    }

    @Override
    public Object visit(SpreadProperty node, Void value) {
        Object expr = node.getExpression().accept(this, value);
        if (hasBuilder(Type.SpreadExpression)) {
            return call(Type.SpreadExpression, node, expr);
        }
        OrdinaryObject expression = createNode(node, Type.SpreadExpression);
        addProperty(expression, "expression", expr);
        return expression;
    }

    @Override
    public Object visit(StatementListMethod node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(StringLiteral node, Void value) {
        return createLiteral(node);
    }

    @Override
    public Object visit(SuperCallExpression node, Void value) {
        Object callee = superNode(node);
        ArrayObject arguments = createList(node.getArguments(), value);
        if (hasBuilder(Type.CallExpression)) {
            return call(Type.CallExpression, node, callee, arguments);
        }
        OrdinaryObject expression = createExpression(node, Type.CallExpression);
        addProperty(expression, "callee", callee);
        addProperty(expression, "arguments", arguments);
        return expression;
    }

    @Override
    public Object visit(SuperElementAccessor node, Void value) {
        Object object = superNode(node);
        Object property = node.getElement().accept(this, value);
        boolean computed = true;
        if (hasBuilder(Type.MemberExpression)) {
            return call(Type.MemberExpression, node, object, property, computed);
        }
        OrdinaryObject expression = createExpression(node, Type.MemberExpression);
        addProperty(expression, "object", object);
        addProperty(expression, "property", property);
        addProperty(expression, "computed", computed);
        return expression;
    }

    @Override
    public Object visit(SuperNewExpression node, Void value) {
        Object callee = superNode(node);
        ArrayObject arguments = createList(node.getArguments(), value);
        if (hasBuilder(Type.NewExpression)) {
            return call(Type.NewExpression, node, callee, arguments);
        }
        OrdinaryObject expression = createExpression(node, Type.NewExpression);
        addProperty(expression, "callee", callee);
        addProperty(expression, "arguments", arguments);
        return expression;
    }

    @Override
    public Object visit(SuperPropertyAccessor node, Void value) {
        Object object = superNode(node);
        Object property = createIdentifier(node.getName());
        boolean computed = false;
        if (hasBuilder(Type.MemberExpression)) {
            return call(Type.MemberExpression, node, object, property, computed);
        }
        OrdinaryObject expression = createExpression(node, Type.MemberExpression);
        addProperty(expression, "object", object);
        addProperty(expression, "property", property);
        addProperty(expression, "computed", computed);
        return expression;
    }

    private Object superNode(Node node) {
        // TODO: Attach correct source location.
        if (hasBuilder(Type.Super)) {
            return call(Type.Super, node);
        }
        return createNode(node, Type.Super);
    }

    @Override
    public Object visit(SwitchClause node, Void value) {
        Object test = acceptOrNull(node.getExpression(), value);
        ArrayObject consequent = createList(node.getStatements(), value);
        if (hasBuilder(Type.SwitchCase)) {
            return call(Type.SwitchCase, node, test, consequent);
        }
        OrdinaryObject switchCase = createNode(node, Type.SwitchCase);
        addProperty(switchCase, "test", test);
        addProperty(switchCase, "consequent", consequent);
        return switchCase;
    }

    @Override
    public Object visit(SwitchStatement node, Void value) {
        Object switchStatement;
        Object discriminant = node.getExpression().accept(this, value);
        ArrayObject cases = createList(node.getClauses(), value);
        boolean lexical = !LexicallyScopedDeclarations(node).isEmpty();
        if (hasBuilder(Type.SwitchStatement)) {
            switchStatement = call(Type.SwitchStatement, node, discriminant, cases, lexical);
        } else {
            OrdinaryObject statement = createStatement(node, Type.SwitchStatement);
            addProperty(statement, "discriminant", discriminant);
            addProperty(statement, "cases", cases);
            addProperty(statement, "lexical", lexical);
            switchStatement = statement;
        }
        return createLabelledStatement(node, switchStatement);
    }

    @Override
    public Object visit(TemplateCallExpression node, Void value) {
        Object callee = node.getBase().accept(this, value);
        Object arguments = node.getTemplate().accept(this, value);
        if (hasBuilder(Type.TaggedTemplate)) {
            return call(Type.TaggedTemplate, node, callee, arguments);
        }
        OrdinaryObject expression = createExpression(node, Type.TaggedTemplate);
        addProperty(expression, "callee", callee);
        addProperty(expression, "arguments", arguments);
        return expression;
    }

    @Override
    public Object visit(TemplateCharacters node, Void value) {
        String raw = node.getRawValue();
        String cooked = node.getValue();
        assert cooked != null;
        if (hasBuilder(Type.TemplateLiteral)) {
            return call(Type.TemplateLiteral, node, raw, cooked);
        }
        OrdinaryObject expression = createExpression(node, Type.TemplateLiteral);
        addProperty(expression, "raw", raw);
        addProperty(expression, "cooked", cooked);
        return expression;
    }

    @Override
    public Object visit(TemplateLiteral node, Void value) {
        if (!node.isTagged()) {
            ArrayList<Object> list = new ArrayList<>();
            for (Expression element : node.getElements()) {
                if (element instanceof TemplateCharacters) {
                    list.add(createLiteral((TemplateCharacters) element));
                } else {
                    list.add(element.accept(this, value));
                }
            }
            if (list.size() == 1) {
                return list.get(0);
            }
            ArrayObject elements = createListFromValues(list);
            if (hasBuilder(Type.TemplateLiteral)) {
                return call(Type.TemplateLiteral, node, elements);
            }
            OrdinaryObject expression = createExpression(node, Type.TemplateLiteral);
            addProperty(expression, "elements", elements);
            return expression;
        } else {
            ArrayList<String> rawList = new ArrayList<>();
            ArrayList<Object> cookedList = new ArrayList<>();
            for (TemplateCharacters chars : TemplateStrings(node)) {
                rawList.add(chars.getRawValue());
                String cooked = chars.getValue();
                cookedList.add(cooked != null ? cooked : UNDEFINED);
            }
            Object callSiteObject;
            ArrayObject raw = createListFromValues(rawList);
            ArrayObject cooked = createListFromValues(cookedList);
            if (hasBuilder(Type.CallSiteObject)) {
                callSiteObject = call(Type.CallSiteObject, node, raw, cooked);
            } else {
                OrdinaryObject callSiteObj = createExpression(node, Type.CallSiteObject);
                addProperty(callSiteObj, "raw", raw);
                addProperty(callSiteObj, "cooked", cooked);
                callSiteObject = callSiteObj;
            }
            ArrayList<Object> arguments = new ArrayList<>();
            arguments.add(callSiteObject);
            for (Expression subst : Substitutions(node)) {
                arguments.add(subst.accept(this, value));
            }
            return createListFromValues(arguments);
        }
    }

    @Override
    public Object visit(ThisExpression node, Void value) {
        if (hasBuilder(Type.ThisExpression)) {
            return call(Type.ThisExpression, node);
        }
        return createExpression(node, Type.ThisExpression);
    }

    @Override
    public Object visit(ThrowExpression node, Void value) {
        OrdinaryObject expression = createExpression(node, Type.ThrowExpression);
        addProperty(expression, "argument", node.getExpression().accept(this, value));
        return expression;
    }

    @Override
    public Object visit(ThrowStatement node, Void value) {
        Object argument = node.getExpression().accept(this, value);
        if (hasBuilder(Type.ThrowStatement)) {
            return call(Type.ThrowStatement, node, argument);
        }
        OrdinaryObject statement = createStatement(node, Type.ThrowStatement);
        addProperty(statement, "argument", argument);
        return statement;
    }

    @Override
    public Object visit(TryStatement node, Void value) {
        Object block = node.getTryBlock().accept(this, value);
        Object handler = acceptOrNull(node.getCatchNode(), value);
        Object finalizer = acceptOrNull(node.getFinallyBlock(), value);
        if (hasBuilder(Type.TryStatement)) {
            return call(Type.TryStatement, node, block, handler, finalizer);
        }
        OrdinaryObject statement = createStatement(node, Type.TryStatement);
        addProperty(statement, "block", block);
        addProperty(statement, "handler", handler);
        addProperty(statement, "finalizer", finalizer);
        return statement;
    }

    @Override
    public Object visit(UnaryExpression node, Void value) {
        Object argument = node.getOperand().accept(this, value);
        String operator = node.getOperator().toString();
        boolean prefix = true;
        if (hasBuilder(Type.UnaryExpression)) {
            return call(Type.UnaryExpression, node, operator, argument, prefix);
        }
        OrdinaryObject expression = createExpression(node, Type.UnaryExpression);
        addProperty(expression, "argument", argument);
        addProperty(expression, "operator", operator);
        addProperty(expression, "prefix", prefix);
        return expression;
    }

    @Override
    public Object visit(UpdateExpression node, Void value) {
        Object argument = node.getOperand().accept(this, value);
        String operator = node.getOperator().toString();
        boolean prefix = !node.getOperator().isPostfix();
        if (hasBuilder(Type.UpdateExpression)) {
            return call(Type.UpdateExpression, node, operator, argument, prefix);
        }
        OrdinaryObject expression = createExpression(node, Type.UpdateExpression);
        addProperty(expression, "argument", argument);
        addProperty(expression, "operator", operator);
        addProperty(expression, "prefix", prefix);
        return expression;
    }

    @Override
    public Object visit(VariableDeclaration node, Void value) {
        Object id = node.getBinding().accept(this, value);
        Object init = acceptOrNull(node.getInitializer(), value);
        if (hasBuilder(Type.VariableDeclarator)) {
            return call(Type.VariableDeclarator, node, id, init);
        }
        OrdinaryObject declarator = createNode(node, Type.VariableDeclarator);
        addProperty(declarator, "id", id);
        addProperty(declarator, "init", init);
        return declarator;
    }

    @Override
    public Object visit(VariableStatement node, Void value) {
        ArrayObject declarations = createList(node.getElements(), value);
        String kind = "var";
        if (hasBuilder(Type.VariableDeclaration)) {
            return call(Type.VariableDeclaration, node, kind, declarations);
        }
        OrdinaryObject declaration = createDeclaration(node, Type.VariableDeclaration);
        addProperty(declaration, "declarations", declarations);
        addProperty(declaration, "kind", kind);
        return declaration;
    }

    @Override
    public Object visit(WhileStatement node, Void value) {
        Object whileStatement;
        Object test = node.getTest().accept(this, value);
        Object body = node.getStatement().accept(this, value);
        if (hasBuilder(Type.WhileStatement)) {
            whileStatement = call(Type.WhileStatement, node, test, body);
        } else {
            OrdinaryObject statement = createStatement(node, Type.WhileStatement);
            addProperty(statement, "test", test);
            addProperty(statement, "body", body);
            whileStatement = statement;
        }
        return createLabelledStatement(node, whileStatement);
    }

    @Override
    public Object visit(WithStatement node, Void value) {
        Object object = node.getExpression().accept(this, value);
        Object body = node.getStatement().accept(this, value);
        if (hasBuilder(Type.WithStatement)) {
            return call(Type.WithStatement, node, object, body);
        }
        OrdinaryObject statement = createStatement(node, Type.WithStatement);
        addProperty(statement, "object", object);
        addProperty(statement, "body", body);
        return statement;
    }

    @Override
    public Object visit(YieldExpression node, Void value) {
        Object argument = acceptOrNull(node.getExpression(), value);
        boolean delegate = node.isDelegatedYield();
        if (hasBuilder(Type.YieldExpression)) {
            return call(Type.YieldExpression, node, argument, delegate);
        }
        OrdinaryObject expression = createExpression(node, Type.YieldExpression);
        addProperty(expression, "argument", argument);
        addProperty(expression, "delegate", delegate);
        return expression;
    }
}
