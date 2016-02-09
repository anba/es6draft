/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;

import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.FunctionExpression;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.FunctionNode.ThisMode;
import com.github.anba.es6draft.ast.MethodDefinition;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
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
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;

/**
 * Generates bytecode for the function entry method
 */
final class FunctionCodeGenerator {
    private static final class Fields {
        static final FieldName Intrinsics_ObjectPrototype = FieldName.findStatic(Types.Intrinsics,
                "ObjectPrototype", Types.Intrinsics);

        static final FieldName MessagesKey_InvalidCallClass = FieldName.findStatic(
                Types.Messages$Key, "InvalidCallClass", Types.Messages$Key);

        static final FieldName MessagesKey_NotObjectTypeFromConstructor = FieldName.findStatic(
                Types.Messages$Key, "NotObjectTypeFromConstructor", Types.Messages$Key);
    }

    private static final class Methods {
        // ExecutionContext
        static final MethodName ExecutionContext_newFunctionExecutionContext = MethodName
                .findStatic(Types.ExecutionContext, "newFunctionExecutionContext", Type.methodType(
                        Types.ExecutionContext, Types.FunctionObject, Types.LexicalEnvironment));

        static final MethodName ExecutionContext_getCurrentFunction = MethodName
                .findVirtual(Types.ExecutionContext, "getCurrentFunction",
                        Type.methodType(Types.FunctionObject));

        static final MethodName ExecutionContext_getFunctionVariableEnvironmentRecord = MethodName
                .findVirtual(Types.ExecutionContext, "getFunctionVariableEnvironmentRecord",
                        Type.methodType(Types.FunctionEnvironmentRecord));

        // FunctionEnvironmentRecord
        static final MethodName FunctionEnvironmentRecord_getThisBinding = MethodName.findVirtual(
                Types.FunctionEnvironmentRecord, "getThisBinding",
                Type.methodType(Types.Object, Types.ExecutionContext));

        // LegacyFunction
        static final MethodName LegacyFunction_getLegacyArguments = MethodName.findVirtual(
                Types.LegacyConstructorFunction, "getLegacyArguments", Type.methodType(Types.Object));

        static final MethodName LegacyFunction_getLegacyCaller = MethodName.findVirtual(
                Types.LegacyConstructorFunction, "getLegacyCaller", Type.methodType(Types.Object));

        static final MethodName LegacyFunction_setLegacyCaller = MethodName.findVirtual(
                Types.LegacyConstructorFunction, "setLegacyCaller",
                Type.methodType(Type.VOID_TYPE, Types.FunctionObject));

        static final MethodName LegacyFunction_restoreLegacyProperties = MethodName.findVirtual(
                Types.LegacyConstructorFunction, "restoreLegacyProperties",
                Type.methodType(Type.VOID_TYPE, Types.Object, Types.Object));

        // LexicalEnvironment
        static final MethodName LexicalEnvironment_newFunctionEnvironment = MethodName.findStatic(
                Types.LexicalEnvironment, "newFunctionEnvironment", Type.methodType(
                        Types.LexicalEnvironment, Types.FunctionObject, Types.Constructor,
                        Types.Object));

        static final MethodName LexicalEnvironment_newFunctionEnvironment_ConstructDerived = MethodName
                .findStatic(Types.LexicalEnvironment, "newFunctionEnvironment", Type.methodType(
                        Types.LexicalEnvironment, Types.FunctionObject, Types.Constructor));

        // OrdinaryAsyncFunction
        static final MethodName OrdinaryAsyncFunction_EvaluateBody = MethodName.findStatic(
                Types.OrdinaryAsyncFunction, "EvaluateBody", Type.methodType(Types.PromiseObject,
                        Types.ExecutionContext, Types.OrdinaryAsyncFunction));

        // OrdinaryGenerator
        static final MethodName OrdinaryGenerator_EvaluateBody = MethodName.findStatic(
                Types.OrdinaryGenerator, "EvaluateBody", Type.methodType(Types.GeneratorObject,
                        Types.ExecutionContext, Types.OrdinaryGenerator));

        // OrdinaryObject
        static final MethodName OrdinaryObject_OrdinaryCreateFromConstructor = MethodName
                .findStatic(Types.OrdinaryObject, "OrdinaryCreateFromConstructor", Type.methodType(
                        Types.OrdinaryObject, Types.ExecutionContext, Types.Constructor,
                        Types.Intrinsics));

