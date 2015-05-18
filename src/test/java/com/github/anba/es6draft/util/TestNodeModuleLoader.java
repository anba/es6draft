/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collection;

import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.repl.loader.NodeModuleLoader;
import com.github.anba.es6draft.repl.loader.NodeSourceTextModuleRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.types.Constructor;

/**
 * 
 */
public class TestNodeModuleLoader extends NodeModuleLoader implements
        TestModuleLoader<ModuleRecord> {
    public TestNodeModuleLoader(ScriptLoader scriptLoader, Path baseDirectory) {
        super(scriptLoader, baseDirectory);
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

    public void initialize(ShellGlobalObject global) throws IOException, URISyntaxException,
            MalformedNameException, ResolutionException {
        ModuleRecord module = global.loadNativeModule("module.jsm");
        Constructor moduleConstructor = global
                .getModuleExport(module, "default", Constructor.class);
        setModuleConstructor(moduleConstructor);
    }
}
