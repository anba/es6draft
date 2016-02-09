/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.HoistableDeclaration;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.VariableDeclaration;
import com.github.anba.es6draft.ast.VariableStatement;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.CodeGenerator.ScriptName;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>18 The Global Object</h1><br>
 * <h2>18.2 Function Properties of the Global Object</h2><br>
 * <h3>18.2.1 eval (x)</h3>
 * <ul>
 * <li>18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation( body, varEnv, lexEnv, strict)
 * </ul>
 */
final class EvalDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_getOuter = MethodName.findVirtual(
                Types.LexicalEnvironment, "getOuter", Type.methodType(Types.LexicalEnvironment));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_canDeclareVarOrThrow = MethodName.findStatic(
                Types.ScriptRuntime, "canDeclareVarOrThrow", Type.methodType(Type.VOID_TYPE,
                        Types.ExecutionContext, Types.DeclarativeEnvironmentRecord, Types.String,
                        Type.BOOLEAN_TYPE));
    }

    private static final int EXECUTION_CONTEXT = 0;

    private static final class EvalDeclInitMethodGenerator extends InstructionVisitor {
        EvalDeclInitMethodGenerator(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", EXECUTION_CONTEXT, Types.ExecutionContext);
        }
    }

    EvalDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(Script evalScript) {
        MethodCode method = codegen.newMethod(evalScript, ScriptName.Init);
        InstructionVisitor mv = new EvalDeclInitMethodGenerator(method);

        mv.lineInfo(evalScript);
        mv.begin();
        if (VarDeclaredNames(evalScript).isEmpty() && LexicallyDeclaredNames(evalScript).isEmpty()) {
            mv._return();
        } else if (evalScript.isScripting()) {
            generateScripting(evalScript, mv);
        } else if (evalScript.isStrict()) {
            generateStrict(evalScript, mv);
        } else if (evalScript.isGlobalCode()) {
            generateGlobal(evalScript, mv);
        } else {
            generateFunction(evalScript, mv);
        }
        mv.end();
    }

    private void generateGlobal(Script evalScript, InstructionVisitor mv) {
        assert evalScript.isGlobalCode() && !evalScript.isStrict() && !evalScript.isScripting();
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment<GlobalEnvironmentRecord>> varEnv = mv.newVariable("varEnv",
                LexicalEnvironment.class).uncheckedCast();
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> lexEnv = mv.newVariable(
                "lexEnv", LexicalEnvironment.class).uncheckedCast();
        Variable<FunctionObject> fo = null;

        getVariableEnvironment(context, varEnv, mv);
        getLexicalEnvironment(context, lexEnv, mv);

        /* step 1 */
        Set<Name> varNames = VarDeclaredNames(evalScript);
        /* step 2 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(evalScript);
        /* step 3 */
        Variable<DeclarativeEnvironmentRecord> lexEnvRec = mv.newVariable("lexEnvRec",
                DeclarativeEnvironmentRecord.class);
        /* step 4 */
        Variable<GlobalEnvironmentRecord> varEnvRec = mv.newVariable("varEnvRec",
                GlobalEnvironmentRecord.class);
        getEnvironmentRecord(varEnv, varEnvRec, mv);
        /* step 5 */
        if (!varNames.isEmpty()) {
            /* step 5.a */
            // Iterate over declarations to be able to emit line-info entries.
            HashSet<Name> checkedVarNames = new HashSet<>();
            for (StatementListItem item : VarScopedDeclarations(evalScript)) {
                if (item instanceof VariableStatement) {
                    for (VariableDeclaration vd : ((VariableStatement) item).getElements()) {
                        for (Name name : BoundNames(vd)) {
                            if (checkedVarNames.add(name)) {
                                canDeclareVarScopedOrThrow(context, varEnvRec, vd, name, mv);
                            }
                        }
                    }
                } else {
                    HoistableDeclaration d = (HoistableDeclaration) item;
                    Name name = BoundName(d);
                    if (checkedVarNames.add(name)) {
                        canDeclareVarScopedOrThrow(context, varEnvRec, d, name, mv);
                    }
                }
            }
            /* steps 5.b-d */
            if (isEnclosedByLexicalOrHasVarForOf(evalScript)) {
                checkLexicalRedeclaration(evalScript, context, varEnv, lexEnv, varNames, mv);
            }
        }
        /* step 6 */
        ArrayDeque<HoistableDeclaration> functionsToInitialize = new ArrayDeque<>();
        /* step 7 */
        HashSet<Name> declaredFunctionNames = new HashSet<>();
        /* step 8 */
        for (StatementListItem item : reverse(varDeclarations)) {
            if (item instanceof HoistableDeclaration) {
                HoistableDeclaration d = (HoistableDeclaration) item;
                Name fn = BoundName(d);
                if (declaredFunctionNames.add(fn)) {
                    canDeclareGlobalFunctionOrThrow(context, varEnvRec, d, fn, mv);
                    functionsToInitialize.addFirst(d);
                }
            }
        }
        if (!functionsToInitialize.isEmpty()) {
            fo = mv.newVariable("fo", FunctionObject.class);
        }
        /* step 9 */
        LinkedHashMap<Name, VariableDeclaration> declaredVarNames = new LinkedHashMap<>();
        /* step 10 */
        for (StatementListItem d : varDeclarations) {
            if (d instanceof VariableStatement) {
                for (VariableDeclaration vd : ((VariableStatement) d).getElements()) {
                    for (Name vn : BoundNames(vd)) {
                        if (!declaredFunctionNames.contains(vn)) {
                            canDeclareGlobalVarOrThrow(context, varEnvRec, vd, vn, mv);
                            declaredVarNames.put(vn, vd);
                        }
                    }
                }
            }
        }
        /* step 11 (note) */
        /* step 12 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(evalScript);
        /* step 13 */
        if (!lexDeclarations.isEmpty()) {
            getEnvironmentRecord(lexEnv, lexEnvRec, mv);
            createLexicalDeclarations(lexDeclarations, lexEnvRec, mv);
        }
        /* step 14 */
        for (HoistableDeclaration f : functionsToInitialize) {
            Name fn = BoundName(f);
            InstantiateFunctionObject(context, lexEnv, f, mv);
            mv.store(fo);
            createGlobalFunctionBinding(varEnvRec, f, fn, fo, true, mv);
        }
        /* step 15 */
        for (Map.Entry<Name, VariableDeclaration> e : declaredVarNames.entrySet()) {
            createGlobalVarBinding(varEnvRec, e.getValue(), e.getKey(), true, mv);
        }
        /* step 16 */
        mv._return();
    }

    private void generateFunction(Script evalScript, InstructionVisitor mv) {
        assert evalScript.isFunctionCode() && !evalScript.isStrict() && !evalScript.isScripting();
        final boolean strict = false;
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> varEnv = mv.newVariable(
                "varEnv", LexicalEnvironment.class).uncheckedCast();
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> lexEnv = mv.newVariable(
                "lexEnv", LexicalEnvironment.class).uncheckedCast();
        Variable<FunctionObject> fo = null;
        Variable<Undefined> undef = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undef);

        getVariableEnvironment(context, varEnv, mv);
        getLexicalEnvironment(context, lexEnv, mv);

        /* step 1 */
        Set<Name> varNames = VarDeclaredNames(evalScript);
        /* step 2 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(evalScript);
        /* step 3 */
        Variable<DeclarativeEnvironmentRecord> lexEnvRec = mv.newVariable("lexEnvRec",
                DeclarativeEnvironmentRecord.class);
        /* step 4 */
        Variable<DeclarativeEnvironmentRecord> varEnvRec = mv.newVariable("varEnvRec",
                DeclarativeEnvironmentRecord.class);
        getEnvironmentRecord(varEnv, varEnvRec, mv);
        /* step 5 */
        if (!varNames.isEmpty() && isEnclosedByLexicalOrHasVarForOf(evalScript)) {
            /* step 5.a (not applicable) */
            /* steps 5.b-d */
            checkLexicalRedeclaration(evalScript, context, varEnv, lexEnv, varNames, mv);
        }
        /* step 6 */
        ArrayDeque<HoistableDeclaration> functionsToInitialize = new ArrayDeque<>();
        /* step 7 */
        HashSet<Name> declaredFunctionNames = new HashSet<>();
        /* step 8 */
        if (findFunctionDeclarations(varDeclarations, functionsToInitialize, declaredFunctionNames)) {
            fo = mv.newVariable("fo", FunctionObject.class);
        }
        /* step 9 */
        LinkedHashSet<Name> declaredVarNames = new LinkedHashSet<>(varNames);
        /* step 10 */
        declaredVarNames.removeAll(declaredFunctionNames);
        /* step 11 (note) */
        /* step 12 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(evalScript);
        /* step 13 */
        if (!lexDeclarations.isEmpty()) {
            getEnvironmentRecord(lexEnv, lexEnvRec, mv);
            createLexicalDeclarations(lexDeclarations, lexEnvRec, mv);
        }
        /* step 14 */
        createFunctions(functionsToInitialize, strict, context, lexEnv, fo, varEnvRec, mv);
        /* step 15 */
        createVarDeclarations(declaredVarNames, varEnvRec, undef, mv);
        /* step 16 */
        mv._return();
    }

    private void generateScripting(Script evalScript, InstructionVisitor mv) {
        assert evalScript.isScripting();
        final boolean strict = IsStrict(evalScript);
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment<EnvironmentRecord>> varEnv = mv.newVariable("varEnv",
                LexicalEnvironment.class).uncheckedCast();
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> lexEnv = mv.newVariable(
                "lexEnv", LexicalEnvironment.class).uncheckedCast();
        Variable<FunctionObject> fo = null;
        Variable<Undefined> undef = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undef);

        getVariableEnvironment(context, varEnv, mv);
        getLexicalEnvironment(context, lexEnv, mv);

        /* step 1 */
        Set<Name> varNames = VarDeclaredNames(evalScript);
        /* step 2 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(evalScript);
        /* step 3 */
        Variable<DeclarativeEnvironmentRecord> lexEnvRec = mv.newVariable("lexEnvRec",
                DeclarativeEnvironmentRecord.class);
        /* step 4 */
        Variable<EnvironmentRecord> varEnvRec = mv
                .newVariable("varEnvRec", EnvironmentRecord.class);
        getEnvironmentRecord(varEnv, varEnvRec, mv);
        /* step 5 (not applicable) */
        /* step 6 */
        ArrayDeque<HoistableDeclaration> functionsToInitialize = new ArrayDeque<>();
        /* step 7 */
        HashSet<Name> declaredFunctionNames = new HashSet<>();
        /* step 8 */
        if (findFunctionDeclarations(varDeclarations, functionsToInitialize, declaredFunctionNames)) {
            fo = mv.newVariable("fo", FunctionObject.class);
        }
        /* step 9 */
        LinkedHashSet<Name> declaredVarNames = new LinkedHashSet<>(varNames);
        /* step 10 */
        declaredVarNames.removeAll(declaredFunctionNames);
        /* step 11 (note) */
        /* step 12 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(evalScript);
        /* step 13 */
        if (!lexDeclarations.isEmpty()) {
            getEnvironmentRecord(lexEnv, lexEnvRec, mv);
            createLexicalDeclarations(lexDeclarations, lexEnvRec, mv);
        }
        /* step 14 */
        createFunctions(functionsToInitialize, strict, context, lexEnv, fo, varEnvRec, mv);
        /* step 15 */
        createVarDeclarations(declaredVarNames, varEnvRec, undef, mv);
        /* step 16 */
        mv._return();
    }

    private void generateStrict(Script evalScript, InstructionVisitor mv) {
        assert evalScript.isStrict() && !evalScript.isScripting();
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> varEnv = mv.newVariable(
                "varEnv", LexicalEnvironment.class).uncheckedCast();
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> lexEnv = mv.newVariable(
                "lexEnv", LexicalEnvironment.class).uncheckedCast();
        Variable<FunctionObject> fo = null;
        Variable<Undefined> undef = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undef);

        getVariableEnvironment(context, varEnv, mv);
        getLexicalEnvironment(context, lexEnv, mv);

        /* step 1 */
        Set<Name> varNames = VarDeclaredNames(evalScript);
        /* step 2 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(evalScript);
        /* step 3 */
        Variable<DeclarativeEnvironmentRecord> lexEnvRec = mv.newVariable("lexEnvRec",
                DeclarativeEnvironmentRecord.class);
        getEnvironmentRecord(lexEnv, lexEnvRec, mv);
        /* step 4 */
        Variable<DeclarativeEnvironmentRecord> varEnvRec = mv.newVariable("varEnvRec",
                DeclarativeEnvironmentRecord.class);
        getEnvironmentRecord(varEnv, varEnvRec, mv);
        /* step 5 (not applicable) */
        /* step 6 */
        ArrayDeque<HoistableDeclaration> functionsToInitialize = new ArrayDeque<>();
        /* step 7 */
        HashSet<Name> declaredFunctionNames = new HashSet<>();
        /* step 8 */
        if (findFunctionDeclarations(varDeclarations, functionsToInitialize, declaredFunctionNames)) {
            fo = mv.newVariable("fo", FunctionObject.class);
        }
        /* step 9 */
        LinkedHashSet<Name> declaredVarNames = new LinkedHashSet<>(varNames);
        /* step 10 */
        declaredVarNames.removeAll(declaredFunctionNames);
        /* step 11 (note) */
        /* step 12 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(evalScript);
        /* step 13 */
        createLexicalDeclarations(lexDeclarations, lexEnvRec, mv);
        /* step 14 */
        for (HoistableDeclaration f : functionsToInitialize) {
            Name fn = BoundName(f);

            // stack: [] -> []
            InstantiateFunctionObject(context, lexEnv, f, mv);
            mv.store(fo);

            // Early error semantics ensure that fn does not already exist in varEnvRec.
            BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(varEnvRec, fn);
            op.createMutableBinding(varEnvRec, fn, true, mv);
            op.initializeBinding(varEnvRec, fn, fo, mv);
        }
        /* step 15 */
        for (Name vn : declaredVarNames) {
            // Early error semantics ensure that vn does not already exist in varEnvRec.
            BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(varEnvRec, vn);
            op.createMutableBinding(varEnvRec, vn, true, mv);
            op.initializeBinding(varEnvRec, vn, undef, mv);
        }
        /* step 16 */
        mv._return();
    }

    /**
     * 18.2.1.2, step 8
     */
    private boolean findFunctionDeclarations(List<StatementListItem> varDeclarations,
            ArrayDeque<HoistableDeclaration> functionsToInitialize,
            HashSet<Name> declaredFunctionNames) {
        for (StatementListItem item : reverse(varDeclarations)) {
            if (item instanceof HoistableDeclaration) {
                HoistableDeclaration d = (HoistableDeclaration) item;
                Name fn = BoundName(d);
                if (declaredFunctionNames.add(fn)) {
                    functionsToInitialize.addFirst(d);
                }
            }
        }
        return !functionsToInitialize.isEmpty();
    }

    /**
     * 18.2.1.2, step 13
     */
    private void createLexicalDeclarations(List<Declaration> lexDeclarations,
            Variable<DeclarativeEnvironmentRecord> lexEnvRec, InstructionVisitor mv) {
        for (Declaration d : lexDeclarations) {
            assert !(d instanceof HoistableDeclaration);
            for (Name dn : BoundNames(d)) {
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(lexEnvRec, dn);
                if (d.isConstDeclaration()) {
                    op.createImmutableBinding(lexEnvRec, dn, true, mv);
                } else {
                    op.createMutableBinding(lexEnvRec, dn, false, mv);
                }
            }
        }
    }

    /**
     * 18.2.1.2, step 14
     */
    private void createFunctions(ArrayDeque<HoistableDeclaration> functionsToInitialize,
            boolean strict, Variable<ExecutionContext> context,
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> lexEnv,
            Variable<FunctionObject> fo, Variable<? extends EnvironmentRecord> varEnvRec,
            InstructionVisitor mv) {
        for (HoistableDeclaration f : functionsToInitialize) {
            Name fn = BoundName(f);

            // stack: [] -> []
            InstantiateFunctionObject(context, lexEnv, f, mv);
            mv.store(fo);

            BindingOp<EnvironmentRecord> op = BindingOp.LOOKUP;
            Jump funcAlreadyDeclared = new Jump(), after = new Jump();

            op.hasBinding(varEnvRec, fn, mv);
            mv.ifne(funcAlreadyDeclared);
            {
                op.createMutableBinding(varEnvRec, fn, true, mv);
                op.initializeBinding(varEnvRec, fn, fo, mv);
                mv.goTo(after);
            }
            mv.mark(funcAlreadyDeclared);
            {
                op.setMutableBinding(varEnvRec, fn, fo, strict, mv);
            }
            mv.mark(after);
        }
    }

    /**
     * 18.2.1.2, step 15
     */
    private void createVarDeclarations(LinkedHashSet<Name> declaredVarNames,
            Variable<? extends EnvironmentRecord> varEnvRec, Variable<Undefined> undef,
            InstructionVisitor mv) {
        for (Name vn : declaredVarNames) {
            BindingOp<EnvironmentRecord> op = BindingOp.LOOKUP;
            Jump varAlreadyDeclared = new Jump();

            op.hasBinding(varEnvRec, vn, mv);
            mv.ifne(varAlreadyDeclared);
            {
                op.createMutableBinding(varEnvRec, vn, true, mv);
                op.initializeBinding(varEnvRec, vn, undef, mv);
            }
            mv.mark(varAlreadyDeclared);
        }
    }

    private boolean isEnclosedByLexicalOrHasVarForOf(Script evalScript) {
        return codegen.isEnabled(Parser.Option.EnclosedByLexicalDeclaration)
                || (codegen.isEnabled(CompatibilityOption.CatchVarStatement) && !evalScript
                        .getScope().varForOfDeclaredNames().isEmpty());
    }

    /**
     * 18.2.1.2, steps 5.b-d
     */
    private void checkLexicalRedeclaration(Script evalScript, Variable<ExecutionContext> context,
            Variable<? extends LexicalEnvironment<? extends EnvironmentRecord>> varEnv,
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> lexEnv, Set<Name> varNames,
            InstructionVisitor mv) {
        Variable<LexicalEnvironment<EnvironmentRecord>> thisLex = mv.newVariable("thisLex",
                LexicalEnvironment.class).uncheckedCast();
        Variable<EnvironmentRecord> thisEnvRec = mv.newVariable("thisEnvRec",
                EnvironmentRecord.class).uncheckedCast();
        Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec",
                DeclarativeEnvironmentRecord.class).uncheckedCast();
        Set<Name> varForOfNames = evalScript.getScope().varForOfDeclaredNames();
        final boolean catchVar = codegen.isEnabled(CompatibilityOption.CatchVarStatement);
        final boolean hasWith = codegen.isEnabled(Parser.Option.EnclosedByWithStatement);

        Jump loopTest = new Jump(), loop = new Jump(), objectEnv = new Jump();
        mv.load(lexEnv);
        if (hasLexicalEnvironment(evalScript)) {
            // Don't need to check own lexical environment.
            mv.invoke(Methods.LexicalEnvironment_getOuter);
        }
        mv.store(thisLex);
        mv.nonDestructiveGoTo(loopTest);
        {
            mv.mark(loop);
            getEnvironmentRecord(thisLex, thisEnvRec, mv);
            if (hasWith) {
                mv.load(thisEnvRec);
                mv.instanceOf(Types.ObjectEnvironmentRecord);
                mv.ifne(objectEnv);
            }
            mv.load(thisEnvRec);
            mv.checkcast(Types.DeclarativeEnvironmentRecord);
            mv.store(envRec);
            for (Name name : varNames) {
                mv.load(context);
                mv.load(envRec);
                mv.aconst(name.getIdentifier());
                mv.iconst(catchVar && !varForOfNames.contains(name));
                mv.invoke(Methods.ScriptRuntime_canDeclareVarOrThrow);
            }
            if (hasWith) {
                mv.mark(objectEnv);
            }
            mv.load(thisLex);
            mv.invoke(Methods.LexicalEnvironment_getOuter);
            mv.store(thisLex);
        }
        mv.mark(loopTest);
        mv.load(thisLex);
        mv.load(varEnv);
        mv.ifacmpne(loop);
    }

    private boolean hasLexicalEnvironment(Script evalScript) {
        assert !evalScript.isStrict() && !evalScript.isScripting();
        return !evalScript.getScope().lexicallyDeclaredNames().isEmpty();
    }
}
