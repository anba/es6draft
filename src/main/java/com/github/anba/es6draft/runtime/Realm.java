/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;

import java.text.Collator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.modules.Loader;
import com.github.anba.es6draft.runtime.objects.*;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferPrototype;
import com.github.anba.es6draft.runtime.objects.binary.DataViewConstructor;
import com.github.anba.es6draft.runtime.objects.binary.DataViewPrototype;
import com.github.anba.es6draft.runtime.objects.binary.ElementKind;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayConstructor;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayPrototype;
import com.github.anba.es6draft.runtime.objects.intl.CollatorConstructor;
import com.github.anba.es6draft.runtime.objects.intl.CollatorPrototype;
import com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatConstructor;
import com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatPrototype;
import com.github.anba.es6draft.runtime.objects.intl.IntlObject;
import com.github.anba.es6draft.runtime.objects.intl.NumberFormatConstructor;
import com.github.anba.es6draft.runtime.objects.intl.NumberFormatPrototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.builtins.ListIterator.ListIteratorPrototype;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;

/**
 * <h1>10 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>10.3 Code Realms
 * </ul>
 */
public class Realm {
    /**
     * [[intrinsics]]
     */
    private Map<Intrinsics, Scriptable> intrinsics = new EnumMap<>(Intrinsics.class);
    private Map<NativeError.ErrorType, Scriptable> errors = new EnumMap<>(
            NativeError.ErrorType.class);

    /**
     * [[globalThis]]
     */
    private Scriptable globalThis;

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

    private Locale locale = Locale.getDefault();
    private TimeZone timezone = TimeZone.getDefault();
    private Messages messages = Messages.create(locale);

    // TODO: move into function source object
    private Map<String, Scriptable> templateCallSites = new HashMap<>();

    private Realm() {
    }

    /**
     * [[intrinsics]]
     */
    public Scriptable getIntrinsic(Intrinsics id) {
        return intrinsics.get(id);
    }

    /**
     * [[globalThis]]
     */
    public Scriptable getGlobalThis() {
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
        assert throwTypeError != null : "throwTypeError not yet initialized";
        return throwTypeError;
    }

    private AtomicInteger evalCounter = new AtomicInteger(0);
    private AtomicInteger functionCounter = new AtomicInteger(0);

    public String nextEvalName() {
        return "Eval_" + evalCounter.incrementAndGet();
    }

    public String nextFunctionName() {
        return "Function_" + functionCounter.incrementAndGet();
    }

    public Scriptable getNativeError(NativeError.ErrorType id) {
        return errors.get(id);
    }

    public Locale getLocale() {
        return locale;
    }

    public TimeZone getTimezone() {
        return timezone;
    }

    public String message(Messages.Key key) {
        return messages.getString(key);
    }

    public Callable getBuiltinEval() {
        return builtinEval;
    }

    public Scriptable getTemplateCallSite(String key) {
        return templateCallSites.get(key);
    }

    public void addTemplateCallSite(String key, Scriptable callSite) {
        templateCallSites.put(key, callSite);
    }

    public Collator getCollator() {
        Collator collator = Collator.getInstance(locale);
        // Use Normalised Form D for comparison (cf. 15.5.4.9, Note 2)
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        // `"\u0001".localeCompare("\u0002") == -1` should yield true
        collator.setStrength(Collator.IDENTICAL);
        return collator;
    }

    public static interface GlobalObjectCreator<GLOBAL extends GlobalObject> {
        GLOBAL createGlobal(Realm realm);
    }

    private static final GlobalObjectCreator<GlobalObject> DEFAULT_GLOBAL_OBJECT = new GlobalObjectCreator<GlobalObject>() {
        @Override
        public GlobalObject createGlobal(Realm realm) {
            return new GlobalObject(realm);
        }
    };

    public static Realm newRealm() {
        return newRealm(DEFAULT_GLOBAL_OBJECT);
    }

