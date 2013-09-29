/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.ExecutionContext.newScriptExecutionContext;

import java.text.Collator;
import java.text.DecimalFormatSymbols;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.modules.Loader;
import com.github.anba.es6draft.runtime.objects.*;
import com.github.anba.es6draft.runtime.objects.NativeErrorConstructor.ErrorType;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferPrototype;
import com.github.anba.es6draft.runtime.objects.binary.DataViewConstructor;
import com.github.anba.es6draft.runtime.objects.binary.DataViewPrototype;
import com.github.anba.es6draft.runtime.objects.binary.ElementType;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayConstructor;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayConstructorPrototype;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayPrototype;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayPrototypePrototype;
import com.github.anba.es6draft.runtime.objects.collection.*;
import com.github.anba.es6draft.runtime.objects.internal.ListIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.intl.CollatorConstructor;
import com.github.anba.es6draft.runtime.objects.intl.CollatorPrototype;
import com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatConstructor;
import com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatPrototype;
import com.github.anba.es6draft.runtime.objects.intl.IntlObject;
import com.github.anba.es6draft.runtime.objects.intl.NumberFormatConstructor;
import com.github.anba.es6draft.runtime.objects.intl.NumberFormatPrototype;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorFunctionConstructor;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorFunctionPrototype;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorPrototype;
import com.github.anba.es6draft.runtime.objects.reflect.ProxyConstructor;
import com.github.anba.es6draft.runtime.objects.reflect.Reflect;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;

/**
 * <h1>8 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>8.2 Code Realms
 * </ul>
 */
public final class Realm {
    /**
     * [[intrinsics]]
     */
    private Map<Intrinsics, ScriptObject> intrinsics = new EnumMap<>(Intrinsics.class);

    /**
     * [[globalThis]]
     */
    private GlobalObject globalThis;

    /**
     * [[globalEnv]]
     */
    private LexicalEnvironment globalEnv;

    /**
     * [[loader]]
     */
    private Loader loader;

    /**
     * [[ThrowTypeError]]
     */
    private Callable throwTypeError;

    private Callable builtinEval;

    private ExecutionContext defaultContext;

    private Set<CompatibilityOption> options;

    private Locale locale = Locale.getDefault();
    private TimeZone timezone = TimeZone.getDefault();
    private Messages messages = Messages.create(locale);

    // TODO: move into function source object
    private Map<String, ScriptObject> templateCallSites = new HashMap<>();

    private Realm() {
    }

    /**
     * [[intrinsics]]
     */
    public ScriptObject getIntrinsic(Intrinsics id) {
        return intrinsics.get(id);
    }

    /**
     * [[globalThis]]
     */
    public GlobalObject getGlobalThis() {
        return globalThis;
    }

    /**
     * [[globalEnv]]
     */
    public LexicalEnvironment getGlobalEnv() {
        return globalEnv;
    }

    /**
     * [[loader]]
     */
    public Loader getLoader() {
        return loader;
    }

    /**
     * [[ThrowTypeError]]
     */
    public Callable getThrowTypeError() {
        assert throwTypeError != null : "throwTypeError not yet initialised";
        return throwTypeError;
    }

    /**
     * Returns the default execution context for this realm
     */
    public ExecutionContext defaultContext() {
        return defaultContext;
    }

    private AtomicInteger evalCounter = new AtomicInteger(0);
    private AtomicInteger functionCounter = new AtomicInteger(0);

    /**
     * Next class name for eval scripts
     * 
     * @see Eval
     */
    public String nextEvalName() {
        return "Eval_" + evalCounter.incrementAndGet();
    }

    /**
     * Next class name for functions
     * 
     * @see FunctionConstructor
     * @see GeneratorFunctionConstructor
     */
    public String nextFunctionName() {
        return "Function_" + functionCounter.incrementAndGet();
    }

    /**
     * Returns this realm's locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns this realm's timezone
     */
    public TimeZone getTimezone() {
        return timezone;
    }

    /**
     * Returns the localised message for {@code key}
     */
    public String message(Messages.Key key) {
        return messages.getString(key);
    }

    /**
     * Returns a reference to the built-in <code>eval</code> function
     */
    public Callable getBuiltinEval() {
        return builtinEval;
    }

    /**
     * Returns the compatibility options for this realm instance
     */
    public Set<CompatibilityOption> getOptions() {
        return options;
    }

