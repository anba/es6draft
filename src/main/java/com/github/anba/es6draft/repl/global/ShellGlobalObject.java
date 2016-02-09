/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import java.io.IOException;
import java.nio.file.Path;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 *
 */
public class ShellGlobalObject extends GlobalObject {
    protected ShellGlobalObject(Realm realm) {
        super(realm);
    }

    /**
     * Parses, compiles and executes the javascript module file.
     * 
     * @param moduleName
     *            the unnormalized module name
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws CompilationException
     *             if the parsed module source cannot be compiled
     */
    public void eval(String moduleName)
            throws IOException, MalformedNameException, ResolutionException, ParserException, CompilationException {
        Realm realm = getRealm();
        ModuleLoader moduleLoader = realm.getModuleLoader();
        SourceIdentifier moduleId = moduleLoader.normalizeName(moduleName, null);
        ModuleRecord module = moduleLoader.resolve(moduleId, realm);
        module.instantiate();
        module.evaluate();
    }

    /**
     * Parses, compiles and executes the javascript file.
     * 
     * @param fileName
     *            the file name for the script file
     * @param file
     *            the absolute path to the file
     * @return the evaluation result
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Object eval(Path fileName, Path file) throws IOException, ParserException, CompilationException {
        Realm realm = getRealm();
        Source source = new Source(file, fileName.toString(), 1);
        Script script = realm.getScriptLoader().script(source, file);
        return script.evaluate(realm);
    }

    /**
     * Parses, compiles and executes the javascript source code.
     * 
     * @param source
     *            the source object
     * @param sourceCode
     *            the source code
     * @return the evaluation result
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Object eval(Source source, String sourceCode) throws ParserException, CompilationException {
        Realm realm = getRealm();
        Script script = realm.getScriptLoader().script(source, sourceCode);
        return script.evaluate(realm);
    }

    /**
     * Parses, compiles and executes the javascript file. (Uses the script cache.)
     * 
     * @param file
     *            the path to the script file
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public void include(Path file) throws IOException, ParserException, CompilationException {
        Realm realm = getRealm();
        ScriptCache scriptCache = getRuntimeContext().getScriptCache();
        Path path = getRuntimeContext().getBaseDirectory().resolve(file);
        Script script = scriptCache.get(realm.getScriptLoader(), path);
        script.evaluate(realm);
    }
}
