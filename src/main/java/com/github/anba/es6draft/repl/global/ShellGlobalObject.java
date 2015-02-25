/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Objects;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.Scripts;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 *
 */
public abstract class ShellGlobalObject extends GlobalObject {
    protected final ShellConsole console;
    private final Path baseDir;
    private final Path script;
    private final ScriptCache scriptCache;

    public ShellGlobalObject(Realm realm, ShellConsole console, Path baseDir, Path script,
            ScriptCache scriptCache) {
        super(realm);
        this.console = console;
        this.baseDir = baseDir.toAbsolutePath();
        this.script = script;
        this.scriptCache = scriptCache;
    }

    @Override
    protected void initializeExtensions() {
        super.initializeExtensions();
        install(this, ShellGlobalObject.class);
    }

    public final <T> T install(T object, Class<T> clazz) {
        Realm realm = getRealm();
        Properties.createProperties(realm.defaultContext(), realm.getGlobalThis(), object, clazz);
        return object;
    }

    /**
     * Returns the shell console.
     * 
     * @return the shell console
     */
    public final ShellConsole getConsole() {
        return console;
    }

    /**
     * Returns the script loader.
     * 
     * @return the script loader
     */
    public final ScriptLoader getScriptLoader() {
        return getRealm().getScriptLoader();
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

    protected static final String getResourceInfo(String resourceName, String defaultValue) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                ShellGlobalObject.class.getResourceAsStream(resourceName), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (IOException e) {
            return defaultValue;
        }
    }

    protected final Path absolutePath(Path file) {
        return baseDir.resolve(file);
    }

    protected final Path relativePath(Path file) {
        return baseDir.resolve(script.getParent().resolve(file));
    }

    protected final Path relativePathToScript(ExecutionContext caller, Path file) {
        Source source = getRealm().sourceInfo(caller);
        return baseDir.resolve(source.getFile().getParent().resolve(file));
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
    public void eval(String moduleName) throws IOException, MalformedNameException,
            ResolutionException, ParserException, CompilationException {
        eval(getRealm(), moduleName);
    }

    /**
     * Parses, compiles and executes the javascript module file.
     * 
     * @param realm
     *            the target realm instance
     * @param moduleName
     *            the unnormalized module name
     * @return the resolved module
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
    protected ModuleRecord eval(Realm realm, String moduleName) throws IOException,
            MalformedNameException, ResolutionException, ParserException, CompilationException {
        ModuleLoader moduleLoader = realm.getModuleLoader();
        SourceIdentifier moduleId = moduleLoader.normalizeName(moduleName, null);
        ModuleRecord module = moduleLoader.resolve(moduleId, realm);
        module.instantiate();
        module.evaluate();
        return module;
    }

    /**
     * Parses, compiles and executes the javascript file.
     * 
     * @param fileName
     *            the file name for the script file
     * @param file
     *            the absolute path to the file
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public void eval(Path fileName, Path file) throws IOException, ParserException,
            CompilationException {
        Source source = new Source(file, fileName.toString(), 1);
        Script script = getScriptLoader().script(source, file);
        eval(script);
    }

    /**
     * Executes the given script.
     * 
     * @param script
     *            the script to evaluate
     * @return the evaluation result
     */
    public Object eval(Script script) {
        return Scripts.ScriptEvaluation(script, getRealm());
    }

    /**
     * Parses, compiles and executes the javascript file (uses {@link #scriptCache}).
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
        Script script = scriptCache.get(getScriptLoader(), absolutePath(file));
        eval(script);
    }

    /**
     * Parses, compiles and executes the javascript file (uses {@link #scriptCache})
     * 
     * @param file
     *            the URL to the file
     * @throws IOException
     *             if there was any I/O error
     * @throws URISyntaxException
     *             the URL is not a valid URI
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public void include(URL file) throws IOException, URISyntaxException, ParserException,
            CompilationException {
        Script script = scriptCache.get(getScriptLoader(), file);
        eval(script);
    }

    protected void includeNative(URL file) throws IOException, URISyntaxException, ParserException,
            CompilationException {
        Script script = scriptCache.get(createNativeScriptLoader(), file);
        eval(script);
    }

    private ScriptLoader createNativeScriptLoader() {
        ScriptLoader scriptLoader = getScriptLoader();
        EnumSet<Parser.Option> parserOptions = EnumSet.copyOf(scriptLoader.getParserOptions());
        parserOptions.add(Parser.Option.NativeCall);
        parserOptions.add(Parser.Option.NativeFunction);
        return new ScriptLoader(scriptLoader.getExecutor(), scriptLoader.getOptions(),
                parserOptions, scriptLoader.getCompilerOptions());
    }

    protected static final ScriptException newError(ExecutionContext cx, String message) {
        return Errors.newError(cx, Objects.toString(message, ""));
    }

    protected String read(ExecutionContext cx, Path fileName, Path path) {
        if (!Files.exists(path)) {
            throw new ScriptException(String.format("can't open '%s'", fileName.toString()));
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw newError(cx, e.getMessage());
        }
    }

    protected Object load(ExecutionContext cx, Path fileName, Path path) {
        if (!Files.exists(path)) {
            throw new ScriptException(String.format("can't open '%s'", fileName.toString()));
        }
        try {
            eval(fileName, path);
            return UNDEFINED;
        } catch (IOException e) {
            throw newError(cx, e.getMessage());
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(cx);
        }
    }

    /**
     * shell-function: {@code readline()}
     * 
     * @return the read line from stdin
     */
    @Function(name = "readline", arity = 0)
    public String readline() {
        return console.readLine();
    }

    /**
     * shell-function: {@code print(message)}
     * 
     * @param messages
     *            the string to print
     */
    @Function(name = "print", arity = 1)
    public void print(String... messages) {
        console.print(Strings.concatWith(' ', messages));
    }

    /**
     * shell-function: {@code load(filename)}
     *
     * @param cx
     *            the execution context
     * @param filename
     *            the file to load
     * @return the result value
     */
    @Function(name = "load", arity = 1)
    public Object load(ExecutionContext cx, String filename) {
        return load(cx, Paths.get(filename), absolutePath(Paths.get(filename)));
    }

    /**
     * shell-function: {@code read(filename)}
     * 
     * @param cx
     *            the execution context
     * @param filename
     *            the file to load
     * @return the file content
     */
    @Function(name = "read", arity = 1)
    public String read(ExecutionContext cx, String filename) {
        return read(cx, Paths.get(filename), absolutePath(Paths.get(filename)));
    }

    /**
     * shell-function: {@code quit()}
     */
    @Function(name = "quit", arity = 0)
    public void quit() {
        throw new StopExecutionException(StopExecutionException.Reason.Quit);
    }
}
