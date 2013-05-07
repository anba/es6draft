/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mjstest;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Realm.GlobalObjectCreator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.builtins.ExoticSymbolObject;

/**
 *
 */
public class MiniJSUnitGlobalObject extends GlobalObject {
    private final Realm realm;
    private final Path baseDir;
    @SuppressWarnings("unused")
    private final Path script;
    private final ScriptCache scriptCache;
    private final Script initScript;

    public MiniJSUnitGlobalObject(Realm realm, Path baseDir, Path script, ScriptCache scriptCache,
            Script initScript) {
        super(realm);
        this.realm = realm;
        this.baseDir = baseDir;
        this.script = script;
        this.scriptCache = scriptCache;
        this.initScript = initScript;
    }

    /**
     * Returns a new instance of this class
     */
    public static MiniJSUnitGlobalObject newGlobal(final Path baseDir, final Path script,
            final ScriptCache scriptCache, final Script initScript) {
        Realm realm = Realm.newRealm(new GlobalObjectCreator<MiniJSUnitGlobalObject>() {
            @Override
            public MiniJSUnitGlobalObject createGlobal(Realm realm) {
                return new MiniJSUnitGlobalObject(realm, baseDir, script, scriptCache, initScript);
            }
        });

        // start initialization
        ExecutionContext cx = realm.defaultContext();
        MiniJSUnitGlobalObject global = (MiniJSUnitGlobalObject) realm.getGlobalThis();
        createProperties(global, cx, MiniJSUnitGlobalObject.class);

        // load init-script
        if (global.initScript != null) {
            ScriptLoader.ScriptEvaluation(global.initScript, realm, false);
        }

        return global;
    }

    /**
     * Compiles the "v8legacy.js" script-file
     */
    public static Script compileLegacy(ScriptCache scriptCache) throws ParserException, IOException {
        String sourceName = "/scripts/v8legacy.js";
        InputStream stream = MiniJSUnitGlobalObject.class.getResourceAsStream(sourceName);
        return scriptCache.script(sourceName, 1, stream);
    }

    /**
     * Parses, compiles and executes the javascript file
     */
    public void eval(Path fileName, Path file) throws IOException, ParserException {
        Script script = scriptCache.script(fileName.toString(), 1, file);
        ScriptLoader.ScriptEvaluation(script, realm, false);
    }

    /**
     * Parses, compiles and executes the javascript file (uses {@link #scriptCache})
     */
    public void include(Path file) throws IOException, ParserException {
        Script script = scriptCache.get(baseDir.resolve(file));
        ScriptLoader.ScriptEvaluation(script, realm, false);
    }

    /**
     * Returns the well-known symbol {@code name} or undefined if there is no such symbol
     */
    @Function(name = "getSym", arity = 1)
    public Object getSym(String name) {
        try {
            if (name.startsWith("@@")) {
                return BuiltinSymbol.valueOf(name.substring(2)).get();
            }
        } catch (IllegalArgumentException e) {
        }
        return UNDEFINED;
    }

    /**
     * Creates a new Symbol object
     */
    @Function(name = "newSym", arity = 2)
    public Object newSym(String name, boolean _private) {
        return new ExoticSymbolObject(name, _private);
    }

    /** shell-function: {@code print(message)} */
    @Function(name = "print", arity = 1)
    public void print(String message) {
    }

    /** shell-function: {@code gc()} */
    @Function(name = "gc", arity = 0)
    public void gc() {
    }
}
