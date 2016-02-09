/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

/** 
 *
 */
public interface SourceInfo {
    /**
     * Returns the file name or {@code null} if not available.
     * 
     * @return the file name or {@code null}
     */
    String getFileName();

    /**
     * Returns the source map or {@code null} if not available.
     * 
     * @return the source map or {@code null}
     */
    String getSourceMap();
}
