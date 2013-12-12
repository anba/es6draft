/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.ExecutionContext.newModuleExecutionContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.objects.modules.LoaderObject;

/**
 * <h1>1 Modules: Semantics</h1><br>
 * <h2>1.3 Module Evaluation</h2>
 */
public final class ModuleEvaluation {
    private ModuleEvaluation() {
    }

    /**
     * 1.3.1 EnsureEvaluated(mod, seen, loader) Abstract Operation
     */
    public static void EnsureEvaluated(ExecutionContext cx, ModuleObject mod, LoaderObject loader) {
        EnsureEvaluated(cx, mod, new HashSet<ModuleObject>(), loader);
    }

    /**
     * 1.3.1 EnsureEvaluated(mod, seen, loader) Abstract Operation
     */
    public static void EnsureEvaluated(ExecutionContext cx, ModuleObject mod,
            Set<ModuleObject> seen, LoaderObject loader) {
        /* step 1 */
        seen.add(mod);
        /* step 2 */
        Map<String, ModuleObject> deps = mod.getDependencies();
        /* step 3 */
        for (Map.Entry<String, ModuleObject> pair : deps.entrySet()) {
            ModuleObject dep = pair.getValue();
            if (!seen.contains(dep)) {
                EnsureEvaluated(cx, dep, seen, loader);
            }
        }
        /* step 4 */
        if (mod.getBody() != null && !mod.isEvaluated()) {
            /* step 4a */
            mod.setEvaluated(true);
            /* steps 4b-4e */
            ExecutionContext initContext = newModuleExecutionContext(loader.getRealm(),
                    mod.getEnvironment());
            /* steps 4f-4k */
            evaluateModule(initContext, mod);
        }
    }

    private static void evaluateModule(ExecutionContext cx, ModuleObject module) {
        // TODO: implement
    }
}
