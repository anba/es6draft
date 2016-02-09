/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.global.BaseShellFunctions;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.StringModuleSource;

/**
 * Global object for test262 tests, includes all necessary global function definitions.
 */
public final class Test262GlobalObject extends ShellGlobalObject {
    Test262GlobalObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initializeExtensions() {
        createGlobalProperties(new BaseShellFunctions(), BaseShellFunctions.class);
    }

    /**
     * Loads and evaluates the requested test harness file.
     * 
     * @param file
     *            the file name
     * @throws IOException
     *             if there was any I/O error
     */
    void include(String file) throws IOException {
        assert !"sta.js".equals(file) : "cannot load sta.js harness file";
        super.include(Paths.get("harness", file));
    }

    /**
     * Parses, compiles and executes the javascript file.
     * 
     * @param file
     *            the script source file
     * @param sourceCode
     *            the source code
     * @param sourceLine
     *            the source line offset
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    void eval(Path file, String sourceCode, int sourceLine) throws ParserException,
            CompilationException {
        Source source = new Source(file, file.getFileName().toString(), sourceLine);
        super.eval(source, sourceCode);
    }

    /**
     * Parses, compiles and executes the javascript module file.
     * 
     * @param moduleName
     *            the module name
     * @param sourceCode
     *            the source code
     * @param sourceLine
     *            the source line offset
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     * @throws MalformedNameException
     *             if the module name cannot be normalized
     * @throws ResolutionException
     *             if the module exports cannot be resolved
     * @throws IOException
     *             if there was any I/O error
     */
    void evalModule(String moduleName, String sourceCode, int sourceLine) throws ParserException,
            CompilationException, MalformedNameException, ResolutionException, IOException {
        ModuleLoader moduleLoader = getRealm().getModuleLoader();
        SourceIdentifier moduleId = moduleLoader.normalizeName(moduleName, null);
        ModuleSource source = new StringModuleSource(moduleId, sourceCode, sourceLine);
        ModuleRecord module = moduleLoader.define(moduleId, source, getRealm());
        module.instantiate();
        module.evaluate();
    }
}
