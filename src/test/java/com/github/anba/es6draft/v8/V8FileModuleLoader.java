/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.v8;

import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.FileSourceIdentifier;
import com.github.anba.es6draft.util.TestFileModuleLoader;

/**
 * 
 */
final class V8FileModuleLoader extends TestFileModuleLoader {
    public V8FileModuleLoader(RuntimeContext context, ScriptLoader scriptLoader) {
        super(context, scriptLoader);
    }

    @Override
    public FileSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException {
        if (!unnormalizedName.startsWith("./") && !unnormalizedName.startsWith("../")) {
            unnormalizedName = "./" + unnormalizedName;
        }
        return super.normalizeName(unnormalizedName, referrerId);
    }
}
