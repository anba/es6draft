/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.ExecutionContext.newModuleExecutionContext;
import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsConstantDeclaration;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicallyScopedDeclarations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.objects.reflect.ModuleObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2>
 * <ul>
 * <li>15.2.6 Runtime Semantics: Module Evaluation
 * </ul>
 */
public final class ModuleEvaluation {
    private ModuleEvaluation() {
    }

    /**
     * 15.2.6.1 EvaluateLoadedModule(load) Functions
     */
    public static final class EvaluateLoadedModule extends BuiltinFunction {
        /** [[Loader]] */
        private final Loader loader;

        public EvaluateLoadedModule(Realm realm, Loader loader) {
            this(realm, loader, null);
            createDefaultFunctionProperties(ANONYMOUS, 0);
        }

        private EvaluateLoadedModule(Realm realm, Loader loader, Void ignore) {
            super(realm, ANONYMOUS);
            this.loader = loader;
        }

        @Override
        public EvaluateLoadedModule clone() {
            return new EvaluateLoadedModule(getRealm(), loader, null);
        }

        @Override
        public ModuleObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object loadArg = args.length > 0 ? args[0] : null;
            assert loadArg instanceof Load;
            Load load = (Load) loadArg;
            /* step 1 */
            Loader loader = this.loader;
            /* step 2 */
            assert load.getStatus() == Load.Status.Linked;
            /* step 3 */
            ModuleLinkage module = load.getModule();
            /* steps 4-5 */
            EnsureEvaluated(calleeContext, module, loader);
            /* step 6 */
            return module.getModuleObject();
        }
    }

    /**
     * 15.2.6.2 EnsureEvaluated(mod, seen, loader) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param mod
     *            the module linkage record
     * @param loader
     *            the loader record
     */
    public static void EnsureEvaluated(ExecutionContext cx, ModuleLinkage mod, Loader loader) {
        EnsureEvaluated(cx, mod, new HashSet<ModuleLinkage>(), loader);
    }

    /**
     * 15.2.6.2 EnsureEvaluated(mod, seen, loader) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param mod
     *            the module linkage record
     * @param seen
     *            the set of already evaluated modules
     * @param loader
     *            the loader record
     */
    public static void EnsureEvaluated(ExecutionContext cx, ModuleLinkage mod,
            Set<ModuleLinkage> seen, Loader loader) {
        /* step 1 */
        if (mod.isEvaluated()) {
            return;
        }
        /* step 2 */
        seen.add(mod);
        /* step 3 */
        CreateModuleEnvironment(mod);
        /* step 4 */
        Map<String, ModuleLinkage> deps = mod.getDependencies();
        /* step 5 */
        for (Map.Entry<String, ModuleLinkage> pair : deps.entrySet()) {
            ModuleLinkage dep = pair.getValue();
            if (!seen.contains(dep)) {
                EnsureEvaluated(cx, dep, seen, loader);
            }
        }
        /* step 6 */
        if (mod.isEvaluated()) {
            return;
        }
        /* step 7 */
        mod.setEvaluated(true);
        /* step 8 */
        if (mod.getBody() == null) {
            return;
        }
        /* step 9 */
        ModuleDeclarationInstantiation(mod.getBody(), mod.getEnvironment());
        /* steps 10-13 */
        ExecutionContext initContext = newModuleExecutionContext(loader.getRealm(),
                mod.getEnvironment());
        /* steps 14-19 */
        evaluateModule(initContext, mod);
    }

    private static void CreateModuleEnvironment(ModuleLinkage mod) {
        // TODO: implement
    }

    /**
     * 15.2.0.15 Runtime Semantics: ModuleDeclarationInstantiation
     * 
     * @param body
     *            the parsed module node
     * @param env
     *            the module lexical environment
     */
    private static void ModuleDeclarationInstantiation(Module body,
            LexicalEnvironment<DeclarativeEnvironmentRecord> env) {
        // TODO: implement
        DeclarativeEnvironmentRecord envRec = env.getEnvRec();
        /* step 1 */
        List<Declaration> declarations = LexicallyScopedDeclarations(body);
        /* step 2 */
        List<FunctionNode> functionsToInitialize = new ArrayList<>();
        /* step 3 */
        for (Declaration d : declarations) {
            for (String dn : BoundNames(d)) {
                if (IsConstantDeclaration(d)) {
                    envRec.createImmutableBinding(dn);
                } else {
                    envRec.createMutableBinding(dn, false);
                }
            }
            if (d instanceof FunctionNode) {
                functionsToInitialize.add((FunctionNode) d);
            }
        }
        /* step 4 */
        for (FunctionNode f : functionsToInitialize) {
            String fn = f.getFunctionName();
            Object fo = InstantiateFunctionObject(f, env);
            envRec.initializeBinding(fn, fo);
        }
    }

    private static Object InstantiateFunctionObject(FunctionNode f,
            LexicalEnvironment<DeclarativeEnvironmentRecord> env) {
        // TODO: implement
        return null;
    }

    private static void evaluateModule(ExecutionContext cx, ModuleLinkage module) {
        // TODO: implement
    }
}
