/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Function;
import com.github.anba.es6draft.runtime.types.Generator;
import com.github.anba.es6draft.runtime.types.Null;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArguments;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 *
 */
final class Types {
    private Types() {
    }

    // java.lang
    static Type Boolean = Type.getType(Boolean.class);
    static Type CharSequence = Type.getType(CharSequence.class);
    static Type Double = Type.getType(Double.class);
    static Type Integer = Type.getType(Integer.class);
    static Type Object = Type.getType(Object.class);
    static Type Object_ = Type.getType(Object[].class);
    static Type String = Type.getType(String.class);
    static Type String_ = Type.getType(String[].class);
    static Type StringBuilder = Type.getType(StringBuilder.class);
    static Type Throwable = Type.getType(Throwable.class);

    // java.lang.invoke
    static Type MethodHandle = Type.getType(MethodHandle.class);

    // java.util
    static Type ArrayList = Type.getType(ArrayList.class);
    static Type HashMap = Type.getType(HashMap.class);
    static Type Iterator = Type.getType(Iterator.class);
    static Type List = Type.getType(List.class);
    static Type Map = Type.getType(Map.class);

    // es6draft.compiler
    static Type CompiledScript = Type.getType(CompiledScript.class);

    // es6draft.runtime
    static Type AbstractOperations = Type.getType(AbstractOperations.class);
    static Type EnvironmentRecord = Type.getType(EnvironmentRecord.class);
    static Type ExecutionContext = Type.getType(ExecutionContext.class);
    static Type GlobalEnvironmentRecord = Type.getType(GlobalEnvironmentRecord.class);
    static Type LexicalEnvironment = Type.getType(LexicalEnvironment.class);
    static Type Realm = Type.getType(Realm.class);
    static Type ScriptRuntime = Type.getType(ScriptRuntime.class);

    // es6draft.runtime.objects
    static Type Eval = Type.getType(Eval.class);

    // es6draft.runtime.types
    static Type Callable = Type.getType(Callable.class);
    static Type Function = Type.getType(Function.class);
    static Type Generator = Type.getType(Generator.class);
    static Type Null = Type.getType(Null.class);
    static Type Reference = Type.getType(Reference.class);
    static Type Scriptable = Type.getType(Scriptable.class);
    static Type Scriptable_ = Type.getType(Scriptable[].class);
    static Type _Type = Type.getType(com.github.anba.es6draft.runtime.types.Type.class);
    static Type Undefined = Type.getType(Undefined.class);

    // es6draft.runtime.types.builtins
    static Type ExoticArguments = Type.getType(ExoticArguments.class);
    static Type ExoticArray = Type.getType(ExoticArray.class);
    static Type OrdinaryGenerator = Type.getType(OrdinaryGenerator.class);
    static Type OrdinaryFunction = Type.getType(OrdinaryFunction.class);
    static Type OrdinaryObject = Type.getType(OrdinaryObject.class);

    // es6draft.runtime.internal
    static Type RuntimeInfo = Type.getType(RuntimeInfo.class);
    static Type RuntimeInfo$Code = Type.getType(RuntimeInfo.Code.class);
    static Type RuntimeInfo$Function = Type.getType(RuntimeInfo.Function.class);
    static Type RuntimeInfo$ScriptBody = Type.getType(RuntimeInfo.ScriptBody.class);
    static Type ScriptException = Type.getType(ScriptException.class);

}
