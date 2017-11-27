/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import java.io.IOException;
import java.util.function.Function;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.modules.loader.FileSourceIdentifier;
import com.github.anba.es6draft.util.TestFileModuleLoader;

/**
 * 
 */
final class MozJitFileModuleLoader extends TestFileModuleLoader {
    private Function<String, SourceTextModuleRecord> moduleResolveHook;

    public MozJitFileModuleLoader(RuntimeContext context, ScriptLoader scriptLoader) {
        super(context, scriptLoader);
    }

    public void setModuleResolveHook(Function<String, SourceTextModuleRecord> resolveHook) {
        this.moduleResolveHook = resolveHook;
    }

    @Override
    public FileSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException {
        if (moduleResolveHook == null && unnormalizedName.indexOf('/') < 0) {
            return new FileSourceIdentifier(getBaseDirectory().resolve("modules").resolve(unnormalizedName));
        }
        return super.normalizeName(unnormalizedName, referrerId);
    }

    @Override
    public SourceTextModuleRecord resolve(SourceIdentifier identifier, Realm realm) throws IOException {
        if (moduleResolveHook != null) {
            String moduleName = getBaseDirectory().toUri().relativize(identifier.toUri()).toString();
            SourceTextModuleRecord module = moduleResolveHook.apply(moduleName);
            linkModule(module, realm);
            return module;
        }
        return super.resolve(identifier, realm);
    }
}
