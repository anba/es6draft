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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.runtime.*;
import com.github.anba.es6draft.runtime.internal.*;
import com.github.anba.es6draft.runtime.modules.ModuleExport;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSemantics;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.objects.async.AsyncAbstractOperations;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.objects.text.RegExpConstructor;
import com.github.anba.es6draft.runtime.objects.text.RegExpObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Null;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.*;

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
    static final Type Collections = Type.of(Collections.class);
    static final Type HashSet = Type.of(HashSet.class);
    static final Type Iterator = Type.of(Iterator.class);
    static final Type List = Type.of(List.class);
    static final Type Map = Type.of(Map.class);
    static final Type Set = Type.of(Set.class);

    // toplevel
    static final Type Script = Type.of(Script.class);

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
    static final Type ObjectEnvironmentRecord = Type.of(ObjectEnvironmentRecord.class);
    static final Type Realm = Type.of(Realm.class);
    static final Type ScriptRuntime = Type.of(ScriptRuntime.class);

    // runtime.modules
    static final Type ModuleExport = Type.of(ModuleExport.class);
    static final Type ModuleRecord = Type.of(ModuleRecord.class);
    static final Type ModuleSemantics = Type.of(ModuleSemantics.class);
    static final Type SourceTextModuleRecord = Type.of(SourceTextModuleRecord.class);

    // runtime.objects
    static final Type Eval = Type.of(Eval.class);
    static final Type PromiseObject = Type.of(PromiseObject.class);
    static final Type RegExpConstructor = Type.of(RegExpConstructor.class);
    static final Type RegExpObject = Type.of(RegExpObject.class);

    // runtime.objects.async
    static final Type AsyncAbstractOperations = Type.of(AsyncAbstractOperations.class);

    // runtime.objects.iteration
    static final Type GeneratorObject = Type.of(GeneratorObject.class);

    // runtime.objects.promise
    static final Type PromiseAbstractOperations = Type.of(PromiseAbstractOperations.class);

    // runtime.types
    static final Type Callable = Type.of(Callable.class);
    static final Type Constructor = Type.of(Constructor.class);
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
    static final Type FunctionObject$ConstructorKind = Type
            .of(FunctionObject.ConstructorKind.class);
    static final Type LegacyArgumentsObject = Type.of(LegacyArgumentsObject.class);
    static final Type LegacyConstructorFunction = Type.of(LegacyConstructorFunction.class);
    static final Type ModuleNamespaceObject = Type.of(ModuleNamespaceObject.class);
    static final Type OrdinaryAsyncFunction = Type.of(OrdinaryAsyncFunction.class);
    static final Type OrdinaryConstructorFunction = Type.of(OrdinaryConstructorFunction.class);
    static final Type OrdinaryFunction = Type.of(OrdinaryFunction.class);
    static final Type OrdinaryGenerator = Type.of(OrdinaryGenerator.class);
    static final Type OrdinaryObject = Type.of(OrdinaryObject.class);

    // runtime.internal
    static final Type DebugInfo = Type.of(DebugInfo.class);
    static final Type Errors = Type.of(Errors.class);
    static final Type Messages$Key = Type.of(Messages.Key.class);
    static final Type ResumptionPoint = Type.of(ResumptionPoint.class);
    static final Type ReturnValue = Type.of(ReturnValue.class);
    static final Type RuntimeInfo = Type.of(RuntimeInfo.class);
    static final Type RuntimeInfo$Function = Type.of(RuntimeInfo.Function.class);
    static final Type RuntimeInfo$ModuleBody = Type.of(RuntimeInfo.ModuleBody.class);
    static final Type RuntimeInfo$ScriptBody = Type.of(RuntimeInfo.ScriptBody.class);
    static final Type ScriptException = Type.of(ScriptException.class);
    static final Type ScriptIterator = Type.of(ScriptIterator.class);
    static final Type TailCallInvocation = Type.of(TailCallInvocation.class);
}
