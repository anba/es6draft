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
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;

/**
 * Generates bytecode for the function entry method
 */
final class FunctionCodeGenerator {
    private static final class Fields {
        static final FieldName Undefined_UNDEFINED = FieldName.findStatic(Types.Undefined,
                "UNDEFINED", Types.Undefined);

        static final FieldName Intrinsics_ObjectPrototype = FieldName.findStatic(Types.Intrinsics,
                "ObjectPrototype", Types.Intrinsics);

        static final FieldName ConstructorKind_Derived = FieldName.findStatic(
                Types.FunctionObject$ConstructorKind, "Derived",
                Types.FunctionObject$ConstructorKind);

        static final FieldName MessagesKey_NotObjectTypeFromConstructor = FieldName.findStatic(
                Types.Messages$Key, "NotObjectTypeFromConstructor", Types.Messages$Key);
    }

    private static final class Methods {
        // ExecutionContext
        static final MethodName ExecutionContext_newFunctionExecutionContext = MethodName
                .findStatic(Types.ExecutionContext, "newFunctionExecutionContext", Type.methodType(
                        Types.ExecutionContext, Types.ExecutionContext, Types.FunctionObject,
                        Types.Constructor, Types.Object));

        static final MethodName ExecutionContext_newFunctionExecutionContextConstructDerived = MethodName
                .findStatic(Types.ExecutionContext, "newFunctionExecutionContext", Type.methodType(
                        Types.ExecutionContext, Types.ExecutionContext, Types.FunctionObject,
                        Types.Constructor));

        static final MethodName ExecutionContext_getCurrentFunction = MethodName
                .findVirtual(Types.ExecutionContext, "getCurrentFunction",
                        Type.methodType(Types.FunctionObject));

        static final MethodName ExecutionContext_resolveThisBinding = MethodName.findVirtual(
                Types.ExecutionContext, "resolveThisBinding", Type.methodType(Types.Object));

        // FunctionObject
        static final MethodName FunctionObject_getConstructorKind = MethodName.findVirtual(
                Types.FunctionObject, "getConstructorKind",
                Type.methodType(Types.FunctionObject$ConstructorKind));

        static final MethodName FunctionObject_getLegacyArguments = MethodName.findVirtual(
                Types.FunctionObject, "getLegacyArguments", Type.methodType(Types.Object));

        static final MethodName FunctionObject_getLegacyCaller = MethodName.findVirtual(
                Types.FunctionObject, "getLegacyCaller", Type.methodType(Types.Object));

        static final MethodName FunctionObject_setLegacyCaller = MethodName.findVirtual(
                Types.FunctionObject, "setLegacyCaller",
                Type.methodType(Type.VOID_TYPE, Types.FunctionObject));

        static final MethodName FunctionObject_restoreLegacyProperties = MethodName.findVirtual(
                Types.FunctionObject, "restoreLegacyProperties",
                Type.methodType(Type.VOID_TYPE, Types.Object, Types.Object));

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

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_BindThisValue = MethodName.findStatic(
                Types.ScriptRuntime, "BindThisValue",
                Type.methodType(Type.VOID_TYPE, Types.ScriptObject, Types.ExecutionContext));
    }

    private static final int FUNCTION = 0;
    private static final int GENERATOR = 0;
    private static final int EXECUTION_CONTEXT = 1;
    private static final int THIS_VALUE = 2;
    private static final int NEW_TARGET = 2;
    private static final int ARGUMENTS = 3;

