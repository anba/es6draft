/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Arrays;
import java.util.Iterator;

import com.github.anba.es6draft.compiler.analyzer.CodeSize;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.language.ClassOperations;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBuffer;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferObject;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.objects.collection.WeakMapObject;
import com.github.anba.es6draft.runtime.objects.collection.WeakSetObject;
import com.github.anba.es6draft.runtime.objects.text.RegExpObject;
import com.github.anba.es6draft.runtime.objects.text.RegExpPrototype;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Default runtime functions.
 */
public final class RuntimeFunctions {
    private RuntimeFunctions() {
    }

    private static Intrinsics getIntrinsicByName(ExecutionContext cx, String name) {
        try {
            return Intrinsics.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw newInternalError(cx, Messages.Key.InternalError, "Invalid intrinsic: " + name);
        }
    }

    private static BuiltinSymbol getSymbolByName(ExecutionContext cx, String name) {
        try {
            return BuiltinSymbol.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw newInternalError(cx, Messages.Key.InternalError, "Invalid symbol name: " + name);
        }
    }

    private static CompatibilityOption getCompatibilityOptionByName(ExecutionContext cx, String name) {
        try {
            return CompatibilityOption.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw newInternalError(cx, Messages.Key.InternalError, "Invalid compatibility option: " + name);
        }
    }

    /**
     * Native function: {@code %Intrinsic(<name>)}.
     * <p>
     * Returns the intrinsic by name.
     * 
     * @param cx
     *            the execution context
     * @param name
     *            the intrinsic name
     * @return the intrinsic
     */
    public static OrdinaryObject Intrinsic(ExecutionContext cx, String name) {
        Intrinsics id = getIntrinsicByName(cx, name);
        return cx.getRealm().getIntrinsic(id);
    }

    /**
     * Native function: {@code %SetIntrinsic(<name>, <realm>)}.
     * <p>
     * Sets the intrinsic to a new value.
     * 
     * @param cx
     *            the execution context
     * @param name
     *            the intrinsic name
     * @param intrinsic
     *            the new intrinsic object
     * @return the intrinsic
     */
    public static OrdinaryObject SetIntrinsic(ExecutionContext cx, String name, OrdinaryObject intrinsic) {
        Intrinsics id = getIntrinsicByName(cx, name);
        cx.getRealm().setIntrinsic(id, intrinsic);
        return intrinsic;
    }

    /**
     * Native function: {@code %CallFunction(<function>, <thisValue>, ...<arguments>)}.
     * <p>
     * Calls the function object.
     * 
     * @param cx
     *            the execution context
     * @param fn
     *            the function
     * @param thisValue
     *            the this-value
     * @param args
     *            the function arguments
     * @return the function return value
     */
    public static Object CallFunction(ExecutionContext cx, Callable fn, Object thisValue, Object... args) {
        return fn.call(cx, thisValue, args);
    }

    /**
     * Native function: {@code %GlobalProperties()}.
     * <p>
     * Returns the global properties object.
     * 
     * @param cx
     *            the execution context
     * @return the global properties object
     */
    public static OrdinaryObject GlobalProperties(ExecutionContext cx) {
        return cx.getRealm().getGlobalPropertiesObject();
    }

    /**
     * Native function: {@code %GlobalObject()}.
     * <p>
     * Returns the global object.
     * 
     * @param cx
     *            the execution context
     * @return the global object
     */
    public static ScriptObject GlobalObject(ExecutionContext cx) {
        return cx.getRealm().getGlobalObject();
    }

    /**
     * Native function: {@code %GlobalThis()}.
     * <p>
     * Returns the global this.
     * 
     * @param cx
     *            the execution context
     * @return the global this
     */
    public static ScriptObject GlobalThis(ExecutionContext cx) {
        return cx.getRealm().getGlobalThis();
    }

    /**
     * Native function: {@code %IsArrayBuffer(<value>)}.
     * <p>
     * Tests whether the input argument is an ArrayBuffer object.
     * 
     * @param value
     *            the input argument
     * @return {@code true} if the object is an ArrayBuffer
     */
    public static boolean IsArrayBuffer(Object value) {
        return value instanceof ArrayBufferObject;
    }

    /**
     * Native function: {@code %IsCompatibilityOptionEnabled(<name>)}.
     * <p>
     * Tests whether or not a compatibility option is enabled
     * 
     * @param cx
     *            the execution context
     * @param name
     *            compatibility option name
     * @return {@code true} if the compatibility option is enabled
     */
    public static boolean IsCompatibilityOptionEnabled(ExecutionContext cx, String name) {
        return cx.getRuntimeContext().isEnabled(getCompatibilityOptionByName(cx, name));
    }

    /**
     * Native function: {@code %IsDetachedBuffer(<arrayBuffer>)}.
     * <p>
     * Tests whether or not the array buffer is detached.
     * 
     * @param arrayBuffer
     *            the array buffer object
     * @return {@code true} if the array buffer is detached
     */
    public static boolean IsDetachedBuffer(ArrayBufferObject arrayBuffer) {
        return ArrayBufferConstructor.IsDetachedBuffer(arrayBuffer);
    }

