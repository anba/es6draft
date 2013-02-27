/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwSyntaxError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Reference.GetThisValue;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArguments.CompleteMappedArgumentsObject;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArguments.InstantiateArgumentsObject;
import static com.github.anba.es6draft.runtime.types.builtins.ListIterator.FromListIterator;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.GeneratorCreate;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Iterator;

import org.mozilla.javascript.ConsString;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.types.*;
import com.github.anba.es6draft.runtime.types.Function.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArguments;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;
import com.github.anba.es6draft.runtime.types.builtins.GeneratorObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * All kinds of different runtime support methods (TODO: clean-up)
 */
public final class ScriptRuntime {
    public static final Object[] EMPTY_ARRAY = new Object[0];

    private ScriptRuntime() {
    }

    /**
     * 8.2.4.2 PutValue (V, W)
     */
    public static Object PutValue(Object v, Object w, Realm realm) {
        Reference.PutValue(v, w, realm);
        return w;
    }

    /**
     * EvalDeclarationInstantiation
     */
    public static void bindingNotPresentOrThrow(Realm realm, EnvironmentRecord envRec, String name) {
        if (envRec.hasBinding(name)) {
            throw throwSyntaxError(realm, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * GlobalDeclarationInstantiation
     */
    public static void canDeclareLexicalScopedOrThrow(Realm realm, GlobalEnvironmentRecord envRec,
            String name) {
        if (envRec.hasVarDeclaration(name)) {
            throw throwSyntaxError(realm, Messages.Key.VariableRedeclaration, name);
        }
        if (envRec.hasLexicalDeclaration(name)) {
            throw throwSyntaxError(realm, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * GlobalDeclarationInstantiation
     */
    public static void canDeclareVarScopedOrThrow(Realm realm, GlobalEnvironmentRecord envRec,
            String name) {
        if (envRec.hasLexicalDeclaration(name)) {
            throw throwSyntaxError(realm, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * GlobalDeclarationInstantiation
     */
    public static void canDeclareGlobalFunctionOrThrow(Realm realm, GlobalEnvironmentRecord envRec,
            String fn) {
        boolean fnDefinable = envRec.canDeclareGlobalFunction(fn);
        if (!fnDefinable) {
            throw throwTypeError(realm, Messages.Key.InvalidDeclaration, fn);
        }
    }

    /**
     * GlobalDeclarationInstantiation
     */
    public static void canDeclareGlobalVarOrThrow(Realm realm, GlobalEnvironmentRecord envRec,
            String vn) {
        boolean vnDefinable = envRec.canDeclareGlobalVar(vn);
        if (!vnDefinable) {
            throw throwTypeError(realm, Messages.Key.InvalidDeclaration, vn);
        }
    }

    /**
     * 11.1.7 Generator Comprehensions
     * <p>
     * Runtime Semantics: Evaluation<br>
     * TODO: not yet defined in draft [rev. 13]
     */
    public static Scriptable EvaluateGeneratorComprehension(MethodHandle handle, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        ExecutionContext calleeContext = ExecutionContext.newGeneratorComprehensionContext(cx);
        RuntimeInfo.Code newCode = RuntimeInfo.newCode(handle);
        GeneratorObject result = new GeneratorObject(realm, newCode, calleeContext);
        result.initialise(realm);
        return result;
    }

    /**
     * 13.1 Function Definitions
     * <p>
     * Runtime Semantics: Evaluation
     * <ul>
     * <li>FunctionExpression : function ( FormalParameterList ) { FunctionBody }
     * <li>FunctionExpression : function BindingIdentifier ( FormalParameterList ) { FunctionBody }
     * </ul>
     */
    public static Object EvaluateFunctionExpression(RuntimeInfo.Function fd, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        String identifier = fd.functionName();
        if (identifier != null) {
            scope = LexicalEnvironment.newDeclarativeEnvironment(scope);
            EnvironmentRecord envRec = scope.getEnvRec();
            envRec.createImmutableBinding(identifier);
        }
        Function closure = FunctionCreate(realm, FunctionKind.Normal, fd, scope);
        OrdinaryFunction.MakeConstructor(realm, closure);
        if (identifier != null) {
            scope.getEnvRec().initializeBinding(identifier, closure);
        }
        return closure;
    }

    /**
     * 13.2 Arrow Function Definitions
     * <p>
     * Runtime Semantics: Evaluation
     * <ul>
     * <li>ArrowFunction : ArrowParameters => ConciseBody
     * </ul>
     */
    public static Object EvaluateArrowFunction(RuntimeInfo.Function fd, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        Function closure = FunctionCreate(realm, FunctionKind.Arrow, fd, scope);
        return closure;
    }

    /**
     * 13.5 Class Definitions
     * <p>
     * Runtime Semantics: ClassDefinitionEvaluation
     */
    public static Scriptable[] getDefaultClassProto(Realm realm) {
        // step 1
        Scriptable protoParent = realm.getIntrinsic(Intrinsics.ObjectPrototype);
        Scriptable constructorParent = realm.getIntrinsic(Intrinsics.FunctionPrototype);
        // step 3
        Scriptable proto = OrdinaryObject.ObjectCreate(realm, protoParent);
        return new Scriptable[] { proto, constructorParent };
    }

    /**
     * 13.5 Class Definitions
     * <p>
     * Runtime Semantics: ClassDefinitionEvaluation
     */
    public static Scriptable[] getClassProto(Object superClass, Realm realm) {
        Scriptable protoParent;
        Scriptable constructorParent;
        // step 2
        if (Type.isNull(superClass)) {
            protoParent = null;
            constructorParent = realm.getIntrinsic(Intrinsics.FunctionPrototype);
        } else if (!Type.isObject(superClass)) {
            throw throwTypeError(realm, Messages.Key.NotObjectType);
        } else if (!IsConstructor(superClass)) {
            // FIXME: spec bug (should use IsConstructor() instead of [[Construct]])
            protoParent = Type.objectValue(superClass);
            constructorParent = realm.getIntrinsic(Intrinsics.FunctionPrototype);
        } else {
            Object p = Get(Type.objectValue(superClass), "prototype");
            if (!(Type.isObject(p) || Type.isNull(p))) {
                throw throwTypeError(realm, Messages.Key.NotObjectOrNull);
            }
            protoParent = (Type.isNull(p) ? null : Type.objectValue(p));
            constructorParent = Type.objectValue(superClass);
        }
        // step 3
        Scriptable proto = OrdinaryObject.ObjectCreate(realm, protoParent);
        return new Scriptable[] { proto, constructorParent };
    }

    /**
     * 13.5 Class Definitions
     * <p>
     * Runtime Semantics: ClassDefinitionEvaluation
     */
    public static RuntimeInfo.Function CreateDefaultConstructor() {
        RuntimeInfo.Function function = RuntimeInfo.newFunction("constructor", false, true, false,
                0, DefaultConstructorInitMH, DefaultConstructorMH, DefaultConstructorSource);

        return function;
    }

    private static final MethodHandle DefaultConstructorInitMH;
    private static final MethodHandle DefaultConstructorMH;
    private static final String DefaultConstructorSource;
    static {
        Lookup lookup = MethodHandles.publicLookup();
        try {
            DefaultConstructorInitMH = lookup.findStatic(ScriptRuntime.class,
                    "DefaultConstructorInit", MethodType.methodType(Void.TYPE,
                            ExecutionContext.class, Function.class, Object[].class));
            DefaultConstructorMH = lookup.findStatic(ScriptRuntime.class, "DefaultConstructor",
                    MethodType.methodType(Object.class, ExecutionContext.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        try {
            String source = "constructor(...args) { super.constructor(...args); }";
            DefaultConstructorSource = SourceCompressor.compress(source).call();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void DefaultConstructorInit(ExecutionContext cx, Function f, Object[] args) {
        Realm realm = cx.getRealm();
        LexicalEnvironment env = cx.getVariableEnvironment();
        EnvironmentRecord envRec = env.getEnvRec();

        envRec.createMutableBinding("args", false);
        envRec.initializeBinding("args", UNDEFINED);

        envRec.createMutableBinding("arguments", false);
        ExoticArguments ao = InstantiateArgumentsObject(realm, args);

        cx.identifierResolution("args", false).PutValue(createRestArray(ao, 0, realm), realm);

        CompleteMappedArgumentsObject(realm, ao, f, new String[] { "args" }, env);
        envRec.initializeBinding("arguments", ao);
    }

    public static Object DefaultConstructor(ExecutionContext cx) {
        Object completionValue = UNDEFINED;

        Realm realm = cx.getRealm();
        Reference ref = getSuperProperty(GetThisEnvironmentOrThrow(cx), "constructor", false);
        Object func = ref.GetValue(realm);
        Object[] args = SpreadArray(cx.identifierResolution("args", false).GetValue(realm), realm);
        Reference.GetValue(EvaluateCall(ref, func, args, realm), realm);

        return completionValue;
    }

    /**
     * 13.3 Method Definitions, 13.5 Class Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation<br>
     * Runtime Semantics: ClassDefinitionEvaluation
     */
    public static Function EvaluateConstructorMethod(Scriptable constructorParent,
            Scriptable proto, RuntimeInfo.Function fd, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        String propName = "constructor";
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        Function constructor;
        if (fd.hasSuperReference()) {
            constructor = FunctionCreate(realm, FunctionKind.Method, fd, scope, constructorParent,
                    proto, propName);
        } else {
            constructor = FunctionCreate(realm, FunctionKind.Method, fd, scope, constructorParent);
        }
        MakeConstructor(realm, constructor, false, proto);
        proto.defineOwnProperty(propName, new PropertyDescriptor(constructor, true, false, true));

        return constructor;
    }

    /**
     * 13.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>PropertyName ( FormalParameterList ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinition(Scriptable object, String propName,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        Function closure;
        if (fd.hasSuperReference()) {
            closure = FunctionCreate(realm, FunctionKind.Method, fd, scope, null, object, propName);
        } else {
            closure = FunctionCreate(realm, FunctionKind.Method, fd, scope);
        }
        object.defineOwnProperty(propName, new PropertyDescriptor(closure, true, true, true));
    }

    /**
     * 13.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>* PropertyName ( FormalParameterList ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionGenerator(Scriptable object, String propName,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        Function closure;
        if (fd.hasSuperReference()) {
            closure = GeneratorCreate(realm, FunctionKind.Method, fd, scope, null, object, propName);
        } else {
            closure = GeneratorCreate(realm, FunctionKind.Method, fd, scope);
        }
        object.defineOwnProperty(propName, new PropertyDescriptor(closure, true, true, true));
    }

    /**
     * 13.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>get PropertyName ( ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionGetter(Scriptable object, String propName,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        Function closure;
        if (fd.hasSuperReference()) {
            closure = FunctionCreate(realm, FunctionKind.Method, fd, scope, null, object, propName);
        } else {
            closure = FunctionCreate(realm, FunctionKind.Method, fd, scope);
        }
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setGetter(closure);
        desc.setEnumerable(true);
        desc.setConfigurable(true);
        object.defineOwnProperty(propName, desc);
    }

    /**
     * 13.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionSetter(Scriptable object, String propName,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        Function closure;
        if (fd.hasSuperReference()) {
            closure = FunctionCreate(realm, FunctionKind.Method, fd, scope, null, object, propName);
        } else {
            closure = FunctionCreate(realm, FunctionKind.Method, fd, scope);
        }
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setSetter(closure);
        desc.setEnumerable(true);
        desc.setConfigurable(true);
        object.defineOwnProperty(propName, desc);
    }

    /**
     * 13.4 Generator Definitions
     * <p>
     * Runtime Semantics: Evaluation
     * <ul>
     * <li>GeneratorExpression: function* ( FormalParameterList ) { FunctionBody }
     * <li>GeneratorExpression: function* BindingIdentifier ( FormalParameterList ) { FunctionBody }
     * </ul>
     */
    public static Object EvaluateGeneratorExpression(RuntimeInfo.Function fd, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        String identifier = fd.functionName();
        if (identifier != null) {
            scope = LexicalEnvironment.newDeclarativeEnvironment(scope);
            EnvironmentRecord envRec = scope.getEnvRec();
            envRec.createImmutableBinding(identifier);
        }
        Generator closure = GeneratorCreate(realm, FunctionKind.Normal, fd, scope);
        OrdinaryFunction.MakeConstructor(realm, closure);
        if (identifier != null) {
            scope.getEnvRec().initializeBinding(identifier, closure);
        }
        return closure;
    }

    /**
     * Runtime Semantics: ArgumentListEvaluation
     */
    public static Object[] SpreadArray(Object spreadValue, Realm realm) {
        /* step 1-3 (cf. generated code) */
        /* step 4-5 */
        Scriptable spreadObj = AbstractOperations.ToObject(realm, spreadValue);
        /* step 6 */
        Object lenVal = Get(spreadObj, "length");
        /* step 7-8 */
        long spreadLen = ToUint32(realm, lenVal);
        assert spreadLen <= Integer.MAX_VALUE;
        Object[] list = new Object[(int) spreadLen];
        /* step 9-10 */
        for (int n = 0; n < spreadLen; ++n) {
            // FIXME: possible spec bug -> HasProperty() check missing?
            Object nextArg = Get(spreadObj, AbstractOperations.ToString(n));
            list[n] = nextArg;
        }
        return list;
    }

    /**
     * Runtime Semantics: ArgumentListEvaluation
     */
    public static Object[] toFlatArray(Object[] array) {
        int newlen = array.length;
        for (int i = 0, len = array.length; i < len; ++i) {
            if (array[i] instanceof Object[]) {
                newlen += ((Object[]) array[i]).length - 1;
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
     * 11.1.9 Template Literals
     * <p>
     * Runtime Semantics: GetTemplateCallSite Abstract Operation
     */
    public static Scriptable GetTemplateCallSite(String key, MethodHandle handle,
            ExecutionContext cx) {
        Realm realm = cx.getRealm();
        /* step 1 */
        Scriptable callSite = realm.getTemplateCallSite(key);
        if (callSite != null) {
            return callSite;
        }
        /* step 2-3 */
        String[] strings = evaluateCallSite(handle);
        assert (strings.length & 1) == 0;
        /* step 4 */
        int count = strings.length >>> 1;
        /* step 5-6 */
        Scriptable siteObj = ExoticArray.ArrayCreate(realm, count);
        Scriptable rawObj = ExoticArray.ArrayCreate(realm, count);
        /* step 7-8 */
        for (int i = 0, n = strings.length; i < n; i += 2) {
            int index = i >>> 1;
            String prop = AbstractOperations.ToString(index);
            String cookedValue = strings[i];
            siteObj.defineOwnProperty(prop, new PropertyDescriptor(cookedValue, false, true, false));
            String rawValue = strings[i + 1];
            rawObj.defineOwnProperty(prop, new PropertyDescriptor(rawValue, false, true, false));
        }
        /* step 9-11 */
        rawObj.freeze();
        siteObj.defineOwnProperty("raw", new PropertyDescriptor(rawObj, false, false, false));
        siteObj.freeze();
        /* step 12 */
        realm.addTemplateCallSite(key, siteObj);

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
     * 11.2.2 The new Operator
     * <p>
     * Runtime Semantics: Evaluation<br>
     * <ul>
     * <li>NewExpression : new NewExpression
     * <li>MemberExpression : new MemberExpression Arguments
     * </ul>
     */
    public static Object EvaluateConstructorCall(Object constructor, Object[] args, Realm realm) {
        /* step 1-3 (generated code) */
        /* step 4/6 */
        if (!Type.isObject(constructor)) {
            throw throwTypeError(realm, Messages.Key.NotObjectType);
        }
        /* step 5/7 */
        if (!(constructor instanceof Constructor)) {
            throw throwTypeError(realm, Messages.Key.NotConstructor);
        }
        /* step 6/8 */
        return ((Constructor) constructor).construct(args);
    }

    /**
     * 11.2.3 Function Calls: EvaluateCall
     */
    public static Callable CheckCallable(Object func, Realm realm) {
        /* step 5 */
        if (!Type.isObject(func)) {
            throw throwTypeError(realm, Messages.Key.NotObjectType);
        }
        /* step 6 */
        if (!IsCallable(func)) {
            throw throwTypeError(realm, Messages.Key.NotCallable);
        }
        return (Callable) func;
    }

    /**
     * 11.2.3 Function Calls: EvaluateCall
     */
    public static Object GetCallThisValue(Object ref, Realm realm) {
        Object thisValue;
        if (ref instanceof Reference) {
            /* step 7 */
            Reference r = (Reference) ref;
            if (r.isPropertyReference()) {
                thisValue = GetThisValue(realm, r);
            } else {
                assert r.getBase() instanceof EnvironmentRecord;
                thisValue = ((EnvironmentRecord) r.getBase()).withBaseObject();
                if (thisValue == null) {
                    thisValue = Undefined.UNDEFINED;
                }
            }
        } else {
            /* step 8 */
            thisValue = Undefined.UNDEFINED;
        }
        return thisValue;
    }

    /**
     * 11.2.3 Function Calls: EvaluateCall
     */
    public static boolean IsBuiltinEval(Object ref, Callable f, Realm realm) {
        if (ref instanceof Reference) {
            Reference r = (Reference) ref;
            if (!r.isPropertyReference()) {
                assert !r.isUnresolvableReference() && r.getBase() instanceof EnvironmentRecord;
                return (f == realm.getBuiltinEval());
            }
        }
        return false;
    }

    /**
     * 11.2.3 Function Calls: EvaluateCall
     */
    public static Object EvaluateCall(Object ref, Object func, Object[] args, Realm realm) {
        /* step 1-4 (compiled code) */
        /* step 5-6 */
        Callable f = CheckCallable(func, realm);
        /* step 7-8 */
        Object thisValue = GetCallThisValue(ref, realm);
        /* step 10 */
        Object result = f.call(thisValue, args);
        /* step 12 */
        return result;
    }

    /**
     * 11.2.3 Function Calls: EvaluateCall
     */
    public static Object EvaluateEvalCall(Object ref, Object func, Object[] args,
            ExecutionContext cx, boolean strict, boolean global) {
        Realm realm = cx.getRealm();
        /* step 1-4 (compiled code) */
        /* step 5-6 */
        Callable f = CheckCallable(func, realm);

        if (IsBuiltinEval(ref, f, realm)) {
            Object x = args.length > 0 ? args[0] : Undefined.UNDEFINED;
            return Eval.directEval(x, cx, strict, global);
        }

        /* step 7-8 */
        Object thisValue = GetCallThisValue(ref, realm);
        /* step 10 */
        Object result = f.call(thisValue, args);
        /* step 12 */
        return result;
    }

    /**
     * 11.2.4 The super Keyword
     */
    public static EnvironmentRecord GetThisEnvironmentOrThrow(ExecutionContext cx) {
        EnvironmentRecord envRec = cx.getThisEnvironment();
        if (!envRec.hasSuperBinding()) {
            throwReferenceError(cx.getRealm(), Messages.Key.MissingSuperBinding);
        }
        return envRec;
    }

    /**
     * 11.2.4 The super Keyword
     * <p>
     * Runtime Semantics: Evaluation<br>
     * MemberExpression : super [ Expression ]
     */
    public static Reference getSuperElement(EnvironmentRecord envRec, Object propertyNameValue,
            Realm realm, boolean strict) {
        assert envRec instanceof FunctionEnvironmentRecord;
        Object actualThis = envRec.getThisBinding();
        // TODO: evaluation order -> side-effects?
        Scriptable baseValue = ((FunctionEnvironmentRecord) envRec).getSuperBase();
        CheckObjectCoercible(realm, propertyNameValue);
        // Object propertyKey = ToPropertyKey(realm, propertyNameValue);
        String propertyKey = ToFlatString(realm, propertyNameValue);
        return new Reference(baseValue, propertyKey, strict, actualThis);
    }

    /**
     * 11.2.4 The super Keyword
     * <p>
     * Runtime Semantics: Evaluation<br>
     * MemberExpression : super . IdentifierName
     */
    public static Reference getSuperProperty(EnvironmentRecord envRec, String propertyKey,
            boolean strict) {
        assert envRec instanceof FunctionEnvironmentRecord;
        Object actualThis = envRec.getThisBinding();
        // TODO: evaluation order -> side-effects?
        Scriptable baseValue = ((FunctionEnvironmentRecord) envRec).getSuperBase();
        return new Reference(baseValue, propertyKey, strict, actualThis);
    }

    /**
     * 11.2.4 The super Keyword
     * <p>
     * Runtime Semantics: Evaluation<br>
     * CallExpression : super Arguments
     */
    public static Reference getSuperMethod(EnvironmentRecord envRec, boolean strict) {
        assert envRec instanceof FunctionEnvironmentRecord;
        Object actualThis = envRec.getThisBinding();
        // TODO: evaluation order -> side-effects?
        Scriptable baseValue = ((FunctionEnvironmentRecord) envRec).getSuperBase();
        String propertyKey = ((FunctionEnvironmentRecord) envRec).getMethodName();
        return new Reference(baseValue, propertyKey, strict, actualThis);
    }

    /**
     * 11.4.1 The delete Operator
     */
    public static boolean delete(Object expr, Realm realm) {
        /* step 1-2 (generated code) */
        /* step 3 */
        if (!(expr instanceof Reference)) {
            return true;
        }
        Reference ref = (Reference) expr;
        /* step 4 */
        if (ref.isUnresolvableReference()) {
            if (ref.isStrictReference()) {
                throw throwSyntaxError(realm, Messages.Key.UnqualifiedDelete);
            }
            return true;
        }
        /* step 5 */
        if (ref.isPropertyReference()) {
            if (ref.isSuperReference()) {
                throw throwReferenceError(realm, Messages.Key.SuperDelete);
            }
            Scriptable obj = AbstractOperations.ToObject(realm, ref.getBase());
            boolean deleteStatus = obj.delete(ref.getReferencedName());
            if (!deleteStatus && ref.isStrictReference()) {
                // FIXME: spec bug (typing 'typeError')
                throw throwTypeError(realm, Messages.Key.PropertyNotDeletable,
                        ref.getReferencedName());
            }
            // FIXME: spec bug (return value)
            return deleteStatus;
        }
        /* step 6 */
        assert ref.getBase() instanceof EnvironmentRecord;
        EnvironmentRecord bindings = (EnvironmentRecord) ref.getBase();
        return bindings.deleteBinding(ref.getReferencedName());
    }

    /**
     * 11.4.3 The typeof Operator
     */
    public static String typeof(Object val, Realm realm) {
        if (val instanceof Reference) {
            if (((Reference) val).isUnresolvableReference()) {
                return "undefined";
            }
            val = ((Reference) val).GetValue(realm);
        }
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
        case Object:
        default:
            if (IsCallable(val)) {
                return "function";
            }
            return "object";
        }
    }

    /**
     * 11.6.1 The Addition operator ( + )
     */
    public static Object add(Object lval, Object rval, Realm realm) {
        Object lprim = ToPrimitive(realm, lval, null);
        Object rprim = ToPrimitive(realm, rval, null);
        if (Type.isString(lprim) || Type.isString(rprim)) {
            CharSequence lstr = AbstractOperations.ToString(realm, lprim);
            CharSequence rstr = AbstractOperations.ToString(realm, rprim);
            return add(lstr, rstr);
        }
        return ToNumber(realm, lprim) + ToNumber(realm, rprim);
    }

    /**
     * 11.6.1 The Addition operator ( + )
     */
    public static CharSequence add(CharSequence lstr, CharSequence rstr) {
        int llen = lstr.length(), rlen = rstr.length();
        if (llen == 0) {
            return rstr;
        }
        if (rlen == 0) {
            return lstr;
        }
        if (llen + rlen <= 10) {
            return new StringBuilder(llen + rlen).append(lstr).append(rstr).toString();
        }
        return new ConsString(lstr, rstr);
    }

    /**
     * 11.8.1 The Abstract Relational Comparison Algorithm
     */
    public static int relationalComparison(Object x, Object y, boolean leftFirst, Realm realm) {
        // true -> 1
        // false -> 0
        // undefined -> -1
        Object px, py;
        if (leftFirst) {
            px = ToPrimitive(realm, x, Type.Number);
            py = ToPrimitive(realm, y, Type.Number);
        } else {
            py = ToPrimitive(realm, y, Type.Number);
            px = ToPrimitive(realm, x, Type.Number);
        }
        if (!(Type.isString(px) && Type.isString(py))) {
            double nx = ToNumber(realm, px);
            double ny = ToNumber(realm, py);
            if (Double.isNaN(nx) || Double.isNaN(ny)) {
                return -1;
            }
            if (nx == ny) {
                return 0;
            }
            if (nx == Double.POSITIVE_INFINITY) {
                return 0;
            }
            if (ny == Double.POSITIVE_INFINITY) {
                return 1;
            }
            if (ny == Double.NEGATIVE_INFINITY) {
                return 0;
            }
            if (nx == Double.NEGATIVE_INFINITY) {
                return 1;
            }
            return (nx < ny ? 1 : 0);
        } else {
            int c = Type.stringValue(px).toString().compareTo(Type.stringValue(py).toString());
            return c < 0 ? 1 : 0;
        }
    }

    /**
     * 11.8 Relational Operators
     */
    public static boolean in(Object lval, Object rval, Realm realm) {
        if (!Type.isObject(rval)) {
            throw throwTypeError(realm, Messages.Key.NotObjectType);
        }
        Object p = ToPropertyKey(realm, lval);
        if (p instanceof String) {
            return HasProperty(Type.objectValue(rval), (String) p);
        } else {
            return HasProperty(Type.objectValue(rval), (Symbol) p);
        }
    }

    /**
     * 11.8.1 Runtime Semantics<br>
     * Runtime Semantics: Evaluation
     */
    public static boolean instanceOfOperator(Object obj, Object constructor, Realm realm) {
        if (!Type.isObject(constructor)) {
            throw throwTypeError(realm, Messages.Key.NotObjectType);
        }
        Callable instOfHandler = GetMethod(realm, Type.objectValue(constructor),
                BuiltinSymbol.hasInstance.get());
        if (instOfHandler != null) {
            Object result = instOfHandler.call(constructor, obj);
            // FIXME: spec bug (missing ToBoolean)
            return ToBoolean(result);
        }
        if (!IsCallable(constructor)) {
            throw throwTypeError(realm, Messages.Key.NotCallable);
        }
        return OrdinaryHasInstance(realm, constructor, obj);
    }

    /**
     * 11.9.1 The Abstract Equality Comparison Algorithm
     */
    public static boolean equalityComparison(Object x, Object y, Realm realm) {
        if (x == y) {
            if (x instanceof Double) {
                return !((Double) x).isNaN();
            }
            return true;
        }
        Type tx = Type.of(x);
        Type ty = Type.of(y);
        if (tx == ty) {
            return strictEqualityComparison(x, y);
        }
        if (tx == Type.Null && ty == Type.Undefined) {
            return true;
        }
        if (tx == Type.Undefined && ty == Type.Null) {
            return true;
        }
        if (tx == Type.Number && ty == Type.String) {
            // return equalityComparison(realm, x, ToNumber(realm, y));
            return Type.numberValue(x) == ToNumber(realm, y);
        }
        if (tx == Type.String && ty == Type.Number) {
            // return equalityComparison(realm, ToNumber(realm, x), y);
            return ToNumber(realm, x) == Type.numberValue(y);
        }
        if (tx == Type.Boolean) {
            return equalityComparison(ToNumber(realm, x), y, realm);
        }
        if (ty == Type.Boolean) {
            return equalityComparison(x, ToNumber(realm, y), realm);
        }
        if ((tx == Type.String || tx == Type.Number) && ty == Type.Object) {
            return equalityComparison(x, ToPrimitive(realm, y, null), realm);
        }
        if (tx == Type.Object && (ty == Type.String || ty == Type.Number)) {
            return equalityComparison(ToPrimitive(realm, x, null), y, realm);
        }
        return false;
    }

    /**
     * 11.9.1 The Strict Equality Comparison Algorithm
     */
    public static boolean strictEqualityComparison(Object x, Object y) {
        if (x == y) {
            if (x instanceof Double) {
                return !((Double) x).isNaN();
            }
            return true;
        }
        Type tx = Type.of(x);
        Type ty = Type.of(y);
        if (tx != ty) {
            return false;
        }
        if (tx == Type.Undefined) {
            return true;
        }
        if (tx == Type.Null) {
            return true;
        }
        if (tx == Type.Number) {
            return Type.numberValue(x) == Type.numberValue(y);
        }
        if (tx == Type.String) {
            return Type.stringValue(x).toString().equals(Type.stringValue(y).toString());
        }
        if (tx == Type.Boolean) {
            return Type.booleanValue(x) == Type.booleanValue(y);
        }
        return (x == y);
    }

    /**
     * 12.13 The throw Statement
     */
    public static ScriptException _throw(Object val) {
        throw new ScriptException(val);
    }

    /**
     * 13.4 Generator Definitions
     */
    public static Object yield(Object value, ExecutionContext cx) {
        return cx.getCurrentGenerator().yield(value);
    }

    /**
     * 13.4 Generator Definitions
     */
    public static Object delegatedYield(Object expr, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        boolean send = true;
        Object received = UNDEFINED;
        Object result = UNDEFINED;
        Scriptable g = AbstractOperations.ToObject(realm, expr);
        try {
            while (true) {
                Object next;
                if (send) {
                    next = Invoke(realm, g, "send", new Object[] { received });
                } else {
                    next = Invoke(realm, g, "throw", new Object[] { received });
                }
                try {
                    received = yield(next, cx);
                    send = true;
                } catch (ScriptException e) {
                    received = e.getValue();
                    send = false;
                }
            }
        } catch (ScriptException e) {
            if (!isStopIteration(realm, e)) {
                throw e;
            }
            // TODO: StopIteration with value
            // result = ((StopIteration) e.getValue()).getValue();
            result = UNDEFINED;
        } finally {
            try {
                Invoke(realm, g, "close", new Object[] {});
            } catch (ScriptException ignore) {
            }
        }
        return result;
    }

    // same functionality as `IteratorComplete`
    private static boolean isStopIteration(Realm realm, ScriptException e) {
        return (realm.getIntrinsic(Intrinsics.StopIteration) == e.getValue());
    }

    public static Object RegExp(Realm realm, String re, String flags) {
        // FIXME: spec bug (call abstract operation RegExpCreate?!)
        Constructor ctor = (Constructor) realm.getIntrinsic(Intrinsics.RegExp);
        return ctor.construct(re, flags);
    }

    /**
     * Helper function
     */
    public static boolean isUndefined(Object o) {
        return Type.isUndefined(o);
    }

    /**
     * Helper function
     */
    public static Iterator<?> enumerate(Object o, Realm realm) {
        Scriptable obj = AbstractOperations.ToObject(realm, o);
        return FromListIterator(realm, obj.enumerate());
    }

    /**
     * Helper function
     */
    public static Iterator<?> iterate(Object o, Realm realm) {
        Scriptable obj = AbstractOperations.ToObject(realm, o);
        Object keys = AbstractOperations.Invoke(realm, obj, BuiltinSymbol.iterator.get());
        return FromListIterator(realm, keys);
    }

    /**
     * 12.2.4 Destructuring Binding Patterns
     * <p>
     * Runtime Semantics: Indexed Binding Initialisation<br>
     * BindingRestElement : ... BindingIdentifier
     */
    public static Scriptable createRestArray(Scriptable array, int index, Realm realm) {
        Object lenVal = Get(array, "length");
        long arrayLength = ToUint32(realm, lenVal);
        Scriptable result = ExoticArray.ArrayCreate(realm, 0);
        long n = 0;
        while (index < arrayLength) {
            String p = AbstractOperations.ToString(index);
            boolean exists = HasProperty(array, p);
            // TODO: assert exists iff FunctionRestParameter
            if (exists) {
                Object v = Get(array, p);
                PropertyDescriptor desc = new PropertyDescriptor(v, true, true, true);
                result.defineOwnProperty(AbstractOperations.ToString(n), desc);
            }
            n = n + 1;
            index = index + 1;
        }
        return result;
    }

    /**
     * 11.2.1 Property Accessors
     * <p>
     * Runtime Semantics: Evaluation<br>
     * MemberExpression : MemberExpression . IdentifierName
     */
    public static Reference getProperty(Object baseValue, String propertyNameString, Realm realm,
            boolean strict) {
        /* step 1-6 (generated code) */
        /* step 7 */
        CheckObjectCoercible(realm, baseValue);
        /* step 8-10 */
        return new Reference(baseValue, propertyNameString, strict);
    }

    /**
     * 11.2.1 Property Accessors
     * <p>
     * Runtime Semantics: Evaluation<br>
     * MemberExpression : MemberExpression [ Expression ]
     */
    public static Reference getElement(Object baseValue, Object propertyNameValue, Realm realm,
            boolean strict) {
        /* step 1-6 (generated code) */
        /* step 7 */
        CheckObjectCoercible(realm, baseValue);
        /* step 8 */
        String propertyNameString = ToFlatString(realm, propertyNameValue);
        /* step 9-10 */
        return new Reference(baseValue, propertyNameString, strict);
    }

    /**
     * 11.1.5 Object Initialiser
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     */
    public static void defineProperty(Scriptable object, String propertyName, Object value) {
        object.defineOwnProperty(propertyName, new PropertyDescriptor(value, true, true, true));
    }

    /**
     * B.3.1.3 __proto___ Object Initialisers
     */
    public static void defineProtoProperty(Scriptable object, Object value) {
        // use [[SetP]] to comply with current SpiderMonkey/JSC behaviour
        object.set("__proto__", value, object);
    }

    /**
     * 11.1.4.1 Array Literal
     * <p>
     * Runtime Semantics: Array Accumulation
     */
    public static void defineProperty(Scriptable array, int nextIndex, Object value) {
        // String propertyName = ToString(ToUint32(nextIndex));
        String propertyName = AbstractOperations.ToString(nextIndex);
        array.defineOwnProperty(propertyName, new PropertyDescriptor(value, true, true, true));
    }

    /**
     * 11.1.4.1 Array Literal
     * <p>
     * Runtime Semantics: Array Accumulation<br>
     * Runtime Semantics: Evaluation
     */
    public static int ArrayAccumulationSpreadElement(Scriptable array, int nextIndex,
            Object spreadValue, Realm realm) {
        /* step 1-2 (cf. generated code) */
        /* step 3-4 */
        Scriptable spreadObj = AbstractOperations.ToObject(realm, spreadValue);
        /* step 5 */
        Object lenVal = Get(spreadObj, "length");
        /* step 6-7 */
        long spreadLen = ToUint32(realm, lenVal);
        /* step 8-9 */
        for (long n = 0; n < spreadLen; ++n, ++nextIndex) {
            boolean exists = HasProperty(spreadObj, AbstractOperations.ToString(n));
            if (exists) {
                // FIXME: possible spec bug
                Object v = spreadObj.get(AbstractOperations.ToString(n), spreadObj);
                defineProperty(array, nextIndex, v);
            }
        }
        return nextIndex;
    }
}
