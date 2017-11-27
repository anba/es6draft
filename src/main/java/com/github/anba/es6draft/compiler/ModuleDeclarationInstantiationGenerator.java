/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicallyScopedDeclarations;
import static com.github.anba.es6draft.semantics.StaticSemantics.VarScopedDeclarations;

import java.util.HashSet;
import java.util.List;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.HoistableDeclaration;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.VariableStatement;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.modules.ExportEntry;
import com.github.anba.es6draft.runtime.modules.ImportEntry;
import com.github.anba.es6draft.runtime.modules.ResolvedBinding;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2><br>
 * <h3>15.2.1 Module Semantics</h3><br>
 * <h4>15.2.1.15 Source Text Module Records</h4>
 * <ul>
 * <li>15.2.1.16.4.2 ModuleDeclarationEnvironmentSetup( module )
 * </ul>
 */
final class ModuleDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: ModuleOperations
        static final MethodName ModuleOperations_createImportBinding = MethodName.findStatic(Types.ModuleOperations,
                "createImportBinding", Type.methodType(Type.VOID_TYPE, Types.ExecutionContext,
                        Types.ModuleEnvironmentRecord, Types.String, Types.ResolvedBinding));

        static final MethodName ModuleOperations_getModuleNamespace = MethodName.findStatic(Types.ModuleOperations,
                "getModuleNamespace", Type.methodType(Types.ScriptObject, Types.ExecutionContext,
                        Types.SourceTextModuleRecord, Types.String));

        static final MethodName ModuleOperations_resolveExportOrThrow = MethodName.findStatic(Types.ModuleOperations,
                "resolveExportOrThrow", Type.methodType(Type.VOID_TYPE, Types.SourceTextModuleRecord, Types.String));

        static final MethodName ModuleOperations_resolveImportOrThrow = MethodName.findStatic(Types.ModuleOperations,
                "resolveImportOrThrow",
                Type.methodType(Types.ResolvedBinding, Types.SourceTextModuleRecord, Types.String, Types.String));
    }

    private static final class ModuleDeclInitVisitor extends InstructionVisitor {
        ModuleDeclInitVisitor(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("module", 1, Types.SourceTextModuleRecord);
            setParameterName("moduleEnv", 2, Types.LexicalEnvironment);
        }

        Variable<ExecutionContext> getExecutionContext() {
            return getParameter(0, ExecutionContext.class);
        }

        Variable<SourceTextModuleRecord> getModule() {
            return getParameter(1, SourceTextModuleRecord.class);
        }

        Variable<LexicalEnvironment<ModuleEnvironmentRecord>> getModuleEnvironment() {
            return getParameter(2, LexicalEnvironment.class).uncheckedCast();
        }
    }

    ModuleDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(Module module, SourceTextModuleRecord moduleRecord, MethodCode method) {
        ModuleDeclInitVisitor mv = new ModuleDeclInitVisitor(method);
        mv.lineInfo(module);
        mv.begin();
        generate(module, moduleRecord, mv);
        mv.end();
    }

    private void generate(Module module, SourceTextModuleRecord moduleRecord, ModuleDeclInitVisitor mv) {
        Variable<ExecutionContext> context = mv.getExecutionContext();
        Variable<SourceTextModuleRecord> moduleRec = mv.getModule();
        Variable<LexicalEnvironment<ModuleEnvironmentRecord>> env = mv.getModuleEnvironment();

        Variable<ModuleEnvironmentRecord> envRec = mv.newVariable("envRec", ModuleEnvironmentRecord.class);
        getEnvironmentRecord(env, envRec, mv);

        Variable<ResolvedBinding> resolved = mv.newVariable("resolved", ResolvedBinding.class);
        Variable<ScriptObject> namespace = null;
        Variable<FunctionObject> fo = null;

        Variable<Undefined> undef = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undef);

        /* step 1 */
        for (ExportEntry exportEntry : moduleRecord.getIndirectExportEntries()) {
            mv.lineInfo(exportEntry.getLine());
            mv.load(moduleRec);
            mv.aconst(exportEntry.getExportName());
            mv.invoke(Methods.ModuleOperations_resolveExportOrThrow);
        }
        /* step 2 (omitted) */
        /* steps 3-6 (already performed in caller) */
        /* step 7 (not applicable) */
        /* step 8 */
        for (ImportEntry importEntry : moduleRecord.getImportEntries()) {
            mv.lineInfo(importEntry.getLine());
            if (importEntry.isStarImport()) {
                Name localName = new Name(importEntry.getLocalName());
                BindingOp<ModuleEnvironmentRecord> op = BindingOp.of(envRec, localName);
                op.createImmutableBinding(envRec, localName, true, mv);

                mv.load(context);
                mv.load(moduleRec);
                mv.aconst(importEntry.getModuleRequest());
                mv.invoke(Methods.ModuleOperations_getModuleNamespace);
                if (namespace == null) {
                    namespace = mv.newVariable("namespace", ScriptObject.class);
                }
                mv.store(namespace);

                op.initializeBinding(envRec, localName, namespace, mv);
            } else {
                mv.load(moduleRec);
                mv.aconst(importEntry.getModuleRequest());
                mv.aconst(importEntry.getImportName());
                mv.invoke(Methods.ModuleOperations_resolveImportOrThrow);
                mv.store(resolved);

                /* step 8.d.iii */
                createImportBinding(context, envRec, importEntry.getLocalName(), resolved, mv);
            }
        }
        /* step 9 (not applicable) */
        /* step 10 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(module);
        /* step 11 */
        HashSet<Name> declaredVarNames = new HashSet<>();
        /* step 12 */
        for (StatementListItem d : varDeclarations) {
            assert d instanceof VariableStatement;
            for (Name dn : BoundNames((VariableStatement) d)) {
                if (declaredVarNames.add(dn)) {
                    BindingOp<ModuleEnvironmentRecord> op = BindingOp.of(envRec, dn);
                    op.createMutableBinding(envRec, dn, false, mv);
                    op.initializeBinding(envRec, dn, undef, mv);
                }
            }
        }
        /* step 13 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(module);
        /* step 14 */
        for (Declaration d : lexDeclarations) {
            for (Name dn : BoundNames(d)) {
                BindingOp<ModuleEnvironmentRecord> op = BindingOp.of(envRec, dn);
                if (d.isConstDeclaration()) {
                    op.createImmutableBinding(envRec, dn, true, mv);
                } else {
                    op.createMutableBinding(envRec, dn, false, mv);
                }
                if (d instanceof HoistableDeclaration) {
                    InstantiateFunctionObject(context, env, d, mv);
                    if (fo == null) {
                        fo = mv.newVariable("fo", FunctionObject.class);
                    }
                    mv.store(fo);
                    op.initializeBinding(envRec, dn, fo, mv);
                }
            }
        }

        mv._return();
    }

    private void createImportBinding(Variable<ExecutionContext> context, Variable<ModuleEnvironmentRecord> envRec,
            String name, Variable<ResolvedBinding> resolved, InstructionVisitor mv) {
        mv.load(context);
        mv.load(envRec);
        mv.aconst(name);
        mv.load(resolved);
        mv.invoke(Methods.ModuleOperations_createImportBinding);
    }
}
