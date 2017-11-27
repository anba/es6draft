/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.AbstractOperations.RequireObjectCoercible;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToPropertyKey;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.IndexedMap;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.PrivateName;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * 12.3.2 Property Accessors
 * <ul>
 * <li>12.3.2.1 Runtime Semantics: Evaluation
 * <ul>
 * <li>MemberExpression : MemberExpression [ Expression ]
 * <li>CallExpression : CallExpression [ Expression ]
 * </ul>
 * <li>12.3.2.1 Runtime Semantics: Evaluation
 * <ul>
 * <li>MemberExpression : MemberExpression . IdentifierName
 * <li>CallExpression : CallExpression . IdentifierName
 * </ul>
 * </ul>
 * <p>
 * 12.3.5 The super Keyword
 * <ul>
 * <li>12.3.5.3 Runtime Semantics: MakeSuperPropertyReference(propertyKey, strict)
 * </ul>
 */
public final class PropertyOperations {
    private PropertyOperations() {
    }

    private static Object toPropertyKey(double propertyKey) {
        long index = (long) propertyKey;
        if (index == propertyKey && IndexedMap.isIndex(index)) {
            return index;
        }
        return ToString(propertyKey);
    }

    private static Object toPropertyKey(String propertyKey) {
        long index = IndexedMap.toIndex(propertyKey);
        if (IndexedMap.isIndex(index)) {
            return index;
        }
        return propertyKey;
    }

    private static Object toPropertyKey(ExecutionContext cx, Object value) {
        Object propertyKey = ToPropertyKey(cx, value);
        if (propertyKey instanceof String) {
            return toPropertyKey((String) propertyKey);
        }
        return (Symbol) propertyKey;
    }

    /* *********************************************************************************************************** */

    public static Object checkAccessElement(Object baseValue, Object propertyName, ExecutionContext cx) {
        RequireObjectCoercible(cx, baseValue);
        if (Type.isString(propertyName)) {
            return toPropertyKey(Type.stringValue(propertyName).toString());
        }
        if (Type.isNumber(propertyName)) {
            return toPropertyKey(Type.numberValue(propertyName));
        }
        return toPropertyKey(cx, propertyName);
    }

    public static int checkAccessElement(Object baseValue, int propertyName, ExecutionContext cx) {
        RequireObjectCoercible(cx, baseValue);
        return propertyName;
    }

    public static long checkAccessElement(Object baseValue, long propertyName, ExecutionContext cx) {
        RequireObjectCoercible(cx, baseValue);
        return propertyName;
    }

    public static double checkAccessElement(Object baseValue, double propertyName, ExecutionContext cx) {
        RequireObjectCoercible(cx, baseValue);
        return propertyName;
    }

    public static String checkAccessElement(Object baseValue, String propertyName, ExecutionContext cx) {
        RequireObjectCoercible(cx, baseValue);
        return propertyName;
    }

    public static Object checkAccessProperty(Object baseValue, ExecutionContext cx) {
        return RequireObjectCoercible(cx, baseValue);
    }

    /* *********************************************************************************************************** */

