/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwInternalError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwSyntaxError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.objects.StopIterationObject.IteratorComplete;
import static com.github.anba.es6draft.runtime.types.Reference.GetThisValue;
import static com.github.anba.es6draft.runtime.types.Reference.GetValue;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArguments.CompleteStrictArgumentsObject;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArguments.InstantiateArgumentsObject;
import static com.github.anba.es6draft.runtime.types.builtins.ListIterator.FromListIterator;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.GeneratorCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Iterator;

import org.mozilla.javascript.ConsString;

import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.objects.ErrorObject;
import com.github.anba.es6draft.runtime.types.*;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArguments;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.GeneratorObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;

/**
 * All kinds of different runtime support methods (TODO: clean-up)
 */
public final class ScriptRuntime {
    public static final Object[] EMPTY_ARRAY = new Object[0];

    private ScriptRuntime() {
    }

    /**
     * EvalDeclarationInstantiation
     */
    public static void bindingNotPresentOrThrow(ExecutionContext cx, EnvironmentRecord envRec,
            String name) {
        if (envRec.hasBinding(name)) {
            throw throwSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * GlobalDeclarationInstantiation
     */
    public static void canDeclareLexicalScopedOrThrow(ExecutionContext cx,
            GlobalEnvironmentRecord envRec, String name) {
        if (envRec.hasVarDeclaration(name)) {
            throw throwSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
        if (envRec.hasLexicalDeclaration(name)) {
            throw throwSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * GlobalDeclarationInstantiation
     */
    public static void canDeclareVarScopedOrThrow(ExecutionContext cx,
            GlobalEnvironmentRecord envRec, String name) {
        if (envRec.hasLexicalDeclaration(name)) {
            throw throwSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * GlobalDeclarationInstantiation
     */
    public static void canDeclareGlobalFunctionOrThrow(ExecutionContext cx,
            GlobalEnvironmentRecord envRec, String fn) {
        boolean fnDefinable = envRec.canDeclareGlobalFunction(fn);
        if (!fnDefinable) {
            throw throwTypeError(cx, Messages.Key.InvalidDeclaration, fn);
        }
    }

    /**
     * GlobalDeclarationInstantiation
     */
    public static void canDeclareGlobalVarOrThrow(ExecutionContext cx,
            GlobalEnvironmentRecord envRec, String vn) {
        boolean vnDefinable = envRec.canDeclareGlobalVar(vn);
        if (!vnDefinable) {
            throw throwTypeError(cx, Messages.Key.InvalidDeclaration, vn);
        }
    }

    /**
     * 11.1.7 Generator Comprehensions
     * <p>
     * Runtime Semantics: Evaluation<br>
     * TODO: not yet defined in draft [rev. 13]
     */
    public static ScriptObject EvaluateGeneratorComprehension(MethodHandle handle,
            ExecutionContext cx) {
        ExecutionContext calleeContext = ExecutionContext.newGeneratorComprehensionContext(cx);
        RuntimeInfo.Code newCode = RuntimeInfo.newCode(handle);
        GeneratorObject result = new GeneratorObject(cx.getRealm(), newCode, calleeContext);
        ScriptObject proto = cx.getIntrinsic(Intrinsics.GeneratorPrototype);
        result.setPrototype(cx, proto);
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
    public static OrdinaryFunction EvaluateFunctionExpression(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        if (fd.hasScopedName()) {
            scope = LexicalEnvironment.newDeclarativeEnvironment(scope);
            EnvironmentRecord envRec = scope.getEnvRec();
            envRec.createImmutableBinding(fd.functionName());
        }
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Normal, fd, scope);
        MakeConstructor(cx, closure);
        if (fd.hasScopedName()) {
            scope.getEnvRec().initializeBinding(fd.functionName(), closure);
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
    public static OrdinaryFunction EvaluateArrowFunction(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        return closure;
    }

    /**
     * 13.5 Class Definitions
     * <p>
     * Runtime Semantics: ClassDefinitionEvaluation
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
     * 13.5 Class Definitions
     * <p>
     * Runtime Semantics: ClassDefinitionEvaluation
     */
    public static ScriptObject[] getClassProto(Object superClass, ExecutionContext cx) {
        ScriptObject protoParent;
        ScriptObject constructorParent;
        // step 2
        if (Type.isNull(superClass)) {
            protoParent = null;
            constructorParent = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        } else if (!Type.isObject(superClass)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        } else if (!IsConstructor(superClass)) {
            throw throwTypeError(cx, Messages.Key.NotConstructor);
        } else {
            Object p = Get(cx, Type.objectValue(superClass), "prototype");
            if (!(Type.isObject(p) || Type.isNull(p))) {
                throw throwTypeError(cx, Messages.Key.NotObjectOrNull);
            }
            protoParent = (Type.isNull(p) ? null : Type.objectValue(p));
            constructorParent = Type.objectValue(superClass);
        }
        // step 3
        ScriptObject proto = ObjectCreate(cx, protoParent);
        return new ScriptObject[] { proto, constructorParent };
    }

    /**
     * 13.5 Class Definitions
     * <p>
     * Runtime Semantics: ClassDefinitionEvaluation
     */
    public static RuntimeInfo.Function CreateDefaultConstructor() {
        String functionName = "constructor";
        int functionFlags = RuntimeInfo.FunctionFlags.Strict.getValue()
                | RuntimeInfo.FunctionFlags.Super.getValue();
        int expectedArguments = 0;
        RuntimeInfo.Function function = RuntimeInfo.newFunction(functionName, functionFlags,
                expectedArguments, DefaultConstructorInitMH, DefaultConstructorMH,
                DefaultConstructorSource);

        return function;
    }

    private static final MethodHandle DefaultConstructorInitMH;
    private static final MethodHandle DefaultConstructorMH;
    private static final String DefaultConstructorSource;
    static {
        Lookup lookup = MethodHandles.publicLookup();
        try {
            DefaultConstructorInitMH = lookup.findStatic(ScriptRuntime.class,
                    "DefaultConstructorInit", MethodType.methodType(ExoticArguments.class,
                            ExecutionContext.class, FunctionObject.class, Object[].class));
            DefaultConstructorMH = lookup.findStatic(ScriptRuntime.class, "DefaultConstructor",
                    MethodType.methodType(Object.class, ExecutionContext.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        try {
            String source = "constructor(...args) { super(...args); }";
            DefaultConstructorSource = SourceCompressor.compress(source).call();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static ExoticArguments DefaultConstructorInit(ExecutionContext cx, FunctionObject f,
            Object[] args) {
        LexicalEnvironment env = cx.getVariableEnvironment();
        EnvironmentRecord envRec = env.getEnvRec();

        envRec.createMutableBinding("args", false);
        envRec.initializeBinding("args", UNDEFINED);

        envRec.createImmutableBinding("arguments");
        ExoticArguments ao = InstantiateArgumentsObject(cx, args);

        cx.identifierResolution("args", true).PutValue(createRestArray(ao, 0, cx), cx);

        CompleteStrictArgumentsObject(cx, ao);
        envRec.initializeBinding("arguments", ao);

        return ao;
    }

    public static Object DefaultConstructor(ExecutionContext cx) {
        Object completionValue = UNDEFINED;

        // super()
        Reference ref = MakeSuperReference(cx, null, true);
        // EvaluateCall: super(...args)
        Object func = ref.GetValue(cx);
        Object[] argList = SpreadArray(cx.identifierValue("args", true), cx);
        Callable f = CheckCallable(func, cx);
        Object thisValue = GetCallThisValue(ref, cx);
        Object result = f.call(cx, thisValue, argList);
        GetValue(result, cx);

        return completionValue;
    }

    /**
     * 13.3 Method Definitions, 13.5 Class Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation<br>
     * Runtime Semantics: ClassDefinitionEvaluation
     */
    public static OrdinaryFunction EvaluateConstructorMethod(ScriptObject constructorParent,
            ScriptObject proto, RuntimeInfo.Function fd, ExecutionContext cx) {
        String propName = "constructor";
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        OrdinaryFunction constructor;
        if (fd.hasSuperReference()) {
            // FIXME: spec bug (constructorParent not used) (Bug 1416)
            constructor = FunctionCreate(cx, FunctionKind.ConstructorMethod, fd, scope,
                    constructorParent, proto, propName);
        } else {
            // FIXME: spec bug (constructorParent not used) (Bug 1416)
            constructor = FunctionCreate(cx, FunctionKind.ConstructorMethod, fd, scope,
                    constructorParent);
        }
        DefinePropertyOrThrow(cx, proto, propName, new PropertyDescriptor(constructor, true, true,
                true));

        MakeConstructor(cx, constructor, false, proto);
        proto.defineOwnProperty(cx, propName,
                new PropertyDescriptor(constructor, true, false, true));

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
    public static void EvaluatePropertyDefinition(ScriptObject object, String propName,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        OrdinaryFunction closure;
        if (fd.hasSuperReference()) {
            closure = FunctionCreate(cx, FunctionKind.Method, fd, scope, null, object, propName);
        } else {
            closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        }
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, true, true);
        DefinePropertyOrThrow(cx, object, propName, desc);
    }

    /**
     * 13.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>* PropertyName ( FormalParameterList ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionGenerator(ScriptObject object, String propName,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        OrdinaryGenerator closure;
        if (fd.hasSuperReference()) {
            closure = GeneratorCreate(cx, FunctionKind.Method, fd, scope, null, object, propName);
        } else {
            closure = GeneratorCreate(cx, FunctionKind.Method, fd, scope);
        }
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, true, true);
        DefinePropertyOrThrow(cx, object, propName, desc);
    }

    /**
     * 13.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>get PropertyName ( ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionGetter(ScriptObject object, String propName,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        OrdinaryFunction closure;
        if (fd.hasSuperReference()) {
            closure = FunctionCreate(cx, FunctionKind.Method, fd, scope, null, object, propName);
        } else {
            closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        }
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setGetter(closure);
        desc.setEnumerable(true);
        desc.setConfigurable(true);
        // FIXME: spec bug (not updated to use DefinePropertyOrThrow) (Bug 1417)
        DefinePropertyOrThrow(cx, object, propName, desc);
    }

    /**
     * 13.3 Method Definitions
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     * <ul>
     * <li>set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     */
    public static void EvaluatePropertyDefinitionSetter(ScriptObject object, String propName,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        OrdinaryFunction closure;
        if (fd.hasSuperReference()) {
            closure = FunctionCreate(cx, FunctionKind.Method, fd, scope, null, object, propName);
        } else {
            closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        }
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setSetter(closure);
        desc.setEnumerable(true);
        desc.setConfigurable(true);
        DefinePropertyOrThrow(cx, object, propName, desc);
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
    public static OrdinaryGenerator EvaluateGeneratorExpression(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        LexicalEnvironment scope = cx.getLexicalEnvironment();
        if (fd.hasScopedName()) {
            scope = LexicalEnvironment.newDeclarativeEnvironment(scope);
            EnvironmentRecord envRec = scope.getEnvRec();
            envRec.createImmutableBinding(fd.functionName());
        }
        OrdinaryGenerator closure = GeneratorCreate(cx, FunctionKind.Normal, fd, scope);
        MakeConstructor(cx, closure);
        if (fd.hasScopedName()) {
            scope.getEnvRec().initializeBinding(fd.functionName(), closure);
        }
        return closure;
    }

    /**
     * Runtime Semantics: ArgumentListEvaluation
     */
    public static Object[] SpreadArray(Object spreadValue, ExecutionContext cx) {
        /* step 1-3 (cf. generated code) */
        /* step 4-5 */
        ScriptObject spreadObj = ToObject(cx, spreadValue);
        /* step 6 */
        Object lenVal = Get(cx, spreadObj, "length");
        /* step 7-8 */
        long spreadLen = ToUint32(cx, lenVal);
        assert spreadLen <= Integer.MAX_VALUE;
        Object[] list = new Object[(int) spreadLen];
        /* step 9-10 */
        for (int n = 0; n < spreadLen; ++n) {
            Object nextArg = Get(cx, spreadObj, ToString(n));
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
    public static ScriptObject GetTemplateCallSite(String key, MethodHandle handle,
            ExecutionContext cx) {
        Realm realm = cx.getRealm();
        /* step 1 */
        ScriptObject callSite = realm.getTemplateCallSite(key);
        if (callSite != null) {
            return callSite;
        }
        /* step 2-3 */
        String[] strings = evaluateCallSite(handle);
        assert (strings.length & 1) == 0;
        /* step 4 */
        int count = strings.length >>> 1;
        /* step 5-6 */
        ScriptObject siteObj = ExoticArray.ArrayCreate(cx, count);
        ScriptObject rawObj = ExoticArray.ArrayCreate(cx, count);
        /* step 7-8 */
        for (int i = 0, n = strings.length; i < n; i += 2) {
            int index = i >>> 1;
            String prop = ToString(index);
            String cookedValue = strings[i];
            siteObj.defineOwnProperty(cx, prop, new PropertyDescriptor(cookedValue, false, true,
                    false));
            String rawValue = strings[i + 1];
            rawObj.defineOwnProperty(cx, prop, new PropertyDescriptor(rawValue, false, true, false));
        }
        /* step 9-11 */
        rawObj.setIntegrity(cx, IntegrityLevel.Frozen);
        siteObj.defineOwnProperty(cx, "raw", new PropertyDescriptor(rawObj, false, false, false));
        siteObj.setIntegrity(cx, IntegrityLevel.Frozen);
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
     * <li>MemberExpression : new super Arguments<sub>opt</sub>
     * </ul>
     */
    public static Object EvaluateConstructorCall(Object constructor, Object[] args,
            ExecutionContext cx) {
        /* step 1-3 (generated code) */
        /* step 4/6 */
        if (!Type.isObject(constructor)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 5/7 */
        if (!(constructor instanceof Constructor)) {
            throw throwTypeError(cx, Messages.Key.NotConstructor);
        }
        /* step 6/8 */
        return ((Constructor) constructor).construct(cx, args);
    }

    /**
     * 11.2.3 Function Calls: EvaluateCall
     */
    public static Callable CheckCallable(Object func, ExecutionContext cx) {
        /* step 5 */
        if (!Type.isObject(func)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 6 */
        if (!IsCallable(func)) {
            throw throwTypeError(cx, Messages.Key.NotCallable);
        }
        return (Callable) func;
    }

    /**
     * 11.2.3 Function Calls: EvaluateCall
     */
    public static Object GetCallThisValue(Object ref, ExecutionContext cx) {
        Object thisValue;
        if (ref instanceof Reference) {
            /* step 7 */
            Reference r = (Reference) ref;
            if (r.isPropertyReference()) {
                thisValue = GetThisValue(cx, r);
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
    public static boolean IsBuiltinEval(Object ref, Callable f, ExecutionContext cx) {
        if (ref instanceof Reference) {
            Reference r = (Reference) ref;
            if (!r.isPropertyReference()) {
                assert !r.isUnresolvableReference() && r.getBase() instanceof EnvironmentRecord;
                return (f == cx.getRealm().getBuiltinEval());
            }
        }
        return false;
    }

    /**
     * Runtime Semantics: Abstract Operation MakeSuperReference(propertyKey, strict)
     */
    public static Reference MakeSuperReference(ExecutionContext cx, String propertyKey,
            boolean strict) {
        EnvironmentRecord envRec = cx.getThisEnvironment();
        if (!envRec.hasSuperBinding()) {
            throwReferenceError(cx, Messages.Key.MissingSuperBinding);
        }
        assert envRec instanceof FunctionEnvironmentRecord;
        Object actualThis = envRec.getThisBinding();
        ScriptObject baseValue = ((FunctionEnvironmentRecord) envRec).getSuperBase();
        // CheckObjectCoercible(cx.getRealm(), baseValue);
        if (baseValue == null) {
            throw throwTypeError(cx, Messages.Key.UndefinedOrNull);
        }
        if (propertyKey == null) {
            propertyKey = ((FunctionEnvironmentRecord) envRec).getMethodName();
        }
        return new Reference.SuperNameReference(baseValue, propertyKey, strict, actualThis);
    }

    /**
     * 11.4.1 The delete Operator
     */
    public static boolean delete(Object expr, ExecutionContext cx) {
        /* step 1-2 (generated code) */
        /* step 3 */
        if (!(expr instanceof Reference)) {
            return true;
        }
        Reference ref = (Reference) expr;
        /* step 4 */
        if (ref.isUnresolvableReference()) {
            if (ref.isStrictReference()) {
                throw throwSyntaxError(cx, Messages.Key.UnqualifiedDelete);
            }
            return true;
        }
        /* step 5 */
        if (ref.isPropertyReference()) {
            if (ref.isSuperReference()) {
                throw throwReferenceError(cx, Messages.Key.SuperDelete);
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
                throw throwTypeError(cx, Messages.Key.PropertyNotDeletable, ref.getReferencedName()
                        .toString());
            }
            // FIXME: spec bug (return value)
            return deleteStatus;
        }
        /* step 6 */
        assert ref instanceof Reference.IdentifierReference;
        Reference.IdentifierReference idref = (Reference.IdentifierReference) ref;
        EnvironmentRecord bindings = idref.getBase();
        return bindings.deleteBinding(idref.getReferencedName());
    }

    /**
     * 11.4.3 The typeof Operator
     */
    public static String typeof(Object val, ExecutionContext cx) {
        if (val instanceof Reference) {
            Reference ref = (Reference) val;
            if (ref.isUnresolvableReference()) {
                return "undefined";
            }
            val = ref.GetValue(cx);
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
     * 11.6.1 The Addition operator ( + )
     */
    public static Object add(Object lval, Object rval, ExecutionContext cx) {
        Object lprim = ToPrimitive(cx, lval, null);
        Object rprim = ToPrimitive(cx, rval, null);
        if (Type.isString(lprim) || Type.isString(rprim)) {
            CharSequence lstr = ToString(cx, lprim);
            CharSequence rstr = ToString(cx, rprim);
            return add(lstr, rstr, cx);
        }
        return ToNumber(cx, lprim) + ToNumber(cx, rprim);
    }

    /**
     * 11.6.1 The Addition operator ( + )
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
            throwInternalError(cx, Messages.Key.OutOfMemory);
        }
        if (newlen <= 10) {
            return new StringBuilder(newlen).append(lstr).append(rstr).toString();
        }
        return new ConsString(lstr, rstr);
    }

    /**
     * 11.8.1 The Abstract Relational Comparison Algorithm
     */
    public static int relationalComparison(Object x, Object y, boolean leftFirst,
            ExecutionContext cx) {
        // true -> 1
        // false -> 0
        // undefined -> -1
        Object px, py;
        if (leftFirst) {
            px = ToPrimitive(cx, x, Type.Number);
            py = ToPrimitive(cx, y, Type.Number);
        } else {
            py = ToPrimitive(cx, y, Type.Number);
            px = ToPrimitive(cx, x, Type.Number);
        }
        if (!(Type.isString(px) && Type.isString(py))) {
            double nx = ToNumber(cx, px);
            double ny = ToNumber(cx, py);
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
    public static boolean in(Object lval, Object rval, ExecutionContext cx) {
        if (!Type.isObject(rval)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        Object p = ToPropertyKey(cx, lval);
        if (p instanceof String) {
            return HasProperty(cx, Type.objectValue(rval), (String) p);
        } else {
            return HasProperty(cx, Type.objectValue(rval), (Symbol) p);
        }
    }

    /**
     * 11.8.1 Runtime Semantics<br>
     * Runtime Semantics: Evaluation
     */
    public static boolean instanceOfOperator(Object obj, Object constructor, ExecutionContext cx) {
        if (!Type.isObject(constructor)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        Callable instOfHandler = GetMethod(cx, Type.objectValue(constructor),
                BuiltinSymbol.hasInstance.get());
        if (instOfHandler != null) {
            Object result = instOfHandler.call(cx, constructor, obj);
            // FIXME: spec bug (missing ToBoolean) (Bug 1418)
            return ToBoolean(result);
        }
        if (!IsCallable(constructor)) {
            throw throwTypeError(cx, Messages.Key.NotCallable);
        }
        return OrdinaryHasInstance(cx, constructor, obj);
    }

    /**
     * 11.9.1 The Abstract Equality Comparison Algorithm
     */
    public static boolean equalityComparison(Object x, Object y, ExecutionContext cx) {
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
            return Type.numberValue(x) == ToNumber(cx, y);
        }
        if (tx == Type.String && ty == Type.Number) {
            // return equalityComparison(realm, ToNumber(realm, x), y);
            return ToNumber(cx, x) == Type.numberValue(y);
        }
        if (tx == Type.Boolean) {
            return equalityComparison(ToNumber(cx, x), y, cx);
        }
        if (ty == Type.Boolean) {
            return equalityComparison(x, ToNumber(cx, y), cx);
        }
        if ((tx == Type.String || tx == Type.Number) && ty == Type.Object) {
            return equalityComparison(x, ToPrimitive(cx, y, null), cx);
        }
        if (tx == Type.Object && (ty == Type.String || ty == Type.Number)) {
            return equalityComparison(ToPrimitive(cx, x, null), y, cx);
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
        assert tx == Type.Object || tx == Type.Symbol;
        return (x == y);
    }

    /**
     * 12.13 The throw Statement
     */
    public static ScriptException _throw(Object val) {
        if (val instanceof ErrorObject) {
            throw ((ErrorObject) val).getException();
        }
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
        ScriptObject g = ToObject(cx, expr);
        try {
            while (true) {
                Object next;
                if (send) {
                    next = Invoke(cx, g, "send", received);
                } else {
                    next = Invoke(cx, g, "throw", received);
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
            if (!IteratorComplete(realm, e)) {
                throw e;
            }
            // TODO: StopIteration with value
            // result = ((StopIteration) e.getValue()).getValue();
            result = UNDEFINED;
        } finally {
            try {
                Invoke(cx, g, "close");
            } catch (ScriptException ignore) {
            }
        }
        return result;
    }

    public static Object RegExp(ExecutionContext cx, String re, String flags) {
        // FIXME: spec bug (call abstract operation RegExpCreate?!) (bug 749)
        Constructor ctor = (Constructor) cx.getIntrinsic(Intrinsics.RegExp);
        return ctor.construct(cx, re, flags);
    }

    /**
     * Helper function
     */
    public static Iterator<?> enumerate(Object o, ExecutionContext cx) {
        ScriptObject obj = ToObject(cx, o);
        return FromListIterator(cx, obj.enumerate(cx));
    }

    /**
     * Helper function
     */
    public static Iterator<?> iterate(Object o, ExecutionContext cx) {
        ScriptObject obj = ToObject(cx, o);
        Object keys = Invoke(cx, obj, BuiltinSymbol.iterator.get());
        return FromListIterator(cx, keys);
    }

    /**
     * 12.2.4 Destructuring Binding Patterns
     * <p>
     * Runtime Semantics: Indexed Binding Initialisation<br>
     * BindingRestElement : ... BindingIdentifier
     */
    public static ScriptObject createRestArray(ScriptObject array, int index, ExecutionContext cx) {
        Object lenVal = Get(cx, array, "length");
        long arrayLength = ToUint32(cx, lenVal);
        ScriptObject result = ExoticArray.ArrayCreate(cx, 0);
        long n = 0;
        while (index < arrayLength) {
            String p = ToString(index);
            boolean exists = HasProperty(cx, array, p);
            // TODO: assert exists iff FunctionRestParameter
            if (exists) {
                Object v = Get(cx, array, p);
                PropertyDescriptor desc = new PropertyDescriptor(v, true, true, true);
                result.defineOwnProperty(cx, ToString(n), desc);
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
    public static Reference getProperty(Object baseValue, String propertyNameString,
            ExecutionContext cx, boolean strict) {
        /* step 1-6 (generated code) */
        /* step 7 */
        CheckObjectCoercible(cx, baseValue);
        /* step 8-10 */
        return new Reference.PropertyNameReference(baseValue, propertyNameString, strict);
    }

    /**
     * 11.2.1 Property Accessors
     * <p>
     * Runtime Semantics: Evaluation<br>
     * MemberExpression : MemberExpression [ Expression ]
     */
    public static Reference getElement(Object baseValue, Object propertyNameValue,
            ExecutionContext cx, boolean strict) {
        /* step 1-6 (generated code) */
        /* step 7 */
        CheckObjectCoercible(cx, baseValue);
        /* step 8 */
        Object propertyKey = ToPropertyKey(cx, propertyNameValue);
        /* step 9-10 */
        if (propertyKey instanceof String) {
            return new Reference.PropertyNameReference(baseValue, (String) propertyKey, strict);
        }
        return new Reference.PropertySymbolReference(baseValue, (Symbol) propertyKey, strict);
    }

    /**
     * 11.1.5 Object Initialiser
     * <p>
     * Runtime Semantics: Property Definition Evaluation
     */
    public static void defineProperty(ScriptObject object, String propertyName, Object value,
            ExecutionContext cx) {
        DefinePropertyOrThrow(cx, object, propertyName, new PropertyDescriptor(value, true, true,
                true));
    }

    /**
     * B.3.1.3 __proto___ Object Initialisers
     */
    public static void defineProtoProperty(ScriptObject object, Object value, ExecutionContext cx) {
        // use Put() to comply with current SpiderMonkey/JSC behaviour
        Put(cx, object, "__proto__", value, true);
    }

    /**
     * 11.1.4.1 Array Literal
     * <p>
     * Runtime Semantics: Array Accumulation
     */
    public static void defineProperty(ScriptObject array, int nextIndex, Object value,
            ExecutionContext cx) {
        // String propertyName = ToString(ToUint32(nextIndex));
        String propertyName = ToString(nextIndex);
        array.defineOwnProperty(cx, propertyName, new PropertyDescriptor(value, true, true, true));
    }

    /**
     * 11.1.4.1 Array Literal
     * <p>
     * Runtime Semantics: Array Accumulation<br>
     * Runtime Semantics: Evaluation
     */
    public static int ArrayAccumulationSpreadElement(ScriptObject array, int nextIndex,
            Object spreadValue, ExecutionContext cx) {
        /* step 1-2 (cf. generated code) */
        /* step 3-4 */
        ScriptObject spreadObj = ToObject(cx, spreadValue);
        /* step 5 */
        Object lenVal = Get(cx, spreadObj, "length");
        /* step 6-7 */
        long spreadLen = ToUint32(cx, lenVal);
        /* step 8-9 */
        for (long n = 0; n < spreadLen; ++n, ++nextIndex) {
            boolean exists = HasProperty(cx, spreadObj, ToString(n));
            if (exists) {
                Object v = spreadObj.get(cx, ToString(n), spreadObj);
                defineProperty(array, nextIndex, v, cx);
            }
        }
        return nextIndex;
    }
}
