/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.DeletePropertyOrThrow;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetMethod;
import static com.github.anba.es6draft.runtime.AbstractOperations.SetIntegrityLevel;
import static com.github.anba.es6draft.runtime.Realm.CreateRealmAndSetRealmGlobalObject;
import static com.github.anba.es6draft.runtime.Realm.SetDefaultGlobalBindings;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.ProxyObject.ProxyCreate;

import java.util.HashSet;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Permission;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.? Realm Objects</h2>
 * <ul>
 * <li>26.?.1 The Reflect.Realm Constructor
 * <li>26.?.2 Properties of the Reflect.Realm Constructor
 * </ul>
 */
public final class RealmConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Realm constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public RealmConstructor(Realm realm) {
        super(realm, "Realm", 0);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, FrozenProperties.class);
    }

    /**
     * IndirectEval (realm, source)
     * 
     * @param caller
     *            the caller context
     * @param realm
     *            the realm instance
     * @param source
     *            the source string
     * @return the evaluation result
     */
    public static Object IndirectEval(ExecutionContext caller, Realm realm, Object source) {
        return Eval.globalEval(realm.defaultContext(), caller, source);
    }

    /**
     * 26.?.1.1 Reflect.Realm ( [ target , handler ] )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "Realm");
    }

    /**
     * 26.?.1.1 Reflect.Realm ( [ target , handler ] )
     */
    @Override
    public RealmObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();

        // Disable Realm constructor if only FrozenRealm option is enabled.
        if (!getRealm().getRuntimeContext().isEnabled(CompatibilityOption.Realm)) {
            throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "Realm");
        }

        /* steps 2-3 */
        RealmObject realmObject = OrdinaryCreateFromConstructor(calleeContext, newTarget, Intrinsics.RealmPrototype,
                RealmObject::new);

        /* steps 4-5 */
        ScriptObject newGlobal;
        if (args.length != 0) {
            /* step 4 */
            Object target = argument(args, 0);
            Object handler = argument(args, 1);
            newGlobal = ProxyCreate(calleeContext, target, handler);
        } else {
            /* step 5 */
            newGlobal = null;
        }

        /* steps 6-7, 17 (Moved before extracting extension hooks to avoid uninitialized object state) */
        Realm realm = CreateRealmAndSetRealmGlobalObject(calleeContext, realmObject, newGlobal, newGlobal);
        /* steps 8-9 */
        Callable translate = GetMethod(calleeContext, realmObject, "directEval");
        /* steps 10-11 */
        Callable fallback = GetMethod(calleeContext, realmObject, "nonEval");
        /* steps 12-13 */
        Callable indirectEval = GetMethod(calleeContext, realmObject, "indirectEval");
        /* steps 14-16 */
        realm.setExtensionHooks(translate, fallback, indirectEval);

        /* steps 18-19 */
        Callable initGlobal = GetMethod(calleeContext, realmObject, "initGlobal");
        /* steps 20-21 */
        if (initGlobal != null) {
            /* step 20 */
            initGlobal.call(calleeContext, realmObject);
        } else {
            /* step 21 */
            SetDefaultGlobalBindings(calleeContext, realm);
        }
        /* step 22 */
        return realmObject;
    }

    /**
     * 26.?.2 Properties of the Reflect.Realm Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "Realm";

        /**
         * 26.?.2.1 Reflect.Realm.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.RealmPrototype;
    }

    /**
     * Extension: Frozen Realms
     */
    @CompatibilityExtension(CompatibilityOption.FrozenRealm)
    public enum FrozenProperties {
        ;

        private static void removePermissions(Realm realm) {
            realm.revoke(Permission.CurrentTime);
            realm.revoke(Permission.RandomNumber);
        }

        private static void removeStatefulBuiltins(Realm realm) {
            ExecutionContext cx = realm.defaultContext();
            DeletePropertyOrThrow(cx, realm.getIntrinsic(Intrinsics.Date), "now");
            DeletePropertyOrThrow(cx, realm.getIntrinsic(Intrinsics.Math), "random");
            if (realm.getRuntimeContext().isEnabled(CompatibilityOption.RegExpStatics)) {
                OrdinaryObject regExp = realm.getIntrinsic(Intrinsics.RegExp);
                for (String propertyKey : new String[] { "$_", "input", "$&", "lastMatch", "$+", "lastParen", "$`",
                        "leftContext", "$'", "rightContext", "$1", "$2", "$3", "$4", "$5", "$6", "$7", "$8", "$9" }) {
                    DeletePropertyOrThrow(cx, regExp, propertyKey);
                }
            }
            for (String propertyKey : new String[] { "fileName", "lineNumber", "columnNumber", "stack",
                    "stackTrace" }) {
                DeletePropertyOrThrow(cx, realm.getIntrinsic(Intrinsics.ErrorPrototype), propertyKey);
            }
            if (realm.getRuntimeContext().isEnabled(CompatibilityOption.WeakReference)) {
                DeletePropertyOrThrow(cx, realm.getIntrinsic(Intrinsics.System), "makeWeakRef");
            }
            if (realm.getRuntimeContext().isEnabled(CompatibilityOption.System)) {
                for (String propertyKey : new String[] { "define", "import", "load", "get", "normalize" }) {
                    DeletePropertyOrThrow(cx, realm.getIntrinsic(Intrinsics.System), propertyKey);
                }
            }
        }

        private static void immutable(Realm realm) {
            ExecutionContext cx = realm.defaultContext();
            HashSet<ScriptObject> visited = new HashSet<>();
            for (Intrinsics id : Intrinsics.values()) {
                ScriptObject obj = realm.getIntrinsic(id);
                if (obj != null) {
                    immutable(cx, obj, visited);
                }
            }
            immutable(cx, realm.getGlobalThis(), visited);
            immutable(cx, realm.getGlobalObject(), visited);
            immutable(cx, realm.getRealmObject(), visited);
        }

        private static void immutable(ExecutionContext cx, ScriptObject obj, HashSet<ScriptObject> visited) {
            if (!visited.add(obj)) {
                return;
            }
            if (!SetIntegrityLevel(cx, obj, IntegrityLevel.Frozen)) {
                throw newTypeError(cx, Messages.Key.ObjectFreezeFailed);
            }
            ScriptObject prototype = obj.getPrototypeOf(cx);
            if (prototype != null) {
                immutable(cx, prototype, visited);
            }
            for (Object propertyKey : obj.ownPropertyKeys(cx)) {
                Property property = obj.getOwnProperty(cx, propertyKey);
                if (property != null) {
                    if (property.isDataDescriptor()) {
                        Object value = property.getValue();
                        if (Type.isObject(value)) {
                            immutable(cx, Type.objectValue(value), visited);
                        }
                    } else {
                        Callable getter = property.getGetter();
                        if (getter != null) {
                            immutable(cx, getter, visited);
                        }
                        Callable setter = property.getSetter();
                        if (setter != null) {
                            immutable(cx, setter, visited);
                        }
                    }
                }
            }
        }

        /**
         * Reflect.Realm.immutableRoot ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the evaluation result
         */
        @Function(name = "immutableRoot", arity = 0)
        public static Object immutableRoot(ExecutionContext cx, Object thisValue) {
            RealmObject realmObject = new RealmObject(cx.getRealm());

            // Request default global object.
            ScriptObject newGlobal = null;
            Realm realm = CreateRealmAndSetRealmGlobalObject(cx, realmObject, newGlobal, newGlobal);

            // Set [[Prototype]] to the new Realm.prototype.
            realmObject.setPrototypeOf(cx, realm.getIntrinsic(Intrinsics.RealmPrototype));

            // Install the default global bindings.
            SetDefaultGlobalBindings(cx, realm);

            // Make realm immutable.
            removePermissions(realm);
            removeStatefulBuiltins(realm);
            immutable(realm);

            return realmObject;
        }
    }
}
