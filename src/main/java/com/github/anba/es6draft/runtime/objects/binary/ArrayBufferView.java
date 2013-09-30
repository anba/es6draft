/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.1 ArrayBuffer Objects</h2>
 * <p>
 * Interface for {@link ScriptObject}s which hold an {@link ArrayBufferObject}
 */
public interface ArrayBufferView extends ScriptObject {
    /** [[ViewedArrayBuffer]] */
    ArrayBufferObject getBuffer();

    /** [[ByteLength]] */
    long getByteLength();

    /** [[ByteOffset]] */
    long getByteOffset();
}
