/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.HasClassInstanceDefinitions;

import java.util.function.Consumer;

import com.github.anba.es6draft.ast.ClassDefinition;
import com.github.anba.es6draft.ast.FunctionDefinition;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.FunctionNode.ThisMode;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionCode;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.FieldName;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.TryCatchLabel;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.LegacyConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncGenerator;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;

/**
 * Generates bytecode for the function entry method
 */
final class FunctionCodeGenerator {
    private static final class Fields {
        static final FieldName Intrinsics_ObjectPrototype = FieldName.findStatic(Types.Intrinsics, "ObjectPrototype",
                Types.Intrinsics);

        static final FieldName MessagesKey_InvalidCallClass = FieldName.findStatic(Types.Messages$Key,
                "InvalidCallClass", Types.Messages$Key);

        static final FieldName MessagesKey_NotObjectTypeFromConstructor = FieldName.findStatic(Types.Messages$Key,
                "NotObjectTypeFromConstructor", Types.Messages$Key);
    }

    private static final class Methods {
        // ExecutionContext
        static final MethodName ExecutionContext_newFunctionExecutionContext = MethodName.findStatic(
                Types.ExecutionContext, "newFunctionExecutionContext",
                Type.methodType(Types.ExecutionContext, Types.FunctionObject, Types.LexicalEnvironment));

        static final MethodName ExecutionContext_getCurrentFunction = MethodName.findVirtual(Types.ExecutionContext,
                "getCurrentFunction", Type.methodType(Types.FunctionObject));

        static final MethodName ExecutionContext_getFunctionVariableEnvironmentRecord = MethodName.findVirtual(
                Types.ExecutionContext, "getFunctionVariableEnvironmentRecord",
                Type.methodType(Types.FunctionEnvironmentRecord));

        // FunctionEnvironmentRecord
        static final MethodName FunctionEnvironmentRecord_getThisBinding = MethodName.findVirtual(
                Types.FunctionEnvironmentRecord, "getThisBinding",
                Type.methodType(Types.Object, Types.ExecutionContext));

        // LegacyFunction
        static final MethodName LegacyConstructorFunction_getLegacyArguments = MethodName.findVirtual(
                Types.LegacyConstructorFunction, "getLegacyArguments",
                Type.methodType(Types.LegacyConstructorFunction$Arguments));

        static final MethodName LegacyConstructorFunction_getLegacyCaller = MethodName
                .findVirtual(Types.LegacyConstructorFunction, "getLegacyCaller", Type.methodType(Types.FunctionObject));

        static final MethodName LegacyConstructorFunction_setLegacyCaller = MethodName.findVirtual(
                Types.LegacyConstructorFunction, "setLegacyCaller",
                Type.methodType(Type.VOID_TYPE, Types.FunctionObject));

        static final MethodName LegacyConstructorFunction_setLegacyArguments = MethodName.findVirtual(
                Types.LegacyConstructorFunction, "setLegacyArguments",
                Type.methodType(Type.VOID_TYPE, Types.LegacyConstructorFunction$Arguments));

        static final MethodName LegacyConstructorFunction$Arguments_new = MethodName.findConstructor(
                Types.LegacyConstructorFunction$Arguments, Type.methodType(Type.VOID_TYPE, Types.Object_));

        // LexicalEnvironment
        static final MethodName LexicalEnvironment_newFunctionEnvironment = MethodName.findStatic(
                Types.LexicalEnvironment, "newFunctionEnvironment",
                Type.methodType(Types.LexicalEnvironment, Types.FunctionObject, Types.Constructor, Types.Object));

        static final MethodName LexicalEnvironment_newFunctionEnvironment_ConstructDerived = MethodName.findStatic(
                Types.LexicalEnvironment, "newFunctionEnvironment",
                Type.methodType(Types.LexicalEnvironment, Types.FunctionObject, Types.Constructor));

        // OrdinaryAsyncFunction
        static final MethodName OrdinaryAsyncFunction_EvaluateBody = MethodName.findStatic(Types.OrdinaryAsyncFunction,
                "EvaluateBody",
                Type.methodType(Types.PromiseObject, Types.ExecutionContext, Types.OrdinaryAsyncFunction));

        // OrdinaryAsyncGenerator
        static final MethodName OrdinaryAsyncGenerator_EvaluateBody = MethodName.findStatic(
                Types.OrdinaryAsyncGenerator, "EvaluateBody",
                Type.methodType(Types.AsyncGeneratorObject, Types.ExecutionContext, Types.OrdinaryAsyncGenerator));

        // OrdinaryGenerator
        static final MethodName OrdinaryGenerator_EvaluateBody = MethodName.findStatic(Types.OrdinaryGenerator,
                "EvaluateBody",
                Type.methodType(Types.GeneratorObject, Types.ExecutionContext, Types.OrdinaryGenerator));

        // OrdinaryObject
        static final MethodName OrdinaryObject_OrdinaryCreateFromConstructor = MethodName.findStatic(
                Types.OrdinaryObject, "OrdinaryCreateFromConstructor",
                Type.methodType(Types.OrdinaryObject, Types.ExecutionContext, Types.Callable, Types.Intrinsics));

        // class: Errors
        static final MethodName Errors_newTypeError = MethodName.findStatic(Types.Errors, "newTypeError",
                Type.methodType(Types.ScriptException, Types.ExecutionContext, Types.Messages$Key));

