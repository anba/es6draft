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
import org.objectweb.asm.commons.CodeSizeEvaluator;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.FunctionNode.StrictMode;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
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
class CodeGenerator implements AutoCloseable {
    private static class Fields {
        static final FieldDesc Undefined_UNDEFINED = FieldDesc.create(FieldType.Static,
                Types.Undefined, "UNDEFINED", Types.Undefined);
    }

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

    final String methodName(StatementListMethod node) {
        String n = methodNames.get(node);
        if (n == null) {
            n = addMethodName(node, "stmtmethod");
        }
        return n;
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
            n = addMethodName(node, "inlarrayspread");
        }
        return n;
    }

    final String methodName(PropertyDefinitionsMethod node) {
        String n = methodNames.get(node);
        if (n == null) {
            n = addMethodName(node, "inlpropdef");
        }
        return n;
    }

    final String methodName(ExpressionMethod node) {
        String n = methodNames.get(node);
        if (n == null) {
            n = addMethodName(node, "methexpr");
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
            return '!' + fname;
        case Init:
            return '!' + fname + "_init";
        case RTI:
            return fname + "_rti";
        default:
            throw new IllegalStateException();
        }
    }

    private final String methodName(FunctionNode node) {
        String n = methodNames.get(node);
        if (n == null) {
            String fname = node.getFunctionName();
            if (fname.isEmpty()) {
                fname = "anonymous";
            }
            n = addMethodName(node, fname);
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
        for (StatementListItem stmt : node.getStatements()) {
            statement(stmt, body);
        }
        body.exitScope();

        body.mark(body.returnLabel());
        body.loadCompletionValue();
        body.areturn();
        body.end();
    }

    void compile(StatementListMethod node, StatementVisitor mv) {
        if (!isCompiled(node)) {
            StatementVisitor body = new StatementListMethodStatementVisitor(this, node, mv);
            body.lineInfo(node);
            body.begin();

            body.setScope(mv.getScope());
            for (StatementListItem stmt : node.getStatements()) {
                statement(stmt, body);
            }

            body.loadCompletionValue();
            body.areturn();

            // emit return-label if nested in function
            if (body.getCodeType() == StatementVisitor.CodeType.Function) {
                body.mark(body.returnLabel());
                body.load(2, Types.boolean_);
                body.iconst(0);
                body.iconst(true);
                body.astore(Type.BOOLEAN_TYPE);

                body.loadCompletionValue();
                body.areturn();
            }

            body.end();
        }
    }

    void compile(SpreadElementMethod node, ExpressionVisitor mv) {
        if (!isCompiled(node)) {
            ExpressionVisitor body = new InlineArraySpreadVisitor(this, node, mv);
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
            ExpressionVisitor body = new InlinePropertyDefinitionVisitor(this, node, mv);
            body.lineInfo(node);
            body.begin();

            body.setScope(mv.getScope());
            List<PropertyDefinition> properties = node.getProperties();
            for (PropertyDefinition property : properties) {
                body.load(1, Types.ScriptObject);
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

        return type;
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
                Type methodDescriptor, boolean strict, CodeType codeType, boolean completionValue,
                boolean initCompletionValue) {
            super(codegen.publicStaticMethod(methodName, methodDescriptor.getInternalName()),
                    methodName, methodDescriptor, strict, codeType, completionValue);
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

        ScriptStatementVisitor(CodeGenerator codegen, Script node) {
            super(codegen, codegen.methodName(node, ScriptName.Code), methodDescriptor, node
                    .isStrict(), node.isGlobalCode() ? CodeType.GlobalScript
                    : CodeType.NonGlobalScript, true, true);
        }
    }

    private static class FunctionStatementVisitor extends StatementVisitorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        FunctionStatementVisitor(CodeGenerator codegen, FunctionNode node) {
            super(codegen, codegen.methodName(node, FunctionName.Code), methodDescriptor,
                    IsStrict(node), CodeType.Function, false, true);
        }
    }

    private static class StatementListMethodStatementVisitor extends StatementVisitorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext, Types.Object, Types.boolean_);

        StatementListMethodStatementVisitor(CodeGenerator codegen, StatementListMethod node,
                StatementVisitor parent) {
            super(codegen, codegen.methodName(node), methodDescriptor, parent.isStrict(), parent
                    .getCodeType(), parent.isCompletionValue(), false);
        }
    }

    private static class ArrowFunctionVisitor extends ExpressionVisitorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        ArrowFunctionVisitor(CodeGenerator codegen, ArrowFunction node) {
            super(codegen, codegen.methodName(node, FunctionName.Code), methodDescriptor,
                    IsStrict(node), false);
        }
    }

    private static class GeneratorComprehensionVisitor extends ExpressionVisitorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        GeneratorComprehensionVisitor(CodeGenerator codegen, GeneratorComprehension node,
                ExpressionVisitor parent) {
            super(codegen, codegen.methodName(node), methodDescriptor, parent.isStrict(), parent
                    .isGlobalCode());
        }
    }

    private static class ExpressionMethodVisitor extends ExpressionVisitorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        ExpressionMethodVisitor(CodeGenerator codegen, ExpressionMethod node,
                ExpressionVisitor parent) {
            super(codegen, codegen.methodName(node), methodDescriptor, parent.isStrict(), parent
                    .isGlobalCode());
        }
    }

    private static class InlineArraySpreadVisitor extends ExpressionVisitorImpl {
        static final Type methodDescriptor = Type.getMethodType(Type.INT_TYPE,
                Types.ExecutionContext, Types.ExoticArray, Type.INT_TYPE);

        InlineArraySpreadVisitor(CodeGenerator codegen, SpreadElementMethod node,
                ExpressionVisitor parent) {
            super(codegen, codegen.methodName(node), methodDescriptor, parent.isStrict(), parent
                    .isGlobalCode());
        }
    }

    private static class InlinePropertyDefinitionVisitor extends ExpressionVisitorImpl {
        static final Type methodDescriptor = Type.getMethodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.ScriptObject);

        InlinePropertyDefinitionVisitor(CodeGenerator codegen, PropertyDefinitionsMethod node,
                ExpressionVisitor parent) {
            super(codegen, codegen.methodName(node), methodDescriptor, parent.isStrict(), parent
                    .isGlobalCode());
        }
    }
}
