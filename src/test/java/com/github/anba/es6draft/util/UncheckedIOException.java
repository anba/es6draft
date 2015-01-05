/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.io.IOException;

/**
 * Unchecked exception wrapping {@link IOException}
 */
@SuppressWarnings("serial")
public class UncheckedIOException extends RuntimeException {
    public UncheckedIOException(IOException cause) {
        super(cause);
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
