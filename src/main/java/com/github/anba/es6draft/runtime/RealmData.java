/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import java.io.IOException;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;

/**
 * 
 */
public class RealmData {
    private final Realm realm;

    public RealmData(Realm realm) {
        this.realm = realm;
    }

    /**
     * Returns the {@link Realm} of this object.
     * 
     * @return the realm instance
     */
    public final Realm getRealm() {
        return realm;
    }

    /**
     * Executes any initialization scripts which should be run for this realm instance.
     * 
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public void initializeScripted() throws IOException, ParserException, CompilationException {
        /* empty */
    }

    /**
     * Initializes implementation defined extensions.
     */
    public void initializeExtensions() {
        /* empty */
    }
}
