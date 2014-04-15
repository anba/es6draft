/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.repl.SourceBuilder.ToSource;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateArrayFromList;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.TerminalFactory;
import jline.TerminalSupport;
import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserEOFException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.JLineConsole;
import com.github.anba.es6draft.repl.console.LegacyConsole;
import com.github.anba.es6draft.repl.console.NativeConsole;
import com.github.anba.es6draft.repl.console.ReplConsole;
import com.github.anba.es6draft.repl.global.MozShellGlobalObject;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.repl.global.SimpleShellGlobalObject;
import com.github.anba.es6draft.repl.global.StopExecutionException;
import com.github.anba.es6draft.repl.global.StopExecutionException.Reason;
import com.github.anba.es6draft.repl.global.V8ShellGlobalObject;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Task;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Simple REPL
 */
public final class Repl {
    private static final String PROGRAM_NAME = "es6draft";
    private static final int STACKTRACE_DEPTH = 20;

    public static void main(String[] args) throws Throwable {
        try {
            EnumSet<Option> options = Option.fromArgs(args);
            StartScript startScript = StartScript.fromArgs(args);
            ReplConsole console;
            if (!options.contains(Option.NoJLine)) {
                configureTerminalFlavors();
                ConsoleReader consoleReader = new ConsoleReader(PROGRAM_NAME, new FileInputStream(
                        FileDescriptor.in), System.out, TerminalFactory.get(), getDefaultEncoding());
                consoleReader.setExpandEvents(false);
                console = new JLineConsole(consoleReader);
            } else if (System.console() != null) {
                console = new NativeConsole(System.console());
            } else {
                console = new LegacyConsole(System.out, System.in);
            }
            new Repl(options, startScript, console).loop();
        } catch (Throwable e) {
            printStackTrace(e);
            System.exit(1);
        }
    }

