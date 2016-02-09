/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateArrayFromList;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateMethodProperty;
import static com.github.anba.es6draft.runtime.Realm.InitializeHostDefinedRealm;
import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.ModuleEvaluationJob;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.RestOfArgumentsHandler;
import org.kohsuke.args4j.spi.Setter;
import org.kohsuke.args4j.spi.StopOptionHandler;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Characters;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserEOFException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.JLineConsole;
import com.github.anba.es6draft.repl.console.LegacyConsole;
import com.github.anba.es6draft.repl.console.NativeConsole;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.ConsoleObject;
import com.github.anba.es6draft.repl.global.MozShellGlobalObject;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.repl.global.SimpleShellGlobalObject;
import com.github.anba.es6draft.repl.global.StopExecutionException;
import com.github.anba.es6draft.repl.global.StopExecutionException.Reason;
import com.github.anba.es6draft.repl.global.V8ShellGlobalObject;
import com.github.anba.es6draft.repl.loader.NodeModuleLoader;
import com.github.anba.es6draft.repl.loader.NodeStandardModuleLoader;
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
import com.github.anba.es6draft.runtime.modules.loader.FileModuleLoader;
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
            ShellConsole console = createConsole(options);
            new Repl(console, options).loop();
        } catch (Throwable e) {
            printStackTrace(System.err, e, options.stacktraceDepth);
            System.exit(1);
        }
    }

    private static ShellConsole createConsole(Options options) throws IOException {
        ShellConsole console;
        if (!options.noJLine) {
            console = new JLineConsole(PROGRAM_NAME);
        } else if (System.console() != null) {
            console = new NativeConsole();
        } else {
            console = new LegacyConsole();
        }
        return console;
    }

    private void printStackTrace(Throwable e) {
        if (options.stacktrace) {
            printStackTrace(console.writer(), e, options.stacktraceDepth);
        }
    }

    private static void printStackTrace(PrintStream stream, Throwable e, int maxDepth) {
        final int depth = Math.max(maxDepth, 0);
        new TruncatedThrowable(e, depth).printStackTrace(stream);
    }

    private static void printStackTrace(PrintWriter writer, Throwable e, int maxDepth) {
        final int depth = Math.max(maxDepth, 0);
        new TruncatedThrowable(e, depth).printStackTrace(writer);
    }

    @SuppressWarnings("serial")
    private static final class TruncatedThrowable extends Throwable {
        private static final boolean REMOVE_PACKAGE_NAME = System.console() != null;
        private static final String PACKAGE_NAME = "com.github.anba.es6draft.";
        private final Throwable throwable;

        TruncatedThrowable(Throwable throwable, int depth) {
            super();
            this.throwable = throwable;
            setStackTrace(truncate(throwable, depth));
            if (throwable.getCause() != null) {
                initCause(new TruncatedThrowable(throwable.getCause(), depth));
            }
            for (Throwable suppressed : throwable.getSuppressed()) {
                addSuppressed(new TruncatedThrowable(suppressed, depth));
            }
        }

        static StackTraceElement[] truncate(Throwable e, int depth) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace.length > depth) {
                int omitted = stackTrace.length - depth;
                stackTrace = Arrays.copyOf(stackTrace, depth + 1);
                stackTrace[depth] = new StackTraceElement("..", "", "Frames omitted", omitted);
            }
            if (REMOVE_PACKAGE_NAME) {
                for (int i = 0; i < stackTrace.length; ++i) {
                    StackTraceElement element = stackTrace[i];
                    String className = element.getClassName();
                    if (className.startsWith(PACKAGE_NAME)) {
                        stackTrace[i] = new StackTraceElement(className.substring(PACKAGE_NAME.length()),
                                element.getMethodName(), element.getFileName(), element.getLineNumber());
                    }
                }
            }
            return stackTrace;
        }

        @Override
        public String toString() {
            return throwable.toString();
        }

        @Override
        public Throwable fillInStackTrace() {
            // empty
            return this;
        }
    }

    private void printScriptStackTrace(Realm realm, ScriptException e) {
        if (options.scriptStacktrace) {
            StringBuilder sb = new StringBuilder();
            printScriptFrames(sb, realm, e, 1);
            if (sb.length() == 0 && e.getCause() != null) {
                printScriptFrames(sb, realm, e.getCause(), 1);
            }
            console.writer().print(sb.toString());
        }
    }

    private void printScriptFrames(StringBuilder sb, Realm realm, Throwable e, int level) {
        final String indent = Strings.repeat('\t', level);
        final int maxDepth = options.stacktraceDepth;
        int depth = 0;
        StackTraceElement[] stackTrace = StackTraces.scriptStackTrace(e);
        for (; depth < Math.min(stackTrace.length, maxDepth); ++depth) {
            StackTraceElement element = stackTrace[depth];
            String methodName = element.getMethodName();
            String fileName = element.getFileName();
            int lineNumber = element.getLineNumber();
            sb.append(indent).append("at ").append(methodName).append(" (").append(fileName).append(':')
                    .append(lineNumber).append(")\n");
        }
        if (depth < stackTrace.length) {
            int skipped = stackTrace.length - depth;
            sb.append("\t.. ").append(skipped).append(" frames omitted\n");
        }
        if (e.getSuppressed().length > 0 && level == 1) {
            Throwable suppressed = e.getSuppressed()[0];
            String message;
            if (suppressed instanceof ScriptException) {
                message = ((ScriptException) suppressed).getMessage(realm.defaultContext());
            } else {
                message = Objects.toString(suppressed.getMessage(), suppressed.getClass().getSimpleName());
            }
            sb.append(indent).append(formatMessage("suppressed_exception", message)).append('\n');
            printScriptFrames(sb, realm, suppressed, level + 1);
        }
    }

    private static void parseOptions(Options options, String[] args) {
        ParserProperties properties = ParserProperties.defaults().withUsageWidth(128);
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

            // Warn if --module is used without input files.
            if (options.module) {
                System.err.println(formatMessage("module_no_files"));
            }
        }
        if (options.ecmascript7) {
            System.err.println(formatMessage("deprecated.es7"));
        }
    }

    private interface EvalScript {
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
            return new EvalSourceIdentifier("eval-module-" + moduleIds.incrementAndGet(), URI.create(""));
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
        // TODO: "simple" is a misnomer, change to "default"?
        Simple, Mozilla, V8;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum ModuleLoaderMode {
        Default, Node, NodeStandard;

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

        @Option(name = "-e", aliases = { "--eval", "--execute" }, metaVar = "meta.string", usage = "options.eval")
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

        @Option(name = "--module-loader", usage = "options.module_loader")
        ModuleLoaderMode moduleLoaderMode = ModuleLoaderMode.Default;

        @Option(name = "--strict", usage = "options.strict")
        boolean strict;

        @Option(name = "--shell", usage = "options.shell")
        ShellMode shellMode = ShellMode.Simple;

        @Option(name = "--es7", usage = "options.es7")
        boolean ecmascript7;

        @Option(name = "--stage", usage = "options.stage", handler = RangeIntOptionHandler.class)
        @RangeIntOptionHandler.Range(min = 0, max = 4)
        int stage = 3;

        @Option(name = "--experimental", usage = "options.experimental")
        boolean experimental;

        @Option(name = "--async", usage = "options.async")
        boolean asyncFunctions;

        @Option(name = "--parser", usage = "options.parser")
        boolean parser;

        @Option(name = "--timers", usage = "options.timers")
        boolean timers;

        @Option(name = "--console", usage = "options.console")
        boolean console;

        @Option(name = "--no-jline", usage = "options.no_jline")
        boolean noJLine;

        @Option(name = "--no-color", usage = "options.no_color")
        boolean noColor;

        @Option(name = "--no-interpreter", aliases = { "--compile-only" }, usage = "options.no_interpreter")
        boolean noInterpreter;

        @Option(name = "--stacktrace", usage = "options.stacktrace")
        boolean stacktrace;

        @Option(name = "--script-stacktrace", usage = "options.script_stacktrace")
        boolean scriptStacktrace;

        @Option(name = "--stacktrace-depth", hidden = true, usage = "options.stacktrace_depth")
        int stacktraceDepth = STACKTRACE_DEPTH;

        @Option(name = "--debug", usage = "options.debug")
        boolean debug;

        @Option(name = "--full-debug", hidden = true, usage = "options.full_debug")
        boolean fullDebug;

        @Option(name = "--debug-info", hidden = true, usage = "options.debug_info")
        boolean debugInfo;

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
        @Argument(index = 1, multiValued = true, metaVar = "meta.arguments", usage = "options.arguments",
                handler = RestOfArgumentsHandler.class)
        List<String> arguments = new ArrayList<>();
    }

    public static final class RangeIntOptionHandler extends OneArgumentOptionHandler<Integer> {
        public RangeIntOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Integer> setter) {
            super(parser, option, setter);
        }

        @Target(value = { ElementType.FIELD })
        @Retention(RetentionPolicy.RUNTIME)
        public @interface Range {
            int min() default Integer.MIN_VALUE;

            int max() default Integer.MAX_VALUE;
        }

        @Override
        protected Integer parse(String argument) throws NumberFormatException, CmdLineException {
            int value = Integer.parseInt(argument);
            Range range = setter.asAnnotatedElement().getAnnotation(Range.class);
            if (range != null && value != Math.min(Math.max(value, range.min()), range.max())) {
                throw new NumberFormatException();
            }
            return value;
        }
    }

    public static final class StopOptionAndConsumeRestHandler extends OptionHandler<String> {
        private final StopOptionHandler stopOptionHandler;
        private final RestOfArgumentsHandler restOfArgumentsHandler;

        public StopOptionAndConsumeRestHandler(CmdLineParser parser, OptionDef option, Setter<String> setter) {
            super(parser, option, setter);
            this.stopOptionHandler = new StopOptionHandler(parser, option, setter);
            this.restOfArgumentsHandler = new RestOfArgumentsHandler(parser, option, setter);
        }

        @Override
        public int parseArguments(Parameters params) throws CmdLineException {
            return stopOptionHandler.parseArguments(params) + restOfArgumentsHandler.parseArguments(params);
        }

        @Override
        public String getDefaultMetaVariable() {
            return "ARGUMENTS";
        }
    }

    public static final class StopOptionAndRepeatHandler extends OptionHandler<String> {
        private final StopOptionHandler stopOptionHandler;

        public StopOptionAndRepeatHandler(CmdLineParser parser, OptionDef option, Setter<String> setter) {
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
        parser.printUsage(writer, rb, showAll ? OptionHandlerFilter.ALL : OptionHandlerFilter.PUBLIC);
        return writer.toString();
    }

    private static ResourceBundle getResourceBundle() {
        ResourceBundle.Control control = new PropertiesReaderControl(StandardCharsets.UTF_8);
        return new XResourceBundle(ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault(), control));
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
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Repl.class.getResourceAsStream(resourceName), StandardCharsets.UTF_8))) {
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

    private final ShellConsole console;
    private final Options options;
    private final SourceBuilder sourceBuilder;
    private final AtomicInteger scriptCounter = new AtomicInteger(0);

    private Repl(ShellConsole console, Options options) {
        this.console = console;
        this.options = options;
        this.sourceBuilder = new SourceBuilder(console.isAnsiSupported() && !options.noColor, 10, 30, 80);
    }

    private void handleException(Throwable e) {
        String message = Objects.toString(e.getMessage(), e.getClass().getSimpleName());
        console.printf("%s%n", message);
        printStackTrace(e);
    }

    private void handleException(IOException e) {
        String message = Objects.toString(e.getMessage(), "");
        console.printf("%s: %s%n", e.getClass().getSimpleName(), message);
        printStackTrace(e);
    }

    private void handleException(Realm realm, OutOfMemoryError e) {
        // Try to recover after OOM.
        Runtime rt = Runtime.getRuntime();
        long beforeGc = rt.freeMemory();
        rt.gc();
        long afterGc = rt.freeMemory();
        if (afterGc > beforeGc && (afterGc - beforeGc) < 50_000_000) {
            // Calling gc() cleared less than 50MB, assume unrecoverable OOM and rethrow error.
            throw e;
        }
        // Create script exception with stacktrace from oom-error.
        ScriptException exception = newInternalError(realm.defaultContext(), Messages.Key.OutOfMemoryVM);
        exception.setStackTrace(e.getStackTrace());
        handleException(realm, exception);
    }

    private void handleException(Realm realm, StackOverflowError e) {
        // Create script exception with stacktrace from stackoverflow-error.
        ScriptException exception = newInternalError(realm.defaultContext(), Messages.Key.StackOverflow);
        exception.setStackTrace(e.getStackTrace());
        handleException(realm, exception);
    }

    private void handleException(Realm realm, ScriptException e) {
        String message = formatMessage("uncaught_exception", e.getMessage(realm.defaultContext()));
        console.printf("%s%n", message);
        printScriptStackTrace(realm, e);
        printStackTrace(e);
    }

    private void handleException(Realm realm, UnhandledRejectionException e) {
        String message = formatMessage("unhandled_rejection", e.getMessage(realm.defaultContext()));
        console.printf("%s%n", message);
        printStackTrace(e.getCauseIfPresent());
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
        printStackTrace(e);
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
        for (int length = s.length(); index < length && !Characters.isLineTerminator(s.charAt(index)); ++index)
            ;
        return index;
    }

    private static com.github.anba.es6draft.ast.Script parse(Realm realm, Source source, String sourceCode)
            throws ParserException {
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
    private com.github.anba.es6draft.ast.Script read(Realm realm, int[] line) {
        StringBuilder sourceBuffer = new StringBuilder();
        for (String prompt = PROMPT;; prompt = "") {
            String s = console.readLine(prompt);
            if (s == null) {
                return null;
            }
            sourceBuffer.append(s).append('\n');
            String sourceCode = sourceBuffer.toString();
            Source source = new Source(Paths.get(".").toAbsolutePath(), "typein", line[0]);
            try {
                com.github.anba.es6draft.ast.Script script = parse(realm, source, sourceCode);
                line[0] += script.getEndLine() - script.getBeginLine();
                return script;
            } catch (ParserEOFException e) {
                continue;
            } catch (ParserException e) {
                throw new ParserExceptionWithSource(e, source, sourceCode);
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
        return script.evaluate(realm);
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
        World world = realm.getWorld();
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
            } catch (StackOverflowError e) {
                handleException(realm, e);
            } catch (OutOfMemoryError e) {
                handleException(realm, e);
            } catch (InternalException e) {
                handleException(e);
            } catch (BootstrapMethodError e) {
                handleException(e.getCause());
            } catch (UncheckedIOException e) {
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
            sources.add(realm.createGlobalProperties(new Timers(), Timers.class));
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
        private final int[] line = { 1 };

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
                    com.github.anba.es6draft.ast.Script parsedScript = read(realm, line);
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
        ObjectAllocator<? extends ShellGlobalObject> allocator;
        if (options.shellMode == ShellMode.Mozilla) {
            allocator = MozShellGlobalObject::new;
        } else if (options.shellMode == ShellMode.V8) {
            allocator = V8ShellGlobalObject::new;
        } else {
            allocator = SimpleShellGlobalObject::new;
        }

        BiFunction<RuntimeContext, ScriptLoader, ModuleLoader> moduleLoader;
        switch (options.moduleLoaderMode) {
        case Default:
            moduleLoader = FileModuleLoader::new;
            break;
        case Node:
            moduleLoader = NodeModuleLoader::new;
            break;
        case NodeStandard:
            moduleLoader = NodeStandardModuleLoader::new;
            break;
        default:
            throw new AssertionError();
        }

        /* @formatter:off */
        RuntimeContext context = new RuntimeContext.Builder()
                                                   .setBaseDirectory(Paths.get("").toAbsolutePath())
                                                   .setGlobalAllocator(allocator)
                                                   .setModuleLoader(moduleLoader)
                                                   .setConsole(console)
                                                   .setOptions(compatibilityOptions(options))
                                                   .setParserOptions(parserOptions(options))
                                                   .setCompilerOptions(compilerOptions(options))
                                                   .build();
        /* @formatter:on */

        World world = new World(context);
        Realm realm = world.newRealm();
        ExecutionContext cx = realm.defaultContext();

        // Add completion to console
        console.addCompleter(new ShellCompleter(realm));

        // Execute global specific initialization
        enqueueScriptTask(realm, () -> {
            InitializeHostDefinedRealm(realm);

            // Add global "arguments" property
            ScriptObject arguments = CreateArrayFromList(cx, options.arguments);
            CreateMethodProperty(cx, realm.getGlobalObject(), "arguments", arguments);
        });
        if (options.console) {
            enqueueScriptTask(realm, () -> {
                ScriptObject console = ConsoleObject.createConsole(realm);
                CreateMethodProperty(cx, realm.getGlobalObject(), "console", console);
            });
        }
        if (options.moduleLoaderMode == ModuleLoaderMode.Node) {
            // TODO: Add default initialize(Realm) method to ModuleLoader interface?
            enqueueScriptTask(realm, () -> {
                NodeModuleLoader nodeLoader = (NodeModuleLoader) world.getModuleLoader();
                nodeLoader.initialize(realm);
            });
        }

        // Run eval expressions and files
        for (EvalScript evalScript : options.evalScripts) {
            if (options.module) {
                enqueueScriptTask(realm, () -> {
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
                });
            } else {
                enqueueScriptTask(realm, () -> {
                    Source source = evalScript.getSource();
                    String sourceCode = evalScript.getSourceCode();
                    try {
                        eval(realm, parse(realm, source, sourceCode));
                    } catch (ParserException e) {
                        throw new ParserExceptionWithSource(e, source, sourceCode);
                    }
                });
            }
        }

        return realm;
    }

    @FunctionalInterface
    private interface Init {
        void apply() throws IOException, URISyntaxException, MalformedNameException, ResolutionException;
    }

    private static void enqueueScriptTask(Realm realm, Init init) {
        realm.enqueueScriptTask(() -> {
            try {
                init.apply();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (URISyntaxException e) {
                throw new UncheckedIOException(new IOException(e));
            } catch (MalformedNameException | ResolutionException e) {
                throw e.toScriptException(realm.defaultContext());
            }
        });
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
        compatibilityOptions.addAll(CompatibilityOption.Version(CompatibilityOption.Version.ECMAScript2016));
        if (options.asyncFunctions) {
            compatibilityOptions.add(CompatibilityOption.AsyncFunction);
        }
        if (options.ecmascript7) {
            compatibilityOptions.addAll(CompatibilityOption.Stage(CompatibilityOption.Stage.Strawman));
            compatibilityOptions.addAll(CompatibilityOption.Experimental());
        }
        if (options.experimental) {
            compatibilityOptions.addAll(CompatibilityOption.Experimental());
        }
        if (options.stage >= 0) {
            compatibilityOptions.addAll(CompatibilityOption.Stage(stageForLevel(options.stage)));
        }
        if (options.parser) {
            compatibilityOptions.add(CompatibilityOption.ReflectParse);
        }
        if (options.promiseRejection) {
            compatibilityOptions.add(CompatibilityOption.PromiseRejection);
        }
        return compatibilityOptions;
    }

    private static CompatibilityOption.Stage stageForLevel(int level) {
        switch (level) {
        case 0:
            return CompatibilityOption.Stage.Strawman;
        case 1:
            return CompatibilityOption.Stage.Proposal;
        case 2:
            return CompatibilityOption.Stage.Draft;
        case 3:
            return CompatibilityOption.Stage.Candidate;
        case 4:
            return CompatibilityOption.Stage.Finished;
        default:
            throw new AssertionError();
        }
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
        if (options.noTailCall) {
            compilerOptions.add(Compiler.Option.NoTailCall);
        }
        return compilerOptions;
    }
}
