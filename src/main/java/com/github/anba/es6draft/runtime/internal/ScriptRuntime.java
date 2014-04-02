/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.*;
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromListIterator;
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromScriptIterator;
import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorYield;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArguments.CreateStrictArgumentsObject;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction.AsyncFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeMethod;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.SetFunctionName;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.GeneratorFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;
import static java.util.Arrays.asList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Iterator;

import org.mozilla.javascript.ConsString;

import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.*;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Different runtime support methods
 */
public final class ScriptRuntime {
    public static final Object[] EMPTY_ARRAY = new Object[0];

    private ScriptRuntime() {
    }

    /**
     * 18.2.1.2 EvalDeclarationInstantiation
     */
    public static void bindingNotPresentOrThrow(ExecutionContext cx, EnvironmentRecord envRec,
            String name) {
        if (envRec.hasBinding(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     */
    public static void canDeclareLexicalScopedOrThrow(ExecutionContext cx,
            GlobalEnvironmentRecord envRec, String name) {
        /* step 4.a */
        if (envRec.hasVarDeclaration(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
        /* step 4.b */
        if (envRec.hasLexicalDeclaration(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     */
    public static void canDeclareVarScopedOrThrow(ExecutionContext cx,
            GlobalEnvironmentRecord envRec, String name) {
        /* step 5.a */
        if (envRec.hasLexicalDeclaration(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     */
    public static void canDeclareGlobalFunctionOrThrow(ExecutionContext cx,
            GlobalEnvironmentRecord envRec, String fn) {
        /* steps 9.a.iii.1 - 9.a.iii.2 */
        boolean fnDefinable = envRec.canDeclareGlobalFunction(fn);
        if (!fnDefinable) {
            throw newTypeError(cx, Messages.Key.InvalidDeclaration, fn);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     */
    public static void canDeclareGlobalVarOrThrow(ExecutionContext cx,
            GlobalEnvironmentRecord envRec, String vn) {
        /* steps 11.a.iii.1.a - 11.a.iii.1.b */
        boolean vnDefinable = envRec.canDeclareGlobalVar(vn);
        if (!vnDefinable) {
            throw newTypeError(cx, Messages.Key.InvalidDeclaration, vn);
        }
    }

    /* ***************************************************************************************** */

    /**
     * 12.1.4.1 Array Literal
     * <p>
     * 12.1.4.1.2 Runtime Semantics: Array Accumulation
     * <ul>
     * <li>ElementList : Elision<sub>opt</opt> AssignmentExpression
     * <li>ElementList : ElementList , Elision<sub>opt</opt> AssignmentExpression
     * </ul>
     */
    public static void defineProperty(ScriptObject array, int nextIndex, Object value,
            ExecutionContext cx) {
        // String propertyName = ToString(ToUint32(nextIndex));
        String propertyName = ToString(nextIndex);
        boolean created = array.defineOwnProperty(cx, propertyName, new PropertyDescriptor(value,
                true, true, true));
        assert created;
    }

    /**
     * 12.1.4.1 Array Literal
     * <p>
     * 12.1.4.1.2 Runtime Semantics: Array Accumulation
     * <ul>
     * <li>SpreadElement : ... AssignmentExpression
     * </ul>
     */
    public static int ArrayAccumulationSpreadElement(ScriptObject array, int nextIndex,
            Object spreadObj, ExecutionContext cx) {
        /* steps 1-3 (cf. generated code) */
        /* step 4 */
        if (!Type.isObject(spreadObj)) {
            // FIXME: spec bug ? why restrict to objects?
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* steps 5-6 */
        ScriptObject iterator = GetIterator(cx, spreadObj);
        /* step 7 */
        for (;;) {
            ScriptObject next = IteratorStep(cx, iterator);
            if (next == null) {
                return nextIndex;
            }
            Object nextValue = IteratorValue(cx, next);
            defineProperty(array, nextIndex, nextValue, cx);
            nextIndex += 1;
        }
    }

    /**
     * 12.1.5 Object Initialiser
     * <p>
     * 12.1.5.8 Runtime Semantics: PropertyDefinitionEvaluation
     */
    public static void defineProperty(ScriptObject object, Object propertyName, Object value,
            ExecutionContext cx) {
        DefinePropertyOrThrow(cx, object, propertyName, new PropertyDescriptor(value, true, true,
                true));
    }

    /**
     * 12.1.5 Object Initialiser
     * <p>
     * 12.1.5.8 Runtime Semantics: PropertyDefinitionEvaluation
     */
    public static void defineProperty(ScriptObject object, String propertyName, Object value,
            ExecutionContext cx) {
        DefinePropertyOrThrow(cx, object, propertyName, new PropertyDescriptor(value, true, true,
                true));
    }

    /**
     * 12.1.5 Object Initialiser
     * <p>
     * 12.1.5.8 Runtime Semantics: PropertyDefinitionEvaluation
     */
    public static void updateMethod(ScriptObject object, Object propertyName, FunctionObject f) {
        f.updateMethod(propertyName, object);
    }

    /**
     * 12.1.7 Generator Comprehensions
     * <p>
     * 12.1.7.2 Runtime Semantics: Evaluation
     */
    public static ScriptObject EvaluateGeneratorComprehension(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        /* step 1 (omitted) */
        /* step 2 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 3-4 (not applicable) */
        /* step 5 */
        OrdinaryGenerator closure = GeneratorFunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        /* step 6 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 7 */
        MakeConstructor(cx, closure, true, prototype);
        /* step 8 */
        GeneratorObject iterator = (GeneratorObject) closure.call(cx, UNDEFINED);
        /* step 9 */
        return iterator;
    }

    /**
     * 12.1.7 Generator Comprehensions
     * <p>
     * Runtime Semantics: Evaluation
     */
    public static ScriptObject EvaluateLegacyGeneratorComprehension(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        /* step 1 (omitted) */
        /* step 2 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 3-4 (not applicable) */
        /* step 5 */
        OrdinaryGenerator closure = GeneratorFunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        /* step 6 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.LegacyGeneratorPrototype);
        /* step 7 */
        MakeConstructor(cx, closure, true, prototype);
        /* step 8 */
        GeneratorObject iterator = (GeneratorObject) closure.call(cx, UNDEFINED);
        /* step 9 */
        return iterator;
    }

    /**
     * 12.1.9 Template Literals
     * <p>
     * 12.1.9.2.2 Runtime Semantics: GetTemplateCallSite
     */
    public static ScriptObject GetTemplateCallSite(String key, MethodHandle handle,
            ExecutionContext cx) {
        Realm realm = cx.getRealm();
        /* step 1 */
        ScriptObject callSite = realm.getTemplateCallSite(key);
        if (callSite != null) {
            return callSite;
        }
        /* steps 2-3 */
        String[] strings = evaluateCallSite(handle);
        assert (strings.length & 1) == 0;
        /* step 4 */
        int count = strings.length >>> 1;
        /* steps 5-6 */
        ScriptObject siteObj = ArrayCreate(cx, count);
        ScriptObject rawObj = ArrayCreate(cx, count);
        /* steps 7-8 */
        for (int i = 0, n = strings.length; i < n; i += 2) {
            int index = i >>> 1;
            String prop = ToString(index);
            String cookedValue = strings[i];
            siteObj.defineOwnProperty(cx, prop, new PropertyDescriptor(cookedValue, false, true,
                    false));
            String rawValue = strings[i + 1];
            rawObj.defineOwnProperty(cx, prop, new PropertyDescriptor(rawValue, false, true, false));
        }
        /* steps 9-11 */
        SetIntegrityLevel(cx, rawObj, IntegrityLevel.Frozen);
        siteObj.defineOwnProperty(cx, "raw", new PropertyDescriptor(rawObj, false, false, false));
        SetIntegrityLevel(cx, siteObj, IntegrityLevel.Frozen);
        /* step 12 */
        realm.addTemplateCallSite(key, siteObj);
        /* step 13 */
        return siteObj;
    }

    private static String[] evaluateCallSite(MethodHandle handle) {
        try {
            return (String[]) handle.invokeExact();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 12.2.2 Property Accessors
     * <p>
     * 12.2.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     */
    public static Reference<Object, String> getProperty(Object baseValue,
            String propertyNameString, ExecutionContext cx, boolean strict) {
        /* steps 1-6 (generated code) */
        /* step 7 */
        CheckObjectCoercible(cx, baseValue);
        /* steps 8-10 */
        return new Reference.PropertyNameReference(baseValue, propertyNameString, strict);
    }

    /**
     * 12.2.2 Property Accessors
     * <p>
     * 12.2.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     */
    public static Object getPropertyValue(Object baseValue, String propertyNameString,
            ExecutionContext cx, boolean strict) {
        /* steps 1-6 (generated code) */
        /* step 7 */
        CheckObjectCoercible(cx, baseValue);
        /* steps 8-10 */
        Reference<Object, String> ref = new Reference.PropertyNameReference(baseValue,
                propertyNameString, strict);
        return ref.getValue(cx);
    }

    /**
     * 12.2.2 Property Accessors
     * <p>
     * 12.2.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression [ Expression ]
     * <li>CallExpression : CallExpression [ Expression ]
     * </ul>
     */
    public static Reference<Object, ?> getElement(Object baseValue, Object propertyNameValue,
            ExecutionContext cx, boolean strict) {
        /* steps 1-6 (generated code) */
        /* step 7 */
        CheckObjectCoercible(cx, baseValue);
        /* step 8 */
        Object propertyKey = ToPropertyKey(cx, propertyNameValue);
        /* steps 9-10 */
        if (propertyKey instanceof String) {
            return new Reference.PropertyNameReference(baseValue, (String) propertyKey, strict);
        }
        return new Reference.PropertySymbolReference(baseValue, (Symbol) propertyKey, strict);
    }

    /**
     * 12.2.2 Property Accessors
     * <p>
     * 12.2.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression [ Expression ]
     * <li>CallExpression : CallExpression [ Expression ]
     * </ul>
     */
    public static Object getElementValue(Object baseValue, Object propertyNameValue,
            ExecutionContext cx, boolean strict) {
        /* steps 1-6 (generated code) */
        /* step 7 */
        CheckObjectCoercible(cx, baseValue);
        /* step 8 */
        Object propertyKey = ToPropertyKey(cx, propertyNameValue);
        /* steps 9-10 */
        if (propertyKey instanceof String) {
            Reference<Object, String> ref = new Reference.PropertyNameReference(baseValue,
                    (String) propertyKey, strict);
            return ref.getValue(cx);
        }
        Reference<Object, Symbol> ref = new Reference.PropertySymbolReference(baseValue,
                (Symbol) propertyKey, strict);
        return ref.getValue(cx);
    }

    /**
     * 12.2.3 The new Operator
     * <p>
     * 12.2.3.1 Runtime Semantics: Evaluation<br>
     * 12.2.5.2 Runtime Semantics: Evaluation
     * <ul>
     * <li>NewExpression : new NewExpression
     * <li>MemberExpression : new MemberExpression Arguments
     * <li>MemberExpression : new super Arguments<sub>opt</sub>
     * </ul>
     */
    public static Object EvaluateConstructorCall(Object constructor, Object[] args,
            ExecutionContext cx) {
        /* steps 1-3/1-3/1-6 (generated code) */
        /* steps 4/6/7 */
        if (!IsConstructor(constructor)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        /* steps 5/7/8 */
        return ((Constructor) constructor).construct(cx, args);
    }

    /**
     * 12.2.3 The new Operator
     * <p>
     * 12.2.3.1 Runtime Semantics: Evaluation<br>
     * 12.2.5.2 Runtime Semantics: Evaluation
     * <ul>
     * <li>NewExpression : new NewExpression
     * <li>MemberExpression : new MemberExpression Arguments
     * <li>MemberExpression : new super Arguments<sub>opt</sub>
     * </ul>
     */
    public static Object EvaluateConstructorTailCall(Object constructor, Object[] args,
            ExecutionContext cx) {
        /* steps 1-3/1-3/1-6 (generated code) */
        /* steps 4/6/7 */
        if (!IsConstructor(constructor)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        /* steps 5/7/8 */
        return PrepareForTailCall(args, null, (Constructor) constructor);
    }

    /**
     * 12.2.4 Function Calls
     * <p>
     * Runtime Semantics: EvaluateCall Abstract Operation
     */
    public static Callable CheckCallable(Object func, ExecutionContext cx) {
        /* step 5 */
        if (!Type.isObject(func)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 6 */
        if (!IsCallable(func)) {
            throw newTypeError(cx, Messages.Key.NotCallable);
        }
        return (Callable) func;
    }

    /**
     * 12.2.4 Function Calls
     * <p>
     * Runtime Semantics: EvaluateCall Abstract Operation
     */
    public static boolean IsBuiltinEval(Object ref, Callable f, ExecutionContext cx) {
        if (ref instanceof Reference) {
            Reference<?, ?> r = (Reference<?, ?>) ref;
            if (!r.isPropertyReference()) {
                assert !r.isUnresolvableReference() && r.getBase() instanceof EnvironmentRecord;
                return (f == cx.getRealm().getBuiltinEval());
            }
        }
        return false;
    }

    /**
     * 12.2.4 Function Calls
     * <p>
     * Runtime Semantics: EvaluateCall Abstract Operation
     */
    public static boolean IsBuiltinEval(Callable f, ExecutionContext cx) {
        return (f == cx.getRealm().getBuiltinEval());
    }

    /**
     * 12.2.4 Function Calls
     * <p>
     * Runtime Semantics: EvaluateCall Abstract Operation
     */
    public static Callable directEvalFallbackHook(ExecutionContext cx) {
        return cx.getRealm().getDirectEvalFallback();
    }

    /**
     * 12.2.4 Function Calls
     * <p>
     * Runtime Semantics: EvaluateCall Abstract Operation
     */
    public static Object[] directEvalFallbackArguments(Object[] args, Object thisValue,
            Callable callee) {
        Object[] fallbackArgs = new Object[2 + args.length];
        fallbackArgs[0] = thisValue;
        fallbackArgs[1] = callee;
        System.arraycopy(args, 0, fallbackArgs, 2, args.length);
        return fallbackArgs;
    }

    /**
     * 12.2.5 The super Keyword
     * <p>
     * Runtime Semantics: Abstract Operation MakeSuperReference(propertyKey, strict)
     */
    public static Reference<ScriptObject, ?> MakeSuperReference(ExecutionContext cx,
            Object propertyKey, boolean strict) {
        assert propertyKey == null || propertyKey instanceof String
                || propertyKey instanceof Symbol;
        EnvironmentRecord envRec = cx.getThisEnvironment();
        if (!envRec.hasSuperBinding()) {
            throw newReferenceError(cx, Messages.Key.MissingSuperBinding);
        }
        assert envRec instanceof FunctionEnvironmentRecord;
        Object actualThis = envRec.getThisBinding();
        ScriptObject baseValue = ((FunctionEnvironmentRecord) envRec).getSuperBase();
        // CheckObjectCoercible(cx.getRealm(), baseValue);
        if (baseValue == null) {
            throw newTypeError(cx, Messages.Key.UndefinedOrNull);
        }
        if (propertyKey == null) {
            propertyKey = ((FunctionEnvironmentRecord) envRec).getMethodName();
            if (propertyKey == null) {
                throw newReferenceError(cx, Messages.Key.MissingSuperBinding);
            }
        }
        if (propertyKey instanceof Symbol) {
            return new Reference.SuperSymbolReference(baseValue, (Symbol) propertyKey, strict,
                    actualThis);
        }
        return new Reference.SuperNameReference(baseValue, (String) propertyKey, strict, actualThis);
    }

    /**
     * 12.2.5 The super Keyword
     * <p>
     * Runtime Semantics: Abstract Operation MakeSuperReference(propertyKey, strict)
     */
    public static Reference<ScriptObject, String> MakeSuperReference(ExecutionContext cx,
            String propertyKey, boolean strict) {
        assert propertyKey != null;
        EnvironmentRecord envRec = cx.getThisEnvironment();
        if (!envRec.hasSuperBinding()) {
            throw newReferenceError(cx, Messages.Key.MissingSuperBinding);
        }
        assert envRec instanceof FunctionEnvironmentRecord;
        Object actualThis = envRec.getThisBinding();
        ScriptObject baseValue = ((FunctionEnvironmentRecord) envRec).getSuperBase();
        // CheckObjectCoercible(cx.getRealm(), baseValue);
        if (baseValue == null) {
            throw newTypeError(cx, Messages.Key.UndefinedOrNull);
        }
        return new Reference.SuperNameReference(baseValue, propertyKey, strict, actualThis);
    }

    /**
     * 12.2.6 Argument Lists
     * <p>
     * 12.2.6.1 Runtime Semantics: ArgumentListEvaluation
     */
    public static Object[] SpreadArray(Object spreadObj, ExecutionContext cx) {
        final int MAX_ARGS = FunctionPrototype.getMaxArguments();
        /* step 1 */
        ArrayList<Object> list = new ArrayList<>();
        /* steps 2-4 (cf. generated code) */
        /* step 5 */
        if (!Type.isObject(spreadObj)) {
            // FIXME: spec bug ? why restrict to objects?
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* steps 6-7 */
        ScriptObject iterator = GetIterator(cx, spreadObj);
        /* step 8 */
        for (int n = 0; n <= MAX_ARGS; ++n) {
            ScriptObject next = IteratorStep(cx, iterator);
            if (next == null) {
                return list.toArray(new Object[n]);
            }
            Object nextArg = IteratorValue(cx, next);
            list.add(nextArg);
        }
        throw newRangeError(cx, Messages.Key.FunctionTooManyArguments);
    }

    /**
     * 12.2.6 Argument Lists
     * <p>
     * 12.2.6.1 Runtime Semantics: ArgumentListEvaluation
     */
    public static Object[] toFlatArray(Object[] array, ExecutionContext cx) {
        final int MAX_ARGS = FunctionPrototype.getMaxArguments();
        int newlen = array.length;
        for (int i = 0, len = array.length; i < len; ++i) {
            if (array[i] instanceof Object[]) {
                newlen += ((Object[]) array[i]).length - 1;
                if (newlen > MAX_ARGS) {
                    throw newRangeError(cx, Messages.Key.FunctionTooManyArguments);
                }
            }
        }
        Object[] result = new Object[newlen];
        for (int i = 0, j = 0, len = array.length; i < len; ++i) {
            if (array[i] instanceof Object[]) {
                Object[] a = (Object[]) array[i];
                System.arraycopy(a, 0, result, j, a.length);
                j += a.length;
            } else {
                result[j++] = array[i];
            }
        }
        return result;
    }

    /**
     * 12.4 Unary Operators<br>
     * 12.4.4 The delete Operator
     */
    public static boolean delete(Reference<?, ?> ref, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 4-6 */
        if (ref.isPropertyReference()) {
            return deleteProperty(ref, cx);
        }
        return deleteBinding(ref, cx);
    }

    /**
     * 12.4 Unary Operators<br>
     * 12.4.4 The delete Operator
     */
    public static boolean deleteBinding(Reference<?, ?> ref, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 5 (not applicable) */
        assert !ref.isPropertyReference();
        /* step 4 */
        if (ref.isUnresolvableReference()) {
            // TODO: spec issue - change to assert, cf. early error restriction
            if (ref.isStrictReference()) {
                throw newSyntaxError(cx, Messages.Key.UnqualifiedDelete);
            }
            return true;
        }
        /* step 6 */
        if (ref instanceof Reference.BindingReference) {
            return false;
        }
        assert ref instanceof Reference.IdentifierReference;
        Reference.IdentifierReference idref = (Reference.IdentifierReference) ref;
        EnvironmentRecord bindings = idref.getBase();
        return bindings.deleteBinding(idref.getReferencedName());
    }

    /**
     * 12.4 Unary Operators<br>
     * 12.4.4 The delete Operator
     */
    public static boolean deleteProperty(Reference<?, ?> ref, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* steps 4, 6 (not applicable) */
        assert ref.isPropertyReference() && !ref.isUnresolvableReference();
        /* step 5 */
        if (ref.isSuperReference()) {
            throw newReferenceError(cx, Messages.Key.SuperDelete);
        }
        ScriptObject obj = ToObject(cx, ref.getBase());
        boolean deleteStatus;
        Object referencedName = ref.getReferencedName();
        if (referencedName instanceof String) {
            deleteStatus = obj.delete(cx, (String) referencedName);
        } else {
            deleteStatus = obj.delete(cx, (Symbol) referencedName);
        }
        if (!deleteStatus && ref.isStrictReference()) {
            throw newTypeError(cx, Messages.Key.PropertyNotDeletable, ref.getReferencedName()
                    .toString());
        }
        return deleteStatus;
    }

    /**
     * 12.4 Unary Operators<br>
     * 12.4.6 The typeof Operator
     */
    public static String typeof(Object val, ExecutionContext cx) {
        /* step 1 (generated code) */
        /* step 2 */
        if (val instanceof Reference) {
            Reference<?, ?> ref = (Reference<?, ?>) val;
            if (ref.isUnresolvableReference()) {
                return "undefined";
            }
            val = ref.getValue(cx);
        }
        /* steps 3-4 */
        switch (Type.of(val)) {
        case Undefined:
            return "undefined";
        case Null:
            return "object";
        case Boolean:
            return "boolean";
        case Number:
            return "number";
        case String:
            return "string";
        case Symbol:
            return "symbol";
        case Object:
        default:
            if (IsCallable(val)) {
                return "function";
            }
            return "object";
        }
    }

    /**
     * 12.6 Additive Operators<br>
     * 12.6.3 The Addition operator ( + )
     */
    public static Object add(Object lval, Object rval, ExecutionContext cx) {
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        Object lprim = ToPrimitive(cx, lval);
        /* steps 9-10 */
        Object rprim = ToPrimitive(cx, rval);
        /* step 11 */
        if (Type.isString(lprim) || Type.isString(rprim)) {
            CharSequence lstr = ToString(cx, lprim);
            CharSequence rstr = ToString(cx, rprim);
            return add(lstr, rstr, cx);
        }
        /* step 12 */
        return ToNumber(cx, lprim) + ToNumber(cx, rprim);
    }

    /**
     * 12.6 Additive Operators<br>
     * 12.6.3 The Addition operator ( + )
     */
    public static CharSequence add(CharSequence lstr, CharSequence rstr, ExecutionContext cx) {
        int llen = lstr.length(), rlen = rstr.length();
        if (llen == 0) {
            return rstr;
        }
        if (rlen == 0) {
            return lstr;
        }
        int newlen = llen + rlen;
        if (newlen < 0) {
            throw newInternalError(cx, Messages.Key.OutOfMemory);
        }
        if (newlen <= 10) {
            return new StringBuilder(newlen).append(lstr).append(rstr).toString();
        }
        return new ConsString(lstr, rstr);
    }

    /**
     * 12.8 Relational Operators<br>
     * 12.8.3 Runtime Semantics: Evaluation
     */
    public static int relationalComparison(Object x, Object y, boolean leftFirst,
            ExecutionContext cx) {
        return RelationalComparison(cx, x, y, leftFirst);
    }

    /**
     * 12.8 Relational Operators<br>
     * 12.8.3 Runtime Semantics: Evaluation
     */
    public static boolean in(Object lval, Object rval, ExecutionContext cx) {
        /* steps 1-6 (generated code) */
        /* step 7 */
        if (!Type.isObject(rval)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 8 */
        return HasProperty(cx, Type.objectValue(rval), ToPropertyKey(cx, lval));
    }

    /**
     * 12.8 Relational Operators<br>
     * 12.8.4 Runtime Semantics: InstanceofOperator(O, C)
     */
    public static boolean InstanceofOperator(Object obj, Object constructor, ExecutionContext cx) {
        /* step 1 */
        if (!Type.isObject(constructor)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* steps 2-3 */
        Callable instOfHandler = GetMethod(cx, Type.objectValue(constructor),
                BuiltinSymbol.hasInstance.get());
        /* step 4 */
        if (instOfHandler != null) {
            Object result = instOfHandler.call(cx, constructor, obj);
            return ToBoolean(result);
        }
        /* step 5 */
        if (!IsCallable(constructor)) {
            throw newTypeError(cx, Messages.Key.NotCallable);
        }
        /* step 6 */
        return OrdinaryHasInstance(cx, constructor, obj);
    }

    /**
     * 12.9 Equality Operators<br>
     * 12.9.3 Runtime Semantics: Evaluation
     */
    public static boolean equalityComparison(Object x, Object y, ExecutionContext cx) {
        return EqualityComparison(cx, x, y);
    }

    /**
     * 12.9 Equality Operators<br>
     * 12.9.3 Runtime Semantics: Evaluation
     */
    public static boolean strictEqualityComparison(Object x, Object y) {
        return StrictEqualityComparison(x, y);
    }

    /**
     * 12.13.2 Runtime Semantics<br>
     * Runtime Semantics: Evaluation
     * <p>
     * 13.2.1.2 Runtime Semantics<br>
     * Runtime Semantics: Evaluation
     * <p>
     * 13.2.2.2 Runtime Semantics<br>
     * Runtime Semantics: Evaluation
     */
    public static ScriptObject ensureObject(Object val, ExecutionContext cx) {
        if (!Type.isObject(val)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        return Type.objectValue(val);
    }

    /**
     * 12.13.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialisation
     */
    public static ScriptObject createRestArray(Iterator<?> iterator, ExecutionContext cx) {
        ScriptObject result = ArrayCreate(cx, 0);
        for (int n = 0; iterator.hasNext(); ++n) {
            Object nextValue = iterator.next();
            CreateDataPropertyOrThrow(cx, result, ToString(n), nextValue);
        }
        return result;
    }

    /**
     * 12.13.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialisation
     */
    public static Iterator<?> getIterator(ScriptObject obj, ExecutionContext cx) {
        return FromScriptIterator(cx, GetIterator(cx, obj));
    }

    /**
     * 12.13.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialisation
     */
    public static void iteratorNextAndIgnore(Iterator<?> iterator) {
        if (iterator.hasNext()) {
            iterator.next();
        }
    }

    /**
     * 12.13.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialisation
     */
    public static Object iteratorNextOrUndefined(Iterator<?> iterator) {
        return iterator.hasNext() ? iterator.next() : UNDEFINED;
    }

    /* ***************************************************************************************** */

    /**
     * 13.6.4 The for-in and for-of Statements
     * <p>
     * 13.6.4.6 Runtime Semantics: ForIn/OfExpressionEvaluation Abstract Operation
     */
    public static Iterator<?> enumerate(Object o, ExecutionContext cx) {
        /* step 5 */
        ScriptObject obj = ToObject(cx, o);
        /* step 6, step 8 */
        ScriptObject keys = obj.enumerate(cx);
        /* step 9 */
        return FromListIterator(cx, obj, keys);
    }

    /**
     * 13.6.4 The for-in and for-of Statements
     * <p>
     * 13.6.4.6 Runtime Semantics: ForIn/OfExpressionEvaluation Abstract Operation
     */
    public static Iterator<?> iterate(Object o, ExecutionContext cx) {
        /* step 5 */
        ScriptObject obj = ToObject(cx, o);
        /* step 7, step 8 */
        ScriptObject keys = GetIterator(cx, obj);
        /* step 9 */
        return FromScriptIterator(cx, keys);
    }

    /**
     * 13.6.4 The for-in and for-of Statements<br>
     * Extension: 'for-each' statement
     * <p>
     * 13.6.4.6 Runtime Semantics: ForIn/OfExpressionEvaluation Abstract Operation
     */
    public static Iterator<?> enumerateValues(Object o, ExecutionContext cx) {
        /* step 5 */
        ScriptObject obj = ToObject(cx, o);
        /* step 6, step 8 */
        ScriptObject keys = obj.enumerate(cx);
        /* step 9 */
        return new ValuesIterator(cx, obj, FromListIterator(cx, obj, keys));
    }

    private static final class ValuesIterator extends SimpleIterator<Object> {
        private final ExecutionContext cx;
        private final ScriptObject object;
        private final Iterator<?> keysIterator;

        ValuesIterator(ExecutionContext cx, ScriptObject object, Iterator<?> keysIterator) {
            this.cx = cx;
            this.object = object;
            this.keysIterator = keysIterator;
        }

        @Override
        protected Object tryNext() {
            if (keysIterator.hasNext()) {
                Object pk = ToPropertyKey(cx, keysIterator.next());
                return Get(cx, object, pk);
            }
            return null;
        }
    }

    /**
     * 13.14 The try Statement
     */
    public static StackOverflowError getStackOverflowError(Error e) {
        if (e instanceof StackOverflowError) {
            return (StackOverflowError) e;
        }
        Throwable cause = e.getCause();
        if (cause instanceof StackOverflowError) {
            return (StackOverflowError) cause;
        }
        throw e;
    }

    /**
     * 13.14 The try Statement
     */
    public static ScriptException toInternalError(StackOverflowError e, ExecutionContext cx) {
        ScriptException exception = newInternalError(cx, Messages.Key.StackOverflow,
                "StackOverflow");
        // use stacktrace from original error
        exception.setStackTrace(e.getStackTrace());
        return exception;
    }

    /**
     * 13.15 The debugger statement
     */
    public static void debugger() {
        // breakpoint
    }

    /* ***************************************************************************************** */

    /**
     * 14.1 Function Definitions
     * <p>
     * 14.1.14 Runtime Semantics: InstantiateFunctionObject
     */
    public static OrdinaryFunction InstantiateFunctionObject(LexicalEnvironment<?> scope,
            ExecutionContext cx, RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        OrdinaryFunction f = FunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 4 */
        if (fd.hasSuperReference()) {
            MakeMethod(f, name, null);
        }
        /* step 5 */
        MakeConstructor(cx, f);
        /* step 6 */
        SetFunctionName(f, name);
        /* step 7 */
        return f;
    }

    /**
     * 14.1 Function Definitions
     * <p>
     * 14.1.15 Runtime Semantics: Evaluation
     * <ul>
     * <li>FunctionExpression : function ( FormalParameters ) { FunctionBody }
     * <li>FunctionExpression : function BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     */
    public static OrdinaryFunction EvaluateFunctionExpression(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        OrdinaryFunction closure;
        if (!fd.hasScopedName()) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = FunctionCreate(cx, FunctionKind.Normal, fd, scope);
            /* step 4 */
            if (fd.hasSuperReference()) {
                MakeMethod(closure, (String) null, null);
            }
            /* step 5 */
            MakeConstructor(cx, closure);
        } else {
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = LexicalEnvironment
                    .newDeclarativeEnvironment(scope);
            /* step 3 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 4 */
            String name = fd.functionName();
            /* step 5 */
            envRec.createImmutableBinding(name);
            /* step 6 */
            closure = FunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 7 */
            if (fd.hasSuperReference()) {
                MakeMethod(closure, name, null);
            }
            /* step 8 */
            MakeConstructor(cx, closure);
            /* step 9 */
            SetFunctionName(closure, name);
            /* step 10 */
            envRec.initialiseBinding(name, closure);
        }
        /* step 6/11 */
        return closure;
    }

    /**
     * 14.2 Arrow Function Definitions
     * <p>
     * Runtime Semantics: Evaluation
     * <ul>
     * <li>ArrowFunction : ArrowParameters => ConciseBody
     * </ul>
     */
    public static OrdinaryFunction EvaluateArrowFunction(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        /* step 1 (not applicable) */
        /* step 2 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 3-4 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        /* step 5 */
        return closure;
    }

    /**
     * 14.3 Method Definitions, 14.5 Class Definitions
     * <p>
     * 14.3.8 Runtime Semantics: DefineMethod<br>
     * 14.5.15 Runtime Semantics: ClassDefinitionEvaluation
     */
    public static OrdinaryFunction EvaluateConstructorMethod(ScriptObject constructorParent,
            ScriptObject proto, RuntimeInfo.Function fd, ExecutionContext cx) {
        // ClassDefinitionEvaluation - steps 9-10
        // -> calls DefineMethod
        String propKey = "constructor";
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        OrdinaryFunction constructor = FunctionCreate(cx, FunctionKind.ConstructorMethod, fd,
                scope, constructorParent);
        if (fd.hasSuperReference()) {
            MakeMethod(constructor, propKey, proto);
        }

        // ClassDefinitionEvaluation - step 11
        MakeConstructor(cx, constructor, false, proto);

        // ClassDefinitionEvaluation - step 13
        PropertyDescriptor desc = new PropertyDescriptor(constructor, true, false, true);

        // ClassDefinitionEvaluation - step 14
        proto.defineOwnProperty(cx, propKey, desc);

        return constructor;
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinition(ScriptObject object, Object propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinition(object, (String) propKey, fd, cx);
        } else {
            EvaluatePropertyDefinition(object, (Symbol) propKey, fd, cx);
        }
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinition(ScriptObject object, String propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (DefineMethod) */
        /* DefineMethod: steps 1-3 (generated code) */
        /* DefineMethod: step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* DefineMethod: step 5 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* DefineMethod: step 6 */
        if (fd.hasSuperReference()) {
            MakeMethod(closure, propKey, object);
        }
        /* step 3 */
        SetFunctionName(closure, propKey);
        /* step 4 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, true, true);
        /* step 5 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinition(ScriptObject object, Symbol propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (DefineMethod) */
        /* DefineMethod: steps 1-3 (generated code) */
        /* DefineMethod: step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* DefineMethod: step 5 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* DefineMethod: step 6 */
        if (fd.hasSuperReference()) {
            MakeMethod(closure, propKey, object);
        }
        /* step 3 */
        SetFunctionName(closure, propKey);
        /* step 4 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, true, true);
        /* step 5 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>get PropertyName ( ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionGetter(ScriptObject object, Object propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinitionGetter(object, (String) propKey, fd, cx);
        } else {
            EvaluatePropertyDefinitionGetter(object, (Symbol) propKey, fd, cx);
        }
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>get PropertyName ( ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionGetter(ScriptObject object, String propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 5-6 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 7 */
        if (fd.hasSuperReference()) {
            MakeMethod(closure, propKey, object);
        }
        /* step 8 */
        SetFunctionName(closure, propKey, "get");
        /* step 9 */
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setGetter(closure);
        desc.setEnumerable(true);
        desc.setConfigurable(true);
        /* step 10 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>get PropertyName ( ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionGetter(ScriptObject object, Symbol propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 5-6 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 7 */
        if (fd.hasSuperReference()) {
            MakeMethod(closure, propKey, object);
        }
        /* step 8 */
        SetFunctionName(closure, propKey, "get");
        /* step 9 */
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setGetter(closure);
        desc.setEnumerable(true);
        desc.setConfigurable(true);
        /* step 10 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionSetter(ScriptObject object, Object propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinitionSetter(object, (String) propKey, fd, cx);
        } else {
            EvaluatePropertyDefinitionSetter(object, (Symbol) propKey, fd, cx);
        }
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionSetter(ScriptObject object, String propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        if (fd.hasSuperReference()) {
            MakeMethod(closure, propKey, object);
        }
        /* step 7 */
        SetFunctionName(closure, propKey, "set");
        /* step 8 */
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setSetter(closure);
        desc.setEnumerable(true);
        desc.setConfigurable(true);
        /* step 9 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionSetter(ScriptObject object, Symbol propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        if (fd.hasSuperReference()) {
            MakeMethod(closure, propKey, object);
        }
        /* step 7 */
        SetFunctionName(closure, propKey, "set");
        /* step 8 */
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setSetter(closure);
        desc.setEnumerable(true);
        desc.setConfigurable(true);
        /* step 9 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.12 Runtime Semantics: InstantiateFunctionObject
     */
    public static OrdinaryGenerator InstantiateGeneratorObject(LexicalEnvironment<?> scope,
            ExecutionContext cx, RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* steps 3-4 */
        OrdinaryGenerator f = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 5 */
        if (fd.hasSuperReference()) {
            MakeMethod(f, name, null);
        }
        /* step 6 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 7 */
        MakeConstructor(cx, f, true, prototype);
        // TODO: missing in spec
        SetFunctionName(f, name);
        /* step 8 */
        return f;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.12 Runtime Semantics: InstantiateFunctionObject
     */
    public static OrdinaryGenerator InstantiateLegacyGeneratorObject(LexicalEnvironment<?> scope,
            ExecutionContext cx, RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* steps 3-4 */
        OrdinaryGenerator f = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 5 */
        if (fd.hasSuperReference()) {
            MakeMethod(f, name, null);
        }
        /* step 6 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.LegacyGeneratorPrototype);
        /* step 7 */
        MakeConstructor(cx, f, true, prototype);
        // TODO: missing in spec
        SetFunctionName(f, name);
        /* step 8 */
        return f;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionGenerator(ScriptObject object, Object propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinitionGenerator(object, (String) propKey, fd, cx);
        } else {
            EvaluatePropertyDefinitionGenerator(object, (Symbol) propKey, fd, cx);
        }
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionGenerator(ScriptObject object, String propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (bytecode) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 (not applicable) */
        /* step 6 */
        OrdinaryGenerator closure = GeneratorFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 7 */
        if (fd.hasSuperReference()) {
            MakeMethod(closure, propKey, object);
        }
        /* step 8 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 9 */
        MakeConstructor(cx, closure, true, prototype);
        /* step 10 */
        SetFunctionName(closure, propKey);
        /* steps 11-12 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, true, true);
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionGenerator(ScriptObject object, Symbol propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (bytecode) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 (not applicable) */
        /* step 6 */
        OrdinaryGenerator closure = GeneratorFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 7 */
        if (fd.hasSuperReference()) {
            MakeMethod(closure, propKey, object);
        }
        /* step 8 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 9 */
        MakeConstructor(cx, closure, true, prototype);
        /* step 10 */
        SetFunctionName(closure, propKey);
        /* steps 11-12 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, true, true);
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     * <ul>
     * <li>GeneratorExpression: function* ( FormalParameters ) { FunctionBody }
     * <li>GeneratorExpression: function* BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     */
    public static OrdinaryGenerator EvaluateGeneratorExpression(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        OrdinaryGenerator closure;
        if (!fd.hasScopedName()) {
            /* steps 1-2 (generated code) */
            /* step 3 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 4 */
            closure = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
            /* step 5 */
            if (fd.hasSuperReference()) {
                MakeMethod(closure, (String) null, null);
            }
            /* step 6 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
            /* step 7 */
            MakeConstructor(cx, closure, true, prototype);
        } else {
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* steps 1-2 (generated code) */
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = LexicalEnvironment
                    .newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name);
            /* step 7 */
            closure = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            if (fd.hasSuperReference()) {
                MakeMethod(closure, name, null);
            }
            /* step 9 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
            /* step 10 */
            MakeConstructor(cx, closure, true, prototype);
            // TODO: missing in spec
            SetFunctionName(closure, name);
            /* step 11 */
            envRec.initialiseBinding(name, closure);
        }
        /* step 8/12 */
        return closure;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     * <ul>
     * <li>GeneratorExpression: function* ( FormalParameters ) { FunctionBody }
     * <li>GeneratorExpression: function* BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     */
    public static OrdinaryGenerator EvaluateLegacyGeneratorExpression(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        OrdinaryGenerator closure;
        if (!fd.hasScopedName()) {
            /* steps 1-2 (generated code) */
            /* step 3 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 4 */
            closure = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
            /* step 5 */
            if (fd.hasSuperReference()) {
                MakeMethod(closure, (String) null, null);
            }
            /* step 6 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.LegacyGeneratorPrototype);
            /* step 7 */
            MakeConstructor(cx, closure, true, prototype);
        } else {
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* steps 1-2 (generated code) */
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = LexicalEnvironment
                    .newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name);
            /* step 7 */
            closure = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            if (fd.hasSuperReference()) {
                MakeMethod(closure, name, null);
            }
            /* step 9 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.LegacyGeneratorPrototype);
            /* step 10 */
            MakeConstructor(cx, closure, true, prototype);
            // TODO: missing in spec
            SetFunctionName(closure, name);
            /* step 11 */
            envRec.initialiseBinding(name, closure);
        }
        /* step 8/12 */
        return closure;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     * <ul>
     * <li>YieldExpression : yield
     * <li>YieldExpression : yield AssignmentExpression
     * </ul>
     */
    public static Object yield(Object value, ExecutionContext cx) {
        return GeneratorYield(cx, CreateIterResultObject(cx, value, false));
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     * <ul>
     * <li>YieldExpression : yield * AssignmentExpression
     * </ul>
     */
    public static Object delegatedYield(Object value, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* steps 4-5 */
        ScriptObject iterator = GetIterator(cx, value);
        /* step 6 */
        boolean normalCompletion = true;
        Object received = UNDEFINED;
        /* step 7 */
        for (;;) {
            ScriptObject innerResult;
            if (normalCompletion) {
                /* step 7a */
                innerResult = IteratorNext(cx, iterator, received);
            } else {
                /* step 7b */
                innerResult = IteratorThrow(cx, iterator, received);
            }
            /* steps 7c-7d */
            boolean done = IteratorComplete(cx, innerResult);
            /* step 7e */
            if (done) {
                return IteratorValue(cx, innerResult);
            }
            /* step 7f */
            try {
                received = GeneratorYield(cx, innerResult);
                normalCompletion = true;
            } catch (ScriptException e) {
                if (HasProperty(cx, iterator, "throw")) {
                    received = e.getValue();
                    normalCompletion = false;
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.15 Runtime Semantics: ClassDefinitionEvaluation
     */
    public static ScriptObject[] getDefaultClassProto(ExecutionContext cx) {
        // step 1
        ScriptObject protoParent = cx.getIntrinsic(Intrinsics.ObjectPrototype);
        ScriptObject constructorParent = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        // step 3
        ScriptObject proto = ObjectCreate(cx, protoParent);
        return new ScriptObject[] { proto, constructorParent };
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.15 Runtime Semantics: ClassDefinitionEvaluation
     */
    public static ScriptObject[] getClassProto(Object superClass, ExecutionContext cx) {
        ScriptObject protoParent;
        ScriptObject constructorParent;
        // step 2
        if (Type.isNull(superClass)) {
            protoParent = null;
            constructorParent = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        } else if (!IsConstructor(superClass)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        } else {
            Object p = Get(cx, Type.objectValue(superClass), "prototype");
            if (!Type.isObjectOrNull(p)) {
                throw newTypeError(cx, Messages.Key.NotObjectOrNull);
            }
            protoParent = Type.objectValueOrNull(p);
            constructorParent = Type.objectValue(superClass);
        }
        // step 3
        ScriptObject proto = ObjectCreate(cx, protoParent);
        return new ScriptObject[] { proto, constructorParent };
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.15 Runtime Semantics: ClassDefinitionEvaluation
     */
    public static RuntimeInfo.Function CreateDefaultConstructor() {
        String functionName = "constructor";
        int functionFlags = RuntimeInfo.FunctionFlags.Strict.getValue()
                | RuntimeInfo.FunctionFlags.Super.getValue()
                | RuntimeInfo.FunctionFlags.TailCall.getValue();
        int expectedArguments = 0;
        RuntimeInfo.Function function = RuntimeInfo.newFunction(functionName, functionFlags,
                expectedArguments, DefaultConstructorSource, DefaultConstructorMH,
                DefaultConstructorCallMH);

        return function;
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.15 Runtime Semantics: ClassDefinitionEvaluation
     */
    public static RuntimeInfo.Function CreateDefaultEmptyConstructor() {
        String functionName = "constructor";
        int functionFlags = RuntimeInfo.FunctionFlags.Strict.getValue();
        int expectedArguments = 0;
        RuntimeInfo.Function function = RuntimeInfo.newFunction(functionName, functionFlags,
                expectedArguments, DefaultEmptyConstructorSource, DefaultEmptyConstructorMH,
                DefaultEmptyConstructorCallMH);

        return function;
    }

    private static final MethodHandle DefaultConstructorMH;
    private static final MethodHandle DefaultConstructorCallMH;
    private static final String DefaultConstructorSource;
    static {
        Lookup lookup = MethodHandles.publicLookup();
        try {
            DefaultConstructorMH = lookup.findStatic(ScriptRuntime.class, "DefaultConstructor",
                    MethodType.methodType(Object.class, ExecutionContext.class));
            DefaultConstructorCallMH = lookup.findStatic(ScriptRuntime.class, "DefaultConstructor",
                    MethodType.methodType(Object.class, OrdinaryFunction.class,
                            ExecutionContext.class, Object.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        try {
            String source = "constructor(...args) { return super(...args); }";
            DefaultConstructorSource = SourceCompressor.compress(source).call();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void DefaultConstructorInit(ExecutionContext cx, FunctionObject f, Object[] args) {
        EnvironmentRecord envRec = cx.getVariableEnvironment().getEnvRec();

        envRec.createMutableBinding("args", false);
        envRec.initialiseBinding("args", UNDEFINED);
        envRec.createImmutableBinding("arguments");

        cx.resolveBinding("args", true).putValue(createRestArray(asList(args).iterator(), cx), cx);

        envRec.initialiseBinding("arguments", CreateStrictArgumentsObject(cx, args));
    }

    public static Object DefaultConstructor(ExecutionContext cx) {
        // super()
        Reference<ScriptObject, ?> ref = MakeSuperReference(cx, (Object) null, true);
        // EvaluateCall: return super(...args)
        Object func = ref.getValue(cx);
        Object[] argList = SpreadArray(cx.resolveBindingValue("args", true), cx);
        Callable f = CheckCallable(func, cx);
        Object thisValue = ref.getThisValue(cx);
        return PrepareForTailCall(argList, thisValue, f);
    }

    public static Object DefaultConstructor(OrdinaryFunction callee,
            ExecutionContext callerContext, Object thisValue, Object[] args) {
        ExecutionContext calleeContext = ExecutionContext.newFunctionExecutionContext(
                callerContext, callee, thisValue);
        DefaultConstructorInit(calleeContext, callee, args);
        return DefaultConstructor(calleeContext);
    }

    private static final MethodHandle DefaultEmptyConstructorMH;
    private static final MethodHandle DefaultEmptyConstructorCallMH;
    private static final String DefaultEmptyConstructorSource;
    static {
        Lookup lookup = MethodHandles.publicLookup();
        try {
            DefaultEmptyConstructorMH = lookup.findStatic(ScriptRuntime.class,
                    "DefaultEmptyConstructor",
                    MethodType.methodType(Object.class, ExecutionContext.class));
            DefaultEmptyConstructorCallMH = lookup.findStatic(ScriptRuntime.class,
                    "DefaultEmptyConstructor", MethodType.methodType(Object.class,
                            OrdinaryFunction.class, ExecutionContext.class, Object.class,
                            Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        try {
            String source = "constructor() { }";
            DefaultEmptyConstructorSource = SourceCompressor.compress(source).call();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void DefaultEmptyConstructorInit(ExecutionContext cx, FunctionObject f,
            Object[] args) {
        EnvironmentRecord envRec = cx.getVariableEnvironment().getEnvRec();

        envRec.createImmutableBinding("arguments");
        envRec.initialiseBinding("arguments", CreateStrictArgumentsObject(cx, args));
    }

    public static Object DefaultEmptyConstructor(ExecutionContext cx) {
        return UNDEFINED;
    }

    public static Object DefaultEmptyConstructor(OrdinaryFunction callee,
            ExecutionContext callerContext, Object thisValue, Object[] args) {
        ExecutionContext calleeContext = ExecutionContext.newFunctionExecutionContext(
                callerContext, callee, thisValue);
        DefaultEmptyConstructorInit(calleeContext, callee, args);
        return DefaultEmptyConstructor(calleeContext);
    }

    /**
     * Extension: Async Function Definitions
     */
    public static OrdinaryAsyncFunction InstantiateAsyncFunctionObject(LexicalEnvironment<?> scope,
            ExecutionContext cx, RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* steps 3-4 */
        OrdinaryAsyncFunction f = AsyncFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 5 */
        if (fd.hasSuperReference()) {
            MakeMethod(f, name, null);
        }
        /* step 6 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.FunctionPrototype);
        /* step 7 */
        MakeConstructor(cx, f, true, prototype);
        // TODO: missing in spec
        SetFunctionName(f, name);
        /* step 8 */
        return f;
    }

    /**
     * Extension: Async Function Definitions
     */
    public static OrdinaryAsyncFunction EvaluateAsyncFunctionExpression(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        OrdinaryAsyncFunction closure;
        if (!fd.hasScopedName()) {
            /* steps 1-2 (generated code) */
            /* step 3 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 4 */
            closure = AsyncFunctionCreate(cx, FunctionKind.Normal, fd, scope);
            /* step 5 */
            if (fd.hasSuperReference()) {
                MakeMethod(closure, (String) null, null);
            }
            /* step 6 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.FunctionPrototype);
            /* step 7 */
            MakeConstructor(cx, closure, true, prototype);
        } else {
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* steps 1-2 (generated code) */
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = LexicalEnvironment
                    .newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name);
            /* step 7 */
            closure = AsyncFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            if (fd.hasSuperReference()) {
                MakeMethod(closure, name, null);
            }
            /* step 9 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.FunctionPrototype);
            /* step 10 */
            MakeConstructor(cx, closure, true, prototype);
            // TODO: missing in spec
            SetFunctionName(closure, name);
            /* step 11 */
            envRec.initialiseBinding(name, closure);
        }
        /* step 8/12 */
        return closure;
    }

    /**
     * Extension: Async Function Definitions
     */
    public static void EvaluatePropertyDefinitionAsync(ScriptObject object, Object propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinitionAsync(object, (String) propKey, fd, cx);
        } else {
            EvaluatePropertyDefinitionAsync(object, (Symbol) propKey, fd, cx);
        }
    }

    /**
     * Extension: Async Function Definitions
     */
    public static void EvaluatePropertyDefinitionAsync(ScriptObject object, String propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (bytecode) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 (not applicable) */
        /* step 6 */
        OrdinaryAsyncFunction closure = AsyncFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 7 */
        if (fd.hasSuperReference()) {
            MakeMethod(closure, propKey, object);
        }
        /* step 8 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.FunctionPrototype);
        /* step 9 */
        MakeConstructor(cx, closure, true, prototype);
        /* step 10 */
        SetFunctionName(closure, propKey);
        /* steps 11-12 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, true, true);
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * Extension: Async Function Definitions
     */
    public static void EvaluatePropertyDefinitionAsync(ScriptObject object, Symbol propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (bytecode) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 (not applicable) */
        /* step 6 */
        OrdinaryAsyncFunction closure = AsyncFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 7 */
        if (fd.hasSuperReference()) {
            MakeMethod(closure, propKey, object);
        }
        /* step 8 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.FunctionPrototype);
        /* step 9 */
        MakeConstructor(cx, closure, true, prototype);
        /* step 10 */
        SetFunctionName(closure, propKey);
        /* steps 11-12 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, true, true);
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.6 Tail Position Calls
     * <p>
     * 14.6.1 Runtime Semantics: PrepareForTailCall
     */
    public static Object PrepareForTailCall(Object[] args, Object thisValue, Callable function) {
        return new TailCallInvocation(function, thisValue, args);
    }

    /* ***************************************************************************************** */

    /**
     * B.3.1 __proto___ Property Names in Object Initialisers
     */
    public static void defineProtoProperty(ScriptObject object, Object value, ExecutionContext cx) {
        // FIXME: function .name and __proto__ interaction unclear
        if (Type.isObjectOrNull(value)) {
            object.setPrototypeOf(cx, Type.objectValueOrNull(value));
        }
    }
}
