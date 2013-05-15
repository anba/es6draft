/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.repl.SourceBuilder.ToSource;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserEOFException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.StopExecutionException.Reason;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Realm.GlobalObjectCreator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 * Simple REPL
 */
public class Repl {
    public static void main(String[] args) {
        new Repl(Option.fromArgs(args), System.console()).loop();
    }

    private enum Option {
        CompileOnly, Debug, Simple;

        static EnumSet<Option> fromArgs(String[] args) {
            EnumSet<Option> options = EnumSet.noneOf(Option.class);
            for (String arg : args) {
                switch (arg) {
                case "--compile-only":
                    options.add(CompileOnly);
                    break;
                case "--debug":
                    options.add(CompileOnly);
                    options.add(Debug);
                    break;
                case "--simple":
                    options.add(Simple);
                    break;
                case "--help":
                    System.out.print(getHelp());
                    System.exit(0);
                    break;
                default:
                    System.err.printf("invalid option '%s'\n", arg);
                }
            }
            return options;
        }
    }

    private static String getHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("es6draft (%s)\n\n", getBuildDate()));
        sb.append("Options: \n");
        sb.append("  --compile-only    Disable interpreter\n");
        sb.append("  --debug           Print generated Java bytecode\n");
        sb.append("  --simple          Do not load extended shell\n");
        sb.append("  --help            Print this help\n");
        return sb.toString();
    }

    private static String getBuildDate() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Repl.class.getResourceAsStream("/build-date"), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (IOException e) {
            return "<unknown build>";
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
    private void print(ExecutionContext cx, Object result) {
        try {
            if (result != UNDEFINED) {
                console.writer().println(ToSource(cx, result));
            }
        } catch (ScriptException e) {
            console.printf("uncaught exception: %s\n", e.getMessage());
        }
    }

    /**
     * REPL: Loop
     */
    private void loop() {
        GlobalObject global = newGlobal();
        Realm realm = global.getRealm();
        ExecutionContext cx = realm.defaultContext();
        for (int line = 1;; line += 1) {
            try {
                console.printf("js> ");
                com.github.anba.es6draft.ast.Script parsedScript = read(line);
                if (parsedScript == null) {
                    continue;
                }
                Object result = eval(realm, parsedScript);
                if (result == null) {
                    continue;
                }
                print(cx, result);
            } catch (StopExecutionException e) {
                if (e.getReason() == Reason.Quit) {
                    System.exit(0);
                }
            }
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

    private GlobalObject newGlobal() {
        if (options.contains(Option.Simple)) {
            return newSimpleGlobal();
        } else {
            return newExtendedGlobal();
        }
    }

    private GlobalObject newSimpleGlobal() {
        Realm realm = Realm.newRealm(new GlobalObjectCreator<GlobalObject>() {
            @Override
            public GlobalObject createGlobal(Realm realm) {
                return new GlobalObject(realm);
            }
        });
        return realm.getGlobalThis();
    }

    private GlobalObject newExtendedGlobal() {
        Path baseDir = Paths.get("").toAbsolutePath();
        Path script = Paths.get("./.");
        Path libDir = Paths.get("");
        ReplConsole console = new ReplConsole(this.console);
        ScriptCache scriptCache = new ScriptCache();
        Script initScript = null;
        try {
            initScript = MozShellGlobalObject.compileLegacy(scriptCache);
        } catch (ParserException | IOException e) {
            System.err.println(e);
        }
        MozShellGlobalObject global = MozShellGlobalObject.newGlobal(console, baseDir, script,
                libDir, scriptCache, initScript);

        return global;
    }

    private static class ReplConsole implements MozShellConsole {
        private Console console;

        ReplConsole(Console console) {
            this.console = console;
        }

        @Override
        public String readLine() {
            return console.readLine();
        }

        @Override
        public void putstr(String s) {
            console.writer().print(s);
        }

        @Override
        public void print(String s) {
            console.writer().println(s);
        }

        @Override
        public void printErr(String s) {
            System.err.println(s);
        }
    }
}
