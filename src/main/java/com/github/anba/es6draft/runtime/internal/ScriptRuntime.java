/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newDeclarativeEnvironment;
import static com.github.anba.es6draft.runtime.internal.Errors.*;
import static com.github.anba.es6draft.runtime.internal.TailCallInvocation.newTailCallInvocation;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.GetModuleNamespace;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.HostResolveImportedModule;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.AccessorPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.LegacyConstructorFunction.LegacyFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction.AsyncFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncGenerator.AsyncGeneratorFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction.ConstructorFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorGenerator.ConstructorGeneratorFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.*;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.GeneratorFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.ConsString;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.compiler.CompiledObject;
import com.github.anba.es6draft.compiler.CompiledScript;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.ObjectEnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleExport;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.ArrayPrototype;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.objects.async.iteration.AsyncGeneratorAbstractOperations;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayPrototypePrototype;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.objects.simd.SIMDType;
import com.github.anba.es6draft.runtime.types.*;
import com.github.anba.es6draft.runtime.types.builtins.*;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.ConstructorKind;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;

/**
 * Runtime support methods
 */
public final class ScriptRuntime {
    public static final Object[] EMPTY_ARRAY = new Object[0];

    private ScriptRuntime() {
    }

    /**
     * 9.2.2.2 OrdinaryCallBindThis ( F, calleeContext, thisArgument )
     * 
     * @param f
     *            the function object
     * @param thisArgument
     *            the thisArgument
     * @return the thisValue
     */
    public static ScriptObject functionThisValue(FunctionObject f, Object thisArgument) {
        Realm calleeRealm = f.getRealm();
        if (Type.isUndefinedOrNull(thisArgument)) {
            return calleeRealm.getGlobalThis();
        }
        return ToObject(calleeRealm.defaultContext(), thisArgument);
    }

