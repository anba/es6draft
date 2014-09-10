/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.*;
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromScriptIterator;
import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorYield;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ArgumentsObject.CreateUnmappedArgumentsObject;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction.AsyncFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeMethod;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.SetFunctionName;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.GeneratorFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;
import static java.util.Arrays.asList;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.mozilla.javascript.ConsString;

import com.github.anba.es6draft.compiler.CompiledScript;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.*;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Runtime support methods
 */
public final class ScriptRuntime {
    public static final Object[] EMPTY_ARRAY = new Object[0];

    private ScriptRuntime() {
    }

    /**
     * 18.2.1.2 EvalDeclarationInstantiation
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     */
    public static void bindingNotPresentOrThrow(ExecutionContext cx, EnvironmentRecord envRec,
            String name) {
        if (envRec.hasBinding(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
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
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
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
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param fn
     *            the function name
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
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param vn
     *            the variable name
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
     * 12.2.4.1 Array Literal
     * <p>
     * 12.2.4.1.2 Runtime Semantics: Array Accumulation
     * <ul>
     * <li>ElementList : Elision<span><sub>opt</sub></span> AssignmentExpression
     * <li>ElementList : ElementList , Elision<span><sub>opt</sub></span> AssignmentExpression
     * </ul>
     * 
     * @param array
     *            the array object
     * @param nextIndex
     *            the array index
     * @param value
     *            the array element value
     * @param cx
     *            the execution context
     */
    public static void defineProperty(ArrayObject array, int nextIndex, Object value,
            ExecutionContext cx) {
        // String propertyName = ToString(ToUint32(nextIndex));
        int propertyName = nextIndex;
        boolean created = array.defineOwnProperty(cx, propertyName, new PropertyDescriptor(value,
                true, true, true));
        assert created;
    }

    /**
     * 12.2.4.1 Array Literal
     * <p>
     * 12.2.4.1.2 Runtime Semantics: Array Accumulation
     * <ul>
     * <li>SpreadElement : ... AssignmentExpression
     * </ul>
     * 
     * @param array
     *            the array object
     * @param nextIndex
     *            the array index
     * @param spreadObj
     *            the spread element
     * @param cx
     *            the execution context
     * @return the next array index
     */
    public static int ArrayAccumulationSpreadElement(ArrayObject array, int nextIndex,
            Object spreadObj, ExecutionContext cx) {
        /* steps 1-2 (cf. generated code) */
        /* steps 2-4 */
        ScriptObject iterator = GetIterator(cx, ToObject(cx, spreadObj));
        /* step 5 */
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
     * 12.2.5 Object Initializer
     * <p>
     * 12.2.5.8 Runtime Semantics: PropertyDefinitionEvaluation
     * 
     * @param object
     *            the script object
     * @param propertyName
     *            the property name
     * @param value
     *            the property value
     * @param cx
     *            the execution context
     */
    public static void defineProperty(OrdinaryObject object, Object propertyName, Object value,
            ExecutionContext cx) {
        DefinePropertyOrThrow(cx, object, propertyName, new PropertyDescriptor(value, true, true,
                true));
    }

    /**
     * 12.2.5 Object Initializer
     * <p>
     * 12.2.5.8 Runtime Semantics: PropertyDefinitionEvaluation
     * 
     * @param object
     *            the script object
     * @param propertyName
     *            the property name
     * @param value
     *            the property value
     * @param cx
     *            the execution context
     */
    public static void defineProperty(OrdinaryObject object, String propertyName, Object value,
            ExecutionContext cx) {
        DefinePropertyOrThrow(cx, object, propertyName, new PropertyDescriptor(value, true, true,
                true));
    }

    /**
     * 12.2.5 Object Initializer
     * <p>
     * 12.2.5.8 Runtime Semantics: PropertyDefinitionEvaluation
     * 
     * @param object
     *            the new home object
     * @param propertyName
     *            the property name
     * @param f
     *            the function object
     */
    public static void updateMethod(OrdinaryObject object, Object propertyName, FunctionObject f) {
        f.updateMethod(propertyName, object);
    }

    /**
     * 12.2.? Generator Comprehensions
     * <p>
     * 12.2.?.2 Runtime Semantics: Evaluation
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the generator object
     */
    public static GeneratorObject EvaluateGeneratorComprehension(RuntimeInfo.Function fd,
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
     * 12.2.? Generator Comprehensions
     * <p>
     * Runtime Semantics: Evaluation
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the generator object
     */
    public static GeneratorObject EvaluateLegacyGeneratorComprehension(RuntimeInfo.Function fd,
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
     * 12.2.8 Template Literals
     * <p>
     * 12.2.8.2.2 Runtime Semantics: GetTemplateCallSite
     * 
     * @param key
     *            the template literal key
     * @param handle
     *            the method handle for the template literal data
     * @param cx
     *            the execution context
     * @return the template call site object
     */
    public static ArrayObject GetTemplateCallSite(int key, MethodHandle handle, ExecutionContext cx) {
        assert cx.getCurrentScript() instanceof CompiledScript : cx.getCurrentScript();
        CompiledScript script = (CompiledScript) cx.getCurrentScript();
        /* step 1 */
        ArrayObject callSite = script.getTemplateCallSite(key);
        if (callSite != null) {
            return callSite;
        }
        /* steps 2-3 */
        String[] strings = evaluateCallSite(handle);
        assert (strings.length & 1) == 0;
        /* step 4 */
        int count = strings.length >>> 1;
        /* steps 5-6 */
        ArrayObject siteObj = ArrayCreate(cx, count);
        ArrayObject rawObj = ArrayCreate(cx, count);
        /* steps 7-8 */
        for (int i = 0, n = strings.length; i < n; i += 2) {
            int index = i >>> 1;
            int prop = index;
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
        script.addTemplateCallSite(key, siteObj);
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
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property reference
     */
    public static Reference<Object, String> getProperty(Object baseValue, int propertyNameString,
            ExecutionContext cx, boolean strict) {
        return getProperty(baseValue, (long) propertyNameString, cx, strict);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property reference
     */
    public static Reference<Object, String> getProperty(Object baseValue, long propertyNameString,
            ExecutionContext cx, boolean strict) {
        /* steps 1-6 (generated code) */
        /* step 7 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 8-10 */
        return new Reference.PropertyIndexReference(baseValue, propertyNameString, strict);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property reference
     */
    public static Reference<Object, String> getProperty(Object baseValue,
            double propertyNameString, ExecutionContext cx, boolean strict) {
        long index = (long) propertyNameString;
        if (index == propertyNameString) {
            return getProperty(baseValue, index, cx, strict);
        }
        return getProperty(baseValue, ToString(propertyNameString), cx, strict);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property reference
     */
    public static Reference<Object, String> getProperty(Object baseValue,
            String propertyNameString, ExecutionContext cx, boolean strict) {
        /* steps 1-6 (generated code) */
        /* step 7 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 8-10 */
        return new Reference.PropertyNameReference(baseValue, propertyNameString, strict);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property value
     */
    public static Object getPropertyValue(Object baseValue, int propertyNameString,
            ExecutionContext cx, boolean strict) {
        return getPropertyValue(baseValue, (long) propertyNameString, cx, strict);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property value
     */
    public static Object getPropertyValue(Object baseValue, long propertyNameString,
            ExecutionContext cx, boolean strict) {
        /* steps 1-6 (generated code) */
        /* step 7 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 8-10 */
        Reference<Object, String> ref = new Reference.PropertyIndexReference(baseValue,
                propertyNameString, strict);
        return ref.getValue(cx);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property value
     */
    public static Object getPropertyValue(Object baseValue, double propertyNameString,
            ExecutionContext cx, boolean strict) {
        long index = (long) propertyNameString;
        if (index == propertyNameString) {
            return getPropertyValue(baseValue, index, cx, strict);
        }
        return getPropertyValue(baseValue, ToString(propertyNameString), cx, strict);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property value
     */
    public static Object getPropertyValue(Object baseValue, String propertyNameString,
            ExecutionContext cx, boolean strict) {
        /* steps 1-6 (generated code) */
        /* step 7 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 8-10 */
        Reference<Object, String> ref = new Reference.PropertyNameReference(baseValue,
                propertyNameString, strict);
        return ref.getValue(cx);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression [ Expression ]
     * <li>CallExpression : CallExpression [ Expression ]
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameValue
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property reference
     */
    public static Reference<Object, ?> getElement(Object baseValue, Object propertyNameValue,
            ExecutionContext cx, boolean strict) {
        /* steps 1-6 (generated code) */
        /* step 7 */
        RequireObjectCoercible(cx, baseValue);
        /* step 8 */
        Object propertyKey = ToPropertyKey(cx, propertyNameValue);
        /* steps 9-10 */
        if (propertyKey instanceof String) {
            return new Reference.PropertyNameReference(baseValue, (String) propertyKey, strict);
        }
        return new Reference.PropertySymbolReference(baseValue, (Symbol) propertyKey, strict);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression [ Expression ]
     * <li>CallExpression : CallExpression [ Expression ]
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameValue
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property value
     */
    public static Object getElementValue(Object baseValue, Object propertyNameValue,
            ExecutionContext cx, boolean strict) {
        /* steps 1-6 (generated code) */
        /* step 7 */
        RequireObjectCoercible(cx, baseValue);
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
     * 12.3.3 The new Operator
     * <p>
     * 12.3.3.1 Runtime Semantics: Evaluation<br>
     * 12.3.5.2 Runtime Semantics: Evaluation
     * <ul>
     * <li>NewExpression : new NewExpression
     * <li>MemberExpression : new MemberExpression Arguments
     * <li>MemberExpression : new super Arguments<span><sub>opt</sub></span>
     * </ul>
     * 
     * @param constructor
     *            the constructor object
     * @param args
     *            the arguments for the new-call
     * @param cx
     *            the execution context
     * @return the constructor call return value
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
     * 12.3.3 The new Operator
     * <p>
     * 12.3.3.1 Runtime Semantics: Evaluation<br>
     * 12.3.5.2 Runtime Semantics: Evaluation
     * <ul>
     * <li>NewExpression : new NewExpression
     * <li>MemberExpression : new MemberExpression Arguments
     * <li>MemberExpression : new super Arguments<span><sub>opt</sub></span>
     * </ul>
     * 
     * @param constructor
     *            the constructor object
     * @param args
     *            the arguments for the new-call
     * @param cx
     *            the execution context
     * @return the tail call trampoline object
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
     * 12.3.4 Function Calls
     * <p>
     * Runtime Semantics: EvaluateCall Abstract Operation
     * 
     * @param func
     *            the function object
     * @param cx
     *            the execution context
     * @return the function object
     * @throws ScriptException
     *             if <var>func</var> is not a function
     */
    public static Callable CheckCallable(Object func, ExecutionContext cx) throws ScriptException {
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
     * 12.3.4 Function Calls
     * <p>
     * Runtime Semantics: EvaluateCall Abstract Operation
     * 
     * @param ref
     *            the reference value
     * @param f
     *            the function object
     * @param cx
     *            the execution context
     * @return {@code true} if <var>f</var> is the built-in eval function
     */
    public static boolean IsBuiltinEval(Object ref, Callable f, ExecutionContext cx) {
        if (ref instanceof Reference) {
            Reference<?, ?> r = (Reference<?, ?>) ref;
            if (!r.isPropertyReference()) {
                assert !r.isUnresolvableReference() && r.getBase() instanceof EnvironmentRecord;
                return f == cx.getRealm().getBuiltinEval();
            }
        }
        return false;
    }

    /**
     * 12.3.4 Function Calls
     * <p>
     * Runtime Semantics: EvaluateCall Abstract Operation
     * 
     * @param f
     *            the function object
     * @param cx
     *            the execution context
     * @return {@code true} if <var>f</var> is the built-in eval function
     */
    public static boolean IsBuiltinEval(Callable f, ExecutionContext cx) {
        return f == cx.getRealm().getBuiltinEval();
    }

    /**
     * 12.3.4 Function Calls
     * <p>
     * Runtime Semantics: EvaluateCall Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @return the direct eval fallback hook
     */
    public static Callable directEvalFallbackHook(ExecutionContext cx) {
        return cx.getRealm().getNonEvalFallback();
    }

    /**
     * 12.3.4 Function Calls
     * <p>
     * Runtime Semantics: EvaluateCall Abstract Operation
     * 
     * @param args
     *            the function call arguments
     * @param thisValue
     *            the function this-value
     * @param callee
     *            the function callee
     * @param cx
     *            the execution context
     * @return the direct eval fallback arguments
     */
    public static Object[] directEvalFallbackArguments(Object[] args, Object thisValue,
            Callable callee, ExecutionContext cx) {
        Object[] fallbackArgs = new Object[3];
        fallbackArgs[0] = callee;
        fallbackArgs[1] = thisValue;
        fallbackArgs[2] = CreateArrayFromList(cx, Arrays.asList(args));
        return fallbackArgs;
    }

    /**
     * 12.3.4 Function Calls
     * <p>
     * Runtime Semantics: EvaluateCall Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @return the direct eval fallback this-argument
     */
    public static Object directEvalFallbackThisArgument(ExecutionContext cx) {
        return cx.getRealm().getRealmObject();
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * Runtime Semantics: Abstract Operation MakeSuperReference(propertyKey, strict)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param strict
     *            the strict mode flag
     * @return the super reference
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
        // RequireObjectCoercible(cx.getRealm(), baseValue);
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
     * 12.3.5 The super Keyword
     * <p>
     * Runtime Semantics: Abstract Operation MakeSuperReference(propertyKey, strict)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param strict
     *            the strict mode flag
     * @return the super reference
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
        // RequireObjectCoercible(cx.getRealm(), baseValue);
        if (baseValue == null) {
            throw newTypeError(cx, Messages.Key.UndefinedOrNull);
        }
        return new Reference.SuperNameReference(baseValue, propertyKey, strict, actualThis);
    }

    /**
     * 12.3.6 Argument Lists
     * <p>
     * 12.3.6.1 Runtime Semantics: ArgumentListEvaluation
     * 
     * @param spreadObj
     *            the spread object
     * @param cx
     *            the execution context
     * @return the spread object elements
     */
    public static Object[] SpreadArray(Object spreadObj, ExecutionContext cx) {
        final int MAX_ARGS = FunctionPrototype.getMaxArguments();
        /* steps 1-3 (cf. generated code) */
        /* steps 3-4 */
        ScriptObject iterator = GetIterator(cx, ToObject(cx, spreadObj));
        /* step 5 */
        ArrayList<Object> list = new ArrayList<>();
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
     * 12.3.6 Argument Lists
     * <p>
     * 12.3.6.1 Runtime Semantics: ArgumentListEvaluation
     * 
     * @param array
     *            the array
     * @param cx
     *            the execution context
     * @return the flattened array
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
     * 12.5 Unary Operators<br>
     * 12.5.4 The delete Operator
     * 
     * @param ref
     *            the reference instance
     * @param cx
     *            the execution context
     * @return {@code true} on success
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
     * 12.5 Unary Operators<br>
     * 12.5.4 The delete Operator
     * 
     * @param ref
     *            the reference instance
     * @param cx
     *            the execution context
     * @return {@code true} on success
     */
    public static boolean deleteBinding(Reference<?, ?> ref, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 5 (not applicable) */
        assert !ref.isPropertyReference();
        /* step 4 */
        if (ref.isUnresolvableReference()) {
            assert !ref.isStrictReference();
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
     * 12.5 Unary Operators<br>
     * 12.5.4 The delete Operator
     * 
     * @param ref
     *            the reference instance
     * @param cx
     *            the execution context
     * @return {@code true} on success
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
     * 12.5 Unary Operators<br>
     * 12.5.6 The typeof Operator
     * 
     * @param val
     *            the value
     * @param cx
     *            the execution context
     * @return the typeof descriptor string
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
     * 12.7 Additive Operators<br>
     * 12.7.3 The Addition operator ( + )
     * 
     * @param lval
     *            the left-hand side operand
     * @param rval
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the operation result
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
     * 12.7 Additive Operators<br>
     * 12.7.3 The Addition operator ( + )
     * 
     * @param lstr
     *            the left-hand side operand
     * @param rstr
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the concatenated string
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
     * 12.9 Relational Operators<br>
     * 12.9.3 Runtime Semantics: Evaluation
     * 
     * @param x
     *            the left-hand side operand
     * @param y
     *            the right-hand side operand
     * @param leftFirst
     *            the operation order flag
     * @param cx
     *            the execution context
     * @return the comparison result
     */
    public static int relationalComparison(Object x, Object y, boolean leftFirst,
            ExecutionContext cx) {
        return RelationalComparison(cx, x, y, leftFirst);
    }

    /**
     * 12.9 Relational Operators<br>
     * 12.9.3 Runtime Semantics: Evaluation
     * 
     * @param lval
     *            the left-hand side operand
     * @param rval
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the operation result
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
     * 12.9 Relational Operators<br>
     * 12.9.4 Runtime Semantics: InstanceofOperator(O, C)
     * 
     * @param obj
     *            the object
     * @param constructor
     *            the constructor function
     * @param cx
     *            the execution context
     * @return the operation result
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
     * 12.10 Equality Operators<br>
     * 12.10.3 Runtime Semantics: Evaluation
     * 
     * @param x
     *            the left-hand side operand
     * @param y
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the operation result
     */
    public static boolean equalityComparison(Object x, Object y, ExecutionContext cx) {
        return EqualityComparison(cx, x, y);
    }

    /**
     * 12.10 Equality Operators<br>
     * 12.10.3 Runtime Semantics: Evaluation
     * 
     * @param x
     *            the left-hand side operand
     * @param y
     *            the right-hand side operand
     * @return the operation result
     */
    public static boolean strictEqualityComparison(Object x, Object y) {
        return StrictEqualityComparison(x, y);
    }

    /**
     * 12.14.2 Runtime Semantics<br>
     * Runtime Semantics: Evaluation
     * <p>
     * 13.2.1.2 Runtime Semantics<br>
     * Runtime Semantics: Evaluation
     * <p>
     * 13.2.2.2 Runtime Semantics<br>
     * Runtime Semantics: Evaluation
     * 
     * @param val
     *            the value
     * @param cx
     *            the execution context
     * @return <var>val</var> if it is a script object
     * @throws ScriptException
     *             if <var>val</var> is not a script object
     */
    public static ScriptObject ensureObject(Object val, ExecutionContext cx) throws ScriptException {
        if (!Type.isObject(val)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        return Type.objectValue(val);
    }

    /**
     * 12.14.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialization
     * 
     * @param iterator
     *            the iterator
     * @param cx
     *            the execution context
     * @return the array with the remaining elements from <var>iterator</var>
     */
    public static ArrayObject createRestArray(Iterator<?> iterator, ExecutionContext cx) {
        ArrayObject result = ArrayCreate(cx, 0);
        for (int n = 0; iterator.hasNext(); ++n) {
            Object nextValue = iterator.next();
            CreateDataPropertyOrThrow(cx, result, n, nextValue);
        }
        return result;
    }

    /**
     * 12.14.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialization
     * 
     * @param obj
     *            the script object
     * @param cx
     *            the execution context
     * @return the object iterator
     */
    public static Iterator<?> getIterator(ScriptObject obj, ExecutionContext cx) {
        return FromScriptIterator(cx, GetIterator(cx, obj));
    }

    /**
     * 12.14.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialization
     * 
     * @param iterator
     *            the iterator
     */
    public static void iteratorNextAndIgnore(Iterator<?> iterator) {
        if (iterator.hasNext()) {
            iterator.next();
        }
    }

    /**
     * 12.14.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialization
     * 
     * @param iterator
     *            the iterator
     * @return the next iterator result, or undefined it is already exhausted
     */
    public static Object iteratorNextOrUndefined(Iterator<?> iterator) {
        return iterator.hasNext() ? iterator.next() : UNDEFINED;
    }

    /* ***************************************************************************************** */

    /**
     * 13.6.4 The for-in and for-of Statements
     * <p>
     * 13.6.4.7 Runtime Semantics: ForIn/OfExpressionEvaluation Abstract Operation
     * 
     * @param o
     *            the object to enumerate
     * @param cx
     *            the execution context
     * @return the keys enumerator
     */
    public static ScriptIterator<?> enumerate(Object o, ExecutionContext cx) {
        /* step 8 */
        ScriptObject obj = ToObject(cx, o);
        /* step 9-11 */
        ScriptIterator<?> keys = obj.enumerateKeys(cx);
        /* step 12 */
        return keys;
    }

    /**
     * 13.6.4 The for-in and for-of Statements
     * <p>
     * 13.6.4.7 Runtime Semantics: ForIn/OfExpressionEvaluation Abstract Operation
     * 
     * @param o
     *            the object to enumerate
     * @param cx
     *            the execution context
     * @return the object iterator
     */
    public static ScriptIterator<?> iterate(Object o, ExecutionContext cx) {
        /* step 8 */
        ScriptObject obj = ToObject(cx, o);
        /* step 9-11 */
        ScriptObject keys = GetIterator(cx, obj);
        /* step 12 */
        return FromScriptIterator(cx, keys);
    }

    /**
     * 13.6.4 The for-in and for-of Statements<br>
     * Extension: 'for-each' statement
     * <p>
     * 13.6.4.7 Runtime Semantics: ForIn/OfExpressionEvaluation Abstract Operation
     * 
     * @param o
     *            the object to enumerate
     * @param cx
     *            the execution context
     * @return the values enumerator
     */
    public static ScriptIterator<?> enumerateValues(Object o, ExecutionContext cx) {
        /* step 8 */
        ScriptObject obj = ToObject(cx, o);
        /* step 9-11 */
        ScriptIterator<?> keys = new ValuesIterator(cx, obj, obj.enumerateKeys(cx));
        /* step 12 */
        return keys;
    }

    private static final class ValuesIterator extends SimpleIterator<Object> implements
            ScriptIterator<Object> {
        private final ExecutionContext cx;
        private final ScriptObject object;
        private final ScriptIterator<?> keysIterator;

        ValuesIterator(ExecutionContext cx, ScriptObject object, ScriptIterator<?> keysIterator) {
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

        @Override
        public ScriptObject getScriptObject() {
            return keysIterator.getScriptObject();
        }
    }

    /**
     * 13.14 The try Statement
     * 
     * @param e
     *            the error cause
     * @return if either <var>e</var> or its cause is a stack overflow error, that error object
     * @throws Error
     *             if neither the error nor its cause is a stack overflow error
     */
    public static StackOverflowError getStackOverflowError(Error e) throws Error {
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
     * 
     * @param e
     *            the error cause
     * @param cx
     *            the execution context
     * @return the new script exception
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
     * 14.1.21 Runtime Semantics: InstantiateFunctionObject
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new function instance
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
        /* steps 6-7 */
        SetFunctionName(f, name);
        /* step 8 */
        return f;
    }

    /**
     * 14.1 Function Definitions
     * <p>
     * 14.1.22 Runtime Semantics: Evaluation
     * <ul>
     * <li>FunctionExpression : function ( FormalParameters ) { FunctionBody }
     * <li>FunctionExpression : function BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new function instance
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
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> runningContext = cx.getLexicalEnvironment();
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = LexicalEnvironment
                    .newDeclarativeEnvironment(runningContext);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name);
            /* step 7 */
            closure = FunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            if (fd.hasSuperReference()) {
                MakeMethod(closure, name, null);
            }
            /* step 9 */
            MakeConstructor(cx, closure);
            /* steps 10-11 */
            SetFunctionName(closure, name);
            /* step 12 */
            envRec.initializeBinding(name, closure);
        }
        /* step 6/13 */
        return closure;
    }

    /**
     * 14.2 Arrow Function Definitions
     * <p>
     * 14.2.18 Runtime Semantics: Evaluation
     * <ul>
     * <li>ArrowFunction : ArrowParameters {@literal =>} ConciseBody
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new function instance
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
     * 14.5.17 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param constructorParent
     *            the constructor prototype
     * @param proto
     *            the class prototype
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new function instance
     */
    public static OrdinaryFunction EvaluateConstructorMethod(ScriptObject constructorParent,
            OrdinaryObject proto, RuntimeInfo.Function fd, ExecutionContext cx) {
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
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinition(OrdinaryObject object, Object propKey,
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
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinition(OrdinaryObject object, String propKey,
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
        /* steps 3-4 */
        SetFunctionName(closure, propKey);
        /* step 4 */
        CreateDataPropertyOrThrow(cx, object, propKey, closure);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinition(OrdinaryObject object, Symbol propKey,
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
        /* steps 3-4 */
        SetFunctionName(closure, propKey);
        /* step 4 */
        CreateDataPropertyOrThrow(cx, object, propKey, closure);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>get PropertyName ( ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionGetter(OrdinaryObject object, Object propKey,
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
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>get PropertyName ( ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionGetter(OrdinaryObject object, String propKey,
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
        /* steps 8-9 */
        SetFunctionName(closure, propKey, "get");
        /* step 10 */
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setGetter(closure);
        desc.setEnumerable(true);
        desc.setConfigurable(true);
        /* step 11 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>get PropertyName ( ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionGetter(OrdinaryObject object, Symbol propKey,
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
        /* steps 8-9 */
        SetFunctionName(closure, propKey, "get");
        /* step 10 */
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setGetter(closure);
        desc.setEnumerable(true);
        desc.setConfigurable(true);
        /* step 11 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionSetter(OrdinaryObject object, Object propKey,
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
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionSetter(OrdinaryObject object, String propKey,
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
        /* steps 7-8 */
        SetFunctionName(closure, propKey, "set");
        /* step 9 */
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setSetter(closure);
        desc.setEnumerable(true);
        desc.setConfigurable(true);
        /* step 10 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionSetter(OrdinaryObject object, Symbol propKey,
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
        /* steps 7-8 */
        SetFunctionName(closure, propKey, "set");
        /* step 9 */
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setSetter(closure);
        desc.setEnumerable(true);
        desc.setConfigurable(true);
        /* step 10 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: InstantiateFunctionObject
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new generator function instance
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
        /* steps 8-9 */
        SetFunctionName(f, name);
        /* step 10 */
        return f;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: InstantiateFunctionObject
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new generator function instance
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
        /* steps 8-9 */
        SetFunctionName(f, name);
        /* step 10 */
        return f;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.15 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionGenerator(OrdinaryObject object, Object propKey,
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
     * 14.4.15 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionGenerator(OrdinaryObject object, String propKey,
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
        /* steps 10-11 */
        SetFunctionName(closure, propKey);
        /* step 12 */
        CreateDataPropertyOrThrow(cx, object, propKey, closure);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.15 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionGenerator(OrdinaryObject object, Symbol propKey,
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
        /* steps 10-11 */
        SetFunctionName(closure, propKey);
        /* step 12 */
        CreateDataPropertyOrThrow(cx, object, propKey, closure);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.16 Runtime Semantics: Evaluation
     * <ul>
     * <li>GeneratorExpression: function* ( FormalParameters ) { FunctionBody }
     * <li>GeneratorExpression: function* BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new generator function instance
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
            /* steps 1-2 (generated code) */
            /* step 3 */
            LexicalEnvironment<?> runningContext = cx.getLexicalEnvironment();
            /* step 4 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = LexicalEnvironment
                    .newDeclarativeEnvironment(runningContext);
            /* step 5 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 6 */
            String name = fd.functionName();
            /* step 7 */
            envRec.createImmutableBinding(name);
            /* step 8 */
            closure = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 9 */
            if (fd.hasSuperReference()) {
                MakeMethod(closure, name, null);
            }
            /* step 10 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
            /* step 11 */
            MakeConstructor(cx, closure, true, prototype);
            /* steps 12-13 */
            SetFunctionName(closure, name);
            /* step 14 */
            envRec.initializeBinding(name, closure);
        }
        /* step 8/15 */
        return closure;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.16 Runtime Semantics: Evaluation
     * <ul>
     * <li>GeneratorExpression: function* ( FormalParameters ) { FunctionBody }
     * <li>GeneratorExpression: function* BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new generator function instance
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
            /* steps 1-2 (generated code) */
            /* step 3 */
            LexicalEnvironment<?> runningContext = cx.getLexicalEnvironment();
            /* step 4 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = LexicalEnvironment
                    .newDeclarativeEnvironment(runningContext);
            /* step 5 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 6 */
            String name = fd.functionName();
            /* step 7 */
            envRec.createImmutableBinding(name);
            /* step 8 */
            closure = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 9 */
            if (fd.hasSuperReference()) {
                MakeMethod(closure, name, null);
            }
            /* step 10 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.LegacyGeneratorPrototype);
            /* step 11 */
            MakeConstructor(cx, closure, true, prototype);
            /* step 12 */
            SetFunctionName(closure, name);
            /* step 13 */
            envRec.initializeBinding(name, closure);
        }
        /* step 8/14 */
        return closure;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.16 Runtime Semantics: Evaluation
     * <ul>
     * <li>YieldExpression : yield
     * <li>YieldExpression : yield AssignmentExpression
     * </ul>
     * 
     * @param value
     *            the value to yield
     * @param cx
     *            the execution context
     * @return the result value
     * @throws ReturnValue
     *             to signal an abrupt Return completion
     */
    public static Object yield(Object value, ExecutionContext cx) throws ReturnValue {
        return GeneratorYield(cx, CreateIterResultObject(cx, value, false));
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.16 Runtime Semantics: Evaluation
     * <ul>
     * <li>YieldExpression : yield * AssignmentExpression
     * </ul>
     * 
     * @param value
     *            the iterator value
     * @param cx
     *            the execution context
     * @return the iterator object
     */
    public static ScriptObject getIteratorObject(Object value, ExecutionContext cx) {
        return GetIterator(cx, ToObject(cx, value));
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.16 Runtime Semantics: Evaluation
     * <ul>
     * <li>YieldExpression : yield * AssignmentExpression
     * </ul>
     * 
     * @param value
     *            the value to yield
     * @param cx
     *            the execution context
     * @return the result value
     * @throws ReturnValue
     *             to signal an abrupt Return completion
     */
    public static Object delegatedYield(Object value, ExecutionContext cx) throws ReturnValue {
        /* steps 1-2 (generated code) */
        /* steps 3-4 */
        ScriptObject iterator = GetIterator(cx, ToObject(cx, value));
        /* step 5 */
        Object received = UNDEFINED;
        /* step 6 */
        for (;;) {
            /* step 6a (empty) */
            /* step 6b */
            ScriptObject innerResult = IteratorNext(cx, iterator, received);
            /* steps 6f-6g */
            boolean done = IteratorComplete(cx, innerResult);
            /* step 6h */
            if (done) {
                return IteratorValue(cx, innerResult);
            }
            try {
                /* step 6i */
                received = GeneratorYield(cx, innerResult);
            } catch (ScriptException e) {
                // FIXME: spec issue - change to GetMethod to avoid Has+Get?
                /* step 6c */
                if (HasProperty(cx, iterator, "throw")) {
                    IteratorThrow(cx, iterator, e.getValue());
                }
                throw e;
            } catch (ReturnValue e) {
                // FIXME: spec issue - change to GetMethod to avoid Has+Get?
                /* step 6d */
                if (HasProperty(cx, iterator, "return")) {
                    Object innerReturnValue = IteratorReturn(cx, iterator, e.getValue());
                    throw new ReturnValue(innerReturnValue);
                }
                throw e;
            }
        }
    }

    public static CallSite runtimeBootstrap(MethodHandles.Lookup caller, String name,
            MethodType type) {
        MethodHandle mh = MethodHandles.identity(Object[].class);
        mh = mh.asCollector(Object[].class, type.parameterCount());
        if ("rt:stack".equals(name) && type.parameterCount() > 1) {
            // TODO: remove permute
            int pc = type.parameterCount();
            int[] reorder = new int[pc];
            for (int i = 0; i < pc; ++i) {
                reorder[i] = pc - 1 - i;
            }
            mh = MethodHandles.permuteArguments(mh, mh.type(), reorder);
        }
        mh = mh.asType(type);
        return new ConstantCallSite(mh);
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.17 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param cx
     *            the execution context
     * @return the tuple (prototype, constructorParent)
     */
    public static ScriptObject[] getDefaultClassProto(ExecutionContext cx) {
        // step 1
        ScriptObject protoParent = cx.getIntrinsic(Intrinsics.ObjectPrototype);
        ScriptObject constructorParent = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        // step 3
        OrdinaryObject proto = ObjectCreate(cx, protoParent);
        return new ScriptObject[] { proto, constructorParent };
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.17 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param superClass
     *            the super class object
     * @param cx
     *            the execution context
     * @return the tuple (prototype, constructorParent)
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
        OrdinaryObject proto = ObjectCreate(cx, protoParent);
        return new ScriptObject[] { proto, constructorParent };
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.17 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @return the runtime info object for the default constructor
     */
    public static RuntimeInfo.Function CreateDefaultConstructor() {
        String functionName = "constructor";
        int functionFlags = RuntimeInfo.FunctionFlags.Strict.getValue()
                | RuntimeInfo.FunctionFlags.ImplicitStrict.getValue()
                | RuntimeInfo.FunctionFlags.Method.getValue()
                | RuntimeInfo.FunctionFlags.Super.getValue();
        int expectedArguments = 0;
        RuntimeInfo.Function function = RuntimeInfo.newFunction(functionName, functionFlags,
                expectedArguments, DefaultConstructorSource, DefaultConstructorSourceBody,
                DefaultConstructorMH, DefaultConstructorCallMH);

        return function;
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.17 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @return the runtime info object for the default constructor
     */
    public static RuntimeInfo.Function CreateDefaultEmptyConstructor() {
        String functionName = "constructor";
        int functionFlags = RuntimeInfo.FunctionFlags.Strict.getValue()
                | RuntimeInfo.FunctionFlags.ImplicitStrict.getValue()
                | RuntimeInfo.FunctionFlags.Method.getValue();
        int expectedArguments = 0;
        RuntimeInfo.Function function = RuntimeInfo.newFunction(functionName, functionFlags,
                expectedArguments, DefaultEmptyConstructorSource,
                DefaultEmptyConstructorSourceBody, DefaultEmptyConstructorMH,
                DefaultEmptyConstructorCallMH);

        return function;
    }

    private static final MethodHandle DefaultConstructorMH, DefaultConstructorCallMH;
    private static final String DefaultConstructorSource;
    private static final int DefaultConstructorSourceBody;
    private static final boolean DefaultConstructorArguments = false;
    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.publicLookup());
        DefaultConstructorMH = lookup.findStatic(ScriptRuntime.class, "DefaultConstructor",
                MethodType.methodType(Object.class, ExecutionContext.class));
        DefaultConstructorCallMH = lookup.findStatic(ScriptRuntime.class, "DefaultConstructor",
                MethodType.methodType(Object.class, OrdinaryFunction.class, ExecutionContext.class,
                        Object.class, Object[].class));
        String parameters = "(...args)", body = "super(...args);", source = parameters + body;
        DefaultConstructorSource = SourceCompressor.compressToString(source);
        DefaultConstructorSourceBody = parameters.length();
    }

    private static void DefaultConstructorInit(ExecutionContext cx, FunctionObject f, Object[] args) {
        EnvironmentRecord envRec = cx.getVariableEnvironment().getEnvRec();

        envRec.createMutableBinding("args", false);
        envRec.initializeBinding("args", UNDEFINED);
        if (DefaultConstructorArguments) {
            envRec.createImmutableBinding("arguments");
        }

        cx.resolveBinding("args", true).putValue(createRestArray(asList(args).iterator(), cx), cx);

        if (DefaultConstructorArguments) {
            envRec.initializeBinding("arguments", CreateUnmappedArgumentsObject(cx, args));
        }
    }

    public static Object DefaultConstructor(ExecutionContext cx) {
        // super()
        Reference<ScriptObject, ?> ref = MakeSuperReference(cx, (Object) null, true);
        // EvaluateCall: super(...args)
        Object func = ref.getValue(cx);
        Object[] argList = SpreadArray(cx.resolveBindingValue("args", true), cx);
        Callable f = CheckCallable(func, cx);
        Object thisValue = ref.getThisValue(cx);
        f.call(cx, thisValue, argList);
        return UNDEFINED;
    }

    public static Object DefaultConstructor(OrdinaryFunction callee,
            ExecutionContext callerContext, Object thisValue, Object[] args) {
        ExecutionContext calleeContext = ExecutionContext.newFunctionExecutionContext(
                callerContext, callee, thisValue);
        DefaultConstructorInit(calleeContext, callee, args);
        return DefaultConstructor(calleeContext);
    }

    private static final MethodHandle DefaultEmptyConstructorMH, DefaultEmptyConstructorCallMH;
    private static final String DefaultEmptyConstructorSource;
    private static final int DefaultEmptyConstructorSourceBody;
    private static final boolean DefaultEmptyConstructorArguments = false;
    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.publicLookup());
        DefaultEmptyConstructorMH = lookup.findStatic(ScriptRuntime.class,
                "DefaultEmptyConstructor",
                MethodType.methodType(Object.class, ExecutionContext.class));
        DefaultEmptyConstructorCallMH = lookup.findStatic(ScriptRuntime.class,
                "DefaultEmptyConstructor", MethodType.methodType(Object.class,
                        OrdinaryFunction.class, ExecutionContext.class, Object.class,
                        Object[].class));
        String parameters = "()", body = "", source = parameters + body;
        DefaultEmptyConstructorSource = SourceCompressor.compressToString(source);
        DefaultEmptyConstructorSourceBody = parameters.length();
    }

    private static void DefaultEmptyConstructorInit(ExecutionContext cx, FunctionObject f,
            Object[] args) {
        if (DefaultEmptyConstructorArguments) {
            EnvironmentRecord envRec = cx.getVariableEnvironment().getEnvRec();
            envRec.createImmutableBinding("arguments");
            envRec.initializeBinding("arguments", CreateUnmappedArgumentsObject(cx, args));
        }
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
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new async function instance
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
        /* step 8 */
        SetFunctionName(f, name);
        /* step 9 */
        return f;
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new async function instance
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
            /* steps 1-2 (generated code) */
            /* step 3 */
            LexicalEnvironment<?> runningContext = cx.getLexicalEnvironment();
            /* step 4 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = LexicalEnvironment
                    .newDeclarativeEnvironment(runningContext);
            /* step 5 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 6 */
            String name = fd.functionName();
            /* step 7 */
            envRec.createImmutableBinding(name);
            /* step 8 */
            closure = AsyncFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 9 */
            if (fd.hasSuperReference()) {
                MakeMethod(closure, name, null);
            }
            /* step 10 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.FunctionPrototype);
            /* step 11 */
            MakeConstructor(cx, closure, true, prototype);
            /* step 12 */
            SetFunctionName(closure, name);
            /* step 13 */
            envRec.initializeBinding(name, closure);
        }
        /* step 8/14 */
        return closure;
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new async function instance
     */
    public static OrdinaryAsyncFunction EvaluateAsyncArrowFunction(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        /* step 1 (not applicable) */
        /* step 2 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 3-4 */
        OrdinaryAsyncFunction closure = AsyncFunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        /* step ? */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.FunctionPrototype);
        /* step ? */
        MakeConstructor(cx, closure, true, prototype);
        /* step 5 */
        return closure;
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     */
    public static void EvaluatePropertyDefinitionAsync(OrdinaryObject object, Object propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinitionAsync(object, (String) propKey, fd, cx);
        } else {
            EvaluatePropertyDefinitionAsync(object, (Symbol) propKey, fd, cx);
        }
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     */
    public static void EvaluatePropertyDefinitionAsync(OrdinaryObject object, String propKey,
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
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionAsync(OrdinaryObject object, Symbol propKey,
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
     * 
     * @param args
     *            the function arguments
     * @param thisValue
     *            the function this-value
     * @param function
     *            the tail call function
     * @return the tail call trampoline object
     */
    public static Object PrepareForTailCall(Object[] args, Object thisValue, Callable function) {
        return new TailCallInvocation(function, thisValue, args);
    }

    /* ***************************************************************************************** */

    /**
     * B.3.1 __proto___ Property Names in Object Initializers
     * 
     * @param object
     *            the object instance
     * @param value
     *            the new prototype
     * @param cx
     *            the execution context
     */
    public static void defineProtoProperty(OrdinaryObject object, Object value, ExecutionContext cx) {
        // FIXME: function .name and __proto__ interaction unclear
        if (Type.isObjectOrNull(value)) {
            object.setPrototypeOf(cx, Type.objectValueOrNull(value));
        }
    }

    /**
     * B.3.2 Web Legacy Compatibility for Block-Level Function Declarations
     * 
     * @param name
     *            the binding name
     * @param cx
     *            the execution context
     */
    @SuppressWarnings("unchecked")
    public static void initializeFunctionBlockBinding(String name, ExecutionContext cx) {
        assert cx.getVariableEnvironment().getEnvRec() instanceof DeclarativeEnvironmentRecord;
        assert cx.getLexicalEnvironment().getEnvRec() instanceof DeclarativeEnvironmentRecord;
        /* step 2.b.1 */
        LexicalEnvironment<DeclarativeEnvironmentRecord> fenv = (LexicalEnvironment<DeclarativeEnvironmentRecord>) cx
                .getVariableEnvironment();
        /* step 2.b.2 */
        LexicalEnvironment<DeclarativeEnvironmentRecord> benv = (LexicalEnvironment<DeclarativeEnvironmentRecord>) cx
                .getLexicalEnvironment();
        /* step 2.b.3-2.b.4 */
        Object fobj = benv.getEnvRec().getBindingValue(name, false);
        /* step 2.b.5-2.b.6 */
        fenv.getEnvRec().setMutableBinding(name, fobj, false);
        /* step 2.b.7 (return) */
    }
}
