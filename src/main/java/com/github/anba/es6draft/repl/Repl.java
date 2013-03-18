/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.EvaluateConstructorCall;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime._throw;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserEOFException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Realm.GlobalObjectCreator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;

/**
 * Simple REPL
 */
public class Repl {
    public static void main(String[] args) {
        new Repl(Option.fromArgs(args), System.console()).loop();
    }

    private enum Option {
        CompileOnly, Debug;

        static EnumSet<Option> fromArgs(String[] args) {
            EnumSet<Option> options = EnumSet.noneOf(Option.class);
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

    private final EnumSet<Option> options;
    private final Console console;

    private Repl(EnumSet<Option> options, Console console) {
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
                    Parser parser = new Parser("typein", line);
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
        } catch (ParserException e) {
            console.printf("%s: %s\n", e.getExceptionType(), e.getMessage());
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
        HashSet<ScriptObject> stack = new HashSet<>();
        return toSource(realm, stack, val);
    }

    private static String toSource(Realm realm, Set<ScriptObject> stack, Object value) {
        switch (Type.of(value)) {
        case Null:
            return "null";
        case Boolean:
            return Type.booleanValue(value) ? "true" : "false";
        case String:
            return stringToSource(Type.stringValue(value));
        case Number:
            return ToFlatString(realm, value);
        case Object:
            ScriptObject objValue = Type.objectValue(value);
            Object toSource = Get(objValue, "toSource");
            if (IsCallable(toSource)) {
                return ToFlatString(realm, ((Callable) toSource).call(objValue));
            }
            if (IsCallable(objValue)) {
                return ((Callable) objValue).toSource();
            }
            if (objValue instanceof Symbol) {
                return ((Symbol) objValue).toString();
            }
            if (stack.contains(objValue)) {
                return "« ... »";
            }
            stack.add(objValue);
            try {
                if (objValue instanceof ExoticArray) {
                    return arrayToSource(realm, stack, objValue);
                } else {
                    return objectToSource(realm, stack, objValue);
                }
            } finally {
                stack.remove(objValue);
            }
        case Undefined:
        default:
            return "(void 0)";
        }
    }

    private static final char[] hexdigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f' };

    private static String stringToSource(CharSequence value) {
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0, len = value.length(); i < len; ++i) {
            char c = value.charAt(i);
            switch (c) {
            case '"':
            case '\\':
                sb.append('\\').append(c);
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\t':
                sb.append("\\t");
                break;
            default:
                if (c < ' ') {
                    sb.append('\\').append('u').append(hexdigits[(c >> 12) & 0xf])
                            .append(hexdigits[(c >> 8) & 0xf]).append(hexdigits[(c >> 4) & 0xf])
                            .append(hexdigits[(c >> 0) & 0xf]);
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String objectToSource(Realm realm, Set<ScriptObject> stack, ScriptObject value) {
        List<String> keys = GetOwnPropertyKeys(realm, value);
        if (keys.isEmpty()) {
            return "{}";
        }
        StringBuilder properties = new StringBuilder();
        for (String k : keys) {
            String p = toSource(realm, stack, Get(value, k));
            properties.append(',').append(k).append(':').append(p);
        }
        properties.append('}').setCharAt(0, '{');
        return properties.toString();
    }

    private static String arrayToSource(Realm realm, Set<ScriptObject> stack, ScriptObject value) {
        long len = ToUint32(realm, Get(value, "length"));
        if (len <= 0) {
            return "[]";
        }
        StringBuilder properties = new StringBuilder();
        for (long index = 0; index < len; ++index) {
            String p = toSource(realm, stack, Get(value, ToString(index)));
            properties.append(',').append(p);
        }
        properties.append(']').setCharAt(0, '[');
        return properties.toString();
    }

    private String loadFile(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private Object evaluate(Realm realm, String source, String sourceName) {
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
        if (options.contains(Option.CompileOnly)) {
            EnumSet<Compiler.Option> opts = EnumSet.noneOf(Compiler.Option.class);
            if (options.contains(Option.Debug)) {
                opts.add(Compiler.Option.Debug);
            }
            return ScriptLoader.compile(className, parsedScript, opts);
        } else {
            return ScriptLoader.load(className, parsedScript);
        }
    }

    private Script script(String sourceName, String source) throws ParserException {
        String className = "typein_" + scriptCounter.incrementAndGet();
        return ScriptLoader.load(sourceName, className, source);
    }

    public static class ReplGlobalObject extends GlobalObject {
        private final long startMilli = System.currentTimeMillis();
        private final long startNano = System.nanoTime();
        private final Repl repl;

        public ReplGlobalObject(Realm realm, Repl repl) {
            super(realm);
            this.repl = repl;
        }

        private static ScriptException throwError(Realm realm, String message) {
            Object error = EvaluateConstructorCall(realm.getIntrinsic(Intrinsics.Error),
                    new Object[] { message }, realm);
            return _throw(error);
        }

        @Function(name = "options", arity = 0)
        public String options() {
            StringBuilder opts = new StringBuilder();
            for (Option opt : this.repl.options) {
                opts.append(opt).append(",");
            }
            if (opts.length() != 0) {
                opts.setLength(opts.length() - 1);
            }
            return opts.toString();
        }

        @Function(name = "load", arity = 1)
        public Object load(String file) {
            try {
                Path path = Paths.get(file);
                String source = repl.loadFile(path);
                return repl.evaluate(realm(), source, path.getFileName().toString());
            } catch (IOException e) {
                throw throwError(realm(), e.getMessage());
            }
        }

        @Function(name = "evaluate", arity = 1)
        public Object evaluate(String source) {
            return repl.evaluate(realm(), source, "string");
        }

        @Function(name = "run", arity = 1)
        public double run(String file) {
            long start = System.nanoTime();
            load(file);
            long end = System.nanoTime();
            return (double) TimeUnit.NANOSECONDS.toMillis(end - start);
        }

        @Function(name = "readline", arity = 0)
        public String readline() {
            return repl.console.readLine();
        }

        @Function(name = "print", arity = 1)
        public void print(String message) {
            repl.console.writer().println(message);
        }

        @Function(name = "printErr", arity = 1)
        public void printErr(String message) {
            System.err.println(message);
        }

        @Function(name = "putstr", arity = 1)
        public void putstr(String message) {
            repl.console.writer().print(message);
        }

        @Function(name = "dateNow", arity = 0)
        public double dateNow() {
            long elapsed = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startNano);
            double date = startMilli + TimeUnit.MICROSECONDS.toMillis(elapsed);
            double subdate = (elapsed % 1000) / 1000d;
            return date + subdate;
        }

        @Function(name = "quit", arity = 0)
        public void quit() {
            System.exit(0);
        }

        @Function(name = "assertEq", arity = 2)
        public void assertEq(Object actual, Object expected, Object message) {
            if (!SameValue(actual, expected)) {
                Realm realm = realm();
                StringBuilder msg = new StringBuilder();
                msg.append(String.format("Assertion failed: got %s, expected %s",
                        ToSource(realm, actual), ToSource(realm, expected)));
                if (!Type.isUndefined(message)) {
                    msg.append(": ").append(ToFlatString(realm, message));
                }
                throwError(realm, msg.toString());
            }
        }

        @Function(name = "setDebug", arity = 1)
        public void setDebug(boolean debug) {
            if (debug) {
                repl.options.add(Option.Debug);
            } else {
                repl.options.remove(Option.Debug);
            }
        }

        @Function(name = "throwError", arity = 0)
        public void throwError() {
            throwError(realm(), "This is an error");
        }

        @Function(name = "build", arity = 0)
        public String build() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    Repl.class.getResourceAsStream("/build-date"), StandardCharsets.UTF_8))) {
                return reader.readLine();
            } catch (IOException e) {
                throw throwError(realm(), "could not read build-date file");
            }
        }

        @Function(name = "sleep", arity = 1)
        public void sleep(double dt) {
            try {
                TimeUnit.SECONDS.sleep(ToUint32(dt));
            } catch (InterruptedException e) {
                throwError(realm(), e.getMessage());
            }
        }

        @Function(name = "snarf", arity = 1)
        public Object snarf(String file) {
            try {
                return repl.loadFile(Paths.get(file));
            } catch (IOException e) {
                throw throwError(realm(), e.getMessage());
            }
        }

        @Function(name = "read", arity = 1)
        public Object read(String file) {
            return snarf(file);
        }

        @Function(name = "elapsed", arity = 0)
        public double elapsed() {
            return (double) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startNano);
        }

        @Function(name = "getSym", arity = 1)
        public Object getSym(String name) {
            try {
                if (name.startsWith("@@")) {
                    return BuiltinSymbol.valueOf(name.substring(2)).get();
                }
            } catch (IllegalArgumentException e) {
            }
            return UNDEFINED;
        }
    }
}
