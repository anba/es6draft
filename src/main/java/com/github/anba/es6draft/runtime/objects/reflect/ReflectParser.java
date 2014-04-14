/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.objects.text.RegExpConstructor.RegExpCreate;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Type.isUndefinedOrNull;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.DenseArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicalDeclarations;
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
import com.github.anba.es6draft.ast.synthetic.ElementAccessorValue;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.IdentifierValue;
import com.github.anba.es6draft.ast.synthetic.PropertyAccessorValue;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadArrayLiteral;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.ast.synthetic.SuperExpressionValue;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * https://developer.mozilla.org/en-US/docs/SpiderMonkey/Parser_API
 */
public final class ReflectParser implements NodeVisitor<Object, Void> {
    private final ExecutionContext cx;
    private final boolean location;
    private final String sourceInfo;
    @SuppressWarnings("unused")
    private final int line;
    private final EnumMap<Type, Callable> builder;

    private enum Type {/* @formatter:off */
        // Programs
        Program("program"),

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

        // Declarations
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
        ArrowExpression("arrowExpression"),
        ArrayExpression("arrayExpression"),
        ObjectExpression("objectExpression"),
        ThisExpression("thisExpression"),
        ComprehensionExpression("comprehensionExpression"),
        GeneratorExpression("generatorExpression"),
        YieldExpression("yieldExpression"),
        LetExpression("letExpression"),

        // Patterns
        ArrayPattern("arrayPattern"),
        ObjectPattern("objectPattern"),
        PropertyPattern("propertyPattern"),

        // Clauses
        SwitchCase("switchCase"),
        CatchClause("catchClause"),
        ComprehensionBlock("comprehensionBlock"),

        // Miscellaneous
        Identifier("identifier"),
        Literal("literal"),
        Property("property"),

        // New node type
        ClassDeclaration(),
        ClassExpression(),
        ClassBody(),
        ComputedPropertyName(),
        MethodDefinition(),
        SpreadExpression(),
        SuperExpression(),
        TaggedTemplateExpression(),
        TemplateLiteral(),

        // Modules
        ExportDeclaration(),
        ExportSpecifier(),
        ExportBatchSpecifier(),
        ImportDeclaration(),
        ImportSpecifier(),

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

