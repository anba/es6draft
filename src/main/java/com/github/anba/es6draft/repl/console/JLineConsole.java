/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.console;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.Terminal;
import jline.TerminalFactory;
import jline.TerminalSupport;
import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;
import jline.console.completer.CandidateListCompletionHandler;

/**
 * {@link ShellConsole} implementation for JLine consoles.
 */
public final class JLineConsole implements ShellConsole {
    private final ConsoleReader console;
    private final Formatter formatter;
    private final JLineReader reader = new JLineReader();
    private final PrintWriter writer = new PrintWriter(new JLineWriter(), true);
    private final PrintWriter errorWriter = new PrintWriter(System.err, true);

    public JLineConsole(String programName) throws IOException {
        this.console = newConsoleReader(programName);
        this.formatter = new Formatter(console.getOutput());
        this.console.setCopyPasteDetection(true);
    }

    private static ConsoleReader newConsoleReader(String programName) throws IOException {
        final boolean isWindows = isWindows();
        final String type = System.getProperty(TerminalFactory.JLINE_TERMINAL);
        if (isWindows && type == null) {
            TerminalFactory.registerFlavor(TerminalFactory.Flavor.WINDOWS, UnsupportedTerminal.class);
        } else if (isWindows && type.equalsIgnoreCase(TerminalFactory.UNIX)) {
            TerminalFactory.registerFlavor(TerminalFactory.Flavor.UNIX, CygwinTerminal.class);
        }
        FileInputStream in = new FileInputStream(FileDescriptor.in);
        Terminal terminal = TerminalFactory.get();
        ConsoleReader consoleReader = new ConsoleReader(programName, in, System.out, terminal, getDefaultEncoding());
        consoleReader.setExpandEvents(false);
        return consoleReader;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private static String getDefaultEncoding() {
        return Charset.defaultCharset().name();
    }

    public static final class CygwinTerminal extends TerminalSupport /* implements Terminal2 */ {
        private final int width, height;
        // private final HashSet<String> bools = new HashSet<>();
        // private final HashMap<String, Integer> ints = new HashMap<>();
        // private final HashMap<String, String> strings = new HashMap<>();

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

            // InfoCmp.parseInfoCmp(InfoCmp.getAnsiCaps(), bools, ints, strings);
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        // @Override
        // public boolean getBooleanCapability(String capability) {
        // return bools.contains(capability);
        // }
        //
        // @Override
        // public Integer getNumericCapability(String capability) {
        // return ints.get(capability);
        // }
        //
        // @Override
        // public String getStringCapability(String capability) {
        // return strings.get(capability);
        // }
    }

    private final class JLineReader extends Reader {
        private final StringBuilder buffer = new StringBuilder(2048);
        private int pos;

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (cbuf == null || off < 0 || len < 0 || off + len > cbuf.length) {
                throw new IllegalArgumentException();
            }
            int n = 0;
            while (n < len) {
                int buffered = buffer.length() - pos;
                if (buffered == 0) {
                    pos = 0;
                    buffer.setLength(0);
                    buffer.append(console.readLine()).append(System.lineSeparator());
                    buffered = buffer.length();
                }
                int r = Math.min(buffered, len - n);
                buffer.getChars(pos, pos + r, cbuf, off + n);
                pos += r;
                n += r;
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            // not applicable
        }
    }

    private final class JLineWriter extends Writer {
        @Override
        public void write(int c) throws IOException {
            console.print(String.valueOf((char) c));
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            console.print(new String(cbuf, off, len));
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            console.print(str.substring(off, off + len));
        }

        @Override
        public void flush() throws IOException {
            console.flush();
        }

        @Override
        public void close() throws IOException {
            // not applicable
        }
    }

    @Override
    public void printf(String format, Object... args) {
        formatter.format(format, args).flush();
    }

    @Override
    public void flush() {
        writer.flush();
        errorWriter.flush();
    }

    @Override
    public String readLine() {
        try {
            return console.readLine("");
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public String readLine(String prompt) {
        try {
            return console.readLine(prompt);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public Reader reader() {
        return reader;
    }

    @Override
    public PrintWriter writer() {
        return writer;
    }

    @Override
    public PrintWriter errorWriter() {
        return errorWriter;
    }

    @Override
    public boolean isAnsiSupported() {
        return console.getTerminal().isAnsiSupported();
    }

    @Override
    public void addCompleter(Completer completer) {
        CandidateListCompletionHandler handler = new CandidateListCompletionHandler();
        // handler.setPrintSpaceAfterFullCompletion(false);
        console.setCompletionHandler(handler);
        console.addCompleter(new JLineCompleter(completer));
    }

    private static final class JLineCompleter implements jline.console.completer.Completer {
        private final Completer completer;

        JLineCompleter(Completer completer) {
            this.completer = completer;
        }

        @Override
        public int complete(String buffer, int cursor, List<CharSequence> candidates) {
            Optional<Completion> opt = completer.complete(buffer, cursor);
            if (!opt.isPresent()) {
                return -1;
            }
            Completion c = opt.get();
            if (c.result().isEmpty()) {
                return -1;
            }
            candidates.addAll(c.result());
            return c.start();
        }
    }
}
