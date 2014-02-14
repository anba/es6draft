/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 *
 */
public abstract class ShellGlobalObject extends GlobalObject {
    protected final ShellConsole console;
    protected final Path baseDir;
    protected final Path script;
    protected final ScriptCache scriptCache;

    public ShellGlobalObject(Realm realm, ShellConsole console, Path baseDir, Path script,
            ScriptCache scriptCache) {
        super(realm);
        this.console = console;
        this.baseDir = baseDir;
        this.script = script;
        this.scriptCache = scriptCache;
    }

    @Override
    public void defineBuiltinProperties(ExecutionContext cx, ScriptObject object) {
        super.defineBuiltinProperties(cx, object);
        createProperties(object, this, cx, ShellGlobalObject.class);
    }

    /**
     * Compiles the script {@code name} from the 'scripts' directory
     */
    public static Script compileScript(ScriptCache scriptCache, String name) throws IOException,
            ParserException, CompilationException {
        String sourceName = "/scripts/" + name;
        try (InputStream stream = ShellGlobalObject.class.getResourceAsStream(sourceName)) {
            return scriptCache.script(sourceName, 1, stream);
        }
    }

    protected static String getResourceInfo(String resourceName, String defaultValue) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                ShellGlobalObject.class.getResourceAsStream(resourceName), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (IOException e) {
            return defaultValue;
        }
    }

    protected static String concat(String... strings) {
        if (strings.length == 0) {
            return "";
        } else if (strings.length == 1) {
            return strings[0];
        } else {
            StringBuilder sb = new StringBuilder();
            for (String string : strings) {
                sb.append(string).append(' ');
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
    }

    protected final Path absolutePath(Path file) {
        return baseDir.resolve(file);
    }

    protected final Path relativePath(Path file) {
        return baseDir.resolve(script.getParent().resolve(file));
    }

    protected final Path relativePathToScript(Path file) {
        Script currentScript = getRealm().getScriptContext().getCurrentScript();
        String sourceFile = currentScript.getScriptBody().sourceFile();
        return baseDir.resolve(Paths.get(sourceFile).getParent().resolve(file));
    }

    /**
     * Returns the initialisation scripts which should be run for this global instance
     */
    protected List<Script> initialisationScripts() throws IOException, ParserException,
            CompilationException {
        return new ArrayList<>();
    }

    /**
     * Execute the initialisation scripts which should be run for this global instance
     */
    public final void executeInitialisation() throws IOException, ParserException,
            CompilationException {
        for (Script initScript : initialisationScripts()) {
            eval(initScript);
        }
    }

    /**
     * Parses, compiles and executes the javascript file
     */
    public void eval(Path fileName, Path file) throws IOException, ParserException,
            CompilationException {
        Script script = scriptCache.script(fileName.toString(), 1, file);
        ScriptLoader.ScriptEvaluation(script, getRealm(), false);
    }

    /**
     * Executes the given script
     */
    public void eval(Script script) {
        ScriptLoader.ScriptEvaluation(script, getRealm(), false);
    }

    /**
     * Parses, compiles and executes the javascript file (uses {@link #scriptCache})
     */
    public void include(Path file) throws IOException, ParserException, CompilationException {
        Script script = scriptCache.get(absolutePath(file));
        ScriptLoader.ScriptEvaluation(script, getRealm(), false);
    }

    protected static ScriptException newError(ExecutionContext cx, String message) {
        return Errors.newError(cx, Objects.toString(message, ""));
    }

    protected String read(ExecutionContext cx, Path path) {
        if (!Files.exists(path)) {
            throw ScriptException.create(String.format("can't open '%s'", path.toString()));
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
            throw ScriptException.create(String.format("can't open '%s'", path.toString()));
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

    /** shell-function: {@code readline()} */
    @Function(name = "readline", arity = 0)
    public String readline() {
        return console.readLine();
    }

    /** shell-function: {@code print(message)} */
    @Function(name = "print", arity = 1)
    public void print(String... messages) {
        console.print(concat(messages));
    }

    /** shell-function: {@code load(filename)} */
    @Function(name = "load", arity = 1)
    public Object load(ExecutionContext cx, String filename) {
        return load(cx, Paths.get(filename), absolutePath(Paths.get(filename)));
    }

    /** shell-function: {@code read(filename)} */
    @Function(name = "read", arity = 1)
    public Object read(ExecutionContext cx, String filename) {
        return read(cx, absolutePath(Paths.get(filename)));
    }

    /** shell-function: {@code quit()} */
    @Function(name = "quit", arity = 0)
    public void quit() {
        throw new StopExecutionException(StopExecutionException.Reason.Quit);
    }
}
