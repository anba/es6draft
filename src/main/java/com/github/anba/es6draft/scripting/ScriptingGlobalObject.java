/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 * {@link GlobalObject} implementation for the JSR-223 Scripting API.
 */
public final class ScriptingGlobalObject extends GlobalObject {
    public ScriptingGlobalObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initializeExtensions() {
        super.initializeExtensions();
        createGlobalProperties(new ScriptingFunctions(), ScriptingFunctions.class);
    }
}
