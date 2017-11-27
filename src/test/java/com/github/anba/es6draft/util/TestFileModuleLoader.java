/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.io.IOException;
import java.util.Collection;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.modules.loader.FileModuleLoader;

/**
 *
 */
public class TestFileModuleLoader extends FileModuleLoader implements TestModuleLoader<SourceTextModuleRecord> {
    public TestFileModuleLoader(RuntimeContext context, ScriptLoader scriptLoader) {
        super(context, scriptLoader);
    }

    @Override
    public void defineFromTemplate(ModuleRecord template, Realm realm) {
        if (!(template instanceof SourceTextModuleRecord)) {
            throw new IllegalArgumentException();
        }
        SourceTextModuleRecord module = ((SourceTextModuleRecord) template).clone();
        defineModule(module);
        linkModule(module, realm);
    }

    @Override
    public SourceTextModuleRecord defineUnlinked(SourceIdentifier sourceId, ModuleSource moduleSource)
            throws IOException {
        SourceTextModuleRecord module = parseModule(sourceId, moduleSource);
        defineModule(module);
        return module;
    }

    @Override
    public Collection<SourceTextModuleRecord> getModules() {
        return super.getModules();
    }
}
