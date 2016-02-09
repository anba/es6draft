/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.GetModuleNamespace;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleExport;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.modules.loader.URLModuleLoader;
import com.github.anba.es6draft.runtime.modules.loader.URLModuleSource;
import com.github.anba.es6draft.runtime.modules.loader.URLSourceIdentifier;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 *
 */
public class ShellGlobalObject extends GlobalObject {
    private final ShellConsole console;

    protected ShellGlobalObject(Realm realm, ShellConsole console) {
        super(realm);
        this.console = console;
    }

    /**
     * Returns the shell console.
     * 
     * @return the shell console
     */
    protected final ShellConsole getConsole() {
        return console;
    }

    /**
     * Returns the URL for the script {@code name} from the 'scripts' directory.
     * 
     * @param name
     *            the script name
     * @return the script's URL
     * @throws IOException
     *             if the resource could not be found
     */
    protected static final URL getScriptURL(String name) throws IOException {
        String sourceName = "/scripts/" + name;
        URL url = ShellGlobalObject.class.getResource(sourceName);
        if (url == null) {
            throw new IOException(String.format("script '%s' not found", name));
        }
        return url;
    }

    // TODO: Consistent use of "load", "eval" and "include"?

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

    /**
     * Parses, compiles and executes the javascript file. (Uses the script cache.)
     * <p>
     * The script file is loaded as a native script with elevated privileges.
     * 
     * @param name
     *            the script name
     * @throws IOException
     *             if there was any I/O error
     * @throws URISyntaxException
     *             the URL is not a valid URI
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public void includeNative(String name)
            throws IOException, URISyntaxException, ParserException, CompilationException {
        Realm realm = getRealm();
        ScriptCache scriptCache = getRuntimeContext().getScriptCache();
        Script script = scriptCache.get(createNativeScriptLoader(), getScriptURL(name));
        script.evaluate(realm);
    }

    /**
     * Loads the javascript module file.
     * <p>
     * The script file is loaded as a native module with elevated privileges.
     * 
     * @param name
     *            the module name
     * @return the native module record
     * @throws IOException
     *             if there was any I/O error
     * @throws URISyntaxException
     *             the URL is not a valid URI
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws CompilationException
     *             if the parsed module source cannot be compiled
     */
    public ModuleRecord loadNativeModule(String name)
            throws IOException, URISyntaxException, MalformedNameException, ResolutionException {
        URLModuleLoader urlLoader = new URLModuleLoader(getRuntimeContext(), createNativeScriptLoader());
        URLSourceIdentifier sourceId = new URLSourceIdentifier(getScriptURL(name));
        URLModuleSource source = new URLModuleSource(sourceId);
        SourceTextModuleRecord module = urlLoader.define(sourceId, source, getRealm());
        module.instantiate();
        module.evaluate();
        return module;
    }

    /**
     * Resolves and returns the exported binding from a module record.
     * 
     * @param <T>
     *            the object type
     * @param module
     *            the module record
     * @param exportName
     *            the export name
     * @param clazz
     *            the expected class
     * @return the exported value
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public final <T> T getModuleExport(ModuleRecord module, String exportName, Class<T> clazz)
            throws IOException, MalformedNameException, ResolutionException {
        ModuleExport export = module.resolveExport(exportName, new HashMap<ModuleRecord, Set<String>>(),
                new HashSet<ModuleRecord>());
        if (export == null) {
            throw new ResolutionException(Messages.Key.ModulesUnresolvedExport, exportName);
        }
        if (export.isAmbiguous()) {
            throw new ResolutionException(Messages.Key.ModulesAmbiguousExport, exportName);
        }
        ModuleRecord targetModule = export.getModule();
        if (!targetModule.isInstantiated() || !targetModule.isEvaluated()) {
            throw new IllegalStateException();
        }
        if (export.isNameSpaceExport()) {
            ExecutionContext cx = getRealm().defaultContext();
            ScriptObject namespace = GetModuleNamespace(cx, targetModule);
            return clazz.cast(namespace);
        }
        LexicalEnvironment<?> targetEnv = targetModule.getEnvironment();
        if (targetEnv == null) {
            throw new ResolutionException(Messages.Key.UninitializedModuleBinding, export.getBindingName(),
                    targetModule.getSourceCodeId().toString());
        }
        Object bindingValue = targetEnv.getEnvRec().getBindingValue(export.getBindingName(), true);
        return clazz.cast(bindingValue);
    }

    private ScriptLoader createNativeScriptLoader() {
        EnumSet<Parser.Option> nativeOptions = EnumSet.of(Parser.Option.NativeCall, Parser.Option.NativeFunction);
        nativeOptions.addAll(getRuntimeContext().getParserOptions());
        /* @formatter:off */
        RuntimeContext nativeContext = new RuntimeContext.Builder(getRuntimeContext())
                                                         .setParserOptions(nativeOptions)
                                                         .build();
        /* @formatter:on */
        return new ScriptLoader(nativeContext);
    }
}
