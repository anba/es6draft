/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.OrdinaryCreateFromConstructor;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.nio.file.Paths;
import java.util.EnumSet;

import org.junit.Before;
import org.junit.Test;

import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.FileModuleLoader;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Null;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 *
 */
public final class PropertiesTest {
    private GlobalObject global;
    private ExecutionContext cx;

    @Before
    public void setUp() throws Throwable {
        ObjectAllocator<GlobalObject> allocator = World.getDefaultGlobalObjectAllocator();
        ScriptLoader scriptLoader = new ScriptLoader(CompatibilityOption.WebCompatibility(),
                EnumSet.noneOf(Parser.Option.class), EnumSet.noneOf(Compiler.Option.class));
        ModuleLoader moduleLoader = new FileModuleLoader(scriptLoader, Paths.get("")
                .toAbsolutePath());

        World<GlobalObject> world = new World<>(allocator, moduleLoader, scriptLoader);
        global = world.newInitializedGlobal();
        cx = global.getRealm().defaultContext();
    }

    @Test
    public void createEmptyClass() {
        Constructor emptyClass = Properties.createClass(cx, "EmptyClass",
                EmptyClass.ConstructorProperties.class, EmptyClass.PrototypeProperties.class);
        assertNotNull(emptyClass);
        assertNotNull(emptyClass.getOwnProperty(cx, "prototype"));
    }

    @Test(expected = ScriptException.class)
    public void createEmptyClassAndCall() {
        Constructor emptyClass = Properties.createClass(cx, "EmptyClass",
                EmptyClass.ConstructorProperties.class, EmptyClass.PrototypeProperties.class);
        emptyClass.call(cx, Null.NULL);
    }

    @Test
    public void createEmptyClassAndConstruct() {
        Constructor emptyClass = Properties.createClass(cx, "EmptyClass",
                EmptyClass.ConstructorProperties.class, EmptyClass.PrototypeProperties.class);

        ScriptObject object = emptyClass.construct(cx, emptyClass);
        assertNotNull(object);
        assertSame(emptyClass.get(cx, "prototype", emptyClass), object.getPrototypeOf(cx));
    }

    @Test
    public void createCustomClass() {
        Constructor customClass = Properties.createClass(cx, "CustomClass",
                CustomClass.ConstructorProperties.class, CustomClass.PrototypeProperties.class);
        ScriptObject object = customClass.construct(cx, customClass);
        assertNotNull(object);
    }

    @Test
    public void createCustomClassWithSubclass() {
        Constructor customClass = Properties.createClass(cx, "CustomClassWithSubclass",
                CustomClassWithSubclass.ConstructorProperties.class,
                CustomClassWithSubclass.PrototypeProperties.class);
        ScriptObject object = customClass.construct(cx, customClass);
        assertNotNull(object);
    }

    @Test
    public void createCustomClassWithObject() {
        Constructor customClass = Properties.createClass(cx, "CustomClassWithObject",
                CustomClassWithObject.ConstructorProperties.class,
                CustomClassWithObject.PrototypeProperties.class);
        ScriptObject object = customClass.construct(cx, customClass);
        assertNotNull(object);
    }

    @Test
    public void createCustomClassNoArgs() {
        Constructor customClass = Properties.createClass(cx, "CustomClassNoArgs",
                CustomClassNoArgs.ConstructorProperties.class,
                CustomClassNoArgs.PrototypeProperties.class);
        ScriptObject object = customClass.construct(cx, customClass);
        assertNotNull(object);
    }

    @Test
    public void createCustomClassWithCallerContext() {
        Constructor customClass = Properties.createClass(cx, "CustomClassWithCallerContext",
                CustomClassWithCallerContext.ConstructorProperties.class,
                CustomClassWithCallerContext.PrototypeProperties.class);
        ScriptObject object = customClass.construct(cx, customClass);
        assertNotNull(object);
    }

    public static final class EmptyClass {
        public enum ConstructorProperties {
            ;
        }

        public enum PrototypeProperties {
            ;
        }
    }

    public static final class CustomClass {
        public enum ConstructorProperties {
            ;
        }

        public enum PrototypeProperties {
            ;

            @Function(name = "constructor", arity = 0)
            public static ScriptObject constructor(ExecutionContext cx, Constructor newTarget,
                    Object thisValue, Object... args) {
                assertNotNull(newTarget);
                assertNull(thisValue);
                return OrdinaryCreateFromConstructor(cx, newTarget, Intrinsics.ObjectPrototype);
            }
        }
    }

    public static final class CustomClassWithSubclass {
        public enum ConstructorProperties {
            ;
        }

        public enum PrototypeProperties {
            ;

            @Function(name = "constructor", arity = 0)
            public static OrdinaryObject constructor(ExecutionContext cx, Constructor newTarget,
                    Object thisValue, Object... args) {
                assertNotNull(newTarget);
                assertNull(thisValue);
                return OrdinaryCreateFromConstructor(cx, newTarget, Intrinsics.ObjectPrototype);
            }
        }
    }

    public static final class CustomClassWithObject {
        public enum ConstructorProperties {
            ;
        }

        public enum PrototypeProperties {
            ;

            @Function(name = "constructor", arity = 0)
            public static Object constructor(ExecutionContext cx, Constructor newTarget,
                    Object thisValue, Object... args) {
                assertNotNull(newTarget);
                assertNull(thisValue);
                return OrdinaryCreateFromConstructor(cx, newTarget, Intrinsics.ObjectPrototype);
            }
        }
    }

    public static final class CustomClassNoArgs {
        public enum ConstructorProperties {
            ;
        }

        public enum PrototypeProperties {
            ;

            @Function(name = "constructor", arity = 0)
            public static ScriptObject constructor(ExecutionContext cx, Constructor newTarget,
                    Object thisValue) {
                assertNotNull(newTarget);
                assertNull(thisValue);
                return OrdinaryCreateFromConstructor(cx, newTarget, Intrinsics.ObjectPrototype);
            }
        }
    }

    public static final class CustomClassWithCallerContext {
        public enum ConstructorProperties {
            ;
        }

        public enum PrototypeProperties {
            ;

            @Function(name = "constructor", arity = 0)
            public static ScriptObject constructor(ExecutionContext cx, ExecutionContext caller,
                    Constructor newTarget, Object thisValue, Object... args) {
                assertNotNull(newTarget);
                assertNull(thisValue);
                return OrdinaryCreateFromConstructor(cx, newTarget, Intrinsics.ObjectPrototype);
            }
        }
    }
}
