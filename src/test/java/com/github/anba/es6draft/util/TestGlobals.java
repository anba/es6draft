/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.configuration.Configuration;
import org.junit.rules.ExternalResource;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Console;
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
public class TestGlobals<GLOBAL extends GlobalObject, TEST extends TestInfo> extends ExternalResource {
    private static final String DEFAULT_MODE = "web-compatibility";
    private static final String DEFAULT_VERSION = CompatibilityOption.Version.ECMAScript2016.name();
    private static final String DEFAULT_STAGE = CompatibilityOption.Stage.Finished.name();
    private static final List<String> DEFAULT_FEATURES = emptyList();

    private final Configuration configuration;
    private final ObjectAllocator<GLOBAL> allocator;
    private final BiFunction<RuntimeContext, ScriptLoader, TestModuleLoader<?>> moduleLoader;
    private EnumSet<CompatibilityOption> options;
    private ScriptCache scriptCache;
    private List<Script> scripts;
    private PreloadModules modules;

    public TestGlobals(Configuration configuration, ObjectAllocator<GLOBAL> allocator) {
        this(configuration, allocator, TestFileModuleLoader::new);
    }

    public TestGlobals(Configuration configuration, ObjectAllocator<GLOBAL> allocator,
            BiFunction<RuntimeContext, ScriptLoader, TestModuleLoader<?>> moduleLoader) {
        this.configuration = configuration;
        this.allocator = allocator;
        this.moduleLoader = moduleLoader;
    }

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
                                 .setWorkerErrorReporter(this::workerErrorReporter)
                                 .build();
        /* @formatter:on */
    }

    protected RuntimeContext createContext(Console console, TEST test) {
        /* @formatter:off */
        return new RuntimeContext.Builder()
                                 .setLocale(getLocale(test))
                                 .setTimeZone(getTimeZone(test))
                                 .setGlobalAllocator(allocator)
                                 .setModuleLoader(moduleLoader)
                                 .setBaseDirectory(test.getBaseDir())
                                 .setExecutor(getExecutor())
                                 .setConsole(console)
                                 .setOptions(getOptions())
                                 .setParserOptions(getParserOptions())
                                 .setCompilerOptions(getCompilerOptions())
                                 .setScriptCache(scriptCache)
                                 .setWorkerErrorReporter(this::workerErrorReporter)
                                 .build();
        /* @formatter:on */
    }

    protected Locale getLocale(TEST test) {
        return Locale.getDefault();
    }

    protected TimeZone getTimeZone(TEST test) {
        return TimeZone.getDefault();
    }

    protected void workerErrorReporter(ExecutionContext cx, Throwable t) {
        t.printStackTrace();
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
        scriptCache = new ScriptCache();

        // read options ...
        EnumSet<CompatibilityOption> compatibilityOptions = EnumSet.noneOf(CompatibilityOption.class);
        optionsFromMode(compatibilityOptions, configuration.getString("mode", DEFAULT_MODE));
        optionsFromVersion(compatibilityOptions, configuration.getString("version", DEFAULT_VERSION));
        optionsFromStage(compatibilityOptions, configuration.getString("stage", DEFAULT_STAGE));
        optionsFromFeatures(compatibilityOptions, configuration.getList("features", DEFAULT_FEATURES));
        options = compatibilityOptions;

        // pre-compile initialization scripts and modules
        scripts = compileScripts();
        modules = compileModules();
    }

    public final GLOBAL newGlobal(Console console, TEST test)
            throws MalformedNameException, ResolutionException, IOException, URISyntaxException {
        RuntimeContext context = createContext(console, test);
        World world = new World(context);
        Realm realm = world.newInitializedRealm();

        // Evaluate additional initialization scripts and modules
        TestModuleLoader<?> moduleLoader = (TestModuleLoader<?>) world.getModuleLoader();
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
            global.getRuntimeContext().getWorkerExecutor().shutdown();
        }
    }

    private static void optionsFromMode(EnumSet<CompatibilityOption> options, String mode) {
        switch (mode) {
        case "moz-compatibility":
            options.addAll(CompatibilityOption.MozCompatibility());
            break;
        case "web-compatibility":
            options.addAll(CompatibilityOption.WebCompatibility());
            break;
        case "strict-compatibility":
            options.addAll(CompatibilityOption.StrictCompatibility());
            break;
        default:
            throw new IllegalArgumentException(String.format("Unsupported mode: '%s'", mode));
        }
    }

    private static void optionsFromVersion(EnumSet<CompatibilityOption> options, String version) {
        options.addAll(Arrays.stream(CompatibilityOption.Version.values()).filter(v -> {
            return v.name().equalsIgnoreCase(version);
        }).findAny().map(CompatibilityOption::Version).orElseThrow(IllegalArgumentException::new));
    }

    private static void optionsFromStage(EnumSet<CompatibilityOption> options, String stage) {
        options.addAll(Arrays.stream(CompatibilityOption.Stage.values()).filter(s -> {
            if (stage.length() == 1 && Character.isDigit(stage.charAt(0))) {
                return s.getLevel() == Character.digit(stage.charAt(0), 10);
            } else {
                return s.name().equalsIgnoreCase(stage);
            }
        }).findAny().map(CompatibilityOption::Stage).orElseThrow(IllegalArgumentException::new));
    }

    private static void optionsFromFeatures(EnumSet<CompatibilityOption> options, List<Object> features) {
        streamNonEmpty(features).map(TestGlobals::optionFromFeature).forEach(options::add);
    }

    private static CompatibilityOption optionFromFeature(String feature) {
        return Arrays.stream(CompatibilityOption.values()).filter(o -> {
            return o.name().equalsIgnoreCase(feature);
        }).findAny().orElseThrow(IllegalArgumentException::new);
    }

    private List<Script> compileScripts() throws IOException {
        List<?> scriptNames = configuration.getList("scripts", emptyList());
        if (scriptNames.isEmpty()) {
            return Collections.emptyList();
        }
        Path basedir = getBaseDirectory();
        RuntimeContext context = createContext();
        ScriptLoader scriptLoader = new ScriptLoader(context);
        ArrayList<Script> scripts = new ArrayList<>();
        for (String scriptName : nonEmpty(scriptNames)) {
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
        ScriptLoader scriptLoader = new ScriptLoader(context);
        TestModuleLoader<?> moduleLoader = this.moduleLoader.apply(context, scriptLoader);
        ArrayList<ModuleRecord> modules = new ArrayList<>();
        for (String moduleName : nonEmpty(moduleNames)) {
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

    private static Iterable<String> nonEmpty(List<?> c) {
        return () -> streamNonEmpty(c).iterator();
    }

    private static Stream<String> streamNonEmpty(List<?> c) {
        return c.stream().filter(Objects::nonNull).map(Object::toString).filter(not(String::isEmpty));
    }

    private static <T> Predicate<T> not(Predicate<T> p) {
        return p.negate();
    }
}