    /**
     * 18.2.1.2 EvalDeclarationInstantiation
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     */
    public static void bindingNotPresentOrThrow(ExecutionContext cx, EnvironmentRecord envRec, String name) {
        if (envRec.hasBinding(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     */
    public static void canDeclareLexicalScopedOrThrow(ExecutionContext cx, GlobalEnvironmentRecord envRec,
            String name) {
        /* step 5.a */
        if (envRec.hasVarDeclaration(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
        /* step 5.b */
        if (envRec.hasLexicalDeclaration(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
        /* step 5.c */
        if (envRec.hasRestrictedGlobalProperty(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     */
    public static void canDeclareVarScopedOrThrow(ExecutionContext cx, GlobalEnvironmentRecord envRec, String name) {
        /* step 6.a */
        if (envRec.hasLexicalDeclaration(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param fn
     *            the function name
     */
    public static void canDeclareGlobalFunctionOrThrow(ExecutionContext cx, GlobalEnvironmentRecord envRec, String fn) {
        /* steps 10.a.iv.1-2 */
        boolean fnDefinable = envRec.canDeclareGlobalFunction(fn);
        if (!fnDefinable) {
            throw newTypeError(cx, Messages.Key.InvalidDeclaration, fn);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param vn
     *            the variable name
     */
    public static void canDeclareGlobalVarOrThrow(ExecutionContext cx, GlobalEnvironmentRecord envRec, String vn) {
        /* steps 12.a.i.1.a-c */
        boolean vnDefinable = envRec.canDeclareGlobalVar(vn);
        if (!vnDefinable) {
            throw newTypeError(cx, Messages.Key.InvalidDeclaration, vn);
        }
    }

    /**
     * 18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation( body, varEnv, lexEnv, strict)
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param name
     *            the variable name
     * @param catchVar
     *            {@code true} if variable redeclarations are allowed in catch clauses
     */
    public static void canDeclareVarOrThrow(ExecutionContext cx, DeclarativeEnvironmentRecord envRec, String name,
            boolean catchVar) {
        /* steps 6.b.ii.2 - 6.b.ii.3 */
        if (envRec.hasBinding(name) && !(catchVar && envRec.isCatchEnvironment())) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation( body, varEnv, lexEnv, strict)
     * 
     * @param varEnv
     *            the variable environment
     * @param lexEnv
     *            the lexical environment
     * @param name
     *            the function name
     * @param catchVar
     *            {@code true} if variable redeclarations are allowed in catch clauses
     * @return {@code true} if the name can be declared
     */
    public static boolean canDeclareVarBinding(LexicalEnvironment<?> varEnv,
            LexicalEnvironment<DeclarativeEnvironmentRecord> lexEnv, String name, boolean catchVar) {
        for (LexicalEnvironment<?> thisEnv = lexEnv; thisEnv != varEnv; thisEnv = thisEnv.getOuter()) {
            EnvironmentRecord thisEnvRec = thisEnv.getEnvRec();
            if (thisEnvRec instanceof ObjectEnvironmentRecord) {
                continue;
            }
            DeclarativeEnvironmentRecord declEnvRec = (DeclarativeEnvironmentRecord) thisEnvRec;
            if (declEnvRec.hasBinding(name) && !(catchVar && declEnvRec.isCatchEnvironment())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation( body, varEnv, lexEnv, strict)
     * 
     * @param cx
     *            the execution context
     * @param functionId
     *            the function id
     */
    public static void setLegacyBlockFunction(ExecutionContext cx, int functionId) {
        Executable executable = cx.getCurrentExecutable();
        ((CompiledScript) executable).setLegacyBlockFunction(functionId);
    }

    /**
     * 18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation( body, varEnv, lexEnv, strict)
     * 
     * @param cx
     *            the execution context
     * @param functionId
     *            the function id
     * @return {@code true} if the funciton is legacy block-level scoped
     */
    public static boolean isLegacyBlockFunction(ExecutionContext cx, int functionId) {
        Executable executable = cx.getCurrentExecutable();
        return ((CompiledScript) executable).isLegacyBlockFunction(functionId);
    }

    /**
     * 15.2.1.16.4 ModuleDeclarationInstantiation( ) Concrete Method
     * 
     * @param module
     *            the module record
     * @param exportName
     *            the export name
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    public static void resolveExportOrThrow(SourceTextModuleRecord module, String exportName)
            throws IOException, MalformedNameException, ResolutionException {
        /* steps 6.a-b */
        ModuleExport resolution = module.resolveExport(exportName, new HashMap<>(), new HashSet<>());
        /* step 6.c */
        if (resolution == null) {
            throw new ResolutionException(Messages.Key.ModulesUnresolvedExport, exportName);
        }
        if (resolution.isAmbiguous()) {
            throw new ResolutionException(Messages.Key.ModulesAmbiguousExport, exportName);
        }
    }

    /**
     * 15.2.1.16.4 ModuleDeclarationInstantiation( ) Concrete Method
     * 
     * @param module
     *            the module record
     * @param moduleRequest
     *            the module specifier string
     * @param importName
     *            the import name
     * @return the resolved module import
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    public static ModuleExport resolveImportOrThrow(SourceTextModuleRecord module, String moduleRequest,
            String importName) throws IOException, MalformedNameException, ResolutionException {
        /* steps 10.a-b */
        ModuleRecord importedModule = HostResolveImportedModule(module, moduleRequest);
        /* steps 10.d.i-ii */
        ModuleExport resolution = importedModule.resolveExport(importName, new HashMap<>(), new HashSet<>());
        /* step 10.d.iii */
        if (resolution == null) {
            throw new ResolutionException(Messages.Key.ModulesUnresolvedImport, importName,
                    importedModule.getSourceCodeId().toString());
        }
        if (resolution.isAmbiguous()) {
            throw new ResolutionException(Messages.Key.ModulesAmbiguousImport, importName,
                    importedModule.getSourceCodeId().toString());
        }
        return resolution;
    }

    /**
     * 15.2.1.16.4 ModuleDeclarationInstantiation( ) Concrete Method
     * 
     * @param cx
     *            the execution context
     * @param module
     *            the module record
     * @param moduleRequest
     *            the module specifier string
     * @return the module namespace object
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    public static ScriptObject getModuleNamespace(ExecutionContext cx, SourceTextModuleRecord module,
            String moduleRequest) throws IOException, MalformedNameException, ResolutionException {
        /* steps 10.a-b */
        ModuleRecord importedModule = HostResolveImportedModule(module, moduleRequest);
        /* steps 10.c.i-ii */
        return GetModuleNamespace(cx, importedModule);
    }

    /**
     * 15.2.1.16.4 ModuleDeclarationInstantiation( ) Concrete Method
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the module environment record
     * @param name
     *            the import name
     * @param resolved
     *            the resolved module export
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    public static void createImportBinding(ExecutionContext cx, ModuleEnvironmentRecord envRec, String name,
            ModuleExport resolved) throws IOException, MalformedNameException, ResolutionException {
        assert !resolved.isAmbiguous();
        if (resolved.isNameSpaceExport()) {
            envRec.createImmutableBinding(name, true);
            envRec.initializeBinding(name, GetModuleNamespace(cx, resolved.getModule()));
        } else {
            envRec.createImportBinding(name, resolved.getModule(), resolved.getBindingName());
        }
    }

    /* ***************************************************************************************** */

    /**
     * 12.2.4.1 Array Literal
     * <p>
     * 12.2.4.1.2 Runtime Semantics: Array Accumulation
     * <ul>
     * <li>ElementList : Elision<span><sub>opt</sub></span> AssignmentExpression
     * <li>ElementList : ElementList , Elision<span><sub>opt</sub></span> AssignmentExpression
     * </ul>
     * 
     * @param array
     *            the array object
     * @param nextIndex
     *            the array index
     * @param value
     *            the array element value
     */
    public static void defineProperty(ArrayObject array, int nextIndex, Object value) {
        // Inlined: CreateDataProperty(array, ToString(ToUint32(nextIndex)), value);
        array.insert(nextIndex, value);
    }

    /**
     * 12.2.4.1 Array Literal
     * <p>
     * 12.2.4.1.3 Runtime Semantics: Evaluation
     * 
     * @param array
     *            the array object
     * @param length
     *            the array length value
     */
    public static void defineLength(ArrayObject array, int length) {
        // Set(cx, array, "length", length, false);
        array.setLengthUnchecked(length);
    }

    /**
     * 12.2.4.1 Array Literal
     * <p>
     * 12.2.4.1.2 Runtime Semantics: Array Accumulation
     * <ul>
     * <li>SpreadElement : ... AssignmentExpression
     * </ul>
     * 
     * @param array
     *            the array object
     * @param nextIndex
     *            the array index
     * @param spreadObj
     *            the spread element
     * @param cx
     *            the execution context
     * @return the next array index
     */
    public static int ArrayAccumulationSpreadElement(ArrayObject array, int nextIndex, Object spreadObj,
            ExecutionContext cx) {
        if (spreadObj instanceof OrdinaryObject) {
            OrdinaryObject object = (OrdinaryObject) spreadObj;
            long length = object.getLength();
            long newLength = nextIndex + length;
            if (0 <= length && newLength <= Integer.MAX_VALUE) {
                if (!(object instanceof TypedArrayObject) && isSpreadable(cx, object, length)) {
                    array.insertFrom(nextIndex, object, length);
                    return (int) newLength;
                }
                if (object instanceof TypedArrayObject && isSpreadable(cx, (TypedArrayObject) object)) {
                    array.insertFrom(cx, nextIndex, (TypedArrayObject) object);
                    return (int) newLength;
                }
            }
        }
        /* steps 1-2 (cf. generated code) */
        /* steps 3-4 */
        ScriptIterator<?> iterator = GetScriptIterator(cx, spreadObj);
        /* step 5 */
        while (iterator.hasNext()) {
            defineProperty(array, nextIndex, iterator.next());
            nextIndex += 1;
        }
        return nextIndex;
    }

    private static boolean isSpreadable(ExecutionContext cx, OrdinaryObject object, long length) {
        if (!(object instanceof ArrayObject || object instanceof ArgumentsObject
                || object.getClass() == OrdinaryObject.class)) {
            return false;
        }
        if (!object.isDenseArray(length)) {
            return false;
        }
        Property iterProp = findIterator(cx, object);
        // Test 1: Is object[Symbol.iterator] == %ArrayPrototype%.values?
        if (iterProp == null || !ArrayPrototype.isBuiltinValues(cx.getRealm(), iterProp.getValue())) {
            return false;
        }
        // Test 2: Is %ArrayIteratorPrototype%.next the built-in next method?
        Property iterNextProp = cx.getIntrinsic(Intrinsics.ArrayIteratorPrototype).getOwnProperty(cx, "next");
        return iterNextProp != null && ArrayIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
    }

    private static boolean isSpreadable(ExecutionContext cx, TypedArrayObject array) {
        if (array.getBuffer().isDetached()) {
            return false;
        }
        Property iterProp = findIterator(cx, array);
        // Test 1: Is object[Symbol.iterator] == %ArrayPrototype%.values?
        if (iterProp == null || !TypedArrayPrototypePrototype.isBuiltinValues(cx.getRealm(), iterProp.getValue())) {
            return false;
        }
        // Test 2: Is %ArrayIteratorPrototype%.next the built-in next method?
        Property iterNextProp = cx.getIntrinsic(Intrinsics.ArrayIteratorPrototype).getOwnProperty(cx, "next");
        return iterNextProp != null && ArrayIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
    }

    private static Property findIterator(ExecutionContext cx, OrdinaryObject object) {
        final int MAX_PROTO_CHAIN_LENGTH = 5;
        for (int i = 0; i < MAX_PROTO_CHAIN_LENGTH; ++i) {
            Property iterProp = object.getOwnProperty(cx, BuiltinSymbol.iterator.get());
            if (iterProp != null) {
                return iterProp;
            }
            ScriptObject proto = object.getPrototype();
            if (!(proto instanceof OrdinaryObject)) {
                break;
            }
            object = (OrdinaryObject) proto;
        }
        return null;
    }

    /**
     * 12.2.5 Object Initializer
     * <p>
     * 12.2.5.9 Runtime Semantics: PropertyDefinitionEvaluation
     * 
     * @param object
     *            the script object
     * @param propertyName
     *            the property name
     * @param value
     *            the property value
     * @param cx
     *            the execution context
     */
    public static void defineProperty(OrdinaryObject object, Object propertyName, Object value, ExecutionContext cx) {
        CreateDataPropertyOrThrow(cx, object, propertyName, value);
    }

    /**
     * 12.2.5 Object Initializer
     * <p>
     * 12.2.5.9 Runtime Semantics: PropertyDefinitionEvaluation
     * 
     * @param object
     *            the script object
     * @param propertyName
     *            the property name
     * @param value
     *            the property value
     * @param cx
     *            the execution context
     */
    public static void defineProperty(OrdinaryObject object, String propertyName, Object value, ExecutionContext cx) {
        CreateDataPropertyOrThrow(cx, object, propertyName, value);
    }

    /**
     * 12.2.5 Object Initializer
     * <p>
     * 12.2.5.9 Runtime Semantics: PropertyDefinitionEvaluation
     * 
     * @param object
     *            the script object
     * @param propertyName
     *            the property name
     * @param value
     *            the property value
     * @param cx
     *            the execution context
     */
    public static void defineProperty(OrdinaryObject object, long propertyName, Object value, ExecutionContext cx) {
        CreateDataPropertyOrThrow(cx, object, propertyName, value);
    }

    /**
     * 12.2.? Generator Comprehensions
     * <p>
     * 12.2.?.2 Runtime Semantics: Evaluation
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the generator object
     */
    public static GeneratorObject EvaluateConstructorGeneratorComprehension(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        /* step 1 (omitted) */
        /* step 2 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 3-4 (not applicable) */
        /* step 5 */
        OrdinaryConstructorGenerator closure = ConstructorGeneratorFunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        /* step 6 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 7 */
        MakeConstructor(closure, true, prototype);
        /* step 8 */
        GeneratorObject iterator = (GeneratorObject) closure.call(cx, UNDEFINED);
        /* step 9 */
        return iterator;
    }

    /**
     * 12.2.? Generator Comprehensions
     * <p>
     * 12.2.?.2 Runtime Semantics: Evaluation
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the generator object
     */
    public static GeneratorObject EvaluateGeneratorComprehension(RuntimeInfo.Function fd, ExecutionContext cx) {
        /* step 1 (omitted) */
        /* step 2 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 3-4 (not applicable) */
        /* step 5 */
        OrdinaryGenerator closure = GeneratorFunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        /* step 6 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 7 */
        closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        /* step 8 */
        GeneratorObject iterator = (GeneratorObject) closure.call(cx, UNDEFINED);
        /* step 9 */
        return iterator;
    }

    /**
     * 12.2.? Generator Comprehensions
     * <p>
     * Runtime Semantics: Evaluation
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the generator object
     */
    public static GeneratorObject EvaluateLegacyGeneratorComprehension(RuntimeInfo.Function fd, ExecutionContext cx) {
        /* step 1 (omitted) */
        /* step 2 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 3-4 (not applicable) */
        /* step 5 */
        OrdinaryConstructorGenerator closure = ConstructorGeneratorFunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        /* step 6 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.LegacyGeneratorPrototype);
        /* step 7 */
        MakeConstructor(closure, true, prototype);
        /* step 8 */
        GeneratorObject iterator = (GeneratorObject) closure.call(cx, UNDEFINED);
        /* step 9 */
        return iterator;
    }

    /**
     * 12.2.8 Template Literals
     * <p>
     * 12.2.8.2.2 Runtime Semantics: GetTemplateObject ( templateLiteral )
     * 
     * @param key
     *            the template literal key
     * @param handle
     *            the method handle for the template literal data
     * @param cx
     *            the execution context
     * @return the template call site object
     */
    public static ArrayObject GetTemplateObject(int key, MethodHandle handle, ExecutionContext cx) {
        assert cx.getCurrentExecutable() instanceof CompiledObject : cx.getCurrentExecutable();
        CompiledObject compiledObject = (CompiledObject) cx.getCurrentExecutable();
        ArrayObject template = compiledObject.getTemplateObject(key);
        if (template == null) {
            template = GetTemplateObject(handle, cx);
            compiledObject.setTemplateObject(key, template);
        }
        return template;
    }

    /**
     * 12.2.9 Template Literals
     * <p>
     * 12.2.9.3 Runtime Semantics: GetTemplateObject ( templateLiteral )
     * 
     * @param handle
     *            the method handle for the template literal data
     * @param cx
     *            the execution context
     * @return the template call site object
     */
    private static ArrayObject GetTemplateObject(MethodHandle handle, ExecutionContext cx) {
        /* steps 1, 6 */
        String[] strings = evaluateTemplateStrings(handle);
        assert (strings.length & 1) == 0;
        /* steps 2-4 */
        Map<String, ArrayObject> templateRegistry = cx.getRealm().getTemplateMap();
        /* step 5 */
        String templateKey = templateStringKey(strings);
        if (templateRegistry.containsKey(templateKey)) {
            return templateRegistry.get(templateKey);
        }
        /* step 7 */
        int count = strings.length >>> 1;
        /* steps 8-9 */
        ArrayObject template = ArrayCreate(cx, count);
        ArrayObject rawObj = ArrayCreate(cx, count);
        /* steps 10-11 */
        for (int i = 0, n = strings.length; i < n; i += 2) {
            int index = i >>> 1;
            int prop = index;
            String cookedValue = strings[i];
            template.defineOwnProperty(cx, prop, new PropertyDescriptor(cookedValue, false, true, false));
            String rawValue = strings[i + 1];
            rawObj.defineOwnProperty(cx, prop, new PropertyDescriptor(rawValue, false, true, false));
        }
        /* steps 12-14 */
        SetIntegrityLevel(cx, rawObj, IntegrityLevel.Frozen);
        template.defineOwnProperty(cx, "raw", new PropertyDescriptor(rawObj, false, false, false));
        SetIntegrityLevel(cx, template, IntegrityLevel.Frozen);
        /* step 15 */
        templateRegistry.put(templateKey, template);
        /* step 16 */
        return template;
    }

    private static String templateStringKey(String[] strings) {
        assert (strings.length & 1) == 0;
        StringBuilder raw = new StringBuilder();
        for (int i = 0, n = strings.length; i < n; i += 2) {
            // Template string normalization removes any \r character in the source string, so it's
            // safe to use that character as a delimiter here.
            String rawValue = strings[i + 1];
            raw.append(rawValue).append('\r');
        }
        return raw.toString();
    }

    private static String[] evaluateTemplateStrings(MethodHandle handle) {
        try {
            return (String[]) handle.invokeExact();
        } catch (Throwable e) {
            throw ScriptRuntime.<RuntimeException> rethrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E rethrow(Throwable e) throws E {
        throw (E) e;
    }

    private static Object toPropertyKey(double propertyKey) {
        long index = (long) propertyKey;
        if (index == propertyKey) {
            return index;
        }
        return ToString(propertyKey);
    }

    public static Object checkAccessElement(Object baseValue, Object propertyName, ExecutionContext cx) {
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        RequireObjectCoercible(cx, baseValue);
        /* step 9 */
        if (Type.isString(propertyName)) {
            // Convert to flat-string.
            return Type.stringValue(propertyName).toString();
        }
        if (Type.isNumber(propertyName)) {
            return toPropertyKey(Type.numberValue(propertyName));
        }
        return ToPropertyKey(cx, propertyName);
    }

    public static int checkAccessProperty(Object baseValue, int propertyName, ExecutionContext cx) {
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        RequireObjectCoercible(cx, baseValue);
        return propertyName;
    }

    public static long checkAccessProperty(Object baseValue, long propertyName, ExecutionContext cx) {
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        RequireObjectCoercible(cx, baseValue);
        return propertyName;
    }

    public static double checkAccessProperty(Object baseValue, double propertyName, ExecutionContext cx) {
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        RequireObjectCoercible(cx, baseValue);
        return propertyName;
    }

    public static String checkAccessProperty(Object baseValue, String propertyName, ExecutionContext cx) {
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        RequireObjectCoercible(cx, baseValue);
        return propertyName;
    }

    public static Object checkAccessProperty(Object baseValue, ExecutionContext cx) {
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        return RequireObjectCoercible(cx, baseValue);
    }

    public static boolean deleteElement(Object baseValue, Object propertyName, ExecutionContext cx, boolean strict) {
        return getElement(baseValue, propertyName, cx, strict).delete(cx);
    }

    public static boolean deleteProperty(Object baseValue, int propertyName, ExecutionContext cx, boolean strict) {
        return getProperty(baseValue, propertyName, cx, strict).delete(cx);
    }

    public static boolean deleteProperty(Object baseValue, long propertyName, ExecutionContext cx, boolean strict) {
        return getProperty(baseValue, propertyName, cx, strict).delete(cx);
    }

    public static boolean deleteProperty(Object baseValue, double propertyName, ExecutionContext cx, boolean strict) {
        return getProperty(baseValue, propertyName, cx, strict).delete(cx);
    }

    public static boolean deleteProperty(Object baseValue, String propertyName, ExecutionContext cx, boolean strict) {
        return getProperty(baseValue, propertyName, cx, strict).delete(cx);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property reference
     */
    public static Reference<Object, String> getProperty(Object baseValue, int propertyNameString, ExecutionContext cx,
            boolean strict) {
        return getProperty(baseValue, (long) propertyNameString, cx, strict);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property reference
     */
    public static Reference<Object, String> getProperty(Object baseValue, long propertyNameString, ExecutionContext cx,
            boolean strict) {
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 9-11 */
        return new Reference.PropertyIndexReference(baseValue, propertyNameString, strict);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property reference
     */
    public static Reference<Object, String> getProperty(Object baseValue, double propertyNameString,
            ExecutionContext cx, boolean strict) {
        long index = (long) propertyNameString;
        if (index == propertyNameString) {
            return getProperty(baseValue, index, cx, strict);
        }
        return getProperty(baseValue, ToString(propertyNameString), cx, strict);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property reference
     */
    public static Reference<Object, String> getProperty(Object baseValue, String propertyNameString,
            ExecutionContext cx, boolean strict) {
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 9-11 */
        return new Reference.PropertyNameReference(baseValue, propertyNameString, strict);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @return the property value
     */
    public static Object getPropertyValue(Object baseValue, int propertyNameString, ExecutionContext cx) {
        return getPropertyValue(baseValue, (long) propertyNameString, cx);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @return the property value
     */
    public static Object getPropertyValue(Object baseValue, long propertyNameString, ExecutionContext cx) {
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 9-11 */
        return Reference.PropertyIndexReference.GetValue(cx, baseValue, propertyNameString);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @return the property value
     */
    public static Object getPropertyValue(Object baseValue, double propertyNameString, ExecutionContext cx) {
        long index = (long) propertyNameString;
        if (index == propertyNameString) {
            return getPropertyValue(baseValue, index, cx);
        }
        return getPropertyValue(baseValue, ToString(propertyNameString), cx);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression . IdentifierName
     * <li>CallExpression : CallExpression . IdentifierName
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameString
     *            the property name
     * @param cx
     *            the execution context
     * @return the property value
     */
    public static Object getPropertyValue(Object baseValue, String propertyNameString, ExecutionContext cx) {
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        RequireObjectCoercible(cx, baseValue);
        /* steps 9-11 */
        return Reference.PropertyNameReference.GetValue(cx, baseValue, propertyNameString);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression [ Expression ]
     * <li>CallExpression : CallExpression [ Expression ]
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameValue
     *            the property name
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the property reference
     */
    public static Reference<Object, ?> getElement(Object baseValue, Object propertyNameValue, ExecutionContext cx,
            boolean strict) {
        if (Type.isString(propertyNameValue)) {
            return getProperty(baseValue, Type.stringValue(propertyNameValue).toString(), cx, strict);
        }
        if (Type.isNumber(propertyNameValue)) {
            return getProperty(baseValue, Type.numberValue(propertyNameValue), cx, strict);
        }
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        RequireObjectCoercible(cx, baseValue);
        /* step 9 */
        Object propertyKey = ToPropertyKey(cx, propertyNameValue);
        /* steps 10-11 */
        if (propertyKey instanceof String) {
            return new Reference.PropertyNameReference(baseValue, (String) propertyKey, strict);
        }
        return new Reference.PropertySymbolReference(baseValue, (Symbol) propertyKey, strict);
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>MemberExpression : MemberExpression [ Expression ]
     * <li>CallExpression : CallExpression [ Expression ]
     * </ul>
     * 
     * @param baseValue
     *            the base value
     * @param propertyNameValue
     *            the property name
     * @param cx
     *            the execution context
     * @return the property value
     */
    public static Object getElementValue(Object baseValue, Object propertyNameValue, ExecutionContext cx) {
        if (Type.isString(propertyNameValue)) {
            return getPropertyValue(baseValue, Type.stringValue(propertyNameValue).toString(), cx);
        }
        if (Type.isNumber(propertyNameValue)) {
            return getPropertyValue(baseValue, Type.numberValue(propertyNameValue), cx);
        }
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        RequireObjectCoercible(cx, baseValue);
        /* step 9 */
        Object propertyKey = ToPropertyKey(cx, propertyNameValue);
        /* steps 10-11 */
        if (propertyKey instanceof String) {
            return Reference.PropertyNameReference.GetValue(cx, baseValue, (String) propertyKey);
        }
        return Reference.PropertySymbolReference.GetValue(cx, baseValue, (Symbol) propertyKey);
    }

    public static void setPropertyValue(Object base, int propertyKey, Object value, ExecutionContext cx,
            boolean strict) {
        Reference.PropertyIndexReference.PutValue(cx, base, (long) propertyKey, value, strict);
    }

    public static void setPropertyValue(Object base, long propertyKey, Object value, ExecutionContext cx,
            boolean strict) {
        Reference.PropertyIndexReference.PutValue(cx, base, propertyKey, value, strict);
    }

    public static void setPropertyValue(Object base, double propertyKey, Object value, ExecutionContext cx,
            boolean strict) {
        long index = (long) propertyKey;
        if (index == propertyKey) {
            setPropertyValue(base, index, value, cx, strict);
        } else {
            setPropertyValue(base, ToString(propertyKey), value, cx, strict);
        }
    }

    public static void setPropertyValue(Object base, String propertyKey, Object value, ExecutionContext cx,
            boolean strict) {
        Reference.PropertyNameReference.PutValue(cx, base, propertyKey, value, strict);
    }

    public static void setPropertyValue(Object base, Symbol propertyKey, Object value, ExecutionContext cx,
            boolean strict) {
        Reference.PropertySymbolReference.PutValue(cx, base, propertyKey, value, strict);
    }

    public static void setElementValue(Object base, Object propertyKey, Object value, ExecutionContext cx,
            boolean strict) {
        if (propertyKey instanceof String) {
            setPropertyValue(base, (String) propertyKey, value, cx, strict);
        } else if (propertyKey instanceof Long) {
            setPropertyValue(base, (Long) propertyKey, value, cx, strict);
        } else {
            setPropertyValue(base, (Symbol) propertyKey, value, cx, strict);
        }
    }

    /**
     * 12.3.3 The new Operator
     * <p>
     * 12.3.3.1 Runtime Semantics: Evaluation<br>
     * 12.3.5.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>NewExpression : new NewExpression
     * <li>MemberExpression : new MemberExpression Arguments
     * <li>MemberExpression : NewSuper Arguments<span><sub>opt</sub></span>
     * </ul>
     * 
     * @param constructor
     *            the constructor function object
     * @param cx
     *            the execution context
     * @return the constructor function object
     * @throws ScriptException
     *             if <var>constructor</var> is not a constructor function
     */
    public static Constructor CheckConstructor(Object constructor, ExecutionContext cx) throws ScriptException {
        /* steps 4/6/5 */
        if (!IsConstructor(constructor)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        return (Constructor) constructor;
    }

    /**
     * 12.3.4 Function Calls
     * <p>
     * 12.3.4.3 Runtime Semantics: EvaluateDirectCall( func, thisValue, arguments, tailPosition )
     * 
     * @param func
     *            the function object
     * @param cx
     *            the execution context
     * @return the function object
     * @throws ScriptException
     *             if <var>func</var> is not a function
     */
    public static Callable CheckCallable(Object func, ExecutionContext cx) throws ScriptException {
        /* steps 3-4 */
        if (!IsCallable(func)) {
            throw newTypeError(cx, Messages.Key.NotCallable);
        }
        return (Callable) func;
    }

    /**
     * 12.3.4 Function Calls
     * <p>
     * 12.3.4.1 Runtime Semantics: Evaluation
     * 
     * @param ref
     *            the reference value
     * @param f
     *            the function object
     * @param cx
     *            the execution context
     * @return {@code true} if <var>f</var> is the built-in eval function
     */
    public static boolean IsBuiltinEval(Object ref, Callable f, ExecutionContext cx) {
        /* step 4 */
        if (ref instanceof Reference) {
            Reference<?, ?> r = (Reference<?, ?>) ref;
            if (!r.isPropertyReference()) {
                assert !r.isUnresolvableReference() && r.getBase() instanceof EnvironmentRecord;
                return f == cx.getRealm().getBuiltinEval();
            }
        }
        return false;
    }

    /**
     * 12.3.4 Function Calls
     * <p>
     * 12.3.4.1 Runtime Semantics: Evaluation
     * 
     * @param f
     *            the function object
     * @param cx
     *            the execution context
     * @return {@code true} if <var>f</var> is the built-in eval function
     */
    public static boolean IsBuiltinEval(Callable f, ExecutionContext cx) {
        /* step 4 */
        return f == cx.getRealm().getBuiltinEval();
    }

    /**
     * 12.3.4 Function Calls
     * 
     * @param cx
     *            the execution context
     * @return the direct eval fallback hook
     */
    public static Callable directEvalFallbackHook(ExecutionContext cx) {
        return cx.getRealm().getNonEvalFallback();
    }

    /**
     * 12.3.4 Function Calls
     * 
     * @param callee
     *            the function callee
     * @param cx
     *            the execution context
     * @param thisValue
     *            the function this-value
     * @param args
     *            the function call arguments
     * @return the direct eval fallback arguments
     */
    public static Object[] directEvalFallbackArguments(Callable callee, ExecutionContext cx, Object thisValue,
            Object[] args) {
        Object[] fallbackArgs = new Object[3];
        fallbackArgs[0] = callee;
        fallbackArgs[1] = thisValue;
        fallbackArgs[2] = CreateArrayFromList(cx, Arrays.asList(args));
        return fallbackArgs;
    }

    /**
     * 12.3.4 Function Calls
     * 
     * @param cx
     *            the execution context
     * @return the direct eval fallback this-argument
     */
    public static Object directEvalFallbackThisArgument(ExecutionContext cx) {
        return cx.getRealm().getRealmObject();
    }

    /**
     * 12.3.8 Meta Properties
     * 
     * @param cx
     *            the execution context
     * @return the NewTarget constructor object
     */
    public static Object GetNewTargetOrUndefined(ExecutionContext cx) {
        Constructor newTarget = cx.getNewTarget();
        if (newTarget == null) {
            return UNDEFINED;
        }
        return newTarget;
    }

    /**
     * 12.3.5 The super Keyword
     * 
     * @param cx
     *            the execution context
     * @return the NewTarget constructor object
     */
    public static Constructor GetNewTarget(ExecutionContext cx) {
        Constructor newTarget = cx.getNewTarget();
        if (newTarget == null) {
            throw newReferenceError(cx, Messages.Key.MissingNewTarget);
        }
        return newTarget;
    }

    /**
     * 12.3.5 The super Keyword
     * 
     * @param result
     *            the new {@code this} binding value
     * @param cx
     *            the execution context
     */
    public static void BindThisValue(ScriptObject result, ExecutionContext cx) {
        EnvironmentRecord thisEnvironment = cx.getThisEnvironment();
        assert thisEnvironment instanceof FunctionEnvironmentRecord;
        ((FunctionEnvironmentRecord) thisEnvironment).bindThisValue(cx, result);
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * 12.3.5.2 Runtime Semantics: GetSuperConstructor ( )
     * 
     * @param cx
     *            the execution context
     * @return the super reference
     */
    public static Constructor GetSuperConstructor(ExecutionContext cx) {
        /* step 1 */
        EnvironmentRecord envRec = cx.getThisEnvironment();
        /* step 2 */
        assert envRec instanceof FunctionEnvironmentRecord;
        FunctionEnvironmentRecord fEnvRec = (FunctionEnvironmentRecord) envRec;
        /* step 3 */
        FunctionObject activeFunction = fEnvRec.getFunctionObject();
        /* steps 4-5 */
        ScriptObject superConstructor = activeFunction.getPrototypeOf(cx);
        /* step 6 */
        if (!IsConstructor(superConstructor)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        /* step 7 */
        return (Constructor) superConstructor;
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * 12.3.5.3 Runtime Semantics: MakeSuperPropertyReference(propertyKey, strict)
     * 
     * @param propertyKey
     *            the property key
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the super reference
     */
    public static Reference<ScriptObject, ?> MakeSuperPropertyReference(Object propertyKey, ExecutionContext cx,
            boolean strict) {
        if (propertyKey instanceof String) {
            return MakeSuperPropertyReference((String) propertyKey, cx, strict);
        } else {
            return MakeSuperPropertyReference((Symbol) propertyKey, cx, strict);
        }
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * 12.3.5.3 Runtime Semantics: MakeSuperPropertyReference(propertyKey, strict)
     * 
     * @param propertyKey
     *            the property key
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the super reference
     */
    public static Reference<ScriptObject, String> MakeSuperPropertyReference(String propertyKey, ExecutionContext cx,
            boolean strict) {
        /* steps 1-2 */
        FunctionEnvironmentRecord envRec = GetSuperEnvironmentRecord(cx);
        /* steps 3-4 */
        Object actualThis = GetSuperThis(envRec, cx);
        /* steps 5-7 */
        ScriptObject baseValue = GetSuperBase(envRec, cx);
        /* step 8 */
        return new Reference.SuperNameReference(baseValue, propertyKey, strict, actualThis);
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * 12.3.5.3 Runtime Semantics: MakeSuperPropertyReference(propertyKey, strict)
     * 
     * @param propertyKey
     *            the property key
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     * @return the super reference
     */
    public static Reference<ScriptObject, Symbol> MakeSuperPropertyReference(Symbol propertyKey, ExecutionContext cx,
            boolean strict) {
        /* steps 1-2 */
        FunctionEnvironmentRecord envRec = GetSuperEnvironmentRecord(cx);
        /* steps 3-4 */
        Object actualThis = GetSuperThis(envRec, cx);
        /* steps 5-7 */
        ScriptObject baseValue = GetSuperBase(envRec, cx);
        /* step 8 */
        return new Reference.SuperSymbolReference(baseValue, propertyKey, strict, actualThis);
    }

    public static FunctionEnvironmentRecord GetSuperEnvironmentRecord(ExecutionContext cx) {
        /* step 1 */
        EnvironmentRecord envRec = cx.getThisEnvironment();
        /* step 2 */
        if (!envRec.hasSuperBinding()) {
            throw newReferenceError(cx, Messages.Key.MissingSuperBinding);
        }
        assert envRec instanceof FunctionEnvironmentRecord : envRec.getClass().toString();
        return (FunctionEnvironmentRecord) envRec;
    }

    public static Object GetSuperThis(FunctionEnvironmentRecord envRec, ExecutionContext cx) {
        /* steps 3-4 */
        return envRec.getThisBinding(cx);
    }

    public static ScriptObject GetSuperBase(FunctionEnvironmentRecord envRec, ExecutionContext cx) {
        /* step 5 */
        ScriptObject baseValue = envRec.getSuperBase(cx);
        /* steps 6-7 */
        // RequireObjectCoercible(cx, baseValue);
        if (baseValue == null) {
            throw newTypeError(cx, Messages.Key.UndefinedOrNull);
        }
        return baseValue;
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * 12.3.5.3 Runtime Semantics: MakeSuperPropertyReference(propertyKey, strict)
     * 
     * @param propertyKey
     *            the property key
     * @param actualThis
     *            the actual {@code this} value
     * @param baseValue
     *            the reference base value
     * @param cx
     *            the execution context
     * @return the super reference value
     */
    public static Object getSuperProperty(Object propertyKey, Object actualThis, ScriptObject baseValue,
            ExecutionContext cx) {
        if (propertyKey instanceof String) {
            return baseValue.get(cx, (String) propertyKey, actualThis);
        } else {
            return baseValue.get(cx, (Symbol) propertyKey, actualThis);
        }
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * 12.3.5.3 Runtime Semantics: MakeSuperPropertyReference(propertyKey, strict)
     * 
     * @param propertyKey
     *            the property key
     * @param actualThis
     *            the actual {@code this} value
     * @param baseValue
     *            the reference base value
     * @param cx
     *            the execution context
     * @return the super reference value
     */
    public static Object getSuperProperty(String propertyKey, Object actualThis, ScriptObject baseValue,
            ExecutionContext cx) {
        return baseValue.get(cx, propertyKey, actualThis);
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * 12.3.5.3 Runtime Semantics: MakeSuperPropertyReference(propertyKey, strict)
     * 
     * @param propertyKey
     *            the property key
     * @param actualThis
     *            the actual {@code this} value
     * @param baseValue
     *            the reference base value
     * @param value
     *            the new property value
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     */
    public static void setSuperProperty(Object propertyKey, Object actualThis, ScriptObject baseValue, Object value,
            ExecutionContext cx, boolean strict) {
        boolean succeeded;
        if (propertyKey instanceof String) {
            succeeded = baseValue.set(cx, (String) propertyKey, value, actualThis);
        } else {
            succeeded = baseValue.set(cx, (Symbol) propertyKey, value, actualThis);
        }
        if (!succeeded && strict) {
            throw newTypeError(cx, Messages.Key.PropertyNotModifiable, propertyKey.toString());
        }
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * 12.3.5.3 Runtime Semantics: MakeSuperPropertyReference(propertyKey, strict)
     * 
     * @param propertyKey
     *            the property key
     * @param actualThis
     *            the actual {@code this} value
     * @param baseValue
     *            the reference base value
     * @param value
     *            the new property value
     * @param cx
     *            the execution context
     * @param strict
     *            the strict mode flag
     */
    public static void setSuperProperty(String propertyKey, Object actualThis, ScriptObject baseValue, Object value,
            ExecutionContext cx, boolean strict) {
        boolean succeeded = baseValue.set(cx, (String) propertyKey, value, actualThis);
        if (!succeeded && strict) {
            throw newTypeError(cx, Messages.Key.PropertyNotModifiable, propertyKey);
        }
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * 12.3.5.3 Runtime Semantics: MakeSuperPropertyReference(propertyKey, strict)
     * 
     * @param cx
     *            the execution context
     * @return no value
     */
    public static boolean deleteSuperProperty(ExecutionContext cx) {
        /* steps 1-2 */
        FunctionEnvironmentRecord envRec = GetSuperEnvironmentRecord(cx);
        /* steps 3-4 */
        GetSuperThis(envRec, cx);
        /* steps 5-7 */
        GetSuperBase(envRec, cx);
        /* step 8 (omitted) */
        /* 12.5.4.2, step 5.a */
        throw newReferenceError(cx, Messages.Key.SuperDelete);
    }

    /**
     * 12.3.6 Argument Lists
     * <p>
     * 12.3.6.1 Runtime Semantics: ArgumentListEvaluation
     * 
     * @param spreadObj
     *            the spread object
     * @param cx
     *            the execution context
     * @return the spread object elements
     */
    public static Object[] SpreadArray(Object spreadObj, ExecutionContext cx) {
        final int MAX_ARGS = FunctionPrototype.getMaxArguments();
        if (spreadObj instanceof OrdinaryObject) {
            OrdinaryObject object = (OrdinaryObject) spreadObj;
            long length = object.getLength();
            if (0 <= length && length <= MAX_ARGS && isSpreadable(cx, object, length)) {
                if (length == 0) {
                    return EMPTY_ARRAY;
                }
                return object.toArray(length);
            }
        }
        /* steps 1-3 (cf. generated code) */
        /* steps 4-5 */
        ScriptIterator<?> iterator = GetScriptIterator(cx, spreadObj);
        /* step 6 */
        ArrayList<Object> list = new ArrayList<>();
        for (int n = 0; n <= MAX_ARGS; ++n) {
            if (!iterator.hasNext()) {
                return list.toArray(new Object[n]);
            }
            list.add(iterator.next());
        }
        throw newRangeError(cx, Messages.Key.FunctionTooManyArguments);
    }

    /**
     * 12.3.6 Argument Lists
     * <p>
     * 12.3.6.1 Runtime Semantics: ArgumentListEvaluation
     * 
     * @param spreadObj
     *            the spread object
     * @param cx
     *            the execution context
     * @return the spread object elements
     */
    public static Object[] NativeCallSpreadArray(Object spreadObj, ExecutionContext cx) {
        final int MAX_ARGS = FunctionPrototype.getMaxArguments();
        if (spreadObj instanceof ArrayObject) {
            ArrayObject array = (ArrayObject) spreadObj;
            long length = array.getLength();
            if (array.isDenseArray() && length <= MAX_ARGS) {
                if (length == 0) {
                    return EMPTY_ARRAY;
                }
                return array.toArray(length);
            }
        }
        throw newInternalError(cx, Messages.Key.InternalError, "Invalid native call");
    }

    /**
     * 12.3.6 Argument Lists
     * <p>
     * 12.3.6.1 Runtime Semantics: ArgumentListEvaluation
     * 
     * @param array
     *            the array
     * @param cx
     *            the execution context
     * @return the flattened array
     */
    public static Object[] toFlatArray(Object[] array, ExecutionContext cx) {
        final int MAX_ARGS = FunctionPrototype.getMaxArguments();
        int newlen = array.length;
        for (int i = 0, len = array.length; i < len; ++i) {
            if (array[i] instanceof Object[]) {
                newlen += ((Object[]) array[i]).length - 1;
                if (newlen > MAX_ARGS) {
                    throw newRangeError(cx, Messages.Key.FunctionTooManyArguments);
                }
            }
        }
        Object[] result = new Object[newlen];
        for (int i = 0, j = 0, len = array.length; i < len; ++i) {
            if (array[i] instanceof Object[]) {
                Object[] a = (Object[]) array[i];
                System.arraycopy(a, 0, result, j, a.length);
                j += a.length;
            } else {
                result[j++] = array[i];
            }
        }
        return result;
    }

    /**
     * 12.5 Unary Operators<br>
     * 12.5.6 The typeof Operator
     * 
     * @param val
     *            the value
     * @param cx
     *            the execution context
     * @return the typeof descriptor string
     */
    public static String typeof(Object val, ExecutionContext cx) {
        /* step 1 (generated code) */
        /* step 2 */
        if (val instanceof Reference) {
            Reference<?, ?> ref = (Reference<?, ?>) val;
            if (ref.isUnresolvableReference()) {
                return "undefined";
            }
            val = ref.getValue(cx);
        }
        return typeof(val);
    }

    /**
     * 12.5 Unary Operators<br>
     * 12.5.6 The typeof Operator
     * 
     * @param ref
     *            the reference
     * @param cx
     *            the execution context
     * @return the typeof descriptor string
     */
    public static String typeof(Reference<?, ?> ref, ExecutionContext cx) {
        if (ref.isUnresolvableReference()) {
            return "undefined";
        }
        return typeof(ref.getValue(cx));
    }

    /**
     * 12.5 Unary Operators<br>
     * 12.5.6 The typeof Operator
     * 
     * @param val
     *            the value
     * @return the typeof descriptor string
     */
    public static String typeof(Object val) {
        /* steps 1-2 (generated code) */
        /* steps 3-4 */
        switch (Type.of(val)) {
        case Undefined:
            return "undefined";
        case Null:
            return "object";
        case Boolean:
            return "boolean";
        case Number:
            return "number";
        case String:
            return "string";
        case Symbol:
            return "symbol";
        case SIMD:
            return Type.simdValue(val).getType().typeof();
        case Object:
            if (IsCallable(val)) {
                return "function";
            }
            return "object";
        default:
            throw new AssertionError();
        }
    }

    /**
     * 12.5 Unary Operators<br>
     * 12.5.6 The typeof Operator
     * 
     * @param val
     *            the value
     * @return {@code true} on success
     */
    public static boolean isNonCallableObjectOrNull(Object val) {
        return Type.isNull(val) || (Type.isObject(val) && !IsCallable(val));
    }

    /**
     * 12.5 Unary Operators<br>
     * 12.5.6 The typeof Operator
     * 
     * @param val
     *            the value
     * @param type
     *            the SIMD type
     * @return {@code true} on success
     */
    public static boolean isSIMDType(Object val, SIMDType type) {
        return Type.isSIMD(val) && Type.simdValue(val).getType() == type;
    }

    /**
     * 12.7 Additive Operators<br>
     * 12.7.3 The Addition operator ( + )
     * 
     * @param lval
     *            the left-hand side operand
     * @param rval
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the operation result
     */
    public static Object add(Object lval, Object rval, ExecutionContext cx) {
        /* steps 1-6 (generated code) */
        /* steps 7-8 */
        Object lprim = ToPrimitive(cx, lval);
        /* steps 9-10 */
        Object rprim = ToPrimitive(cx, rval);
        /* step 11 */
        if (Type.isString(lprim) || Type.isString(rprim)) {
            CharSequence lstr = ToString(cx, lprim);
            CharSequence rstr = ToString(cx, rprim);
            return add(lstr, rstr, cx);
        }
        /* steps 12-16 */
        return ToNumber(cx, lprim) + ToNumber(cx, rprim);
    }

    /**
     * 12.7 Additive Operators<br>
     * 12.7.3 The Addition operator ( + )
     * 
     * @param lstr
     *            the left-hand side operand
     * @param rstr
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the concatenated string
     */
    public static CharSequence add(CharSequence lstr, CharSequence rstr, ExecutionContext cx) {
        int llen = lstr.length(), rlen = rstr.length();
        if (llen == 0) {
            return rstr;
        }
        if (rlen == 0) {
            return lstr;
        }
        int newlen = llen + rlen;
        if (newlen < 0) {
            throw newInternalError(cx, Messages.Key.OutOfMemory);
        }
        if (newlen <= 10) {
            // return new StringBuilder(newlen).append(lstr).append(rstr).toString();
            return inlineString(lstr, rstr, llen, rlen);
        }
        return new ConsString(lstr, rstr);
    }

    private static String inlineString(CharSequence lstr, CharSequence rstr, int llen, int rlen) {
        char[] ca = new char[llen + rlen];
        lstr.toString().getChars(0, llen, ca, 0);
        rstr.toString().getChars(0, rlen, ca, llen);
        return new String(ca);
    }

    /**
     * 12.7 Additive Operators<br>
     * 12.7.3 The Addition operator ( + )
     * 
     * @param value
     *            the argument value
     * @param cx
     *            the execution context
     * @return the result of ToString(ToPrimitive(value))
     */
    public static CharSequence toStr(Object value, ExecutionContext cx) {
        return ToString(cx, ToPrimitive(cx, value));
    }

    /**
     * 12.9 Relational Operators<br>
     * 12.9.3 Runtime Semantics: Evaluation
     * 
     * @param lval
     *            the left-hand side operand
     * @param rval
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the operation result
     */
    public static boolean in(Object lval, Object rval, ExecutionContext cx) {
        /* steps 1-6 (generated code) */
        /* step 7 */
        if (!Type.isObject(rval)) {
            throw newTypeError(cx, Messages.Key.InNotObject);
        }
        /* step 8 */
        return HasProperty(cx, Type.objectValue(rval), ToPropertyKey(cx, lval));
    }

    /**
     * 12.9 Relational Operators<br>
     * 12.9.4 Runtime Semantics: InstanceofOperator(O, C)
     * 
     * @param obj
     *            the object
     * @param constructor
     *            the constructor function
     * @param cx
     *            the execution context
     * @return the operation result
     */
    public static boolean InstanceofOperator(Object obj, Object constructor, ExecutionContext cx) {
        /* step 1 */
        if (!Type.isObject(constructor)) {
            throw newTypeError(cx, Messages.Key.InstanceofNotObject);
        }
        /* steps 2-3 */
        Callable instOfHandler = GetMethod(cx, Type.objectValue(constructor),
                BuiltinSymbol.hasInstance.get());
        /* step 4 */
        if (instOfHandler != null) {
            return ToBoolean(instOfHandler.call(cx, constructor, obj));
        }
        /* step 5 */
        if (!IsCallable(constructor)) {
            throw newTypeError(cx, Messages.Key.InstanceofNotCallable);
        }
        /* step 6 */
        return OrdinaryHasInstance(cx, constructor, obj);
    }

    /**
     * 12.10 Equality Operators
     * 
     * @param lval
     *            the first string
     * @param rval
     *            the second string
     * @return the operation result
     */
    public static boolean compare(CharSequence lval, CharSequence rval) {
        return lval.length() == rval.length() && lval.toString().equals(rval.toString());
    }

    /**
     * 12.14.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialization
     * 
     * @param iterator
     *            the iterator
     * @param cx
     *            the execution context
     * @return the array with the remaining elements from <var>iterator</var>
     */
    public static ArrayObject createRestArray(Iterator<?> iterator, ExecutionContext cx) {
        ArrayObject result = ArrayCreate(cx, 0);
        for (int n = 0; iterator.hasNext(); ++n) {
            Object nextValue = iterator.next();
            CreateDataProperty(cx, result, n, nextValue);
        }
        return result;
    }

    /**
     * 12.14.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialization
     * 
     * @param iterator
     *            the iterator
     */
    public static void iteratorNextAndIgnore(Iterator<?> iterator) {
        if (iterator.hasNext()) {
            iterator.next();
        }
    }

    /**
     * 12.14.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialization
     * 
     * @param iterator
     *            the iterator
     * @return the next iterator result, or undefined it is already exhausted
     */
    public static Object iteratorNextOrUndefined(Iterator<?> iterator) {
        return iterator.hasNext() ? iterator.next() : UNDEFINED;
    }

    /* ***************************************************************************************** */

    /**
     * 13.6.4 The for-in and for-of Statements
     * <ul>
     * <li>13.6.4.12 Runtime Semantics: ForIn/OfHeadEvaluation ( TDZnames, expr, iterationKind, labelSet)
     * </ul>
     * 
     * @param value
     *            the object to enumerate
     * @param cx
     *            the execution context
     * @return the keys enumerator
     */
    public static ScriptIterator<?> enumerate(Object value, ExecutionContext cx) {
        /* step 7.b */
        ScriptObject obj = ToObject(cx, value);
        /* step 7.c */
        if (cx.getRealm().isEnabled(CompatibilityOption.Enumerate)) {
            return obj.enumerateKeys(cx);
        }
        return new EnumeratePropertiesIterator(cx, obj);
    }

    /**
     * EnumerateObjectProperties (O)
     */
    private static final class EnumeratePropertiesIterator extends SimpleIterator<String>
            implements ScriptIterator<String> {
        private final HashSet<Object> visitedKeys = new HashSet<>();
        private final ExecutionContext cx;
        private ScriptObject obj;
        private Iterator<String> keys;

        EnumeratePropertiesIterator(ExecutionContext cx, ScriptObject obj) {
            this.cx = cx;
            this.obj = obj;
            this.keys = obj.ownEnumerablePropertyKeys(cx);
        }

        @Override
        protected String findNext() {
            HashSet<Object> visitedKeys = this.visitedKeys;
            ExecutionContext cx = this.cx;
            for (ScriptObject obj = this.obj; obj != null;) {
                for (Iterator<String> keys = this.keys; keys.hasNext();) {
                    String key = keys.next();
                    ScriptObject.Enumerability e = obj.isEnumerableOwnProperty(cx, key);
                    if (e != ScriptObject.Enumerability.Deleted) {
                        if (visitedKeys.add(key) && e == ScriptObject.Enumerability.Enumerable) {
                            return key;
                        }
                    }
                }
                obj = this.obj = obj.getPrototypeOf(cx);
                if (obj != null) {
                    this.keys = obj.ownEnumerablePropertyKeys(cx);
                } else {
                    this.keys = null;
                }
            }
            return null;
        }

        @Override
        public void close() throws ScriptException {
            // No return() action required.
        }

        @Override
        public void close(Throwable cause) throws ScriptException {
            // No return() action required.
        }
    }

    /**
     * 13.6.4 The for-in and for-of Statements
     * <ul>
     * <li>13.6.4.12 Runtime Semantics: ForIn/OfHeadEvaluation ( TDZnames, expr, iterationKind, labelSet)
     * </ul>
     * <p>
     * 12.14.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialization
     * 
     * @param value
     *            the object to iterate
     * @param cx
     *            the execution context
     * @return the object iterator
     */
    public static ScriptIterator<?> iterate(Object value, ExecutionContext cx) {
        /* step 8 */
        return GetScriptIterator(cx, value);
    }

    /**
     * 13.6.4 The for-in and for-of Statements<br>
     * Extension: 'for-await' statement
     * <p>
     * 13.6.4.12 Runtime Semantics: ForIn/OfHeadEvaluation ( TDZnames, expr, iterationKind, labelSet)
     * 
     * @param value
     *            the object to enumerate
     * @param cx
     *            the execution context
     * @return the async iterator
     */
    public static ScriptObject asyncIterate(Object value, ExecutionContext cx) {
        return AsyncGeneratorAbstractOperations.GetAsyncIterator(cx, value);
    }

    /**
     * 13.6.4 The for-in and for-of Statements<br>
     * Extension: 'for-each' statement
     * <p>
     * 13.6.4.12 Runtime Semantics: ForIn/OfHeadEvaluation ( TDZnames, expr, iterationKind, labelSet)
     * 
     * @param value
     *            the object to enumerate
     * @param cx
     *            the execution context
     * @return the values enumerator
     */
    public static ScriptIterator<?> enumerateValues(Object value, ExecutionContext cx) {
        /* step 7.b */
        ScriptObject obj = ToObject(cx, value);
        /* steps 7.b-c */
        return new ValuesIterator(cx, obj, enumerate(obj, cx));
    }

    private static final class ValuesIterator extends SimpleIterator<Object> implements ScriptIterator<Object> {
        private final ExecutionContext cx;
        private final ScriptObject object;
        private final ScriptIterator<?> keysIterator;

        ValuesIterator(ExecutionContext cx, ScriptObject object, ScriptIterator<?> keysIterator) {
            this.cx = cx;
            this.object = object;
            this.keysIterator = keysIterator;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (keysIterator.hasNext()) {
                Object pk = ToPropertyKey(cx, keysIterator.next());
                return Get(cx, object, pk);
            }
            return null;
        }

        @Override
        public void close() throws ScriptException {
            keysIterator.close();
        }

        @Override
        public void close(Throwable cause) throws ScriptException {
            keysIterator.close(cause);
        }
    }

    /**
     * 13.14 The try Statement
     * 
     * @param e
     *            the error cause
     * @return if either <var>e</var> or its cause is a stack overflow error, that error object
     * @throws Error
     *             if neither the error nor its cause is a stack overflow error
     */
    public static StackOverflowError stackOverflowError(Error e) throws Error {
        if (e instanceof StackOverflowError) {
            return (StackOverflowError) e;
        }
        Throwable cause = e.getCause();
        if (cause instanceof StackOverflowError) {
            return (StackOverflowError) cause;
        }
        throw e;
    }

    /**
     * 13.14 The try Statement
     * 
     * @param e
     *            the error cause
     * @param cx
     *            the execution context
     * @return the new script exception
     */
    public static ScriptException toInternalError(StackOverflowError e, ExecutionContext cx) {
        ScriptException exception = newInternalError(cx, Messages.Key.StackOverflow);
        // use stacktrace from original error
        exception.setStackTrace(e.getStackTrace());
        return exception;
    }

    /**
     * 13.15 The debugger statement
     */
    public static void debugger() {
        // breakpoint
    }

    /* ***************************************************************************************** */

    /**
     * 14.1 Function Definitions
     * <p>
     * 14.1.20 Runtime Semantics: InstantiateFunctionObject
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new function instance
     */
    public static OrdinaryConstructorFunction InstantiateFunctionObject(LexicalEnvironment<?> scope,
            ExecutionContext cx, RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        OrdinaryConstructorFunction f = ConstructorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 4 */
        MakeConstructor(cx, f);
        /* step 4 */
        SetFunctionName(f, name);
        /* step 6 */
        return f;
    }

    /**
     * 14.1 Function Definitions
     * <p>
     * 14.1.20 Runtime Semantics: InstantiateFunctionObject
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new function instance
     */
    public static LegacyConstructorFunction InstantiateLegacyFunctionObject(LexicalEnvironment<?> scope,
            ExecutionContext cx, RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        LegacyConstructorFunction f = LegacyFunctionCreate(cx, fd, scope);
        /* step 4 */
        MakeConstructor(cx, f);
        /* step 4 */
        SetFunctionName(f, name);
        /* step 6 */
        return f;
    }

    /**
     * 14.1 Function Definitions
     * <p>
     * 14.1.21 Runtime Semantics: Evaluation
     * <ul>
     * <li>FunctionExpression : function ( FormalParameters ) { FunctionBody }
     * <li>FunctionExpression : function BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new function instance
     */
    public static OrdinaryConstructorFunction EvaluateFunctionExpression(RuntimeInfo.Function fd, ExecutionContext cx) {
        OrdinaryConstructorFunction closure;
        if (!fd.is(RuntimeInfo.FunctionFlags.ScopedName)) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = ConstructorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
            /* step 4 */
            MakeConstructor(cx, closure);
        } else {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name, false);
            /* step 7 */
            closure = ConstructorFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            MakeConstructor(cx, closure);
            /* step 9 */
            SetFunctionName(closure, name);
            /* step 10 */
            envRec.initializeBinding(name, closure);
        }
        /* step 5/11 */
        return closure;
    }

    /**
     * 14.1 Function Definitions
     * <p>
     * 14.1.21 Runtime Semantics: Evaluation
     * <ul>
     * <li>FunctionExpression : function ( FormalParameters ) { FunctionBody }
     * <li>FunctionExpression : function BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new function instance
     */
    public static LegacyConstructorFunction EvaluateLegacyFunctionExpression(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        LegacyConstructorFunction closure;
        if (!fd.is(RuntimeInfo.FunctionFlags.ScopedName)) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = LegacyFunctionCreate(cx, fd, scope);
            /* step 4 */
            MakeConstructor(cx, closure);
        } else {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name, false);
            /* step 7 */
            closure = LegacyFunctionCreate(cx, fd, funcEnv);
            /* step 8 */
            MakeConstructor(cx, closure);
            /* step 9 */
            SetFunctionName(closure, name);
            /* step 10 */
            envRec.initializeBinding(name, closure);
        }
        /* step 5/11 */
        return closure;
    }

    /**
     * 14.2 Arrow Function Definitions
     * <p>
     * 14.2.17 Runtime Semantics: Evaluation
     * <ul>
     * <li>ArrowFunction : ArrowParameters {@literal =>} ConciseBody
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new function instance
     */
    public static OrdinaryFunction EvaluateArrowFunction(RuntimeInfo.Function fd, ExecutionContext cx) {
        /* step 1 (not applicable) */
        /* step 2 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 4 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        /* step 5 */
        return closure;
    }

    /**
     * 14.3 Method Definitions, 14.5 Class Definitions
     * <p>
     * 14.3.8 Runtime Semantics: DefineMethod<br>
     * 14.5.14 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param constructorParent
     *            the constructor prototype
     * @param proto
     *            the class prototype
     * @param fd
     *            the function runtime info object
     * @param isDerived
     *            {@code true} if evaluating the constructor of a derived class
     * @param cx
     *            the execution context
     * @return the new function instance
     */
    public static OrdinaryConstructorFunction EvaluateConstructorMethod(ScriptObject constructorParent,
            OrdinaryObject proto, RuntimeInfo.Function fd, boolean isDerived, ExecutionContext cx) {
        // ClassDefinitionEvaluation - steps 11-13 (call DefineMethod)
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        ConstructorKind constructorKind = isDerived ? ConstructorKind.Derived : ConstructorKind.Base;
        OrdinaryConstructorFunction constructor = ConstructorFunctionCreate(cx, FunctionKind.ClassConstructor,
                constructorKind, fd, scope, constructorParent);
        MakeMethod(constructor, proto);

        // ClassDefinitionEvaluation - step 14 (not applicable, cf. ConstructorFunctionCreate)

        // ClassDefinitionEvaluation - step 15
        MakeConstructor(constructor, false, proto);

        // ClassDefinitionEvaluation - step 16
        MakeClassConstructor(constructor);

        // ClassDefinitionEvaluation - step 17
        proto.defineOwnProperty(cx, "constructor", new PropertyDescriptor(constructor, true, false, true));

        return constructor;
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinition(OrdinaryObject object, Object propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinition(object, (String) propKey, enumerable, fd, cx);
        } else {
            EvaluatePropertyDefinition(object, (Symbol) propKey, enumerable, fd, cx);
        }
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinition(OrdinaryObject object, String propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (DefineMethod) */
        /* DefineMethod: steps 1-3 (generated code) */
        /* DefineMethod: step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* DefineMethod: steps 5-6 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* DefineMethod: step 7 */
        MakeMethod(closure, object);
        /* step 3 */
        SetFunctionName(closure, propKey);
        /* step 4 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, enumerable, true);
        /* step 5 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinition(OrdinaryObject object, Symbol propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (Call DefineMethod) */
        /* DefineMethod: steps 1-3 (generated code) */
        /* DefineMethod: step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* DefineMethod: steps 5-6 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* DefineMethod: step 7 */
        MakeMethod(closure, object);
        /* step 3 */
        SetFunctionName(closure, propKey);
        /* step 4 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, enumerable, true);
        /* step 5 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>get PropertyName ( ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionGetter(OrdinaryObject object, Object propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinitionGetter(object, (String) propKey, enumerable, fd, cx);
        } else {
            EvaluatePropertyDefinitionGetter(object, (Symbol) propKey, enumerable, fd, cx);
        }
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>get PropertyName ( ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionGetter(OrdinaryObject object, String propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 5-6 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 7 */
        MakeMethod(closure, object);
        /* steps 8-9 */
        SetFunctionName(closure, propKey, "get");
        /* step 10 */
        PropertyDescriptor desc = AccessorPropertyDescriptor(closure, null, enumerable, true);
        /* step 11 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>get PropertyName ( ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionGetter(OrdinaryObject object, Symbol propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 5-6 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 7 */
        MakeMethod(closure, object);
        /* steps 8-9 */
        SetFunctionName(closure, propKey, "get");
        /* step 10 */
        PropertyDescriptor desc = AccessorPropertyDescriptor(closure, null, enumerable, true);
        /* step 11 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionSetter(OrdinaryObject object, Object propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinitionSetter(object, (String) propKey, enumerable, fd, cx);
        } else {
            EvaluatePropertyDefinitionSetter(object, (Symbol) propKey, enumerable, fd, cx);
        }
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionSetter(OrdinaryObject object, String propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* steps 7-8 */
        SetFunctionName(closure, propKey, "set");
        /* step 9 */
        PropertyDescriptor desc = AccessorPropertyDescriptor(null, closure, enumerable, true);
        /* step 10 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionSetter(OrdinaryObject object, Symbol propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* steps 7-8 */
        SetFunctionName(closure, propKey, "set");
        /* step 9 */
        PropertyDescriptor desc = AccessorPropertyDescriptor(null, closure, enumerable, true);
        /* step 10 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.12 Runtime Semantics: InstantiateFunctionObject
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new generator function instance
     */
    public static OrdinaryConstructorGenerator InstantiateConstructorGeneratorObject(LexicalEnvironment<?> scope,
            ExecutionContext cx, RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        OrdinaryConstructorGenerator f = ConstructorGeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 4 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 5 */
        MakeConstructor(f, true, prototype);
        /* step 6 */
        SetFunctionName(f, name);
        /* step 7 */
        return f;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.12 Runtime Semantics: InstantiateFunctionObject
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new generator function instance
     */
    public static OrdinaryGenerator InstantiateGeneratorObject(LexicalEnvironment<?> scope, ExecutionContext cx,
            RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        OrdinaryGenerator f = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 4 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 5 */
        f.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        /* step 6 */
        SetFunctionName(f, name);
        /* step 7 */
        return f;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.12 Runtime Semantics: InstantiateFunctionObject
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new generator function instance
     */
    public static OrdinaryConstructorGenerator InstantiateLegacyGeneratorObject(LexicalEnvironment<?> scope,
            ExecutionContext cx, RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        OrdinaryConstructorGenerator f = ConstructorGeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 4 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.LegacyGeneratorPrototype);
        /* step 5 */
        MakeConstructor(f, true, prototype);
        /* step 6 */
        SetFunctionName(f, name);
        /* step 7 */
        return f;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionConstructorGenerator(OrdinaryObject object, Object propKey,
            boolean enumerable, RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinitionConstructorGenerator(object, (String) propKey, enumerable, fd, cx);
        } else {
            EvaluatePropertyDefinitionConstructorGenerator(object, (Symbol) propKey, enumerable, fd, cx);
        }
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionConstructorGenerator(OrdinaryObject object, String propKey,
            boolean enumerable, RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (bytecode) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryConstructorGenerator closure = ConstructorGeneratorFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* step 7 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 8 */
        MakeConstructor(closure, true, prototype);
        /* step 9 */
        SetFunctionName(closure, propKey);
        /* step 10 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, enumerable, true);
        /* step 11 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionConstructorGenerator(OrdinaryObject object, Symbol propKey,
            boolean enumerable, RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (bytecode) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryConstructorGenerator closure = ConstructorGeneratorFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* step 7 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 8 */
        MakeConstructor(closure, true, prototype);
        /* step 9 */
        SetFunctionName(closure, propKey);
        /* step 10 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, enumerable, true);
        /* step 11 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionGenerator(OrdinaryObject object, Object propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinitionGenerator(object, (String) propKey, enumerable, fd, cx);
        } else {
            EvaluatePropertyDefinitionGenerator(object, (Symbol) propKey, enumerable, fd, cx);
        }
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionGenerator(OrdinaryObject object, String propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (bytecode) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryGenerator closure = GeneratorFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* step 7 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 8 */
        closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        /* step 9 */
        SetFunctionName(closure, propKey);
        /* step 10 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, enumerable, true);
        /* step 11 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionGenerator(OrdinaryObject object, Symbol propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (bytecode) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryGenerator closure = GeneratorFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* step 7 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 8 */
        closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        /* step 9 */
        SetFunctionName(closure, propKey);
        /* step 10 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, enumerable, true);
        /* step 11 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     * <ul>
     * <li>GeneratorExpression: function* ( FormalParameters ) { FunctionBody }
     * <li>GeneratorExpression: function* BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new generator function instance
     */
    public static OrdinaryConstructorGenerator EvaluateConstructorGeneratorExpression(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        OrdinaryConstructorGenerator closure;
        if (!fd.is(RuntimeInfo.FunctionFlags.ScopedName)) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = ConstructorGeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
            /* step 4 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
            /* step 5 */
            MakeConstructor(closure, true, prototype);
        } else {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name, false);
            /* step 7 */
            closure = ConstructorGeneratorFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
            /* step 9 */
            MakeConstructor(closure, true, prototype);
            /* step 10 */
            SetFunctionName(closure, name);
            /* step 11 */
            envRec.initializeBinding(name, closure);
        }
        /* step 6/12 */
        return closure;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     * <ul>
     * <li>GeneratorExpression: function* ( FormalParameters ) { FunctionBody }
     * <li>GeneratorExpression: function* BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new generator function instance
     */
    public static OrdinaryGenerator EvaluateGeneratorExpression(RuntimeInfo.Function fd, ExecutionContext cx) {
        OrdinaryGenerator closure;
        if (!fd.is(RuntimeInfo.FunctionFlags.ScopedName)) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
            /* step 4 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
            /* step 5 */
            closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        } else {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name, false);
            /* step 7 */
            closure = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
            /* step 9 */
            closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
            /* step 10 */
            SetFunctionName(closure, name);
            /* step 11 */
            envRec.initializeBinding(name, closure);
        }
        /* step 6/12 */
        return closure;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     * <ul>
     * <li>GeneratorExpression: function* ( FormalParameters ) { FunctionBody }
     * <li>GeneratorExpression: function* BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new generator function instance
     */
    public static OrdinaryConstructorGenerator EvaluateLegacyGeneratorExpression(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        OrdinaryConstructorGenerator closure;
        if (!fd.is(RuntimeInfo.FunctionFlags.ScopedName)) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = ConstructorGeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
            /* step 4 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.LegacyGeneratorPrototype);
            /* step 5 */
            MakeConstructor(closure, true, prototype);
        } else {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 2 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name, false);
            /* step 7 */
            closure = ConstructorGeneratorFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.LegacyGeneratorPrototype);
            /* step 9 */
            MakeConstructor(closure, true, prototype);
            /* step 10 */
            SetFunctionName(closure, name);
            /* step 11 */
            envRec.initializeBinding(name, closure);
        }
        /* step 6/12 */
        return closure;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     * <ul>
     * <li>YieldExpression : yield * AssignmentExpression
     * </ul>
     * 
     * @param cx
     *            the execution context
     * @param iterator
     *            the iterator object
     * @param e
     *            the script exception
     * @return the {@code throw} method result value
     */
    public static ScriptObject yieldThrowCompletion(ExecutionContext cx, ScriptObject iterator, ScriptException e) {
        /* step 6.b */
        /* steps 6.b.i-ii */
        Callable throwMethod = GetMethod(cx, iterator, "throw");
        /* steps 6.b.iii-iv */
        if (throwMethod != null) {
            /* step 6.b.iii */
            /* steps 6.b.iii.1-3 */
            Object innerThrowResult = throwMethod.call(cx, iterator, e.getValue());
            /* step 6.b.iii.4 */
            return requireObjectResult(innerThrowResult, "throw", cx);
        } else {
            /* step 6.b.iv */
            /* steps 6.b.iv.1-3 */
            IteratorClose(cx, iterator);
            /* steps 6.b.iv.4-5 */
            throw reportPropertyNotCallable("throw", cx);
        }
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     * <ul>
     * <li>YieldExpression : yield * AssignmentExpression
     * </ul>
     * 
     * @param cx
     *            the execution context
     * @param iterator
     *            the iterator object
     * @param e
     *            the return value wrapper
     * @return the {@code return} method result value or {@code null}
     */
    public static ScriptObject yieldReturnCompletion(ExecutionContext cx, ScriptObject iterator, ReturnValue e) {
        /* step 6.c */
        /* steps 6.c.i-iii */
        Callable returnMethod = GetMethod(cx, iterator, "return");
        /* step 6.c.iv */
        if (returnMethod == null) {
            return null;
        }
        /* steps 6.c.v-vi */
        Object innerReturnResult = returnMethod.call(cx, iterator, e.getValue());
        /* step 6.c.vii */
        return requireObjectResult(innerReturnResult, "return", cx);
    }

    public static ScriptObject requireObjectResult(Object resultValue, String methodName, ExecutionContext cx) {
        if (!Type.isObject(resultValue)) {
            throw newTypeError(cx, Messages.Key.NotObjectTypeReturned, methodName);
        }
        return Type.objectValue(resultValue);
    }

    public static ScriptException reportPropertyNotCallable(String methodName, ExecutionContext cx) {
        throw newTypeError(cx, Messages.Key.PropertyNotCallable, methodName);
    }

    public static CallSite runtimeBootstrap(MethodHandles.Lookup caller, String name, MethodType type) {
        assert "rt:stack".equals(name) || "rt:locals".equals(name);
        MethodHandle mh = MethodHandles.identity(Object[].class);
        mh = mh.asCollector(Object[].class, type.parameterCount());
        mh = mh.asType(type);
        return new ConstantCallSite(mh);
    }

    /**
     * Extension: 'function.sent' meta property
     * 
     * @param cx
     *            the execution context
     * @return the last yield value
     */
    public static Object functionSent(ExecutionContext cx) {
        return cx.getCurrentGenerator().getLastYieldValue();
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.14 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param cx
     *            the execution context
     * @return the tuple (prototype, constructorParent)
     */
    public static ScriptObject[] getDefaultClassProto(ExecutionContext cx) {
        // step 5
        ScriptObject protoParent = cx.getIntrinsic(Intrinsics.ObjectPrototype);
        ScriptObject constructorParent = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        // step 7
        OrdinaryObject proto = ObjectCreate(cx, protoParent);
        return new ScriptObject[] { constructorParent, proto };
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.14 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param cx
     *            the execution context
     * @return the tuple (prototype, constructorParent)
     */
    public static ScriptObject[] getClassProto(ExecutionContext cx) {
        // step 6
        ScriptObject protoParent = null;
        ScriptObject constructorParent = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        // step 7
        OrdinaryObject proto = ObjectCreate(cx, protoParent);
        return new ScriptObject[] { constructorParent, proto };
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.14 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param superClass
     *            the super class object
     * @param cx
     *            the execution context
     * @return the tuple (prototype, constructorParent)
     */
    public static ScriptObject[] getClassProto(Object superClass, ExecutionContext cx) {
        ScriptObject protoParent;
        ScriptObject constructorParent;
        // step 6
        if (Type.isNull(superClass)) {
            protoParent = null;
            constructorParent = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        } else if (!IsConstructor(superClass)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        } else {
            Constructor superClassObj = (Constructor) superClass;
            if (superClassObj instanceof OrdinaryConstructorGenerator) {
                throw newTypeError(cx, Messages.Key.InvalidSuperClass);
            }
            Object p = Get(cx, superClassObj, "prototype");
            if (!Type.isObjectOrNull(p)) {
                throw newTypeError(cx, Messages.Key.NotObjectOrNull);
            }
            protoParent = Type.objectValueOrNull(p);
            constructorParent = superClassObj;
        }
        // step 7
        OrdinaryObject proto = ObjectCreate(cx, protoParent);
        return new ScriptObject[] { constructorParent, proto };
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new async function instance
     */
    public static OrdinaryAsyncFunction InstantiateAsyncFunctionObject(LexicalEnvironment<?> scope, ExecutionContext cx,
            RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        OrdinaryAsyncFunction f = AsyncFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 4 */
        SetFunctionName(f, name);
        /* step 5 */
        return f;
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new async function instance
     */
    public static OrdinaryAsyncFunction EvaluateAsyncFunctionExpression(RuntimeInfo.Function fd, ExecutionContext cx) {
        OrdinaryAsyncFunction closure;
        if (!fd.is(RuntimeInfo.FunctionFlags.ScopedName)) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = AsyncFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        } else {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name, false);
            /* step 7 */
            closure = AsyncFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            SetFunctionName(closure, name);
            /* step 9 */
            envRec.initializeBinding(name, closure);
        }
        /* step 4/10 */
        return closure;
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new async function instance
     */
    public static OrdinaryAsyncFunction EvaluateAsyncArrowFunction(RuntimeInfo.Function fd, ExecutionContext cx) {
        /* step 1 (not applicable) */
        /* step 2 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 3-4/3-5 */
        OrdinaryAsyncFunction closure = AsyncFunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        /* step 5/6 */
        return closure;
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     */
    public static void EvaluatePropertyDefinitionAsync(OrdinaryObject object, Object propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinitionAsync(object, (String) propKey, enumerable, fd, cx);
        } else {
            EvaluatePropertyDefinitionAsync(object, (Symbol) propKey, enumerable, fd, cx);
        }
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     */
    public static void EvaluatePropertyDefinitionAsync(OrdinaryObject object, String propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (bytecode) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryAsyncFunction closure = AsyncFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* step 7 */
        SetFunctionName(closure, propKey);
        /* step 8 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, enumerable, true);
        /* step 9 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionAsync(OrdinaryObject object, Symbol propKey, boolean enumerable,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (bytecode) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryAsyncFunction closure = AsyncFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* step 7 */
        SetFunctionName(closure, propKey);
        /* step 8 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, enumerable, true);
        /* step 9 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * Extension: Async Generator Function Definitions
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new async generator instance
     */
    public static OrdinaryAsyncGenerator InstantiateAsyncGeneratorObject(LexicalEnvironment<?> scope,
            ExecutionContext cx, RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        OrdinaryAsyncGenerator f = AsyncGeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 4 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.AsyncGeneratorPrototype);
        /* step 5 */
        f.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        /* step 6 */
        SetFunctionName(f, name);
        /* step 7 */
        return f;
    }

    /**
     * Extension: Async Generator Function Definitions
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new async generator instance
     */
    public static OrdinaryAsyncGenerator EvaluateAsyncGeneratorExpression(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        OrdinaryAsyncGenerator closure;
        if (!fd.is(RuntimeInfo.FunctionFlags.ScopedName)) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = AsyncGeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
            /* step 4 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.AsyncGeneratorPrototype);
            /* step 5 */
            closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        } else {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name, false);
            /* step 7 */
            closure = AsyncGeneratorFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.AsyncGeneratorPrototype);
            /* step 9 */
            closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
            /* step 10 */
            SetFunctionName(closure, name);
            /* step 11 */
            envRec.initializeBinding(name, closure);
        }
        /* step 6/12 */
        return closure;
    }

    /**
     * Extension: Async Generator Function Definitions
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     */
    public static void EvaluatePropertyDefinitionAsyncGenerator(OrdinaryObject object, Object propKey,
            boolean enumerable, RuntimeInfo.Function fd, ExecutionContext cx) {
        if (propKey instanceof String) {
            EvaluatePropertyDefinitionAsyncGenerator(object, (String) propKey, enumerable, fd, cx);
        } else {
            EvaluatePropertyDefinitionAsyncGenerator(object, (Symbol) propKey, enumerable, fd, cx);
        }
    }

    /**
     * Extension: Async Generator Function Definitions
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     */
    public static void EvaluatePropertyDefinitionAsyncGenerator(OrdinaryObject object, String propKey,
            boolean enumerable, RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (bytecode) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryAsyncGenerator closure = AsyncGeneratorFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* step 7 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.AsyncGeneratorPrototype);
        /* step 8 */
        closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        /* step 9 */
        SetFunctionName(closure, propKey);
        /* step 10 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, enumerable, true);
        /* step 11 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * Extension: Async Generator Function Definitions
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param enumerable
     *            the enumerable property attribute
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     */
    public static void EvaluatePropertyDefinitionAsyncGenerator(OrdinaryObject object, Symbol propKey,
            boolean enumerable, RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (bytecode) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryAsyncGenerator closure = AsyncGeneratorFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* step 7 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.AsyncGeneratorPrototype);
        /* step 8 */
        closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        /* step 9 */
        SetFunctionName(closure, propKey);
        /* step 10 */
        PropertyDescriptor desc = new PropertyDescriptor(closure, true, enumerable, true);
        /* step 11 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * Extension: Class Decorators
     * 
     * @param f
     *            the function object
     * @param decorators
     *            the list of decorator functions
     * @param cx
     *            the execution context
     */
    public static void EvaluateClassDecorators(OrdinaryConstructorFunction f, ArrayList<Callable> decorators,
            ExecutionContext cx) {
        for (Callable decorator : decorators) {
            decorator.call(cx, UNDEFINED, f);
        }
    }

    /**
     * Extension: Method Decorators
     * 
     * @param object
     *            the script object
     * @param decorators
     *            the list of method decorators
     * @param cx
     *            the execution context
     */
    public static void EvaluateMethodDecorators(OrdinaryObject object, ArrayList<Object> decorators,
            ExecutionContext cx) {
        // TODO: Deserves clean-up when proper evaluation semantics are specified.
        // decorators = object, list of <1..n callable, property key>
        for (int i = 0, size = decorators.size(); i < size;) {
            int count = evaluateMethodDecorators(object, decorators, i, cx);
            i += count + 1;
        }
    }

    /**
     * Extension: Class Method Decorators
     * 
     * @param decorators
     *            the list of method decorators
     * @param cx
     *            the execution context
     */
    public static void EvaluateClassMethodDecorators(ArrayList<Object> decorators, ExecutionContext cx) {
        // TODO: Deserves clean-up when proper evaluation semantics are specified.
        // decorators = list of <object, 1..n callable, property key>
        for (int i = 0, size = decorators.size(); i < size;) {
            OrdinaryObject object = (OrdinaryObject) decorators.get(i);
            int count = evaluateMethodDecorators(object, decorators, i + 1, cx);
            i += count + 2;
        }
    }

    private static int evaluateMethodDecorators(OrdinaryObject object, ArrayList<Object> decorators, int start,
            ExecutionContext cx) {
        int count = 0;
        for (int i = start, size = decorators.size(); i < size; ++i, ++count) {
            if (!(decorators.get(i) instanceof Callable))
                break;
        }
        assert count > 0;
        Object propKey = decorators.get(start + count);
        Property property = object.getOwnProperty(cx, propKey);
        // Current proposal uses `undefined` instead of the initial property descriptor in, and only
        // in, object literals. We don't support this distinction between decorators for object and
        // decorators for class methods.
        Object desc = FromPropertyDescriptor(cx, property);
        for (int i = start; i < start + count; ++i) {
            Callable decorator = (Callable) decorators.get(i);
            Object result = decorator.call(cx, UNDEFINED, object, propKey, desc);
            if (Type.isObject(result)) {
                // So, this means a bad decorator can mess up all following decorators?
                // Example: `({ @(()=>({})) @((o,p,d)=>{ print(JSON.stringify(d)) }) m() {} })`
                desc = result;
            }
        }
        if (Type.isObject(desc)) {
            PropertyDescriptor pdesc = ToPropertyDescriptor(cx, desc);
            DefinePropertyOrThrow(cx, object, propKey, pdesc);
        }
        return count;
    }

    /**
     * Extension: Object Spread Initializer
     * <p>
     * Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>PropertyDefinition : ... AssignmentExpression
     * </ul>
     * 
     * @param object
     *            the script object
     * @param value
     *            the spread value
     * @param cx
     *            the execution context
     */
    public static void defineSpreadProperty(OrdinaryObject object, Object value, ExecutionContext cx) {
        CopyDataProperties(cx, object, value, Collections.EMPTY_SET);
    }

    /**
     * Extension: Object Rest Destructuring
     * <ul>
     * <li>Runtime Semantics: DestructuringAssignmentEvaluation
     * <li>Runtime Semantics: BindingInitialization
     * </ul>
     * 
     * @param value
     *            the rest property
     * @param excludedNames
     *            the excluded property names
     * @param cx
     *            the execution context
     * @return the rest object
     */
    public static OrdinaryObject createRestObject(Object value, Set<?> excludedNames, ExecutionContext cx) {
        OrdinaryObject restObj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        return CopyDataProperties(cx, restObj, value, excludedNames);
    }

    /**
     * 14.6 Tail Position Calls
     * <p>
     * 14.6.1 Runtime Semantics: PrepareForTailCall
     * 
     * @param args
     *            the function arguments
     * @param thisValue
     *            the function this-value
     * @param function
     *            the tail call function
     * @return the tail call trampoline object
     */
    public static Object PrepareForTailCall(Callable function, Object thisValue, Object[] args) {
        return newTailCallInvocation(function, thisValue, args);
    }

    // Called from generated code
    public static Object PrepareForTailCall(Callable function, ExecutionContext cx, Object thisValue, Object[] args) {
        return newTailCallInvocation(function, thisValue, args);
    }

    // Called from generated code
    public static Object PrepareForTailCall(Object function, ExecutionContext cx, Object thisValue, Object[] args) {
        return newTailCallInvocation(CheckCallable(function, cx), thisValue, args);
    }

    /* ***************************************************************************************** */

    /**
     * B.3.1 __proto___ Property Names in Object Initializers
     * 
     * @param object
     *            the object instance
     * @param value
     *            the new prototype
     * @param cx
     *            the execution context
     */
    public static void defineProtoProperty(OrdinaryObject object, Object value, ExecutionContext cx) {
        if (Type.isObjectOrNull(value)) {
            object.setPrototypeOf(cx, Type.objectValueOrNull(value));
        }
    }
}
