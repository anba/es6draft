/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;
import static com.github.anba.es6draft.semantics.StaticSemantics.TailCallNodes;
import static com.github.anba.es6draft.semantics.StaticSemantics.TemplateStrings;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.CodeSizeEvaluator;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.FunctionNode.StrictMode;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodAllocation;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.Parser.Option;
import com.github.anba.es6draft.runtime.internal.ImmediateFuture;
import com.github.anba.es6draft.runtime.internal.JVMNames;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * 12.1.5.7 Runtime Semantics: PropertyDefinitionEvaluation
 */
class CodeGenerator implements AutoCloseable {
    private static class Methods {
        // class: Reference
        static final MethodDesc Reference_GetValue = MethodDesc.create(MethodType.Virtual,
                Types.Reference, "GetValue",
                Type.getMethodType(Types.Object, Types.ExecutionContext));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_GetTemplateCallSite = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "GetTemplateCallSite", Type.getMethodType(
                        Types.ScriptObject, Types.String, Types.MethodHandle,
                        Types.ExecutionContext));
    }

    private static final boolean EVALUATE_SIZE = false;
    private static final boolean INCLUDE_SOURCE = true;
    private static final Future<String> NO_SOURCE = new ImmediateFuture<>(null);
    private static final int MAX_FNAME_LENGTH = 0x4000;

    private final ClassWriter cw;
    private final String className;
    private final EnumSet<Option> options;
    private ExecutorService sourceCompressor;

    private StatementGenerator stmtgen = new StatementGenerator(this);
    private ExpressionGenerator exprgen = new ExpressionGenerator(this);
    private PropertyGenerator propgen = new PropertyGenerator(this);

    CodeGenerator(ClassWriter cw, String className, EnumSet<Parser.Option> options) {
        this.cw = cw;
        this.className = className;
        this.options = options;
        if (INCLUDE_SOURCE) {
            this.sourceCompressor = Executors.newFixedThreadPool(1);
        }
    }

    String getClassName() {
        return className;
    }

    boolean isEnabled(Parser.Option option) {
        return options.contains(option);
    }

    @Override
    public void close() {
        if (INCLUDE_SOURCE) {
            sourceCompressor.shutdown();
        }
        sourceCompressor = null;
    }

    private Future<String> compressed(String source) {
        if (INCLUDE_SOURCE) {
            return sourceCompressor.submit(SourceCompressor.compress(source));
        } else {
            return NO_SOURCE;
        }
    }

    private static class $CodeSizeEvaluator extends CodeSizeEvaluator {
        private final String methodName;

        $CodeSizeEvaluator(String methodName, MethodVisitor mv) {
            super(Opcodes.ASM4, mv);
            this.methodName = methodName;
        }

        @Override
        public void visitEnd() {
            System.out.printf("%s: [%d, %d]\n", methodName, getMinSize(), getMaxSize());
            super.visitEnd();
        }
    }

    MethodVisitor publicStaticMethod(String methodName, String methodDescriptor) {
        int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
        String signature = null;
        String[] exceptions = null;
        MethodVisitor mv = cw.visitMethod(access, methodName, methodDescriptor, signature,
                exceptions);
        if (EVALUATE_SIZE) {
            mv = new $CodeSizeEvaluator(methodName, mv);
        }
        return mv;
    }

    InstructionVisitor publicStaticMethod(String methodName, Type methodDescriptor) {
        MethodVisitor mv = publicStaticMethod(methodName, methodDescriptor.getInternalName());
        return new InstructionVisitor(mv, methodName, methodDescriptor, MethodAllocation.Class);
    }

    // template strings
    private Map<TemplateLiteral, String> templateKeys = new HashMap<>();

    private String templateKey(TemplateLiteral template) {
        String key = templateKeys.get(template);
        if (key == null) {
            templateKeys.put(template, key = UUID.randomUUID().toString());
        }
        return key;
    }

    // method names
    private Map<Node, String> methodNames = new HashMap<>(32);
    private AtomicInteger methodCounter = new AtomicInteger(0);

    private final boolean isCompiled(Node node) {
        return methodNames.containsKey(node);
    }

    enum ScriptName {
        Code, Init, EvalInit, RTI
    }

    final String methodName(Script node, ScriptName name) {
        switch (name) {
        case Code:
            return "!~script";
        case Init:
            return "script_init";
        case EvalInit:
            return "script_evalinit";
        case RTI:
            return "script_rti";
        default:
            throw new IllegalStateException();
        }
    }

    private final String getNameOrThrow(Node node) {
        String name = methodNames.get(node);
        if (name == null) {
            throw new IllegalStateException("no method-name present for: " + node);
        }
        return name;
    }

    final String methodName(StatementListMethod node) {
        return getNameOrThrow(node);
    }

    private final String methodName(TopLevelNode topLevel, StatementListMethod node) {
        return addMethodName(node, getCodeName(topLevel), '\'');
    }

    private final String methodName(TemplateLiteral node) {
        String n = methodNames.get(node);
        if (n == null) {
            n = addMethodName(node, "template");
        }
        return n;
    }

    final String methodName(GeneratorComprehension node) {
        String n = methodNames.get(node);
        if (n == null) {
            n = addMethodName(node, "gencompr");
        }
        return n;
    }

    final String methodName(SpreadElementMethod node) {
        String n = methodNames.get(node);
        if (n == null) {
            n = addMethodName(node, "spread");
        }
        return n;
    }

    final String methodName(PropertyDefinitionsMethod node) {
        String n = methodNames.get(node);
        if (n == null) {
            n = addMethodName(node, "propdef");
        }
        return n;
    }

    final String methodName(ExpressionMethod node) {
        String n = methodNames.get(node);
        if (n == null) {
            n = addMethodName(node, "expr");
        }
        return n;
    }

    enum FunctionName {
        Code, Init, RTI
    }

    final String methodName(FunctionNode node, FunctionName name) {
        String fname = methodName(node);
        switch (name) {
        case Code:
            return insertMarker("!", fname, "");
        case Init:
            return insertMarker("!", fname, "_init");
        case RTI:
            return insertMarker("", fname, "_rti");
        default:
            throw new IllegalStateException();
        }
    }

    private final String insertMarker(String prefix, String fname, String suffix) {
        StringBuilder sb = new StringBuilder(2 + fname.length() + prefix.length() + suffix.length());
        if (fname.charAt(0) != '\\') {
            // simple concat if string not mangled
            return sb.append(prefix).append(fname).append(suffix).toString();
        }
        if (fname.charAt(1) != '=') {
            // add \= indicator before adding prefix
            return sb.append("\\=").append(prefix).append(fname).append(suffix).toString();
        }
        // add \= indicator already present, add prefix after it
        return sb.append(fname, 0, 2).append(prefix).append(fname, 2, fname.length())
                .append(suffix).toString();
    }

    private final String methodName(FunctionNode node) {
        String n = methodNames.get(node);
        if (n == null) {
            String fname = node.getFunctionName();
            if (fname == null) {
                fname = "<...>";
            } else if (fname.isEmpty()) {
                fname = "anonymous";
            } else if (fname.length() > MAX_FNAME_LENGTH) {
                fname = fname.substring(0, MAX_FNAME_LENGTH);
            }
            n = addMethodName(node, fname);
        }
        return n;
    }

    private final String addMethodName(Node node, String name) {
        return addMethodName(node, name, '~');
    }

    private final String addMethodName(Node node, String name, char sep) {
        assert !methodNames.containsKey(node);
        String n = JVMNames.toBytecodeName(name + sep + methodCounter.incrementAndGet());
        methodNames.put(node, n);
        return n;
    }

    private final String getCodeName(TopLevelNode node) {
        if (node instanceof FunctionNode) {
            return methodName((FunctionNode) node, FunctionName.Code);
        }
        assert node instanceof Script;
        return methodName((Script) node, ScriptName.Code);
    }

    /**
     * [12.1.9] Runtime Semantics: GetTemplateCallSite Abstract Operation
     */
    void GetTemplateCallSite(TemplateLiteral node, ExpressionVisitor mv) {
        assert isCompiled(node);
        String methodName = methodName(node);
        String desc = Type.getMethodDescriptor(Types.String_);

        // GetTemplateCallSite
        mv.aconst(templateKey(node));
        mv.invokeStaticMH(className, methodName, desc);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_GetTemplateCallSite);
    }

    void compile(TemplateLiteral node) {
        if (!isCompiled(node)) {
            String name = methodName(node);
            Type desc = Type.getMethodType(Types.String_);
            InstructionVisitor body = publicStaticMethod(name, desc);
            body.lineInfo(node.getLine());
            body.begin();

            List<TemplateCharacters> strings = TemplateStrings(node);
            body.newarray(strings.size() * 2, Types.String);
            for (int i = 0, size = strings.size(); i < size; ++i) {
                TemplateCharacters e = strings.get(i);
                int index = i << 1;
                body.astore(index, e.getValue());
                body.astore(index + 1, e.getRawValue());
            }

            body.areturn();
            body.end();
        }
    }

    void compile(Script node) {
        // initialisation methods
        new GlobalDeclarationInstantiationGenerator(this).generate(node);
        new EvalDeclarationInstantiationGenerator(this).generate(node);

        // runtime method
        StatementVisitor mv = new ScriptStatementVisitor(this, node);
        mv.lineInfo(node);
        mv.begin();
        mv.loadUndefined();
        mv.storeCompletionValue();

        mv.enterScope(node);
        Completion result = Completion.Normal;
        for (StatementListItem stmt : node.getStatements()) {
            if ((result = result.then(statement(stmt, mv))).isAbrupt()) {
                break;
            }
        }
        mv.exitScope();

        if (!result.isAbrupt()) {
            mv.loadCompletionValue();
            mv.areturn();
        }
        mv.end();

        // runtime-info method
        new RuntimeInfoGenerator(this).runtimeInfo(node);
    }

    void compile(GeneratorComprehension node, ExpressionVisitor mv) {
        if (!isCompiled(node)) {
            ExpressionVisitor body = new GeneratorComprehensionVisitor(this, node, mv);
            body.lineInfo(node);
            body.begin();

            body.setScope(mv.getScope());
            node.accept(new GeneratorComprehensionGenerator(this), body);

            body.loadUndefined();
            body.areturn();
            body.end();
        }
    }

    void compile(FunctionNode node) {
        if (!isCompiled(node)) {
            StringBuilder sb = new StringBuilder();
            sb.append(node.getHeaderSource());
            sb.append('{');
            if (node.getStrictMode() == StrictMode.ImplicitStrict) {
                sb.append("\n\"use strict\";\n");
            }
            sb.append(node.getBodySource());
            sb.append('}');
            Future<String> source = compressed(sb.toString());

            // initialisation method
            new FunctionDeclarationInstantiationGenerator(this).generate(node);

            // runtime method
            if (node instanceof ArrowFunction && ((ArrowFunction) node).getExpression() != null) {
                conciseFunctionBody((ArrowFunction) node);
            } else {
                functionBody(node);
            }

            // runtime-info method
            new RuntimeInfoGenerator(this).runtimeInfo(node, source);
        }
    }

    private void conciseFunctionBody(ArrowFunction node) {
        ExpressionVisitor body = new ArrowFunctionVisitor(this, node);
        body.lineInfo(node);
        body.begin();

        // call expression in concise function body is always in tail-call position
        body.setTailCall(TailCallNodes(node.getExpression()));

        body.enterScope(node);
        ValType type = expressionValue(node.getExpression(), body);
        body.toBoxed(type);
        body.exitScope();

        body.areturn();
        body.end();
    }

    private void functionBody(FunctionNode node) {
        StatementVisitor body = new FunctionStatementVisitor(this, node);
        body.lineInfo(node);
        body.begin();

        body.enterScope(node);
        Completion result = Completion.Normal;
        for (StatementListItem stmt : node.getStatements()) {
            if ((result = result.then(statement(stmt, body))).isAbrupt()) {
                break;
            }
        }
        body.exitScope();

        if (!result.isAbrupt()) {
            // fall-thru, clear any previously stored entry in completion-value
            body.loadUndefined();
            body.areturn();
        }

        if (body.hasReturn()) {
            body.mark(body.returnLabelImmediate());
            body.loadCompletionValue();
            body.areturn();
        }
        body.end();
    }

    void compile(StatementListMethod node, StatementVisitor mv) {
        if (!isCompiled(node)) {
            StatementVisitor body = new StatementListMethodStatementVisitor(this, node, mv);
            body.lineInfo(node);
            body.nop(); // force line-number entry
            body.begin();

            body.setScope(mv.getScope());
            Completion result = Completion.Normal;
            for (StatementListItem stmt : node.getStatements()) {
                if ((result = result.then(statement(stmt, body))).isAbrupt()) {
                    break;
                }
            }

            if (body.getCodeType() == StatementVisitor.CodeType.Function) {
                // function case
                if (!result.isAbrupt()) {
                    body.aconst(null);
                    body.areturn();
                }

                // emit return-label if nested in function
                if (body.hasReturn()) {
                    body.mark(body.returnLabelImmediate());
                    body.loadCompletionValue();
                    body.areturn();
                }
            } else {
                // script case
                if (!result.isAbrupt()) {
                    body.loadCompletionValue();
                    body.areturn();
                }
            }

            body.end();
        }
    }

    void compile(SpreadElementMethod node, ExpressionVisitor mv) {
        if (!isCompiled(node)) {
            ExpressionVisitor body = new SpreadElementMethodVisitor(this, node, mv);
            body.lineInfo(node);
            body.begin();

            body.setScope(mv.getScope());
            expression(node.getExpression(), body);

            body.areturn();
            body.end();
        }
    }

    void compile(PropertyDefinitionsMethod node, ExpressionVisitor mv) {
        if (!isCompiled(node)) {
            ExpressionVisitor body = new PropertyDefinitionsMethodVisitor(this, node, mv);
            body.lineInfo(node);
            body.begin();

            body.setScope(mv.getScope());
            Variable<ScriptObject> object = body.getParameter(1, ScriptObject.class);
            for (PropertyDefinition property : node.getProperties()) {
                body.load(object);
                propertyDefinition(property, body);
            }

            body.areturn();
            body.end();
        }
    }

    void compile(ExpressionMethod node, ExpressionVisitor mv) {
        if (!isCompiled(node)) {
            ExpressionVisitor body = new ExpressionMethodVisitor(this, node, mv);
            body.lineInfo(node);
            body.begin();

            body.setScope(mv.getScope());
            ValType type = expressionValue(node.getExpression(), body);
            body.toBoxed(type);

            body.areturn();
            body.end();
        }
    }

    /* ----------------------------------------------------------------------------------------- */

    ValType expression(Expression node, ExpressionVisitor mv) {
        return node.accept(exprgen, mv);
    }

    ValType expressionValue(Expression node, ExpressionVisitor mv) {
        Expression nodeValue = node.asValue();
        ValType type = nodeValue.accept(exprgen, mv);
        if (type == ValType.Reference) {
            mv.loadExecutionContext();
            mv.invoke(Methods.Reference_GetValue);
        }
        return (type != ValType.Reference ? type : ValType.Any);
    }

    void expressionBoxedValue(Expression node, ExpressionVisitor mv) {
        mv.toBoxed(expressionValue(node, mv));
    }

    void propertyDefinition(PropertyDefinition node, ExpressionVisitor mv) {
        node.accept(propgen, mv);
    }

    Completion statement(StatementListItem node, StatementVisitor mv) {
        return node.accept(stmtgen, mv);
    }

    /* ----------------------------------------------------------------------------------------- */

    private static class ScriptStatementVisitor extends StatementVisitor {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        ScriptStatementVisitor(CodeGenerator codegen, Script node) {
            super(codegen, codegen.methodName(node, ScriptName.Code), methodDescriptor,
                    IsStrict(node), node, node.isGlobalCode() ? CodeType.GlobalScript
                            : CodeType.NonGlobalScript);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static class FunctionStatementVisitor extends StatementVisitor {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        FunctionStatementVisitor(CodeGenerator codegen, FunctionNode node) {
            super(codegen, codegen.methodName(node, FunctionName.Code), methodDescriptor,
                    IsStrict(node), node, CodeType.Function);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static class StatementListMethodStatementVisitor extends StatementVisitor {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext, Types.Object);

        StatementListMethodStatementVisitor(CodeGenerator codegen, StatementListMethod node,
                StatementVisitor parent) {
            super(codegen, codegen.methodName(parent.getTopLevelNode(), node), methodDescriptor,
                    parent.isStrict(), parent.getTopLevelNode(), parent.getCodeType());
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("completion", 1, Types.Object);
        }
    }

    private static class ArrowFunctionVisitor extends ExpressionVisitor {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        ArrowFunctionVisitor(CodeGenerator codegen, ArrowFunction node) {
            super(codegen, codegen.methodName(node, FunctionName.Code), methodDescriptor,
                    IsStrict(node), false);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static class GeneratorComprehensionVisitor extends ExpressionVisitor {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        GeneratorComprehensionVisitor(CodeGenerator codegen, GeneratorComprehension node,
                ExpressionVisitor parent) {
            super(codegen, codegen.methodName(node), methodDescriptor, parent.isStrict(), parent
                    .isGlobalCode());
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static class ExpressionMethodVisitor extends ExpressionVisitor {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        ExpressionMethodVisitor(CodeGenerator codegen, ExpressionMethod node,
                ExpressionVisitor parent) {
            super(codegen, codegen.methodName(node), methodDescriptor, parent.isStrict(), parent
                    .isGlobalCode());
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static class SpreadElementMethodVisitor extends ExpressionVisitor {
        static final Type methodDescriptor = Type.getMethodType(Type.INT_TYPE,
                Types.ExecutionContext, Types.ExoticArray, Type.INT_TYPE);

        SpreadElementMethodVisitor(CodeGenerator codegen, SpreadElementMethod node,
                ExpressionVisitor parent) {
            super(codegen, codegen.methodName(node), methodDescriptor, parent.isStrict(), parent
                    .isGlobalCode());
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("array", 1, Types.ExoticArray);
            setParameterName("index", 2, Type.INT_TYPE);
        }
    }

    private static class PropertyDefinitionsMethodVisitor extends ExpressionVisitor {
        static final Type methodDescriptor = Type.getMethodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.ScriptObject);

        PropertyDefinitionsMethodVisitor(CodeGenerator codegen, PropertyDefinitionsMethod node,
                ExpressionVisitor parent) {
            super(codegen, codegen.methodName(node), methodDescriptor, parent.isStrict(), parent
                    .isGlobalCode());
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("object", 1, Types.ScriptObject);
        }
    }
}
