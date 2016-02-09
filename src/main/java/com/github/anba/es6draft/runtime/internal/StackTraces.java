/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 
 */
public final class StackTraces {
    private StackTraces() {
    }

    /**
     * Returns the script stack trace elements.
     * 
     * @param e
     *            the throwable object
     * @return the script stack trace elements
     */
    public static Stream<StackTraceElement> stackTraceStream(Throwable e) {
        StackTraceElementIterator iterator = new StackTraceElementIterator(e);
        int characteristics = Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED;
        Spliterator<StackTraceElement> spliterator = Spliterators.spliteratorUnknownSize(iterator, characteristics);
        return StreamSupport.stream(spliterator, false).map(StackTraces::toScriptFrame);
    }

    /**
     * Returns the script stack trace elements.
     * 
     * @param e
     *            the throwable object
     * @return the script stack trace elements
     */
    public static StackTraceElement[] scriptStackTrace(Throwable e) {
        ArrayList<StackTraceElement> list = new ArrayList<>();
        for (Iterator<StackTraceElement> it = new StackTraceElementIterator(e); it.hasNext();) {
            list.add(toScriptFrame(it.next()));
        }
        return list.toArray(new StackTraceElement[0]);
    }

    /**
     * Returns a script stack trace element.
     * 
     * @param e
     *            the stack trace element
     * @return the script stack trace element
     */
    private static StackTraceElement toScriptFrame(StackTraceElement e) {
        String methodName = JVMNames.fromBytecodeName(e.getMethodName());
        assert methodName.charAt(0) != '!';
        int i = methodName.lastIndexOf('~');
        String scriptMethod = methodName.substring(0, (i != -1 ? i : methodName.length()));
        return new StackTraceElement("", scriptMethod, e.getFileName(), e.getLineNumber());
    }

    private static final class StackTraceElementIterator extends SimpleIterator<StackTraceElement> {
        private StackTraceElement[] elements;
        private int cursor;

        StackTraceElementIterator(Throwable e) {
            this.elements = e.getStackTrace();
        }

        @Override
        protected StackTraceElement findNext() {
            StackTraceElement[] elements = this.elements;
            if (elements != null) {
                int c = cursor;
                while (cursor < elements.length) {
                    StackTraceElement element = elements[cursor++];
                    if (isScriptStackFrame(element)) {
                        return element;
                    }
                }
                this.elements = null;
                // Return an "Interpreter" frame if no script stack frames were found.
                if (c == 0) {
                    return interpreterFrame();
                }
            }
            return null;
        }

        private static boolean isScriptStackFrame(StackTraceElement element) {
            // Filter stacktrace elements based on the encoding in Compiler/CodeGenerator.
            return element.getClassName().charAt(0) == '#'
                    && JVMNames.fromBytecodeName(element.getMethodName()).charAt(0) != '!'
                    && element.getLineNumber() > 0;
        }

        private static StackTraceElement interpreterFrame() {
            return new StackTraceElement("#Interpreter", "~interpreter", "<Interpreter>", 1);
        }
    }
}