    private static void configureTerminalFlavors() {
        final boolean isWindows = isWindows();
        final String type = System.getProperty(TerminalFactory.JLINE_TERMINAL);
        if (isWindows && type == null) {
            TerminalFactory.registerFlavor(TerminalFactory.Flavor.WINDOWS,
                    UnsupportedTerminal.class);
        } else if (isWindows && type.equalsIgnoreCase(TerminalFactory.UNIX)) {
            TerminalFactory.registerFlavor(TerminalFactory.Flavor.UNIX, CygwinTerminal.class);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private static String getDefaultEncoding() {
        return Charset.defaultCharset().name();
    }

    public static final class CygwinTerminal extends TerminalSupport {
        private final int width, height;

        public CygwinTerminal() {
            super(true);
            String settings = System.getProperty(TerminalFactory.JLINE_TERMINAL + ".settings", "");
            width = getProperty(settings, "columns", DEFAULT_WIDTH);
            height = getProperty(settings, "rows", DEFAULT_HEIGHT);
        }

        private static int getProperty(String settings, String name, int defaultValue) {
            Matcher m = Pattern.compile(name + "\\s+(\\d{1,4})").matcher(settings);
            return m.find() ? Integer.parseInt(m.group(1)) : defaultValue;
        }

        @Override
        public void init() throws Exception {
            super.init();
            setEchoEnabled(false);
            setAnsiSupported(true);
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }

    private static void printStackTrace(Throwable e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length > STACKTRACE_DEPTH) {
            int omitted = stackTrace.length - STACKTRACE_DEPTH;
            stackTrace = Arrays.copyOf(stackTrace, STACKTRACE_DEPTH + 1);
            stackTrace[STACKTRACE_DEPTH] = new StackTraceElement("..", "", "Frames omitted",
                    omitted);
            e.setStackTrace(stackTrace);
        }
        e.printStackTrace();
    }

    private enum Option {
        NoInterpreter, Debug, FullDebug, StackTrace, Strict, SimpleShell, MozillaShell, V8Shell,
        NoJLine, NoColor, Async, VerifyStack;

        static EnumSet<Option> fromArgs(String[] args) {
            EnumSet<Option> options = EnumSet.noneOf(Option.class);
            for (String arg : args) {
                switch (arg) {
                case "--compile-only":
                case "--no-interpreter":
                    options.add(NoInterpreter);
                    break;
                case "--full-debug":
                    options.add(FullDebug);
                    // fall-thru
                case "--debug":
                    options.add(NoInterpreter);
                    options.add(Debug);
                    break;
                case "--stacktrace":
                    options.add(StackTrace);
                    break;
                case "--strict":
                    options.add(Strict);
                    break;
                case "--shell=simple":
                    options.add(SimpleShell);
                    break;
                case "--shell=mozilla":
                    options.add(MozillaShell);
                    break;
                case "--shell=v8":
                    options.add(V8Shell);
                    break;
                case "--no-jline":
                    options.add(NoJLine);
                    break;
                case "--no-color":
                    options.add(NoColor);
                    break;
                case "--async":
                    options.add(Async);
                    break;
                case "--verify-stack":
                    options.add(VerifyStack);
                    break;
                case "--help":
                    System.out.print(getHelp());
                    System.exit(0);
                    break;
                default:
                    if (arg.length() > 1 && arg.charAt(0) == '-') {
                        System.err.printf("invalid option '%s'\n\n", arg);
                        System.out.print(getHelp());
                        System.exit(0);
                    }
                    break;
                }
            }
            return options;
        }
    }

    private static String getHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s\n\n", getResourceInfo("/version", PROGRAM_NAME)));
        sb.append("Options: \n");
        sb.append("  --shell=[mode]    Set default shell emulation [simple, mozilla, v8] (default = simple)\n");
        sb.append("  --strict          Strict semantics without web compatibility\n");
        sb.append("  --async           Enable experimental support for async functions\n");
        sb.append("  --no-interpreter  Disable interpreter\n");
        sb.append("  --no-color        Disable colored output\n");
        sb.append("  --no-jline        Disable JLine support\n");
        sb.append("  --stacktrace      Print stack-trace on error\n");
        sb.append("  --debug           Print generated Java bytecode\n");
        sb.append("  --full-debug      Print generated Java bytecode (full type descriptors)\n");
        sb.append("  --help            Print this help\n");
        return sb.toString();
    }

    private static String getResourceInfo(String resourceName, String defaultValue) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Repl.class.getResourceAsStream(resourceName), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (IOException e) {
            return defaultValue;
        }
    }

    private static final class StartScript {
        private Path script = Paths.get("-");
        private List<String> arguments = new ArrayList<>();

        static StartScript fromArgs(String[] args) {
            StartScript startScript = new StartScript();
            boolean inOptions = true;
            for (String arg : args) {
                if (inOptions && arg.length() > 1 && arg.charAt(0) == '-') {
                    // skip options
                    continue;
                }
                if (inOptions) {
                    inOptions = false;
                    startScript.script = Paths.get(arg);
                } else if (!arg.isEmpty()) {
                    startScript.arguments.add(arg);
                }
            }
            return startScript;
        }
    }

    @SuppressWarnings("serial")
    private static final class ParserExceptionWithSource extends RuntimeException {
        private final String source;

        ParserExceptionWithSource(ParserException e, String source) {
            super(e);
            this.source = source;
        }

        @Override
        public ParserException getCause() {
            return (ParserException) super.getCause();
        }

        public String getSource() {
            return source;
        }
    }

    private final EnumSet<Option> options;
    private final StartScript startScript;
    private final ReplConsole console;
    private AtomicInteger scriptCounter = new AtomicInteger(0);

    private Repl(EnumSet<Option> options, StartScript startScript, ReplConsole console) {
        this.options = options;
        this.startScript = startScript;
        this.console = console;
    }

    private void handleException(Throwable e) {
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        console.printf("%s%n", message);
        if (options.contains(Option.StackTrace)) {
            printStackTrace(e);
        }
    }

    private void handleException(Realm realm, ScriptException e) {
        console.printf("uncaught exception: %s%n", e.getMessage(realm.defaultContext()));
        if (options.contains(Option.StackTrace)) {
            printStackTrace(e);
        }
    }

