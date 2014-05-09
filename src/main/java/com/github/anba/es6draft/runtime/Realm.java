/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.ExecutionContext.newScriptExecutionContext;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newGlobalEnvironment;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.security.SecureRandom;
import java.text.Collator;
import java.text.DecimalFormatSymbols;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
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
import com.github.anba.es6draft.runtime.objects.date.DateConstructor;
import com.github.anba.es6draft.runtime.objects.date.DatePrototype;
import com.github.anba.es6draft.runtime.objects.internal.ListIteratorNext;
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
import com.github.anba.es6draft.runtime.objects.number.MathObject;
import com.github.anba.es6draft.runtime.objects.number.NumberConstructor;
import com.github.anba.es6draft.runtime.objects.number.NumberPrototype;
import com.github.anba.es6draft.runtime.objects.promise.PromiseConstructor;
import com.github.anba.es6draft.runtime.objects.promise.PromisePrototype;
import com.github.anba.es6draft.runtime.objects.reflect.LoaderConstructor;
import com.github.anba.es6draft.runtime.objects.reflect.LoaderIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.reflect.LoaderPrototype;
import com.github.anba.es6draft.runtime.objects.reflect.ProxyConstructorFunction;
import com.github.anba.es6draft.runtime.objects.reflect.RealmConstructor;
import com.github.anba.es6draft.runtime.objects.reflect.RealmObject;
import com.github.anba.es6draft.runtime.objects.reflect.RealmPrototype;
import com.github.anba.es6draft.runtime.objects.reflect.Reflect;
import com.github.anba.es6draft.runtime.objects.reflect.SystemObject;
import com.github.anba.es6draft.runtime.objects.text.RegExpConstructor;
import com.github.anba.es6draft.runtime.objects.text.RegExpPrototype;
import com.github.anba.es6draft.runtime.objects.text.StringConstructor;
import com.github.anba.es6draft.runtime.objects.text.StringIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.text.StringPrototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

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
    private final EnumMap<Intrinsics, OrdinaryObject> intrinsics = new EnumMap<>(Intrinsics.class);

    /**
     * [[realmObject]]
     */
    private final RealmObject realmObject;

    /**
     * [[globalThis]]
     */
    private final ScriptObject globalThis;

    /**
     * [[globalEnv]]
     */
    private final LexicalEnvironment<GlobalEnvironmentRecord> globalEnv;

    /**
     * [[ThrowTypeError]]
     */
    private Callable throwTypeError;

    /**
     * [[directEvalTranslate]]
     */
    private Callable directEvalTranslate;

    /**
     * [[nonEvalFallback]]
     */
    private Callable nonEvalFallback;

    /**
     * [[indirectEval]]
     */
    private Callable indirectEval;

    private final Callable builtinEval;

    private ExecutionContext scriptContext;

    private final World<? extends GlobalObject> world;

    private final ExecutionContext defaultContext;

    private final GlobalObject globalObject;

    private final SecureRandom random = new SecureRandom();

    // TODO: move into function source object
    private final HashMap<String, ExoticArray> templateCallSites = new HashMap<>();

    private Realm(World<? extends GlobalObject> world) {
        this.world = world;
        this.defaultContext = newScriptExecutionContext(this, null);
        this.globalObject = world.getAllocator().newInstance(this);
        this.globalThis = world.getAllocator().newInstance(this); // TODO: yuk...
        this.globalEnv = newGlobalEnvironment(defaultContext, globalThis);
        this.realmObject = new RealmObject(this);

        // Create all built-in intrinsics
        createIntrinsics(this);

        // Initialize global object
        globalObject.initialize(defaultContext);

        // Store reference to built-in eval
        builtinEval = (Callable) Get(defaultContext, globalObject, "eval");

        // [[Prototype]] for default global is implementation defined
        globalThis.setPrototypeOf(defaultContext, getIntrinsic(Intrinsics.ObjectPrototype));

        // Set [[Prototype]] after intrinsics are initialized
        realmObject.setPrototype(getIntrinsic(Intrinsics.RealmPrototype));
        realmObject.setRealm(this);
    }

    private Realm(World<? extends GlobalObject> world, RealmObject realmObject) {
        this.world = world;
        this.defaultContext = newScriptExecutionContext(this, null);
        this.globalObject = world.getAllocator().newInstance(this);
        this.globalThis = ObjectCreate(defaultContext, (ScriptObject) null);
        this.globalEnv = newGlobalEnvironment(defaultContext, globalThis);
        this.realmObject = realmObject;

        // Create all built-in intrinsics
        createIntrinsics(this);

        // Initialize global object
        globalObject.initialize(defaultContext);

        // Store reference to built-in eval
        builtinEval = (Callable) Get(defaultContext, globalObject, "eval");

        // Set prototype to %ObjectPrototype%, cf. Reflect.Realm.[[Call]]
        globalThis.setPrototypeOf(defaultContext, getIntrinsic(Intrinsics.ObjectPrototype));
    }

    private Realm(World<? extends GlobalObject> world, RealmObject realmObject,
            ScriptObject globalThis) {
        this.world = world;
        this.defaultContext = newScriptExecutionContext(this, null);
        this.globalObject = world.getAllocator().newInstance(this);
        this.globalThis = globalThis;
        this.globalEnv = newGlobalEnvironment(defaultContext, globalThis);
        this.realmObject = realmObject;

        // Create all built-in intrinsics
        createIntrinsics(this);

        // Initialize global object
        globalObject.initialize(defaultContext);

        // Store reference to built-in eval
        builtinEval = (Callable) Get(defaultContext, globalObject, "eval");
    }

    /**
     * [[intrinsics]]
     * 
     * @param id
     *            the intrinsic identifier
     * @return the intrinsic object
     */
    public OrdinaryObject getIntrinsic(Intrinsics id) {
        return intrinsics.get(id);
    }

    /**
     * [[intrinsics]]
     * 
     * @param id
     *            the intrinsic identifier
     * @param intrinsic
     *            the intrinsic object
     */
    public void setIntrinsic(Intrinsics id, OrdinaryObject intrinsic) {
        intrinsics.put(id, intrinsic);
    }

    /**
     * [[realmObject]]
     * 
     * @return the realm object
     */
    public RealmObject getRealmObject() {
        return realmObject;
    }

    /**
     * [[globalThis]]
     * 
     * @return the global object
     */
    public ScriptObject getGlobalThis() {
        return globalThis;
    }

    /**
     * [[globalEnv]]
     * 
     * @return the global environment
     */
    public LexicalEnvironment<GlobalEnvironmentRecord> getGlobalEnv() {
        return globalEnv;
    }

    /**
     * [[ThrowTypeError]]
     * 
     * @return the global %ThrowTypeError% object
     */
    public Callable getThrowTypeError() {
        assert throwTypeError != null : "throwTypeError not yet initialized";
        return throwTypeError;
    }

    /**
     * [[directEvalTranslate]]
     * 
     * @return the user hook for direct eval calls
     */
    public Callable getDirectEvalTranslate() {
        return directEvalTranslate;
    }

    /**
     * [[nonEvalFallback]]
     * 
     * @return the user hook for direct eval fallback calls
     */
    public Callable getNonEvalFallback() {
        return nonEvalFallback;
    }

    /**
     * [[indirectEval]]
     * 
     * @return the user hook for indirect eval calls
     */
    public Callable getIndirectEval() {
        return indirectEval;
    }

    /**
     * Returns the {@link Random} for this realm.
     * 
     * @return the random object
     */
    public Random getRandom() {
        return random;
    }

    /**
     * Returns the {@link World} for this realm.
     * 
     * @return the world instance
     */
    public World<? extends GlobalObject> getWorld() {
        return world;
    }

    /**
     * Returns the {@link GlobalObject} for this realm.
     * 
     * @return the global object instance
     */
    public GlobalObject getGlobalObject() {
        return globalObject;
    }

    /**
     * Returns the default execution context for this realm.
     * 
     * @return the default execution context
     */
    public ExecutionContext defaultContext() {
        return defaultContext;
    }

    /**
     * Returns the current script execution context for this realm.
     * 
     * @return the current script execution context
     */
    public ExecutionContext getScriptContext() {
        return scriptContext;
    }

    /**
     * Sets a new script execution context for this realm.
     * 
     * @param scriptContext
     *            the new script execution context
     */
    public void setScriptContext(ExecutionContext scriptContext) {
        this.scriptContext = scriptContext;
    }

    /**
     * Returns this realm's locale.
     * 
     * @return the locale
     */
    public Locale getLocale() {
        return world.getLocale();
    }

    /**
     * Returns this realm's timezone.
     * 
     * @return the timezone
     */
    public TimeZone getTimezone() {
        return world.getTimezone();
    }

    /**
     * Returns the localised message for {@code key}.
     * 
     * @param key
     *            the message key
     * @return the localised message
     */
    public String message(Messages.Key key) {
        return world.message(key);
    }

    /**
     * Returns the localised message for {@code key}.
     * 
     * @param key
     *            the message key
     * @param args
     *            the message arguments
     * @return the localised message
     */
    public String message(Messages.Key key, String... args) {
        return world.message(key, args);
    }

    /**
     * Returns a reference to the built-in <code>eval</code> function.
     * 
     * @return the built-in eval function
     */
    public Callable getBuiltinEval() {
        return builtinEval;
    }

    /**
     * Returns the script loader.
     * 
     * @return the script loader
     */
    public ScriptLoader getScriptLoader() {
        return world.getScriptLoader();
    }

    /**
     * Tests whether the requested compatibility option is enabled in this code realm.
     * 
     * @param option
     *            the compatibility option
     * @return {@code true} if the compatibility option is enabled
     */
    public boolean isEnabled(CompatibilityOption option) {
        return world.isEnabled(option);
    }

    /**
     * Returns the global symbol registry.
     * 
     * @return the global symbol registry
     */
    public GlobalSymbolRegistry getSymbolRegistry() {
        return world.getSymbolRegistry();
    }

    /**
     * 8.4.1 EnqueueTask ( queueName, task, arguments) Abstract Operation
     * <p>
     * Enqueues {@code task} to the queue of pending script-tasks.
     * 
     * @param task
     *            the new script task
     */
    public void enqueueScriptTask(Task task) {
        world.enqueueScriptTask(task);
    }

    /**
     * 8.4.1 EnqueueTask ( queueName, task, arguments) Abstract Operation
     * <p>
     * Enqueues {@code task} to the queue of pending promise-tasks.
     * 
     * @param task
     *            the new promise task
     */
    public void enqueuePromiseTask(Task task) {
        world.enqueuePromiseTask(task);
    }

    /**
     * Returns the template call-site object for {@code key}.
     * 
     * @param key
     *            the template literal key
     * @return the call-site object
     */
    public ExoticArray getTemplateCallSite(String key) {
        return templateCallSites.get(key);
    }

    /**
     * Stores the template call-site object.
     * 
     * @param key
     *            the template literal key
     * @param callSite
     *            the call-site object
     */
    public void addTemplateCallSite(String key, ExoticArray callSite) {
        templateCallSites.put(key, callSite);
    }

    /**
     * Returns a {@link Collator} for this realm's locale
     * 
     * @deprecated No longer used
     * @return the locale specific collator
     */
    @Deprecated
    public Collator getCollator() {
        Collator collator = Collator.getInstance(getLocale());
        // Use Normalized Form D for comparison (cf. 21.1.3.10, Note 2)
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        // `"\u0001".localeCompare("\u0002") == -1` should yield true
        collator.setStrength(Collator.IDENTICAL);
        return collator;
    }

    /**
     * Returns the locale specific list separator.
     * 
     * @return the locale specific list separator
     */
    public String getListSeparator() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        return symbols.getDecimalSeparator() == ',' ? ";" : ",";
    }

    /**
     * Initializes the custom extension points.
     * 
     * @param directEvalTranslate
     *            the user hook for direct eval calls
     * @param nonEvalFallback
     *            the user hook for direct eval fallback calls
     * @param indirectEval
     *            the user hook for indirect eval calls
     */
    public void setExtensionHooks(Callable directEvalTranslate, Callable nonEvalFallback,
            Callable indirectEval) {
        this.directEvalTranslate = directEvalTranslate;
        this.nonEvalFallback = nonEvalFallback;
        this.indirectEval = indirectEval;
    }

    /**
     * 8.2.1 CreateRealm ( )
     * 
     * Creates a new {@link Realm} object.
     * 
     * @param cx
     *            the execution context
     * @param realmObject
     *            the realm object
     * @param globalThis
     *            the global this object or {@code null}
     * @return the new realm instance
     */
    public static Realm CreateRealm(ExecutionContext cx, RealmObject realmObject,
            ScriptObject globalThis) {
        World<? extends GlobalObject> world = cx.getRealm().getWorld();
        if (globalThis == null) {
            return new Realm(world, realmObject);
        } else {
            return new Realm(world, realmObject, globalThis);
        }
    }

    /**
     * Creates a new {@link Realm} object.
     * 
     * @param <GLOBAL>
     *            the global object type
     * @param world
     *            the world instance
     * @return the new realm instance
     */
    static <GLOBAL extends GlobalObject> Realm newRealm(World<GLOBAL> world) {
        return new Realm(world);
    }

    private static void createIntrinsics(Realm realm) {
        // intrinsics: 19, 20, 21, 22.1, 24.3
        initializeFundamentalObjects(realm);
        initializeStandardObjects(realm);
        initializeNativeErrors(realm);
        initializeInternalObjects(realm);

        // intrinsics: 22.2, 23, 24.1, 24.2, 25
        initializeBinaryModule(realm);
        initializeCollectionModule(realm);
        initializeReflectModule(realm);
        initializeIterationModule(realm);
        initializePromiseObjects(realm);

        // intrinsics: Internationalization API
        initializeInternationalisation(realm);
    }

    /**
     * <h1>19.1 Object Objects - 19.2 Function Objects</h1>
     * 
     * Fundamental built-in objects which must be initialized early
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeFundamentalObjects(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;
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

        // initialization phase
        objectConstructor.initialize(defaultContext);
        objectPrototype.initialize(defaultContext);
        functionConstructor.initialize(defaultContext);
        functionPrototype.initialize(defaultContext);

        // Object.prototype.toString is also an intrinsic
        Object objectPrototypeToString = Get(defaultContext, objectPrototype, "toString");
        intrinsics.put(Intrinsics.ObjProto_toString, (OrdinaryObject) objectPrototypeToString);
    }

    /**
     * <h1>19.3, 19.4, 19.5, 20, 21, 22.1, 24.3</h1>
     * 
     * Standard built-in objects
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeStandardObjects(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;
        ExecutionContext defaultContext = realm.defaultContext;

        // allocation phase
        ArrayConstructor arrayConstructor = new ArrayConstructor(realm);
        ArrayPrototype arrayPrototype = new ArrayPrototype(realm);
        ArrayIteratorPrototype arrayIteratorPrototype = new ArrayIteratorPrototype(realm);
        StringConstructor stringConstructor = new StringConstructor(realm);
        StringPrototype stringPrototype = new StringPrototype(realm);
        StringIteratorPrototype stringIteratorPrototype = new StringIteratorPrototype(realm);
        SymbolConstructor symbolConstructor = new SymbolConstructor(realm);
        SymbolPrototype symbolPrototype = new SymbolPrototype(realm);
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
        intrinsics.put(Intrinsics.StringIteratorPrototype, stringIteratorPrototype);
        intrinsics.put(Intrinsics.Symbol, symbolConstructor);
        intrinsics.put(Intrinsics.SymbolPrototype, symbolPrototype);
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

        // initialization phase
        arrayConstructor.initialize(defaultContext);
        arrayPrototype.initialize(defaultContext);
        arrayIteratorPrototype.initialize(defaultContext);
        stringConstructor.initialize(defaultContext);
        stringPrototype.initialize(defaultContext);
        stringIteratorPrototype.initialize(defaultContext);
        symbolConstructor.initialize(defaultContext);
        symbolPrototype.initialize(defaultContext);
        booleanConstructor.initialize(defaultContext);
        booleanPrototype.initialize(defaultContext);
        numberConstructor.initialize(defaultContext);
        numberPrototype.initialize(defaultContext);
        mathObject.initialize(defaultContext);
        dateConstructor.initialize(defaultContext);
        datePrototype.initialize(defaultContext);
        regExpConstructor.initialize(defaultContext);
        regExpPrototype.initialize(defaultContext);
        errorConstructor.initialize(defaultContext);
        errorPrototype.initialize(defaultContext);
        jsonObject.initialize(defaultContext);

        // Array.prototype.values is also an intrinsic
        Object arrayPrototypeValues = Get(defaultContext, arrayPrototype, "values");
        intrinsics.put(Intrinsics.ArrayProto_values, (OrdinaryObject) arrayPrototypeValues);
    }

    /**
     * <h1>19.4.5 Native Error Types Used in This Standard</h1>
     * 
     * Native Error built-in objects
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeNativeErrors(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;
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

        // initialization phase
        evalErrorConstructor.initialize(defaultContext);
        evalErrorPrototype.initialize(defaultContext);
        rangeErrorConstructor.initialize(defaultContext);
        rangeErrorPrototype.initialize(defaultContext);
        referenceErrorConstructor.initialize(defaultContext);
        referenceErrorPrototype.initialize(defaultContext);
        syntaxErrorConstructor.initialize(defaultContext);
        syntaxErrorPrototype.initialize(defaultContext);
        typeErrorConstructor.initialize(defaultContext);
        typeErrorPrototype.initialize(defaultContext);
        uriErrorConstructor.initialize(defaultContext);
        uriErrorPrototype.initialize(defaultContext);
        internalErrorConstructor.initialize(defaultContext);
        internalErrorPrototype.initialize(defaultContext);
    }

    /**
     * Additional internal built-in objects used in this implementation
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeInternalObjects(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;

        // intrinsic functions
        intrinsics.put(Intrinsics.ListIteratorNext, new ListIteratorNext(realm));
    }

    /**
     * <h1>23 Keyed Collection</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeCollectionModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;
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

        // initialization phase
        mapConstructor.initialize(defaultContext);
        mapPrototype.initialize(defaultContext);
        mapIteratorPrototype.initialize(defaultContext);
        weakMapConstructor.initialize(defaultContext);
        weakMapPrototype.initialize(defaultContext);
        setConstructor.initialize(defaultContext);
        setPrototype.initialize(defaultContext);
        setIteratorPrototype.initialize(defaultContext);
        weakSetConstructor.initialize(defaultContext);
        weakSetPrototype.initialize(defaultContext);
    }

    /**
     * <h1>26 Reflection</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeReflectModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;
        ExecutionContext defaultContext = realm.defaultContext;

        // allocation phase
        ProxyConstructorFunction proxy = new ProxyConstructorFunction(realm);
        Reflect reflect = new Reflect(realm);
        LoaderConstructor loaderConstructor = new LoaderConstructor(realm);
        LoaderPrototype loaderPrototype = new LoaderPrototype(realm);
        LoaderIteratorPrototype loaderIteratorPrototype = new LoaderIteratorPrototype(realm);
        RealmConstructor realmConstructor = new RealmConstructor(realm);
        RealmPrototype realmPrototype = new RealmPrototype(realm);
        SystemObject systemObject = new SystemObject(realm);

        // registration phase
        intrinsics.put(Intrinsics.Proxy, proxy);
        intrinsics.put(Intrinsics.Reflect, reflect);
        intrinsics.put(Intrinsics.Loader, loaderConstructor);
        intrinsics.put(Intrinsics.LoaderPrototype, loaderPrototype);
        intrinsics.put(Intrinsics.LoaderIteratorPrototype, loaderIteratorPrototype);
        intrinsics.put(Intrinsics.Realm, realmConstructor);
        intrinsics.put(Intrinsics.RealmPrototype, realmPrototype);
        intrinsics.put(Intrinsics.System, systemObject);

        // initialization phase
        proxy.initialize(defaultContext);
        reflect.initialize(defaultContext);
        loaderConstructor.initialize(defaultContext);
        loaderPrototype.initialize(defaultContext);
        loaderIteratorPrototype.initialize(defaultContext);
        realmConstructor.initialize(defaultContext);
        realmPrototype.initialize(defaultContext);
        systemObject.initialize(defaultContext);
    }

    /**
     * <h1>25 Control Abstraction Objects</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeIterationModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;
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

        // initialization phase
        generatorFunctionConstructor.initialize(defaultContext);
        generatorPrototype.initialize(defaultContext);
        generator.initialize(defaultContext);

        if (realm.isEnabled(CompatibilityOption.LegacyGenerator)) {
            OrdinaryObject legacyGeneratorPrototype = OrdinaryObject.ObjectCreate(defaultContext);
            intrinsics.put(Intrinsics.LegacyGeneratorPrototype, legacyGeneratorPrototype);
        }
    }

    /**
     * <h1>22.2 TypedArray Objects, 24.1 ArrayBuffer Objects, 24.2 DataView Objects</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeBinaryModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;
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

        // initialization phase
        arrayBufferConstructor.initialize(defaultContext);
        arrayBufferPrototype.initialize(defaultContext);
        typedArrayConstructor.initialize(defaultContext);
        typedArrayPrototype.initialize(defaultContext);
        int8ArrayConstructor.initialize(defaultContext);
        int8ArrayPrototype.initialize(defaultContext);
        uint8ArrayConstructor.initialize(defaultContext);
        uint8ArrayPrototype.initialize(defaultContext);
        uint8CArrayConstructor.initialize(defaultContext);
        uint8CArrayPrototype.initialize(defaultContext);
        int16ArrayConstructor.initialize(defaultContext);
        int16ArrayPrototype.initialize(defaultContext);
        uint16ArrayConstructor.initialize(defaultContext);
        uint16ArrayPrototype.initialize(defaultContext);
        int32ArrayConstructor.initialize(defaultContext);
        int32ArrayPrototype.initialize(defaultContext);
        uint32ArrayConstructor.initialize(defaultContext);
        uint32ArrayPrototype.initialize(defaultContext);
        float32ArrayConstructor.initialize(defaultContext);
        float32ArrayPrototype.initialize(defaultContext);
        float64ArrayConstructor.initialize(defaultContext);
        float64ArrayPrototype.initialize(defaultContext);
        dataViewConstructor.initialize(defaultContext);
        dataViewPrototype.initialize(defaultContext);
    }

    /**
     * <h1>25 Control Abstraction Objects</h1><br>
     * <h2>25.4 Promise Objects</h2>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializePromiseObjects(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;
        ExecutionContext defaultContext = realm.defaultContext;

        // allocation phase
        PromiseConstructor promiseConstructor = new PromiseConstructor(realm);
        PromisePrototype promisePrototype = new PromisePrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.Promise, promiseConstructor);
        intrinsics.put(Intrinsics.PromisePrototype, promisePrototype);

        // initialization phase
        promiseConstructor.initialize(defaultContext);
        promisePrototype.initialize(defaultContext);
    }

    /**
     * <h1>Internationalisation API (ECMA-402)</h1><br>
     * <h2>8 The Intl Object - 12 DateTimeFormat Objects</h2>
     * 
     * Additional built-in objects from the Internationalisation API
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeInternationalisation(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;
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

        // initialization phase
        intlObject.initialize(defaultContext);
        collatorConstructor.initialize(defaultContext);
        collatorPrototype.initialize(defaultContext);
        numberFormatConstructor.initialize(defaultContext);
        numberFormatPrototype.initialize(defaultContext);
        dateTimeFormatConstructor.initialize(defaultContext);
        dateTimeFormatPrototype.initialize(defaultContext);
    }
}
