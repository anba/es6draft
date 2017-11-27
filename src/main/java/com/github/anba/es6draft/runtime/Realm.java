/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;
import static com.github.anba.es6draft.runtime.ExecutionContext.newDefaultExecutionContext;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newGlobalEnvironment;
import static com.github.anba.es6draft.runtime.internal.Errors.newError;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.Collator;
import java.text.DecimalFormatSymbols;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Permission;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.objects.*;
import com.github.anba.es6draft.runtime.objects.NativeErrorConstructor.ErrorType;
import com.github.anba.es6draft.runtime.objects.async.AsyncFunctionConstructor;
import com.github.anba.es6draft.runtime.objects.async.AsyncFunctionPrototype;
import com.github.anba.es6draft.runtime.objects.async.iteration.AsyncFromSyncIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.async.iteration.AsyncGeneratorFunctionConstructor;
import com.github.anba.es6draft.runtime.objects.async.iteration.AsyncGeneratorFunctionPrototype;
import com.github.anba.es6draft.runtime.objects.async.iteration.AsyncGeneratorPrototype;
import com.github.anba.es6draft.runtime.objects.async.iteration.AsyncIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.atomics.AtomicsObject;
import com.github.anba.es6draft.runtime.objects.atomics.SharedArrayBufferConstructor;
import com.github.anba.es6draft.runtime.objects.atomics.SharedArrayBufferPrototype;
import com.github.anba.es6draft.runtime.objects.bigint.BigIntConstructor;
import com.github.anba.es6draft.runtime.objects.bigint.BigIntPrototype;
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
import com.github.anba.es6draft.runtime.objects.intl.*;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorFunctionConstructor;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorFunctionPrototype;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorPrototype;
import com.github.anba.es6draft.runtime.objects.iteration.IteratorPrototype;
import com.github.anba.es6draft.runtime.objects.number.MathObject;
import com.github.anba.es6draft.runtime.objects.number.NumberConstructor;
import com.github.anba.es6draft.runtime.objects.number.NumberPrototype;
import com.github.anba.es6draft.runtime.objects.observable.ObservableConstructor;
import com.github.anba.es6draft.runtime.objects.observable.ObservablePrototype;
import com.github.anba.es6draft.runtime.objects.observable.SubscriptionObserverPrototype;
import com.github.anba.es6draft.runtime.objects.observable.SubscriptionPrototype;
import com.github.anba.es6draft.runtime.objects.promise.PromiseConstructor;
import com.github.anba.es6draft.runtime.objects.promise.PromisePrototype;
import com.github.anba.es6draft.runtime.objects.reflect.LoaderConstructor;
import com.github.anba.es6draft.runtime.objects.reflect.LoaderPrototype;
import com.github.anba.es6draft.runtime.objects.reflect.ProxyConstructor;
import com.github.anba.es6draft.runtime.objects.reflect.RealmConstructor;
import com.github.anba.es6draft.runtime.objects.reflect.RealmObject;
import com.github.anba.es6draft.runtime.objects.reflect.RealmPrototype;
import com.github.anba.es6draft.runtime.objects.reflect.ReflectObject;
import com.github.anba.es6draft.runtime.objects.reflect.SystemObject;
import com.github.anba.es6draft.runtime.objects.reflect.WeakRefPrototype;
import com.github.anba.es6draft.runtime.objects.simd.*;
import com.github.anba.es6draft.runtime.objects.text.RegExpConstructor;
import com.github.anba.es6draft.runtime.objects.text.RegExpPrototype;
import com.github.anba.es6draft.runtime.objects.text.RegExpStringIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.text.StringConstructor;
import com.github.anba.es6draft.runtime.objects.text.StringIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.text.StringPrototype;
import com.github.anba.es6draft.runtime.objects.zone.ZoneConstructor;
import com.github.anba.es6draft.runtime.objects.zone.ZoneObject;
import com.github.anba.es6draft.runtime.objects.zone.ZonePrototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.runtime.types.builtins.TypeErrorThrower;

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
     * [[globalObject]]
     */
    private final ScriptObject globalObject;

    /**
     * [[globalThis]]
     */
    private final ScriptObject globalThis;

    /**
     * [[globalEnv]]
     */
    private final LexicalEnvironment<GlobalEnvironmentRecord> globalEnv;

    /**
     * [[templateMap]]
     */
    private final HashMap<String, ArrayObject> templateMap = new HashMap<>();

    /**
     * [[ThrowTypeError]]
     */
    private TypeErrorThrower throwTypeError;

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

    /**
     * [[CurrentZone]]
     */
    private ZoneObject currentZone;

    /**
     * [[HostDefined]]
     */
    private final RealmData realmData;

    private final Callable builtinEval;

    private final World world;

    private final ExecutionContext defaultContext;

    private final GlobalObject globalPropertiesObject;

    private final SecureRandom random = new SecureRandom();

    private final EnumSet<Permission> permissions = EnumSet.allOf(Permission.class);

    /**
     * Creates a new {@link Realm} object.
     * 
     * @param world
     *            the world instance
     */
    public Realm(World world) {
        this.world = world;
        this.defaultContext = newDefaultExecutionContext(this);
        this.globalPropertiesObject = new GlobalObject(this);
        this.globalObject = new GlobalObject(this);
        this.globalThis = globalObject;
        this.globalEnv = newGlobalEnvironment(defaultContext, globalObject, globalThis);
        this.realmObject = new RealmObject(this);

        // Create all built-in intrinsics
        CreateIntrinsics(this);

        // Store reference to built-in eval
        builtinEval = (Callable) intrinsics.get(Intrinsics.eval);

        // Expose realm when we finished constructing the internal state.
        realmData = world.getRuntimeContext().getRealmData().apply(this);

        // [[Prototype]] for default global is implementation-dependent
        globalObject.setPrototypeOf(defaultContext, getIntrinsic(Intrinsics.ObjectPrototype));

        // Set [[Prototype]] after intrinsics are initialized
        realmObject.setPrototypeOf(defaultContext, getIntrinsic(Intrinsics.RealmPrototype));
        realmObject.setRealm(this);
    }

    private Realm(World world, RealmObject realmObject, ScriptObject globalObj, ScriptObject thisValue) {
        this.world = world;
        this.defaultContext = newDefaultExecutionContext(this);
        this.globalPropertiesObject = new GlobalObject(this);
        this.globalObject = globalObj != null ? globalObj : ObjectCreate(defaultContext, (ScriptObject) null);
        this.globalThis = thisValue != null ? thisValue : globalObject;
        this.globalEnv = newGlobalEnvironment(defaultContext, globalObject, globalThis);
        this.realmObject = realmObject;

        // Create all built-in intrinsics
        CreateIntrinsics(this);

        // Store reference to built-in eval
        builtinEval = (Callable) intrinsics.get(Intrinsics.eval);

        // Expose realm when we finished constructing the internal state.
        realmData = world.getRuntimeContext().getRealmData().apply(this);

        if (globalObj == null) {
            // Set prototype to %ObjectPrototype%, cf. 8.2.3 SetRealmGlobalObject
            globalObject.setPrototypeOf(defaultContext, getIntrinsic(Intrinsics.ObjectPrototype));
        }

        realmObject.setRealm(this);
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
     * [[globalObject]]
     * 
     * @return the global object
     */
    public ScriptObject getGlobalObject() {
        return globalObject;
    }

    /**
     * [[globalThis]]
     * <p>
     * Short cut for: {@code getGlobalEnv().getEnvRec().getGlobalThisValue()}
     * 
     * @return the global this value
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
     * [[templateMap]]
     * 
     * @return the map of template string objects
     */
    public Map<String, ArrayObject> getTemplateMap() {
        return templateMap;
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
     * [[CurrentZone]]
     * 
     * @return the current zone
     */
    public ZoneObject getCurrentZone() {
        return currentZone;
    }

    /**
     * [[CurrentZone]]
     * 
     * @param currentZone
     *            the new zone
     */
    public void setCurrentZone(ZoneObject currentZone) {
        assert currentZone != null;
        this.currentZone = currentZone;
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
     * Checks if the given permission is granted.
     * 
     * @param permission
     *            the permission
     * @return {@code true} if the permission is granted
     */
    public boolean isGranted(Permission permission) {
        return permissions.contains(permission);
    }

    /**
     * Grants a permission.
     * 
     * @param permission
     *            the permission
     */
    public void grant(Permission permission) {
        permissions.add(permission);
    }

    /**
     * Revokes a permission.
     * 
     * @param permission
     *            the permission
     */
    public void revoke(Permission permission) {
        permissions.remove(permission);
    }

    /**
     * Returns the realm data for this realm.
     * 
     * @return the realm data
     */
    public RealmData getRealmData() {
        return realmData;
    }

    /**
     * Returns the {@link World} for this realm.
     * 
     * @return the world instance
     */
    public World getWorld() {
        return world;
    }

    /**
     * Returns the runtime context for this realm.
     * 
     * @return the runtime context
     */
    public RuntimeContext getRuntimeContext() {
        return world.getRuntimeContext();
    }

    /**
     * Returns the global properties object for this realm.
     * 
     * @return the global properties object
     */
    public OrdinaryObject getGlobalPropertiesObject() {
        return globalPropertiesObject;
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
     * Returns this realm's locale.
     * 
     * @return the locale
     */
    public Locale getLocale() {
        return getRuntimeContext().getLocale();
    }

    /**
     * Returns this realm's timezone.
     * 
     * @return the timezone
     */
    public TimeZone getTimeZone() {
        return getRuntimeContext().getTimeZone();
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
     * Returns the module loader.
     * 
     * @return the module loader
     */
    public ModuleLoader getModuleLoader() {
        return world.getModuleLoader();
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
     * Returns the global symbol registry.
     * 
     * @return the global symbol registry
     */
    public GlobalSymbolRegistry getSymbolRegistry() {
        return world.getSymbolRegistry();
    }

    /**
     * 8.4.1 EnqueueJob (queueName, job, arguments)
     * <p>
     * Enqueues {@code job} to the queue of pending script-jobs.
     * 
     * @param job
     *            the new script job
     */
    public void enqueueScriptJob(Job job) {
        world.enqueueScriptJob(job);
    }

    /**
     * 8.4.1 EnqueueJob (queueName, job, arguments)
     * <p>
     * Enqueues {@code job} to the queue of pending promise-jobs.
     * 
     * @param job
     *            the new promise job
     */
    public void enqueuePromiseJob(Job job) {
        world.enqueuePromiseJob(job);
    }

    /**
     * 8.4.1 EnqueueJob (queueName, job, arguments)
     * <p>
     * Enqueues {@code job} to the queue of pending finalizer-jobs.
     * 
     * @param job
     *            the new finalizer job
     */
    public void enqueueFinalizerJob(Job job) {
        world.enqueueFinalizerJob(job);
    }

    /**
     * 8.4.1 EnqueueJob (queueName, job, arguments)
     * <p>
     * Enqueues {@code job} to the queue of pending async-jobs.
     * 
     * @param job
     *            the new async job
     */
    public void enqueueAsyncJob(Job job) {
        world.enqueueAsyncJob(job);
    }

    /**
     * Enqueue a promise rejection reason to the global rejection list.
     * 
     * @param reason
     *            the promise rejection reason
     */
    public void enqueueUnhandledPromiseRejection(Object reason) {
        world.enqueueUnhandledPromiseRejection(reason);
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
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(getLocale());
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
    public void setExtensionHooks(Callable directEvalTranslate, Callable nonEvalFallback, Callable indirectEval) {
        this.directEvalTranslate = directEvalTranslate;
        this.nonEvalFallback = nonEvalFallback;
        this.indirectEval = indirectEval;
    }

    /**
     * Creates user-defined native global functions.
     * 
     * @param <T>
     *            the owner type
     * @param object
     *            the owner object instance
     * @param clazz
     *            the class which holds the properties
     * @return the owner object
     */
    public <T> T createGlobalProperties(T object, Class<T> clazz) {
        Properties.createProperties(defaultContext(), getGlobalObject(), object, clazz);
        return object;
    }

    /**
     * 8.2.1 CreateRealm ( )<br>
     * 8.2.3 SetRealmGlobalObject ( realmRec, globalObj, thisValue )
     * <p>
     * Creates a new {@link Realm} object.
     * 
     * @param cx
     *            the execution context
     * @param realmObject
     *            the realm object
     * @param globalObj
     *            the global object or {@code null}
     * @param thisValue
     *            the global this value or {@code null}
     * @return the new realm instance
     */
    public static Realm CreateRealmAndSetRealmGlobalObject(ExecutionContext cx, RealmObject realmObject,
            ScriptObject globalObj, ScriptObject thisValue) {
        World world = cx.getRealm().getWorld();
        Realm realm = new Realm(world, realmObject, globalObj, thisValue);

        // Run any initialization scripts, if required. But do _not_ install extensions!
        try {
            realm.getRealmData().initializeScripted();
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(cx);
        } catch (IOException e) {
            throw newError(cx, e.getMessage());
        }

        return realm;
    }

    /**
     * 8.2.4 SetDefaultGlobalBindings ( realmRec )
     * <p>
     * Initializes {@code [[globalObject]]} with the default properties of the Global Object.
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @return the global object
     */
    public static ScriptObject SetDefaultGlobalBindings(ExecutionContext cx, Realm realm) {
        /* step 1 */
        ScriptObject globalObject = realm.getGlobalObject();
        OrdinaryObject globalProperties = realm.getGlobalPropertiesObject();
        assert globalObject != null && globalProperties != null;
        /* step 2 */
        for (Object key : globalProperties.ownPropertyKeys(cx)) {
            Property prop = globalProperties.getOwnProperty(cx, key);
            if (prop != null) {
                PropertyDescriptor desc = prop.toPropertyDescriptor();
                DefinePropertyOrThrow(cx, globalObject, key, desc);
            }
        }
        /* step 3 */
        return globalObject;
    }

    /**
     * 8.5 InitializeHostDefinedRealm ( )
     * <p>
     * Initializes the global this with the default properties of the Global Object.
     * 
     * @param realm
     *            the realm instance
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public static void InitializeHostDefinedRealm(Realm realm)
            throws IOException, ParserException, CompilationException {
        /* steps 1-9 (not applicable) */
        /* step 10 */
        // Run initialization scripts before installing global bindings and extensions.
        realm.getRealmData().initializeScripted();
        SetDefaultGlobalBindings(realm.defaultContext(), realm);
        /* step 11 */
        realm.getRealmData().initializeExtensions();
    }

    /**
     * 8.5 InitializeHostDefinedRealm ( )
     * <p>
     * Creates and initializes a new Realm instance.
     * 
     * @param world
     *            the world instance
     * @return the new realm instance
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public static Realm InitializeHostDefinedRealm(World world)
            throws IOException, ParserException, CompilationException {
        /* steps 1-9 */
        Realm realm = new Realm(world);
        /* steps 10-11 */
        InitializeHostDefinedRealm(realm);
        return realm;
    }

    /**
     * 8.2.2 CreateIntrinsics ( realmRec )
     * 
     * @param realm
     *            the realm instance
     */
    private static void CreateIntrinsics(Realm realm) {
        /* steps 1-14 */
        initializeFundamentalObjects(realm);
        initializeStandardObjects(realm);
        initializeNativeErrors(realm);
        initializeBinaryModule(realm);
        initializeCollectionModule(realm);
        initializeReflectModule(realm);
        initializeIterationModule(realm);
        initializePromiseObjects(realm);
        initializeAsyncModule(realm);

        // intrinsics: Internationalization API
        initializeInternationalisation(realm);

        // intrinsics: Async generators
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.AsyncIteration)) {
            initializeAsyncIterationModule(realm);
        }

        // intrinsics: SIMD
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.SIMD)) {
            initializeSIMDModule(realm);
        }

        // intrinsics: Observable
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.Observable)) {
            initializeObservableModule(realm);
        }

        // intrinsics: Shared Memory and Atomics
        initializeAtomicsModule(realm);

        // intrinsics: Zones
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.Zones)) {
            initializeZonesModule(realm);
        }

        // intrinsics: BigInt
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.BigInt)) {
            initializeBigIntModule(realm);
        }

        // Initialized last because it accesses other intrinsics.
        initializeGlobalObject(realm);
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

        // Create [[ThrowTypeError]] function before initializing intrinsics.
        realm.throwTypeError = new TypeErrorThrower(realm);

        // Also stored in intrinsics table.
        intrinsics.put(Intrinsics.ThrowTypeError, realm.throwTypeError);

        // initialization phase
        objectConstructor.initialize(realm);
        objectPrototype.initialize(realm);
        functionConstructor.initialize(realm);
        functionPrototype.initialize(realm);

        AddRestrictedFunctionProperties(functionPrototype, realm);

        // Object.prototype.toString is also an intrinsic
        Object objectPrototypeToString = objectPrototype.lookupOwnProperty("toString").getValue();
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
        IteratorPrototype iteratorPrototype = new IteratorPrototype(realm);

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
        intrinsics.put(Intrinsics.IteratorPrototype, iteratorPrototype);

        // initialization phase
        arrayConstructor.initialize(realm);
        arrayPrototype.initialize(realm);
        arrayIteratorPrototype.initialize(realm);
        stringConstructor.initialize(realm);
        stringPrototype.initialize(realm);
        stringIteratorPrototype.initialize(realm);
        symbolConstructor.initialize(realm);
        symbolPrototype.initialize(realm);
        booleanConstructor.initialize(realm);
        booleanPrototype.initialize(realm);
        numberConstructor.initialize(realm);
        numberPrototype.initialize(realm);
        mathObject.initialize(realm);
        dateConstructor.initialize(realm);
        datePrototype.initialize(realm);
        regExpConstructor.initialize(realm);
        regExpPrototype.initialize(realm);
        errorConstructor.initialize(realm);
        errorPrototype.initialize(realm);
        jsonObject.initialize(realm);
        iteratorPrototype.initialize(realm);

        // Array.prototype.values is also an intrinsic
        Object arrayPrototypeValues = arrayPrototype.lookupOwnProperty("values").getValue();
        intrinsics.put(Intrinsics.ArrayProto_values, (OrdinaryObject) arrayPrototypeValues);

        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.StringMatchAll)) {
            RegExpStringIteratorPrototype regExpStringIteratorPrototype = new RegExpStringIteratorPrototype(realm);
            intrinsics.put(Intrinsics.RegExpStringIteratorPrototype, regExpStringIteratorPrototype);
            regExpStringIteratorPrototype.initialize(realm);
        }
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

        // allocation phase
        NativeErrorConstructor evalErrorConstructor = new NativeErrorConstructor(realm, ErrorType.EvalError);
        NativeErrorPrototype evalErrorPrototype = new NativeErrorPrototype(realm, ErrorType.EvalError);
        NativeErrorConstructor rangeErrorConstructor = new NativeErrorConstructor(realm, ErrorType.RangeError);
        NativeErrorPrototype rangeErrorPrototype = new NativeErrorPrototype(realm, ErrorType.RangeError);
        NativeErrorConstructor referenceErrorConstructor = new NativeErrorConstructor(realm, ErrorType.ReferenceError);
        NativeErrorPrototype referenceErrorPrototype = new NativeErrorPrototype(realm, ErrorType.ReferenceError);
        NativeErrorConstructor syntaxErrorConstructor = new NativeErrorConstructor(realm, ErrorType.SyntaxError);
        NativeErrorPrototype syntaxErrorPrototype = new NativeErrorPrototype(realm, ErrorType.SyntaxError);
        NativeErrorConstructor typeErrorConstructor = new NativeErrorConstructor(realm, ErrorType.TypeError);
        NativeErrorPrototype typeErrorPrototype = new NativeErrorPrototype(realm, ErrorType.TypeError);
        NativeErrorConstructor uriErrorConstructor = new NativeErrorConstructor(realm, ErrorType.URIError);
        NativeErrorPrototype uriErrorPrototype = new NativeErrorPrototype(realm, ErrorType.URIError);
        NativeErrorConstructor internalErrorConstructor = new NativeErrorConstructor(realm, ErrorType.InternalError);
        NativeErrorPrototype internalErrorPrototype = new NativeErrorPrototype(realm, ErrorType.InternalError);

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
        evalErrorConstructor.initialize(realm);
        evalErrorPrototype.initialize(realm);
        rangeErrorConstructor.initialize(realm);
        rangeErrorPrototype.initialize(realm);
        referenceErrorConstructor.initialize(realm);
        referenceErrorPrototype.initialize(realm);
        syntaxErrorConstructor.initialize(realm);
        syntaxErrorPrototype.initialize(realm);
        typeErrorConstructor.initialize(realm);
        typeErrorPrototype.initialize(realm);
        uriErrorConstructor.initialize(realm);
        uriErrorPrototype.initialize(realm);
        internalErrorConstructor.initialize(realm);
        internalErrorPrototype.initialize(realm);
    }

    /**
     * <h1>23 Keyed Collection</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeCollectionModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;

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
        mapConstructor.initialize(realm);
        mapPrototype.initialize(realm);
        mapIteratorPrototype.initialize(realm);
        weakMapConstructor.initialize(realm);
        weakMapPrototype.initialize(realm);
        setConstructor.initialize(realm);
        setPrototype.initialize(realm);
        setIteratorPrototype.initialize(realm);
        weakSetConstructor.initialize(realm);
        weakSetPrototype.initialize(realm);
    }

    /**
     * <h1>26 Reflection</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeReflectModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;

        // allocation phase
        ProxyConstructor proxy = new ProxyConstructor(realm);
        ReflectObject reflect = new ReflectObject(realm);

        // registration phase
        intrinsics.put(Intrinsics.Proxy, proxy);
        intrinsics.put(Intrinsics.Reflect, reflect);

        // initialization phase
        proxy.initialize(realm);

        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.Realm)
                || realm.getRuntimeContext().isEnabled(CompatibilityOption.FrozenRealm)) {
            RealmConstructor realmConstructor = new RealmConstructor(realm);
            RealmPrototype realmPrototype = new RealmPrototype(realm);

            intrinsics.put(Intrinsics.Realm, realmConstructor);
            intrinsics.put(Intrinsics.RealmPrototype, realmPrototype);

            realmConstructor.initialize(realm);
            realmPrototype.initialize(realm);
        }

        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.Loader)) {
            LoaderConstructor loaderConstructor = new LoaderConstructor(realm);
            LoaderPrototype loaderPrototype = new LoaderPrototype(realm);

            intrinsics.put(Intrinsics.Loader, loaderConstructor);
            intrinsics.put(Intrinsics.LoaderPrototype, loaderPrototype);

            loaderConstructor.initialize(realm);
            loaderPrototype.initialize(realm);
        }

        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.WeakReference)) {
            WeakRefPrototype weakReferencePrototype = new WeakRefPrototype(realm);

            intrinsics.put(Intrinsics.WeakRefPrototype, weakReferencePrototype);

            weakReferencePrototype.initialize(realm);
        }

        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.System)
                || realm.getRuntimeContext().isEnabled(CompatibilityOption.SystemGlobal)
                || realm.getRuntimeContext().isEnabled(CompatibilityOption.WeakReference)
                || realm.getRuntimeContext().isEnabled(CompatibilityOption.ErrorStacks)) {
            SystemObject systemObject = new SystemObject(realm);

            intrinsics.put(Intrinsics.System, systemObject);

            systemObject.initialize(realm);
        }

        reflect.initialize(realm);
    }

    /**
     * <h1>25 Control Abstraction Objects</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeIterationModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;

        // allocation phase
        GeneratorFunctionConstructor generatorFunctionConstructor = new GeneratorFunctionConstructor(realm);
        GeneratorPrototype generatorPrototype = new GeneratorPrototype(realm);
        GeneratorFunctionPrototype generator = new GeneratorFunctionPrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.GeneratorFunction, generatorFunctionConstructor);
        intrinsics.put(Intrinsics.GeneratorPrototype, generatorPrototype);
        intrinsics.put(Intrinsics.Generator, generator);

        // initialization phase
        generatorFunctionConstructor.initialize(realm);
        generatorPrototype.initialize(realm);
        generator.initialize(realm);
    }

    /**
     * <h1>22.2 TypedArray Objects, 24.1 ArrayBuffer Objects, 24.2 DataView Objects</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeBinaryModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;

        // allocation phase
        ArrayBufferConstructor arrayBufferConstructor = new ArrayBufferConstructor(realm);
        ArrayBufferPrototype arrayBufferPrototype = new ArrayBufferPrototype(realm);
        TypedArrayConstructorPrototype typedArrayConstructor = new TypedArrayConstructorPrototype(realm);
        TypedArrayPrototypePrototype typedArrayPrototype = new TypedArrayPrototypePrototype(realm);
        TypedArrayConstructor int8ArrayConstructor = new TypedArrayConstructor(realm, ElementType.Int8);
        TypedArrayPrototype int8ArrayPrototype = new TypedArrayPrototype(realm, ElementType.Int8);
        TypedArrayConstructor uint8ArrayConstructor = new TypedArrayConstructor(realm, ElementType.Uint8);
        TypedArrayPrototype uint8ArrayPrototype = new TypedArrayPrototype(realm, ElementType.Uint8);
        TypedArrayConstructor uint8CArrayConstructor = new TypedArrayConstructor(realm, ElementType.Uint8C);
        TypedArrayPrototype uint8CArrayPrototype = new TypedArrayPrototype(realm, ElementType.Uint8C);
        TypedArrayConstructor int16ArrayConstructor = new TypedArrayConstructor(realm, ElementType.Int16);
        TypedArrayPrototype int16ArrayPrototype = new TypedArrayPrototype(realm, ElementType.Int16);
        TypedArrayConstructor uint16ArrayConstructor = new TypedArrayConstructor(realm, ElementType.Uint16);
        TypedArrayPrototype uint16ArrayPrototype = new TypedArrayPrototype(realm, ElementType.Uint16);
        TypedArrayConstructor int32ArrayConstructor = new TypedArrayConstructor(realm, ElementType.Int32);
        TypedArrayPrototype int32ArrayPrototype = new TypedArrayPrototype(realm, ElementType.Int32);
        TypedArrayConstructor uint32ArrayConstructor = new TypedArrayConstructor(realm, ElementType.Uint32);
        TypedArrayPrototype uint32ArrayPrototype = new TypedArrayPrototype(realm, ElementType.Uint32);
        TypedArrayConstructor float32ArrayConstructor = new TypedArrayConstructor(realm, ElementType.Float32);
        TypedArrayPrototype float32ArrayPrototype = new TypedArrayPrototype(realm, ElementType.Float32);
        TypedArrayConstructor float64ArrayConstructor = new TypedArrayConstructor(realm, ElementType.Float64);
        TypedArrayPrototype float64ArrayPrototype = new TypedArrayPrototype(realm, ElementType.Float64);
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
        arrayBufferConstructor.initialize(realm);
        arrayBufferPrototype.initialize(realm);
        typedArrayConstructor.initialize(realm);
        typedArrayPrototype.initialize(realm);
        int8ArrayConstructor.initialize(realm);
        int8ArrayPrototype.initialize(realm);
        uint8ArrayConstructor.initialize(realm);
        uint8ArrayPrototype.initialize(realm);
        uint8CArrayConstructor.initialize(realm);
        uint8CArrayPrototype.initialize(realm);
        int16ArrayConstructor.initialize(realm);
        int16ArrayPrototype.initialize(realm);
        uint16ArrayConstructor.initialize(realm);
        uint16ArrayPrototype.initialize(realm);
        int32ArrayConstructor.initialize(realm);
        int32ArrayPrototype.initialize(realm);
        uint32ArrayConstructor.initialize(realm);
        uint32ArrayPrototype.initialize(realm);
        float32ArrayConstructor.initialize(realm);
        float32ArrayPrototype.initialize(realm);
        float64ArrayConstructor.initialize(realm);
        float64ArrayPrototype.initialize(realm);
        dataViewConstructor.initialize(realm);
        dataViewPrototype.initialize(realm);
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

        // allocation phase
        PromiseConstructor promiseConstructor = new PromiseConstructor(realm);
        PromisePrototype promisePrototype = new PromisePrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.Promise, promiseConstructor);
        intrinsics.put(Intrinsics.PromisePrototype, promisePrototype);

        // initialization phase
        promiseConstructor.initialize(realm);
        promisePrototype.initialize(realm);
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

        // allocation phase
        IntlObject intlObject = new IntlObject(realm);
        CollatorConstructor collatorConstructor = new CollatorConstructor(realm);
        CollatorPrototype collatorPrototype = new CollatorPrototype(realm);
        NumberFormatConstructor numberFormatConstructor = new NumberFormatConstructor(realm);
        NumberFormatPrototype numberFormatPrototype = new NumberFormatPrototype(realm);
        DateTimeFormatConstructor dateTimeFormatConstructor = new DateTimeFormatConstructor(realm);
        DateTimeFormatPrototype dateTimeFormatPrototype = new DateTimeFormatPrototype(realm);
        PluralRulesConstructor pluralRulesConstructor = null;
        PluralRulesPrototype pluralRulesPrototype = null;
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.PluralRules)) {
            pluralRulesConstructor = new PluralRulesConstructor(realm);
            pluralRulesPrototype = new PluralRulesPrototype(realm);
        }
        SegmenterConstructor segmenterConstructor = null;
        SegmenterPrototype segmenterPrototype = null;
        SegmentIteratorPrototype segmentIteratorPrototype = null;
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.IntlSegmenter)) {
            segmenterConstructor = new SegmenterConstructor(realm);
            segmenterPrototype = new SegmenterPrototype(realm);
            segmentIteratorPrototype = new SegmentIteratorPrototype(realm);
        }
        ListFormatConstructor listFormatConstructor = null;
        ListFormatPrototype listFormatPrototype = null;
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.IntlListFormat)) {
            listFormatConstructor = new ListFormatConstructor(realm);
            listFormatPrototype = new ListFormatPrototype(realm);
        }

        // registration phase
        intrinsics.put(Intrinsics.Intl, intlObject);
        intrinsics.put(Intrinsics.Intl_Collator, collatorConstructor);
        intrinsics.put(Intrinsics.Intl_CollatorPrototype, collatorPrototype);
        intrinsics.put(Intrinsics.Intl_NumberFormat, numberFormatConstructor);
        intrinsics.put(Intrinsics.Intl_NumberFormatPrototype, numberFormatPrototype);
        intrinsics.put(Intrinsics.Intl_DateTimeFormat, dateTimeFormatConstructor);
        intrinsics.put(Intrinsics.Intl_DateTimeFormatPrototype, dateTimeFormatPrototype);
        if (pluralRulesConstructor != null) {
            intrinsics.put(Intrinsics.Intl_PluralRules, pluralRulesConstructor);
            intrinsics.put(Intrinsics.Intl_PluralRulesPrototype, pluralRulesPrototype);
        }
        if (segmenterConstructor != null) {
            intrinsics.put(Intrinsics.Intl_Segmenter, segmenterConstructor);
            intrinsics.put(Intrinsics.Intl_SegmenterPrototype, segmenterPrototype);
            intrinsics.put(Intrinsics.Intl_SegmentIteratorPrototype, segmentIteratorPrototype);
        }
        if (listFormatConstructor != null) {
            intrinsics.put(Intrinsics.Intl_ListFormat, listFormatConstructor);
            intrinsics.put(Intrinsics.Intl_ListFormatPrototype, listFormatPrototype);
        }

        // initialization phase
        intlObject.initialize(realm);
        collatorConstructor.initialize(realm);
        collatorPrototype.initialize(realm);
        numberFormatConstructor.initialize(realm);
        numberFormatPrototype.initialize(realm);
        dateTimeFormatConstructor.initialize(realm);
        dateTimeFormatPrototype.initialize(realm);
        if (pluralRulesConstructor != null) {
            pluralRulesConstructor.initialize(realm);
            pluralRulesPrototype.initialize(realm);
        }
        if (segmenterConstructor != null) {
            segmenterConstructor.initialize(realm);
            segmenterPrototype.initialize(realm);
            segmentIteratorPrototype.initialize(realm);
        }
        if (listFormatConstructor != null) {
            listFormatConstructor.initialize(realm);
            listFormatPrototype.initialize(realm);
        }
    }

    /**
     * <h1>Extension: Async Function Declaration</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeAsyncModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;

        // allocation phase
        AsyncFunctionConstructor asyncFunctionConstructor = new AsyncFunctionConstructor(realm);
        AsyncFunctionPrototype asyncFunctionPrototype = new AsyncFunctionPrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.AsyncFunction, asyncFunctionConstructor);
        intrinsics.put(Intrinsics.AsyncFunctionPrototype, asyncFunctionPrototype);

        // initialization phase
        asyncFunctionConstructor.initialize(realm);
        asyncFunctionPrototype.initialize(realm);
    }

    /**
     * <h1>Extension: Async Generator Function Declaration</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeAsyncIterationModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;

        // allocation phase
        AsyncGeneratorFunctionConstructor asyncGenFunctionConstructor = new AsyncGeneratorFunctionConstructor(realm);
        AsyncGeneratorPrototype asyncGeneratorPrototype = new AsyncGeneratorPrototype(realm);
        AsyncGeneratorFunctionPrototype asyncGenerator = new AsyncGeneratorFunctionPrototype(realm);
        AsyncIteratorPrototype asyncIteratorPrototype = new AsyncIteratorPrototype(realm);
        AsyncFromSyncIteratorPrototype asyncFromSyncIteratorPrototype = new AsyncFromSyncIteratorPrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.AsyncGeneratorFunction, asyncGenFunctionConstructor);
        intrinsics.put(Intrinsics.AsyncGeneratorPrototype, asyncGeneratorPrototype);
        intrinsics.put(Intrinsics.AsyncGenerator, asyncGenerator);
        intrinsics.put(Intrinsics.AsyncIteratorPrototype, asyncIteratorPrototype);
        intrinsics.put(Intrinsics.AsyncFromSyncIteratorPrototype, asyncFromSyncIteratorPrototype);

        // initialization phase
        asyncGenFunctionConstructor.initialize(realm);
        asyncGeneratorPrototype.initialize(realm);
        asyncGenerator.initialize(realm);
        asyncIteratorPrototype.initialize(realm);
        asyncFromSyncIteratorPrototype.initialize(realm);
    }

    /**
     * <h1>Extension: SIMD</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeSIMDModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;

        // allocation phase
        SIMD simd = new SIMD(realm);
        Float64x2Constructor float64x2Constructor = null;
        Float64x2Prototype float64x2Prototype = null;
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.SIMD_Phase2)) {
            float64x2Constructor = new Float64x2Constructor(realm);
            float64x2Prototype = new Float64x2Prototype(realm);
        }
        Float32x4Constructor float32x4Constructor = new Float32x4Constructor(realm);
        Float32x4Prototype float32x4Prototype = new Float32x4Prototype(realm);
        Int32x4Constructor int32x4Constructor = new Int32x4Constructor(realm);
        Int32x4Prototype int32x4Prototype = new Int32x4Prototype(realm);
        Int16x8Constructor int16x8Constructor = new Int16x8Constructor(realm);
        Int16x8Prototype int16x8Prototype = new Int16x8Prototype(realm);
        Int8x16Constructor int8x16Constructor = new Int8x16Constructor(realm);
        Int8x16Prototype int8x16Prototype = new Int8x16Prototype(realm);
        Uint32x4Constructor uint32x4Constructor = new Uint32x4Constructor(realm);
        Uint32x4Prototype uint32x4Prototype = new Uint32x4Prototype(realm);
        Uint16x8Constructor uint16x8Constructor = new Uint16x8Constructor(realm);
        Uint16x8Prototype uint16x8Prototype = new Uint16x8Prototype(realm);
        Uint8x16Constructor uint8x16Constructor = new Uint8x16Constructor(realm);
        Uint8x16Prototype uint8x16Prototype = new Uint8x16Prototype(realm);
        Bool64x2Constructor bool64x2Constructor = null;
        Bool64x2Prototype bool64x2Prototype = null;
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.SIMD_Phase2)) {
            bool64x2Constructor = new Bool64x2Constructor(realm);
            bool64x2Prototype = new Bool64x2Prototype(realm);
        }
        Bool32x4Constructor bool32x4Constructor = new Bool32x4Constructor(realm);
        Bool32x4Prototype bool32x4Prototype = new Bool32x4Prototype(realm);
        Bool16x8Constructor bool16x8Constructor = new Bool16x8Constructor(realm);
        Bool16x8Prototype bool16x8Prototype = new Bool16x8Prototype(realm);
        Bool8x16Constructor bool8x16Constructor = new Bool8x16Constructor(realm);
        Bool8x16Prototype bool8x16Prototype = new Bool8x16Prototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.SIMD, simd);
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.SIMD_Phase2)) {
            intrinsics.put(Intrinsics.SIMD_Float64x2, float64x2Constructor);
            intrinsics.put(Intrinsics.SIMD_Float64x2Prototype, float64x2Prototype);
        }
        intrinsics.put(Intrinsics.SIMD_Float32x4, float32x4Constructor);
        intrinsics.put(Intrinsics.SIMD_Float32x4Prototype, float32x4Prototype);
        intrinsics.put(Intrinsics.SIMD_Int32x4, int32x4Constructor);
        intrinsics.put(Intrinsics.SIMD_Int32x4Prototype, int32x4Prototype);
        intrinsics.put(Intrinsics.SIMD_Int16x8, int16x8Constructor);
        intrinsics.put(Intrinsics.SIMD_Int16x8Prototype, int16x8Prototype);
        intrinsics.put(Intrinsics.SIMD_Int8x16, int8x16Constructor);
        intrinsics.put(Intrinsics.SIMD_Int8x16Prototype, int8x16Prototype);
        intrinsics.put(Intrinsics.SIMD_Uint32x4, uint32x4Constructor);
        intrinsics.put(Intrinsics.SIMD_Uint32x4Prototype, uint32x4Prototype);
        intrinsics.put(Intrinsics.SIMD_Uint16x8, uint16x8Constructor);
        intrinsics.put(Intrinsics.SIMD_Uint16x8Prototype, uint16x8Prototype);
        intrinsics.put(Intrinsics.SIMD_Uint8x16, uint8x16Constructor);
        intrinsics.put(Intrinsics.SIMD_Uint8x16Prototype, uint8x16Prototype);
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.SIMD_Phase2)) {
            intrinsics.put(Intrinsics.SIMD_Bool64x2, bool64x2Constructor);
            intrinsics.put(Intrinsics.SIMD_Bool64x2Prototype, bool64x2Prototype);
        }
        intrinsics.put(Intrinsics.SIMD_Bool32x4, bool32x4Constructor);
        intrinsics.put(Intrinsics.SIMD_Bool32x4Prototype, bool32x4Prototype);
        intrinsics.put(Intrinsics.SIMD_Bool16x8, bool16x8Constructor);
        intrinsics.put(Intrinsics.SIMD_Bool16x8Prototype, bool16x8Prototype);
        intrinsics.put(Intrinsics.SIMD_Bool8x16, bool8x16Constructor);
        intrinsics.put(Intrinsics.SIMD_Bool8x16Prototype, bool8x16Prototype);

        // initialization phase
        simd.initialize(realm);
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.SIMD_Phase2)) {
            float64x2Constructor.initialize(realm);
            float64x2Prototype.initialize(realm);
        }
        float32x4Constructor.initialize(realm);
        float32x4Prototype.initialize(realm);
        int32x4Constructor.initialize(realm);
        int32x4Prototype.initialize(realm);
        int16x8Constructor.initialize(realm);
        int16x8Prototype.initialize(realm);
        int8x16Constructor.initialize(realm);
        int8x16Prototype.initialize(realm);
        uint32x4Constructor.initialize(realm);
        uint32x4Prototype.initialize(realm);
        uint16x8Constructor.initialize(realm);
        uint16x8Prototype.initialize(realm);
        uint8x16Constructor.initialize(realm);
        uint8x16Prototype.initialize(realm);
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.SIMD_Phase2)) {
            bool64x2Constructor.initialize(realm);
            bool64x2Prototype.initialize(realm);
        }
        bool32x4Constructor.initialize(realm);
        bool32x4Prototype.initialize(realm);
        bool16x8Constructor.initialize(realm);
        bool16x8Prototype.initialize(realm);
        bool8x16Constructor.initialize(realm);
        bool8x16Prototype.initialize(realm);
    }

    /**
     * <h1>Extension: Observable</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeObservableModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;

        // allocation phase
        ObservableConstructor observableConstructor = new ObservableConstructor(realm);
        ObservablePrototype observablePrototype = new ObservablePrototype(realm);
        SubscriptionPrototype subscriptionPrototype = new SubscriptionPrototype(realm);
        SubscriptionObserverPrototype subscriptionObserverPrototype = new SubscriptionObserverPrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.Observable, observableConstructor);
        intrinsics.put(Intrinsics.ObservablePrototype, observablePrototype);
        intrinsics.put(Intrinsics.SubscriptionPrototype, subscriptionPrototype);
        intrinsics.put(Intrinsics.SubscriptionObserverPrototype, subscriptionObserverPrototype);

        // initialization phase
        observableConstructor.initialize(realm);
        observablePrototype.initialize(realm);
        subscriptionPrototype.initialize(realm);
        subscriptionObserverPrototype.initialize(realm);
    }

    /**
     * <h1>Extension: Shared Memory and Atomics</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeAtomicsModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;

        // allocation phase
        AtomicsObject atomicsObject = new AtomicsObject(realm);
        SharedArrayBufferConstructor sharedArrayBufferConstructor = new SharedArrayBufferConstructor(realm);
        SharedArrayBufferPrototype sharedArrayBufferPrototype = new SharedArrayBufferPrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.Atomics, atomicsObject);
        intrinsics.put(Intrinsics.SharedArrayBuffer, sharedArrayBufferConstructor);
        intrinsics.put(Intrinsics.SharedArrayBufferPrototype, sharedArrayBufferPrototype);

        // initialization phase
        atomicsObject.initialize(realm);
        sharedArrayBufferConstructor.initialize(realm);
        sharedArrayBufferPrototype.initialize(realm);
    }

    /**
     * <h1>Extension: Zones</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeZonesModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;

        // allocation phase
        ZoneConstructor zoneConstructor = new ZoneConstructor(realm);
        ZonePrototype zonePrototype = new ZonePrototype(realm);

        // registration phase
        intrinsics.put(Intrinsics.Zone, zoneConstructor);
        intrinsics.put(Intrinsics.ZonePrototype, zonePrototype);

        // initialization phase
        zoneConstructor.initialize(realm);
        zonePrototype.initialize(realm);
    }

    /**
     * <h1>Extension: BigInt</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeBigIntModule(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;

        // allocation phase
        BigIntConstructor bigIntConstructor = new BigIntConstructor(realm);
        BigIntPrototype bigIntPrototype = new BigIntPrototype(realm);
        TypedArrayConstructor int64ArrayConstructor = new TypedArrayConstructor(realm, ElementType.BigInt64);
        TypedArrayPrototype int64ArrayPrototype = new TypedArrayPrototype(realm, ElementType.BigInt64);
        TypedArrayConstructor uint64ArrayConstructor = new TypedArrayConstructor(realm, ElementType.BigUint64);
        TypedArrayPrototype uint64ArrayPrototype = new TypedArrayPrototype(realm, ElementType.BigUint64);

        // registration phase
        intrinsics.put(Intrinsics.BigInt, bigIntConstructor);
        intrinsics.put(Intrinsics.BigIntPrototype, bigIntPrototype);
        intrinsics.put(Intrinsics.BigInt64Array, int64ArrayConstructor);
        intrinsics.put(Intrinsics.BigInt64ArrayPrototype, int64ArrayPrototype);
        intrinsics.put(Intrinsics.BigUint64Array, uint64ArrayConstructor);
        intrinsics.put(Intrinsics.BigUint64ArrayPrototype, uint64ArrayPrototype);

        // initialization phase
        bigIntConstructor.initialize(realm);
        bigIntPrototype.initialize(realm);
        int64ArrayConstructor.initialize(realm);
        int64ArrayPrototype.initialize(realm);
        uint64ArrayConstructor.initialize(realm);
        uint64ArrayPrototype.initialize(realm);
    }

    /**
     * <h1>18 The Global Object</h1>
     * 
     * @param realm
     *            the realm instance
     */
    private static void initializeGlobalObject(Realm realm) {
        EnumMap<Intrinsics, OrdinaryObject> intrinsics = realm.intrinsics;
        GlobalObject global = realm.globalPropertiesObject;

        global.initialize(realm);

        intrinsics.put(Intrinsics.decodeURI, getBuiltin(global, "decodeURI"));
        intrinsics.put(Intrinsics.decodeURIComponent, getBuiltin(global, "decodeURIComponent"));
        intrinsics.put(Intrinsics.encodeURI, getBuiltin(global, "encodeURI"));
        intrinsics.put(Intrinsics.encodeURIComponent, getBuiltin(global, "encodeURIComponent"));
        intrinsics.put(Intrinsics.eval, getBuiltin(global, "eval"));
        intrinsics.put(Intrinsics.isFinite, getBuiltin(global, "isFinite"));
        intrinsics.put(Intrinsics.isNaN, getBuiltin(global, "isNaN"));
        intrinsics.put(Intrinsics.parseFloat, getBuiltin(global, "parseFloat"));
        intrinsics.put(Intrinsics.parseInt, getBuiltin(global, "parseInt"));

        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.GlobalObject)) {
            intrinsics.put(Intrinsics.escape, getBuiltin(global, "escape"));
            intrinsics.put(Intrinsics.unescape, getBuiltin(global, "unescape"));
        }
    }

    private static OrdinaryObject getBuiltin(GlobalObject global, String name) {
        return (OrdinaryObject) global.lookupOwnProperty(name).getValue();
    }
}
