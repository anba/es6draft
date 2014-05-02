/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateArrayFromList;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.JVMNames;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.SimpleIterator;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.5 Error Objects</h2>
 * <ul>
 * <li>19.5.3 Properties of the Error Prototype Object
 * </ul>
 */
public final class ErrorPrototype extends OrdinaryObject implements Initializable {
    public ErrorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    /**
     * Package private accessor for {@link ErrorObject#toString()}
     * 
     * @param propertyKey
     *            the property key
     * @return the property record or {@code null} if none found
     */
    Property getOwnProperty(String propertyKey) {
        return ordinaryGetOwnProperty(propertyKey);
    }

    /**
     * 19.5.3 Properties of the Error Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 19.5.3.1 Error.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Error;

        /**
         * 19.5.3.3 Error.prototype.name
         */
        @Value(name = "name")
        public static final String name = "Error";

        /**
         * 19.5.3.2 Error.prototype.message
         */
        @Value(name = "message")
        public static final String message = "";

        /**
         * 19.5.3.4 Error.prototype.toString ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string representation
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            ScriptObject o = Type.objectValue(thisValue);
            /* steps 3-4 */
            Object name = Get(cx, o, "name");
            /* step 5 */
            CharSequence sname = Type.isUndefined(name) ? "Error" : ToString(cx, name);
            /* steps 6-7 */
            Object msg = Get(cx, o, "message");
            /* step 8 */
            CharSequence smsg = Type.isUndefined(msg) ? "" : ToString(cx, msg);
            /* step 9 */
            if (sname.length() == 0) {
                return smsg;
            }
            /* step 10 */
            if (smsg.length() == 0) {
                return sname;
            }
            /* step 11 */
            return sname + ": " + smsg;
        }

        /**
         * Extension: Error.prototype.fileName
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the file name
         */
        @Accessor(name = "fileName", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object get_fileName(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            return getTopStackTraceElement((ErrorObject) thisValue).getFileName();
        }

        /**
         * Extension: Error.prototype.fileName
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the new file name
         * @return the undefined value
         */
        @Accessor(name = "fileName", type = Accessor.Type.Setter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object set_fileName(ExecutionContext cx, Object thisValue, Object value) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            CreateDataProperty(cx, (ErrorObject) thisValue, "fileName", value);
            return UNDEFINED;
        }

        /**
         * Extension: Error.prototype.lineNumber
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the line number
         */
        @Accessor(name = "lineNumber", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object get_lineNumber(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            return getTopStackTraceElement((ErrorObject) thisValue).getLineNumber();
        }

        /**
         * Extension: Error.prototype.lineNumber
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the new line number
         * @return the undefined value
         */
        @Accessor(name = "lineNumber", type = Accessor.Type.Setter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object set_lineNumber(ExecutionContext cx, Object thisValue, Object value) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            CreateDataProperty(cx, (ErrorObject) thisValue, "lineNumber", value);
            return UNDEFINED;
        }

        /**
         * Extension: Error.prototype.columnNumber
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the column number
         */
        @Accessor(name = "columnNumber", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object get_columnNumber(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            // no column information available in StackTraceElements...
            return 0;
        }

        /**
         * Extension: Error.prototype.columnNumber
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the new column number
         * @return the undefined value
         */
        @Accessor(name = "columnNumber", type = Accessor.Type.Setter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object set_columnNumber(ExecutionContext cx, Object thisValue, Object value) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            CreateDataProperty(cx, (ErrorObject) thisValue, "columnNumber", value);
            return UNDEFINED;
        }

        /**
         * Extension: Error.prototype.stack
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the stack string
         */
        @Accessor(name = "stack", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object get_stack(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            return getStack((ErrorObject) thisValue);
        }

        /**
         * Extension: Error.prototype.stack
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the new stack string
         * @return the undefined value
         */
        @Accessor(name = "stack", type = Accessor.Type.Setter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object set_stack(ExecutionContext cx, Object thisValue, Object value) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            CreateDataProperty(cx, (ErrorObject) thisValue, "stack", value);
            return UNDEFINED;
        }

        /**
         * Extension: Error.prototype.stacktrace
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return stack-trace object
         */
        @Accessor(name = "stacktrace", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object get_stacktrace(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            return getStackTrace(cx, (ErrorObject) thisValue);
        }
    }

    private static StackTraceElement getTopStackTraceElement(ErrorObject e) {
        for (StackTraceElement element : new StackTraceElementIterable(e)) {
            return element;
        }
        return new StackTraceElement("", "", "", -1);
    }

    private static String getStack(ErrorObject e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : new StackTraceElementIterable(e)) {
            String methodName = getMethodName(element);
            String fileName = element.getFileName();
            int lineNumber = element.getLineNumber();
            sb.append(methodName).append('@').append(fileName).append(':').append(lineNumber)
                    .append('\n');
        }
        return sb.toString();
    }

    private static ExoticArray getStackTrace(ExecutionContext cx, ErrorObject e) {
        List<OrdinaryObject> list = new ArrayList<>();
        for (StackTraceElement element : new StackTraceElementIterable(e)) {
            OrdinaryObject elem = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            CreateDataProperty(cx, elem, "methodName", getMethodName(element));
            CreateDataProperty(cx, elem, "fileName", element.getFileName());
            CreateDataProperty(cx, elem, "lineNumber", element.getLineNumber());
            list.add(elem);
        }
        return CreateArrayFromList(cx, list);
    }

    private static String getMethodName(StackTraceElement element) {
        String methodName = JVMNames.fromBytecodeName(element.getMethodName());
        assert methodName.charAt(0) == '!';
        int i = methodName.lastIndexOf('~');
        return methodName.substring(1, (i != -1 ? i : methodName.length()));
    }

    private static final class StackTraceElementIterable implements Iterable<StackTraceElement> {
        private ErrorObject error;

        StackTraceElementIterable(ErrorObject error) {
            this.error = error;
        }

        @Override
        public Iterator<StackTraceElement> iterator() {
            return new StackTraceElementIterator(error);
        }
    }

    private static final class StackTraceElementIterator extends SimpleIterator<StackTraceElement> {
        private StackTraceElement[] elements;
        private int cursor = 0;
        private Iterator<StackTraceElement[]> stackTraces;

        StackTraceElementIterator(ErrorObject error) {
            this.elements = error.getException().getStackTrace();
            this.stackTraces = error.getStackTraces().iterator();
        }

        private static boolean isInternalStackFrame(StackTraceElement element) {
            // filter stacktrace elements based on the encoding in CodeGenerator/ScriptLoader
            return (element.getClassName().charAt(0) == '#'
                    && JVMNames.fromBytecodeName(element.getMethodName()).charAt(0) == '!' && element
                    .getLineNumber() > 0);
        }

        @Override
        protected StackTraceElement tryNext() {
            while (elements != null) {
                while (cursor < elements.length) {
                    StackTraceElement element = elements[cursor++];
                    if (isInternalStackFrame(element)) {
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
            return null;
        }
    }
}