    /**
     * Native function: {@code %IsFunctionExpression(<function>)}.
     * <p>
     * Returns {@code true} if <var>function</var> is a function expression.
     * 
     * @param function
     *            the function object
     * @return {@code true} if <var>function</var> is a function expression
     */
    public static boolean IsFunctionExpression(Callable function) {
        if (!(function instanceof FunctionObject)) {
            return false;
        }
        RuntimeInfo.Function code = ((FunctionObject) function).getCode();
        return code.is(RuntimeInfo.FunctionFlags.Expression) && !code.is(RuntimeInfo.FunctionFlags.Arrow);
    }

    /**
     * Native function: {@code %IsGeneratorFunction(<function>)}.
     * <p>
     * Returns {@code true} if <var>function</var> is a generator function.
     * 
     * @param function
     *            the function object
     * @return {@code true} if <var>function</var> is a generator function
     */
    public static boolean IsGeneratorFunction(Callable function) {
        if (!(function instanceof FunctionObject)) {
            return false;
        }
        RuntimeInfo.Function code = ((FunctionObject) function).getCode();
        return code.is(RuntimeInfo.FunctionFlags.Generator) && !code.is(RuntimeInfo.FunctionFlags.Async);
    }

    /**
     * Native function: {@code %IsTypedArrayObject(<elementType>, <value>)}.
     * <p>
     * Tests whether the input argument is a typed array object.
     * 
     * @param elementType
     *            the element type name
     * @param value
     *            the input argument
     * @return {@code true} if the object is a typed array object
     */
    public static boolean IsTypedArrayObject(CharSequence elementType, Object value) {
        if (!(value instanceof TypedArrayObject)) {
            return false;
        }
        return ((TypedArrayObject) value).getElementType().getConstructorName().equals(elementType.toString());
    }

    /**
     * Native function: {@code %RegExpTest(<regexp>, <string>)}.
     * <p>
     * Returns {@code true} if <var>string</var> matches <var>regexp</var>.
     * 
     * @param cx
     *            the execution context
     * @param regexp
     *            the regular expression object
     * @param string
     *            the input string
     * @return {@code true} if <var>string</var> matches <var>regexp</var>
     */
    public static boolean RegExpTest(ExecutionContext cx, RegExpObject regexp, CharSequence string) {
        return RegExpPrototype.RegExpTest(cx, regexp, string);
    }

    /**
     * Native function: {@code %WellKnownSymbol(<name>)}.
     * <p>
     * Returns the well-known symbol by name.
     * 
     * @param cx
     *            the execution context
     * @param name
     *            the well-known symbol name
     * @return the well-known symbol
     */
    public static Symbol WellKnownSymbol(ExecutionContext cx, String name) {
        return getSymbolByName(cx, name).get();
    }

    /**
     * Native function: {@code %IsWellKnownSymbol(<symbol>)}.
     * <p>
     * Returns {@code true} if <var>symbol</var> is a well-known symbol.
     * 
     * @param symbol
     *            the symbol object
     * @return {@code true} if <var>symbol</var> is a well-known symbol.
     */
    public static boolean IsWellKnownSymbol(Symbol symbol) {
        for (BuiltinSymbol builtin : BuiltinSymbol.values()) {
            if (builtin != BuiltinSymbol.NONE && builtin.get() == symbol) {
                return true;
            }
        }
        return false;
    }

    /**
     * Native function: {@code %SymbolDescription(<symbol>)}.
     * <p>
     * Returns the symbol's description or {@link Undefined#UNDEFINED}.
     * 
     * @param symbol
     *            the symbol object
     * @return the symbol's description or {@link Undefined#UNDEFINED}
     */
    public static Object SymbolDescription(Symbol symbol) {
        return symbol.getDescription() != null ? symbol.getDescription() : UNDEFINED;
    }

    /**
     * Native function: {@code %ToPropertyKey(<value>)}.
     * <p>
     * Converts the input argument to a property key.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the input argument
     * @return the property key
     */
    public static Object ToPropertyKey(ExecutionContext cx, Object value) {
        return AbstractOperations.ToPropertyKey(cx, value);
    }

    /**
     * Native function: {@code %ToString(<value>)}.
     * <p>
     * Converts the input argument to a string.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the input argument
     * @return the string
     */
    public static CharSequence ToString(ExecutionContext cx, Object value) {
        return AbstractOperations.ToString(cx, value);
    }

