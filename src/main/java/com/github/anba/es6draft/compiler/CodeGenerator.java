/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.ConstructorMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;

/**
 * 
 */
class CodeGenerator extends DefaultCodeGenerator<Void> {
    private static final boolean INCLUDE_SOURCE = true;

    private static final Future<String> NO_SOURCE = new Future<String>() {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public String get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public String get(long timeout, TimeUnit unit) throws InterruptedException,
                ExecutionException, TimeoutException {
            return null;
        }
    };

    private final ClassWriter cw;
    private final String className;
    private ExecutorService sourceCompressor;
    private Map<Node, String> methodNames = new HashMap<>(32);
    private AtomicInteger methodCounter = new AtomicInteger(0);

    private ScriptGenerator scriptgen = new ScriptGenerator(this);
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

    MethodVisitor publicStaticMethod(String name, String desc) {
        int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
        String signature = null;
        String[] exceptions = null;
        return cw.visitMethod(access, name, desc, signature, exceptions);
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

    private final int nextMethodInt() {
        return methodCounter.incrementAndGet();
    }

    private final String methodName(TemplateLiteral node) {
        String n = methodNames.get(node);
        if (n == null) {
            methodNames.put(node, n = "template_" + nextMethodInt());
        }
        return n;
    }

    final String methodName(GeneratorComprehension node) {
        String n = methodNames.get(node);
        if (n == null) {
            methodNames.put(node, n = "generator_" + nextMethodInt());
        }
        return n;
    }

    final String methodName(FunctionNode node) {
        String n = methodNames.get(node);
        if (n == null) {
            methodNames.put(node, n = node.accept(FunctionName.INSTANCE, "anonymous") + "_"
                    + nextMethodInt());
        }
        return n;
    }

    private final boolean isCompiled(TemplateLiteral node) {
        return methodNames.containsKey(node);
    }

    private final boolean isCompiled(GeneratorComprehension node) {
        return methodNames.containsKey(node);
    }

    private final boolean isCompiled(FunctionNode node) {
        return methodNames.containsKey(node);
    }

    private MethodGenerator generatorComprehensionGenerator(GeneratorComprehension node,
            MethodGenerator parent) {
        String name = methodName(node);
        String desc = Type.getMethodDescriptor(Types.Object, Types.ExecutionContext);
        MethodVisitor mv = publicStaticMethod(name, desc);
        MethodGenerator gen = new GeneratorComprehensionMethodGenerator(mv, name, desc,
                parent.isStrict(), parent.isGlobal());
        gen.init(Register.ExecutionContext);
        gen.init(Register.CompletionValue);
        gen.init(Register.Realm);

        lineInfo(node, gen);

        return gen;
    }

    private MethodGenerator functionGenerator(FunctionNode node) {
        String name = methodName(node);
        String desc = Type.getMethodDescriptor(Types.Object, Types.ExecutionContext);
        MethodVisitor mv = publicStaticMethod(name, desc);
        MethodGenerator gen = new FunctionMethodGenerator(mv, name, desc, node.isStrict());
        gen.init(Register.ExecutionContext);
        gen.init(Register.CompletionValue);
        gen.init(Register.Realm);

        lineInfo(node, gen);

        return gen;
    }

    MethodGenerator scriptGenerator(Script node) {
        return scriptGenerator(node, -1);
    }

    MethodGenerator scriptGenerator(Script node, int index) {
        String name, desc;
        if (index < 0) {
            name = "script";
            desc = Type.getMethodDescriptor(Types.Object, Types.ExecutionContext);
        } else {
            name = "script_" + index;
            desc = Type.getMethodDescriptor(Types.Object, Types.ExecutionContext, Types.Object);
        }
        MethodVisitor mv = publicStaticMethod(name, desc);
        MethodGenerator gen = new ScriptMethodGenerator(mv, name, desc, node.isStrict(),
                node.isGlobal());
        gen.init(Register.ExecutionContext);
        gen.init(Register.CompletionValue);
        gen.init(Register.Realm);

        lineInfo(node, gen);

        return gen;
    }

    void GetTemplateCallSite(TemplateLiteral node, MethodGenerator mv) {
        assert isCompiled(node);
        String methodName = methodName(node);
        String desc = Type.getMethodDescriptor(Types.String_);

        // GetTemplateCallSite
        mv.aconst(templateKey(node));
        mv.invokeStaticMH(className, methodName, desc);
        mv.load(Register.ExecutionContext);
        mv.invokestatic(Methods.ScriptRuntime_GetTemplateCallSite);
    }

    void compile(TemplateLiteral node) {
        if (!isCompiled(node)) {
            String name = methodName(node);
            String desc = Type.getMethodDescriptor(Types.String_);
            InstructionAdapter body = new InstructionAdapter(publicStaticMethod(name, desc));
            body.visitCode();
            // - start -
            List<Expression> elements = node.getElements();
            assert (elements.size() & 1) == 1;
            int numChars = ((elements.size() / 2) + 1);
            body.iconst(numChars * 2);
            body.newarray(Types.String);
            for (int i = 0, k = 0, size = elements.size(); i < size; ++i) {
                if ((i & 1) == 1) {
                    assert !(elements.get(i) instanceof TemplateCharacters);
                    continue;
                }
                TemplateCharacters e = (TemplateCharacters) elements.get(i);
                body.dup();
                body.iconst(k++);
                body.aconst(e.getValue());
                body.astore(Types.String);
                body.dup();
                body.iconst(k++);
                body.aconst(e.getRawValue());
                body.astore(Types.String);
            }
            body.areturn(Types.String_);
            // - end -
            body.visitMaxs(0, 0);
            body.visitEnd();
        }
    }

    void compile(GeneratorComprehension node, MethodGenerator mv) {
        if (!isCompiled(node)) {
            MethodGenerator body = generatorComprehensionGenerator(node, mv);

            body.visitCode();
            // - start -
            body.getstatic(Fields.Undefined_UNDEFINED);
            body.store(Register.CompletionValue);
            body.load(Register.ExecutionContext);
            body.invokevirtual(Methods.ExecutionContext_getRealm);
            body.store(Register.Realm);

            new GeneratorComprehensionGenerator(this).visit(node, body);

            body.mark(body.returnLabel());
            body.load(Register.CompletionValue);
            body.areturn(Types.Object);
            // - end -
            body.visitMaxs(0, 0);
            body.visitEnd();
        }
    }

    void compile(ArrowFunction node) {
        if (!isCompiled(node)) {
            Future<String> source = compressed(node.getSource());

            // binding method
            visitFunctionBinding(node, methodName(node));

            // runtime method
            visitFunctionBody(functionGenerator(node), node);

            // runtime-info method
            new RuntimeInfoGenerator(this).runtimeInfo(node, source);
        }
    }

    void compile(FunctionNode node) {
        if (!isCompiled(node)) {
            Future<String> source = compressed(node.getSource());

            // binding method
            visitFunctionBinding(node, methodName(node));

            // runtime method
            visitFunctionBody(functionGenerator(node), node);

            // runtime-info method
            new RuntimeInfoGenerator(this).runtimeInfo(node, source);
        }
    }

    private void visitFunctionBinding(FunctionNode node, String name) {
        String methodName = name + "_binding";
        String desc = Type.getMethodDescriptor(Type.VOID_TYPE, Types.ExecutionContext,
                Types.Scriptable, Types.LexicalEnvironment);
        MethodVisitor mv = publicStaticMethod(methodName, desc);
        MethodGenerator binding = new BindingMethodGenerator(mv, methodName, desc, node.isStrict());
        binding.initVariable(0, Types.ExecutionContext);
        binding.initVariable(1, Types.Scriptable);
        binding.initVariable(2, Types.LexicalEnvironment);
        binding.initVariable(3, Types.Realm);
        binding.visitCode();
        // - start -
        binding.load(Register.ExecutionContext);
        binding.invokevirtual(Methods.ExecutionContext_getRealm);
        binding.store(Register.Realm);
        BindingInitialisation(node, binding);
        binding.areturn(Type.VOID_TYPE);
        // - end -
        binding.visitMaxs(0, 0);
        binding.visitEnd();
    }

    private void visitFunctionBody(MethodGenerator body, ArrowFunction node) {
        assert body.isCompletionValue() == false;

        if (node.getExpression() != null) {
            body.visitCode();
            // - start -
            body.load(Register.ExecutionContext);
            body.invokevirtual(Methods.ExecutionContext_getRealm);
            body.store(Register.Realm);

            tailCall(node.getExpression(), body);

            node.getExpression().accept(this, body);
            invokeGetValue(node.getExpression(), body);

            body.areturn(Types.Object);
            // - end -
            body.visitMaxs(0, 0);
            body.visitEnd();
        } else {
            visitFunctionBody(body, (FunctionNode) node);
        }
    }

    private void visitFunctionBody(MethodGenerator body, FunctionNode node) {
        assert body.isCompletionValue() == false;

        body.visitCode();
        // - start -
        body.getstatic(Fields.Undefined_UNDEFINED);
        body.store(Register.CompletionValue);
        body.load(Register.ExecutionContext);
        body.invokevirtual(Methods.ExecutionContext_getRealm);
        body.store(Register.Realm);

        for (StatementListItem stmt : node.getStatements()) {
            stmt.accept(this, body);
        }

        body.mark(body.returnLabel());
        body.load(Register.CompletionValue);
        body.areturn(Types.Object);
        // - end -
        body.visitMaxs(0, 0);
        body.visitEnd();
    }

    enum Register {
        ExecutionContext(Types.ExecutionContext), Realm(Types.Realm), CompletionValue(Types.Object);
        final Type type;

        Register(Type type) {
            this.type = type;
        }
    }

    private static class ScriptMethodGenerator extends MethodGenerator {
        private ScriptMethodGenerator(MethodVisitor mv, String methodName, String methodDescriptor,
                boolean strict, boolean global) {
            super(mv, methodName, methodDescriptor, strict, global, true);
        }

        @Override
        protected int var(Register reg) {
            switch (reg) {
            case ExecutionContext:
                return 0;
            case CompletionValue:
                return 1;
            case Realm:
                return 2;
            default:
                assert false : reg;
                return -1;
            }
        }
    }

    private static class FunctionMethodGenerator extends MethodGenerator {
        private FunctionMethodGenerator(MethodVisitor mv, String methodName,
                String methodDescriptor, boolean strict) {
            super(mv, methodName, methodDescriptor, strict, false, false);
        }

        @Override
        protected int var(Register reg) {
            switch (reg) {
            case ExecutionContext:
                return 0;
            case CompletionValue:
                return 1;
            case Realm:
                return 2;
            default:
                assert false : reg;
                return -1;
            }
        }
    }

    private static class GeneratorComprehensionMethodGenerator extends MethodGenerator {
        private GeneratorComprehensionMethodGenerator(MethodVisitor mv, String methodName,
                String methodDescriptor, boolean strict, boolean global) {
            super(mv, methodName, methodDescriptor, strict, global, false);
        }

        @Override
        protected int var(Register reg) {
            switch (reg) {
            case ExecutionContext:
                return 0;
            case CompletionValue:
                return 1;
            case Realm:
                return 2;
            default:
                assert false : reg;
                return -1;
            }
        }
    }

    private static class BindingMethodGenerator extends MethodGenerator {
        private BindingMethodGenerator(MethodVisitor mv, String methodName,
                String methodDescriptor, boolean strict) {
            super(mv, methodName, methodDescriptor, strict, false, false);
        }

        @Override
        protected int var(Register reg) {
            switch (reg) {
            case ExecutionContext:
                return 0;
                // 1 = Scriptable
                // 2 = LexicalEnvironment
            case Realm:
                return 3;
            case CompletionValue:
            default:
                assert false : reg;
                return -1;
            }
        }
    }

    /* ----------------------------------------------------------------------------------------- */

    protected Void visit(Node node, MethodGenerator mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    @Override
    public Void visit(Script node, MethodGenerator mv) {
        node.accept(scriptgen, mv);
        return null;
    }

    @Override
    public Void visit(Expression node, MethodGenerator mv) {
        ValType type = node.accept(exprgen, mv);
        mv.toBoxed(type);
        return null;
    }

    ValType expression(Expression node, MethodGenerator mv) {
        return node.accept(exprgen, mv);
    }

    @Override
    protected Void visit(PropertyDefinition node, MethodGenerator mv) {
        node.accept(propgen, mv);
        return null;
    }

    @Override
    protected Void visit(StatementListItem node, MethodGenerator mv) {
        node.accept(stmtgen, mv);
        return null;
    }

    /* ----------------------------------------------------------------------------------------- */

    void BindingInitialisation(FunctionNode node, MethodGenerator mv) {
        new BindingInitialisationGenerator(this).generate(node, mv);
    }

    void BindingInitialisation(Binding node, MethodGenerator mv) {
        new BindingInitialisationGenerator(this).generate(node, mv);
    }

    void BindingInitialisationWithEnvironment(Binding node, MethodGenerator mv) {
        new BindingInitialisationGenerator(this).generateWithEnvironment(node, mv);
    }

    void DestructuringAssignment(AssignmentPattern node, MethodGenerator mv) {
        new DestructuringAssignmentGenerator(this).generate(node, mv);
    }

    void ClassDefinitionEvaluation(ClassDefinition def, String className, MethodGenerator mv) {
        // stack: [] -> [<proto,ctor>]
        if (def.getHeritage() == null) {
            mv.load(Register.Realm);
            mv.invokestatic(Methods.ScriptRuntime_getDefaultClassProto);
        } else {
            // FIXME: spec bug (ClassHeritage runtime evaluation not defined)
            def.getHeritage().accept(this, mv);
            invokeGetValue(def.getHeritage(), mv);
            mv.load(Register.Realm);
            mv.invokestatic(Methods.ScriptRuntime_getClassProto);
        }

        // stack: [<proto,ctor>] -> [ctor, proto]
        mv.dup();
        mv.iconst(1);
        mv.aload(Types.Scriptable_);
        mv.swap();
        mv.iconst(0);
        mv.aload(Types.Scriptable_);

        // steps 4-5
        if (className != null) {
            // stack: [ctor, proto] -> [ctor, proto, scope]
            newDeclarativeEnvironment(mv);

            // stack: [ctor, proto, scope] -> [ctor, proto, scope, proto, scope]
            mv.dup2();

            // stack: [ctor, proto, scope, proto, scope] -> [ctor, proto, scope, proto, envRec]
            mv.invokevirtual(Methods.LexicalEnvironment_getEnvRec);

            // stack: [ctor, proto, scope, proto, envRec] -> [ctor, proto, scope, proto, envRec]
            mv.dup();
            mv.aconst(className);
            mv.invokeinterface(Methods.EnvironmentRecord_createImmutableBinding);

            // stack: [ctor, proto, scope, proto, envRec] -> [ctor, proto, scope]
            mv.swap();
            mv.aconst(className);
            mv.swap();
            mv.invokeinterface(Methods.EnvironmentRecord_initializeBinding);

            // stack: [ctor, proto, scope] -> [ctor, proto]
            pushLexicalEnvironment(mv);
        }

        // steps 6-12
        MethodDefinition constructor = ConstructorMethod(def);
        if (constructor != null) {
            compile(constructor);

            // Runtime Semantics: Evaluation -> MethodDefinition
            // stack: [ctor, proto] -> [proto, F]
            mv.dupX1();
            mv.invokestatic(this.className, methodName(constructor) + "_rti",
                    Type.getMethodDescriptor(Types.RuntimeInfo$Function));
            mv.load(Register.ExecutionContext);
            mv.invokestatic(Methods.ScriptRuntime_EvaluateConstructorMethod);
        } else {
            // default constructor
            // stack: [ctor, proto] -> [proto, F]
            mv.dupX1();
            mv.invokestatic(Methods.ScriptRuntime_CreateDefaultConstructor);
            mv.load(Register.ExecutionContext);
            mv.invokestatic(Methods.ScriptRuntime_EvaluateConstructorMethod);
        }

        // stack: [proto, F] -> [F, proto]
        mv.swap();

        // steps 13-14
        List<MethodDefinition> methods = def.getBody();
        for (MethodDefinition method : methods) {
            if (method == constructor) {
                // FIXME: spec bug? (not handled in draft)
                continue;
            }
            mv.dup();
            method.accept(this, mv);
        }

        // step 15
        if (className != null) {
            // restore previous lexical environment
            popLexicalEnvironment(mv);
        }

        // stack: [F, proto] -> [F]
        mv.pop();
    }
}
