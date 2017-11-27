/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.GetModuleNamespace;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashMap;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.RealmData;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.ResolvedBinding;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.modules.loader.FileSourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.URLModuleLoader;
import com.github.anba.es6draft.runtime.modules.loader.URLModuleSource;
import com.github.anba.es6draft.runtime.modules.loader.URLSourceIdentifier;

/**
 *
 */
public final class ScriptLoading {
    private ScriptLoading() {
    }

    /**
     * Parses, compiles and executes the javascript file. (Uses the script cache.)
     * 
     * @param realm
     *            the realm instance
     * @param file
     *            the path to the script file
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public static void include(Realm realm, Path file) throws IOException, ParserException, CompilationException {
        ScriptCache scriptCache = realm.getRuntimeContext().getScriptCache();
        Script script = scriptCache.get(file, realm.getScriptLoader()::script);
        script.evaluate(realm);
    }

    /**
     * Parses, compiles and executes the javascript source code.
     * 
     * @param realm
     *            the realm instance
     * @param sourceName
     *            the file name for the script file
     * @param sourceCode
     *            the script source code
     * @return the evaluation result
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public static Object eval(Realm realm, String sourceName, String sourceCode)
            throws ParserException, CompilationException {
        Source source = new Source(new FileSourceIdentifier(Paths.get("")), sourceName, 1);
        Script script = realm.getScriptLoader().script(source, sourceCode);
        return script.evaluate(realm);
    }

    /**
     * Parses, compiles and executes the javascript file.
     * 
     * @param realm
     *            the realm instance
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
    public static Object eval(Realm realm, Path fileName, Path file)
            throws IOException, ParserException, CompilationException {
        Source source = new Source(file, fileName.toString(), 1);
        Script script = realm.getScriptLoader().script(source, file);
        return script.evaluate(realm);
    }

    /**
     * Parses, compiles and executes the javascript file. (Uses the script cache.)
     * <p>
     * The script file is loaded as a native script with elevated privileges.
     * 
     * @param realm
     *            the realm instance
     * @param name
     *            the script name
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public static void evalNative(Realm realm, String name) throws IOException, ParserException, CompilationException {
        URL scriptURL = RealmData.class.getResource("/scripts/" + name);
        if (scriptURL == null) {
            throw new IOException(String.format("script '%s' not found", name));
        }
        RuntimeContext context = realm.getRuntimeContext();
        ScriptCache scriptCache = context.getScriptCache();
        URLSourceIdentifier sourceId = new URLSourceIdentifier(scriptURL);
        Script script = scriptCache.get(sourceId,
                (source, id) -> createNativeScriptLoader(context).script(source, id.toUri().toURL()));
        script.evaluate(realm);
    }

    /**
     * Parses, compiles and executes the javascript module file.
     * 
     * @param realm
     *            the realm instance
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
    public static void evalModule(Realm realm, String moduleName)
            throws IOException, MalformedNameException, ResolutionException, ParserException, CompilationException {
        ModuleLoader moduleLoader = realm.getModuleLoader();
        SourceIdentifier moduleId = moduleLoader.normalizeName(moduleName, null);
        ModuleRecord module = moduleLoader.resolve(moduleId, realm);
        module.instantiate();
        module.evaluate();
    }

    /**
     * Loads the javascript module file.
     * <p>
     * The script file is loaded as a native module with elevated privileges.
     * 
     * @param realm
     *            the realm instance
     * @param name
     *            the module name
     * @return the native module record
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
    public static ModuleRecord evalNativeModule(Realm realm, String name)
            throws IOException, MalformedNameException, ResolutionException {
        URL scriptURL = RealmData.class.getResource("/scripts/" + name);
        if (scriptURL == null) {
            throw new IOException(String.format("module '%s' not found", name));
        }
        RuntimeContext context = realm.getRuntimeContext();
        URLModuleLoader urlLoader = new URLModuleLoader(context, createNativeScriptLoader(context));
        URLSourceIdentifier sourceId = new URLSourceIdentifier(scriptURL);
        URLModuleSource source = new URLModuleSource(scriptURL, name);
        SourceTextModuleRecord module = urlLoader.define(sourceId, source, realm);
        module.instantiate();
        module.evaluate();
        return module;
    }

    /**
     * Resolves and returns the exported binding from a module record.
     * 
     * @param module
     *            the module record
     * @param exportName
     *            the export name
     * @return the exported value
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static Object getModuleExport(ModuleRecord module, String exportName)
            throws IOException, MalformedNameException, ResolutionException {
        // Throw if module isn't linked.
        if (module.getRealm() == null) {
            throw new IllegalArgumentException();
        }

        // Ensure the module is instantiated and evaluated.
        module.instantiate();
        module.evaluate();

        ResolvedBinding export = module.resolveExport(exportName, new HashMap<>());
        if (export == null) {
            throw new ResolutionException(Messages.Key.ModulesUnresolvedExport, exportName);
        }
        if (export.isAmbiguous()) {
            throw new ResolutionException(Messages.Key.ModulesAmbiguousExport, exportName);
        }
        ModuleRecord targetModule = export.getModule();
        if (targetModule.getEnvironment() == null) {
            throw new IllegalStateException();
        }
        if (export.isNameSpaceExport()) {
            return GetModuleNamespace(module.getRealm().defaultContext(), targetModule);
        }
        return targetModule.getEnvironment().getEnvRec().getBindingValue(export.getBindingName(), true);
    }

    private static ScriptLoader createNativeScriptLoader(RuntimeContext context) {
        EnumSet<Parser.Option> nativeOptions = EnumSet.of(Parser.Option.NativeCall, Parser.Option.NativeFunction);
        nativeOptions.addAll(context.getParserOptions());
        return new ScriptLoader(new RuntimeContext.Builder(context).setParserOptions(nativeOptions).build());
    }
}
