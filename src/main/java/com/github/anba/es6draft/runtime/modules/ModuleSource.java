/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import java.io.IOException;

import com.github.anba.es6draft.runtime.internal.Source;

/**
 *
 */
public interface ModuleSource {
    /**
     * Returns the module source code string.
     * 
     * @return the module source code
     * @throws IOException
     *             if there was any I/O exception
     */
    String sourceCode() throws IOException;

    /**
     * Returns the module source code information object.
     * 
     * @return the source information
     */
    Source toSource();
}
