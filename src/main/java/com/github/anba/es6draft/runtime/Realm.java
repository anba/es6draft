/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;
import static com.github.anba.es6draft.runtime.ExecutionContext.newDefaultExecutionContext;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newGlobalEnvironment;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.security.SecureRandom;
import java.text.Collator;
import java.text.DecimalFormatSymbols;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
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
import com.github.anba.es6draft.runtime.objects.iteration.IteratorPrototype;
import com.github.anba.es6draft.runtime.objects.number.MathObject;
import com.github.anba.es6draft.runtime.objects.number.NumberConstructor;
import com.github.anba.es6draft.runtime.objects.number.NumberPrototype;
import com.github.anba.es6draft.runtime.objects.promise.PromiseConstructor;
import com.github.anba.es6draft.runtime.objects.promise.PromisePrototype;
import com.github.anba.es6draft.runtime.objects.reflect.LoaderConstructor;
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
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
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

    private final Callable builtinEval;

    private ExecutionContext scriptContext;

    private final World<? extends GlobalObject> world;

    private final ExecutionContext defaultContext;

    private final GlobalObject globalObject;

    private final SecureRandom random = new SecureRandom();

    private Realm(World<? extends GlobalObject> world) {
        this.world = world;
        this.defaultContext = newDefaultExecutionContext(this);
        this.globalObject = world.newGlobal(this);
        this.globalThis = world.newGlobal(this); // TODO: yuk...
        this.globalEnv = newGlobalEnvironment(defaultContext, globalThis);
        this.realmObject = new RealmObject(this);

        // Create all built-in intrinsics
        CreateIntrinsics(this);

        // Initialize global object
        globalObject.initialize(this);

        // Store reference to built-in eval
        builtinEval = (Callable) globalObject.lookupOwnProperty("eval").getValue();
        intrinsics.put(Intrinsics.eval, (OrdinaryObject) builtinEval);

        // [[Prototype]] for default global is implementation-dependent
        globalThis.setPrototypeOf(defaultContext, getIntrinsic(Intrinsics.ObjectPrototype));

        // Set [[Prototype]] after intrinsics are initialized
        realmObject.setPrototype(getIntrinsic(Intrinsics.RealmPrototype));
        realmObject.setRealm(this);
    }

    private Realm(World<? extends GlobalObject> world, RealmObject realmObject) {
        this.world = world;
        this.defaultContext = newDefaultExecutionContext(this);
        this.globalObject = world.newGlobal(this);
        this.globalThis = ObjectCreate(defaultContext, (ScriptObject) null);
        this.globalEnv = newGlobalEnvironment(defaultContext, globalThis);
        this.realmObject = realmObject;

        // Create all built-in intrinsics
        CreateIntrinsics(this);

        // Initialize global object
        globalObject.initialize(this);

        // Store reference to built-in eval
        builtinEval = (Callable) globalObject.lookupOwnProperty("eval").getValue();
        intrinsics.put(Intrinsics.eval, (OrdinaryObject) builtinEval);

        // Set prototype to %ObjectPrototype%, cf. 8.2.3 SetRealmGlobalObject
        globalThis.setPrototypeOf(defaultContext, getIntrinsic(Intrinsics.ObjectPrototype));
    }

    private Realm(World<? extends GlobalObject> world, RealmObject realmObject,
            ScriptObject globalThis) {
        this.world = world;
        this.defaultContext = newDefaultExecutionContext(this);
        this.globalObject = world.newGlobal(this);
        this.globalThis = globalThis;
        this.globalEnv = newGlobalEnvironment(defaultContext, globalThis);
        this.realmObject = realmObject;

        // Create all built-in intrinsics
        CreateIntrinsics(this);

        // Initialize global object
        globalObject.initialize(this);

        // Store reference to built-in eval
        builtinEval = (Callable) globalObject.lookupOwnProperty("eval").getValue();
        intrinsics.put(Intrinsics.eval, (OrdinaryObject) builtinEval);
    }

    /**
     * Returns the source info from the caller execution context. If no applicable source is
     * attached to the caller context, the source from the most recent script execution on this
     * realm is returned.
     * 
     * @param caller
     *            the caller context
     * @return the source info or {@code null} if not available
     */
    public Source sourceInfo(ExecutionContext caller) {
        Executable callerExec = caller.getCurrentExecutable();
        if (hasSourceInfo(callerExec)) {
            return callerExec.getSourceObject().toSource();
        }
        ExecutionContext scriptContext = getScriptContext();
        if (scriptContext != null) {
            Executable currentExec = scriptContext.getCurrentExecutable();
            if (hasSourceInfo(currentExec)) {
                return currentExec.getSourceObject().toSource();
            }
        }
        // Neither caller nor realm has source info available, return null
        return null;
    }

    private boolean hasSourceInfo(Executable exec) {
        assert exec == null || exec.getSourceObject() != null
                || exec == defaultContext.getCurrentExecutable();
        return exec != null && exec.getSourceObject() != null;
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
    public TimeZone getTimeZone() {
        return world.getTimeZone();
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
    public void setExtensionHooks(Callable directEvalTranslate, Callable nonEvalFallback,
            Callable indirectEval) {
        this.directEvalTranslate = directEvalTranslate;
        this.nonEvalFallback = nonEvalFallback;
        this.indirectEval = indirectEval;
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

    /**
     * 8.2.1 CreateRealm ( ) Abstract Operation<br>
     * 8.2.3 SetRealmGlobalObject ( realmRec, globalObj ) Abstract Operation
     * <p>
     * Creates a new {@link Realm} object.
     * 
     * @param cx
     *            the execution context
     * @param realmObject
     *            the realm object
     * @param globalObj
     *            the global this object or {@code null}
     * @return the new realm instance
     */
    public static Realm CreateRealmAndSetRealmGlobalObject(ExecutionContext cx,
            RealmObject realmObject, ScriptObject globalObj) {
        World<? extends GlobalObject> world = cx.getRealm().getWorld();
        if (globalObj == null) {
            return new Realm(world, realmObject);
        } else {
            return new Realm(world, realmObject, globalObj);
        }
    }

    /**
     * 8.2.4 SetDefaultGlobalBindings ( realmRec ) Abstract Operation
     * <p>
     * Initializes {@code [[globalThis]]} with the default properties of the Global Object.
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @return the global this object
     */
    public static ScriptObject SetDefaultGlobalBindings(ExecutionContext cx, Realm realm) {
        /* step 1 */
        ScriptObject globalThis = realm.getGlobalThis();
        GlobalObject globalObject = realm.getGlobalObject();
        assert globalThis != null && globalObject != null;
        /* step 2 */
        for (Object key : globalObject.ownPropertyKeys(cx)) {
            if (key instanceof String) {
                String propertyKey = (String) key;
                Property prop = globalObject.getOwnProperty(cx, propertyKey);
                if (prop != null) {
                    PropertyDescriptor desc = prop.toPropertyDescriptor();
                    DefinePropertyOrThrow(cx, globalThis, propertyKey, desc);
                }
            } else {
                Symbol propertyKey = (Symbol) key;
                Property prop = globalObject.getOwnProperty(cx, propertyKey);
                if (prop != null) {
                    PropertyDescriptor desc = prop.toPropertyDescriptor();
                    DefinePropertyOrThrow(cx, globalThis, propertyKey, desc);
                }
            }
        }
        /* step 3 */
        return globalThis;
    }

    /**
     * 8.2.1 CreateRealm ( )
     * <p>
     * Creates a new {@link Realm} object.
     * 
     * @param cx
     *            the execution context
     * @return the new realm instance
     */
    public static Realm CreateRealm(ExecutionContext cx) {
        // The operation is not supported in this implementation.
        throw new UnsupportedOperationException();
    }

    /**
     * 8.2.3 SetRealmGlobalObject ( realmRec, globalObj ) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param globalObj
     *            the global this object or {@code null}
     * @return the new realm instance
     */
    public static Realm SetRealmGlobalObject(ExecutionContext cx, Realm realm,
            ScriptObject globalObj) {
        // The operation is not supported in this implementation.
        throw new UnsupportedOperationException();
    }

    /**
     * 8.2.2 CreateIntrinsics ( realmRec ) Abstract Operation
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
        realm.throwTypeError = TypeErrorThrower.createThrowTypeError(realm);

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
        ProxyConstructorFunction proxy = new ProxyConstructorFunction(realm);
        Reflect reflect = new Reflect(realm);

        // registration phase
        intrinsics.put(Intrinsics.Proxy, proxy);
        intrinsics.put(Intrinsics.Reflect, reflect);

        // initialization phase
        proxy.initialize(realm);

        if (realm.isEnabled(CompatibilityOption.Realm)) {
            RealmConstructor realmConstructor = new RealmConstructor(realm);
            RealmPrototype realmPrototype = new RealmPrototype(realm);

            intrinsics.put(Intrinsics.Realm, realmConstructor);
            intrinsics.put(Intrinsics.RealmPrototype, realmPrototype);

            realmConstructor.initialize(realm);
            realmPrototype.initialize(realm);
        }

        if (realm.isEnabled(CompatibilityOption.Loader)) {
            LoaderConstructor loaderConstructor = new LoaderConstructor(realm);
            LoaderPrototype loaderPrototype = new LoaderPrototype(realm);

            intrinsics.put(Intrinsics.Loader, loaderConstructor);
            intrinsics.put(Intrinsics.LoaderPrototype, loaderPrototype);

            loaderConstructor.initialize(realm);
            loaderPrototype.initialize(realm);
        }

        if (realm.isEnabled(CompatibilityOption.System)) {
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
        GeneratorFunctionConstructor generatorFunctionConstructor = new GeneratorFunctionConstructor(
                realm);
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

        if (realm.isEnabled(CompatibilityOption.LegacyGenerator)) {
            OrdinaryObject legacyGeneratorPrototype = ObjectCreate(realm,
                    Intrinsics.ObjectPrototype);
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

        // registration phase
        intrinsics.put(Intrinsics.Intl, intlObject);
        intrinsics.put(Intrinsics.Intl_Collator, collatorConstructor);
        intrinsics.put(Intrinsics.Intl_CollatorPrototype, collatorPrototype);
        intrinsics.put(Intrinsics.Intl_NumberFormat, numberFormatConstructor);
        intrinsics.put(Intrinsics.Intl_NumberFormatPrototype, numberFormatPrototype);
        intrinsics.put(Intrinsics.Intl_DateTimeFormat, dateTimeFormatConstructor);
        intrinsics.put(Intrinsics.Intl_DateTimeFormatPrototype, dateTimeFormatPrototype);

        // initialization phase
        intlObject.initialize(realm);
        collatorConstructor.initialize(realm);
        collatorPrototype.initialize(realm);
        numberFormatConstructor.initialize(realm);
        numberFormatPrototype.initialize(realm);
        dateTimeFormatConstructor.initialize(realm);
        dateTimeFormatPrototype.initialize(realm);
    }
}
