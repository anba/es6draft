/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.LinkModules;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.ModuleEvaluation;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.NormalizeModuleName;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.ParseModuleAndImports;
import static com.github.anba.es6draft.util.Functional.toStrings;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.junit.rules.ExternalResource;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.ShadowRealm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.FileModuleLoader;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 * {@link ExternalResource} sub-class to facilitate creation of {@link GlobalObject} instances
 */
public abstract class TestGlobals<GLOBAL extends ShellGlobalObject, TEST extends TestInfo> extends
        ExternalResource {
    private final Configuration configuration;
    private Set<CompatibilityOption> options;
    private ScriptCache scriptCache;
    private List<Script> scripts;
    private List<PreloadedModule> modules;

    protected TestGlobals(Configuration configuration) {
        this.configuration = configuration;
    }

    protected ExecutorService getExecutor() {
        return null;
    }

    protected Set<CompatibilityOption> getOptions() {
        return options;
    }

    protected Set<Parser.Option> getParserOptions() {
        return EnumSet.noneOf(Parser.Option.class);
    }

    protected Set<Compiler.Option> getCompilerOptions() {
        return EnumSet.noneOf(Compiler.Option.class);
    }

    protected Path getBaseDirectory() {
        return Resources.getTestSuitePath(configuration);
    }

    protected ScriptLoader createScriptLoader() {
        return new ScriptLoader(getExecutor(), getOptions(), getParserOptions(),
                getCompilerOptions());
    }

    protected ModuleLoader createModuleLoader(Path baseDirectory) {
        return new FileModuleLoader(baseDirectory);
    }

    protected Locale getLocale(TEST test) {
        return Locale.getDefault();
    }

    protected TimeZone getTimeZone(TEST test) {
        return TimeZone.getDefault();
    }

    protected static ThreadPoolExecutor createDefaultSharedExecutor() {
        int coreSize = 4;
        int maxSize = 12;
        long timeout = 60L;
        int queueCapacity = 10;
        return new ThreadPoolExecutor(coreSize, maxSize, timeout, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    protected void before() throws Throwable {
        if (!Resources.isEnabled(configuration)) {
            // skip initialization if test suite not enabled
            return;
        }

        // read options ...
        options = compatibilityOptions(configuration.getString("mode", ""));
        scriptCache = new ScriptCache();

        // pre-compile initialization scripts and modules
        scripts = compileScripts();
        modules = compileModules();
    }

    public final GLOBAL newGlobal(ShellConsole console, TEST test) throws IOException,
            URISyntaxException {
        ObjectAllocator<GLOBAL> allocator = newAllocator(console, test, scriptCache);
        ModuleLoader moduleLoader = createModuleLoader(getBaseDirectory());
        ScriptLoader scriptLoader = createScriptLoader();
        Locale locale = getLocale(test);
        TimeZone timeZone = getTimeZone(test);
        World<GLOBAL> world = new World<>(allocator, moduleLoader, scriptLoader, locale, timeZone);
        GLOBAL global = world.newInitializedGlobal();
        // Evaluate additional initialization scripts and modules
        for (PreloadedModule preloadModule : modules) {
            Realm realm = global.getRealm();
            SourceIdentifier moduleName = preloadModule.getModuleName();
            LinkedHashMap<SourceIdentifier, ModuleRecord> newModuleSet = preloadModule
                    .getRequires();
            assert newModuleSet.containsKey(moduleName);
            ModuleRecord module = newModuleSet.get(moduleName);
            LinkModules(realm, newModuleSet);
            ModuleEvaluation(module, realm);
        }
        for (Script script : scripts) {
            global.eval(script);
        }
        return global;
    }

    protected abstract ObjectAllocator<GLOBAL> newAllocator(ShellConsole console, TEST test,
            ScriptCache scriptCache);

    private static Set<CompatibilityOption> compatibilityOptions(String mode) {
        switch (mode) {
        case "moz-compatibility":
            return CompatibilityOption.MozCompatibility();
        case "web-compatibility":
            return CompatibilityOption.WebCompatibility();
        case "strict-compatibility":
        default:
            return CompatibilityOption.StrictCompatibility();
        }
    }

    private List<Script> compileScripts() throws IOException {
        List<?> scriptNames = configuration.getList("scripts", emptyList());
        if (scriptNames.isEmpty()) {
            return Collections.emptyList();
        }
        Path basedir = getBaseDirectory();
        ScriptLoader scriptLoader = createScriptLoader();
        ArrayList<Script> scripts = new ArrayList<>();
        for (String scriptName : toStrings(scriptNames)) {
            Script script = scriptLoader.script(
                    new Source(Resources.resourcePath(scriptName, basedir), scriptName, 1),
                    Resources.resource(scriptName, basedir));
            scripts.add(script);
        }
        return scripts;
    }

    private List<PreloadedModule> compileModules() throws IOException {
        List<?> moduleNames = configuration.getList("modules", emptyList());
        if (moduleNames.isEmpty()) {
            return Collections.emptyList();
        }
        ModuleLoader moduleLoader = createModuleLoader(getBaseDirectory());
        ScriptLoader scriptLoader = createScriptLoader();
        PreloaderRealm realm = new PreloaderRealm(moduleLoader, scriptLoader);
        ArrayList<PreloadedModule> modules = new ArrayList<>();
        for (String moduleName : toStrings(moduleNames)) {
            SourceIdentifier normalizedModuleName = NormalizeModuleName(realm, moduleName, null);
            if (!realm.getModules().containsKey(normalizedModuleName)) {
                LinkedHashMap<SourceIdentifier, ModuleRecord> newModules = new LinkedHashMap<>();
                ModuleRecord module = ParseModuleAndImports(realm, normalizedModuleName, newModules);
                realm.getModules().putAll(newModules);
                modules.add(new PreloadedModule(module, newModules));
            }
        }
        return modules;
    }

    private static final class PreloadedModule {
        private final ModuleRecord module;
        private final LinkedHashMap<SourceIdentifier, ModuleRecord> requires;

        PreloadedModule(ModuleRecord module, LinkedHashMap<SourceIdentifier, ModuleRecord> requires) {
            this.module = module;
            this.requires = requires;
        }

        public SourceIdentifier getModuleName() {
            return module.getSourceCodeId();
        }

        public LinkedHashMap<SourceIdentifier, ModuleRecord> getRequires() {
            LinkedHashMap<SourceIdentifier, ModuleRecord> requiresCopy = new LinkedHashMap<>(
                    requires);
            for (Map.Entry<SourceIdentifier, ModuleRecord> entry : requiresCopy.entrySet()) {
                entry.setValue(entry.getValue().clone());
            }
            return requiresCopy;
        }
    }

    private static final class PreloaderRealm implements ShadowRealm {
        private final ModuleLoader moduleLoader;
        private final ScriptLoader scriptLoader;
        private final HashMap<SourceIdentifier, ModuleRecord> modules = new HashMap<>();

        PreloaderRealm(ModuleLoader moduleLoader, ScriptLoader scriptLoader) {
            this.moduleLoader = moduleLoader;
            this.scriptLoader = scriptLoader;
        }

        @Override
        public ModuleLoader getModuleLoader() {
            return moduleLoader;
        }

        @Override
        public ScriptLoader getScriptLoader() {
            return scriptLoader;
        }

        @Override
        public Map<SourceIdentifier, ModuleRecord> getModules() {
            return modules;
        }
    }
}