    /**
     * Native function: {@code %WeakMapClear(<weakMap>)}.
     * <p>
     * Clears the weak map object.
     * 
     * @param cx
     *            the execution context
     * @param weakMap
     *            the weak map object
     */
    public static void WeakMapClear(ExecutionContext cx, Object weakMap) {
        if (!(weakMap instanceof WeakMapObject)) {
            throw Errors.newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        ((WeakMapObject) weakMap).getWeakMapData().clear();
    }

    /**
     * Native function: {@code %WeakSetClear(<weakSet>)}.
     * <p>
     * Clears the weak set object.
     * 
     * @param cx
     *            the execution context
     * @param weakSet
     *            the weak set object
     */
    public static void WeakSetClear(ExecutionContext cx, Object weakSet) {
        if (!(weakSet instanceof WeakSetObject)) {
            throw Errors.newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        ((WeakSetObject) weakSet).getWeakSetData().clear();
    }

    /**
     * Creates a new data property.
     * 
     * @param cx
     *            the execution context
     * @param object
     *            the script object
     * @param key
     *            the property key
     * @param value
     *            the property value
     */
    public static void CreateDataPropertyOrThrow(ExecutionContext cx, ScriptObject object, Object key, Object value) {
        AbstractOperations.CreateDataPropertyOrThrow(cx, object, ToPropertyKey(cx, key), value);
    }

    /**
     * Creates new method properties.
     * 
     * @param cx
     *            the execution context
     * @param object
     *            the script object
     * @param methods
     *            the methods holder
     */
    public static void CreateMethodProperties(ExecutionContext cx, ScriptObject object, ScriptObject methods) {
        for (Object propertyKey : methods.ownPropertyKeys(cx)) {
            Property property = methods.getOwnProperty(cx, propertyKey);
            if (property != null) {
                PropertyDescriptor method = property.toPropertyDescriptor();
                method.setEnumerable(false);
                object.defineOwnProperty(cx, propertyKey, method);
            }
        }
    }

    /**
     * Returns a getter property.
     * 
     * @param cx
     *            the execution context
     * @param object
     *            the script object
     * @param key
     *            the property key
     * @return the getter or {@code undefined}
     */
    public static Object LookupGetter(ExecutionContext cx, ScriptObject object, Object key) {
        Property desc = object.getOwnProperty(cx, ToPropertyKey(cx, key));
        return desc != null && desc.getGetter() != null ? desc.getGetter() : UNDEFINED;
    }

    /**
     * Returns the array buffer's byte order
     * 
     * @param cx
     *            the execution context
     * @param arrayBuffer
     *            the array buffer object
     * @return the byte order
     */
    public static String ByteOrder(ExecutionContext cx, ArrayBuffer arrayBuffer) {
        if (arrayBuffer.isDetached()) {
            return "";
        }
        return arrayBuffer.getData().order().toString();
    }

    /**
     * Returns the estimated code size for the given source code.
     * 
     * @param cx
     *            the execution context
     * @param sourceCode
     *            the source code
     * @return the estimated code size
     */
    public static int CodeSize(ExecutionContext cx, CharSequence sourceCode) {
        try {
            Source source = new Source("<script>", 1);
            com.github.anba.es6draft.ast.Script parsedScript = cx.getRealm().getScriptLoader().parseScript(source,
                    sourceCode.toString());
            return CodeSize.compute(parsedScript);
        } catch (ParserException e) {
            throw e.toScriptException(cx);
        }
    }

    /**
     * Returns an iterator over the class fields.
     * 
     * @param cx
     *            the execution context
     * @return the class fields iterator
     */
    public static Object GetClassFields(ExecutionContext cx) {
        FunctionObject currentFunction = cx.getCurrentFunction();
        Object[] classFields = ClassOperations.GetClassFields(currentFunction);
        return Arrays.asList((Object[]) classFields).iterator();
    }

    /**
     * Returns the next class field name.
     * 
     * @param cx
     *            the execution context
     * @param fieldsIterator
     *            the class field iterator
     * @return the next class field name
     */
    public static Object GetNextClassField(ExecutionContext cx, Object fieldsIterator) {
        assert fieldsIterator instanceof Iterator;
        Iterator<?> iterator = (Iterator<?>) fieldsIterator;
        assert iterator.hasNext();
        return iterator.next();
    }

    /**
     * Native function: {@code %MethodKind(<function>)}.
     * <p>
     * Returns the method kind of <var>function</var>.
     * 
     * @param function
     *            the function object
     * @return the method kind
     */
    public static Object MethodKind(Callable function) {
        if (!(function instanceof FunctionObject)) {
            return UNDEFINED;
        }
        RuntimeInfo.Function code = ((FunctionObject) function).getCode();
        if (code.is(RuntimeInfo.FunctionFlags.Method)) {
            if (code.is(RuntimeInfo.FunctionFlags.Async) && code.is(RuntimeInfo.FunctionFlags.Generator)) {
                return "async*";
            }
            if (code.is(RuntimeInfo.FunctionFlags.Async)) {
                return "async";
            }
            if (code.is(RuntimeInfo.FunctionFlags.Generator)) {
                return "*";
            }
            if (code.is(RuntimeInfo.FunctionFlags.Getter)) {
                return "get";
            }
            if (code.is(RuntimeInfo.FunctionFlags.Setter)) {
                return "set";
            }
            return "normal";
        }
        return UNDEFINED;
    }

    /**
     * Native function: {@code %FunctionName(<function>)}.
     * <p>
     * Returns the function name of <var>function</var>.
     * 
     * @param function
     *            the function object
     * @return the function name
     */
    public static Object FunctionName(Callable function) {
        if (!(function instanceof FunctionObject)) {
            return UNDEFINED;
        }
        RuntimeInfo.Function code = ((FunctionObject) function).getCode();
        return code.functionName();
    }
}
