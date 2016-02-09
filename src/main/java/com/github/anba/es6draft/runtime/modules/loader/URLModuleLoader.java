/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules.loader;

import static com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord.ParseModule;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;

/**
 * 
 */
public class URLModuleLoader extends AbstractURLModuleLoader<SourceTextModuleRecord> {
    private final ScriptLoader scriptLoader;
    private final HashMap<SourceIdentifier, SourceTextModuleRecord> modules = new HashMap<>();

    public URLModuleLoader(RuntimeContext context, ScriptLoader scriptLoader) {
        super(context);
        this.scriptLoader = scriptLoader;
    }

    @Override
    protected void defineModule(SourceTextModuleRecord module) {
        SourceIdentifier identifier = module.getSourceCodeId();
        if (modules.containsKey(identifier)) {
            throw new IllegalArgumentException();
        }
        modules.put(identifier, module);
    }

    @Override
    protected SourceTextModuleRecord getModule(SourceIdentifier identifier) {
        return modules.get(identifier);
    }

    @Override
    protected SourceTextModuleRecord parseModule(SourceIdentifier identifier, ModuleSource source) throws IOException {
        return ParseModule(scriptLoader, identifier, source);
    }

    @Override
    protected void linkModule(SourceTextModuleRecord module, Realm realm) {
        if (module.getRealm() == null) {
            module.setRealm(realm);
        }
    }

    @Override
    protected Set<String> getRequestedModules(SourceTextModuleRecord module) {
        return module.getRequestedModules();
    }

    @Override
    protected Collection<SourceTextModuleRecord> getModules() {
        return Collections.unmodifiableCollection(modules.values());
    }
}
