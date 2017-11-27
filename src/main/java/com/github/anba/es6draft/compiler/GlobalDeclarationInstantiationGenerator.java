/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.HoistableDeclaration;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.VariableDeclaration;
import com.github.anba.es6draft.ast.VariableStatement;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.1 Scripts</h2>
 * <ul>
 * <li>15.1.11 Runtime Semantics: GlobalDeclarationInstantiation (script, env)
 * </ul>
 */
final class GlobalDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static final class GlobalDeclInitVisitor extends InstructionVisitor {
        GlobalDeclInitVisitor(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }

        Variable<ExecutionContext> getExecutionContext() {
            return getParameter(0, ExecutionContext.class);
        }
    }

    GlobalDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(Script script, MethodCode method) {
        GlobalDeclInitVisitor mv = new GlobalDeclInitVisitor(method);
        mv.lineInfo(script);
        mv.begin();
        if (VarDeclaredNames(script).isEmpty() && LexicallyDeclaredNames(script).isEmpty()
                && !hasBlockFunctions(script)) {
            mv._return();
        } else {
            generate(script, mv);
        }
        mv.end();
    }

    private void generate(Script script, GlobalDeclInitVisitor mv) {
        Variable<ExecutionContext> context = mv.getExecutionContext();
        Variable<LexicalEnvironment<GlobalEnvironmentRecord>> env = mv
                .newVariable("globalEnv", LexicalEnvironment.class).uncheckedCast();
        Variable<GlobalEnvironmentRecord> envRec = mv.newVariable("envRec", GlobalEnvironmentRecord.class);
        Variable<FunctionObject> fo = null;

        /* steps 1-2 */
        getLexicalEnvironment(context, env, mv);
        getEnvironmentRecord(env, envRec, mv);
        /* step 3 */
        HashSet<Name> lexNames = new HashSet<>();
        /* step 4 */
        HashSet<Name> varNames = new HashSet<>();
        /* step 5 */
        // Iterate over declarations to be able to emit line-info entries.
        for (Declaration d : LexicallyScopedDeclarations(script)) {
            assert !(d instanceof HoistableDeclaration);
            for (Name name : BoundNames(d)) {
                if (lexNames.add(name)) {
                    canDeclareLexicalScopedOrThrow(context, envRec, d, name, mv);
                }
            }
        }
        /* step 6 */
        // Iterate over declarations to be able to emit line-info entries.
        for (StatementListItem item : VarScopedDeclarations(script)) {
            if (item instanceof VariableStatement) {
                for (VariableDeclaration vd : ((VariableStatement) item).getElements()) {
                    for (Name name : BoundNames(vd)) {
                        if (varNames.add(name)) {
                            canDeclareVarScopedOrThrow(context, envRec, vd, name, mv);
                        }
                    }
                }
            } else {
                HoistableDeclaration d = (HoistableDeclaration) item;
                Name name = BoundName(d);
                if (varNames.add(name)) {
                    canDeclareVarScopedOrThrow(context, envRec, d, name, mv);
                }
            }
        }
        /* step 7 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(script);
        /* step 8 */
        ArrayDeque<HoistableDeclaration> functionsToInitialize = new ArrayDeque<>();
        /* step 9 */
        HashSet<Name> declaredFunctionNames = new HashSet<>();
        /* step 10 */
        for (StatementListItem item : reverse(varDeclarations)) {
            if (item instanceof HoistableDeclaration) {
                HoistableDeclaration d = (HoistableDeclaration) item;
                Name fn = BoundName(d);
                if (declaredFunctionNames.add(fn)) {
                    canDeclareGlobalFunctionOrThrow(context, envRec, d, fn, mv);
                    functionsToInitialize.addFirst(d);
                }
            }
        }
        if (!functionsToInitialize.isEmpty()) {
            fo = mv.newVariable("fo", FunctionObject.class);
        }
        /* step 11 */
        LinkedHashMap<Name, VariableDeclaration> declaredVarNames = new LinkedHashMap<>();
        /* step 12 */
        for (StatementListItem d : varDeclarations) {
            if (d instanceof VariableStatement) {
                for (VariableDeclaration vd : ((VariableStatement) d).getElements()) {
                    for (Name vn : BoundNames(vd)) {
                        if (!declaredFunctionNames.contains(vn)) {
                            canDeclareGlobalVarOrThrow(context, envRec, vd, vn, mv);
                            declaredVarNames.put(vn, vd);
                        }
                    }
                }
            }
        }
        /* step 13 (note) */
        /* step 14 */
        // ES2016: Block-scoped global function declarations
        if (hasBlockFunctions(script)) {
            int idCounter = 0;
            HashSet<Name> declaredFunctionOrVarNames = new HashSet<>();
            declaredFunctionOrVarNames.addAll(declaredFunctionNames);
            declaredFunctionOrVarNames.addAll(declaredVarNames.keySet());
            for (FunctionDeclaration f : script.getScope().blockFunctions()) {
                Name fn = BoundName(f);
                Jump next = new Jump();

                // Runtime check always required for global block-level function declarations.
                f.setLegacyBlockScopeId(++idCounter);
                // FIXME: spec issue - avoid (observable!) duplicate checks for same name?
                // FIXME: spec issue - property creation order important?
                canDeclareGlobalFunction(envRec, f, fn, next, mv);
                setLegacyBlockFunction(context, f, mv);
                if (declaredFunctionOrVarNames.add(fn)) {
                    createGlobalFunctionBinding(envRec, f, fn, false, mv);
                }
                mv.mark(next);
            }
        }
        /* step 15 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(script);
        /* step 16 */
        for (Declaration d : lexDeclarations) {
            assert !(d instanceof HoistableDeclaration);
            mv.lineInfo(d);
            for (Name dn : BoundNames(d)) {
                BindingOp<GlobalEnvironmentRecord> op = BindingOp.of(envRec, dn);
                if (d.isConstDeclaration()) {
                    op.createImmutableBinding(envRec, dn, true, mv);
                } else {
                    op.createMutableBinding(envRec, dn, false, mv);
                }
            }
        }
        /* step 17 */
        for (HoistableDeclaration f : functionsToInitialize) {
            Name fn = BoundName(f);
            InstantiateFunctionObject(context, env, f, mv);
            mv.store(fo);
            createGlobalFunctionBinding(envRec, f, fn, fo, false, mv);
        }
        /* step 18 */
        for (Map.Entry<Name, VariableDeclaration> e : declaredVarNames.entrySet()) {
            createGlobalVarBinding(envRec, e.getValue(), e.getKey(), false, mv);
        }
        /* step 19 */
        mv._return();
    }

    private static boolean hasBlockFunctions(Script script) {
        return !script.getScope().blockFunctions().isEmpty();
    }
}