    /**
     * Tests whether the requested compatibility option is enabled in this code realm
     */
    public boolean isEnabled(CompatibilityOption option) {
        return options.contains(option);
    }

    /**
     * Returns the template call-site object for {@code key}
     */
    public ScriptObject getTemplateCallSite(String key) {
        return templateCallSites.get(key);
    }

    /**
     * Stores the template call-site object
     */
    public void addTemplateCallSite(String key, ScriptObject callSite) {
        templateCallSites.put(key, callSite);
    }

    /**
     * Returns a {@link Collator} for this realm's locale
     * 
     * @deprecated No longer used
     */
    @Deprecated
    public Collator getCollator() {
        Collator collator = Collator.getInstance(locale);
        // Use Normalised Form D for comparison (cf. 21.1.3.10, Note 2)
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        // `"\u0001".localeCompare("\u0002") == -1` should yield true
        collator.setStrength(Collator.IDENTICAL);
        return collator;
    }

    /**
     * Returns the locale specific list separator
     */
    public String getListSeparator() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        return symbols.getDecimalSeparator() == ',' ? ";" : ",";
    }

    private static final ObjectAllocator<GlobalObject> DEFAULT_GLOBAL_OBJECT = new ObjectAllocator<GlobalObject>() {
        @Override
        public GlobalObject newInstance(Realm realm) {
            return new GlobalObject(realm);
        }
    };

    /**
     * Creates a new {@link Realm} object with the default settings
     */
    public static Realm newRealm() {
        return newRealm(DEFAULT_GLOBAL_OBJECT, CompatibilityOption.WebCompatibility());
    }

    /**
     * Creates a new {@link Realm} object
     */
    public static Realm newRealm(ObjectAllocator<? extends GlobalObject> allocator,
            Set<CompatibilityOption> options) {
        Realm realm = new Realm();
        GlobalObject globalThis = allocator.newInstance(realm);
        ExecutionContext defaultContext = newScriptExecutionContext(realm);
        GlobalEnvironmentRecord envRec = new GlobalEnvironmentRecord(defaultContext, globalThis);
        LexicalEnvironment globalEnv = new LexicalEnvironment(defaultContext, envRec);

        //
        realm.globalThis = globalThis;
        realm.globalEnv = globalEnv;
        realm.defaultContext = defaultContext;
        realm.options = EnumSet.copyOf(options);

        // intrinsics: 19, 20, 21, 22.1, 24.3
        initialiseFundamentalObjects(realm);
        initialiseStandardObjects(realm);
        initialiseNativeErrors(realm);
        initialiseInternalObjects(realm);

        // intrinsics: 22.2, 23, 24.1, 24.2, 25
        initialiseBinaryModule(realm);
        initialiseCollectionModule(realm);
        initialiseReflectModule(realm);
        initialiseIterationModule(realm);

        // intrinsics: Internationalization API
        initialiseInternationalisation(realm);

        // finish initialising global object
        globalThis.initialise(defaultContext);

        // store reference to built-in eval
        realm.builtinEval = (Callable) Get(defaultContext, globalThis, "eval");

        return realm;
    }

    /**
     * <h1>19.1 Object Objects - 19.2 Function Objects</h1>
     * 
     * Fundamental built-in objects which must be initialised early
     */
    private static void initialiseFundamentalObjects(Realm realm) {
        Map<Intrinsics, ScriptObject> intrinsics = realm.intrinsics;
        ExecutionContext defaultContext = realm.defaultContext;

        // allocation phase
        ObjectConstructor objectConstructor = new ObjectConstructor(realm);
        ObjectPrototype objectPrototype = new ObjectPrototype(realm);
        FunctionConstructor functionConstructor = new FunctionConstructor(realm);
        FunctionPrototype functionPrototype = new FunctionPrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.Object, objectConstructor);
        intrinsics.put(Intrinsics.ObjectPrototype, objectPrototype);
        intrinsics.put(Intrinsics.Function, functionConstructor);
        intrinsics.put(Intrinsics.FunctionPrototype, functionPrototype);

        // create [[ThrowTypeError]] unique function (needs to be done before init'ing intrinsics)
        realm.throwTypeError = OrdinaryFunction.createThrowTypeError(defaultContext);

        // initialisation phase
        objectConstructor.initialise(defaultContext);
        objectPrototype.initialise(defaultContext);
        functionConstructor.initialise(defaultContext);
        functionPrototype.initialise(defaultContext);

        // Object.prototype.toString is also an intrinsic
        Object objectPrototypeToString = Get(defaultContext, objectPrototype, "toString");
        intrinsics.put(Intrinsics.ObjProto_toString, (ScriptObject) objectPrototypeToString);
    }

    /**
     * <h1>19.3, 19.4, 20, 21, 22.1, 24.3</h1>
     * 
     * Standard built-in objects
     */
    private static void initialiseStandardObjects(Realm realm) {
        Map<Intrinsics, ScriptObject> intrinsics = realm.intrinsics;
        ExecutionContext defaultContext = realm.defaultContext;

        // allocation phase
        ArrayConstructor arrayConstructor = new ArrayConstructor(realm);
        ArrayPrototype arrayPrototype = new ArrayPrototype(realm);
        ArrayIteratorPrototype arrayIteratorPrototype = new ArrayIteratorPrototype(realm);
        StringConstructor stringConstructor = new StringConstructor(realm);
        StringPrototype stringPrototype = new StringPrototype(realm);
        BooleanConstructor booleanConstructor = new BooleanConstructor(realm);
        BooleanPrototype booleanPrototype = new BooleanPrototype(realm);
        NumberConstructor numberConstructor = new NumberConstructor(realm);
        NumberPrototype numberPrototype = new NumberPrototype(realm);
        MathObject mathObject = new MathObject(realm);
        DateConstructor dateConstructor = new DateConstructor(realm);
        DatePrototype datePrototype = new DatePrototype(realm);
        RegExpConstructor regExpConstructor = new RegExpConstructor(realm);
        RegExpPrototype regExpPrototype = new RegExpPrototype(realm);
        ErrorConstructor errorConstructor = new ErrorConstructor(realm);
        ErrorPrototype errorPrototype = new ErrorPrototype(realm);
        JSONObject jsonObject = new JSONObject(realm);

        // registration phase
        intrinsics.put(Intrinsics.Array, arrayConstructor);
        intrinsics.put(Intrinsics.ArrayPrototype, arrayPrototype);
        intrinsics.put(Intrinsics.ArrayIteratorPrototype, arrayIteratorPrototype);
        intrinsics.put(Intrinsics.String, stringConstructor);
        intrinsics.put(Intrinsics.StringPrototype, stringPrototype);
        intrinsics.put(Intrinsics.Boolean, booleanConstructor);
        intrinsics.put(Intrinsics.BooleanPrototype, booleanPrototype);
        intrinsics.put(Intrinsics.Number, numberConstructor);
        intrinsics.put(Intrinsics.NumberPrototype, numberPrototype);
        intrinsics.put(Intrinsics.Math, mathObject);
        intrinsics.put(Intrinsics.Date, dateConstructor);
        intrinsics.put(Intrinsics.DatePrototype, datePrototype);
        intrinsics.put(Intrinsics.RegExp, regExpConstructor);
        intrinsics.put(Intrinsics.RegExpPrototype, regExpPrototype);
        intrinsics.put(Intrinsics.Error, errorConstructor);
        intrinsics.put(Intrinsics.ErrorPrototype, errorPrototype);
        intrinsics.put(Intrinsics.JSON, jsonObject);

        // initialisation phase
        arrayConstructor.initialise(defaultContext);
        arrayPrototype.initialise(defaultContext);
        arrayIteratorPrototype.initialise(defaultContext);
        stringConstructor.initialise(defaultContext);
        stringPrototype.initialise(defaultContext);
        booleanConstructor.initialise(defaultContext);
        booleanPrototype.initialise(defaultContext);
        numberConstructor.initialise(defaultContext);
        numberPrototype.initialise(defaultContext);
        mathObject.initialise(defaultContext);
        dateConstructor.initialise(defaultContext);
        datePrototype.initialise(defaultContext);
        regExpConstructor.initialise(defaultContext);
        regExpPrototype.initialise(defaultContext);
        errorConstructor.initialise(defaultContext);
        errorPrototype.initialise(defaultContext);
        jsonObject.initialise(defaultContext);
    }

    /**
     * <h1>19.4.5 Native Error Types Used in This Standard</h1>
     * 
     * Native Error built-in objects
     */
    private static void initialiseNativeErrors(Realm realm) {
        Map<Intrinsics, ScriptObject> intrinsics = realm.intrinsics;
        ExecutionContext defaultContext = realm.defaultContext;

        // allocation phase
        NativeErrorConstructor evalErrorConstructor = new NativeErrorConstructor(realm,
                ErrorType.EvalError);
        NativeErrorPrototype evalErrorPrototype = new NativeErrorPrototype(realm,
                ErrorType.EvalError);
        NativeErrorConstructor rangeErrorConstructor = new NativeErrorConstructor(realm,
                ErrorType.RangeError);
        NativeErrorPrototype rangeErrorPrototype = new NativeErrorPrototype(realm,
                ErrorType.RangeError);
        NativeErrorConstructor referenceErrorConstructor = new NativeErrorConstructor(realm,
                ErrorType.ReferenceError);
        NativeErrorPrototype referenceErrorPrototype = new NativeErrorPrototype(realm,
                ErrorType.ReferenceError);
        NativeErrorConstructor syntaxErrorConstructor = new NativeErrorConstructor(realm,
                ErrorType.SyntaxError);
        NativeErrorPrototype syntaxErrorPrototype = new NativeErrorPrototype(realm,
                ErrorType.SyntaxError);
        NativeErrorConstructor typeErrorConstructor = new NativeErrorConstructor(realm,
                ErrorType.TypeError);
        NativeErrorPrototype typeErrorPrototype = new NativeErrorPrototype(realm,
                ErrorType.TypeError);
        NativeErrorConstructor uriErrorConstructor = new NativeErrorConstructor(realm,
                ErrorType.URIError);
        NativeErrorPrototype uriErrorPrototype = new NativeErrorPrototype(realm, ErrorType.URIError);
        NativeErrorConstructor internalErrorConstructor = new NativeErrorConstructor(realm,
                ErrorType.InternalError);
        NativeErrorPrototype internalErrorPrototype = new NativeErrorPrototype(realm,
                ErrorType.InternalError);

        // registration phase
        intrinsics.put(Intrinsics.EvalError, evalErrorConstructor);
        intrinsics.put(Intrinsics.EvalErrorPrototype, evalErrorPrototype);
        intrinsics.put(Intrinsics.RangeError, rangeErrorConstructor);
        intrinsics.put(Intrinsics.RangeErrorPrototype, rangeErrorPrototype);
        intrinsics.put(Intrinsics.ReferenceError, referenceErrorConstructor);
        intrinsics.put(Intrinsics.ReferenceErrorPrototype, referenceErrorPrototype);
        intrinsics.put(Intrinsics.SyntaxError, syntaxErrorConstructor);
        intrinsics.put(Intrinsics.SyntaxErrorPrototype, syntaxErrorPrototype);
        intrinsics.put(Intrinsics.TypeError, typeErrorConstructor);
        intrinsics.put(Intrinsics.TypeErrorPrototype, typeErrorPrototype);
        intrinsics.put(Intrinsics.URIError, uriErrorConstructor);
        intrinsics.put(Intrinsics.URIErrorPrototype, uriErrorPrototype);
        intrinsics.put(Intrinsics.InternalError, internalErrorConstructor);
        intrinsics.put(Intrinsics.InternalErrorPrototype, internalErrorPrototype);

        // initialisation phase
        evalErrorConstructor.initialise(defaultContext);
        evalErrorPrototype.initialise(defaultContext);
        rangeErrorConstructor.initialise(defaultContext);
        rangeErrorPrototype.initialise(defaultContext);
        referenceErrorConstructor.initialise(defaultContext);
        referenceErrorPrototype.initialise(defaultContext);
        syntaxErrorConstructor.initialise(defaultContext);
        syntaxErrorPrototype.initialise(defaultContext);
        typeErrorConstructor.initialise(defaultContext);
        typeErrorPrototype.initialise(defaultContext);
        uriErrorConstructor.initialise(defaultContext);
        uriErrorPrototype.initialise(defaultContext);
        internalErrorConstructor.initialise(defaultContext);
        internalErrorPrototype.initialise(defaultContext);
    }

    /**
     * Additional internal built-in objects used in this implementation
     */
    private static void initialiseInternalObjects(Realm realm) {
        Map<Intrinsics, ScriptObject> intrinsics = realm.intrinsics;
        ExecutionContext defaultContext = realm.defaultContext;

        // allocation phase
        ListIteratorPrototype listIteratorPrototype = new ListIteratorPrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.ListIteratorPrototype, listIteratorPrototype);

        // initialisation phase
        listIteratorPrototype.initialise(defaultContext);
    }

    /**
     * <h1>23 Keyed Collection</h1>
     */
    private static void initialiseCollectionModule(Realm realm) {
        Map<Intrinsics, ScriptObject> intrinsics = realm.intrinsics;
        ExecutionContext defaultContext = realm.defaultContext;

        // allocation phase
        MapConstructor mapConstructor = new MapConstructor(realm);
        MapPrototype mapPrototype = new MapPrototype(realm);
        MapIteratorPrototype mapIteratorPrototype = new MapIteratorPrototype(realm);
        WeakMapConstructor weakMapConstructor = new WeakMapConstructor(realm);
        WeakMapPrototype weakMapPrototype = new WeakMapPrototype(realm);
        SetConstructor setConstructor = new SetConstructor(realm);
        SetPrototype setPrototype = new SetPrototype(realm);
        SetIteratorPrototype setIteratorPrototype = new SetIteratorPrototype(realm);
        WeakSetConstructor weakSetConstructor = new WeakSetConstructor(realm);
        WeakSetPrototype weakSetPrototype = new WeakSetPrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.Map, mapConstructor);
        intrinsics.put(Intrinsics.MapPrototype, mapPrototype);
        intrinsics.put(Intrinsics.MapIteratorPrototype, mapIteratorPrototype);
        intrinsics.put(Intrinsics.WeakMap, weakMapConstructor);
        intrinsics.put(Intrinsics.WeakMapPrototype, weakMapPrototype);
        intrinsics.put(Intrinsics.Set, setConstructor);
        intrinsics.put(Intrinsics.SetPrototype, setPrototype);
        intrinsics.put(Intrinsics.SetIteratorPrototype, setIteratorPrototype);
        intrinsics.put(Intrinsics.WeakSet, weakSetConstructor);
        intrinsics.put(Intrinsics.WeakSetPrototype, weakSetPrototype);

        // initialisation phase
        mapConstructor.initialise(defaultContext);
        mapPrototype.initialise(defaultContext);
        mapIteratorPrototype.initialise(defaultContext);
        weakMapConstructor.initialise(defaultContext);
        weakMapPrototype.initialise(defaultContext);
        setConstructor.initialise(defaultContext);
        setPrototype.initialise(defaultContext);
        setIteratorPrototype.initialise(defaultContext);
        weakSetConstructor.initialise(defaultContext);
        weakSetPrototype.initialise(defaultContext);
    }

    /**
     * <h1>26 The Reflect Module</h1>
     */
    private static void initialiseReflectModule(Realm realm) {
        Map<Intrinsics, ScriptObject> intrinsics = realm.intrinsics;
        ExecutionContext defaultContext = realm.defaultContext;

        // allocation phase
        ProxyConstructor proxyConstructor = new ProxyConstructor(realm);
        Reflect reflect = new Reflect(realm);

        // registration phase
        intrinsics.put(Intrinsics.Proxy, proxyConstructor);
        intrinsics.put(Intrinsics.Reflect, reflect);

        // initialisation phase
        proxyConstructor.initialise(defaultContext);
        reflect.initialise(defaultContext);
    }

    /**
     * <h1>25 The "std:iteration" Module</h1>
     */
    private static void initialiseIterationModule(Realm realm) {
        Map<Intrinsics, ScriptObject> intrinsics = realm.intrinsics;
        ExecutionContext defaultContext = realm.defaultContext;

        // allocation phase
        GeneratorFunctionConstructor generatorFunctionConstructor = new GeneratorFunctionConstructor(
                realm);
        GeneratorPrototype generatorPrototype = new GeneratorPrototype(realm);
        GeneratorFunctionPrototype generator = new GeneratorFunctionPrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.GeneratorFunction, generatorFunctionConstructor);
        intrinsics.put(Intrinsics.GeneratorPrototype, generatorPrototype);
        intrinsics.put(Intrinsics.Generator, generator);

        // initialisation phase
        generatorFunctionConstructor.initialise(defaultContext);
        generatorPrototype.initialise(defaultContext);
        generator.initialise(defaultContext);
    }

    /**
     * <h1>22.2 TypedArray Objects, 24.1 ArrayBuffer Objects, 24.2 DataView Objects</h1>
     */
    private static void initialiseBinaryModule(Realm realm) {
        Map<Intrinsics, ScriptObject> intrinsics = realm.intrinsics;
        ExecutionContext defaultContext = realm.defaultContext;

        // allocation phase
        ArrayBufferConstructor arrayBufferConstructor = new ArrayBufferConstructor(realm);
        ArrayBufferPrototype arrayBufferPrototype = new ArrayBufferPrototype(realm);
        TypedArrayConstructorPrototype typedArrayConstructor = new TypedArrayConstructorPrototype(
                realm);
        TypedArrayPrototypePrototype typedArrayPrototype = new TypedArrayPrototypePrototype(realm);
        TypedArrayConstructor int8ArrayConstructor = new TypedArrayConstructor(realm,
                ElementType.Int8);
        TypedArrayPrototype int8ArrayPrototype = new TypedArrayPrototype(realm, ElementType.Int8);
        TypedArrayConstructor uint8ArrayConstructor = new TypedArrayConstructor(realm,
                ElementType.Uint8);
        TypedArrayPrototype uint8ArrayPrototype = new TypedArrayPrototype(realm, ElementType.Uint8);
        TypedArrayConstructor uint8CArrayConstructor = new TypedArrayConstructor(realm,
                ElementType.Uint8C);
        TypedArrayPrototype uint8CArrayPrototype = new TypedArrayPrototype(realm,
                ElementType.Uint8C);
        TypedArrayConstructor int16ArrayConstructor = new TypedArrayConstructor(realm,
                ElementType.Int16);
        TypedArrayPrototype int16ArrayPrototype = new TypedArrayPrototype(realm, ElementType.Int16);
        TypedArrayConstructor uint16ArrayConstructor = new TypedArrayConstructor(realm,
                ElementType.Uint16);
        TypedArrayPrototype uint16ArrayPrototype = new TypedArrayPrototype(realm,
                ElementType.Uint16);
        TypedArrayConstructor int32ArrayConstructor = new TypedArrayConstructor(realm,
                ElementType.Int32);
        TypedArrayPrototype int32ArrayPrototype = new TypedArrayPrototype(realm, ElementType.Int32);
        TypedArrayConstructor uint32ArrayConstructor = new TypedArrayConstructor(realm,
                ElementType.Uint32);
        TypedArrayPrototype uint32ArrayPrototype = new TypedArrayPrototype(realm,
                ElementType.Uint32);
        TypedArrayConstructor float32ArrayConstructor = new TypedArrayConstructor(realm,
                ElementType.Float32);
        TypedArrayPrototype float32ArrayPrototype = new TypedArrayPrototype(realm,
                ElementType.Float32);
        TypedArrayConstructor float64ArrayConstructor = new TypedArrayConstructor(realm,
                ElementType.Float64);
        TypedArrayPrototype float64ArrayPrototype = new TypedArrayPrototype(realm,
                ElementType.Float64);
        DataViewConstructor dataViewConstructor = new DataViewConstructor(realm);
        DataViewPrototype dataViewPrototype = new DataViewPrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.ArrayBuffer, arrayBufferConstructor);
        intrinsics.put(Intrinsics.ArrayBufferPrototype, arrayBufferPrototype);
        intrinsics.put(Intrinsics.TypedArray, typedArrayConstructor);
        intrinsics.put(Intrinsics.TypedArrayPrototype, typedArrayPrototype);
        intrinsics.put(Intrinsics.Int8Array, int8ArrayConstructor);
        intrinsics.put(Intrinsics.Int8ArrayPrototype, int8ArrayPrototype);
        intrinsics.put(Intrinsics.Uint8Array, uint8ArrayConstructor);
        intrinsics.put(Intrinsics.Uint8ArrayPrototype, uint8ArrayPrototype);
        intrinsics.put(Intrinsics.Uint8ClampedArray, uint8CArrayConstructor);
        intrinsics.put(Intrinsics.Uint8ClampedArrayPrototype, uint8CArrayPrototype);
        intrinsics.put(Intrinsics.Int16Array, int16ArrayConstructor);
        intrinsics.put(Intrinsics.Int16ArrayPrototype, int16ArrayPrototype);
        intrinsics.put(Intrinsics.Uint16Array, uint16ArrayConstructor);
        intrinsics.put(Intrinsics.Uint16ArrayPrototype, uint16ArrayPrototype);
        intrinsics.put(Intrinsics.Int32Array, int32ArrayConstructor);
        intrinsics.put(Intrinsics.Int32ArrayPrototype, int32ArrayPrototype);
        intrinsics.put(Intrinsics.Uint32Array, uint32ArrayConstructor);
        intrinsics.put(Intrinsics.Uint32ArrayPrototype, uint32ArrayPrototype);
        intrinsics.put(Intrinsics.Float32Array, float32ArrayConstructor);
        intrinsics.put(Intrinsics.Float32ArrayPrototype, float32ArrayPrototype);
        intrinsics.put(Intrinsics.Float64Array, float64ArrayConstructor);
        intrinsics.put(Intrinsics.Float64ArrayPrototype, float64ArrayPrototype);
        intrinsics.put(Intrinsics.DataView, dataViewConstructor);
        intrinsics.put(Intrinsics.DataViewPrototype, dataViewPrototype);

        // initialisation phase
        arrayBufferConstructor.initialise(defaultContext);
        arrayBufferPrototype.initialise(defaultContext);
        typedArrayConstructor.initialise(defaultContext);
        typedArrayPrototype.initialise(defaultContext);
        int8ArrayConstructor.initialise(defaultContext);
        int8ArrayPrototype.initialise(defaultContext);
        uint8ArrayConstructor.initialise(defaultContext);
        uint8ArrayPrototype.initialise(defaultContext);
        uint8CArrayConstructor.initialise(defaultContext);
        uint8CArrayPrototype.initialise(defaultContext);
        int16ArrayConstructor.initialise(defaultContext);
        int16ArrayPrototype.initialise(defaultContext);
        uint16ArrayConstructor.initialise(defaultContext);
        uint16ArrayPrototype.initialise(defaultContext);
        int32ArrayConstructor.initialise(defaultContext);
        int32ArrayPrototype.initialise(defaultContext);
        uint32ArrayConstructor.initialise(defaultContext);
        uint32ArrayPrototype.initialise(defaultContext);
        float32ArrayConstructor.initialise(defaultContext);
        float32ArrayPrototype.initialise(defaultContext);
        float64ArrayConstructor.initialise(defaultContext);
        float64ArrayPrototype.initialise(defaultContext);
        dataViewConstructor.initialise(defaultContext);
        dataViewPrototype.initialise(defaultContext);
    }

    /**
     * <h1>Internationalisation API (ECMA-402)</h1><br>
     * <h2>8 The Intl Object - 12 DateTimeFormat Objects</h2>
     * 
     * Additional built-in objects from the Internationalisation API
     */
    private static void initialiseInternationalisation(Realm realm) {
        Map<Intrinsics, ScriptObject> intrinsics = realm.intrinsics;
        ExecutionContext defaultContext = realm.defaultContext;

        // allocation phase
        IntlObject intlObject = new IntlObject(realm);
        CollatorConstructor collatorConstructor = new CollatorConstructor(realm);
        CollatorPrototype collatorPrototype = new CollatorPrototype(realm);
        NumberFormatConstructor numberFormatConstructor = new NumberFormatConstructor(realm);
        NumberFormatPrototype numberFormatPrototype = new NumberFormatPrototype(realm);
        DateTimeFormatConstructor dateTimeFormatConstructor = new DateTimeFormatConstructor(realm);
        DateTimeFormatPrototype dateTimeFormatPrototype = new DateTimeFormatPrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.Intl, intlObject);
        intrinsics.put(Intrinsics.Intl_Collator, collatorConstructor);
        intrinsics.put(Intrinsics.Intl_CollatorPrototype, collatorPrototype);
        intrinsics.put(Intrinsics.Intl_NumberFormat, numberFormatConstructor);
        intrinsics.put(Intrinsics.Intl_NumberFormatPrototype, numberFormatPrototype);
        intrinsics.put(Intrinsics.Intl_DateTimeFormat, dateTimeFormatConstructor);
        intrinsics.put(Intrinsics.Intl_DateTimeFormatPrototype, dateTimeFormatPrototype);

        // initialisation phase
        intlObject.initialise(defaultContext);
        collatorConstructor.initialise(defaultContext);
        collatorPrototype.initialise(defaultContext);
        numberFormatConstructor.initialise(defaultContext);
        numberFormatPrototype.initialise(defaultContext);
        dateTimeFormatConstructor.initialise(defaultContext);
        dateTimeFormatPrototype.initialise(defaultContext);
    }
}
