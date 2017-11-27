/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.HashSet;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.Source;

/**
 * Base class for compiled scripts.
 */
public class CompiledScript extends CompiledObject implements Script {
    private HashSet<Integer> blockFunctions;

    protected CompiledScript(Source source, RuntimeInfo.ScriptBody scriptBody) {
        super(source, scriptBody);
    }

    @Override
    public final RuntimeInfo.ScriptBody getScriptBody() {
        return (RuntimeInfo.ScriptBody) getRuntimeObject();
    }

    /**
     * Returns {@code true} if the function for {@code functionId} is a legacy block-level function declaration.
     * 
     * @param functionId
     *            the block function identifier
     * @return {@code true} if script block function
     */
    public final boolean isLegacyBlockFunction(int functionId) {
        return blockFunctions != null && blockFunctions.contains(functionId);
    }

    /**
     * Marks the function for {@code functionId} as a legacy block-level function declaration.
     * 
     * @param functionId
     *            the block function identifier
     */
    public final void setLegacyBlockFunction(int functionId) {
        if (blockFunctions == null) {
            blockFunctions = new HashSet<>();
        }
        blockFunctions.add(functionId);
    }
}
