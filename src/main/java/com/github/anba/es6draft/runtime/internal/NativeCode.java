/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.GetModuleNamespace;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleExport;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.modules.loader.URLModuleLoader;
import com.github.anba.es6draft.runtime.modules.loader.URLModuleSource;
import com.github.anba.es6draft.runtime.modules.loader.URLSourceIdentifier;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * 
 */
// TODO: Rename 'NativeCode'
public final class NativeCode {
    private NativeCode() {
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
    public static URL getScriptURL(String name) throws IOException {
        String sourceName = "/scripts/" + name;
        URL url = NativeCode.class.getResource(sourceName);
        if (url == null) {
            throw new IOException(String.format("script '%s' not found", name));
        }
        return url;
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
     * @throws URISyntaxException
     *             the URL is not a valid URI
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public static void load(Realm realm, String name)
            throws IOException, URISyntaxException, ParserException, CompilationException {
        RuntimeContext context = realm.getWorld().getContext();
        ScriptCache scriptCache = context.getScriptCache();
        Script script = scriptCache.get(createNativeScriptLoader(context), getScriptURL(name));
        script.evaluate(realm);
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
    public static ModuleRecord loadModule(Realm realm, String name)
            throws IOException, URISyntaxException, MalformedNameException, ResolutionException {
        RuntimeContext context = realm.getWorld().getContext();
        URLModuleLoader urlLoader = new URLModuleLoader(context, createNativeScriptLoader(context));
        URLSourceIdentifier sourceId = new URLSourceIdentifier(getScriptURL(name));
        URLModuleSource source = new URLModuleSource(sourceId);
        SourceTextModuleRecord module = urlLoader.define(sourceId, source, realm);
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
    public static <T> T getModuleExport(ModuleRecord module, String exportName, Class<T> clazz)
            throws IOException, MalformedNameException, ResolutionException {
        ModuleExport export = module.resolveExport(exportName, new HashMap<>(), new HashSet<>());
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
            Realm realm = module.getRealm();
            if (realm == null) {
                throw new IllegalArgumentException();
            }
            ScriptObject namespace = GetModuleNamespace(realm.defaultContext(), targetModule);
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

    private static ScriptLoader createNativeScriptLoader(RuntimeContext context) {
        EnumSet<Parser.Option> nativeOptions = EnumSet.of(Parser.Option.NativeCall, Parser.Option.NativeFunction);
        nativeOptions.addAll(context.getParserOptions());
        /* @formatter:off */
        RuntimeContext nativeContext = new RuntimeContext.Builder(context)
                                                         .setParserOptions(nativeOptions)
                                                         .build();
        /* @formatter:on */
        return new ScriptLoader(nativeContext);
    }
}
