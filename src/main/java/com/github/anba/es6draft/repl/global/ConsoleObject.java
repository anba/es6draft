/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateMethodProperty;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.anba.es6draft.repl.SourceBuilder;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.NativeCode;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 *
 */
public final class ConsoleObject {
    private ConsoleObject() {
    }

    /**
     * Creates a new {@code console} object.
     * 
     * @param realm
     *            the realm instance
     * @return the console object
     * @throws IOException
     *             if there was any I/O error
     * @throws URISyntaxException
     *             the URL is not a valid URI
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static ScriptObject createConsole(Realm realm)
            throws IOException, URISyntaxException, MalformedNameException, ResolutionException {
        ExecutionContext cx = realm.defaultContext();
        ModuleRecord module = NativeCode.loadModule(realm, "console.jsm");
        ScriptObject console = NativeCode.getModuleExport(module, "default", ScriptObject.class);
        Callable inspectFn = Properties.createFunction(cx, new InspectFunction(), InspectFunction.class);
        CreateMethodProperty(cx, console, "_inspect", inspectFn);
        return console;
    }

    public static final class InspectFunction {
        @Function(name = "inspect", arity = 1)
        public Object inspect(ExecutionContext cx, Object argument) {
            return SourceBuilder.ToSource(cx, argument);
        }
    }
}