    private static final class AsyncFunctionCallMethodGenerator extends InstructionVisitor {
        AsyncFunctionCallMethodGenerator(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("function", FUNCTION, Types.OrdinaryAsyncFunction);
            setParameterName("callerContext", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("thisValue", THIS_VALUE, Types.Object);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    private static final class AsyncFunctionConstructMethodGenerator extends InstructionVisitor {
        AsyncFunctionConstructMethodGenerator(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("function", FUNCTION, Types.OrdinaryAsyncFunction);
            setParameterName("callerContext", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("newTarget", NEW_TARGET, Types.Constructor);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    private static final class FunctionCallMethodGenerator extends InstructionVisitor {
        FunctionCallMethodGenerator(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("function", FUNCTION, Types.OrdinaryFunction);
            setParameterName("callerContext", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("thisValue", THIS_VALUE, Types.Object);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    private static final class FunctionConstructMethodGenerator extends InstructionVisitor {
        FunctionConstructMethodGenerator(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("function", FUNCTION, Types.OrdinaryConstructorFunction);
            setParameterName("callerContext", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("newTarget", NEW_TARGET, Types.Constructor);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    private static final class GeneratorCallMethodGenerator extends InstructionVisitor {
        GeneratorCallMethodGenerator(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("generator", GENERATOR, Types.OrdinaryGenerator);
            setParameterName("callerContext", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("thisValue", THIS_VALUE, Types.Object);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    private static final class GeneratorConstructMethodGenerator extends InstructionVisitor {
        GeneratorConstructMethodGenerator(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("generator", GENERATOR, Types.OrdinaryGenerator);
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
        if (node.isGenerator()) {
            InstructionVisitor mv = new GeneratorCallMethodGenerator(method);
            mv.lineInfo(node.getBeginLine());
            mv.begin();

            generateGeneratorCall(node, mv);

            mv.end();
        } else if (node.isAsync()) {
            InstructionVisitor mv = new AsyncFunctionCallMethodGenerator(method);
            mv.lineInfo(node.getBeginLine());
            mv.begin();

            generateAsyncFunctionCall(node, mv);

            mv.end();
        } else {
            InstructionVisitor mv = new FunctionCallMethodGenerator(method);
            mv.lineInfo(node.getBeginLine());
            mv.begin();

            if (isLegacy(node)) {
                generateLegacyFunctionCall(node, mv);
            } else {
                generateFunctionCall(node, mv);
            }

            mv.end();
        }
    }

    private void generateConstruct(FunctionNode node, boolean tailCall) {
        MethodCode method = codegen.newMethod(node, tailCall ? FunctionName.ConstructTailCall
                : FunctionName.Construct);
        if (node.isGenerator()) {
            assert !tailCall;
            InstructionVisitor mv = new GeneratorConstructMethodGenerator(method);
            mv.lineInfo(node.getBeginLine());
            mv.begin();

            generateGeneratorConstruct(node, mv);

            mv.end();
        } else if (node.isAsync()) {
            assert !tailCall;
            InstructionVisitor mv = new AsyncFunctionConstructMethodGenerator(method);
            mv.lineInfo(node.getBeginLine());
            mv.begin();

            generateAsyncFunctionConstruct(node, mv);

            mv.end();
        } else {
            InstructionVisitor mv = new FunctionConstructMethodGenerator(method);
            mv.lineInfo(node.getBeginLine());
            mv.begin();

            if (isLegacy(node)) {
                assert !tailCall;
                generateLegacyFunctionConstruct(node, mv);
            } else if (isConstructorMethod(node)) {
                generateClassConstructorConstruct(node, tailCall, mv);
            } else {
                generateFunctionConstruct(node, tailCall, mv);
            }

            mv.end();
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
     *   calleeContext = newCallFunctionExecutionContext(callerContext, function, thisValue)
     *   function_init(calleeContext, function, arguments)
     *   return function_code(calleeContext)
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
        Variable<OrdinaryFunction> function = mv.getParameter(FUNCTION, OrdinaryFunction.class);
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
        mv.load(function);
        mv.invoke(Methods.FunctionObject_getLegacyCaller);
        mv.store(oldCaller);

        mv.load(function);
        mv.invoke(Methods.FunctionObject_getLegacyArguments);
        mv.store(oldArguments);

        // (2) Update 'caller' property
        setLegacyCaller(function, callerContext, mv);

        TryCatchLabel startFinally = new TryCatchLabel(), endFinally = new TryCatchLabel();
        TryCatchLabel handlerFinally = new TryCatchLabel();
        mv.mark(startFinally);
        {
            // (3) Create a new ExecutionContext
            prepareCallAndBindThis(calleeContext, callerContext, function, null, thisValue, mv);

            // (4) Call OrdinaryCallEvaluateBody
            ordinaryCallEvaluateBody(node, calleeContext, function, arguments, mv);

            // (5) Restore 'caller' and 'arguments'
            restoreLegacyProperties(function, oldCaller, oldArguments, mv);

            // (6) Return result value
            returnResultOrUndefined(mv);
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
     * calleeContext = newFunctionExecutionContext(callerContext, function, thisValue)
     * function_init(calleeContext, function, arguments)
     * return function_code(calleeContext)
     * </pre>
     * 
     * @param node
     *            the function node
     * @param mv
     *            the instruction visitor
     */
    private void generateFunctionCall(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryFunction> function = mv.getParameter(FUNCTION, OrdinaryFunction.class);
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // (1) Create a new ExecutionContext
        /* steps 1-6 */
        prepareCallAndBindThis(calleeContext, callerContext, function, null, thisValue, mv);

        // (2) Call OrdinaryCallEvaluateBody
        /* steps 7-8 */
        ordinaryCallEvaluateBody(node, calleeContext, function, arguments, mv);

        // (3) Return result value
        /* steps 9-10 */
        returnResultOrUndefined(mv);
    }

    private void generateLegacyFunctionConstruct(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryConstructorFunction> function = mv.getParameter(FUNCTION,
                OrdinaryConstructorFunction.class);
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
        mv.load(function);
        mv.invoke(Methods.FunctionObject_getLegacyCaller);
        mv.store(oldCaller);

        mv.load(function);
        mv.invoke(Methods.FunctionObject_getLegacyArguments);
        mv.store(oldArguments);

        // (2) Update 'caller' property
        setLegacyCaller(function, callerContext, mv);

        TryCatchLabel startFinally = new TryCatchLabel(), endFinally = new TryCatchLabel();
        TryCatchLabel handlerFinally = new TryCatchLabel();
        mv.mark(startFinally);
        {
            // (3) Create this-argument
            ordinaryCreateFromConstructor(callerContext, newTarget, thisArg, mv);

            // (4) Create a new ExecutionContext
            prepareCallAndBindThis(calleeContext, callerContext, function, newTarget, thisArg, mv);

            // (5) Call OrdinaryCallEvaluateBody
            ordinaryCallEvaluateBody(node, calleeContext, function, arguments, mv);

            // (6) Restore 'caller' and 'arguments'
            restoreLegacyProperties(function, oldCaller, oldArguments, mv);

            // (7) Return result value
            returnConstructResultOrThis(thisArg, false, mv);
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
        /* steps 6-11 */
        prepareCallAndBindThis(calleeContext, callerContext, function, newTarget, thisArgument, mv);

        // (2) Call OrdinaryCallEvaluateBody
        /* steps 12-13 */
        ordinaryCallEvaluateBody(node, calleeContext, function, arguments, mv);

        // (3) Return result value
        /* steps 14-16 */
        returnConstructResultOrThis(thisArgument, tailCall, mv);
    }

    private void generateClassConstructorConstruct(FunctionNode node, boolean tailCall,
            InstructionVisitor mv) {
        Variable<OrdinaryConstructorFunction> function = mv.getParameter(FUNCTION,
                OrdinaryConstructorFunction.class);
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Constructor> newTarget = mv.getParameter(NEW_TARGET, Constructor.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ScriptObject> thisArg = mv.newVariable("thisArgument", ScriptObject.class);
        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        Jump isDerived = new Jump(), callBody = new Jump();
        mv.load(function);
        mv.invoke(Methods.FunctionObject_getConstructorKind);
        mv.get(Fields.ConstructorKind_Derived);
        mv.ifacmpeq(isDerived);
        {
            // Base constructor
            ordinaryCreateFromConstructor(callerContext, newTarget, thisArg, mv);
            prepareCallAndBindThis(calleeContext, callerContext, function, newTarget, thisArg, mv);
            mv.goTo(callBody);
        }
        mv.mark(isDerived);
        {
            // Derived constructor
            prepareCall(calleeContext, callerContext, function, newTarget, thisArg, mv);
        }
        mv.mark(callBody);

        // (2) Call OrdinaryCallEvaluateBody
        /* steps 12-13 */
        ordinaryCallEvaluateBody(node, calleeContext, function, arguments, mv);

        // (3) Return result value
        /* steps 14-16 */
        returnConstructResultOrThis(callerContext, calleeContext, function, thisArg, tailCall, mv);
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * calleeContext = newFunctionExecutionContext(callerContext, function, thisValue)
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
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // (1) Create a new ExecutionContext
        prepareCallAndBindThis(calleeContext, callerContext, function, null, thisValue, mv);

        // (2) Perform FunctionDeclarationInstantiation
        functionDeclarationInstantiation(node, calleeContext, function, arguments, mv);

        // (3) Perform EvaluateBody
        mv.load(calleeContext);
        mv.load(function);
        mv.invoke(Methods.OrdinaryAsyncFunction_EvaluateBody);

        // (4) Return result value
        mv._return();
    }

    private void generateAsyncFunctionConstruct(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryAsyncFunction> function = mv.getParameter(FUNCTION,
                OrdinaryAsyncFunction.class);
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Constructor> newTarget = mv.getParameter(NEW_TARGET, Constructor.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // 9.2.4 FunctionAllocate - Async functions are always derived constructor kinds

        // (1) Create a new ExecutionContext
        prepareCall(calleeContext, callerContext, function, newTarget, mv);

        // (2) Perform OrdinaryCallEvaluateBody - FunctionDeclarationInstantiation
        functionDeclarationInstantiation(node, calleeContext, function, arguments, mv);

        // (3) Perform OrdinaryCallEvaluateBody - EvaluateBody
        mv.load(calleeContext);
        mv.load(function);
        mv.invoke(Methods.OrdinaryAsyncFunction_EvaluateBody);

        // FIXME: spec bug - missing bind this
        mv.dup();
        mv.load(calleeContext);
        mv.invoke(Methods.ScriptRuntime_BindThisValue);

        // (4) Return result value
        mv._return();
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * calleeContext = newFunctionExecutionContext(callerContext, generator, thisValue)
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
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // (1) Create a new ExecutionContext
        prepareCallAndBindThis(calleeContext, callerContext, generator, null, thisValue, mv);

        // (2) Perform OrdinaryCallEvaluateBody - FunctionDeclarationInstantiation
        functionDeclarationInstantiation(node, calleeContext, generator, arguments, mv);

        // (3) Perform OrdinaryCallEvaluateBody - EvaluateBody
        mv.load(calleeContext);
        mv.load(generator);
        mv.invoke(Methods.OrdinaryGenerator_EvaluateBody);

        // (4) Return result value
        mv._return();
    }

    private void generateGeneratorConstruct(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryGenerator> generator = mv.getParameter(GENERATOR, OrdinaryGenerator.class);
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Constructor> newTarget = mv.getParameter(NEW_TARGET, Constructor.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // 9.2.4 FunctionAllocate - Generator functions are always derived constructor kinds

        // (1) Create a new ExecutionContext
        prepareCall(calleeContext, callerContext, generator, newTarget, mv);

        // (2) Perform OrdinaryCallEvaluateBody - FunctionDeclarationInstantiation
        functionDeclarationInstantiation(node, calleeContext, generator, arguments, mv);

        // (3) Perform OrdinaryCallEvaluateBody - EvaluateBody
        mv.load(calleeContext);
        mv.load(generator);
        mv.invoke(Methods.OrdinaryGenerator_EvaluateBody);

        // FIXME: spec bug - missing bind this
        mv.dup();
        mv.load(calleeContext);
        mv.invoke(Methods.ScriptRuntime_BindThisValue);

        // (4) Return result value
        mv._return();
    }

    /**
     * 9.2.2.1 PrepareOrdinaryCall( F, newTarget )<br>
     * 9.2.2.2 OrdinaryCallBindThis ( F, calleeContext, thisArgument )
     * 
     * <code>
     * calleeContext = newCallFunctionExecutionContext(callerContext, function, newTarget, thisValue)
     * </code>
     * 
     * @param calleeContext
     *            the variable which holds the callee context
     * @param callerContext
     *            the variable which holds the caller context
     * @param function
     *            the variable which holds the function object
     * @param newTarget
     *            the variable which holds the newTarget or {@code null}
     * @param thisArgument
     *            the variable which holds the thisArgument
     * @param mv
     *            the instruction visitor
     */
    private void prepareCallAndBindThis(Variable<ExecutionContext> calleeContext,
            Variable<ExecutionContext> callerContext, Variable<? extends FunctionObject> function,
            Variable<Constructor> newTarget, Variable<? extends Object> thisArgument,
            InstructionVisitor mv) {
        mv.load(callerContext);
        mv.load(function);
        if (newTarget != null) {
            mv.load(newTarget);
        } else {
            mv.anull();
        }
        mv.load(thisArgument);
        mv.invoke(Methods.ExecutionContext_newFunctionExecutionContext);
        mv.store(calleeContext);
    }

    /**
     * 9.2.2.1 PrepareOrdinaryCall( F, newTarget )<br>
     * 9.2.2.2 OrdinaryCallBindThis ( F, calleeContext, thisArgument )
     * 
     * @param calleeContext
     *            the variable which holds the callee context
     * @param callerContext
     *            the variable which holds the caller context
     * @param function
     *            the variable which holds the function object
     * @param thisArgument
     *            the variable which holds the thisArgument
     * @param mv
     *            the instruction visitor
     */
    private void prepareCall(Variable<ExecutionContext> calleeContext,
            Variable<ExecutionContext> callerContext, Variable<? extends FunctionObject> function,
            Variable<Constructor> newTarget, Variable<? extends Object> thisArgument,
            InstructionVisitor mv) {
        mv.load(callerContext);
        mv.load(function);
        mv.load(newTarget);
        mv.invoke(Methods.ExecutionContext_newFunctionExecutionContextConstructDerived);
        mv.store(calleeContext);
        mv.anull();
        mv.store(thisArgument);
    }

    /**
     * 9.2.2.1 PrepareOrdinaryCall( F, newTarget )<br>
     * 9.2.2.2 OrdinaryCallBindThis ( F, calleeContext, thisArgument )
     * 
     * @param calleeContext
     *            the variable which holds the callee context
     * @param callerContext
     *            the variable which holds the caller context
     * @param function
     *            the variable which holds the function object
     * @param mv
     *            the instruction visitor
     */
    private void prepareCall(Variable<ExecutionContext> calleeContext,
            Variable<ExecutionContext> callerContext, Variable<? extends FunctionObject> function,
            Variable<Constructor> newTarget, InstructionVisitor mv) {
        mv.load(callerContext);
        mv.load(function);
        mv.load(newTarget);
        mv.invoke(Methods.ExecutionContext_newFunctionExecutionContextConstructDerived);
        mv.store(calleeContext);
    }

    /**
     * 9.2.2.3 OrdinaryCallEvaluateBody ( F, calleeContext, argumentsList )
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
            Variable<? extends OrdinaryFunction> function, Variable<Object[]> arguments,
            InstructionVisitor mv) {
        /* steps 1-3 (Perform FunctionDeclarationInstantiation) */
        functionDeclarationInstantiation(node, calleeContext, function, arguments, mv);

        /* step 4 (Perform EvaluateBody) */
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
        mv.invoke(Methods.FunctionObject_setLegacyCaller);
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
        mv.invoke(Methods.FunctionObject_restoreLegacyProperties);
    }

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
     * if (result != null) {
     *     return result;
     * }
     * return Undefined.UNDEFINED;
     * </pre>
     * 
     * @param mv
     *            the instruction visitor
     */
    private void returnResultOrUndefined(InstructionVisitor mv) {
        Jump noResult = new Jump();
        mv.dup();
        mv.ifnull(noResult);
        mv._return();
        mv.mark(noResult);
        mv.pop();
        mv.get(Fields.Undefined_UNDEFINED);
        mv._return();
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * if (result != null) {
     *     if (tailCall &amp;&amp; result instanceof TailCallInvocation) {
     *         return ((TailCallInvocation) result).toConstructTailCall(thisArgument);
     *     }
     *     if (Type.isObject(result)) {
     *         return Type.objectValue(result);
     *     }
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
    private void returnConstructResultOrThis(Variable<ScriptObject> thisArgument, boolean tailCall,
            InstructionVisitor mv) {
        Jump noResult = new Jump();
        mv.dup();
        mv.ifnull(noResult);
        {
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

            mv.dup();
            mv.instanceOf(Types.ScriptObject);
            mv.ifeq(noResult);
            {
                mv.checkcast(Types.ScriptObject);
                mv._return();
            }
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
     * if (result != null) {
     *     if (tailCall &amp;&amp; result instanceof TailCallInvocation) {
     *         return ((TailCallInvocation) result).toConstructTailCall(thisArgument);
     *     }
     *     if (Type.isObject(result)) {
     *         return Type.objectValue(result);
     *     }
     *     throw Errors.newTypeError();
     * }
     * return calleeContext.resolveThisBinding();
     * </pre>
     * 
     * @param callerContext
     *            the variable which holds the caller context
     * @param calleeContext
     *            the variable which holds the callee context
     * @param function
     *            the variable which holds the function object
     * @param thisArgument
     *            the variable which holds the thisArgument
     * @param tailCall
     *            {@code true} if the constructor function contains a tail-call
     * @param mv
     *            the instruction visitor
     */
    private void returnConstructResultOrThis(Variable<ExecutionContext> callerContext,
            Variable<ExecutionContext> calleeContext, Variable<? extends FunctionObject> function,
            Variable<ScriptObject> thisArgument, boolean tailCall, InstructionVisitor mv) {
        Jump noResult = new Jump();
        mv.dup();
        mv.ifnull(noResult);
        {
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

            Jump noObject = new Jump();
            mv.dup();
            mv.instanceOf(Types.ScriptObject);
            mv.ifeq(noObject);
            {
                mv.checkcast(Types.ScriptObject);
                mv._return();
            }
            mv.mark(noObject);
            mv.pop();

            Jump throwTypeError = new Jump();
            mv.load(thisArgument);
            mv.ifnull(throwTypeError);
            {
                mv.load(thisArgument);
                mv._return();
            }
            mv.mark(throwTypeError);

            mv.load(callerContext);
            mv.get(Fields.MessagesKey_NotObjectTypeFromConstructor);
            mv.invoke(Methods.Errors_newTypeError);
            mv.athrow();
        }
        mv.mark(noResult);
        mv.pop();

        // Calling `calleeContext.resolveThisBinding()` is equivalent to
        // `calleeContext().getFunctionVariableEnvironment().getThisBinding()`.
        mv.load(calleeContext);
        mv.invoke(Methods.ExecutionContext_resolveThisBinding);
        // The this-binding is either an ScriptObject or ReferenceError is thrown, so it's valid to
        // emit a checkcast instruction.
        mv.checkcast(Types.ScriptObject);
        mv._return();
    }

    private boolean isLegacy(FunctionNode node) {
        return !IsStrict(node)
                && (node instanceof FunctionDeclaration || node instanceof FunctionExpression)
                && codegen.isEnabled(CompatibilityOption.FunctionPrototype);
    }

    private boolean isConstructorMethod(FunctionNode node) {
        if (node instanceof MethodDefinition) {
            return ((MethodDefinition) node).getType() == MethodDefinition.MethodType.Constructor;
        }
        return false;
    }
}
