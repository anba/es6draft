/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateArrayFromList;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.ModuleEvaluationJob;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.RestOfArgumentsHandler;
import org.kohsuke.args4j.spi.Setter;
import org.kohsuke.args4j.spi.StopOptionHandler;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.Scripts;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Characters;
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
import com.github.anba.es6draft.runtime.extensions.timer.Timers;
import com.github.anba.es6draft.runtime.internal.*;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Simple REPL
 */
public final class Repl {
    private static final String BUNDLE_NAME = "com.github.anba.es6draft.repl.messages";
    private static final String PROGRAM_NAME = "es6draft";
    private static final String PROMPT = "js> ";
    private static final int STACKTRACE_DEPTH = 20;

    public static void main(String[] args) throws Throwable {
        Options options = new Options();
        try {
            parseOptions(options, args);
            ReplConsole console = createConsole(options);
            new Repl(console, options).loop();
        } catch (Throwable e) {
            printStackTrace(e, options.stacktraceDepth);
            System.exit(1);
        }
    }

    private static ReplConsole createConsole(Options options) throws IOException {
        ReplConsole console;
        if (!options.noJLine) {
            console = new JLineConsole(PROGRAM_NAME);
        } else if (System.console() != null) {
            console = new NativeConsole();
        } else {
            console = new LegacyConsole();
        }
        return console;
    }

    private static void printStackTrace(Throwable e, Options options) {
        if (options.stacktrace) {
            printStackTrace(e, options.stacktraceDepth);
        }
    }

