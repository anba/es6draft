/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.compiler.CodeGenerator.ScriptName;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.TryCatchLabel;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Value;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;

/**
 * Generates bytecode for the script entry method.
 */
final class ScriptCodeGenerator {
    private static final class Methods {
        // ExecutionContext
        static final MethodName ExecutionContext_newEvalExecutionContext = MethodName.findStatic(
                Types.ExecutionContext, "newEvalExecutionContext",
                Type.methodType(Types.ExecutionContext, Types.ExecutionContext, Types.Script,
                        Types.LexicalEnvironment, Types.LexicalEnvironment));

        static final MethodName ExecutionContext_newScriptExecutionContext = MethodName.findStatic(
                Types.ExecutionContext, "newScriptExecutionContext",
                Type.methodType(Types.ExecutionContext, Types.Realm, Types.Script));

        static final MethodName ExecutionContext_getLexicalEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "getLexicalEnvironment",
                Type.methodType(Types.LexicalEnvironment));

        static final MethodName ExecutionContext_setLexicalEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "setLexicalEnvironment",
                Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        static final MethodName ExecutionContext_getVariableEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "getVariableEnvironment",
                Type.methodType(Types.LexicalEnvironment));

        static final MethodName ExecutionContext_getRealm = MethodName
                .findVirtual(Types.ExecutionContext, "getRealm", Type.methodType(Types.Realm));

        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_newDeclarativeEnvironment = MethodName
                .findStatic(Types.LexicalEnvironment, "newDeclarativeEnvironment", Type.methodType(
                        Types.LexicalEnvironment, Types.LexicalEnvironment));

        // Realm
        static final MethodName Realm_getGlobalEnv = MethodName.findVirtual(Types.Realm,
                "getGlobalEnv", Type.methodType(Types.LexicalEnvironment));

        static final MethodName Realm_getScriptContext = MethodName.findVirtual(Types.Realm,
                "getScriptContext", Type.methodType(Types.ExecutionContext));

        static final MethodName Realm_setScriptContext = MethodName.findVirtual(Types.Realm,
                "setScriptContext", Type.methodType(Type.VOID_TYPE, Types.ExecutionContext));
    }

    private static final int EXECUTION_CONTEXT = 0;
    private static final int SCRIPT = 1;

    private static final class ScriptEvalMethodGenerator extends InstructionVisitor {
        ScriptEvalMethodGenerator(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("callerContext", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("script", SCRIPT, Types.Script);
        }
    }

    private final CodeGenerator codegen;

    ScriptCodeGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    void generate(Script node) {
        InstructionVisitor mv = new ScriptEvalMethodGenerator(
                codegen.newMethod(node, ScriptName.Eval));
        mv.lineInfo(node);
        mv.begin();
        if (node.isScripting()) {
            generateScriptingEvaluation(node, mv);
        } else if (node.isEvalScript()) {
            generateEvalScriptEvaluation(node, mv);
        } else {
            generateGlobalScriptEvaluation(node, mv);
        }
        mv.end();
    }

    /**
     * 15.1.7 Runtime Semantics: ScriptEvaluation
     * 
     * @param node
     *            the script node
     * @param mv
     *            the instruction visitor
     */
    private void generateGlobalScriptEvaluation(Script node, InstructionVisitor mv) {
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<com.github.anba.es6draft.Script> script = mv.getParameter(SCRIPT,
                com.github.anba.es6draft.Script.class);
        Variable<Realm> realm = mv.newVariable("realm", Realm.class);
        Variable<ExecutionContext> scriptCxt = mv.newVariable("scriptCxt", ExecutionContext.class);
        Variable<ExecutionContext> oldScriptContext = mv.newVariable("oldScriptContext",
                ExecutionContext.class);
        Variable<Object> result = mv.newVariable("result", Object.class);
        Variable<Throwable> throwable = mv.newVariable("throwable", Throwable.class);

        getRealm(callerContext, realm, mv);
        /* steps 1-2 (not applicable) */
        /* steps 3-7 */
        newScriptExecutionContext(realm, script, scriptCxt, mv);
        /* step 8 */
        getScriptContext(realm, oldScriptContext, mv);
        /* step 9 */
        setScriptContext(realm, scriptCxt, mv);

        TryCatchLabel startFinally = new TryCatchLabel(), endFinally = new TryCatchLabel();
        TryCatchLabel handlerFinally = new TryCatchLabel();
        mv.mark(startFinally);
        {
            /* step 10 */
            mv.load(scriptCxt);
            mv.invoke(codegen.methodDesc(node, ScriptName.Init));
            /* steps 11-12 */
            mv.load(scriptCxt);
            mv.invoke(codegen.methodDesc(node, ScriptName.Code));
            mv.store(result);
            /* steps 13-15  */
            setScriptContext(realm, oldScriptContext, mv);
            /* step 16 */
            mv.load(result);
            mv._return();
        }
        mv.mark(endFinally);

        // Exception: Restore script context and then rethrow exception
        mv.finallyHandler(handlerFinally);
        mv.store(throwable);
        /* steps 13-15 */
        setScriptContext(realm, oldScriptContext, mv);
        mv.load(throwable);
        mv.athrow();

        mv.tryFinally(startFinally, endFinally, handlerFinally);
    }

    /**
     * 18.2.1.1 Runtime Semantics: PerformEval( x, evalRealm, strictCaller, direct)
     * 
     * @param node
     *            the script node
     * @param mv
     *            the instruction visitor
     */
    private void generateEvalScriptEvaluation(Script node, InstructionVisitor mv) {
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<com.github.anba.es6draft.Script> script = mv.getParameter(SCRIPT,
                com.github.anba.es6draft.Script.class);
        Variable<ExecutionContext> evalCxt = mv.newVariable("evalCxt", ExecutionContext.class);
        Variable<? extends LexicalEnvironment<?>> varEnv = mv
                .newVariable("varEnv", LexicalEnvironment.class).uncheckedCast();
        Variable<? extends LexicalEnvironment<?>> lexEnv = mv
                .newVariable("lexEnv", LexicalEnvironment.class).uncheckedCast();

        // Optimization: Skip creating lexical environment if no declarations are present.
        boolean noDeclarations = node.getScope().lexicallyDeclaredNames().isEmpty();
        if (node.isStrict()) {
            noDeclarations &= node.getScope().varDeclaredNames().isEmpty();
        }

        /* steps 1-5 (not applicable) */
        /* steps 6-7 */
        boolean strictEval = node.isStrict();
        /* step 8 (omitted) */
        /* steps 9-10 */
        if (node.isDirectEval()) {
            /* step 9 */
            getVariableEnvironment(callerContext, varEnv, mv);
            if (noDeclarations) {
                getLexicalEnvironment(callerContext, lexEnv, mv);
            } else {
                newDeclarativeEnvironment(callerContext, lexEnv, mv);
            }
        } else {
            /* step 10 */
            getGlobalEnv(callerContext, varEnv, mv);
            if (noDeclarations) {
                lexEnv = varEnv;
            } else {
                newDeclarativeEnvironment(varEnv, lexEnv, mv);
            }
        }
        /* step 11 */
        if (strictEval) {
            varEnv = lexEnv;
        }
        /* steps 12-17 */
        newEvalExecutionContext(callerContext, script, varEnv, lexEnv, evalCxt, mv);
        /* step 18 */
        mv.load(evalCxt);
        mv.invoke(codegen.methodDesc(node, ScriptName.Init));
        /* steps 19-23 */
        mv.load(evalCxt);
        mv.invoke(codegen.methodDesc(node, ScriptName.Code));
        mv._return();
    }

    private void generateScriptingEvaluation(Script node, InstructionVisitor mv) {
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> lexEnv = mv
                .newVariable("lexEnv", LexicalEnvironment.class).uncheckedCast();

        // Create an empty declarative environment for the lexical bindings.
        newDeclarativeEnvironment(context, lexEnv, mv);

        // Replace the lexical environment component.
        setLexicalEnvironment(context, lexEnv, mv);

        // Create local bindings.
        mv.load(context);
        mv.invoke(codegen.methodDesc(node, ScriptName.Init));

        // Evaluate the actual script code.
        mv.load(context);
        mv.invoke(codegen.methodDesc(node, ScriptName.Code));
        mv._return();
    }

    /**
     * Emit: {@code realm = context.getRealm()}
     */
    private void getRealm(Variable<ExecutionContext> context, Variable<Realm> realm,
            InstructionVisitor mv) {
        mv.load(context);
        mv.invoke(Methods.ExecutionContext_getRealm);
        mv.store(realm);
    }

    /**
     * Emit: {@code context = ExecutionContext.newScriptExecutionContext(realm, script)}
     */
    private void newScriptExecutionContext(Variable<Realm> realm,
            Variable<com.github.anba.es6draft.Script> script, Variable<ExecutionContext> context,
            InstructionVisitor mv) {
        mv.load(realm);
        mv.load(script);
        mv.invoke(Methods.ExecutionContext_newScriptExecutionContext);
        mv.store(context);
    }

    /**
     * Emit:
     * {@code context = ExecutionContext.newEvalExecutionContext(callerContext, script, varEnv, lexEnv)}
     */
    private void newEvalExecutionContext(Variable<ExecutionContext> callerContext,
            Variable<com.github.anba.es6draft.Script> script,
            Variable<? extends LexicalEnvironment<?>> varEnv,
            Variable<? extends LexicalEnvironment<?>> lexEnv, Variable<ExecutionContext> context,
            InstructionVisitor mv) {
        mv.load(callerContext);
        mv.load(script);
        mv.load(varEnv);
        mv.load(lexEnv);
        mv.invoke(Methods.ExecutionContext_newEvalExecutionContext);
        mv.store(context);
    }

    /**
     * Emit: {@code env = cx.getRealm().getGlobalEnv()}
     */
    private void getGlobalEnv(Variable<ExecutionContext> context,
            Variable<? extends LexicalEnvironment<?>> env, InstructionVisitor mv) {
        mv.load(context);
        mv.invoke(Methods.ExecutionContext_getRealm);
        mv.invoke(Methods.Realm_getGlobalEnv);
        mv.store(env);
    }

    /**
     * Emit: {@code realm = realm.getScriptContext()}
     */
    private void getScriptContext(Variable<Realm> realm, Variable<ExecutionContext> context,
            InstructionVisitor mv) {
        mv.load(realm);
        mv.invoke(Methods.Realm_getScriptContext);
        mv.store(context);
    }

    /**
     * Emit: {@code realm.setScriptContext(context)}
     */
    private void setScriptContext(Variable<Realm> realm, Variable<ExecutionContext> context,
            InstructionVisitor mv) {
        mv.load(realm);
        mv.load(context);
        mv.invoke(Methods.Realm_setScriptContext);
    }

    /**
     * Emit: {@code env = LexicalEnvironment.newDeclarativeEnvironment(outer)}
     */
    private void newDeclarativeEnvironment(Value<? extends LexicalEnvironment<?>> outer,
            Variable<? extends LexicalEnvironment<?>> env, InstructionVisitor mv) {
        mv.load(outer);
        mv.invoke(Methods.LexicalEnvironment_newDeclarativeEnvironment);
        mv.store(env);
    }

    /**
     * Emit:
     * {@code env = LexicalEnvironment.newDeclarativeEnvironment(context.getLexicalEnvironment())}
     */
    private void newDeclarativeEnvironment(Variable<ExecutionContext> context,
            Variable<? extends LexicalEnvironment<?>> env, InstructionVisitor mv) {
        mv.load(context);
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_newDeclarativeEnvironment);
        mv.store(env);
    }

    /**
     * Emit: {@code env = context.getVariableEnvironment()}
     */
    private void getVariableEnvironment(Variable<ExecutionContext> context,
            Variable<? extends LexicalEnvironment<?>> env, InstructionVisitor mv) {
        mv.load(context);
        mv.invoke(Methods.ExecutionContext_getVariableEnvironment);
        mv.store(env);
    }

    /**
     * Emit: {@code env = context.getLexicalEnvironment()}
     */
    private void getLexicalEnvironment(Variable<ExecutionContext> context,
            Variable<? extends LexicalEnvironment<?>> env, InstructionVisitor mv) {
        mv.load(context);
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.store(env);
    }

    /**
     * Emit: {@code context.setLexicalEnvironment(env)}
     */
    private void setLexicalEnvironment(Variable<ExecutionContext> context,
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env, InstructionVisitor mv) {
        mv.load(context);
        mv.load(env);
        mv.invoke(Methods.ExecutionContext_setLexicalEnvironment);
    }
}
