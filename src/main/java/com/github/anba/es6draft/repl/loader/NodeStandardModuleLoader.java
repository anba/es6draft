/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.loader;

import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.FileModuleLoader;
import com.github.anba.es6draft.runtime.modules.loader.FileSourceIdentifier;

/**
 * 
 */
public class NodeStandardModuleLoader extends FileModuleLoader {
    public NodeStandardModuleLoader(RuntimeContext context, ScriptLoader scriptLoader) {
        super(context, scriptLoader);
    }

    @Override
    public FileSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException {
        FileSourceIdentifier normalizedName = super.normalizeName(unnormalizedName, referrerId);
        return NodeModuleResolution.resolve(getBaseDirectory(), normalizedName, unnormalizedName, referrerId);
    }
}