    private static EnumMap<Type, Callable> toBuilderMap(ExecutionContext cx,
            ScriptObject builderObject) {
        EnumMap<Type, Callable> map = new EnumMap<>(Type.class);
        for (Type builder : Type.values()) {
            String methodName = builder.name;
            if (methodName == null) {
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

    private ReflectParser(ExecutionContext cx, boolean location, String sourceInfo, int line,
            EnumMap<Type, Callable> builder) {
        this.cx = cx;
        this.location = location;
        this.sourceInfo = sourceInfo;
        this.line = line;
        this.builder = builder;
    }

    @SuppressWarnings("serial")
    private static final class NotImplementedExpception extends RuntimeException {
        NotImplementedExpception(Node node) {
            super(node.getClass().toString());
        }
    }

    public static Object parse(ExecutionContext cx, String source, ScriptObject options) {
        boolean location = true;
        String sourceInfo = null;
        int line = 1;
        EnumMap<Type, Callable> builder = new EnumMap<>(Type.class);
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
        }
        return parse(cx, source, location, sourceInfo, line, builder);
    }

    public static Object parse(ExecutionContext cx, String source, boolean location,
            String sourceInfo, int line, EnumMap<Type, Callable> builder) {
        Realm realm = cx.getRealm();
        ReflectParser reflect = new ReflectParser(cx, location, sourceInfo, line, builder);
        Node parsedNode = null;
        try {
            Parser parser = new Parser("<parse>", line, realm.getOptions());
            parsedNode = parser.parseScript(source);
        } catch (ParserException ignore) {
            // TODO: Reflect.parse() currently accepts scripts and modules...
            try {
                Parser parser = new Parser("<parse>", line, realm.getOptions());
                parsedNode = parser.parseModule(source);
            } catch (ParserException e) {
                throw e.toScriptException(cx);
            }
        }
        return parsedNode.accept(reflect, null);
    }

    private ScriptObject createEmptyNode() {
        return ObjectCreate(cx);
    }

    private void addProperty(ScriptObject holder, String key, Object value) {
        CreateDataProperty(cx, holder, key, value);
    }

    private void addNodeInfo(ScriptObject holder, Node node, Type type) {
        Object loc = location ? createSourceLocation(node) : NULL;
        addSourceLocation(holder, loc);
        addType(holder, type);
    }

    private void addNodeInfo(ScriptObject holder, Type type) {
        addSourceLocation(holder, NULL);
        addType(holder, type);
    }

    private void addSourceLocation(ScriptObject holder, Object loc) {
        addProperty(holder, "loc", loc);
    }

    private void addType(ScriptObject holder, Type type) {
        addProperty(holder, "type", type.name());
    }

    private ScriptObject createSourceLocation(Node node) {
        ScriptObject loc = createEmptyNode();
        addProperty(loc, "start", createPosition(node.getBeginLine(), node.getBeginColumn()));
        addProperty(loc, "end", createPosition(node.getEndLine(), node.getEndColumn()));
        addProperty(loc, "source", sourceInfo != null ? sourceInfo : NULL);
        return loc;
    }

    private ScriptObject createPosition(int line, int column) {
        // subtract one to make columns 0-indexed
        ScriptObject pos = createEmptyNode();
        addProperty(pos, "line", line);
        addProperty(pos, "column", column - 1);
        return pos;
    }

    private ScriptObject createNode(Node node, Type type) {
        ScriptObject object = createEmptyNode();
        addNodeInfo(object, node, type);
        return object;
    }

    private ScriptObject createNode(Type type) {
        ScriptObject object = createEmptyNode();
        addNodeInfo(object, type);
        return object;
    }

    private ScriptObject createModuleItem(ModuleItem node, Type type) {
        return createNode(node, type);
    }

    private ScriptObject createStatement(Statement node, Type type) {
        return createNode(node, type);
    }

    private ScriptObject createDeclaration(LexicalDeclaration node, Type type) {
        return createNode(node, type);
    }

    private ScriptObject createDeclaration(VariableStatement node, Type type) {
        return createNode(node, type);
    }

    private ScriptObject createExpression(Expression node, Type type) {
        return createNode(node, type);
    }

    private ScriptObject createPattern(AssignmentPattern node, Type type) {
        return createNode(node, type);
    }

    private ScriptObject createPattern(BindingPattern node, Type type) {
        return createNode(node, type);
    }

    private ScriptObject createBinding(Binding node, Type type) {
        return createNode(node, type);
    }

    private ScriptObject createFunction(FunctionNode node, Type type) {
        return createNode(node, type);
    }

    private ScriptObject createClass(ClassDefinition node, Type type) {
        return createNode(node, type);
    }

    private Object createLiteral(ValueLiteral<?> node) {
        return createLiteral(node, node.getValue());
    }

    private Object createLiteral(Literal node, Object value) {
        if (hasBuilder(Type.Literal)) {
            return call(Type.Literal, node, value);
        }
        ScriptObject literal = createExpression(node, Type.Literal);
        addProperty(literal, "value", value);
        return literal;
    }

    private Object createLiteral(String value) {
        if (hasBuilder(Type.Literal)) {
            return call(Type.Literal, null, value);
        }
        ScriptObject literal = createNode(Type.Literal);
        addProperty(literal, "value", value);
        return literal;
    }

    private Object createIdentifierOrNull(String name) {
        return name != null ? createIdentifier(name) : NULL;
    }

    private Object createIdentifier(String name) {
        if (hasBuilder(Type.Identifier)) {
            return call(Type.Identifier, null, name);
        }
        ScriptObject identifier = createNode(Type.Identifier);
        addProperty(identifier, "name", name);
        return identifier;
    }

    private ScriptObject createList(List<? extends Node> nodes, Void value) {
        Object[] values = new Object[nodes.size()];
        int index = 0;
        for (Node node : nodes) {
            values[index++] = node.accept(this, value);
        }
        return DenseArrayCreate(cx, values);
    }

    private ScriptObject createListFromValues(List<? extends Object> values) {
        return DenseArrayCreate(cx, values.toArray());
    }

    private Object acceptOrNull(Node node, Void value) {
        return node != null ? node.accept(this, value) : NULL;
    }

    private static <T> T lastElement(List<T> list) {
        return !list.isEmpty() ? list.get(list.size() - 1) : null;
    }

    private List<Binding> getParameterBindings(FormalParameterList formals) {
        List<Binding> bindings = new ArrayList<>();
        for (FormalParameter formalParameter : formals) {
            if (formalParameter instanceof BindingElement) {
                bindings.add(((BindingElement) formalParameter).getBinding());
            }
        }
        return bindings;
    }

    private List<Expression> getParameterDefaults(FormalParameterList formals) {
        // esprima inserts 'null' for every parameter if any parameter has defaults...
        // final NullLiteral noDefault = new NullLiteral(0);
        boolean hasDefault = false;
        List<Expression> defaults = new ArrayList<>();
        for (FormalParameter formalParameter : formals) {
            if (formalParameter instanceof BindingElement) {
                Expression initializer = ((BindingElement) formalParameter).getInitializer();
                hasDefault |= initializer != null;
                // defaults.add(initializer != null ? initializer : noDefault);
                if (initializer != null) {
                    defaults.add(initializer);
                }
            }
        }
        return hasDefault ? defaults : Collections.<Expression> emptyList();
    }

    private BindingRestElement getRestParameter(FormalParameterList formals) {
        FormalParameter last = lastElement(formals.getFormals());
        return last instanceof BindingRestElement ? (BindingRestElement) last : null;
    }

    private List<Node> getBindingElements(List<BindingElementItem> list) {
        List<Node> elements = new ArrayList<>();
        for (BindingElementItem item : list) {
            if (item instanceof BindingElision) {
                elements.add((BindingElision) item);
            } else if (item instanceof BindingElement) {
                elements.add(((BindingElement) item).getBinding());
            }
        }
        return elements;
    }

    private List<Expression> getBindingDefaults(List<BindingElementItem> list) {
        final NullLiteral noDefault = new NullLiteral(0, 0);
        boolean hasDefault = false;
        List<Expression> defaults = new ArrayList<>();
        for (BindingElementItem item : list) {
            if (item instanceof BindingElision) {
                defaults.add(noDefault);
            } else if (item instanceof BindingElement) {
                Expression initializer = ((BindingElement) item).getInitializer();
                hasDefault |= initializer != null;
                defaults.add(initializer != null ? initializer : noDefault);
            }
        }
        return hasDefault ? defaults : Collections.<Expression> emptyList();
    }

    private BindingRestElement getRestBinding(List<BindingElementItem> list) {
        BindingElementItem last = lastElement(list);
        return last instanceof BindingRestElement ? (BindingRestElement) last : null;
    }

    private List<Expression> getAssignmentElements(List<AssignmentElementItem> list) {
        List<Expression> elements = new ArrayList<>();
        for (AssignmentElementItem item : list) {
            if (item instanceof Elision) {
                elements.add((Elision) item);
            } else if (item instanceof AssignmentElement) {
                elements.add(((AssignmentElement) item).getTarget());
            }
        }
        return elements;
    }

    private List<Expression> getAssignmentDefaults(List<AssignmentElementItem> list) {
        final NullLiteral noDefault = new NullLiteral(0, 0);
        boolean hasDefault = false;
        List<Expression> defaults = new ArrayList<>();
        for (AssignmentElementItem item : list) {
            if (item instanceof Elision) {
                defaults.add(noDefault);
            } else if (item instanceof AssignmentElement) {
                Expression initializer = ((AssignmentElement) item).getInitializer();
                hasDefault |= initializer != null;
                defaults.add(initializer != null ? initializer : noDefault);
            }
        }
        return hasDefault ? defaults : Collections.<Expression> emptyList();
    }

    private AssignmentRestElement getRestAssignment(List<AssignmentElementItem> list) {
        AssignmentElementItem last = lastElement(list);
        return last instanceof AssignmentRestElement ? (AssignmentRestElement) last : null;
    }

    private <STATEMENT extends Statement & AbruptNode> Object createLabelledStatement(
            STATEMENT node, Object body) {
        if (node.getLabelSet().isEmpty()) {
            return body;
        }
        Iterator<String> labels = new ArrayDeque<String>(node.getLabelSet()).descendingIterator();
        while (labels.hasNext()) {
            Object label = createIdentifier(labels.next());
            if (hasBuilder(Type.LabeledStatement)) {
                body = call(Type.LabeledStatement, node, label, body);
            } else {
                ScriptObject statement = createStatement(node, Type.LabeledStatement);
                addProperty(statement, "body", body);
                addProperty(statement, "label", label);
                body = statement;
            }
        }
        return body;
    }

    private Object createFunctionBody(FunctionNode node, Void value) {
        // FunctionBody is materalized as BlockStatement
        ScriptObject body = createList(node.getStatements(), value);
        if (hasBuilder(Type.BlockStatement)) {
            return call(Type.BlockStatement, node, body);
        }
        ScriptObject statement = createNode(node, Type.BlockStatement);
        addProperty(statement, "body", body);
        return statement;
    }

    private ScriptObject createClassBody(ClassDefinition node, Void value) {
        // ClassBody is materalized as a single node
        List<ScriptObject> methods = new ArrayList<>();
        for (MethodDefinition method : node.getPrototypeMethods()) {
            methods.add(createClassMethod(method, value, false));
        }
        for (MethodDefinition method : node.getStaticMethods()) {
            methods.add(createClassMethod(method, value, true));
        }
        ScriptObject body = createListFromValues(methods);
        ScriptObject classBody = createNode(node, Type.ClassBody);
        addProperty(classBody, "body", body);
        return classBody;
    }

    private ScriptObject createClassMethod(MethodDefinition node, Void value, boolean isStatic) {
        Object key = node.getPropertyName().accept(this, null);
        Object _value = toFunctionExpression(node, value);
        String kind = methodKind(node, "");
        ScriptObject property = createNode(node, Type.MethodDefinition);
        addProperty(property, "key", key);
        addProperty(property, "value", _value);
        addProperty(property, "kind", kind);
        addProperty(property, "static", isStatic);
        return property;
    }

    private Object toFunctionExpression(MethodDefinition node, Void value) {
        // esprima outputs method definitions as function expressions
        Object id = NULL;
        ScriptObject params = createList(getParameterBindings(node.getParameters()), value);
        ScriptObject defaults = createList(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        // TODO: async functions
        boolean generator = node.getType() == MethodDefinition.MethodType.Generator;
        boolean expression = false;
        if (hasBuilder(Type.FunctionExpression)) {
            return call(Type.FunctionExpression, node, id, params, body, generator, expression);
        }
        ScriptObject function = createFunction(node, Type.FunctionExpression);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "expression", expression);
        return function;
    }

    private static String methodKind(MethodDefinition method, String defaultKind) {
        switch (method.getType()) {
        case Getter:
            return "get";
        case Setter:
            return "set";
        case AsyncFunction:
        case Function:
        case Generator:
        default:
            return defaultKind;
        }
    }

    @Override
    public Object visit(ArrayAssignmentPattern node, Void value) {
        ScriptObject elements = createList(getAssignmentElements(node.getElements()), value);
        ScriptObject defaults = createList(getAssignmentDefaults(node.getElements()), value);
        Object rest = acceptOrNull(getRestAssignment(node.getElements()), value);
        if (hasBuilder(Type.ArrayPattern)) {
            return call(Type.ArrayPattern, node, elements);
        }
        ScriptObject pattern = createPattern(node, Type.ArrayPattern);
        addProperty(pattern, "elements", elements);
        addProperty(pattern, "defaults", defaults);
        addProperty(pattern, "rest", rest);
        return pattern;
    }

    @Override
    public Object visit(ArrayBindingPattern node, Void value) {
        ScriptObject elements = createList(getBindingElements(node.getElements()), value);
        ScriptObject defaults = createList(getBindingDefaults(node.getElements()), value);
        Object rest = acceptOrNull(getRestBinding(node.getElements()), value);
        if (hasBuilder(Type.ArrayPattern)) {
            return call(Type.ArrayPattern, node, elements);
        }
        ScriptObject pattern = createPattern(node, Type.ArrayPattern);
        addProperty(pattern, "elements", elements);
        addProperty(pattern, "defaults", defaults);
        addProperty(pattern, "rest", rest);
        return pattern;
    }

    @Override
    public Object visit(ArrayComprehension node, Void value) {
        // Comprehension/LegacyComprehension already created a partial result
        ScriptObject expression = (ScriptObject) node.getComprehension().accept(this, value);
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
        ScriptObject elements = createList(node.getElements(), value);
        if (hasBuilder(Type.ArrayExpression)) {
            return call(Type.ArrayExpression, node, elements);
        }
        ScriptObject expression = createExpression(node, Type.ArrayExpression);
        addProperty(expression, "elements", elements);
        return expression;
    }

    @Override
    public Object visit(ArrowFunction node, Void value) {
        Object id = NULL;
        ScriptObject params = createList(getParameterBindings(node.getParameters()), value);
        ScriptObject defaults = createList(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body;
        if (node.getExpression() == null) {
            body = createFunctionBody(node, value);
        } else {
            body = node.getExpression().accept(this, value);
        }
        boolean generator = false;
        boolean expression = node.getExpression() != null;
        if (hasBuilder(Type.ArrowExpression)) {
            return call(Type.ArrowExpression, node, id, params, body, generator, expression);
        }
        ScriptObject function = createFunction(node, Type.ArrowExpression);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(AssignmentElement node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(AssignmentExpression node, Void value) {
        Object left = node.getLeft().accept(this, value);
        Object right = node.getRight().accept(this, value);
        String operator = node.getOperator().getName();
        if (hasBuilder(Type.AssignmentExpression)) {
            return call(Type.AssignmentExpression, node, operator, left, right);
        }
        ScriptObject expression = createExpression(node, Type.AssignmentExpression);
        addProperty(expression, "left", left);
        addProperty(expression, "right", right);
        addProperty(expression, "operator", operator);
        return expression;
    }

    @Override
    public Object visit(AssignmentProperty node, Void value) {
        Object key = node.getPropertyName().accept(this, value);
        Object _value = node.getTarget().accept(this, value);
        Object initializer = acceptOrNull(node.getInitializer(), value);
        String kind = "init";
        boolean method = false;
        boolean shorthand = false;
        if (hasBuilder(Type.PropertyPattern)) {
            return call(Type.PropertyPattern, node, kind, key, _value);
        }
        ScriptObject property = createNode(node, Type.Property); // not PropertyPattern!
        addProperty(property, "key", key);
        addProperty(property, "value", _value);
        addProperty(property, "default", initializer);
        addProperty(property, "kind", kind);
        addProperty(property, "method", method);
        addProperty(property, "shorthand", shorthand);
        return property;
    }

    @Override
    public Object visit(AssignmentRestElement node, Void value) {
        return node.getTarget().accept(this, value);
    }

    @Override
    public Object visit(AsyncFunctionDeclaration node, Void value) {
        Object id = node.getIdentifier().accept(this, value);
        ScriptObject params = createList(getParameterBindings(node.getParameters()), value);
        ScriptObject defaults = createList(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        // TODO: flag for async
        boolean generator = false;
        boolean expression = false;
        if (hasBuilder(Type.FunctionDeclaration)) {
            return call(Type.FunctionDeclaration, node, id, params, body, generator, expression);
        }
        ScriptObject function = createFunction(node, Type.FunctionDeclaration);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(AsyncFunctionExpression node, Void value) {
        Object id = acceptOrNull(node.getIdentifier(), value);
        ScriptObject params = createList(getParameterBindings(node.getParameters()), value);
        ScriptObject defaults = createList(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        // TODO: flag for async
        boolean generator = false;
        boolean expression = false;
        if (hasBuilder(Type.FunctionExpression)) {
            return call(Type.FunctionExpression, node, id, params, body, generator, expression);
        }
        ScriptObject function = createFunction(node, Type.FunctionExpression);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(AwaitExpression node, Void value) {
        // TODO: add own expression node
        Object argument = acceptOrNull(node.getExpression(), value);
        if (hasBuilder(Type.YieldExpression)) {
            return call(Type.YieldExpression, node, argument);
        }
        ScriptObject expression = createExpression(node, Type.YieldExpression);
        addProperty(expression, "argument", argument);
        return expression;
    }

    @Override
    public Object visit(BinaryExpression node, Void value) {
        Object left = node.getLeft().accept(this, value);
        Object right = node.getRight().accept(this, value);
        String operator = node.getOperator().getName();
        if (hasBuilder(type(node))) {
            return call(type(node), node, operator, left, right);
        }
        ScriptObject expression = createExpression(node, type(node));
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
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(BindingElision node, Void value) {
        return NULL;
    }

    @Override
    public Object visit(BindingIdentifier node, Void value) {
        String name = node.getName();
        if (hasBuilder(Type.Identifier)) {
            return call(Type.Identifier, node, name);
        }
        ScriptObject binding = createBinding(node, Type.Identifier);
        addProperty(binding, "name", name);
        return binding;
    }

    @Override
    public Object visit(BindingProperty node, Void value) {
        // TODO: handle BindingProperty : SingleNameBinding
        Object key = acceptOrNull(node.getPropertyName(), value);
        Object _value = node.getBinding().accept(this, value);
        Object initializer = acceptOrNull(node.getInitializer(), value);
        String kind = "init";
        boolean method = false;
        boolean shorthand = false;
        if (hasBuilder(Type.PropertyPattern)) {
            return call(Type.PropertyPattern, node, kind, key, _value);
        }
        ScriptObject property = createNode(node, Type.Property); // not PropertyPattern!
        addProperty(property, "key", key);
        addProperty(property, "value", _value);
        addProperty(property, "default", initializer);
        addProperty(property, "kind", kind);
        addProperty(property, "method", method);
        addProperty(property, "shorthand", shorthand);
        return property;
    }

    @Override
    public Object visit(BindingRestElement node, Void value) {
        return node.getBindingIdentifier().accept(this, value);
    }

    @Override
    public Object visit(BlockStatement node, Void value) {
        ScriptObject body = createList(node.getStatements(), value);
        if (hasBuilder(Type.BlockStatement)) {
            return call(Type.BlockStatement, node, body);
        }
        ScriptObject statement = createStatement(node, Type.BlockStatement);
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
        ScriptObject statement = createStatement(node, Type.BreakStatement);
        addProperty(statement, "label", label);
        return statement;
    }

    @Override
    public Object visit(CallExpression node, Void value) {
        Object callee = node.getBase().accept(this, value);
        ScriptObject arguments = createList(node.getArguments(), value);
        if (hasBuilder(Type.CallExpression)) {
            return call(Type.CallExpression, node, callee, arguments);
        }
        ScriptObject expression = createExpression(node, Type.CallExpression);
        addProperty(expression, "callee", callee);
        addProperty(expression, "arguments", arguments);
        return expression;
    }

    @Override
    public Object visit(CallSpreadElement node, Void value) {
        Object expr = node.getExpression().accept(this, value);
        ScriptObject expression = createExpression(node, Type.SpreadExpression);
        addProperty(expression, "expression", expr);
        return expression;
    }

    @Override
    public Object visit(CatchNode node, Void value) {
        Object param = node.getCatchParameter().accept(this, value);
        Object guard = NULL;
        Object body = node.getCatchBlock().accept(this, value);
        if (hasBuilder(Type.CatchClause)) {
            return call(Type.CatchClause, node, param, guard, body);
        }
        ScriptObject catchClause = createNode(node, Type.CatchClause);
        addProperty(catchClause, "param", param);
        addProperty(catchClause, "guard", guard);
        addProperty(catchClause, "body", body);
        return catchClause;
    }

    @Override
    public Object visit(ClassDeclaration node, Void value) {
        Object id = node.getName().accept(this, value);
        Object superClass = acceptOrNull(node.getHeritage(), value);
        Object body = createClassBody(node, value);
        ScriptObject classDef = createClass(node, Type.ClassDeclaration);
        addProperty(classDef, "id", id);
        addProperty(classDef, "superClass", superClass);
        addProperty(classDef, "body", body);
        return classDef;
    }

    @Override
    public Object visit(ClassExpression node, Void value) {
        Object id = acceptOrNull(node.getName(), value);
        Object superClass = acceptOrNull(node.getHeritage(), value);
        Object body = createClassBody(node, value);
        ScriptObject classDef = createClass(node, Type.ClassExpression);
        addProperty(classDef, "id", id);
        addProperty(classDef, "superClass", superClass);
        addProperty(classDef, "body", body);
        return classDef;
    }

    @Override
    public Object visit(CommaExpression node, Void value) {
        ScriptObject expressions = createList(node.getOperands(), value);
        if (hasBuilder(Type.SequenceExpression)) {
            return call(Type.SequenceExpression, node, expressions);
        }
        ScriptObject expression = createExpression(node, Type.SequenceExpression);
        addProperty(expression, "expressions", expressions);
        return expression;
    }

    @Override
    public Object visit(Comprehension node, Void value) {
        // multiple filters possible in Comprehension, single element 'filter' useless here...
        Object body = node.getExpression().accept(this, value);
        ScriptObject blocks = createList(node.getList(), value);
        Object filter = NULL;
        ScriptObject expression = createEmptyNode();
        addProperty(expression, "body", body);
        addProperty(expression, "blocks", blocks);
        addProperty(expression, "filter", filter);
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
        ScriptObject comprehensionBlock = createNode(node, Type.ComprehensionBlock);
        addProperty(comprehensionBlock, "left", left);
        addProperty(comprehensionBlock, "right", right);
        addProperty(comprehensionBlock, "each", each);
        addProperty(comprehensionBlock, "of", of);
        return comprehensionBlock;
    }

    @Override
    public Object visit(ComprehensionIf node, Void value) {
        return node.getTest().accept(this, value);
    }

    @Override
    public Object visit(ComputedPropertyName node, Void value) {
        Object expr = node.getExpression().accept(this, value);
        ScriptObject propertyName = createNode(node, Type.ComputedPropertyName);
        addProperty(propertyName, "expression", expr);
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
        ScriptObject expression = createExpression(node, Type.ConditionalExpression);
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
        ScriptObject statement = createStatement(node, Type.ContinueStatement);
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
    public Object visit(DoWhileStatement node, Void value) {
        Object doWhileStatement;
        Object test = node.getTest().accept(this, value);
        Object body = node.getStatement().accept(this, value);
        if (hasBuilder(Type.DoWhileStatement)) {
            doWhileStatement = call(Type.DoWhileStatement, node, test, body);
        } else {
            ScriptObject statement = createStatement(node, Type.DoWhileStatement);
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
        ScriptObject expression = createExpression(node, Type.MemberExpression);
        addProperty(expression, "object", object);
        addProperty(expression, "property", property);
        addProperty(expression, "computed", computed);
        return expression;
    }

    @Override
    public Object visit(ElementAccessorValue node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(Elision node, Void value) {
        return NULL;
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
        Object expression = NULL;
        Object specifiers = NULL;
        Object source = NULL;
        switch (node.getType()) {
        case All:
            specifiers = createListFromValues(Collections
                    .singletonList(createNode(Type.ExportBatchSpecifier)));
            source = createLiteral(node.getModuleSpecifier());
            break;
        case External:
            specifiers = node.getExportsClause().accept(this, value);
            source = createLiteral(node.getModuleSpecifier());
            break;
        case Local:
            specifiers = node.getExportsClause().accept(this, value);
            break;
        case Default:
            expression = node.getExpression().accept(this, value);
            break;
        case Declaration:
            declaration = node.getDeclaration().accept(this, value);
            break;
        case Variable:
            declaration = node.getVariableStatement().accept(this, value);
            break;
        default:
            throw new IllegalStateException();
        }

        ScriptObject exportDecl = createModuleItem(node, Type.ExportDeclaration);
        addProperty(exportDecl, "declaration", declaration);
        addProperty(exportDecl, "expression", expression);
        addProperty(exportDecl, "specifiers", specifiers);
        addProperty(exportDecl, "source", source);
        return exportDecl;
    }

    @Override
    public Object visit(ExportSpecifier node, Void value) {
        Object id = createIdentifier(node.getLocalName() != null ? node.getLocalName() : node
                .getImportName());
        Object name = createIdentifier(node.getExportName());
        ScriptObject exportSpec = createNode(node, Type.ExportSpecifier);
        addProperty(exportSpec, "id", id);
        addProperty(exportSpec, "name", name);
        return exportSpec;
    }

    @Override
    public Object visit(ExportsClause node, Void value) {
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
        ScriptObject statement = createStatement(node, Type.ExpressionStatement);
        addProperty(statement, "expression", expression);
        return statement;
    }

    @Override
    public Object visit(ForEachStatement node, Void value) {
        Object forEachStatement;
        Object left = node.getHead().accept(this, value);
        Object right = node.getExpression().accept(this, value);
        Object body = node.getStatement().accept(this, value);
        boolean each = true;
        if (hasBuilder(Type.ForInStatement)) {
            forEachStatement = call(Type.ForInStatement, node, left, right, body, each);
        } else {
            ScriptObject statement = createStatement(node, Type.ForInStatement);
            addProperty(statement, "left", left);
            addProperty(statement, "right", right);
            addProperty(statement, "body", body);
            addProperty(statement, "each", each);
            forEachStatement = statement;
        }
        return createLabelledStatement(node, forEachStatement);
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
            ScriptObject statement = createStatement(node, Type.ForInStatement);
            addProperty(statement, "left", left);
            addProperty(statement, "right", right);
            addProperty(statement, "body", body);
            addProperty(statement, "each", each);
            forInStatement = statement;
        }
        return createLabelledStatement(node, forInStatement);
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
            ScriptObject statement = createStatement(node, Type.ForOfStatement);
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
            ScriptObject statement = createStatement(node, Type.ForStatement);
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
        Object id = node.getIdentifier().accept(this, value);
        ScriptObject params = createList(getParameterBindings(node.getParameters()), value);
        ScriptObject defaults = createList(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        boolean generator = false;
        boolean expression = false;
        if (hasBuilder(Type.FunctionDeclaration)) {
            return call(Type.FunctionDeclaration, node, id, params, body, generator, expression);
        }
        ScriptObject function = createFunction(node, Type.FunctionDeclaration);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(FunctionExpression node, Void value) {
        Object id = acceptOrNull(node.getIdentifier(), value);
        ScriptObject params = createList(getParameterBindings(node.getParameters()), value);
        ScriptObject defaults = createList(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        boolean generator = false;
        boolean expression = false;
        if (hasBuilder(Type.FunctionExpression)) {
            return call(Type.FunctionExpression, node, id, params, body, generator, expression);
        }
        ScriptObject function = createFunction(node, Type.FunctionExpression);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(GeneratorComprehension node, Void value) {
        // Comprehension/LegacyComprehension already created a partial result
        ScriptObject expression = (ScriptObject) node.getComprehension().accept(this, value);
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
        Object id = node.getIdentifier().accept(this, value);
        ScriptObject params = createList(getParameterBindings(node.getParameters()), value);
        ScriptObject defaults = createList(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        boolean generator = true;
        boolean expression = false;
        if (hasBuilder(Type.FunctionDeclaration)) {
            return call(Type.FunctionDeclaration, node, id, params, body, generator, expression);
        }
        ScriptObject function = createFunction(node, Type.FunctionDeclaration);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(GeneratorExpression node, Void value) {
        Object id = acceptOrNull(node.getIdentifier(), value);
        ScriptObject params = createList(getParameterBindings(node.getParameters()), value);
        ScriptObject defaults = createList(getParameterDefaults(node.getParameters()), value);
        Object rest = acceptOrNull(getRestParameter(node.getParameters()), value);
        Object body = createFunctionBody(node, value);
        boolean generator = true;
        boolean expression = false;
        if (hasBuilder(Type.FunctionExpression)) {
            return call(Type.FunctionExpression, node, id, params, body, generator, expression);
        }
        ScriptObject function = createFunction(node, Type.FunctionExpression);
        addProperty(function, "id", id);
        addProperty(function, "params", params);
        addProperty(function, "defaults", defaults);
        addProperty(function, "body", body);
        addProperty(function, "rest", rest);
        addProperty(function, "generator", generator);
        addProperty(function, "expression", expression);
        return function;
    }

    @Override
    public Object visit(GuardedCatchNode node, Void value) {
        Object param = node.getCatchParameter().accept(this, value);
        Object guard = node.getGuard().accept(this, value);
        Object body = node.getCatchBlock().accept(this, value);
        if (hasBuilder(Type.CatchClause)) {
            return call(Type.CatchClause, node, param, guard, body);
        }
        ScriptObject catchClause = createNode(node, Type.CatchClause);
        addProperty(catchClause, "param", param);
        addProperty(catchClause, "guard", guard);
        addProperty(catchClause, "body", body);
        return catchClause;
    }

    @Override
    public Object visit(Identifier node, Void value) {
        String name = node.getName();
        if (hasBuilder(Type.Identifier)) {
            return call(Type.Identifier, node, name);
        }
        ScriptObject expression = createExpression(node, Type.Identifier);
        addProperty(expression, "name", name);
        return expression;
    }

    @Override
    public Object visit(IdentifierValue node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(IfStatement node, Void value) {
        Object test = node.getTest().accept(this, value);
        Object consequent = node.getThen().accept(this, value);
        Object alternate = acceptOrNull(node.getOtherwise(), value);
        if (hasBuilder(Type.IfStatement)) {
            return call(Type.IfStatement, node, test, consequent, alternate);
        }
        ScriptObject statement = createStatement(node, Type.IfStatement);
        addProperty(statement, "test", test);
        addProperty(statement, "consequent", consequent);
        addProperty(statement, "alternate", alternate);
        return statement;
    }

    @Override
    public Object visit(ImportDeclaration node, Void value) {
        Object specifiers = NULL;
        Object source = NULL;
        switch (node.getType()) {
        case ModuleImport:
            node.getModuleImport().accept(this, value);
            break;
        case ImportFrom:
            specifiers = node.getImportClause().accept(this, value);
            source = createLiteral(node.getModuleSpecifier());
            break;
        case ImportModule:
            specifiers = createList(Collections.<Node> emptyList(), value);
            source = createLiteral(node.getModuleSpecifier());
            break;
        default:
            throw new IllegalStateException();
        }
        ScriptObject importDecl = createModuleItem(node, Type.ImportDeclaration);
        addProperty(importDecl, "specifiers", specifiers);
        addProperty(importDecl, "source", source);
        return importDecl;
    }

    @Override
    public Object visit(ImportSpecifier node, Void value) {
        Object id = createIdentifier(node.getImportName());
        Object name = node.getLocalName().accept(this, value);
        ScriptObject importSpec = createNode(node, Type.ImportSpecifier);
        addProperty(importSpec, "id", id);
        addProperty(importSpec, "name", name);
        return importSpec;
    }

    @Override
    public Object visit(ImportClause node, Void value) {
        if (node.getDefaultEntry() != null && !node.getNamedImports().isEmpty()) {
            throw new NotImplementedExpception(node);
        }
        if (node.getDefaultEntry() != null) {
            Object id = createIdentifier("default");
            Object name = node.getDefaultEntry().accept(this, value);
            ScriptObject importSpec = createNode(node, Type.ImportSpecifier);
            addProperty(importSpec, "id", id);
            addProperty(importSpec, "name", name);
            return createListFromValues(Collections.singletonList(importSpec));
        }
        return createList(node.getNamedImports(), value);
    }

    @Override
    public Object visit(SpreadArrayLiteral node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
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
    public Object visit(LabelledStatement node, Void value) {
        Object body = node.getStatement().accept(this, value);
        return createLabelledStatement(node, body);
    }

    @Override
    public Object visit(LegacyComprehension node, Void value) {
        // Extract the single if-qualifier, if present
        ComprehensionIf ifQualifier = null;
        List<ComprehensionQualifier> qualifiers = node.getList();
        ComprehensionQualifier last = lastElement(qualifiers);
        if (last instanceof ComprehensionIf) {
            ifQualifier = (ComprehensionIf) last;
            qualifiers = qualifiers.subList(0, qualifiers.size() - 1);
        }
        Object body = node.getExpression().accept(this, value);
        ScriptObject blocks = createList(qualifiers, value);
        Object filter = acceptOrNull(ifQualifier, value);
        ScriptObject expression = createEmptyNode();
        addProperty(expression, "body", body);
        addProperty(expression, "blocks", blocks);
        addProperty(expression, "filter", filter);
        return expression;
    }

    @Override
    public Object visit(LegacyComprehensionFor node, Void value) {
        Object left = node.getBinding().accept(this, value);
        Object right = node.getExpression().accept(this, value);
        boolean each = node.getIterationKind() == LegacyComprehensionFor.IterationKind.EnumerateValues;
        boolean of = node.getIterationKind() == LegacyComprehensionFor.IterationKind.Iterate;
        if (hasBuilder(Type.ComprehensionBlock)) {
            return call(Type.ComprehensionBlock, node, left, right, each);
        }
        ScriptObject comprehensionBlock = createNode(node, Type.ComprehensionBlock);
        addProperty(comprehensionBlock, "left", left);
        addProperty(comprehensionBlock, "right", right);
        addProperty(comprehensionBlock, "each", each);
        addProperty(comprehensionBlock, "of", of);
        return comprehensionBlock;
    }

    @Override
    public Object visit(LegacyGeneratorDeclaration node, Void value) {
        return visit((GeneratorDeclaration) node, value);
    }

    @Override
    public Object visit(LegacyGeneratorExpression node, Void value) {
        return visit((GeneratorExpression) node, value);
    }

    @Override
    public Object visit(LetExpression node, Void value) {
        ScriptObject head = createList(node.getBindings(), value);
        Object body = node.getExpression().accept(this, value);
        if (hasBuilder(Type.LetExpression)) {
            return call(Type.LetExpression, node, head, body);
        }
        ScriptObject expression = createExpression(node, Type.LetExpression);
        addProperty(expression, "head", head);
        addProperty(expression, "body", body);
        return expression;
    }

    @Override
    public Object visit(LetStatement node, Void value) {
        ScriptObject head = createList(node.getBindings(), value);
        Object body = node.getStatement().accept(this, value);
        if (hasBuilder(Type.LetStatement)) {
            return call(Type.LetStatement, node, head, body);
        }
        ScriptObject statement = createStatement(node, Type.LetStatement);
        addProperty(statement, "head", head);
        addProperty(statement, "body", body);
        return statement;
    }

    @Override
    public Object visit(LexicalBinding node, Void value) {
        Object id = node.getBinding().accept(this, value);
        Object init = acceptOrNull(node.getInitializer(), value);
        if (hasBuilder(Type.VariableDeclarator)) {
            return call(Type.VariableDeclarator, node, id, init);
        }
        ScriptObject declarator = createNode(node, Type.VariableDeclarator);
        addProperty(declarator, "id", id);
        addProperty(declarator, "init", init);
        return declarator;
    }

    @Override
    public Object visit(LexicalDeclaration node, Void value) {
        ScriptObject declarations = createList(node.getElements(), value);
        String kind = node.getType() == LexicalDeclaration.Type.Const ? "const" : "let";
        if (hasBuilder(Type.VariableDeclaration)) {
            return call(Type.VariableDeclaration, node, kind, declarations);
        }
        ScriptObject declaration = createDeclaration(node, Type.VariableDeclaration);
        addProperty(declaration, "declarations", declarations);
        addProperty(declaration, "kind", kind);
        return declaration;
    }

    @Override
    public Object visit(MethodDefinition node, Void value) {
        Object key = node.getPropertyName().accept(this, value);
        Object _value = toFunctionExpression(node, value);
        String kind = methodKind(node, "init");
        boolean method = true;
        boolean shorthand = false;
        if (hasBuilder(Type.Property)) {
            return call(Type.Property, node, kind, key, _value);
        }
        ScriptObject property = createNode(node, Type.Property);
        addProperty(property, "key", key);
        addProperty(property, "value", _value);
        addProperty(property, "kind", kind);
        addProperty(property, "method", method);
        addProperty(property, "shorthand", shorthand);
        return property;
    }

    @Override
    public Object visit(Module node, Void value) {
        ScriptObject body = createList(node.getStatements(), value);
        if (hasBuilder(Type.Program)) {
            return call(Type.Program, node, body);
        }
        ScriptObject program = createNode(node, Type.Program);
        addProperty(program, "body", body);
        return program;
    }

    @Override
    public Object visit(ModuleImport node, Void value) {
        throw new NotImplementedExpception(node);
    }

    @Override
    public Object visit(NewExpression node, Void value) {
        Object callee = node.getExpression().accept(this, value);
        ScriptObject arguments = createList(node.getArguments(), value);
        if (hasBuilder(Type.NewExpression)) {
            return call(Type.NewExpression, node, callee, arguments);
        }
        ScriptObject expression = createExpression(node, Type.NewExpression);
        addProperty(expression, "callee", callee);
        addProperty(expression, "arguments", arguments);
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
        ScriptObject properties = createList(node.getProperties(), value);
        if (hasBuilder(Type.ObjectPattern)) {
            return call(Type.ObjectPattern, node, properties);
        }
        ScriptObject pattern = createPattern(node, Type.ObjectPattern);
        addProperty(pattern, "properties", properties);
        return pattern;
    }

    @Override
    public Object visit(ObjectBindingPattern node, Void value) {
        ScriptObject properties = createList(node.getProperties(), value);
        if (hasBuilder(Type.ObjectPattern)) {
            return call(Type.ObjectPattern, node, properties);
        }
        ScriptObject pattern = createPattern(node, Type.ObjectPattern);
        addProperty(pattern, "properties", properties);
        return pattern;
    }

    @Override
    public Object visit(ObjectLiteral node, Void value) {
        ScriptObject properties = createList(node.getProperties(), value);
        if (hasBuilder(Type.ObjectExpression)) {
            return call(Type.ObjectExpression, node, properties);
        }
        ScriptObject expression = createExpression(node, Type.ObjectExpression);
        addProperty(expression, "properties", properties);
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
        ScriptObject expression = createExpression(node, Type.MemberExpression);
        addProperty(expression, "object", object);
        addProperty(expression, "property", property);
        addProperty(expression, "computed", computed);
        return expression;
    }

    @Override
    public Object visit(PropertyAccessorValue node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(PropertyNameDefinition node, Void value) {
        Object key = node.getPropertyName().accept(this, value);
        Object _value = key;
        String kind = "init";
        boolean method = false;
        boolean shorthand = true;
        if (hasBuilder(Type.Property)) {
            return call(Type.Property, node, kind, key, _value);
        }
        ScriptObject property = createNode(node, Type.Property);
        addProperty(property, "key", key);
        addProperty(property, "value", _value);
        addProperty(property, "kind", kind);
        addProperty(property, "method", method);
        addProperty(property, "shorthand", shorthand);
        return property;
    }

    @Override
    public Object visit(PropertyValueDefinition node, Void value) {
        Object key = node.getPropertyName().accept(this, value);
        Object _value = node.getPropertyValue().accept(this, value);
        String kind = "init";
        boolean method = false;
        boolean shorthand = false;
        if (hasBuilder(Type.Property)) {
            return call(Type.Property, node, kind, key, _value);
        }
        ScriptObject property = createNode(node, Type.Property);
        addProperty(property, "key", key);
        addProperty(property, "value", _value);
        addProperty(property, "kind", kind);
        addProperty(property, "method", method);
        addProperty(property, "shorthand", shorthand);
        return property;
    }

    @Override
    public Object visit(RegularExpressionLiteral node, Void value) {
        ScriptObject _value = RegExpCreate(cx, node.getRegexp(), node.getFlags());
        if (hasBuilder(Type.Literal)) {
            return call(Type.Literal, node, _value);
        }
        ScriptObject expression = createExpression(node, Type.Literal);
        addProperty(expression, "value", _value);
        return expression;
    }

    @Override
    public Object visit(ReturnStatement node, Void value) {
        Object argument = acceptOrNull(node.getExpression(), value);
        if (hasBuilder(Type.ReturnStatement)) {
            return call(Type.ReturnStatement, node, argument);
        }
        ScriptObject statement = createStatement(node, Type.ReturnStatement);
        addProperty(statement, "argument", argument);
        return statement;
    }

    @Override
    public Object visit(Script node, Void value) {
        ScriptObject body = createList(node.getStatements(), value);
        if (hasBuilder(Type.Program)) {
            return call(Type.Program, node, body);
        }
        ScriptObject program = createNode(node, Type.Program);
        addProperty(program, "body", body);
        return program;
    }

    @Override
    public Object visit(SpreadElement node, Void value) {
        Object expr = node.getExpression().accept(this, value);
        ScriptObject expression = createExpression(node, Type.SpreadExpression);
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
    public Object visit(SuperExpression node, Void value) {
        Object property = NULL;
        boolean computed = false;
        Object arguments = NULL;
        String kind;
        switch (node.getType()) {
        case PropertyAccessor: {
            property = createIdentifier(node.getName());
            computed = false;
            kind = "property";
            break;
        }
        case ElementAccessor: {
            property = node.getExpression().accept(this, value);
            computed = true;
            kind = "property";
            break;
        }
        case CallExpression: {
            arguments = createList(node.getArguments(), value);
            kind = "call";
            break;
        }
        case NewExpression: {
            kind = "new";
            break;
        }
        default:
            throw new IllegalStateException();
        }
        ScriptObject expression = createExpression(node, Type.SuperExpression);
        addProperty(expression, "property", property);
        addProperty(expression, "computed", computed);
        addProperty(expression, "arguments", arguments);
        addProperty(expression, "kind", kind);
        return expression;
    }

    @Override
    public Object visit(SuperExpressionValue node, Void value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public Object visit(SwitchClause node, Void value) {
        Object test = acceptOrNull(node.getExpression(), value);
        ScriptObject consequent = createList(node.getStatements(), value);
        if (hasBuilder(Type.SwitchCase)) {
            return call(Type.SwitchCase, node, test, consequent);
        }
        ScriptObject switchCase = createNode(node, Type.SwitchCase);
        addProperty(switchCase, "test", test);
        addProperty(switchCase, "consequent", consequent);
        return switchCase;
    }

    @Override
    public Object visit(SwitchStatement node, Void value) {
        Object switchStatement;
        Object discriminant = node.getExpression().accept(this, value);
        ScriptObject cases = createList(node.getClauses(), value);
        boolean lexical = !LexicalDeclarations(node).isEmpty();
        if (hasBuilder(Type.SwitchStatement)) {
            switchStatement = call(Type.SwitchStatement, node, discriminant, cases, lexical);
        } else {
            ScriptObject statement = createStatement(node, Type.SwitchStatement);
            addProperty(statement, "discriminant", discriminant);
            addProperty(statement, "cases", cases);
            addProperty(statement, "lexical", lexical);
            switchStatement = statement;
        }
        return createLabelledStatement(node, switchStatement);
    }

    @Override
    public Object visit(TemplateCallExpression node, Void value) {
        // Typename and properties based on esprima
        Object tag = node.getBase().accept(this, value);
        Object quasi = node.getTemplate().accept(this, value);
        ScriptObject expression = createExpression(node, Type.TaggedTemplateExpression);
        addProperty(expression, "tag", tag);
        addProperty(expression, "quasi", quasi);
        return expression;
    }

    @Override
    public Object visit(TemplateCharacters node, Void value) {
        // Typename and properties based on esprima
        String raw = node.getRawValue();
        String cooked = node.getValue();
        ScriptObject expression = createExpression(node, Type.TemplateLiteral);
        addProperty(expression, "raw", raw);
        addProperty(expression, "cooked", cooked);
        return expression;
    }

    @Override
    public Object visit(TemplateLiteral node, Void value) {
        // Typename and properties based on esprima
        ScriptObject quasis = createList(TemplateStrings(node), value);
        ScriptObject expressions = createList(Substitutions(node), value);
        ScriptObject expression = createExpression(node, Type.TemplateLiteral);
        addProperty(expression, "quasis", quasis);
        addProperty(expression, "expressions", expressions);
        return expression;
    }

    @Override
    public Object visit(ThisExpression node, Void value) {
        if (hasBuilder(Type.ThisExpression)) {
            return call(Type.ThisExpression, node);
        }
        ScriptObject expression = createExpression(node, Type.ThisExpression);
        return expression;
    }

    @Override
    public Object visit(ThrowStatement node, Void value) {
        Object argument = node.getExpression().accept(this, value);
        if (hasBuilder(Type.ThrowStatement)) {
            return call(Type.ThrowStatement, node, argument);
        }
        ScriptObject statement = createStatement(node, Type.ThrowStatement);
        addProperty(statement, "argument", argument);
        return statement;
    }

    @Override
    public Object visit(TryStatement node, Void value) {
        Object block = node.getTryBlock().accept(this, value);
        Object handler = acceptOrNull(node.getCatchNode(), value);
        ScriptObject guardedHandlers = createList(node.getGuardedCatchNodes(), value);
        Object finalizer = acceptOrNull(node.getFinallyBlock(), value);
        if (hasBuilder(Type.TryStatement)) {
            return call(Type.TryStatement, node, block, guardedHandlers, handler, finalizer);
        }
        ScriptObject statement = createStatement(node, Type.TryStatement);
        addProperty(statement, "block", block);
        addProperty(statement, "handler", handler);
        addProperty(statement, "guardedHandlers", guardedHandlers);
        addProperty(statement, "finalizer", finalizer);
        return statement;
    }

    @Override
    public Object visit(UnaryExpression node, Void value) {
        Object argument = node.getOperand().accept(this, value);
        String operator = node.getOperator().getName();
        boolean prefix = !node.getOperator().isPostfix();
        if (hasBuilder(type(node))) {
            return call(type(node), node, operator, argument, prefix);
        }
        ScriptObject expression = createExpression(node, type(node));
        addProperty(expression, "argument", argument);
        addProperty(expression, "operator", operator);
        addProperty(expression, "prefix", prefix);
        return expression;
    }

    private static Type type(UnaryExpression node) {
        switch (node.getOperator()) {
        case POST_DEC:
        case POST_INC:
        case PRE_DEC:
        case PRE_INC:
            return Type.UpdateExpression;
        default:
            return Type.UnaryExpression;
        }
    }

    @Override
    public Object visit(VariableDeclaration node, Void value) {
        Object id = node.getBinding().accept(this, value);
        Object init = acceptOrNull(node.getInitializer(), value);
        if (hasBuilder(Type.VariableDeclarator)) {
            return call(Type.VariableDeclarator, node, id, init);
        }
        ScriptObject declarator = createNode(node, Type.VariableDeclarator);
        addProperty(declarator, "id", id);
        addProperty(declarator, "init", init);
        return declarator;
    }

    @Override
    public Object visit(VariableStatement node, Void value) {
        ScriptObject declarations = createList(node.getElements(), value);
        String kind = "var";
        if (hasBuilder(Type.VariableDeclaration)) {
            return call(Type.VariableDeclaration, node, kind, declarations);
        }
        ScriptObject declaration = createDeclaration(node, Type.VariableDeclaration);
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
            ScriptObject statement = createStatement(node, Type.WhileStatement);
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
        ScriptObject statement = createStatement(node, Type.WithStatement);
        addProperty(statement, "object", object);
        addProperty(statement, "body", body);
        return statement;
    }

    @Override
    public Object visit(YieldExpression node, Void value) {
        Object argument = acceptOrNull(node.getExpression(), value);
        if (hasBuilder(Type.YieldExpression)) {
            return call(Type.YieldExpression, node, argument);
        }
        ScriptObject expression = createExpression(node, Type.YieldExpression);
        addProperty(expression, "argument", argument);
        return expression;
    }
}