        // class: Errors
        static final MethodName Errors_newTypeError = MethodName.findStatic(Types.Errors,
                "newTypeError",
                Type.methodType(Types.ScriptException, Types.ExecutionContext, Types.Messages$Key));

        // class: TailCallInvocation
        static final MethodName TailCallInvocation_toConstructTailCall = MethodName.findVirtual(
                Types.TailCallInvocation, "toConstructTailCall",
                Type.methodType(Types.TailCallInvocation, Types.ScriptObject));

        static final MethodName TailCallInvocation_toConstructTailCallWithEnvironment = MethodName
                .findVirtual(Types.TailCallInvocation, "toConstructTailCall",
                        Type.methodType(Types.TailCallInvocation, Types.FunctionEnvironmentRecord));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_functionThisValue = MethodName.findStatic(
                Types.ScriptRuntime, "functionThisValue",
                Type.methodType(Types.ScriptObject, Types.FunctionObject, Types.Object));

        // class: PromiseAbstractOperations
        static final MethodName PromiseAbstractOperations_PromiseOf = MethodName.findStatic(
                Types.PromiseAbstractOperations, "PromiseOf",
                Type.methodType(Types.PromiseObject, Types.ExecutionContext, Types.ScriptException));
    }

    private static final int FUNCTION = 0;
    private static final int GENERATOR = 0;
    private static final int EXECUTION_CONTEXT = 1;
    private static final int THIS_VALUE = 2;
    private static final int NEW_TARGET = 2;
    private static final int ARGUMENTS = 3;

    private static class CallMethodGenerator extends InstructionVisitor {
        private final String name;
        private final Type type;

        CallMethodGenerator(MethodCode method, String name, Type type) {
            super(method);
            this.name = name;
            this.type = type;
        }

