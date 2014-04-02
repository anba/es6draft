/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.ExecutionContext.newModuleExecutionContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.objects.modules.ModuleObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>15 ECMAScript Language: Modules and Scripts</h1><br>
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
            super(realm, ANONYMOUS, 0);
            this.loader = loader;
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
     */
    public static void EnsureEvaluated(ExecutionContext cx, ModuleLinkage mod, Loader loader) {
        EnsureEvaluated(cx, mod, new HashSet<ModuleLinkage>(), loader);
    }

    /**
     * 15.2.6.2 EnsureEvaluated(mod, seen, loader) Abstract Operation
     */
    public static void EnsureEvaluated(ExecutionContext cx, ModuleLinkage mod,
            Set<ModuleLinkage> seen, Loader loader) {
        /* step 1 */
        seen.add(mod);
        /* step 2 */
        Map<String, ModuleLinkage> deps = mod.getDependencies();
        /* step 3 */
        for (Map.Entry<String, ModuleLinkage> pair : deps.entrySet()) {
            ModuleLinkage dep = pair.getValue();
            if (!seen.contains(dep)) {
                EnsureEvaluated(cx, dep, seen, loader);
            }
        }
        /* step 4 */
        if (mod.isEvaluated()) {
            return;
        }
        /* step 5 */
        mod.setEvaluated(true);
        /* step 6 */
        if (mod.getBody() == null) {
            return;
        }
        /* step 7 */
        ModuleDeclarationInstantiation(mod.getBody(), mod.getEnvironment());
        /* steps 8-11 */
        ExecutionContext initContext = newModuleExecutionContext(loader.getRealm(),
                mod.getEnvironment());
        /* steps 12-17 */
        evaluateModule(initContext, mod);
    }

    /**
     * 15.2.0.15 Runtime Semantics: ModuleDeclarationInstantiation
     */
    private static void ModuleDeclarationInstantiation(Module body,
            LexicalEnvironment<DeclarativeEnvironmentRecord> env) {
        // TODO: implement
    }

    private static void evaluateModule(ExecutionContext cx, ModuleLinkage module) {
        // TODO: implement
    }
}
