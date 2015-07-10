/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import static com.github.anba.es6draft.util.Functional.toStrings;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
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
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
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
    private PreloadModules modules;

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

    protected TestModuleLoader<?> createModuleLoader(ScriptLoader scriptLoader) {
        return new TestFileModuleLoader(scriptLoader, getBaseDirectory());
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

    public final GLOBAL newGlobal(ShellConsole console, TEST test) throws MalformedNameException,
            ResolutionException, IOException, URISyntaxException {
        ObjectAllocator<GLOBAL> allocator = newAllocator(console, test, scriptCache);
        ScriptLoader scriptLoader = createScriptLoader();
        TestModuleLoader<?> moduleLoader = createModuleLoader(scriptLoader);
        Locale locale = getLocale(test);
        TimeZone timeZone = getTimeZone(test);
        World<GLOBAL> world = new World<>(allocator, moduleLoader, scriptLoader, locale, timeZone);
        GLOBAL global = world.newInitializedGlobal();
        // Evaluate additional initialization scripts and modules
        for (ModuleRecord module : modules.allModules) {
            moduleLoader.defineFromTemplate(module, global.getRealm());
        }
        for (ModuleRecord module : modules.mainModules) {
            ModuleRecord testModule = moduleLoader.get(module.getSourceCodeId(), global.getRealm());
            testModule.instantiate();
            testModule.evaluate();
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

    private PreloadModules compileModules() throws IOException, MalformedNameException {
        List<?> moduleNames = configuration.getList("modules", emptyList());
        if (moduleNames.isEmpty()) {
            return new PreloadModules(Collections.<ModuleRecord> emptyList(),
                    Collections.<ModuleRecord> emptyList());
        }
        ScriptLoader scriptLoader = createScriptLoader();
        TestModuleLoader<?> moduleLoader = createModuleLoader(scriptLoader);
        ArrayList<ModuleRecord> modules = new ArrayList<>();
        for (String moduleName : toStrings(moduleNames)) {
            SourceIdentifier moduleId = moduleLoader.normalizeName(moduleName, null);
            modules.add(moduleLoader.load(moduleId));
        }
        return new PreloadModules(modules, moduleLoader.getModules());
    }

    private static final class PreloadModules {
        private final List<ModuleRecord> mainModules;
        private final Collection<ModuleRecord> allModules;

        PreloadModules(List<ModuleRecord> modules, Collection<? extends ModuleRecord> requires) {
            assert modules.size() <= requires.size();
            this.mainModules = Collections.unmodifiableList(modules);
            this.allModules = Collections.<ModuleRecord> unmodifiableCollection(requires);
        }
    }
}
