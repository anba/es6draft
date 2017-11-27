/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.loader;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.internal.Properties.createFunction;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.GetModuleNamespace;

import java.io.IOException;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.types.Callable;

/**
 *
 */
final class NodeFunctions {
    private NodeFunctions() {
    }

    /**
     * Creates a new {@code require()} function bound to the module record.
     * 
     * @param module
     *            the module record
     * @return the require function
     */
    public static Callable createRequireFunction(ModuleRecord module) {
        ExecutionContext cx = module.getRealm().defaultContext();
        SourceIdentifier sourceId = module.getSourceCodeId();
        Callable requireFn = createFunction(cx, new RequireFunction(sourceId), RequireFunction.class);
        Callable resolveFn = createFunction(cx, new ResolveFunction(sourceId), ResolveFunction.class);
        CreateDataProperty(cx, requireFn, "resolve", resolveFn);
        return requireFn;
    }

    public static final class RequireFunction {
        private final SourceIdentifier identifier;

        RequireFunction(SourceIdentifier identifier) {
            this.identifier = identifier;
        }

        @Function(name = "require", arity = 1)
        public Object require(ExecutionContext cx, String moduleName)
                throws IOException, ResolutionException, MalformedNameException {
            Realm realm = cx.getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();
            SourceIdentifier normalizedName = moduleLoader.normalizeName(moduleName, identifier);
            ModuleRecord module = moduleLoader.resolve(normalizedName, realm);
            module.instantiate();
            module.evaluate();
            if (module instanceof NodeModuleRecord) {
                return ((NodeModuleRecord) module).getModuleExports();
            }
            return GetModuleNamespace(cx, module);
        }
    }

    public static final class ResolveFunction {
        private final SourceIdentifier identifier;

        ResolveFunction(SourceIdentifier identifier) {
            this.identifier = identifier;
        }

        @Function(name = "resolve", arity = 1)
        public Object resolve(ExecutionContext cx, String moduleName) throws MalformedNameException {
            return cx.getRealm().getModuleLoader().normalizeName(moduleName, identifier).toString();
        }
    }
}
