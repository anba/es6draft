/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.objects.text.RegExpConstructor;
import com.github.anba.es6draft.runtime.objects.text.RegExpObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Null;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArguments;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;
import com.github.anba.es6draft.runtime.types.builtins.ExoticLegacyArguments;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction;
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
    static final Type Boolean = Type.getType(Boolean.class);
    static final Type Byte = Type.getType(Byte.class);
    static final Type Character = Type.getType(Character.class);
    static final Type CharSequence = Type.getType(CharSequence.class);
    static final Type Class = Type.getType(Class.class);
    static final Type Double = Type.getType(Double.class);
    static final Type Error = Type.getType(Error.class);
    static final Type Float = Type.getType(Float.class);
    static final Type IllegalStateException = Type.getType(IllegalStateException.class);
    static final Type Integer = Type.getType(Integer.class);
    static final Type Long = Type.getType(Long.class);
    static final Type Math = Type.getType(Math.class);
    static final Type Number = Type.getType(Number.class);
    static final Type Object = Type.getType(Object.class);
    static final Type Object_ = Type.getType(Object[].class);
    static final Type Short = Type.getType(Short.class);
    static final Type StackOverflowError = Type.getType(StackOverflowError.class);
    static final Type String = Type.getType(String.class);
    static final Type String_ = Type.getType(String[].class);
    static final Type StringBuilder = Type.getType(StringBuilder.class);
    static final Type Throwable = Type.getType(Throwable.class);
    static final Type Void = Type.getType(Void.class);

    // java.lang.invoke
    static final Type MethodHandle = Type.getType(MethodHandle.class);
    static final Type MethodType = Type.getType(MethodType.class);

    // java.util
    static final Type ArrayList = Type.getType(ArrayList.class);
    static final Type Arrays = Type.getType(Arrays.class);
    static final Type Iterator = Type.getType(Iterator.class);
    static final Type List = Type.getType(List.class);

    // compiler
    static final Type CompiledFunction = Type.getType(CompiledFunction.class);
    static final Type CompiledScript = Type.getType(CompiledScript.class);

    // runtime
    static final Type AbstractOperations = Type.getType(AbstractOperations.class);
    static final Type DeclarativeEnvironmentRecord = Type
            .getType(DeclarativeEnvironmentRecord.class);
    static final Type DeclarativeEnvironmentRecord$Binding = Type
            .getType(DeclarativeEnvironmentRecord.Binding.class);
    static final Type EnvironmentRecord = Type.getType(EnvironmentRecord.class);
    static final Type ExecutionContext = Type.getType(ExecutionContext.class);
    static final Type GlobalEnvironmentRecord = Type.getType(GlobalEnvironmentRecord.class);
    static final Type LexicalEnvironment = Type.getType(LexicalEnvironment.class);
    static final Type Realm = Type.getType(Realm.class);
    static final Type ScriptRuntime = Type.getType(ScriptRuntime.class);

    // runtime.objects
    static final Type Eval = Type.getType(Eval.class);
    static final Type PromiseObject = Type.getType(PromiseObject.class);
    static final Type RegExpConstructor = Type.getType(RegExpConstructor.class);
    static final Type RegExpObject = Type.getType(RegExpObject.class);

    // runtime.objects.iteration
    static final Type GeneratorObject = Type.getType(GeneratorObject.class);

    // runtime.types
    static final Type Callable = Type.getType(Callable.class);
    static final Type Intrinsics = Type.getType(Intrinsics.class);
    static final Type Null = Type.getType(Null.class);
    static final Type Reference = Type.getType(Reference.class);
    static final Type ScriptObject = Type.getType(ScriptObject.class);
    static final Type ScriptObject_ = Type.getType(ScriptObject[].class);
    static final Type Symbol = Type.getType(Symbol.class);
    static final Type _Type = Type.getType(com.github.anba.es6draft.runtime.types.Type.class);
    static final Type Undefined = Type.getType(Undefined.class);

    // runtime.types.builtins
    static final Type ExoticArguments = Type.getType(ExoticArguments.class);
    static final Type ExoticLegacyArguments = Type.getType(ExoticLegacyArguments.class);
    static final Type ExoticArray = Type.getType(ExoticArray.class);
    static final Type FunctionObject = Type.getType(FunctionObject.class);
    static final Type OrdinaryAsyncFunction = Type.getType(OrdinaryAsyncFunction.class);
    static final Type OrdinaryGenerator = Type.getType(OrdinaryGenerator.class);
    static final Type OrdinaryFunction = Type.getType(OrdinaryFunction.class);
    static final Type OrdinaryObject = Type.getType(OrdinaryObject.class);

    // runtime.internal
    static final Type ResumptionPoint = Type.getType(ResumptionPoint.class);
    static final Type ReturnValue = Type.getType(ReturnValue.class);
    static final Type RuntimeInfo = Type.getType(RuntimeInfo.class);
    static final Type RuntimeInfo$Function = Type.getType(RuntimeInfo.Function.class);
    static final Type RuntimeInfo$ScriptBody = Type.getType(RuntimeInfo.ScriptBody.class);
    static final Type ScriptException = Type.getType(ScriptException.class);
    static final Type ScriptIterator = Type.getType(ScriptIterator.class);
}
