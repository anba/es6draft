/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.util.Collection;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
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
    public Collection<SourceTextModuleRecord> getModules() {
        return super.getModules();
    }
}