    private static void printStackTrace(Throwable e, int maxDepth) {
        final int depth = Math.max(maxDepth, 0);
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length > depth) {
            int omitted = stackTrace.length - depth;
            stackTrace = Arrays.copyOf(stackTrace, depth + 1);
            stackTrace[depth] = new StackTraceElement("..", "", "Frames omitted", omitted);
            e.setStackTrace(stackTrace);
        }
        e.printStackTrace();
    }

    private static void parseOptions(Options options, String[] args) {
        ParserProperties properties = ParserProperties.defaults().withUsageWidth(120);
        CmdLineParser parser = new CmdLineParser(options, properties);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println(getUsageString(parser, false));
            System.exit(1);
        }
        if (options.showVersion) {
            System.out.println(getVersionString());
            System.exit(0);
        }
        if (options.showHelp) {
            System.out.println(getUsageString(parser, false));
            System.exit(0);
        }
        if (options.showExtendedHelp) {
            System.out.println(getUsageString(parser, true));
            System.exit(0);
        }
        if (options.debug || options.fullDebug || options.debugInfo) {
            // Disable interpreter when bytecode is requested
            options.noInterpreter = true;
        }
        if (options.fileName != null) {
            // Execute as last script
            if (options.fileName.toString().equals("-")) {
                // "-" is a short-hand to request reading from System.in
                if (System.console() == null) {
                    // System.in is not interactive
                    options.evalScripts.add(new EvalString(read(System.in)));
                } else {
                    options.interactive = true;
                }
            } else {
                options.evalScripts.add(new EvalPath(options.fileName));
            }
        }
        if (options.evalScripts.isEmpty()) {
            // Default to interactive mode when no files or expressions were set
            options.interactive = true;
        }
    }

    private static interface EvalScript {
        Source getSource();

        String getSourceCode() throws IOException;

        SourceIdentifier getModuleName();

        ModuleSource getModuleSource() throws IOException;
    }

    private static final class EvalString implements EvalScript {
        private static final AtomicInteger moduleIds = new AtomicInteger(0);
        private final String sourceCode;

        EvalString(String sourceCode) {
            this.sourceCode = sourceCode;
        }

        @Override
        public Source getSource() {
            return new Source(Paths.get(".").toAbsolutePath(), "<eval>", 1);
        }

        @Override
        public String getSourceCode() {
            return sourceCode;
        }

        @Override
        public SourceIdentifier getModuleName() {
            return new EvalSourceIdentifier("eval-module-" + moduleIds.incrementAndGet(),
                    URI.create(""));
        }

        @Override
        public ModuleSource getModuleSource() {
            return new EvalModuleSource(getSourceCode(), getSource());
        }
    }

    private static final class EvalPath implements EvalScript {
        private final Path path;

        EvalPath(Path path) {
            this.path = path;
        }

        @Override
        public Source getSource() {
            return new Source(path.toAbsolutePath(), path.toString(), 1);
        }

        @Override
        public String getSourceCode() throws IOException {
            Path filePath = path.toAbsolutePath();
            if (!Files.exists(filePath)) {
                String message = formatMessage("file_not_found", filePath.toString());
                throw new FileNotFoundException(message);
            }
            byte[] content = Files.readAllBytes(filePath);
            return new String(content, StandardCharsets.UTF_8);
        }

        @Override
        public SourceIdentifier getModuleName() {
            URI file = Paths.get("").toAbsolutePath().toUri().relativize(path.toUri());
            return new EvalSourceIdentifier(file.toString(), file);
        }

        @Override
        public ModuleSource getModuleSource() throws IOException {
            return new EvalModuleSource(getSourceCode(), getSource());
        }
    }

    private static final class EvalSourceIdentifier implements SourceIdentifier {
        private final String name;
        private final URI uri;

        EvalSourceIdentifier(String name, URI uri) {
            this.name = name;
            this.uri = uri;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof EvalSourceIdentifier) {
                return name.equals(((EvalSourceIdentifier) obj).name);
            }
            if (obj instanceof SourceIdentifier) {
                return uri.equals(((SourceIdentifier) obj).toUri());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public URI toUri() {
            return uri;
        }
    }

    private static final class EvalModuleSource implements ModuleSource {
        private final String sourceCode;
        private final Source source;

        public EvalModuleSource(String sourceCode, Source source) {
            this.sourceCode = sourceCode;
            this.source = source;
        }

        @Override
        public String sourceCode() {
            return sourceCode;
        }

        @Override
        public Source toSource() {
            return source;
        }
    }

    public enum ShellMode {
        // TODO: "simple" is a misnomer, change to "standard"?
        Simple, Mozilla, V8;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final class Options {
        ArrayList<EvalScript> evalScripts = new ArrayList<>();

        @Option(name = "-v", aliases = { "--version" }, usage = "options.version")
        boolean showVersion;

        @Option(name = "-h", aliases = { "--help" }, help = true, usage = "options.help")
        boolean showHelp;

        @Option(name = "-e", aliases = { "--eval", "--execute" }, metaVar = "meta.string",
                usage = "options.eval")
        void setEvalExpression(String expression) {
            evalScripts.add(new EvalString(expression));
        }

        @Option(name = "-f", aliases = { "--file" }, metaVar = "meta.file", usage = "options.file")
        void setFile(Path path) {
            evalScripts.add(new EvalPath(path));
        }

        @Option(name = "-i", aliases = { "--interactive" }, usage = "options.interactive")
        boolean interactive;

        @Option(name = "--module", usage = "options.module")
        boolean module;

        @Option(name = "--strict", usage = "options.strict")
        boolean strict;

        @Option(name = "--shell", usage = "options.shell")
        ShellMode shellMode = ShellMode.Simple;

        @Option(name = "--async", usage = "options.async")
        boolean asyncFunctions;

        @Option(name = "--es7", usage = "options.es7")
        boolean ecmascript7;

        @Option(name = "--parser", usage = "options.parser")
        boolean parser;

        @Option(name = "--timers", usage = "options.timers")
        boolean timers;

        @Option(name = "--no-jline", usage = "options.no_jline")
        boolean noJLine;

        @Option(name = "--no-color", usage = "options.no_color")
        boolean noColor;

        @Option(name = "--no-interpreter", aliases = { "--compile-only" },
                usage = "options.no_interpreter")
        boolean noInterpreter;

        @Option(name = "--stacktrace", usage = "options.stacktrace")
        boolean stacktrace;

        @Option(name = "--stacktrace-depth", hidden = true, usage = "options.stacktrace_depth")
        int stacktraceDepth = STACKTRACE_DEPTH;

        @Option(name = "--debug", usage = "options.debug")
        boolean debug;

        @Option(name = "--full-debug", hidden = true, usage = "options.full_debug")
        boolean fullDebug;

        @Option(name = "--debug-info", hidden = true, usage = "options.debug_info")
        boolean debugInfo;

        @Option(name = "--no-resume", hidden = true, usage = "options.no_resume")
        boolean noResume;

        @Option(name = "--no-tailcall", hidden = true, usage = "options.no_tailcall")
        boolean noTailCall;

        @Option(name = "--native-calls", hidden = true, usage = "options.native_calls")
        boolean nativeCalls;

        @Option(name = "--promise-rejection", hidden = true, usage = "options.promise_rejection")
        boolean promiseRejection;

        @Option(name = "--xhelp", help = true, hidden = true, usage = "options.extended_help")
        boolean showExtendedHelp;

        @Option(name = "-", handler = StopOptionAndRepeatHandler.class)
        @Argument(index = 0, multiValued = false, metaVar = "meta.file", usage = "options.filename")
        Path fileName = null;

        @Option(name = "--", handler = StopOptionAndConsumeRestHandler.class)
        @Argument(index = 1, multiValued = true, metaVar = "meta.arguments",
                usage = "options.arguments", handler = RestOfArgumentsHandler.class)
        List<String> arguments = new ArrayList<>();
    }

    public static final class StopOptionAndConsumeRestHandler extends OptionHandler<String> {
        private final StopOptionHandler stopOptionHandler;
        private final RestOfArgumentsHandler restOfArgumentsHandler;

        public StopOptionAndConsumeRestHandler(CmdLineParser parser, OptionDef option,
                Setter<String> setter) {
            super(parser, option, setter);
            this.stopOptionHandler = new StopOptionHandler(parser, option, setter);
            this.restOfArgumentsHandler = new RestOfArgumentsHandler(parser, option, setter);
        }

        @Override
        public int parseArguments(Parameters params) throws CmdLineException {
            return stopOptionHandler.parseArguments(params)
                    + restOfArgumentsHandler.parseArguments(params);
        }

        @Override
        public String getDefaultMetaVariable() {
            return "ARGUMENTS";
        }
    }

    public static final class StopOptionAndRepeatHandler extends OptionHandler<String> {
        private final StopOptionHandler stopOptionHandler;

        public StopOptionAndRepeatHandler(CmdLineParser parser, OptionDef option,
                Setter<String> setter) {
            super(parser, option, setter);
            this.stopOptionHandler = new StopOptionHandler(parser, option, setter);
        }

        @Override
        public int parseArguments(Parameters params) throws CmdLineException {
            stopOptionHandler.parseArguments(params);
            return -1;
        }

        @Override
        public String getDefaultMetaVariable() {
            return "";
        }
    }

    private static String getUsageString(CmdLineParser parser, boolean showAll) {
        ResourceBundle rb = getResourceBundle();
        StringWriter writer = new StringWriter();
        writer.write(formatMessage(rb, "usage", getVersionString(), PROGRAM_NAME));
        parser.printUsage(writer, rb, showAll ? OptionHandlerFilter.ALL
                : OptionHandlerFilter.PUBLIC);
        return writer.toString();
    }

    private static ResourceBundle getResourceBundle() {
        ResourceBundle.Control control = new PropertiesReaderControl(StandardCharsets.UTF_8);
        return new XResourceBundle(ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault(),
                control));
    }

    private static final class XResourceBundle extends ResourceBundle {
        private final ResourceBundle bundle;

        XResourceBundle(ResourceBundle bundle) {
            this.bundle = bundle;
        }

        @Override
        protected Object handleGetObject(String key) {
            if (bundle.containsKey(key)) {
                return bundle.getObject(key);
            }
            // TODO: Workarounds for https://github.com/kohsuke/args4j/issues/70
            // and https://github.com/kohsuke/args4j/issues/71
            return key;
        }

        @Override
        public Enumeration<String> getKeys() {
            return bundle.getKeys();
        }

        @Override
        public Locale getLocale() {
            return bundle.getLocale();
        }

        @Override
        public boolean containsKey(String key) {
            return bundle.containsKey(key);
        }

        @Override
        public Set<String> keySet() {
            return bundle.keySet();
        }
    }

    private static String read(InputStream in) {
        try (Reader reader = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()))) {
            StringBuilder sb = new StringBuilder(4096);
            char cbuf[] = new char[4096];
            for (int len; (len = reader.read(cbuf)) != -1;) {
                sb.append(cbuf, 0, len);
            }
            return sb.toString();
        } catch (IOException e) {
            System.err.println(e);
            return "";
        }
    }

    private static String formatMessage(String key, String... messageArguments) {
        return formatMessage(getResourceBundle(), key, messageArguments);
    }

    private static String formatMessage(ResourceBundle rb, String key, String... messageArguments) {
        return new MessageFormat(rb.getString(key), rb.getLocale()).format(messageArguments);
    }

    private static String getVersionString() {
        return getResourceInfo("/version", PROGRAM_NAME);
    }

    private static String getResourceInfo(String resourceName, String defaultValue) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Repl.class.getResourceAsStream(resourceName), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (IOException e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("serial")
    private static final class ParserExceptionWithSource extends RuntimeException {
        private final Source source;
        private final String sourceCode;

        ParserExceptionWithSource(ParserException e, Source source, String sourceCode) {
            super(e);
            this.source = source;
            this.sourceCode = sourceCode;
        }

        @Override
        public ParserException getCause() {
            return (ParserException) super.getCause();
        }

        public Source getSource() {
            return source;
        }

        public String getSourceCode() {
            return sourceCode;
        }
    }

    private final ReplConsole console;
    private final Options options;
    private final SourceBuilder sourceBuilder;
    private final AtomicInteger scriptCounter = new AtomicInteger(0);

    private Repl(ReplConsole console, Options options) {
        this.console = console;
        this.options = options;
        this.sourceBuilder = new SourceBuilder(console.isAnsiSupported() && !options.noColor, 10,
                30, 80);
    }

    private void handleException(Throwable e) {
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        console.printf("%s%n", message);
        printStackTrace(e, options);
    }

    private void handleException(Realm realm, ScriptException e) {
        String message = formatMessage("uncaught_exception", e.getMessage(realm.defaultContext()));
        console.printf("%s%n", message);
        printStackTrace(e, options);
    }

    private void handleException(Realm realm, UnhandledRejectionException e) {
        String message = formatMessage("unhandled_rejection", e.getMessage(realm.defaultContext()));
        console.printf("%s%n", message);
        printStackTrace(e.getCauseIfPresent(), options);
    }

    private void handleException(ParserExceptionWithSource exception) {
        ParserException e = exception.getCause();
        String sourceCode = exception.getSourceCode();
        int lineOffset = exception.getSource().getLine();

        String sourceInfo = String.format("%s:%d:%d", e.getFile(), e.getLine(), e.getColumn());
        int start = skipLines(sourceCode, e.getLine() - lineOffset);
        int end = nextLineTerminator(sourceCode, start);
        String offendingLine = sourceCode.substring(start, end);
        String marker = Strings.repeat('.', Math.max(e.getColumn() - 1, 0)) + '^';

        console.printf("%s %s: %s%n", sourceInfo, e.getType(), e.getFormattedMessage());
        console.printf("%s %s%n", sourceInfo, offendingLine);
        console.printf("%s %s%n", sourceInfo, marker);
        printStackTrace(e, options);
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
        for (int length = s.length(); index < length
                && !Characters.isLineTerminator(s.charAt(index)); ++index) {
        }
        return index;
    }

    private static com.github.anba.es6draft.ast.Script parse(Realm realm, Source source,
            String sourceCode) throws ParserException {
        return realm.getScriptLoader().parseScript(source, sourceCode);
    }

    /**
     * REPL: Read
     * 
     * @param realm
     *            the realm instance
     * @param line
     *            the current line
     * @return the parsed script node or {@coden null} if the end of stream has been reached
     */
    private com.github.anba.es6draft.ast.Script read(Realm realm, int line) {
        StringBuilder sourceCode = new StringBuilder();
        for (String prompt = PROMPT;; prompt = "") {
            String s = console.readLine(prompt);
            if (s == null) {
                return null;
            }
            sourceCode.append(s).append('\n');
            Source source = new Source(Paths.get(".").toAbsolutePath(), "typein", line);
            try {
                return parse(realm, source, sourceCode.toString());
            } catch (ParserEOFException e) {
                continue;
            } catch (ParserException e) {
                throw new ParserExceptionWithSource(e, source, sourceCode.toString());
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
        String className = "#typein_" + scriptCounter.incrementAndGet();
        Script script;
        if (options.noInterpreter) {
            script = realm.getScriptLoader().compile(parsedScript, className);
        } else {
            script = realm.getScriptLoader().load(parsedScript, className);
        }
        return Scripts.ScriptEvaluation(script, realm);
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
            console.printf("%s%n", sourceBuilder.toSource(realm.defaultContext(), result));
        }
    }

    /**
     * REPL: Loop
     */
    private void loop() throws InterruptedException {
        Realm realm = newRealm();
        World<?> world = realm.getWorld();
        TaskSource taskSource = createTaskSource(realm);
        for (;;) {
            try {
                world.runEventLoop(taskSource);
                return;
            } catch (StopExecutionException e) {
                if (e.getReason() == Reason.Quit) {
                    System.exit(0);
                }
            } catch (ParserExceptionWithSource e) {
                handleException(e);
            } catch (ScriptException e) {
                handleException(realm, e);
            } catch (UnhandledRejectionException e) {
                handleException(realm, e);
            } catch (InternalException | StackOverflowError e) {
                handleException(e);
            } catch (BootstrapMethodError | UncheckedIOException e) {
                handleException(e.getCause());
            }
        }
    }

    private TaskSource createTaskSource(Realm realm) {
        ArrayList<TaskSource> sources = new ArrayList<>();
        if (options.interactive) {
            sources.add(new InteractiveTaskSource(realm));
        }
        if (options.timers) {
            sources.add(createTimersTaskSource(realm));
        }
        switch (sources.size()) {
        case 0:
            return new EmptyTaskSource();
        case 1:
            return sources.get(0);
        default:
            return new MultiTaskSource(sources);
        }
    }

    private static Timers createTimersTaskSource(Realm realm) {
        Timers timers = new Timers();
        Properties.createProperties(realm.defaultContext(), realm.getGlobalThis(), timers,
                Timers.class);
        return timers;
    }

    private static final class EmptyTaskSource implements TaskSource {
        @Override
        public Task nextTask() {
            return null;
        }

        @Override
        public Task awaitTask() {
            throw new IllegalStateException();
        }
    }

    private final class InteractiveTaskSource implements TaskSource {
        private final Realm realm;
        private int line = 0;

        InteractiveTaskSource(Realm realm) {
            this.realm = realm;
        }

        @Override
        public Task nextTask() throws InterruptedException {
            return awaitTask();
        }

        @Override
        public Task awaitTask() throws InterruptedException {
            for (;;) {
                try {
                    com.github.anba.es6draft.ast.Script parsedScript = read(realm, ++line);
                    if (parsedScript == null) {
                        return null;
                    }
                    if (parsedScript.getStatements().isEmpty()) {
                        continue;
                    }
                    return new EvalPrintTask(realm, parsedScript);
                } catch (RuntimeException e) {
                    return new ThrowExceptionTask<>(e);
                } catch (Error e) {
                    return new ThrowErrorTask<>(e);
                }
            }
        }

        private final class EvalPrintTask implements Task {
            private final Realm realm;
            private final com.github.anba.es6draft.ast.Script parsedScript;

            EvalPrintTask(Realm realm, com.github.anba.es6draft.ast.Script parsedScript) {
                this.realm = realm;
                this.parsedScript = parsedScript;
            }

            @Override
            public void execute() {
                Object result = eval(realm, parsedScript);
                print(realm, result);
            }
        }

        private final class ThrowErrorTask<E extends Error> implements Task {
            private final E exception;

            ThrowErrorTask(E exception) {
                this.exception = exception;
            }

            @Override
            public void execute() throws E {
                throw exception;
            }
        }

        private final class ThrowExceptionTask<E extends RuntimeException> implements Task {
            private final E exception;

            ThrowExceptionTask(E exception) {
                this.exception = exception;
            }

            @Override
            public void execute() throws E {
                throw exception;
            }
        }
    }

    private static final class MultiTaskSource implements TaskSource {
        private final SynchronousQueue<Task> queue = new SynchronousQueue<>();
        private final Semaphore sem = new Semaphore(-1);

        MultiTaskSource(List<TaskSource> sources) {
            ExecutorService service = Executors.newFixedThreadPool(sources.size());
            for (TaskSource source : sources) {
                service.submit(new TaskRunner(source));
            }
            service.shutdown();
        }

        @Override
        public Task nextTask() throws InterruptedException {
            return awaitTask();
        }

        @Override
        public Task awaitTask() throws InterruptedException {
            sem.release();
            return queue.take();
        }

        private class TaskRunner implements Runnable {
            private final TaskSource source;

            TaskRunner(TaskSource source) {
                this.source = source;
            }

            @Override
            public void run() {
                for (;;) {
                    try {
                        queue.put(source.awaitTask());
                        sem.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Throwable t) {
                        System.err.println("Unexpected exception: " + t.getMessage());
                        t.printStackTrace();
                        System.exit(1);
                    }
                }
            }
        }
    }

    private Realm newRealm() {
        ReplConsole console = this.console;
        Path baseDir = Paths.get("").toAbsolutePath();
        Path script = Paths.get("./.");
        Set<CompatibilityOption> compatibilityOptions = compatibilityOptions(options);
        EnumSet<Parser.Option> parserOptions = parserOptions(options);
        EnumSet<Compiler.Option> compilerOptions = compilerOptions(options);

        ScriptCache scriptCache = new ScriptCache();
        ObjectAllocator<? extends ShellGlobalObject> allocator;
        if (options.shellMode == ShellMode.Mozilla) {
            allocator = MozShellGlobalObject.newGlobalObjectAllocator(console, baseDir, script,
                    scriptCache);
        } else if (options.shellMode == ShellMode.V8) {
            allocator = V8ShellGlobalObject.newGlobalObjectAllocator(console, baseDir, script,
                    scriptCache);
        } else {
            allocator = SimpleShellGlobalObject.newGlobalObjectAllocator(console, baseDir, script,
                    scriptCache);
        }
        ScriptLoader scriptLoader = new ScriptLoader(compatibilityOptions, parserOptions,
                compilerOptions);
        ModuleLoader moduleLoader = new FileModuleLoader(scriptLoader, baseDir);

        World<? extends ShellGlobalObject> world = new World<>(allocator, moduleLoader,
                scriptLoader);
        final ShellGlobalObject global = world.newGlobal();
        final Realm realm = global.getRealm();
        ScriptObject globalThis = realm.getGlobalThis();
        ExecutionContext cx = realm.defaultContext();

        // Add completion to console
        console.addCompletion(realm);

        // Add global "arguments" property
        ScriptObject arguments = CreateArrayFromList(cx, options.arguments);
        globalThis.defineOwnProperty(cx, "arguments", new PropertyDescriptor(arguments, true,
                false, true));

        // Execute any global specific initialization
        realm.enqueueScriptTask(new Task() {
            @Override
            public void execute() {
                try {
                    global.initializeHostDefinedRealm();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (URISyntaxException e) {
                    throw new UncheckedIOException(new IOException(e));
                }
            }
        });

        // Run eval expressions and files
        for (EvalScript evalScript : options.evalScripts) {
            if (options.module) {
                realm.enqueueScriptTask(new ModuleEvaluationTask(realm, evalScript));
            } else {
                realm.enqueueScriptTask(new ScriptEvaluationTask(realm, evalScript));
            }
        }

        return realm;
    }

    private final class ScriptEvaluationTask implements Task {
        private final Realm realm;
        private final EvalScript evalScript;

        ScriptEvaluationTask(Realm realm, EvalScript evalScript) {
            this.realm = realm;
            this.evalScript = evalScript;
        }

        @Override
        public void execute() {
            try {
                Source source = evalScript.getSource();
                String sourceCode = evalScript.getSourceCode();
                try {
                    eval(realm, parse(realm, source, sourceCode));
                } catch (ParserException e) {
                    throw new ParserExceptionWithSource(e, source, sourceCode);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static final class ModuleEvaluationTask implements Task {
        private final Realm realm;
        private final EvalScript evalScript;

        ModuleEvaluationTask(Realm realm, EvalScript evalScript) {
            this.realm = realm;
            this.evalScript = evalScript;
        }

        @Override
        public void execute() {
            try {
                ModuleSource moduleSource = evalScript.getModuleSource();
                SourceIdentifier moduleName = evalScript.getModuleName();
                try {
                    ModuleEvaluationJob(realm, moduleName, moduleSource);
                } catch (ParserException e) {
                    Source source = moduleSource.toSource();
                    String file = e.getFile();
                    if (file.equals(source.getFileString())) {
                        throw new ParserExceptionWithSource(e, source, moduleSource.sourceCode());
                    }
                    Path filePath = Paths.get(file).toAbsolutePath();
                    Source errorSource = new Source(filePath, file, 1);
                    String code = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
                    throw new ParserExceptionWithSource(e, errorSource, code);
                }
            } catch (MalformedNameException | ResolutionException e) {
                throw e.toScriptException(realm.defaultContext());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static Set<CompatibilityOption> compatibilityOptions(Options options) {
        Set<CompatibilityOption> compatibilityOptions;
        if (options.strict) {
            compatibilityOptions = CompatibilityOption.StrictCompatibility();
        } else if (options.shellMode == ShellMode.Mozilla) {
            compatibilityOptions = CompatibilityOption.MozCompatibility();
        } else {
            compatibilityOptions = CompatibilityOption.WebCompatibility();
        }
        if (options.asyncFunctions) {
            compatibilityOptions.add(CompatibilityOption.AsyncFunction);
        }
        if (options.ecmascript7) {
            compatibilityOptions.addAll(CompatibilityOption.ECMAScript7());
        }
        if (options.parser) {
            compatibilityOptions.add(CompatibilityOption.ReflectParse);
        }
        if (options.promiseRejection) {
            compatibilityOptions.add(CompatibilityOption.PromiseRejection);
        }
        return compatibilityOptions;
    }

    private static EnumSet<Parser.Option> parserOptions(Options options) {
        EnumSet<Parser.Option> parserOptions = EnumSet.noneOf(Parser.Option.class);
        if (options.nativeCalls) {
            parserOptions.add(Parser.Option.NativeCall);
        }
        return parserOptions;
    }

    private static EnumSet<Compiler.Option> compilerOptions(Options options) {
        EnumSet<Compiler.Option> compilerOptions = EnumSet.noneOf(Compiler.Option.class);
        if (options.debug) {
            compilerOptions.add(Compiler.Option.PrintCode);
        }
        if (options.fullDebug) {
            compilerOptions.add(Compiler.Option.PrintCode);
            compilerOptions.add(Compiler.Option.PrintFullCode);
        }
        if (options.debugInfo) {
            compilerOptions.add(Compiler.Option.DebugInfo);
        }
        if (options.noResume) {
            compilerOptions.add(Compiler.Option.NoResume);
        }
        if (options.noTailCall) {
            compilerOptions.add(Compiler.Option.NoTailCall);
        }
        return compilerOptions;
    }
}
