/**
 * Copyright (c) 2012-2016 André Bargull
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
import com.github.anba.es6draft.compiler.CodeGenerator.ModuleName;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.modules.ExportEntry;
import com.github.anba.es6draft.runtime.modules.ImportEntry;
import com.github.anba.es6draft.runtime.modules.ModuleExport;
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
 * <li>15.2.1.15.4 ModuleDeclarationInstantiation( ) Concrete Method
 * </ul>
 */
final class ModuleDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: ScriptRuntime
        static final MethodName ScriptRuntime_createImportBinding = MethodName.findStatic(
                Types.ScriptRuntime, "createImportBinding", Type.methodType(Type.VOID_TYPE,
                        Types.ExecutionContext, Types.ModuleEnvironmentRecord, Types.String,
                        Types.ModuleExport));

        static final MethodName ScriptRuntime_getModuleNamespace = MethodName.findStatic(
                Types.ScriptRuntime, "getModuleNamespace", Type.methodType(Types.ScriptObject,
                        Types.ExecutionContext, Types.SourceTextModuleRecord, Types.String));

        static final MethodName ScriptRuntime_resolveExportOrThrow = MethodName.findStatic(
                Types.ScriptRuntime, "resolveExportOrThrow",
                Type.methodType(Type.VOID_TYPE, Types.SourceTextModuleRecord, Types.String));

        static final MethodName ScriptRuntime_resolveImportOrThrow = MethodName.findStatic(
                Types.ScriptRuntime, "resolveImportOrThrow", Type.methodType(Types.ModuleExport,
                        Types.SourceTextModuleRecord, Types.String, Types.String));
    }

    private static final int EXECUTION_CONTEXT = 0;
    private static final int MODULE = 1;
    private static final int MODULE_ENV = 2;

    private static final class ModuleDeclInitMethodGenerator extends InstructionVisitor {
        ModuleDeclInitMethodGenerator(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("module", MODULE, Types.SourceTextModuleRecord);
            setParameterName("moduleEnv", MODULE_ENV, Types.LexicalEnvironment);
        }
    }

    ModuleDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(Module module, SourceTextModuleRecord moduleRecord) {
        MethodCode method = codegen.newMethod(module, ModuleName.Init);
        InstructionVisitor mv = new ModuleDeclInitMethodGenerator(method);

        mv.lineInfo(module);
        mv.begin();
        generate(module, moduleRecord, mv);
        mv.end();
    }

    private void generate(Module module, SourceTextModuleRecord moduleRecord, InstructionVisitor mv) {
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<SourceTextModuleRecord> moduleRec = mv.getParameter(MODULE,
                SourceTextModuleRecord.class);
        Variable<LexicalEnvironment<ModuleEnvironmentRecord>> env = mv.getParameter(MODULE_ENV,
                LexicalEnvironment.class).uncheckedCast();

        Variable<ModuleEnvironmentRecord> envRec = mv.newVariable("envRec",
                ModuleEnvironmentRecord.class);
        getEnvironmentRecord(env, envRec, mv);

        Variable<ModuleExport> resolved = mv.newVariable("resolved", ModuleExport.class);
        Variable<ScriptObject> namespace = null;
        Variable<FunctionObject> fo = null;

        Variable<Undefined> undef = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undef);

        /* steps 1-8 (not applicable) */
        /* step 9 */
        for (ExportEntry exportEntry : moduleRecord.getIndirectExportEntries()) {
            mv.lineInfo(exportEntry.getLine());
            mv.load(moduleRec);
            mv.aconst(exportEntry.getExportName());
            mv.invoke(Methods.ScriptRuntime_resolveExportOrThrow);
        }
        /* steps 10-11 (not applicable) */
        /* step 12 */
        for (ImportEntry importEntry : moduleRecord.getImportEntries()) {
            mv.lineInfo(importEntry.getLine());
            if (importEntry.isStarImport()) {
                Name localName = new Name(importEntry.getLocalName());
                BindingOp<ModuleEnvironmentRecord> op = BindingOp.of(envRec, localName);
                op.createImmutableBinding(envRec, localName, true, mv);

                mv.load(context);
                mv.load(moduleRec);
                mv.aconst(importEntry.getModuleRequest());
                mv.invoke(Methods.ScriptRuntime_getModuleNamespace);
                if (namespace == null) {
                    namespace = mv.newVariable("namespace", ScriptObject.class);
                }
                mv.store(namespace);

                op.initializeBinding(envRec, localName, namespace, mv);
            } else {
                mv.load(moduleRec);
                mv.aconst(importEntry.getModuleRequest());
                mv.aconst(importEntry.getImportName());
                mv.invoke(Methods.ScriptRuntime_resolveImportOrThrow);
                mv.store(resolved);

                createImportBinding(context, envRec, importEntry.getLocalName(), resolved, mv);
            }
        }
        /* step 13 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(module);
        HashSet<Name> declaredVarNames = new HashSet<>();
        /* step 14 */
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
        /* step 15 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(module);
        /* step 16 */
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
        /* step 17 */
        mv._return();
    }

    private void createImportBinding(Variable<ExecutionContext> context,
            Variable<ModuleEnvironmentRecord> envRec, String name, Variable<ModuleExport> resolved,
            InstructionVisitor mv) {
        mv.load(context);
        mv.load(envRec);
        mv.aconst(name);
        mv.load(resolved);
        mv.invoke(Methods.ScriptRuntime_createImportBinding);
    }
}