        // class: TailCallInvocation
        static final MethodName TailCallInvocation_toConstructTailCall = MethodName.findVirtual(
                Types.TailCallInvocation, "toConstructTailCall",
                Type.methodType(Types.TailCallInvocation, Types.ScriptObject));

        static final MethodName TailCallInvocation_toConstructTailCallWithEnvironment = MethodName.findVirtual(
                Types.TailCallInvocation, "toConstructTailCall",
                Type.methodType(Types.TailCallInvocation, Types.FunctionEnvironmentRecord));

        // class: FunctionOperations
        static final MethodName FunctionOperations_functionThisValue = MethodName.findStatic(Types.FunctionOperations,
                "functionThisValue", Type.methodType(Types.ScriptObject, Types.FunctionObject, Types.Object));

        // class: PromiseAbstractOperations
        static final MethodName PromiseAbstractOperations_PromiseOf = MethodName.findStatic(
                Types.PromiseAbstractOperations, "PromiseOf",
                Type.methodType(Types.PromiseObject, Types.ExecutionContext, Types.ScriptException));

        // class: ClassOperations
        static final MethodName ClassOperations_BindThisValue = MethodName.findStatic(Types.ClassOperations,
                "BindThisValue", Type.methodType(Type.VOID_TYPE, Types.ScriptObject, Types.ExecutionContext));

        static final MethodName ClassOperations_InitializeInstanceFields = MethodName.findStatic(Types.ClassOperations,
                "InitializeInstanceFields", Type.methodType(Type.VOID_TYPE, Types.ScriptObject,
                        Types.OrdinaryConstructorFunction, Types.ExecutionContext));
    }

    private FunctionCodeGenerator() {
    }

    private static final class CallMethodVisitor extends InstructionVisitor {
        CallMethodVisitor(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterNameUnchecked("function", 0);
            setParameterName("callerContext", 1, Types.ExecutionContext);
            setParameterName("thisValue", 2, Types.Object);
            setParameterName("arguments", 3, Types.Object_);
        }

        <F extends FunctionObject> Variable<F> getFunction(Class<F> clazz) {
            return getParameter(0, clazz);
        }

        Variable<ExecutionContext> getCallerContext() {
            return getParameter(1, ExecutionContext.class);
        }

        Variable<Object> getThisValue() {
            return getParameter(2, Object.class);
        }

        Variable<Object[]> getArguments() {
            return getParameter(3, Object[].class);
        }
    }

    private static final class ConstructMethodVisitor extends InstructionVisitor {
        ConstructMethodVisitor(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterNameUnchecked("function", 0);
            setParameterName("callerContext", 1, Types.ExecutionContext);
            setParameterName("newTarget", 2, Types.Constructor);
            setParameterName("arguments", 3, Types.Object_);
        }

        <F extends FunctionObject> Variable<F> getFunction(Class<F> clazz) {
            return getParameter(0, clazz);
        }

        Variable<ExecutionContext> getCallerContext() {
            return getParameter(1, ExecutionContext.class);
        }

        Variable<Constructor> getNewTarget() {
            return getParameter(2, Constructor.class);
        }

        Variable<Object[]> getArguments() {
            return getParameter(3, Object[].class);
        }
    }

    private static void callMethod(FunctionNode node, MethodCode method, Consumer<CallMethodVisitor> compiler) {
        CallMethodVisitor mv = new CallMethodVisitor(method);
        mv.lineInfo(node);
        mv.begin();

        compiler.accept(mv);

        mv.end();
    }