    public static boolean deleteElement(Object baseValue, Object propertyName, ExecutionContext cx, boolean strict) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        Object propertyKey = checkAccessElement(baseValue, propertyName, cx);
        /* steps 7-8 */
        if (propertyKey instanceof String) {
            return Reference.PropertyNameReference.Delete(cx, baseValue, (String) propertyKey, strict);
        }
        if (propertyKey instanceof Long) {
            return Reference.PropertyIndexReference.Delete(cx, baseValue, (long) propertyKey, strict);
        }
        return Reference.PropertySymbolReference.Delete(cx, baseValue, (Symbol) propertyKey, strict);
    }

    public static boolean deleteElement(Object baseValue, int propertyName, ExecutionContext cx, boolean strict) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 7-8 */
        if (propertyName >= 0) {
            return Reference.PropertyIndexReference.Delete(cx, baseValue, propertyName, strict);
        }
        return Reference.PropertyNameReference.Delete(cx, baseValue, Integer.toString(propertyName), strict);
    }

    public static boolean deleteElement(Object baseValue, long propertyName, ExecutionContext cx, boolean strict) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 7-8 */
        return Reference.PropertyIndexReference.Delete(cx, baseValue, propertyName, strict);
    }

    public static boolean deleteElement(Object baseValue, double propertyName, ExecutionContext cx, boolean strict) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 7-8 */
        long index = (long) propertyName;
        if (index == propertyName && IndexedMap.isIndex(index)) {
            return Reference.PropertyIndexReference.Delete(cx, baseValue, index, strict);
        }
        return Reference.PropertyNameReference.Delete(cx, baseValue, ToString(propertyName), strict);
    }

    public static boolean deleteElement(Object baseValue, String propertyName, ExecutionContext cx, boolean strict) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 7-8 */
        long index = IndexedMap.toIndex(propertyName);
        if (IndexedMap.isIndex(index)) {
            return Reference.PropertyIndexReference.Delete(cx, baseValue, index, strict);
        }
        return Reference.PropertyNameReference.Delete(cx, baseValue, propertyName, strict);
    }

    public static boolean deleteProperty(Object baseValue, String propertyName, ExecutionContext cx, boolean strict) {
        /* steps 1-2 (generated code) */
        /* step 3 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 4-6 */
        return Reference.PropertyNameReference.Delete(cx, baseValue, propertyName, strict);
    }

    /* *********************************************************************************************************** */

    public static Reference<Object, ?> getElement(Object baseValue, Object propertyName, ExecutionContext cx,
            boolean strict) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        Object propertyKey = checkAccessElement(baseValue, propertyName, cx);
        /* steps 7-8 */
        if (propertyKey instanceof String) {
            return new Reference.PropertyNameReference(baseValue, (String) propertyKey, strict);
        }
        if (propertyKey instanceof Long) {
            return new Reference.PropertyIndexReference(baseValue, (long) propertyKey, strict);
        }
        return new Reference.PropertySymbolReference(baseValue, (Symbol) propertyKey, strict);
    }

    public static Reference<Object, String> getElement(Object baseValue, int propertyName, ExecutionContext cx,
            boolean strict) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 7-8 */
        if (propertyName >= 0) {
            return new Reference.PropertyIndexReference(baseValue, propertyName, strict);
        }
        return new Reference.PropertyNameReference(baseValue, Integer.toString(propertyName), strict);
    }

    public static Reference<Object, String> getElement(Object baseValue, long propertyName, ExecutionContext cx,
            boolean strict) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 7-8 */
        return new Reference.PropertyIndexReference(baseValue, propertyName, strict);
    }

    public static Reference<Object, String> getElement(Object baseValue, double propertyName, ExecutionContext cx,
            boolean strict) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 7-8 */
        long index = (long) propertyName;
        if (index == propertyName && IndexedMap.isIndex(index)) {
            return new Reference.PropertyIndexReference(baseValue, index, strict);
        }
        return new Reference.PropertyNameReference(baseValue, ToString(propertyName), strict);
    }

    public static Reference<Object, String> getElement(Object baseValue, String propertyName, ExecutionContext cx,
            boolean strict) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 7-8 */
        long index = IndexedMap.toIndex(propertyName);
        if (IndexedMap.isIndex(index)) {
            return new Reference.PropertyIndexReference(baseValue, index, strict);
        }
        return new Reference.PropertyNameReference(baseValue, propertyName, strict);
    }

    public static Reference<Object, String> getProperty(Object baseValue, String propertyName, ExecutionContext cx,
            boolean strict) {
        /* steps 1-2 (generated code) */
        /* step 3 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 4-6 */
        return new Reference.PropertyNameReference(baseValue, propertyName, strict);
    }

    /* *********************************************************************************************************** */

    public static Object getElementValue(Object baseValue, Object propertyName, ExecutionContext cx) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        Object propertyKey = checkAccessElement(baseValue, propertyName, cx);
        /* steps 7-8 */
        if (propertyKey instanceof String) {
            return Reference.PropertyNameReference.GetValue(cx, baseValue, (String) propertyKey);
        }
        if (propertyKey instanceof Long) {
            return Reference.PropertyIndexReference.GetValue(cx, baseValue, (long) propertyKey);
        }
        return Reference.PropertySymbolReference.GetValue(cx, baseValue, (Symbol) propertyKey);
    }

    public static Object getElementValue(Object baseValue, int propertyName, ExecutionContext cx) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 7-8 */
        if (propertyName >= 0) {
            return Reference.PropertyIndexReference.GetValue(cx, baseValue, propertyName);
        }
        return Reference.PropertyNameReference.GetValue(cx, baseValue, Integer.toString(propertyName));
    }

    public static Object getElementValue(Object baseValue, long propertyName, ExecutionContext cx) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 7-8 */
        return Reference.PropertyIndexReference.GetValue(cx, baseValue, propertyName);
    }

    public static Object getElementValue(Object baseValue, double propertyName, ExecutionContext cx) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 7-8 */
        long index = (long) propertyName;
        if (index == propertyName && IndexedMap.isIndex(index)) {
            return Reference.PropertyIndexReference.GetValue(cx, baseValue, index);
        }
        return Reference.PropertyNameReference.GetValue(cx, baseValue, ToString(propertyName));
    }

    public static Object getElementValue(Object baseValue, String propertyName, ExecutionContext cx) {
        /* steps 1-4 (generated code) */
        /* steps 5-6 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 7-8 */
        long index = IndexedMap.toIndex(propertyName);
        if (IndexedMap.isIndex(index)) {
            return Reference.PropertyIndexReference.GetValue(cx, baseValue, index);
        }
        return Reference.PropertyNameReference.GetValue(cx, baseValue, propertyName);
    }

    public static Object getPropertyValue(Object baseValue, String propertyName, ExecutionContext cx) {
        /* steps 1-2 (generated code) */
        /* step 3 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 4-6 */
        return Reference.PropertyNameReference.GetValue(cx, baseValue, propertyName);
    }

    /* *********************************************************************************************************** */

    public static void setElementValue(Object base, Object propertyKey, Object value, ExecutionContext cx,
            boolean strict) {
        if (propertyKey instanceof String) {
            Reference.PropertyNameReference.PutValue(cx, base, (String) propertyKey, value, strict);
        } else if (propertyKey instanceof Long) {
            Reference.PropertyIndexReference.PutValue(cx, base, (long) propertyKey, value, strict);
        } else {
            Reference.PropertySymbolReference.PutValue(cx, base, (Symbol) propertyKey, value, strict);
        }
    }

    public static void setElementValue(Object base, int propertyKey, Object value, ExecutionContext cx,
            boolean strict) {
        if (propertyKey >= 0) {
            Reference.PropertyIndexReference.PutValue(cx, base, (long) propertyKey, value, strict);
        } else {
            Reference.PropertyNameReference.PutValue(cx, base, Integer.toString(propertyKey), value, strict);
        }
    }

    public static void setElementValue(Object base, long propertyKey, Object value, ExecutionContext cx,
            boolean strict) {
        Reference.PropertyIndexReference.PutValue(cx, base, propertyKey, value, strict);
    }

    public static void setElementValue(Object base, double propertyKey, Object value, ExecutionContext cx,
            boolean strict) {
        long index = (long) propertyKey;
        if (index == propertyKey && IndexedMap.isIndex(index)) {
            Reference.PropertyIndexReference.PutValue(cx, base, index, value, strict);
        } else {
            setPropertyValue(base, ToString(propertyKey), value, cx, strict);
        }
    }

    public static void setElementValue(Object base, String propertyKey, Object value, ExecutionContext cx,
            boolean strict) {
        long index = IndexedMap.toIndex(propertyKey);
        if (IndexedMap.isIndex(index)) {
            Reference.PropertyIndexReference.PutValue(cx, base, index, value, strict);
        } else {
            Reference.PropertyNameReference.PutValue(cx, base, propertyKey, value, strict);
        }
    }

    public static void setPropertyValue(Object base, String propertyKey, Object value, ExecutionContext cx,
            boolean strict) {
        Reference.PropertyNameReference.PutValue(cx, base, propertyKey, value, strict);
    }

    /* *********************************************************************************************************** */

    public static FunctionEnvironmentRecord GetSuperEnvironmentRecord(ExecutionContext cx) {
        /* step 1 */
        EnvironmentRecord envRec = cx.getThisEnvironment();
        /* step 2 */
        if (!envRec.hasSuperBinding()) {
            throw newReferenceError(cx, Messages.Key.MissingSuperBinding);
        }
        assert envRec instanceof FunctionEnvironmentRecord : envRec.getClass().toString();
        return (FunctionEnvironmentRecord) envRec;
    }

    public static Object GetSuperThis(FunctionEnvironmentRecord envRec, ExecutionContext cx) {
        /* steps 3-4 */
        return envRec.getThisBinding(cx);
    }

    public static ScriptObject GetSuperBase(FunctionEnvironmentRecord envRec, ExecutionContext cx) {
        /* step 5 */
        ScriptObject baseValue = envRec.getSuperBase(cx);
        /* steps 6-7 */
        // RequireObjectCoercible(cx, baseValue);
        if (baseValue == null) {
            throw newTypeError(cx, Messages.Key.UndefinedOrNull);
        }
        return baseValue;
    }

    /* *********************************************************************************************************** */

    public static Object getSuperElement(Object propertyKey, Object actualThis, ScriptObject baseValue,
            ExecutionContext cx) {
        return baseValue.get(cx, propertyKey, actualThis);
    }

    public static Object getSuperProperty(String propertyKey, Object actualThis, ScriptObject baseValue,
            ExecutionContext cx) {
        return baseValue.get(cx, propertyKey, actualThis);
    }

    public static void setSuperElement(Object propertyKey, Object actualThis, ScriptObject baseValue, Object value,
            ExecutionContext cx, boolean strict) {
        boolean succeeded = baseValue.set(cx, propertyKey, value, actualThis);
        if (!succeeded && strict) {
            throw newTypeError(cx, Messages.Key.PropertyNotModifiable, propertyKey.toString());
        }
    }

    public static void setSuperProperty(String propertyKey, Object actualThis, ScriptObject baseValue, Object value,
            ExecutionContext cx, boolean strict) {
        boolean succeeded = baseValue.set(cx, (String) propertyKey, value, actualThis);
        if (!succeeded && strict) {
            throw newTypeError(cx, Messages.Key.PropertyNotModifiable, propertyKey);
        }
    }

    public static boolean deleteSuperElement(Object propertyKey, ExecutionContext cx) {
        /* steps 1-2 */
        FunctionEnvironmentRecord envRec = GetSuperEnvironmentRecord(cx);
        /* steps 3-4 */
        GetSuperThis(envRec, cx);
        /* steps 5-7 */
        GetSuperBase(envRec, cx);
        /* step 8 (omitted) */
        /* 12.5.3.2, step 5.a */
        throw newReferenceError(cx, Messages.Key.SuperDelete);
    }

    public static boolean deleteSuperProperty(String propertyKey, ExecutionContext cx) {
        /* steps 1-2 */
        FunctionEnvironmentRecord envRec = GetSuperEnvironmentRecord(cx);
        /* steps 3-4 */
        GetSuperThis(envRec, cx);
        /* steps 5-7 */
        GetSuperBase(envRec, cx);
        /* step 8 (omitted) */
        /* 12.5.3.2, step 5.a */
        throw newReferenceError(cx, Messages.Key.SuperDelete);
    }

    /* *********************************************************************************************************** */

    private static PrivateName resolvePrivateName(String name, ExecutionContext cx) {
        for (LexicalEnvironment<?> env = cx.getLexicalEnvironment(); env != null; env = env.getOuter()) {
            EnvironmentRecord envRec = env.getEnvRec();
            if (!(envRec instanceof DeclarativeEnvironmentRecord)) {
                break;
            }
            Object value = ((DeclarativeEnvironmentRecord) envRec).getBindingValueOrNull(name, true);
            if (value != null) {
                return (PrivateName) value;
            }
        }
        throw newReferenceError(cx, Messages.Key.UnresolvablePrivateField, name);
    }

    /**
     * PrivateFieldGet (P, O )
     * 
     * @param baseValue
     *            the object value
     * @param name
     *            the private name identifier
     * @param cx
     *            the execution context
     * @return the private name value
     */
    public static Object getPrivateValue(Object baseValue, String name, ExecutionContext cx) {
        PrivateName privateName = resolvePrivateName(name, cx);
        /* step 1 (not applicable) */
        /* step 2 */
        if (!Type.isObject(baseValue)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject obj = Type.objectValue(baseValue);
        /* steps 3, 5 */
        Property desc = obj.get(privateName);
        /* step 4 */
        if (desc == null) {
            throw newTypeError(cx, Messages.Key.PrivateFieldNotPresent, name);
        }
        /* step 6 */
        if (desc.isDataDescriptor()) {
            return desc.getValue();
        }
        /* step 7 */
        assert desc.isAccessorDescriptor();
        /* step 8 */
        Callable getter = desc.getGetter();
        /* step 9 */
        if (getter == null) {
            throw newTypeError(cx, Messages.Key.PrivateFieldNoGetter, name);
        }
        /* step 10 */
        return getter.call(cx, obj);
    }

    /**
     * PrivateFieldSet (P, O, value )
     * 
     * @param baseValue
     *            the object value
     * @param name
     *            the private name identifier
     * @param value
     *            the new value
     * @param cx
     *            the execution context
     */
    public static void setPrivateValue(Object baseValue, String name, Object value, ExecutionContext cx) {
        PrivateName privateName = resolvePrivateName(name, cx);
        /* step 1 (not applicable) */
        /* step 2 */
        if (!Type.isObject(baseValue)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject obj = Type.objectValue(baseValue);
        /* steps 3, 5 */
        Property desc = obj.get(privateName);
        /* step 4 */
        if (desc == null) {
            throw newTypeError(cx, Messages.Key.PrivateFieldNotPresent, name);
        }
        /* steps 6-7 */
        if (desc.isDataDescriptor()) {
            /* step 6.a */
            if (!desc.isWritable()) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, name);
            }
            /* step 6.b */
            desc.setValue(value);
        } else {
            /* step 7.a */
            assert desc.isAccessorDescriptor();
            /* step 7.b */
            Callable setter = desc.getSetter();
            /* step 7.c */
            if (setter == null) {
                throw newTypeError(cx, Messages.Key.PrivateFieldNoSetter, name);
            }
            /* step 7.d */
            setter.call(cx, obj, value);
        }
    }
}
