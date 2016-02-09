/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1>
 * <ul>
 * <li>15.1 Scripts
 * <li>15.2 Modules
 * </ul>
 */
public interface Executable {
    /**
     * Returns the runtime source object if available.
     * 
     * @return the source object or {@code null} if not available
     */
    RuntimeInfo.SourceObject getSourceObject();
}