    private void handleException(ParserExceptionWithSource exception, int lineOffset) {
        ParserException e = exception.getCause();
        String source = exception.getSource();

        String sourceInfo = String.format("%s:%d:%d", e.getFile(), e.getLine(), e.getColumn());
        int start = skipLines(source, e.getLine() - lineOffset);
        int end = nextLineTerminator(source, start);
        String offendingLine = source.substring(start, end);
        String marker = Strings.repeat('.', Math.max(e.getColumn() - 1, 0)) + '^';

        console.printf("%s %s: %s%n", sourceInfo, e.getType(), e.getFormattedMessage());
        console.printf("%s %s%n", sourceInfo, offendingLine);
        console.printf("%s %s%n", sourceInfo, marker);
        if (options.contains(Option.StackTrace)) {
            printStackTrace(e);
        }
    }

    private static int skipLines(String s, int n) {
        int index = 0;
        for (int length = s.length(); n > 0; --n) {
            int lineEnd = nextLineTerminator(s, index);
            if (lineEnd + 1 < length && s.charAt(lineEnd) == '\r' && s.charAt(lineEnd + 1) == '\n') {
                index = lineEnd + 2;
            } else {
                index = lineEnd + 1;
            }
        }
        return index;
    }

    private static int nextLineTerminator(String s, int index) {
        for (int length = s.length(); index < length && !Strings.isLineTerminator(s.charAt(index)); ++index) {
        }
        return index;
    }

    /**
     * REPL: Read
     * 
     * @param realm
     *            the realm instance
     * @param line
     *            the current line
     * @return the parsed script node
     */
    private com.github.anba.es6draft.ast.Script read(Realm realm, int line) {
        StringBuilder source = new StringBuilder();
        for (String prompt = "js> ";; prompt = "") {
            String s = console.readLine(prompt);
            if (s == null) {
                continue;
            }
            source.append(s).append('\n');
            try {
                Parser parser = new Parser("typein", line, realm.getOptions());
                return parser.parseScript(source);
            } catch (ParserEOFException e) {
                continue;
            } catch (ParserException e) {
                throw new ParserExceptionWithSource(e, source.toString());
            }
        }
    }

    /**
     * REPL: Eval
     * 
     * @param realm
     *            the realm instance
     * @param parsedScript
     *            the parsed script node
     * @return the evaluated script result
     */
    private Object eval(Realm realm, com.github.anba.es6draft.ast.Script parsedScript) {
        String className = "typein_" + scriptCounter.incrementAndGet();
        Script script;
        if (options.contains(Option.NoInterpreter)) {
            script = ScriptLoader.compile(realm, parsedScript, className);
        } else {
            script = ScriptLoader.load(realm, parsedScript, className);
        }
        return ScriptLoader.ScriptEvaluation(script, realm, false);
    }

    /**
     * REPL: Print
     * 
     * @param realm
     *            the realm instance
     * @param result
     *            the object to be printed
     */
    private void print(Realm realm, Object result) {
        if (result != UNDEFINED) {
            boolean color = console.isAnsiSupported() && !options.contains(Option.NoColor);
            SourceBuilder.Mode mode = color ? SourceBuilder.Mode.Color : SourceBuilder.Mode.Simple;
            console.printf("%s%n", ToSource(mode, realm.defaultContext(), result));
        }
    }

    /**
     * REPL: Loop
     */
    private void loop() {
        Realm realm = newRealm();
        for (int line = 1;; line += 1) {
            drainTaskQueue(realm);
            try {
                com.github.anba.es6draft.ast.Script parsedScript = read(realm, line);
                if (parsedScript.getStatements().isEmpty()) {
                    continue;
                }
                Object result = eval(realm, parsedScript);
                print(realm, result);
            } catch (StopExecutionException e) {
                if (e.getReason() == Reason.Quit) {
                    System.exit(0);
                }
            } catch (ParserExceptionWithSource e) {
                handleException(e, line);
            } catch (ScriptException e) {
                handleException(realm, e);
            } catch (ParserException | CompilationException | StackOverflowError e) {
                handleException(e);
            } catch (BootstrapMethodError | UncheckedIOException e) {
                handleException(e.getCause());
            }
        }
    }

