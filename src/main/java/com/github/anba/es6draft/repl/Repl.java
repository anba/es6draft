/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.EvaluateConstructorCall;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime._throw;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.strictEqualityComparison;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserEOFException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Realm.GlobalObjectCreator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.objects.JSONObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * Simple REPL
 */
public class Repl {
    public static void main(String[] args) {
        new Repl(Options.fromArgs(args), System.console()).loop();
    }

    enum Options {
        CompileOnly, Debug;

        static EnumSet<Options> fromArgs(String[] args) {
            EnumSet<Options> options = EnumSet.noneOf(Options.class);
            for (String arg : args) {
                switch (arg) {
                case "compile-only":
                    options.add(CompileOnly);
                    break;
                case "debug":
                    options.add(Debug);
                    break;
                default:
                    System.err.printf("invalid option '%s'\n", arg);
                }
            }
            return options;
        }
    }

    private final EnumSet<Options> options;
    private final Console console;

    public Repl(EnumSet<Options> options, Console console) {
        this.options = options;
        this.console = console;
    }

    /**
     * REPL: Read
     */
    private com.github.anba.es6draft.ast.Script read(int line) {
        StringBuilder source = new StringBuilder();
        try {
            for (;;) {
                String s = console.readLine();
                if (s == null) {
                    continue;
                }
                source.append(s).append('\n');
                try {
                    Parser parser = new Parser("typein", line, false, true);
                    return parser.parse(source);
                } catch (ParserEOFException e) {
                    continue;
                }
            }
        } catch (ParserException e) {
            console.printf("%s: %s\n", e.getExceptionType(), e.getMessage());
            return null;
        }
    }

    /**
     * REPL: Eval
     */
    private Object eval(Realm realm, com.github.anba.es6draft.ast.Script parsedScript) {
        if (parsedScript.getStatements().isEmpty()) {
            console.writer().println();
            return null;
        }
        try {
            Script script = script(parsedScript);
            return ScriptLoader.ScriptEvaluation(script, realm, false);
        } catch (ScriptException e) {
            console.printf("uncaught exception: %s\n", e.getMessage());
            return null;
        }
    }

    /**
     * REPL: Print
     */
    private void print(Realm realm, Object result) {
        try {
            console.writer().println(ToSource(realm, result));
        } catch (ScriptException e) {
            console.printf("uncaught exception: %s\n", e.getMessage());
        }
    }

    /**
     * REPL: Loop
     */
    private void loop() {
        Realm realm = Realm.newRealm(new GlobalObjectCreator<GlobalObject>() {
            @Override
            public GlobalObject createGlobal(Realm realm) {
                return new ReplGlobalObject(realm, Repl.this);
            }
        });
        createProperties(realm.getGlobalThis(), realm, ReplGlobalObject.class);

        for (int line = 1;; line += 1) {
            console.printf("js> ");
            com.github.anba.es6draft.ast.Script parsedScript = read(line);
            if (parsedScript == null) {
                continue;
            }
            Object result = eval(realm, parsedScript);
            if (result == null) {
                continue;
            }
            print(realm, result);
        }
    }

    private static String ToSource(Realm realm, Object val) {
        return JSONObject.ToSource(realm, val);
    }

    private Object loadFile(Realm realm, Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String sourceName = path.getFileName().toString();
        String source = new String(bytes, "UTF-8");
        try {
            Script script = script(sourceName, source);
            return ScriptLoader.ScriptEvaluation(script, realm, false);
        } catch (ParserException | ScriptException e) {
            System.console().printf("uncaught exception: %s\n", e.getMessage());
            return UNDEFINED;
        }
    }

    private AtomicInteger scriptCounter = new AtomicInteger(0);

    private Script script(com.github.anba.es6draft.ast.Script parsedScript) throws ParserException {
        String className = "typein_" + scriptCounter.incrementAndGet();
        if (options.contains(Options.CompileOnly)) {
            return ScriptLoader.compile(className, parsedScript, options.contains(Options.Debug));
        } else {
            return ScriptLoader.load(className, parsedScript);
        }
    }

    private Script script(String sourceName, String source) throws ParserException {
        String className = "typein_" + scriptCounter.incrementAndGet();
        return ScriptLoader.load(sourceName, className, source, false);
    }

    public static class ReplGlobalObject extends GlobalObject {
        private final Repl repl;

        public ReplGlobalObject(Realm realm, Repl repl) {
            super(realm);
            this.repl = repl;
        }

        @Function(name = "print", arity = 1, attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public void print(String message) {
            repl.console.writer().println(message);
        }

        @Function(name = "options", arity = 0, attributes = @Attributes(writable = true,
                enumerable = false, configurable = true))
        public void options() {
            repl.console.writer().println(this.repl.options.toString());
        }

        @Function(name = "assertEq", arity = 2, attributes = @Attributes(writable = true,
                enumerable = false, configurable = true))
        public void assertEq(Object actual, Object expected, Object message) {
            if (!strictEqualityComparison(actual, expected)) {
                Realm realm = realm();
                StringBuilder msg = new StringBuilder();
                msg.append(String.format("Assertion failed: got %s, expected %s",
                        ToSource(realm, actual), ToSource(realm, expected)));
                if (!Type.isUndefined(message)) {
                    msg.append(": ").append(ToFlatString(realm, message));
                }
                Object error = EvaluateConstructorCall(realm.getIntrinsic(Intrinsics.Error),
                        new Object[] { msg.toString() }, realm);
                _throw(error);
            } else {
                System.out.println("pass");
            }
        }

        @Function(name = "load", arity = 1, attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public Object load(String file) throws IOException {
            return repl.loadFile(realm(), Paths.get(file));
        }

        @Function(name = "quit", arity = 0, attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public void quit() {
            System.exit(0);
        }
    }
}
