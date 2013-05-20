/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Realm.GlobalObjectCreator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 *
 */
public class SimpleShellGlobalObject extends GlobalObject {
    private final ShellConsole console;

    public SimpleShellGlobalObject(Realm realm, ShellConsole console) {
        super(realm);
        this.console = console;
    }

    static SimpleShellGlobalObject newGlobal(final ShellConsole console) {
        Realm realm = Realm.newRealm(new GlobalObjectCreator<SimpleShellGlobalObject>() {
            @Override
            public SimpleShellGlobalObject createGlobal(Realm realm) {
                return new SimpleShellGlobalObject(realm, console);
            }
        });

        // start initialization
        ExecutionContext cx = realm.defaultContext();
        SimpleShellGlobalObject global = (SimpleShellGlobalObject) realm.getGlobalThis();
        createProperties(global, cx, SimpleShellGlobalObject.class);

        return global;
    }

    /** shell-function: {@code print(message)} */
    @Function(name = "print", arity = 1)
    public void print(String message) {
        console.print(message);
    }
}
