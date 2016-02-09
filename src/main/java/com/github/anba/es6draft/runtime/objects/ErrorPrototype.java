/**
 * Copyright (c) 2012-2015 André Bargull
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

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.StackTraces;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.5 Error Objects</h2>
 * <ul>
 * <li>19.5.3 Properties of the Error Prototype Object
 * </ul>
 */
public final class ErrorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Error prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public ErrorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
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
            /* steps 5-6 */
            CharSequence sname = Type.isUndefined(name) ? "Error" : ToString(cx, name);
            /* steps 7-8 */
            Object msg = Get(cx, o, "message");
            /* steps 9-10 */
            CharSequence smsg = Type.isUndefined(msg) ? "" : ToString(cx, msg);
            /* step 11 */
            if (sname.length() == 0) {
                return smsg;
            }
            /* step 12 */
            if (smsg.length() == 0) {
                return sname;
            }
            /* step 13 */
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
            ScriptException e = ((ErrorObject) thisValue).getException();
            return StackTraces.stackTraceStream(e).findFirst().map(StackTraceElement::getFileName).orElse("");
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
            ScriptException e = ((ErrorObject) thisValue).getException();
            return StackTraces.stackTraceStream(e).findFirst().map(StackTraceElement::getLineNumber).orElse(0);
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
            ScriptException e = ((ErrorObject) thisValue).getException();
            return StackTraces.stackTraceStream(e).collect(StringBuilder::new, (sb, element) -> {
                String methodName = element.getMethodName();
                String fileName = element.getFileName();
                int lineNumber = element.getLineNumber();
                sb.append(methodName).append('@').append(fileName).append(':').append(lineNumber).append('\n');
            }, StringBuilder::append).toString();
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
         * Extension: Error.prototype.stackTrace
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return stack-trace object
         */
        @Accessor(name = "stackTrace", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object get_stackTrace(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            ScriptException e = ((ErrorObject) thisValue).getException();
            return CreateArrayFromList(cx, StackTraces.stackTraceStream(e).map(element -> {
                OrdinaryObject elem = ObjectCreate(cx, Intrinsics.ObjectPrototype);
                CreateDataProperty(cx, elem, "methodName", element.getMethodName());
                CreateDataProperty(cx, elem, "fileName", element.getFileName());
                CreateDataProperty(cx, elem, "lineNumber", element.getLineNumber());
                return elem;
            }));
        }
    }
}