        @Override
        public final void begin() {
            super.begin();
            setParameterName(name, FUNCTION, type);
            setParameterName("callerContext", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("thisValue", THIS_VALUE, Types.Object);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    private static class ConstructMethodGenerator extends InstructionVisitor {
        private final String name;
        private final Type type;

        ConstructMethodGenerator(MethodCode method, String name, Type type) {
            super(method);
            this.name = name;
            this.type = type;
        }

        @Override
        public final void begin() {
            super.begin();
            setParameterName(name, FUNCTION, type);
            setParameterName("callerContext", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("newTarget", NEW_TARGET, Types.Constructor);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    private final CodeGenerator codegen;

    FunctionCodeGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    void generate(FunctionNode node, boolean tailCall) {
        generateCall(node);
        if (node.isConstructor()) {
            generateConstruct(node, tailCall);
        }
    }

    private void generateCall(FunctionNode node) {
        MethodCode method = codegen.newMethod(node, FunctionName.Call);
        InstructionVisitor mv = new CallMethodGenerator(method, targetName(node), targetType(node));
        mv.lineInfo(node);
        mv.begin();

        if (node.isAsync()) {
            generateAsyncFunctionCall(node, mv);
        } else if (node.isGenerator()) {
            generateGeneratorCall(node, mv);
        } else if (isClassConstructor(node)) {
            generateClassConstructorCall(mv);
        } else if (isLegacy(node)) {
            generateLegacyFunctionCall(node, mv);
        } else if (node.isConstructor()) {
            generateFunctionCall(node, OrdinaryConstructorFunction.class, mv);
        } else {
            generateFunctionCall(node, OrdinaryFunction.class, mv);
        }

        mv.end();
    }

    private void generateConstruct(FunctionNode node, boolean tailCall) {
        MethodCode method = codegen.newMethod(node, tailCall ? FunctionName.ConstructTailCall : FunctionName.Construct);
        InstructionVisitor mv = new ConstructMethodGenerator(method, targetName(node), targetType(node));
        mv.lineInfo(node);
        mv.begin();

        if (node.isGenerator()) {
            generateGeneratorConstruct(node, mv);
        } else if (isDerivedClassConstructor(node)) {
            generateDerivedClassConstructorConstruct(node, tailCall, mv);
        } else if (isLegacy(node)) {
            generateLegacyFunctionConstruct(node, mv);
        } else {
            generateFunctionConstruct(node, tailCall, mv);
        }

        mv.end();
    }

    private Type targetType(FunctionNode node) {
        if (node.isGenerator()) {
            return Types.OrdinaryGenerator;
        } else if (node.isAsync()) {
            return Types.OrdinaryAsyncFunction;
        } else if (isLegacy(node)) {
            return Types.LegacyConstructorFunction;
        } else if (node.isConstructor()) {
            return Types.OrdinaryConstructorFunction;
        } else {
            return Types.OrdinaryFunction;
        }
    }

    private String targetName(FunctionNode node) {
        if (node.isGenerator()) {
            return "generator";
        } else {
            return "function";
        }
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
     * @param node
     *            the function node
     * @param mv
     *            the instruction visitor
     */
    private void generateLegacyFunctionCall(FunctionNode node, InstructionVisitor mv) {
        final boolean hasArguments = codegen.isEnabled(CompatibilityOption.FunctionArguments);
        final boolean hasCaller = codegen.isEnabled(CompatibilityOption.FunctionCaller);

        Variable<LegacyConstructorFunction> function = mv.getParameter(FUNCTION, LegacyConstructorFunction.class);
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);
        Variable<Object> oldCaller = mv.newVariable("oldCaller", Object.class);
        Variable<Object> oldArguments = mv.newVariable("oldArguments", Object.class);
        Variable<Throwable> throwable = mv.newVariable("throwable", Throwable.class);

        // (1) Retrieve 'caller' and 'arguments' and store in local variables
        if (hasCaller) {
            mv.load(function);
            mv.invoke(Methods.LegacyFunction_getLegacyCaller);
        } else {
            mv.anull();
        }
        mv.store(oldCaller);

        if (hasArguments) {
            mv.load(function);
            mv.invoke(Methods.LegacyFunction_getLegacyArguments);
        } else {
            mv.anull();
        }
        mv.store(oldArguments);

        // (2) Update 'caller' property
        if (hasCaller) {
            setLegacyCaller(function, callerContext, mv);
        }

        TryCatchLabel startFinally = new TryCatchLabel(), endFinally = new TryCatchLabel();
        TryCatchLabel handlerFinally = new TryCatchLabel();
        mv.mark(startFinally);
        {
            // (3) Create a new ExecutionContext
            prepareCallAndBindThis(node, calleeContext, function, thisValue, mv);

            // (4) Call OrdinaryCallEvaluateBody
            ordinaryCallEvaluateBody(node, calleeContext, function, arguments, mv);

            // (5) Restore 'caller' and 'arguments'
            restoreLegacyProperties(function, oldCaller, oldArguments, mv);

            // (6) Return result value
            mv._return();
        }
        mv.mark(endFinally);

        // Exception: Restore 'caller' and 'arguments' and then rethrow exception
        mv.finallyHandler(handlerFinally);
        mv.store(throwable);
        restoreLegacyProperties(function, oldCaller, oldArguments, mv);
        mv.load(throwable);
        mv.athrow();

        mv.tryFinally(startFinally, endFinally, handlerFinally);
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
     * @param node
     *            the function node
     * @param functionClass
     *            the target function class
     * @param mv
     *            the instruction visitor
     */
    private void generateFunctionCall(FunctionNode node, Class<? extends FunctionObject> functionClass,
            InstructionVisitor mv) {
        Variable<? extends FunctionObject> function = mv.getParameter(FUNCTION, functionClass);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext", ExecutionContext.class);

        // (1) Create a new ExecutionContext
        /* steps 1-6 */
        prepareCallAndBindThis(node, calleeContext, function, thisValue, mv);

        // (2) Call OrdinaryCallEvaluateBody
        /* steps 7-8 */
        ordinaryCallEvaluateBody(node, calleeContext, function, arguments, mv);

        // (3) Return result value
        /* steps 9-11 */
        mv._return();
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * throw Errors.newTypeError()
     * </pre>
     * 
     * @param mv
     *            the instruction visitor
     */
    private void generateClassConstructorCall(InstructionVisitor mv) {
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);

        // 9.2.2 [[Call]] ( thisArgument, argumentsList) - step 2
        mv.load(callerContext);
        mv.get(Fields.MessagesKey_InvalidCallClass);
        mv.invoke(Methods.Errors_newTypeError);
        mv.athrow();
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
     * @param node
     *            the function node
     * @param mv
     *            the instruction visitor
     */
    private void generateLegacyFunctionConstruct(FunctionNode node, InstructionVisitor mv) {
        final boolean hasArguments = codegen.isEnabled(CompatibilityOption.FunctionArguments);
        final boolean hasCaller = codegen.isEnabled(CompatibilityOption.FunctionCaller);

        Variable<LegacyConstructorFunction> function = mv.getParameter(FUNCTION, LegacyConstructorFunction.class);
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Constructor> newTarget = mv.getParameter(NEW_TARGET, Constructor.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ScriptObject> thisArg = mv.newVariable("thisArgument", ScriptObject.class);
        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);
        Variable<Object> oldCaller = mv.newVariable("oldCaller", Object.class);
        Variable<Object> oldArguments = mv.newVariable("oldArguments", Object.class);
        Variable<Throwable> throwable = mv.newVariable("throwable", Throwable.class);

        // (1) Retrieve 'caller' and 'arguments' and store in local variables
        if (hasCaller) {
            mv.load(function);
            mv.invoke(Methods.LegacyFunction_getLegacyCaller);
        } else {
            mv.anull();
        }
        mv.store(oldCaller);

        if (hasArguments) {
            mv.load(function);
            mv.invoke(Methods.LegacyFunction_getLegacyArguments);
        } else {
            mv.anull();
        }
        mv.store(oldArguments);

        // (2) Update 'caller' property
        if (hasCaller) {
            setLegacyCaller(function, callerContext, mv);
        }

        TryCatchLabel startFinally = new TryCatchLabel(), endFinally = new TryCatchLabel();
        TryCatchLabel handlerFinally = new TryCatchLabel();
        mv.mark(startFinally);
        {
            // (3) Create this-argument
            ordinaryCreateFromConstructor(callerContext, newTarget, thisArg, mv);

            // (4) Create a new ExecutionContext
            prepareCallAndBindThis(node, calleeContext, function, newTarget, thisArg, mv);

            // (5) Call OrdinaryCallEvaluateBody
            ordinaryCallEvaluateBody(node, calleeContext, function, arguments, mv);

            // (6) Restore 'caller' and 'arguments'
            restoreLegacyProperties(function, oldCaller, oldArguments, mv);

            // (7) Return result value
            returnResultOrThis(thisArg, false, mv);
        }
        mv.mark(endFinally);

        // Exception: Restore 'caller' and 'arguments' and then rethrow exception
        mv.finallyHandler(handlerFinally);
        mv.store(throwable);
        restoreLegacyProperties(function, oldCaller, oldArguments, mv);
        mv.load(throwable);
        mv.athrow();

        mv.tryFinally(startFinally, endFinally, handlerFinally);
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
     * @param node
     *            the function node
     * @param tailCall
     *            {@code true} if the constructor function contains a tail-call
     * @param mv
     *            the instruction visitor
     */
    private void generateFunctionConstruct(FunctionNode node, boolean tailCall,
            InstructionVisitor mv) {
        Variable<OrdinaryConstructorFunction> function = mv.getParameter(FUNCTION,
                OrdinaryConstructorFunction.class);
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Constructor> newTarget = mv.getParameter(NEW_TARGET, Constructor.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ScriptObject> thisArgument = mv.newVariable("thisArgument", ScriptObject.class);
        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        /* steps 1-5 */
        ordinaryCreateFromConstructor(callerContext, newTarget, thisArgument, mv);

        // (1) Create a new ExecutionContext
        /* steps 6-10 */
        prepareCallAndBindThis(node, calleeContext, function, newTarget, thisArgument, mv);

        // (2) Call OrdinaryCallEvaluateBody
        /* steps 11-12 */
        ordinaryCallEvaluateBody(node, calleeContext, function, arguments, mv);

        // (3) Return result value
        /* steps 13-15 */
        returnResultOrThis(thisArgument, tailCall, mv);
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
     * @param node
     *            the function node
     * @param tailCall
     *            {@code true} if the constructor function contains a tail-call
     * @param mv
     *            the instruction visitor
     */
    private void generateDerivedClassConstructorConstruct(FunctionNode node, boolean tailCall,
            InstructionVisitor mv) {
        Variable<OrdinaryConstructorFunction> function = mv.getParameter(FUNCTION,
                OrdinaryConstructorFunction.class);
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Constructor> newTarget = mv.getParameter(NEW_TARGET, Constructor.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // (1) Create a new ExecutionContext
        /* steps 1-5 (not applicable) */
        /* steps 6-7 */
        prepareCall(calleeContext, function, newTarget, mv);
        /* steps 8-10 (not applicable) */

        // (2) Call OrdinaryCallEvaluateBody
        /* steps 11-12 */
        ordinaryCallEvaluateBody(node, calleeContext, function, arguments, mv);

        // (3) Return result value
        /* steps 13-15 */
        returnResultOrThis(callerContext, calleeContext, tailCall, mv);
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
     * @param node
     *            the function node
     * @param mv
     *            the instruction visitor
     */
    private void generateAsyncFunctionCall(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryAsyncFunction> function = mv.getParameter(FUNCTION,
                OrdinaryAsyncFunction.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // (1) Create a new ExecutionContext
        prepareCallAndBindThis(node, calleeContext, function, thisValue, mv);

        // (2) Perform FunctionDeclarationInstantiation
        {
            TryCatchLabel startCatch = new TryCatchLabel();
            TryCatchLabel endCatch = new TryCatchLabel(), handlerCatch = new TryCatchLabel();
            Jump noException = new Jump();

            mv.mark(startCatch);
            functionDeclarationInstantiation(node, calleeContext, function, arguments, mv);
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
        mv.load(function);
        mv.invoke(Methods.OrdinaryAsyncFunction_EvaluateBody);

        // (4) Return result value
        mv._return();
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
     * @param node
     *            the function node
     * @param mv
     *            the instruction visitor
     */
    private void generateGeneratorCall(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryGenerator> generator = mv.getParameter(GENERATOR, OrdinaryGenerator.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // (1) Create a new ExecutionContext
        prepareCallAndBindThis(node, calleeContext, generator, thisValue, mv);

        // (2) Perform OrdinaryCallEvaluateBody - FunctionDeclarationInstantiation
        functionDeclarationInstantiation(node, calleeContext, generator, arguments, mv);

        // (3) Perform OrdinaryCallEvaluateBody - EvaluateBody
        mv.load(calleeContext);
        mv.load(generator);
        mv.invoke(Methods.OrdinaryGenerator_EvaluateBody);

        // (4) Return result value
        mv._return();
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * calleeContext = newFunctionExecutionContext(generator, newTarget)
     * function_init(calleeContext, generator, arguments)
     * generatorObject = EvaluateBody(calleeContext, generator)
     * BindThisValue(calleeContext, generatorObject)
     * return generatorObject
     * </pre>
     * 
     * @param node
     *            the function node
     * @param mv
     *            the instruction visitor
     */
    private void generateGeneratorConstruct(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryGenerator> generator = mv.getParameter(GENERATOR, OrdinaryGenerator.class);
        Variable<Constructor> newTarget = mv.getParameter(NEW_TARGET, Constructor.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // 9.2.4 FunctionAllocate - Generator functions are always derived constructor kinds.

        // (1) Create a new ExecutionContext
        prepareCall(calleeContext, generator, newTarget, mv);

        // (2) Perform OrdinaryCallEvaluateBody - FunctionDeclarationInstantiation
        functionDeclarationInstantiation(node, calleeContext, generator, arguments, mv);

        // (3) Perform OrdinaryCallEvaluateBody - EvaluateBody
        mv.load(calleeContext);
        mv.load(generator);
        mv.invoke(Methods.OrdinaryGenerator_EvaluateBody);

        // (4) Return result value
        mv._return();
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
    private void prepareCallAndBindThis(FunctionNode node, Variable<ExecutionContext> calleeContext,
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
    private void prepareCallAndBindThis(FunctionNode node,
            Variable<ExecutionContext> calleeContext, Variable<? extends FunctionObject> function,
            Variable<Constructor> newTarget, Variable<ScriptObject> thisArgument,
            InstructionVisitor mv) {
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
    private void prepareCall(Variable<ExecutionContext> calleeContext,
            Variable<? extends FunctionObject> function, Variable<Constructor> newTarget,
            InstructionVisitor mv) {
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
    private void ordinaryCallBindThis(FunctionNode node,
            Variable<? extends FunctionObject> function, Variable<? extends Object> thisArgument,
            InstructionVisitor mv) {
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
            mv.invoke(Methods.ScriptRuntime_functionThisValue);
        }
        /* steps 7-9 (not applicable) */
    }

    /**
     * 9.2.1.3 OrdinaryCallEvaluateBody ( F, argumentsList )
     * 
     * @param node
     *            the function node
     * @param calleeContext
     *            the variable which holds the callee context
     * @param function
     *            the variable which holds the function object
     * @param arguments
     *            the variable which holds the function arguments
     * @param mv
     *            the instruction visitor
     */
    private void ordinaryCallEvaluateBody(FunctionNode node,
            Variable<ExecutionContext> calleeContext,
            Variable<? extends FunctionObject> function, Variable<Object[]> arguments,
            InstructionVisitor mv) {
        /* steps 1-2 (Perform FunctionDeclarationInstantiation) */
        functionDeclarationInstantiation(node, calleeContext, function, arguments, mv);

        /* step 3 (Perform EvaluateBody) */
        evaluateBody(node, calleeContext, mv);
    }

    /**
     * <code>
     * function_init(calleeContext, function, arguments)
     * </code>
     * 
     * @param node
     *            the function node
     * @param calleeContext
     *            the variable which holds the callee context
     * @param function
     *            the variable which holds the function object
     * @param arguments
     *            the variable which holds the function arguments
     * @param mv
     *            the instruction visitor
     */
    private void functionDeclarationInstantiation(FunctionNode node,
            Variable<ExecutionContext> calleeContext, Variable<? extends FunctionObject> function,
            Variable<Object[]> arguments, InstructionVisitor mv) {
        mv.load(calleeContext);
        mv.load(function);
        mv.load(arguments);
        mv.invoke(codegen.methodDesc(node, FunctionName.Init));
    }

    /**
     * <code>
     * function_code(calleeContext)
     * </code>
     * 
     * @param node
     *            the function node
     * @param calleeContext
     *            the variable which holds the callee context
     * @param mv
     *            the instruction visitor
     */
    private void evaluateBody(FunctionNode node, Variable<ExecutionContext> calleeContext,
            InstructionVisitor mv) {
        mv.load(calleeContext);
        mv.invoke(codegen.methodDesc(node, FunctionName.Code));
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
    private void setLegacyCaller(Variable<? extends FunctionObject> function,
            Variable<ExecutionContext> callerContext, InstructionVisitor mv) {
        mv.load(function);
        mv.load(callerContext);
        mv.invoke(Methods.ExecutionContext_getCurrentFunction);
        mv.invoke(Methods.LegacyFunction_setLegacyCaller);
    }

    /**
     * <code>
     * function.restoreLegacyProperties(oldCaller, oldArguments)
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
    private void restoreLegacyProperties(Variable<? extends FunctionObject> function,
            Variable<Object> oldCaller, Variable<Object> oldArguments, InstructionVisitor mv) {
        mv.load(function);
        mv.load(oldCaller);
        mv.load(oldArguments);
        mv.invoke(Methods.LegacyFunction_restoreLegacyProperties);
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
    private void ordinaryCreateFromConstructor(Variable<ExecutionContext> callerContext,
            Variable<Constructor> newTarget, Variable<ScriptObject> thisArgument,
            InstructionVisitor mv) {
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
    private void returnResultOrThis(Variable<ScriptObject> thisArgument, boolean tailCall,
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
    private void returnResultOrThis(Variable<ExecutionContext> callerContext,
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

    private boolean isLegacy(FunctionNode node) {
        if (IsStrict(node)) {
            return false;
        }
        if (!(node instanceof FunctionDeclaration || node instanceof FunctionExpression)) {
            return false;
        }
        return codegen.isEnabled(CompatibilityOption.FunctionArguments)
                || codegen.isEnabled(CompatibilityOption.FunctionCaller);
    }

    private boolean isClassConstructor(FunctionNode node) {
        if (node instanceof MethodDefinition) {
            return ((MethodDefinition) node).isClassConstructor();
        }
        return false;
    }

    private boolean isDerivedClassConstructor(FunctionNode node) {
        if (node instanceof MethodDefinition) {
            return ((MethodDefinition) node).getType() == MethodDefinition.MethodType.DerivedConstructor;
        }
        return false;
    }
}
