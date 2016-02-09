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
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
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
public abstract class TestGlobals<GLOBAL extends GlobalObject, TEST extends TestInfo> extends ExternalResource {
    private final Configuration configuration;
    private EnumSet<CompatibilityOption> options;
    private ScriptCache scriptCache;
    private List<Script> scripts;
    private PreloadModules modules;

    protected TestGlobals(Configuration configuration) {
        this.configuration = configuration;
    }

    protected abstract ObjectAllocator<GLOBAL> newAllocator(ShellConsole console);

    protected ExecutorService getExecutor() {
        return null;
    }

    protected EnumSet<CompatibilityOption> getOptions() {
        return EnumSet.copyOf(options);
    }

    protected EnumSet<Parser.Option> getParserOptions() {
        return EnumSet.noneOf(Parser.Option.class);
    }

    protected EnumSet<Compiler.Option> getCompilerOptions() {
        return EnumSet.noneOf(Compiler.Option.class);
    }

    protected Path getBaseDirectory() {
        return Resources.getTestSuitePath(configuration);
    }

    protected RuntimeContext createContext() {
        /* @formatter:off */
        return new RuntimeContext.Builder()
                                 .setBaseDirectory(getBaseDirectory())
                                 .setExecutor(getExecutor())
                                 .setOptions(getOptions())
                                 .setParserOptions(getParserOptions())
                                 .setCompilerOptions(getCompilerOptions())
                                 .setScriptCache(scriptCache)
                                 .build();
        /* @formatter:on */
    }

    protected RuntimeContext createContext(ShellConsole console, TEST test) {
        /* @formatter:off */
        return new RuntimeContext.Builder()
                                 .setLocale(getLocale(test))
                                 .setTimeZone(getTimeZone(test))
                                 .setGlobalAllocator(newAllocator(console))
                                 .setBaseDirectory(test.getBaseDir())
                                 .setExecutor(getExecutor())
                                 .setReader(console.reader())
                                 .setWriter(console.writer())
                                 .setErrorWriter(console.errorWriter())
                                 .setOptions(getOptions())
                                 .setParserOptions(getParserOptions())
                                 .setCompilerOptions(getCompilerOptions())
                                 .setScriptCache(scriptCache)
                                 .build();
        /* @formatter:on */
    }

    protected ScriptLoader createScriptLoader(RuntimeContext context) {
        return new ScriptLoader(context);
    }

    protected TestModuleLoader<?> createModuleLoader(RuntimeContext context, ScriptLoader scriptLoader) {
        return new TestFileModuleLoader(context, scriptLoader);
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
                new LinkedBlockingQueue<Runnable>(queueCapacity), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    protected void before() throws Throwable {
        if (!Resources.isEnabled(configuration)) {
            // skip initialization if test suite not enabled
            return;
        }

        // read options ...
        options = enumSet(CompatibilityOption.class, compatibilityOptions(configuration.getString("mode", "")));
        scriptCache = new ScriptCache();

        // pre-compile initialization scripts and modules
        scripts = compileScripts();
        modules = compileModules();
    }

    public final GLOBAL newGlobal(ShellConsole console, TEST test)
            throws MalformedNameException, ResolutionException, IOException, URISyntaxException {
        RuntimeContext context = createContext(console, test);
        ScriptLoader scriptLoader = createScriptLoader(context);
        TestModuleLoader<?> moduleLoader = createModuleLoader(context, scriptLoader);
        World world = new World(context, moduleLoader, scriptLoader);
        Realm realm = world.newInitializedRealm();

        // Evaluate additional initialization scripts and modules
        for (ModuleRecord module : modules.allModules) {
            moduleLoader.defineFromTemplate(module, realm);
        }
        for (ModuleRecord module : modules.mainModules) {
            ModuleRecord testModule = moduleLoader.get(module.getSourceCodeId(), realm);
            testModule.instantiate();
            testModule.evaluate();
        }
        for (Script script : scripts) {
            script.evaluate(realm);
        }

        @SuppressWarnings("unchecked")
        GLOBAL global = (GLOBAL) realm.getGlobalObject();
        return global;
    }

    public void release(GLOBAL global) {
        if (global != null) {
            global.getRuntimeContext().getExecutor().shutdown();
        }
    }

    private static <T extends Enum<T>> EnumSet<T> enumSet(Class<T> clazz, Set<? extends T> elements) {
        EnumSet<T> set = EnumSet.noneOf(clazz);
        set.addAll(elements);
        return set;
    }

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
        RuntimeContext context = createContext();
        ScriptLoader scriptLoader = createScriptLoader(context);
        ArrayList<Script> scripts = new ArrayList<>();
        for (String scriptName : toStrings(scriptNames)) {
            Source source = new Source(Resources.resourcePath(scriptName, basedir), scriptName, 1);
            Script script = scriptLoader.script(source, Resources.resource(scriptName, basedir));
            scripts.add(script);
        }
        return scripts;
    }

    private PreloadModules compileModules() throws IOException, MalformedNameException {
        List<?> moduleNames = configuration.getList("modules", emptyList());
        if (moduleNames.isEmpty()) {
            return new PreloadModules(Collections.<ModuleRecord> emptyList(), Collections.<ModuleRecord> emptyList());
        }
        RuntimeContext context = createContext();
        ScriptLoader scriptLoader = createScriptLoader(context);
        TestModuleLoader<?> moduleLoader = createModuleLoader(context, scriptLoader);
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
