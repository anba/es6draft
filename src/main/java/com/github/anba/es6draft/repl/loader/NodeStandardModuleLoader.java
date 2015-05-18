/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.loader;

import java.nio.file.Path;

import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.FileModuleLoader;
import com.github.anba.es6draft.runtime.modules.loader.FileSourceIdentifier;

/**
 * 
 */
public class NodeStandardModuleLoader extends FileModuleLoader {
    public NodeStandardModuleLoader(ScriptLoader scriptLoader, Path baseDirectory) {
        super(scriptLoader, baseDirectory);
    }

    @Override
    public FileSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException {
        return NodeModuleResolution.resolve(getBaseDirectory(),
                super.normalizeName(unnormalizedName, referrerId), unnormalizedName, referrerId);
    }
}