    private static void constructMethod(FunctionNode node, MethodCode method,
            Consumer<ConstructMethodVisitor> compiler) {
        ConstructMethodVisitor mv = new ConstructMethodVisitor(method);
        mv.lineInfo(node);
        mv.begin();

        compiler.accept(mv);

        mv.end();
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * calleeContext = newFunctionExecutionContext(function, null, thisValue)
     * result = OrdinaryCallEvaluateBody(function, argumentsList)
     * return returnResultOrUndefined(result)
     * </pre>
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param method
     *            the bytecode method
     * @param function
     *            the script function
     */
    static void functionCall(CodeGenerator codegen, FunctionNode node, MethodCode method, FunctionCode function) {
        callMethod(node, method, mv -> {
            Variable<OrdinaryFunction> fn = mv.getFunction(OrdinaryFunction.class);
            Variable<Object> thisValue = mv.getThisValue();
            Variable<Object[]> arguments = mv.getArguments();

            Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext", ExecutionContext.class);

            // (1) Create a new ExecutionContext
            /* steps 1-6 */
            prepareCallAndBindThis(node, calleeContext, fn, thisValue, mv);

            // (2) Call OrdinaryCallEvaluateBody
            /* steps 7-8 */
            ordinaryCallEvaluateBody(function.instantiation, function.body, calleeContext, fn, arguments, mv);

            // (3) Return result value
            /* steps 9-11 */
            mv._return();
        });
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * calleeContext = newFunctionExecutionContext(function, null, thisValue)
     * result = OrdinaryCallEvaluateBody(function, argumentsList)
     * return returnResultOrUndefined(result)
     * </pre>
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param method
     *            the bytecode method
     * @param function
     *            the script function
     */
    static void constructorFunctionCall(CodeGenerator codegen, FunctionNode node, MethodCode method,
            FunctionCode function) {
        callMethod(node, method, mv -> {
            Variable<OrdinaryConstructorFunction> fn = mv.getFunction(OrdinaryConstructorFunction.class);
            Variable<Object> thisValue = mv.getThisValue();
            Variable<Object[]> arguments = mv.getArguments();

            Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext", ExecutionContext.class);

            // (1) Create a new ExecutionContext
            /* steps 1-6 */
            prepareCallAndBindThis(node, calleeContext, fn, thisValue, mv);

            // (2) Call OrdinaryCallEvaluateBody
            /* steps 7-8 */
            ordinaryCallEvaluateBody(function.instantiation, function.body, calleeContext, fn, arguments, mv);

            // (3) Return result value
            /* steps 9-11 */
            mv._return();
        });
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * oldCaller = function.getLegacyCaller()
     * oldArguments = function.getLegacyArguments()
     * function.setLegacyCaller(callerContext.getCurrentFunction())
     * try {
     *   calleeContext = newFunctionExecutionContext(function, null, thisValue)
     *   return OrdinaryCallEvaluateBody(function, argumentsList)
     * } finally {
     *   function.restoreLegacyProperties(oldCaller, oldArguments)
     * }
     * </pre>
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param method
     *            the bytecode method
     * @param function
     *            the script function
     */
    static void legacyFunctionCall(CodeGenerator codegen, FunctionDefinition node, MethodCode method,
            FunctionCode function) {
        callMethod(node, method, mv -> {
            final boolean hasArguments = codegen.isEnabled(CompatibilityOption.FunctionArguments);
            final boolean hasCaller = codegen.isEnabled(CompatibilityOption.FunctionCaller);

            Variable<LegacyConstructorFunction> fn = mv.getFunction(LegacyConstructorFunction.class);
            Variable<ExecutionContext> callerContext = mv.getCallerContext();
            Variable<Object> thisValue = mv.getThisValue();
            Variable<Object[]> arguments = mv.getArguments();

            Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext", ExecutionContext.class);
            Variable<FunctionObject> oldCaller = mv.newVariable("oldCaller", FunctionObject.class);
            Variable<LegacyConstructorFunction.Arguments> oldArguments = mv.newVariable("oldArguments",
                    LegacyConstructorFunction.Arguments.class);
            Variable<Throwable> throwable = mv.newVariable("throwable", Throwable.class);

            // (1) Retrieve 'caller' and 'arguments' and store in local variables
            if (hasCaller) {
                getLegacyCaller(fn, mv);
            } else {
                mv.anull();
            }
            mv.store(oldCaller);

            if (hasArguments) {
                getLegacyArguments(fn, mv);
            } else {
                mv.anull();
            }
            mv.store(oldArguments);

            // (2) Update 'caller' and 'arguments' properties
            if (hasCaller) {
                setLegacyCaller(fn, callerContext, mv);
            }
            if (hasArguments) {
                setLegacyArguments(fn, arguments, mv);
            }

            TryCatchLabel startFinally = new TryCatchLabel(), endFinally = new TryCatchLabel();
            TryCatchLabel handlerFinally = new TryCatchLabel();
            mv.mark(startFinally);
            {
                // (3) Create a new ExecutionContext
                prepareCallAndBindThis(node, calleeContext, fn, thisValue, mv);

                // (4) Call OrdinaryCallEvaluateBody
                ordinaryCallEvaluateBody(function.instantiation, function.body, calleeContext, fn, arguments, mv);

                // (5) Restore 'caller' and 'arguments'
                restoreLegacyProperties(fn, oldCaller, oldArguments, mv);

                // (6) Return result value
                mv._return();
            }
            mv.mark(endFinally);

            // Exception: Restore 'caller' and 'arguments' and then rethrow exception
            mv.finallyHandler(handlerFinally);
            mv.store(throwable);
            restoreLegacyProperties(fn, oldCaller, oldArguments, mv);
            mv.load(throwable);
            mv.athrow();

            mv.tryFinally(startFinally, endFinally, handlerFinally);
        });
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * calleeContext = newFunctionExecutionContext(function, null, thisValue)
     * function_init(calleeContext, function, arguments)
     * return EvaluateBody(calleeContext, generator)
     * </pre>
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param method
     *            the bytecode method
     * @param function
     *            the script function
     */
    static void asyncFunctionCall(CodeGenerator codegen, FunctionNode node, MethodCode method, FunctionCode function) {
        callMethod(node, method, mv -> {
            Variable<OrdinaryAsyncFunction> fn = mv.getFunction(OrdinaryAsyncFunction.class);
            Variable<Object> thisValue = mv.getThisValue();
            Variable<Object[]> arguments = mv.getArguments();

            Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext", ExecutionContext.class);

            // (1) Create a new ExecutionContext
            prepareCallAndBindThis(node, calleeContext, fn, thisValue, mv);

            // (2) Perform FunctionDeclarationInstantiation
            {
                TryCatchLabel startCatch = new TryCatchLabel();
                TryCatchLabel endCatch = new TryCatchLabel(), handlerCatch = new TryCatchLabel();
                Jump noException = new Jump();

                mv.mark(startCatch);
                functionDeclarationInstantiation(function.instantiation, calleeContext, fn, arguments, mv);
                mv.goTo(noException);
                mv.mark(endCatch);
                mv.catchHandler(handlerCatch, Types.ScriptException);
                {
                    // stack: [exception] -> [cx, exception]
                    mv.load(calleeContext);
                    mv.swap();
                    // stack: [cx, exception] -> [promise]
                    mv.invoke(Methods.PromiseAbstractOperations_PromiseOf);
                    mv._return();
                }
                mv.mark(noException);
                mv.tryCatch(startCatch, endCatch, handlerCatch, Types.ScriptException);
            }

            // (3) Perform EvaluateBody
            mv.load(calleeContext);
            mv.load(fn);
            mv.invoke(Methods.OrdinaryAsyncFunction_EvaluateBody);

            // (4) Return result value
            mv._return();
        });
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * calleeContext = newFunctionExecutionContext(generator, null, thisValue)
     * function_init(calleeContext, generator, arguments)
     * return EvaluateBody(calleeContext, generator)
     * </pre>
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param method
     *            the bytecode method
     * @param function
     *            the script function
     */
    static void asyncGeneratorCall(CodeGenerator codegen, FunctionNode node, MethodCode method, FunctionCode function) {
        callMethod(node, method, mv -> {
            Variable<OrdinaryAsyncGenerator> generator = mv.getFunction(OrdinaryAsyncGenerator.class);
            Variable<Object> thisValue = mv.getThisValue();
            Variable<Object[]> arguments = mv.getArguments();

            Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext", ExecutionContext.class);

            // (1) Create a new ExecutionContext
            prepareCallAndBindThis(node, calleeContext, generator, thisValue, mv);

            // (2) Perform OrdinaryCallEvaluateBody - FunctionDeclarationInstantiation
            functionDeclarationInstantiation(function.instantiation, calleeContext, generator, arguments, mv);

            // (3) Perform OrdinaryCallEvaluateBody - EvaluateBody
            mv.load(calleeContext);
            mv.load(generator);
            mv.invoke(Methods.OrdinaryAsyncGenerator_EvaluateBody);

            // (4) Return result value
            mv._return();
        });
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * calleeContext = newFunctionExecutionContext(generator, null, thisValue)
     * function_init(calleeContext, generator, arguments)
     * return EvaluateBody(calleeContext, generator)
     * </pre>
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param method
     *            the bytecode method
     * @param function
     *            the script function
     */
    static void generatorCall(CodeGenerator codegen, FunctionNode node, MethodCode method, FunctionCode function) {
        callMethod(node, method, mv -> {
            Variable<OrdinaryGenerator> generator = mv.getFunction(OrdinaryGenerator.class);
            Variable<Object> thisValue = mv.getThisValue();
            Variable<Object[]> arguments = mv.getArguments();

            Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext", ExecutionContext.class);

            // (1) Create a new ExecutionContext
            prepareCallAndBindThis(node, calleeContext, generator, thisValue, mv);

            // (2) Perform OrdinaryCallEvaluateBody - FunctionDeclarationInstantiation
            functionDeclarationInstantiation(function.instantiation, calleeContext, generator, arguments, mv);

            // (3) Perform OrdinaryCallEvaluateBody - EvaluateBody
            mv.load(calleeContext);
            mv.load(generator);
            mv.invoke(Methods.OrdinaryGenerator_EvaluateBody);

            // (4) Return result value
            mv._return();
        });
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * throw Errors.newTypeError()
     * </pre>
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param method
     *            the bytecode method
     * @param function
     *            the script function
     */
    static void classConstructorCall(CodeGenerator codegen, ClassDefinition node, MethodCode method,
            FunctionCode function) {
        callMethod(node.getConstructor(), method, mv -> {
            Variable<ExecutionContext> callerContext = mv.getCallerContext();

            // 9.2.2 [[Call]] ( thisArgument, argumentsList) - step 2
            mv.load(callerContext);
            mv.get(Fields.MessagesKey_InvalidCallClass);
            mv.invoke(Methods.Errors_newTypeError);
            mv.athrow();
        });
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * thisArgument = OrdinaryCreateFromConstructor(callerContext, newTarget, %ObjectPrototype%)
     * calleeContext = newFunctionExecutionContext(function, newTarget, thisArgument)
     * result = OrdinaryCallEvaluateBody(function, argumentsList)
     * return returnResultOrThis(result)
     * </pre>
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param method
     *            the bytecode method
     * @param function
     *            the script function
     */
    static void functionConstruct(CodeGenerator codegen, FunctionNode node, MethodCode method, FunctionCode function) {
        constructMethod(node, method, mv -> {
            Variable<OrdinaryConstructorFunction> fn = mv.getFunction(OrdinaryConstructorFunction.class);
            Variable<ExecutionContext> callerContext = mv.getCallerContext();
            Variable<Constructor> newTarget = mv.getNewTarget();
            Variable<Object[]> arguments = mv.getArguments();

            Variable<ScriptObject> thisArgument = mv.newVariable("thisArgument", ScriptObject.class);
            Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext", ExecutionContext.class);

            /* steps 1-5 */
            ordinaryCreateFromConstructor(callerContext, newTarget, thisArgument, mv);

            // (1) Create a new ExecutionContext
            /* steps 6-10 */
            prepareCallAndBindThis(node, calleeContext, fn, newTarget, thisArgument, mv);

            // (2) Call OrdinaryCallEvaluateBody
            /* steps 11-12 */
            ordinaryCallEvaluateBody(function.instantiation, function.body, calleeContext, fn, arguments, mv);

            // (3) Return result value
            /* steps 13-15 */
            returnResultOrThis(thisArgument, function.tailCall, mv);
        });
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * oldCaller = function.getLegacyCaller()
     * oldArguments = function.getLegacyArguments()
     * function.setLegacyCaller(callerContext.getCurrentFunction())
     * try {
     *   thisArgument = OrdinaryCreateFromConstructor(callerContext, newTarget, %ObjectPrototype%)
     *   calleeContext = newFunctionExecutionContext(function, newTarget, thisArgument)
     *   result = OrdinaryCallEvaluateBody(function, argumentsList)
     *   return returnResultOrThis(result)
     * } finally {
     *   function.restoreLegacyProperties(oldCaller, oldArguments)
     * }
     * </pre>
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param method
     *            the bytecode method
     * @param function
     *            the script function
     */
    static void legacyFunctionConstruct(CodeGenerator codegen, FunctionDefinition node, MethodCode method,
            FunctionCode function) {
        constructMethod(node, method, mv -> {
            final boolean hasArguments = codegen.isEnabled(CompatibilityOption.FunctionArguments);
            final boolean hasCaller = codegen.isEnabled(CompatibilityOption.FunctionCaller);

            Variable<LegacyConstructorFunction> fn = mv.getFunction(LegacyConstructorFunction.class);
            Variable<ExecutionContext> callerContext = mv.getCallerContext();
            Variable<Constructor> newTarget = mv.getNewTarget();
            Variable<Object[]> arguments = mv.getArguments();

            Variable<ScriptObject> thisArg = mv.newVariable("thisArgument", ScriptObject.class);
            Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext", ExecutionContext.class);
            Variable<FunctionObject> oldCaller = mv.newVariable("oldCaller", FunctionObject.class);
            Variable<LegacyConstructorFunction.Arguments> oldArguments = mv.newVariable("oldArguments",
                    LegacyConstructorFunction.Arguments.class);
            Variable<Throwable> throwable = mv.newVariable("throwable", Throwable.class);

            // (1) Retrieve 'caller' and 'arguments' and store in local variables
            if (hasCaller) {
                getLegacyCaller(fn, mv);
            } else {
                mv.anull();
            }
            mv.store(oldCaller);

            if (hasArguments) {
                getLegacyArguments(fn, mv);
            } else {
                mv.anull();
            }
            mv.store(oldArguments);

            // (2) Update 'caller' and 'arguments' properties
            if (hasCaller) {
                setLegacyCaller(fn, callerContext, mv);
            }
            if (hasArguments) {
                setLegacyArguments(fn, arguments, mv);
            }

            TryCatchLabel startFinally = new TryCatchLabel(), endFinally = new TryCatchLabel();
            TryCatchLabel handlerFinally = new TryCatchLabel();
            mv.mark(startFinally);
            {
                // (3) Create this-argument
                ordinaryCreateFromConstructor(callerContext, newTarget, thisArg, mv);

                // (4) Create a new ExecutionContext
                prepareCallAndBindThis(node, calleeContext, fn, newTarget, thisArg, mv);

                // (5) Call OrdinaryCallEvaluateBody
                ordinaryCallEvaluateBody(function.instantiation, function.body, calleeContext, fn, arguments, mv);

                // (6) Restore 'caller' and 'arguments'
                restoreLegacyProperties(fn, oldCaller, oldArguments, mv);

                // (7) Return result value
                returnResultOrThis(thisArg, false, mv);
            }
            mv.mark(endFinally);

            // Exception: Restore 'caller' and 'arguments' and then rethrow exception
            mv.finallyHandler(handlerFinally);
            mv.store(throwable);
            restoreLegacyProperties(fn, oldCaller, oldArguments, mv);
            mv.load(throwable);
            mv.athrow();

            mv.tryFinally(startFinally, endFinally, handlerFinally);
        });
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * thisArgument = OrdinaryCreateFromConstructor(callerContext, newTarget, %ObjectPrototype%)
     * calleeContext = newFunctionExecutionContext(function, newTarget)
     * BindThisValue(calleeContext, thisArgument)
     * result = OrdinaryCallEvaluateBody(function, argumentsList)
     * return returnResultOrThis(result)
     * </pre>
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param method
     *            the bytecode method
     * @param function
     *            the script function
     */
    static void baseClassConstruct(CodeGenerator codegen, ClassDefinition node, MethodCode method,
            FunctionCode function) {
        constructMethod(node.getConstructor(), method, mv -> {
            Variable<OrdinaryConstructorFunction> fn = mv.getFunction(OrdinaryConstructorFunction.class);
            Variable<ExecutionContext> callerContext = mv.getCallerContext();
            Variable<Constructor> newTarget = mv.getNewTarget();
            Variable<Object[]> arguments = mv.getArguments();

            Variable<ScriptObject> thisArgument = mv.newVariable("thisArgument", ScriptObject.class);
            Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext", ExecutionContext.class);

            /* steps 1-5 */
            ordinaryCreateFromConstructor(callerContext, newTarget, thisArgument, mv);

            // (1) Create a new ExecutionContext
            /* steps 6-7 */
            prepareCall(calleeContext, fn, newTarget, mv);

            /* step 8 */
            mv.load(thisArgument);
            mv.load(calleeContext);
            mv.invoke(Methods.ClassOperations_BindThisValue);

            // Extension: Class Fields
            if (HasClassInstanceDefinitions(node)) {
                mv.load(thisArgument);
                mv.load(fn);
                mv.load(calleeContext);
                mv.invoke(Methods.ClassOperations_InitializeInstanceFields);
            }

            /* steps 9-10 (not applicable) */

            // (2) Call OrdinaryCallEvaluateBody
            /* steps 11-12 */
            ordinaryCallEvaluateBody(function.instantiation, function.body, calleeContext, fn, arguments, mv);

            // (3) Return result value
            /* steps 13-15 */
            returnResultOrThis(thisArgument, function.tailCall, mv);
        });
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * calleeContext = newFunctionExecutionContext(function, newTarget)
     * result = OrdinaryCallEvaluateBody(function, argumentsList)
     * return returnResultOrThis(result)
     * </pre>
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param method
     *            the bytecode method
     * @param function
     *            the script function
     */
    static void derivedClassConstruct(CodeGenerator codegen, ClassDefinition node, MethodCode method,
            FunctionCode function) {
        constructMethod(node.getConstructor(), method, mv -> {
            Variable<OrdinaryConstructorFunction> fn = mv.getFunction(OrdinaryConstructorFunction.class);
            Variable<ExecutionContext> callerContext = mv.getCallerContext();
            Variable<Constructor> newTarget = mv.getNewTarget();
            Variable<Object[]> arguments = mv.getArguments();

            Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext", ExecutionContext.class);

            // (1) Create a new ExecutionContext
            /* steps 1-5 (not applicable) */
            /* steps 6-7 */
            prepareCall(calleeContext, fn, newTarget, mv);
            /* steps 8-10 (not applicable) */

            // (2) Call OrdinaryCallEvaluateBody
            /* steps 11-12 */
            ordinaryCallEvaluateBody(function.instantiation, function.body, calleeContext, fn, arguments, mv);

            // (3) Return result value
            /* steps 13-15 */
            returnResultOrThis(callerContext, calleeContext, function.tailCall, mv);
        });
    }

    /**
     * 9.2.1.1 PrepareForOrdinaryCall( F, newTarget )<br>
     * 9.2.1.2 OrdinaryCallBindThis ( F, calleeContext, thisArgument )
     * 
     * <pre>
     * funEnv = newFunctionEnvironment(function, newTarget, thisValue)
     * calleeContext = newFunctionExecutionContext(function, funEnv)
     * </pre>
     * 
     * @param node
     *            the function node
     * @param calleeContext
     *            the variable which holds the callee context
     * @param function
     *            the variable which holds the function object
     * @param thisArgument
     *            the variable which holds the thisArgument
     * @param mv
     *            the instruction visitor
     */
    private static void prepareCallAndBindThis(FunctionNode node, Variable<ExecutionContext> calleeContext,
            Variable<? extends FunctionObject> function, Variable<? extends Object> thisArgument,
            InstructionVisitor mv) {
        mv.load(function);
        {
            // Create new function environment.
            mv.load(function);
            mv.anull();
            ordinaryCallBindThis(node, function, thisArgument, mv);
            mv.invoke(Methods.LexicalEnvironment_newFunctionEnvironment);
        }
        mv.invoke(Methods.ExecutionContext_newFunctionExecutionContext);
        mv.store(calleeContext);
    }

    /**
     * 9.2.1.1 PrepareForOrdinaryCall( F, newTarget )<br>
     * 9.2.1.2 OrdinaryCallBindThis ( F, calleeContext, thisArgument )
     * 
     * <pre>
     * funEnv = newFunctionEnvironment(function, newTarget, thisValue)
     * calleeContext = newFunctionExecutionContext(function, funEnv)
     * </pre>
     * 
     * @param node
     *            the function node
     * @param calleeContext
     *            the variable which holds the callee context
     * @param function
     *            the variable which holds the function object
     * @param newTarget
     *            the variable which holds the newTarget
     * @param thisArgument
     *            the variable which holds the thisArgument
     * @param mv
     *            the instruction visitor
     */
    private static void prepareCallAndBindThis(FunctionNode node, Variable<ExecutionContext> calleeContext,
            Variable<? extends FunctionObject> function, Variable<Constructor> newTarget,
            Variable<ScriptObject> thisArgument, InstructionVisitor mv) {
        mv.load(function);
        {
            // Create new function environment.
            mv.load(function);
            mv.load(newTarget);
            mv.load(thisArgument);
            mv.invoke(Methods.LexicalEnvironment_newFunctionEnvironment);
        }
        mv.invoke(Methods.ExecutionContext_newFunctionExecutionContext);
        mv.store(calleeContext);
    }

    /**
     * 9.2.1.1 PrepareForOrdinaryCall( F, newTarget )
     * 
     * <pre>
     * funEnv = newFunctionEnvironment(function, newTarget)
     * calleeContext = newFunctionExecutionContext(function, funEnv)
     * </pre>
     * 
     * @param calleeContext
     *            the variable which holds the callee context
     * @param function
     *            the variable which holds the function object
     * @param newTarget
     *            the variable which holds the newTarget constructor
     * @param mv
     *            the instruction visitor
     */
    private static void prepareCall(Variable<ExecutionContext> calleeContext,
            Variable<? extends FunctionObject> function, Variable<Constructor> newTarget, InstructionVisitor mv) {
        mv.load(function);
        {
            // Create new function environment.
            mv.load(function);
            mv.load(newTarget);
            mv.invoke(Methods.LexicalEnvironment_newFunctionEnvironment_ConstructDerived);
        }
        mv.invoke(Methods.ExecutionContext_newFunctionExecutionContext);
        mv.store(calleeContext);
    }

    /**
     * 9.2.1.2 OrdinaryCallBindThis ( F, calleeContext, thisArgument )
     * 
     * @param node
     *            the function node
     * @param function
     *            the variable which holds the function object
     * @param thisArgument
     *            the variable which holds the thisArgument
     * @param mv
     *            the instruction visitor
     */
    private static void ordinaryCallBindThis(FunctionNode node, Variable<? extends FunctionObject> function,
            Variable<? extends Object> thisArgument, InstructionVisitor mv) {
        /* step 1 */
        FunctionNode.ThisMode thisMode = node.getThisMode();
        /* step 2 */
        if (thisMode == ThisMode.Lexical) {
            mv.anull();
            return;
        }
        /* steps 3-4 (not applicable) */
        /* steps 5-6 */
        if (thisMode == ThisMode.Strict) {
            /* step 5 */
            mv.load(thisArgument);
        } else {
            /* step 6 */
            mv.load(function);
            mv.load(thisArgument);
            mv.lineInfo(node);
            mv.invoke(Methods.FunctionOperations_functionThisValue);
        }
        /* steps 7-9 (not applicable) */
    }

    /**
     * 9.2.1.3 OrdinaryCallEvaluateBody ( F, argumentsList )
     * 
     * @param functionInit
     *            the function declaration instantiation method
     * @param functionBody
     *            the function body method
     * @param calleeContext
     *            the variable which holds the callee context
     * @param function
     *            the variable which holds the function object
     * @param arguments
     *            the variable which holds the function arguments
     * @param mv
     *            the instruction visitor
     */
    private static void ordinaryCallEvaluateBody(MethodName functionInit, MethodName functionBody,
            Variable<ExecutionContext> calleeContext, Variable<? extends FunctionObject> function,
            Variable<Object[]> arguments, InstructionVisitor mv) {
        /* steps 1-2 (Perform FunctionDeclarationInstantiation) */
        functionDeclarationInstantiation(functionInit, calleeContext, function, arguments, mv);

        /* step 3 (Perform EvaluateBody) */
        evaluateBody(functionBody, calleeContext, mv);
    }

    /**
     * <code>
     * function_init(calleeContext, function, arguments)
     * </code>
     * 
     * @param functionInit
     *            the function declaration instantiation method
     * @param calleeContext
     *            the variable which holds the callee context
     * @param function
     *            the variable which holds the function object
     * @param arguments
     *            the variable which holds the function arguments
     * @param mv
     *            the instruction visitor
     */
    private static void functionDeclarationInstantiation(MethodName functionInit,
            Variable<ExecutionContext> calleeContext, Variable<? extends FunctionObject> function,
            Variable<Object[]> arguments, InstructionVisitor mv) {
        mv.load(calleeContext);
        mv.load(function);
        mv.load(arguments);
        mv.invoke(functionInit);
    }

    /**
     * <code>
     * function_code(calleeContext)
     * </code>
     * 
     * @param functionBody
     *            the function body method
     * @param calleeContext
     *            the variable which holds the callee context
     * @param mv
     *            the instruction visitor
     */
    private static void evaluateBody(MethodName functionBody, Variable<ExecutionContext> calleeContext,
            InstructionVisitor mv) {
        mv.load(calleeContext);
        mv.invoke(functionBody);
    }

    /**
     * <code>
     * function.getLegacyCaller()
     * </code>
     * 
     * @param function
     *            the variable which holds the function object
     * @param mv
     *            the instruction visitor
     */
    private static void getLegacyCaller(Variable<LegacyConstructorFunction> function, InstructionVisitor mv) {
        mv.load(function);
        mv.invoke(Methods.LegacyConstructorFunction_getLegacyCaller);
    }

    /**
     * <code>
     * function.setLegacyCaller(callerContext.getCurrentFunction())
     * </code>
     * 
     * @param function
     *            the variable which holds the function object
     * @param callerContext
     *            the variable which holds the caller context
     * @param mv
     *            the instruction visitor
     */
    private static void setLegacyCaller(Variable<LegacyConstructorFunction> function,
            Variable<ExecutionContext> callerContext, InstructionVisitor mv) {
        mv.load(function);
        mv.load(callerContext);
        mv.invoke(Methods.ExecutionContext_getCurrentFunction);
        mv.invoke(Methods.LegacyConstructorFunction_setLegacyCaller);
    }

    /**
     * <code>
     * function.getLegacyArguments()
     * </code>
     * 
     * @param function
     *            the variable which holds the function object
     * @param mv
     *            the instruction visitor
     */
    private static void getLegacyArguments(Variable<LegacyConstructorFunction> function, InstructionVisitor mv) {
        mv.load(function);
        mv.invoke(Methods.LegacyConstructorFunction_getLegacyArguments);
    }

    /**
     * <code>
     * function.setLegacyCaller(callerContext.getCurrentFunction())
     * </code>
     * 
     * @param function
     *            the variable which holds the function object
     * @param arguments
     *            the variable which holds the function arguments
     * @param mv
     *            the instruction visitor
     */
    private static void setLegacyArguments(Variable<LegacyConstructorFunction> function, Variable<Object[]> arguments,
            InstructionVisitor mv) {
        mv.load(function);
        mv.anew(Methods.LegacyConstructorFunction$Arguments_new, arguments);
        mv.invoke(Methods.LegacyConstructorFunction_setLegacyArguments);
    }

    /**
     * <code>
     * function.setLegacyCaller(oldCaller)
     * function.setLegacyArguments(oldArguments)
     * </code>
     * 
     * @param function
     *            the variable which holds the function object
     * @param oldCaller
     *            the variable which holds the old caller
     * @param oldArguments
     *            the variable which holds the old arguments
     * @param mv
     *            the instruction visitor
     */
    private static void restoreLegacyProperties(Variable<LegacyConstructorFunction> function,
            Variable<FunctionObject> oldCaller, Variable<LegacyConstructorFunction.Arguments> oldArguments,
            InstructionVisitor mv) {
        mv.load(function);
        mv.load(oldCaller);
        mv.invoke(Methods.LegacyConstructorFunction_setLegacyCaller);

        mv.load(function);
        mv.load(oldArguments);
        mv.invoke(Methods.LegacyConstructorFunction_setLegacyArguments);
    }

    /**
     * <code>
     * thisArgument = OrdinaryCreateFromConstructor(callerContext, newTarget, %ObjectPrototype%)
     * </code>
     * 
     * @param callerContext
     *            the variable which holds the caller context
     * @param newTarget
     *            the variable which holds the newTarget constructor
     * @param thisArgument
     *            the variable which holds the thisArgument
     * @param mv
     *            the instruction visitor
     */
    static void ordinaryCreateFromConstructor(Variable<ExecutionContext> callerContext, Variable<Constructor> newTarget,
            Variable<ScriptObject> thisArgument, InstructionVisitor mv) {
        mv.load(callerContext);
        mv.load(newTarget);
        mv.get(Fields.Intrinsics_ObjectPrototype);
        mv.invoke(Methods.OrdinaryObject_OrdinaryCreateFromConstructor);
        mv.store(thisArgument);
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * if (tailCall &amp;&amp; result instanceof TailCallInvocation) {
     *     return ((TailCallInvocation) result).toConstructTailCall(thisArgument);
     * }
     * if (Type.isObject(result)) {
     *     return Type.objectValue(result);
     * }
     * return thisArgument;
     * </pre>
     * 
     * @param thisArgument
     *            the variable which holds the thisArgument
     * @param tailCall
     *            {@code true} if the constructor function contains a tail-call
     * @param mv
     *            the instruction visitor
     */
    private static void returnResultOrThis(Variable<ScriptObject> thisArgument, boolean tailCall,
            InstructionVisitor mv) {
        if (tailCall) {
            Jump noTailCall = new Jump();
            mv.dup();
            mv.instanceOf(Types.TailCallInvocation);
            mv.ifeq(noTailCall);
            {
                mv.checkcast(Types.TailCallInvocation);
                mv.load(thisArgument);
                mv.invoke(Methods.TailCallInvocation_toConstructTailCall);
                mv._return();
            }
            mv.mark(noTailCall);
        }

        Jump noResult = new Jump();
        mv.dup();
        mv.instanceOf(Types.ScriptObject);
        mv.ifeq(noResult);
        {
            mv.checkcast(Types.ScriptObject);
            mv._return();
        }
        mv.mark(noResult);
        mv.pop();

        mv.load(thisArgument);
        mv._return();
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * if (tailCall &amp;&amp; result instanceof TailCallInvocation) {
     *     return ((TailCallInvocation) result).toConstructTailCall(envRec);
     * }
     * if (Type.isObject(result)) {
     *     return Type.objectValue(result);
     * }
     * if (!Type.isUndefined(result)) {
     *     throw Errors.newTypeError();
     * }
     * return envRec.getThisBinding(callerContext);
     * </pre>
     * 
     * @param callerContext
     *            the variable which holds the caller context
     * @param calleeContext
     *            the variable which holds the callee context
     * @param tailCall
     *            {@code true} if the constructor function contains a tail-call
     * @param mv
     *            the instruction visitor
     */
    private static void returnResultOrThis(Variable<ExecutionContext> callerContext,
            Variable<ExecutionContext> calleeContext, boolean tailCall, InstructionVisitor mv) {
        if (tailCall) {
            Jump noTailCall = new Jump();
            mv.dup();
            mv.instanceOf(Types.TailCallInvocation);
            mv.ifeq(noTailCall);
            {
                mv.checkcast(Types.TailCallInvocation);
                mv.load(calleeContext);
                mv.invoke(Methods.ExecutionContext_getFunctionVariableEnvironmentRecord);
                mv.invoke(Methods.TailCallInvocation_toConstructTailCallWithEnvironment);
                mv._return();
            }
            mv.mark(noTailCall);
        }

        Jump notObject = new Jump();
        mv.dup();
        mv.instanceOf(Types.ScriptObject);
        mv.ifeq(notObject);
        {
            mv.checkcast(Types.ScriptObject);
            mv._return();
        }
        mv.mark(notObject);

        Jump notUndefined = new Jump();
        mv.dup();
        mv.loadUndefined();
        mv.ifacmpeq(notUndefined);
        {
            mv.load(callerContext);
            mv.get(Fields.MessagesKey_NotObjectTypeFromConstructor);
            mv.invoke(Methods.Errors_newTypeError);
            mv.athrow();
        }
        mv.mark(notUndefined);
        mv.pop();

        mv.load(calleeContext);
        mv.invoke(Methods.ExecutionContext_getFunctionVariableEnvironmentRecord);
        mv.load(callerContext);
        mv.invoke(Methods.FunctionEnvironmentRecord_getThisBinding);
        // If the this-binding is present it's a ScriptObject; if it's not present calling
        // getThisBinding() will result in a ReferenceError being thrown. So emitting a
        // checkcast instruction is safe here.
        mv.checkcast(Types.ScriptObject);
        mv._return();
    }
}
