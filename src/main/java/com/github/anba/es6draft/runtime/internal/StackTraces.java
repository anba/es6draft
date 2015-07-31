/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.runtime.objects.ErrorObject;

/**
 * 
 */
public final class StackTraces {
    private StackTraces() {
    }

    /**
     * Returns stack traces from generator threads.
     * 
     * @return stack traces from generator threads
     */
    public static List<StackTraceElement[]> collectGeneratorStackTraces() {
        Thread thread = Thread.currentThread();
        if (!(thread instanceof GeneratorThread)) {
            return Collections.emptyList();
        }
        ArrayList<StackTraceElement[]> stackTraces = new ArrayList<>();
        do {
            thread = ((GeneratorThread) thread).getParent();
            stackTraces.add(thread.getStackTrace());
        } while (thread instanceof GeneratorThread);
        return stackTraces;
    }

    /**
     * Returns the method name of a script stack trace element.
     * 
     * @param element
     *            the script stack trace element
     * @return the method name
     */
    public static String getMethodName(StackTraceElement element) {
        String methodName = JVMNames.fromBytecodeName(element.getMethodName());
        assert methodName.charAt(0) != '!';
        int i = methodName.lastIndexOf('~');
        return methodName.substring(0, (i != -1 ? i : methodName.length()));
    }

    /**
     * Returns the top script stack trace element.
     * 
     * @param e
     *            the error object
     * @return the top script stack trace element
     */
    public static StackTraceElement getTopStackTraceElement(ErrorObject e) {
        for (StackTraceElement element : new StackTraceElementIterable(e)) {
            return element;
        }
        throw new AssertionError();
    }

    /**
     * Returns the top script stack trace element.
     * 
     * @param e
     *            the throwable object
     * @return the top script stack trace element
     */
    public static StackTraceElement getTopStackTraceElement(Throwable e) {
        for (StackTraceElement element : new StackTraceElementIterable2(e)) {
            return element;
        }
        throw new AssertionError();
    }

    /**
     * Returns the script stack trace elements.
     * 
     * @param e
     *            the error object
     * @return the script stack trace elements
     */
    public static Iterable<StackTraceElement> getStackTrace(ErrorObject e) {
        return new StackTraceElementIterable(e);
    }

    /**
     * Returns the script stack trace elements.
     * 
     * @param e
     *            the throwable object
     * @return the script stack trace elements
     */
    public static Iterable<StackTraceElement> getStackTrace(Throwable e) {
        return new StackTraceElementIterable2(e);
    }

    /**
     * Returns the script stack trace elements.
     * 
     * @param e
     *            the throwable object
     * @return the script stack trace elements
     */
    public static StackTraceElement[] scriptStackTrace(Throwable e) {
        return scriptStackTrace(e.getStackTrace());
    }

    /**
     * Returns the script stack trace elements.
     * 
     * @param stackTrace
     *            the stack trace elements
     * @return the script stack trace elements
     */
    public static StackTraceElement[] scriptStackTrace(StackTraceElement[] stackTrace) {
        ArrayList<StackTraceElement> list = new ArrayList<>();
        for (Iterator<StackTraceElement> it = new StackTraceElementIterator(stackTrace); it.hasNext();) {
            list.add(toScriptFrame(it.next()));
        }
        return list.toArray(new StackTraceElement[list.size()]);
    }

    /**
     * Returns a script stack trace element.
     * 
     * @param e
     *            the stack trace element
     * @return the script stack trace element
     */
    public static StackTraceElement toScriptFrame(StackTraceElement e) {
        String className = "", methodName = getMethodName(e), fileName = e.getFileName();
        int lineNumber = e.getLineNumber();
        return new StackTraceElement(className, methodName, fileName, lineNumber);
    }

    private static final class StackTraceElementIterable implements Iterable<StackTraceElement> {
        private final ErrorObject error;

        StackTraceElementIterable(ErrorObject error) {
            this.error = error;
        }

        @Override
        public Iterator<StackTraceElement> iterator() {
            return new StackTraceElementIterator(error);
        }
    }

    private static final class StackTraceElementIterable2 implements Iterable<StackTraceElement> {
        private final Throwable exception;

        StackTraceElementIterable2(Throwable exception) {
            this.exception = exception;
        }

        @Override
        public Iterator<StackTraceElement> iterator() {
            return new StackTraceElementIterator(exception);
        }
    }

    private static final class StackTraceElementIterator extends SimpleIterator<StackTraceElement> {
        private StackTraceElement[] elements;
        private final Iterator<StackTraceElement[]> stackTraces;
        private int cursor = 0;
        private boolean foundScriptFrame = false;

        StackTraceElementIterator(ErrorObject error) {
            this.elements = error.getException().getStackTrace();
            this.stackTraces = error.getStackTraces().iterator();
        }

        StackTraceElementIterator(Throwable exception) {
            this.elements = exception.getStackTrace();
            this.stackTraces = Collections.emptyIterator();
        }

        StackTraceElementIterator(StackTraceElement[] elements) {
            this.elements = elements;
            this.stackTraces = Collections.emptyIterator();
        }

        private static boolean isScriptStackFrame(StackTraceElement element) {
            // filter stacktrace elements based on the encoding in Compiler/CodeGenerator
            return element.getClassName().charAt(0) == '#'
                    && JVMNames.fromBytecodeName(element.getMethodName()).charAt(0) != '!'
                    && element.getLineNumber() > 0;
        }

        private static StackTraceElement interpreterFrame() {
            return new StackTraceElement("#Interpreter", "~interpreter", "<Interpreter>", 1);
        }

        @Override
        protected StackTraceElement findNext() {
            while (elements != null) {
                while (cursor < elements.length) {
                    StackTraceElement element = elements[cursor++];
                    if (isScriptStackFrame(element)) {
                        foundScriptFrame = true;
                        return element;
                    }
                }
                if (stackTraces.hasNext()) {
                    cursor = 0;
                    elements = stackTraces.next();
                } else {
                    elements = null;
                }
            }
            if (!foundScriptFrame) {
                foundScriptFrame = true;
                return interpreterFrame();
            }
            return null;
        }
    }
}
