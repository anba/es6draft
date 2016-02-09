/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.util.Collection;

import com.github.anba.es6draft.repl.loader.NodeModuleLoader;
import com.github.anba.es6draft.repl.loader.NodeSourceTextModuleRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;

/**
 * 
 */
public class TestNodeModuleLoader extends NodeModuleLoader implements TestModuleLoader<ModuleRecord> {
    public TestNodeModuleLoader(RuntimeContext context, ScriptLoader scriptLoader) {
        super(context, scriptLoader);
    }

    @Override
    public void defineFromTemplate(ModuleRecord template, Realm realm) {
        if (!(template instanceof NodeSourceTextModuleRecord)) {
            throw new IllegalArgumentException();
        }
        NodeSourceTextModuleRecord module = ((NodeSourceTextModuleRecord) template).clone();
        defineModule(module);
        linkModule(module, realm);
    }

    @Override
    public Collection<ModuleRecord> getModules() {
        return super.getModules();
    }
}