    private void drainTaskQueue(Realm realm) {
        World<?> world = realm.getWorld();
        while (world.hasPendingTasks()) {
            try {
                world.executeTasks();
            } catch (StopExecutionException e) {
                if (e.getReason() == Reason.Quit) {
                    System.exit(0);
                }
            } catch (ParserExceptionWithSource e) {
                handleException(e, 1);
            } catch (ScriptException e) {
                handleException(realm, e);
            } catch (ParserException | CompilationException | StackOverflowError e) {
                handleException(e);
            } catch (BootstrapMethodError | UncheckedIOException e) {
                handleException(e.getCause());
            }
        }
    }

    private Realm newRealm() {
        ReplConsole console = this.console;
        Path baseDir = Paths.get("").toAbsolutePath();
        Path script = Paths.get("./.");
        Set<CompatibilityOption> compatibilityOptions;
        if (options.contains(Option.Strict)) {
            compatibilityOptions = CompatibilityOption.StrictCompatibility();
        } else if (options.contains(Option.MozillaShell)) {
            compatibilityOptions = CompatibilityOption.MozCompatibility();
        } else {
            compatibilityOptions = CompatibilityOption.WebCompatibility();
        }
        if (options.contains(Option.Async)) {
            compatibilityOptions.add(CompatibilityOption.AsyncFunction);
        }
        Set<Parser.Option> parserOptions = EnumSet.noneOf(Parser.Option.class);
        Set<Compiler.Option> compilerOptions = EnumSet.noneOf(Compiler.Option.class);
        if (options.contains(Option.Debug)) {
            compilerOptions.add(Compiler.Option.Debug);
        }
        if (options.contains(Option.FullDebug)) {
            compilerOptions.add(Compiler.Option.FullDebug);
        }
        if (options.contains(Option.VerifyStack)) {
            compilerOptions.add(Compiler.Option.VerifyStack);
        }
        ScriptCache scriptCache = new ScriptCache(compatibilityOptions, parserOptions,
                compilerOptions);

        ObjectAllocator<? extends ShellGlobalObject> allocator;
        if (options.contains(Option.MozillaShell)) {
            allocator = MozShellGlobalObject.newGlobalObjectAllocator(console, baseDir, script,
                    scriptCache);
        } else if (options.contains(Option.V8Shell)) {
            allocator = V8ShellGlobalObject.newGlobalObjectAllocator(console, baseDir, script,
                    scriptCache);
        } else {
            allocator = SimpleShellGlobalObject.newGlobalObjectAllocator(console, baseDir, script,
                    scriptCache);
        }

        World<? extends ShellGlobalObject> world = new World<>(allocator, compatibilityOptions,
                compilerOptions);
        final ShellGlobalObject global = world.newGlobal();
        final Realm realm = global.getRealm();
        ExecutionContext cx = realm.defaultContext();

        // Add completion to console
        console.addCompletion(realm);

        // Add global "arguments" property
        ScriptObject arguments = CreateArrayFromList(cx, startScript.arguments);
        global.defineOwnProperty(cx, "arguments", new PropertyDescriptor(arguments, true, false,
                true));

        // Execute any global specific initialisation scripts
        realm.enqueueLoadingTask(new Task() {
            @Override
            public void execute() {
                try {
                    global.initialize(global);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (URISyntaxException e) {
                    throw new UncheckedIOException(new IOException(e));
                }
            }
        });

        // Run start script, if defined
        if (!startScript.script.toString().equals("-")) {
            final Path startScriptName = startScript.script.getFileName();
            final Path startScriptPath = baseDir.resolve(startScript.script);
            realm.enqueueLoadingTask(new Task() {
                @Override
                public void execute() {
                    try {
                        try {
                            global.eval(startScriptName, startScriptPath);
                        } catch (ParserException e) {
                            byte[] content = Files.readAllBytes(startScriptPath);
                            String source = new String(content, StandardCharsets.UTF_8);
                            throw new ParserExceptionWithSource(e, source);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        }

        return realm;
    }
}
