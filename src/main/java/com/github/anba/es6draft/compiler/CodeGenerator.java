/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.tailCall;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;
import static com.github.anba.es6draft.semantics.StaticSemantics.TemplateStrings;

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

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.FunctionNode.StrictMode;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.runtime.internal.ImmediateFuture;
import com.github.anba.es6draft.runtime.internal.JVMNames;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;

/**
 * 
 */
class CodeGenerator {
    private static class Fields {
        static final FieldDesc Undefined_UNDEFINED = FieldDesc.create(FieldType.Static,
                Types.Undefined, "UNDEFINED", Types.Undefined);
    }

    private static class Methods {
        // class: Reference
        static final MethodDesc Reference_GetValue = MethodDesc.create(MethodType.Static,
                Types.Reference, "GetValue",
                Type.getMethodType(Types.Object, Types.Object, Types.ExecutionContext));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_GetTemplateCallSite = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "GetTemplateCallSite", Type.getMethodType(
                        Types.ScriptObject, Types.String, Types.MethodHandle,
                        Types.ExecutionContext));
    }

    private static final boolean INCLUDE_SOURCE = true;
    private static final Future<String> NO_SOURCE = new ImmediateFuture<>(null);

    private final ClassWriter cw;
    private final String className;
    private ExecutorService sourceCompressor;

    private StatementGenerator stmtgen = new StatementGenerator(this);
    private ExpressionGenerator exprgen = new ExpressionGenerator(this);
    private PropertyGenerator propgen = new PropertyGenerator(this);

    CodeGenerator(ClassWriter cw, String className) {
        this.cw = cw;
        this.className = className;
        if (INCLUDE_SOURCE) {
            this.sourceCompressor = Executors.newFixedThreadPool(1);
        }
    }

    String getClassName() {
        return className;
    }

    void close() {
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

    MethodVisitor publicStaticMethod(String methodName, String methodDescriptor) {
        int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
        String signature = null;
        String[] exceptions = null;
        return cw.visitMethod(access, methodName, methodDescriptor, signature, exceptions);
    }

    InstructionVisitor publicStaticMethod(String methodName, Type methodDescriptor) {
        MethodVisitor mv = publicStaticMethod(methodName, methodDescriptor.getInternalName());
        return new InstructionVisitor(mv, methodName, methodDescriptor);
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

    private final boolean isCompiled(TemplateLiteral node) {
        return methodNames.containsKey(node);
    }

    private final boolean isCompiled(GeneratorComprehension node) {
        return methodNames.containsKey(node);
    }

    private final boolean isCompiled(FunctionNode node) {
        return methodNames.containsKey(node);
    }

    final String methodName(Script node) {
        return "!script";
    }

    final String methodName(Script node, int index) {
        return "!script_" + index;
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

    final String methodName(FunctionNode node) {
        String n = methodNames.get(node);
        if (n == null) {
            String fname = node.getFunctionName();
            if (fname.isEmpty()) {
                fname = "anonymous";
            }
            n = addMethodName(node, '!' + fname);
        }
        return n;
    }

    private final String addMethodName(Node node, String name) {
        String n = JVMNames.toBytecodeName(name + "~" + methodCounter.incrementAndGet());
        methodNames.put(node, n);
        return n;
    }

    /**
     * [11.1.9] Runtime Semantics: GetTemplateCallSite Abstract Operation
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
                body.astore(index, e.getValue(), Types.String);
                body.astore(index + 1, e.getRawValue(), Types.String);
            }

            body.areturn();
            body.end();
        }
    }

    void compile(Script node) {
        // initialisation method
        new GlobalDeclarationInstantiationGenerator(this).generate(node);

        // TODO: only generate eval-script-init when requested
        new EvalDeclarationInstantiationGenerator(this).generate(node);

        // runtime method
        if (node.getStatements().size() < STATEMENTS_THRESHOLD) {
            singleScript(node);
        } else {
            multiScript(node);
        }

        // runtime-info method
        new RuntimeInfoGenerator(this).runtimeInfo(node);
    }

    private static final int STATEMENTS_THRESHOLD = 300;

    private void singleScript(Script node) {
        StatementVisitor mv = new ScriptStatementVisitor(this, node);
        mv.lineInfo(node);
        mv.begin();

        mv.enterScope(node);
        for (StatementListItem stmt : node.getStatements()) {
            statement(stmt, mv);
        }
        mv.exitScope();

        mv.loadCompletionValue();
        mv.areturn();
        mv.end();
    }

    private void multiScript(Script node) {
        // split script into several parts to pass all test262 test cases
        String desc = ScriptChunkStatementVisitor.methodDescriptor.getInternalName();

        List<StatementListItem> statements = node.getStatements();
        int num = statements.size();
        int index = 0;
        for (int end = 0, start = 0; end < num; start += STATEMENTS_THRESHOLD, ++index) {
            end = Math.min(start + STATEMENTS_THRESHOLD, num);
            StatementVisitor mv = new ScriptChunkStatementVisitor(this, node, index);
            mv.lineInfo(statements.get(start));
            mv.begin();

            mv.enterScope(node);
            for (StatementListItem stmt : statements.subList(start, end)) {
                statement(stmt, mv);
            }
            mv.exitScope();

            mv.loadCompletionValue();
            mv.areturn();
            mv.end();
        }

        StatementVisitor mv = new ScriptStatementVisitor(this, node);
        mv.lineInfo(node);
        mv.begin();

        for (int i = 0; i < index; ++i) {
            mv.loadExecutionContext();
            mv.loadCompletionValue();
            mv.invokestatic(getClassName(), methodName(node, i), desc);
            mv.storeCompletionValue();
        }

        mv.loadCompletionValue();
        mv.areturn();
        mv.end();
    }

    void compile(GeneratorComprehension node, ExpressionVisitor mv) {
        if (!isCompiled(node)) {
            ExpressionVisitor body = new GeneratorComprehensionVisitor(this, node, mv);
            body.lineInfo(node);
            body.begin();

            body.setScope(mv.getScope());
            node.accept(new GeneratorComprehensionGenerator(this), body);

            body.get(Fields.Undefined_UNDEFINED);
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

        // call expression in concise function is always in tail-call position
        tailCall(node.getExpression(), body);

        body.enterScope(node);
        ValType type = expression(node.getExpression(), body);
        body.toBoxed(type);
        invokeGetValue(node.getExpression(), body);
        body.exitScope();

        body.areturn();
        body.end();
    }

    private void functionBody(FunctionNode node) {
        StatementVisitor body = new FunctionStatementVisitor(this, node);
        body.lineInfo(node);
        body.begin();

        body.enterScope(node);
        for (StatementListItem stmt : node.getStatements()) {
            statement(stmt, body);
        }
        body.exitScope();

        body.mark(body.returnLabel());
        body.loadCompletionValue();
        body.areturn();
        body.end();
    }

    private void invokeGetValue(Expression node, ExpressionVisitor mv) {
        if (node.accept(IsReference.INSTANCE, null)) {
            mv.loadExecutionContext();
            mv.invoke(Methods.Reference_GetValue);
        }
    }

    ValType expression(Expression node, ExpressionVisitor mv) {
        return node.accept(exprgen, mv);
    }

    void propertyDefinition(PropertyDefinition node, ExpressionVisitor mv) {
        node.accept(propgen, mv);
    }

    void statement(StatementListItem node, StatementVisitor mv) {
        node.accept(stmtgen, mv);
    }

    /* ----------------------------------------------------------------------------------------- */

    private abstract static class StatementVisitorImpl extends StatementVisitor {
        private static final int COMPLETION_SLOT = 1;
        private static final Type COMPLETION_TYPE = Types.Object;

        private final boolean initCompletionValue;

        protected StatementVisitorImpl(CodeGenerator codegen, String methodName,
                Type methodDescriptor, boolean strict, boolean globalCode, boolean completionValue,
                boolean initCompletionValue) {
            super(codegen.publicStaticMethod(methodName, methodDescriptor.getInternalName()),
                    methodName, methodDescriptor, strict, globalCode, completionValue);
            this.initCompletionValue = initCompletionValue;
            reserveFixedSlot(COMPLETION_SLOT, COMPLETION_TYPE);
        }

        @Override
        void storeCompletionValue() {
            store(COMPLETION_SLOT, COMPLETION_TYPE);
        }

        @Override
        void loadCompletionValue() {
            load(COMPLETION_SLOT, COMPLETION_TYPE);
        }

        @Override
        public void begin() {
            super.begin();
            if (initCompletionValue) {
                get(Fields.Undefined_UNDEFINED);
                storeCompletionValue();
            }
        }
    }

    private abstract static class ExpressionVisitorImpl extends ExpressionVisitor {
        protected ExpressionVisitorImpl(CodeGenerator codegen, String methodName,
                Type methodDescriptor, boolean strict, boolean globalCode) {
            super(codegen.publicStaticMethod(methodName, methodDescriptor.getInternalName()),
                    methodName, methodDescriptor, strict, globalCode);
        }
    }

    private static class ScriptStatementVisitor extends StatementVisitorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        private ScriptStatementVisitor(CodeGenerator codegen, Script node) {
            super(codegen, codegen.methodName(node), methodDescriptor, node.isStrict(), node
                    .isGlobalCode(), true, true);
        }
    }

    private static class ScriptChunkStatementVisitor extends StatementVisitorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext, Types.Object);

        private ScriptChunkStatementVisitor(CodeGenerator codegen, Script node, int index) {
            super(codegen, codegen.methodName(node, index), methodDescriptor, IsStrict(node), node
                    .isGlobalCode(), true, false);
        }
    }

    private static class FunctionStatementVisitor extends StatementVisitorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        private FunctionStatementVisitor(CodeGenerator codegen, FunctionNode node) {
            super(codegen, codegen.methodName(node), methodDescriptor, IsStrict(node), false,
                    false, true);
        }
    }

    private static class ArrowFunctionVisitor extends ExpressionVisitorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        private ArrowFunctionVisitor(CodeGenerator codegen, ArrowFunction node) {
            super(codegen, codegen.methodName(node), methodDescriptor, IsStrict(node), false);
        }
    }

    private static class GeneratorComprehensionVisitor extends ExpressionVisitorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        private GeneratorComprehensionVisitor(CodeGenerator codegen, GeneratorComprehension node,
                ExpressionVisitor parent) {
            super(codegen, codegen.methodName(node), methodDescriptor, parent.isStrict(), parent
                    .isGlobalCode());
        }
    }
}
