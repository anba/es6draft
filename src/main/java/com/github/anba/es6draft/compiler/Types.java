/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.DebugInfo;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;
import com.github.anba.es6draft.runtime.modules.ModuleExport;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSemantics;
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
import com.github.anba.es6draft.runtime.types.builtins.ArgumentsObject;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.LegacyArgumentsObject;
import com.github.anba.es6draft.runtime.types.builtins.ModuleNamespaceObject;
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
    static final Type Boolean = Type.of(Boolean.class);
    static final Type Byte = Type.of(Byte.class);
    static final Type Character = Type.of(Character.class);
    static final Type CharSequence = Type.of(CharSequence.class);
    static final Type Class = Type.of(Class.class);
    static final Type Double = Type.of(Double.class);
    static final Type Error = Type.of(Error.class);
    static final Type Float = Type.of(Float.class);
    static final Type IllegalStateException = Type.of(IllegalStateException.class);
    static final Type Integer = Type.of(Integer.class);
    static final Type Long = Type.of(Long.class);
    static final Type Math = Type.of(Math.class);
    static final Type Number = Type.of(Number.class);
    static final Type Object = Type.of(Object.class);
    static final Type Object_ = Type.of(Object[].class);
    static final Type Short = Type.of(Short.class);
    static final Type StackOverflowError = Type.of(StackOverflowError.class);
    static final Type String = Type.of(String.class);
    static final Type String_ = Type.of(String[].class);
    static final Type StringBuilder = Type.of(StringBuilder.class);
    static final Type Throwable = Type.of(Throwable.class);
    static final Type Void = Type.of(Void.class);

    // java.lang.invoke
    static final Type MethodHandle = Type.of(MethodHandle.class);

    // java.util
    static final Type ArrayList = Type.of(ArrayList.class);
    static final Type Arrays = Type.of(Arrays.class);
    static final Type Iterator = Type.of(Iterator.class);
    static final Type List = Type.of(List.class);
    static final Type Map = Type.of(Map.class);

    // compiler
    static final Type CompiledFunction = Type.of(CompiledFunction.class);
    static final Type CompiledModule = Type.of(CompiledModule.class);
    static final Type CompiledScript = Type.of(CompiledScript.class);

    // runtime
    static final Type AbstractOperations = Type.of(AbstractOperations.class);
    static final Type DeclarativeEnvironmentRecord = Type.of(DeclarativeEnvironmentRecord.class);
    static final Type DeclarativeEnvironmentRecord$Binding = Type
            .of(DeclarativeEnvironmentRecord.Binding.class);
    static final Type EnvironmentRecord = Type.of(EnvironmentRecord.class);
    static final Type ExecutionContext = Type.of(ExecutionContext.class);
    static final Type FunctionEnvironmentRecord = Type.of(FunctionEnvironmentRecord.class);
    static final Type GlobalEnvironmentRecord = Type.of(GlobalEnvironmentRecord.class);
    static final Type LexicalEnvironment = Type.of(LexicalEnvironment.class);
    static final Type ModuleEnvironmentRecord = Type.of(ModuleEnvironmentRecord.class);
    static final Type Realm = Type.of(Realm.class);
    static final Type ScriptRuntime = Type.of(ScriptRuntime.class);

    // runtime.modules
    static final Type ModuleExport = Type.of(ModuleExport.class);
    static final Type ModuleRecord = Type.of(ModuleRecord.class);
    static final Type ModuleSemantics = Type.of(ModuleSemantics.class);

    // runtime.objects
    static final Type Eval = Type.of(Eval.class);
    static final Type PromiseObject = Type.of(PromiseObject.class);
    static final Type RegExpConstructor = Type.of(RegExpConstructor.class);
    static final Type RegExpObject = Type.of(RegExpObject.class);

    // runtime.objects.iteration
    static final Type GeneratorObject = Type.of(GeneratorObject.class);

    // runtime.types
    static final Type Callable = Type.of(Callable.class);
    static final Type Intrinsics = Type.of(Intrinsics.class);
    static final Type Null = Type.of(Null.class);
    static final Type Reference = Type.of(Reference.class);
    static final Type ScriptObject = Type.of(ScriptObject.class);
    static final Type ScriptObject_ = Type.of(ScriptObject[].class);
    static final Type Symbol = Type.of(Symbol.class);
    static final Type _Type = Type.of(com.github.anba.es6draft.runtime.types.Type.class);
    static final Type Undefined = Type.of(Undefined.class);

    // runtime.types.builtins
    static final Type ArgumentsObject = Type.of(ArgumentsObject.class);
    static final Type ArrayObject = Type.of(ArrayObject.class);
    static final Type FunctionObject = Type.of(FunctionObject.class);
    static final Type LegacyArgumentsObject = Type.of(LegacyArgumentsObject.class);
    static final Type ModuleNamespaceObject = Type.of(ModuleNamespaceObject.class);
    static final Type OrdinaryAsyncFunction = Type.of(OrdinaryAsyncFunction.class);
    static final Type OrdinaryGenerator = Type.of(OrdinaryGenerator.class);
    static final Type OrdinaryFunction = Type.of(OrdinaryFunction.class);
    static final Type OrdinaryObject = Type.of(OrdinaryObject.class);

    // runtime.internal
    static final Type DebugInfo = Type.of(DebugInfo.class);
    static final Type ResumptionPoint = Type.of(ResumptionPoint.class);
    static final Type ReturnValue = Type.of(ReturnValue.class);
    static final Type RuntimeInfo = Type.of(RuntimeInfo.class);
    static final Type RuntimeInfo$Function = Type.of(RuntimeInfo.Function.class);
    static final Type RuntimeInfo$ModuleBody = Type.of(RuntimeInfo.ModuleBody.class);
    static final Type RuntimeInfo$ScriptBody = Type.of(RuntimeInfo.ScriptBody.class);
    static final Type ScriptException = Type.of(ScriptException.class);
    static final Type ScriptIterator = Type.of(ScriptIterator.class);
}
