/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

/**
 * Exception class to propagate {@link BaseShellFunctions#quit()}.
 */
@SuppressWarnings("serial")
public final class StopExecutionException extends RuntimeException {
    private final Reason reason;

    public enum Reason {
        Quit, Terminate
    }

    public StopExecutionException(Reason reason) {
        super(reason.toString());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