    public static Realm newRealm(GlobalObjectCreator<? extends GlobalObject> creator) {
        Realm realm = new Realm();
        GlobalObject globalThis = creator.createGlobal(realm);
        GlobalEnvironmentRecord envRec = new GlobalEnvironmentRecord(realm, globalThis);
        LexicalEnvironment globalEnv = new LexicalEnvironment(realm, envRec);

        //
        realm.globalThis = globalThis;
        realm.globalEnv = globalEnv;

        // intrinsics (1)
        ObjectConstructor objectConstructor = new ObjectConstructor(realm);
        ObjectPrototype objectPrototype = new ObjectPrototype(realm);
        FunctionConstructor functionConstructor = new FunctionConstructor(realm);
        FunctionPrototype functionPrototype = new FunctionPrototype(realm);
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
        MapConstructor mapConstructor = new MapConstructor(realm);
        MapPrototype mapPrototype = new MapPrototype(realm);
        MapIteratorPrototype mapIteratorPrototype = new MapIteratorPrototype(realm);
        WeakMapConstructor weakMapConstructor = new WeakMapConstructor(realm);
        WeakMapPrototype weakMapPrototype = new WeakMapPrototype(realm);
        SetConstructor setConstructor = new SetConstructor(realm);
        SetPrototype setPrototype = new SetPrototype(realm);
        SetIteratorPrototype setIteratorPrototype = new SetIteratorPrototype(realm);
        StopIterationObject stopIterationObject = new StopIterationObject(realm);

        // binary module intrinsics
        ArrayBufferConstructor arrayBufferConstructor = new ArrayBufferConstructor(realm);
        ArrayBufferPrototype arrayBufferPrototype = new ArrayBufferPrototype(realm);
        TypedArrayConstructor int8ArrayConstructor = TypedArrayConstructor.createConstructor(realm,
                ElementKind.Int8);
        TypedArrayPrototype int8ArrayPrototype = TypedArrayPrototype.createPrototype(realm,
                ElementKind.Int8);
        TypedArrayConstructor uint8ArrayConstructor = TypedArrayConstructor.createConstructor(
                realm, ElementKind.Uint8);
        TypedArrayPrototype uint8ArrayPrototype = TypedArrayPrototype.createPrototype(realm,
                ElementKind.Uint8);
        TypedArrayConstructor uint8CArrayConstructor = TypedArrayConstructor.createConstructor(
                realm, ElementKind.Uint8C);
        TypedArrayPrototype uint8CArrayPrototype = TypedArrayPrototype.createPrototype(realm,
                ElementKind.Uint8C);
        TypedArrayConstructor int16ArrayConstructor = TypedArrayConstructor.createConstructor(
                realm, ElementKind.Int16);
        TypedArrayPrototype int16ArrayPrototype = TypedArrayPrototype.createPrototype(realm,
                ElementKind.Int16);
        TypedArrayConstructor uint16ArrayConstructor = TypedArrayConstructor.createConstructor(
                realm, ElementKind.Uint16);
        TypedArrayPrototype uint16ArrayPrototype = TypedArrayPrototype.createPrototype(realm,
                ElementKind.Uint16);
        TypedArrayConstructor int32ArrayConstructor = TypedArrayConstructor.createConstructor(
                realm, ElementKind.Int32);
        TypedArrayPrototype int32ArrayPrototype = TypedArrayPrototype.createPrototype(realm,
                ElementKind.Int32);
        TypedArrayConstructor uint32ArrayConstructor = TypedArrayConstructor.createConstructor(
                realm, ElementKind.Uint32);
        TypedArrayPrototype uint32ArrayPrototype = TypedArrayPrototype.createPrototype(realm,
                ElementKind.Uint32);
        TypedArrayConstructor float32ArrayConstructor = TypedArrayConstructor.createConstructor(
                realm, ElementKind.Float32);
        TypedArrayPrototype float32ArrayPrototype = TypedArrayPrototype.createPrototype(realm,
                ElementKind.Float32);
        TypedArrayConstructor float64ArrayConstructor = TypedArrayConstructor.createConstructor(
                realm, ElementKind.Float64);
        TypedArrayPrototype float64ArrayPrototype = TypedArrayPrototype.createPrototype(realm,
                ElementKind.Float64);
        DataViewConstructor dataViewConstructor = new DataViewConstructor(realm);
        DataViewPrototype dataViewPrototype = new DataViewPrototype(realm);

        // Internationalization API
        IntlObject intlObject = new IntlObject(realm);
        CollatorConstructor collatorConstructor = new CollatorConstructor(realm);
        CollatorPrototype collatorPrototype = new CollatorPrototype(realm);
        NumberFormatConstructor numberFormatConstructor = new NumberFormatConstructor(realm);
        NumberFormatPrototype numberFormatPrototype = new NumberFormatPrototype(realm);
        DateTimeFormatConstructor dateTimeFormatConstructor = new DateTimeFormatConstructor(realm);
        DateTimeFormatPrototype dateTimeFormatPrototype = new DateTimeFormatPrototype(realm);

        // internal intrinsics
        ListIteratorPrototype listIteratorPrototype = new ListIteratorPrototype(realm);

        // intrinsics (2)
        Map<Intrinsics, Scriptable> intrinsics = realm.intrinsics;
        intrinsics.put(Intrinsics.Object, objectConstructor);
        intrinsics.put(Intrinsics.ObjectPrototype, objectPrototype);
        intrinsics.put(Intrinsics.Function, functionConstructor);
        intrinsics.put(Intrinsics.FunctionPrototype, functionPrototype);
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
        intrinsics.put(Intrinsics.Map, mapConstructor);
        intrinsics.put(Intrinsics.MapPrototype, mapPrototype);
        intrinsics.put(Intrinsics.MapIteratorPrototype, mapIteratorPrototype);
        intrinsics.put(Intrinsics.WeakMap, weakMapConstructor);
        intrinsics.put(Intrinsics.WeakMapPrototype, weakMapPrototype);
        intrinsics.put(Intrinsics.Set, setConstructor);
        intrinsics.put(Intrinsics.SetPrototype, setPrototype);
        intrinsics.put(Intrinsics.SetIteratorPrototype, setIteratorPrototype);
        intrinsics.put(Intrinsics.StopIteration, stopIterationObject);

        // binary module intrinsics
        intrinsics.put(Intrinsics.ArrayBuffer, arrayBufferConstructor);
        intrinsics.put(Intrinsics.ArrayBufferPrototype, arrayBufferPrototype);
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

        // Internationalization API
        intrinsics.put(Intrinsics.Intl, intlObject);
        intrinsics.put(Intrinsics.Intl_Collator, collatorConstructor);
        intrinsics.put(Intrinsics.Intl_CollatorPrototype, collatorPrototype);
        intrinsics.put(Intrinsics.Intl_NumberFormat, numberFormatConstructor);
        intrinsics.put(Intrinsics.Intl_NumberFormatPrototype, numberFormatPrototype);
        intrinsics.put(Intrinsics.Intl_DateTimeFormat, dateTimeFormatConstructor);
        intrinsics.put(Intrinsics.Intl_DateTimeFormatPrototype, dateTimeFormatPrototype);

        // internal intrinsics
        intrinsics.put(Intrinsics.ListIteratorPrototype, listIteratorPrototype);

        // create [[ThrowTypeError]] unique function (needs to be done before init'ing intrinsics)
        realm.throwTypeError = OrdinaryFunction.createThrowTypeError(realm);

        // intrinsics (3)
        objectConstructor.initialise(realm);
        objectPrototype.initialise(realm);
        functionConstructor.initialise(realm);
        functionPrototype.initialise(realm);
        arrayConstructor.initialise(realm);
        arrayPrototype.initialise(realm);
        arrayIteratorPrototype.initialise(realm);
        stringConstructor.initialise(realm);
        stringPrototype.initialise(realm);
        booleanConstructor.initialise(realm);
        booleanPrototype.initialise(realm);
        numberConstructor.initialise(realm);
        numberPrototype.initialise(realm);
        mathObject.initialise(realm);
        dateConstructor.initialise(realm);
        datePrototype.initialise(realm);
        regExpConstructor.initialise(realm);
        regExpPrototype.initialise(realm);
        errorConstructor.initialise(realm);
        errorPrototype.initialise(realm);
        jsonObject.initialise(realm);
        mapConstructor.initialise(realm);
        mapPrototype.initialise(realm);
        mapIteratorPrototype.initialise(realm);
        weakMapConstructor.initialise(realm);
        weakMapPrototype.initialise(realm);
        setConstructor.initialise(realm);
        setPrototype.initialise(realm);
        setIteratorPrototype.initialise(realm);
        stopIterationObject.initialise(realm);

        // binary module intrinsics
        arrayBufferConstructor.initialise(realm);
        arrayBufferPrototype.initialise(realm);
        int8ArrayConstructor.initialise(realm);
        int8ArrayPrototype.initialise(realm);
        uint8ArrayConstructor.initialise(realm);
        uint8ArrayPrototype.initialise(realm);
        uint8CArrayConstructor.initialise(realm);
        uint8CArrayPrototype.initialise(realm);
        int16ArrayConstructor.initialise(realm);
        int16ArrayPrototype.initialise(realm);
        uint16ArrayConstructor.initialise(realm);
        uint16ArrayPrototype.initialise(realm);
        int32ArrayConstructor.initialise(realm);
        int32ArrayPrototype.initialise(realm);
        uint32ArrayConstructor.initialise(realm);
        uint32ArrayPrototype.initialise(realm);
        float32ArrayConstructor.initialise(realm);
        float32ArrayPrototype.initialise(realm);
        float64ArrayConstructor.initialise(realm);
        float64ArrayPrototype.initialise(realm);
        dataViewConstructor.initialise(realm);
        dataViewPrototype.initialise(realm);

        // Internationalization API
        intlObject.initialise(realm);
        collatorConstructor.initialise(realm);
        collatorPrototype.initialise(realm);
        numberFormatConstructor.initialise(realm);
        numberFormatPrototype.initialise(realm);
        dateTimeFormatConstructor.initialise(realm);
        dateTimeFormatPrototype.initialise(realm);

        // internal intrinsics
        listIteratorPrototype.initialise(realm);

        // intrinsics (4)
        Object objectPrototypeToString = Get(objectPrototype, "toString");
        intrinsics.put(Intrinsics.ObjProto_toString, (Scriptable) objectPrototypeToString);

        // store reference to native errors
        for (NativeError.ErrorType type : NativeError.ErrorType.values()) {
            realm.errors.put(type, NativeError.create(realm, type));
        }

        // finish initialising global object
        globalThis.initialise(realm);

        // store reference to built-in eval
        realm.builtinEval = (Callable) Get(globalThis, "eval");

        return realm;
    }
}
