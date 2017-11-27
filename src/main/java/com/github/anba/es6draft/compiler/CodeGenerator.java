/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.GeneratorComprehensionGenerator.EvaluateGeneratorComprehension;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.compiler.CodeVisitor.GeneratorState;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.Code;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.InstructionAssembler;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.MethodTypeDescriptor;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.JVMNames;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * 
 */
final class CodeGenerator {
    private static final class Methods {
        // class: CompiledFunction
        static final MethodName CompiledFunction_Constructor = MethodName.findConstructor(Types.CompiledFunction,
                Type.methodType(Type.VOID_TYPE, Types.Source, Types.RuntimeInfo$Function));

        // class: CompiledModule
        static final MethodName CompiledModule_Constructor = MethodName.findConstructor(Types.CompiledModule,
                Type.methodType(Type.VOID_TYPE, Types.Source, Types.RuntimeInfo$ModuleBody));

        // class: CompiledScript
        static final MethodName CompiledScript_Constructor = MethodName.findConstructor(Types.CompiledScript,
                Type.methodType(Type.VOID_TYPE, Types.Source, Types.RuntimeInfo$ScriptBody));

        // class: DebugInfo
        static final MethodName DebugInfo_new = MethodName.findConstructor(Types.DebugInfo,
                Type.methodType(Type.VOID_TYPE));

        static final MethodName DebugInfo_addMethod = MethodName.findVirtual(Types.DebugInfo, "addMethod",
                Type.methodType(Type.VOID_TYPE, Types.MethodHandle));
    }

    private static final class MethodDescriptors {
        static final MethodTypeDescriptor DefaultConstructor = Type.methodType(Type.VOID_TYPE, Types.Source);

        static final MethodTypeDescriptor Script_Code = Type.methodType(Types.Object, Types.ExecutionContext,
                Types.Script);
        static final MethodTypeDescriptor Script_Body = Type.methodType(Types.Object, Types.ExecutionContext);
        static final MethodTypeDescriptor Script_Init = Type.methodType(Type.VOID_TYPE, Types.ExecutionContext);
        static final MethodTypeDescriptor Script_RTI = Type.methodType(Types.RuntimeInfo$ScriptBody);

        static final MethodTypeDescriptor Module_Code = Type.methodType(Types.Object, Types.ExecutionContext);
        static final MethodTypeDescriptor Module_Init = Type.methodType(Type.VOID_TYPE, Types.ExecutionContext,
                Types.SourceTextModuleRecord, Types.LexicalEnvironment);
        static final MethodTypeDescriptor Module_RTI = Type.methodType(Types.RuntimeInfo$ScriptBody);

        static final MethodTypeDescriptor Function_RTI = Type.methodType(Types.RuntimeInfo$Function);

        static final MethodTypeDescriptor DebugInfo = Type.methodType(Types.DebugInfo);
    }

    private static final boolean INCLUDE_SOURCE = true;
    private static final boolean OFFTHREAD_SOURCE = false;
    private static final CompletableFuture<String> NO_SOURCE = CompletableFuture.completedFuture(null);

    private final RuntimeContext context;
    private final Code code;
    private final Program program;

    private final StatementGenerator stmtgen = new StatementGenerator(this);
    private final ExpressionGenerator exprgen = new ExpressionGenerator(this);
    private final PropertyGenerator propgen = new PropertyGenerator(this);
    private final BlockDeclarationInstantiationGenerator blockgen = new BlockDeclarationInstantiationGenerator(this);

    CodeGenerator(RuntimeContext context, Code code, Program program) {
        this.context = context;
        this.code = code;
        this.program = program;
    }

    Program getProgram() {
        return program;
    }

    public ExecutorService getExecutor() {
        return context.getExecutor();
    }

    boolean isEnabled(CompatibilityOption option) {
        return context.isEnabled(option);
    }

    boolean isEnabled(Parser.Option option) {
        return program.getParserOptions().contains(option);
    }

    boolean isEnabled(Compiler.Option option) {
        return context.isEnabled(option);
    }

    /* ----------------------------------------------------------------------------------------- */

    ValType expression(Expression node, CodeVisitor mv) {
        return node.accept(exprgen, mv);
    }

    ValType expressionBoxed(Expression node, CodeVisitor mv) {
        ValType type = node.accept(exprgen, mv);
        if (type.isJavaPrimitive()) {
            mv.toBoxed(type);
            return ValType.Any;
        }
        return type;
    }

    PropertyGenerator propertyGenerator() {
        return propgen;
    }

    void propertyDefinition(PropertyDefinition node, CodeVisitor mv) {
        node.accept(propgen, mv);
    }

    void blockInit(BlockStatement node, CodeVisitor mv) {
        blockgen.generate(node, mv);
    }

    void blockInit(SwitchStatement node, CodeVisitor mv) {
        blockgen.generate(node, mv);
    }

    Completion statement(ModuleItem node, CodeVisitor mv) {
        return node.accept(stmtgen, mv);
    }

    Completion statements(List<? extends ModuleItem> statements, CodeVisitor mv) {
        // 13.1.13 Runtime Semantics: Evaluation<br>
        // StatementList : StatementList StatementListItem
        /* steps 1-6 */
        Completion result = Completion.Normal;
        for (ModuleItem item : statements) {
            if ((result = statement(item, mv)).isAbrupt()) {
                break;
            }
        }
        return result;
    }

    /* ----------------------------------------------------------------------------------------- */

    // Template strings of this compilation unit.
    private final HashMap<TemplateLiteral, Integer> templateKeys = new HashMap<>();

    int templateKey(TemplateLiteral template) {
        Integer key = templateKeys.get(template);
        if (key == null) {
            templateKeys.put(template, key = templateKeys.size());
        }
        return key;
    }

    MethodCode newMethod(String methodName, MethodTypeDescriptor methodDescriptor) {
        final int access = Modifier.PUBLIC | Modifier.STATIC;
        MethodCode method = code.newMethod(access, methodName, methodDescriptor);
        // System.out.printf("add <%s, %s>%n", methodName, method.classCode.className);
        return method;
    }

    // Counter to ensure unique method names.
    private int methodCounter = 0;

    private String newUniqueName(String name, char separator) {
        return JVMNames.toBytecodeName(name + separator + ++methodCounter);
    }

    /**
     * Annotates {@code name} as an internal stack frame which should be elided from error stack traces.
     * 
     * @param name
     *            the method name
     * @param suffix
     *            the optional suffix
     * @return the new method name
     */
    private String hiddenFrame(String name, String suffix) {
        return JVMNames.addPrefixSuffix(name, '!', suffix);
    }

    /**
     * Annotates {@code name} as a script frame.
     * 
     * @param name
     *            the method name
     * @return the new method name
     */
    String scriptFrame(String name) {
        if (isEnabled(Parser.Option.NativeFunction)) {
            return JVMNames.addPrefixSuffix(name, '!', "");
        }
        return name;
    }

    /**
     * Annotates {@code name} as a script frame.
     * 
     * @param name
     *            the method name
     * @param suffix
     *            the optional suffix
     * @return the new method name
     */
    String scriptFrame(String name, String suffix) {
        if (isEnabled(Parser.Option.NativeFunction)) {
            return JVMNames.addPrefixSuffix(name, '!', suffix);
        }
        return name + suffix;
    }

    /**
     * Returns a new method.
     * 
     * @param name
     *            the base method name
     * @param descriptor
     *            the method descriptor
     * @return the compiled method name
     */
    MethodCode method(String name, MethodTypeDescriptor descriptor) {
        String methodName = newUniqueName(name, '~');
        return newMethod(methodName, descriptor);
    }

    /**
     * Returns a new method.
     * 
     * @param mv
     *            the code visitor
     * @param name
     *            the base method name
     * @param descriptor
     *            the method descriptor
     * @return the new method
     */
    MethodCode method(CodeVisitor mv, String name, MethodTypeDescriptor descriptor) {
        String methodName = newUniqueName(mv.getRoot().getMethod().methodName + '|' + name, '\'');
        return newMethod(methodName, descriptor);
    }

    /* ----------------------------------------------------------------------------------------- */

    /**
     * Compiles a script node.
     * 
     * @param node
     *            the script node
     */
    void compile(Script node) {
        // instantiation method
        MethodCode scriptInit = newMethod(scriptFrame("~script_init"), MethodDescriptors.Script_Init);
        if (!(node.isEvalScript() || node.isScripting())) {
            new GlobalDeclarationInstantiationGenerator(this).generate(node, scriptInit);
        } else {
            new EvalDeclarationInstantiationGenerator(this).generate(node, scriptInit);
        }

        // runtime method
        MethodCode scriptBody = newMethod(scriptFrame("~script_body"), MethodDescriptors.Script_Body);
        scriptBody(node, scriptBody);

        // entry method (hidden frame)
        MethodCode scriptCode = newMethod("!script", MethodDescriptors.Script_Code);
        ScriptCodeGenerator.scriptEvaluation(node, scriptCode, scriptInit.name(), scriptBody.name());

        // debug-info method
        Function<MethodName, MethodName> debugInfo = null;
        if (isEnabled(Compiler.Option.DebugInfo)) {
            debugInfo = rt -> {
                MethodCode method = newMethod("!script_dbg", MethodDescriptors.DebugInfo);
                debugInfo(method, rt, scriptCode.name(), scriptInit.name(), scriptBody.name());
                return method.name();
            };
        }

        // runtime-info method
        MethodCode runtimeInfo = newMethod("!script_rti", MethodDescriptors.Script_RTI);
        RuntimeInfoGenerator.runtimeInfo(node, runtimeInfo, scriptCode.name(), debugInfo);

        defaultConstructor(Methods.CompiledScript_Constructor, runtimeInfo.name());
    }

    private void scriptBody(Script node, MethodCode method) {
        ScriptCodeVisitor body = new ScriptCodeVisitor(method, node);
        body.lineInfo(node);
        body.begin();
        body.loadUndefined();
        body.storeCompletionValue(ValType.Undefined);

        body.enterScope(node);
        Completion result = statements(node.getStatements(), body);
        body.exitScope();

        if (!result.isAbrupt()) {
            body.loadCompletionValue();
            body._return();
        }
        body.end();
    }

    /**
     * Compiles a module node.
     * 
     * @param node
     *            the module node
     * @param moduleRecord
     *            the module record
     */
    void compile(Module node, SourceTextModuleRecord moduleRecord) {
        // instantiation methods
        MethodCode moduleInit = newMethod(scriptFrame("~module_init"), MethodDescriptors.Module_Init);
        new ModuleDeclarationInstantiationGenerator(this).generate(node, moduleRecord, moduleInit);

        // runtime method
        MethodCode moduleBody = newMethod(scriptFrame("~module"), MethodDescriptors.Module_Code);
        moduleBody(node, moduleBody);

        // debug-info method
        Function<MethodName, MethodName> debugInfo = null;
        if (isEnabled(Compiler.Option.DebugInfo)) {
            debugInfo = rt -> {
                MethodCode method = newMethod("!module_dbg", MethodDescriptors.DebugInfo);
                debugInfo(method, rt, moduleInit.name(), moduleBody.name());
                return method.name();
            };
        }

        // runtime-info method
        MethodCode runtimeInfo = newMethod("!module_rti", MethodDescriptors.Module_RTI);
        RuntimeInfoGenerator.runtimeInfo(node, runtimeInfo, moduleInit.name(), moduleBody.name(), debugInfo);

        defaultConstructor(Methods.CompiledModule_Constructor, runtimeInfo.name());
    }

    private void moduleBody(Module node, MethodCode method) {
        ModuleCodeVisitor body = new ModuleCodeVisitor(method, node);
        body.lineInfo(node);
        body.begin();

        body.enterScope(node);
        Completion result = statements(node.getStatements(), body);
        body.exitScope();

        if (!result.isAbrupt()) {
            // Completion values are currently ignored for module code.
            body.loadUndefined();
            body._return();
        }
        body.end();
    }

    /**
     * Compiles a stand-alone function definition.
     * 
     * @param function
     *            the function node
     */
    void compileFunction(FunctionDefinition function) {
        MethodName method = functionDefinition(function);

        defaultConstructor(Methods.CompiledFunction_Constructor, method);
    }

    /**
     * Compiles a stand-alone generator definition.
     * 
     * @param function
     *            the function node
     */
    void compileFunction(GeneratorDefinition function) {
        MethodName method = generatorDefinition(function);

        defaultConstructor(Methods.CompiledFunction_Constructor, method);
    }

    /**
     * Compiles a stand-alone async function definition.
     * 
     * @param function
     *            the function node
     */
    void compileFunction(AsyncFunctionDefinition function) {
        MethodName method = asyncFunctionDefinition(function);

        defaultConstructor(Methods.CompiledFunction_Constructor, method);
    }

    /**
     * Compiles a stand-alone async generator definition.
     * 
     * @param function
     *            the function node
     */
    void compileFunction(AsyncGeneratorDefinition function) {
        MethodName method = asyncGeneratorDefinition(function);

        defaultConstructor(Methods.CompiledFunction_Constructor, method);
    }

    private void defaultConstructor(MethodName superConstructor, MethodName runtimeInfo) {
        MethodCode constructor = code.newConstructor(Modifier.PUBLIC, MethodDescriptors.DefaultConstructor);
        InstructionVisitor mv = new InstructionVisitor(constructor);
        mv.begin();
        mv.loadThis();
        mv.loadParameter(0, Types.Source);
        mv.invoke(runtimeInfo);
        mv.invoke(superConstructor);
        mv._return();
        mv.end();
    }

    /* ----------------------------------------------------------------------------------------- */

    static final class FunctionCode {
        final MethodName entry;
        final MethodName instantiation;
        final MethodName body;
        final boolean tailCall;

        FunctionCode(MethodName entry, MethodName instantiation, MethodName body, boolean tailCall) {
            this.entry = entry;
            this.instantiation = instantiation;
            this.body = body;
            this.tailCall = tailCall;
        }
    }

    private static final class FunctionDesc {
        final MethodTypeDescriptor instantiation;
        final MethodTypeDescriptor body;
        final MethodTypeDescriptor call;
        final MethodTypeDescriptor construct;

        private enum Kind {
            Function, Generator
        }

        FunctionDesc(Kind kind, Type functionType, Type callReturnType) {
            this.instantiation = instantiation(functionType);
            this.body = body(kind);
            this.call = call(callReturnType, functionType);
            this.construct = null;
        }

        FunctionDesc(Kind kind, Type functionType, Type callReturnType, Type constructReturnType) {
            this.instantiation = instantiation(functionType);
            this.body = body(kind);
            this.call = call(callReturnType, functionType);
            this.construct = construct(constructReturnType, functionType);
        }

        private static MethodTypeDescriptor instantiation(Type functionType) {
            return Type.methodType(Type.VOID_TYPE, Types.ExecutionContext, functionType, Types.Object_);
        }

        private static MethodTypeDescriptor body(Kind kind) {
            if (kind == Kind.Function) {
                return Type.methodType(Types.Object, Types.ExecutionContext);
            }
            return Type.methodType(Types.Object, Types.ExecutionContext, Types.ResumptionPoint);
        }

        private static MethodTypeDescriptor call(Type returnType, Type functionType) {
            return Type.methodType(returnType, functionType, Types.ExecutionContext, Types.Object, Types.Object_);
        }

        private static MethodTypeDescriptor construct(Type returnType, Type functionType) {
            return Type.methodType(returnType, functionType, Types.ExecutionContext, Types.Constructor, Types.Object_);
        }

        static final FunctionDesc ConstructorFunction = new FunctionDesc(Kind.Function,
                Types.OrdinaryConstructorFunction, Types.Object, Types.ScriptObject);

        static final FunctionDesc ConstructorFunctionTailCall = new FunctionDesc(Kind.Function,
                Types.OrdinaryConstructorFunction, Types.Object, Types.Object);

        static final FunctionDesc LegacyFunction = new FunctionDesc(Kind.Function, Types.LegacyConstructorFunction,
                Types.Object, Types.ScriptObject);

        static final FunctionDesc Function = new FunctionDesc(Kind.Function, Types.OrdinaryFunction, Types.Object);

        static final FunctionDesc Generator = new FunctionDesc(Kind.Generator, Types.OrdinaryGenerator,
                Types.GeneratorObject);

        static final FunctionDesc AsyncFunction = new FunctionDesc(Kind.Generator, Types.OrdinaryAsyncFunction,
                Types.PromiseObject);

        static final FunctionDesc AsyncGenerator = new FunctionDesc(Kind.Generator, Types.OrdinaryAsyncGenerator,
                Types.AsyncGeneratorObject);
    }

    @FunctionalInterface
    private interface FunctionEntryCompiler<F> {
        void compile(CodeGenerator codegen, F node, MethodCode method, FunctionCode function);
    }

    @FunctionalInterface
    private interface FunctionBodyCompiler<F> {
        boolean compile(CodeGenerator codegen, F node, MethodCode method);
    }

    private static final class ClassCompiler {
        final FunctionDesc desc;
        final FunctionBodyCompiler<MethodDefinition> body;
        final FunctionEntryCompiler<ClassDefinition> call;
        final FunctionEntryCompiler<ClassDefinition> construct;

        ClassCompiler(FunctionDesc desc, FunctionBodyCompiler<MethodDefinition> body,
                FunctionEntryCompiler<ClassDefinition> call, FunctionEntryCompiler<ClassDefinition> construct) {
            this.desc = desc;
            this.body = body;
            this.call = call;
            this.construct = construct;
        }

        static final ClassCompiler BaseClass = new ClassCompiler(FunctionDesc.ConstructorFunction,
                CodeGenerator::functionBody, FunctionCodeGenerator::classConstructorCall,
                FunctionCodeGenerator::baseClassConstruct);

        static final ClassCompiler DerivedClass = new ClassCompiler(FunctionDesc.ConstructorFunction,
                CodeGenerator::functionBody, FunctionCodeGenerator::classConstructorCall,
                FunctionCodeGenerator::derivedClassConstruct);
    }

    private static final class FunctionCompiler<F> {
        final FunctionDesc desc;
        final FunctionBodyCompiler<F> body;
        final FunctionEntryCompiler<F> call;
        final FunctionEntryCompiler<F> construct;
        final FunctionBodyCompiler<F> callInline;
        final FunctionBodyCompiler<F> constructInline;

        FunctionCompiler(FunctionDesc desc, FunctionBodyCompiler<F> body, FunctionEntryCompiler<F> call,
                FunctionEntryCompiler<F> construct, FunctionBodyCompiler<F> callInline,
                FunctionBodyCompiler<F> constructInline) {
            this.desc = desc;
            this.body = body;
            this.call = call;
            this.construct = construct;
            this.callInline = callInline;
            this.constructInline = constructInline;
        }

        static final FunctionCompiler<MethodDefinition> CallConstructor = new FunctionCompiler<>(
                FunctionDesc.ConstructorFunction, CodeGenerator::functionBody,
                FunctionCodeGenerator::constructorFunctionCall, null, null, null);

        static final FunctionCompiler<FunctionDefinition> LegacyFunction = new FunctionCompiler<>(
                FunctionDesc.LegacyFunction, CodeGenerator::functionBody, FunctionCodeGenerator::legacyFunctionCall,
                FunctionCodeGenerator::legacyFunctionConstruct, CodeGenerator::functionCallInline,
                CodeGenerator::functionConstructInline);

        static final FunctionCompiler<FunctionDefinition> ConstructorFunction = new FunctionCompiler<>(
                FunctionDesc.ConstructorFunction, CodeGenerator::functionBody,
                FunctionCodeGenerator::constructorFunctionCall, FunctionCodeGenerator::functionConstruct,
                CodeGenerator::functionCallInline, CodeGenerator::functionConstructInline);

        static final FunctionCompiler<FunctionNode> Function = new FunctionCompiler<>(FunctionDesc.Function,
                CodeGenerator::functionBody, FunctionCodeGenerator::functionCall, null,
                CodeGenerator::functionCallInline, null);

        static final FunctionCompiler<ArrowFunction> Arrow = new FunctionCompiler<>(FunctionDesc.Function,
                CodeGenerator::arrowConciseBody, FunctionCodeGenerator::functionCall, null,
                CodeGenerator::arrowConciseCallInline, null);

        static final FunctionCompiler<FunctionNode> Generator = new FunctionCompiler<>(FunctionDesc.Generator,
                CodeGenerator::generatorBody, FunctionCodeGenerator::generatorCall, null, null, null);

        static final FunctionCompiler<GeneratorComprehension> GeneratorComprehension = new FunctionCompiler<>(
                FunctionDesc.Generator, CodeGenerator::generatorComprehensionBody, FunctionCodeGenerator::generatorCall,
                null, null, null);

        static final FunctionCompiler<FunctionNode> AsyncFunction = new FunctionCompiler<>(FunctionDesc.AsyncFunction,
                CodeGenerator::generatorBody, FunctionCodeGenerator::asyncFunctionCall, null, null, null);

        static final FunctionCompiler<AsyncArrowFunction> AsyncArrow = new FunctionCompiler<>(
                FunctionDesc.AsyncFunction, CodeGenerator::asyncArrowConciseBody,
                FunctionCodeGenerator::asyncFunctionCall, null, null, null);

        static final FunctionCompiler<FunctionNode> AsyncGenerator = new FunctionCompiler<>(FunctionDesc.AsyncGenerator,
                CodeGenerator::generatorBody, FunctionCodeGenerator::asyncGeneratorCall, null, null, null);
    }

    private static final int MAX_FNAME_LENGTH = 0x400;

    String newUniqueName(FunctionNode node) {
        String methodName = node.getMethodName();
        if (methodName.isEmpty()) {
            methodName = "anonymous";
        } else if (methodName.length() > MAX_FNAME_LENGTH) {
            methodName = methodName.substring(0, MAX_FNAME_LENGTH);
        }
        return newUniqueName(methodName, '~');
    }

    String newUniqueName(ClassDefinition node) {
        String methodName = node.getClassName();
        if (methodName.isEmpty()) {
            methodName = "anonymous";
        } else if (methodName.length() > MAX_FNAME_LENGTH) {
            methodName = methodName.substring(0, MAX_FNAME_LENGTH);
        }
        return newUniqueName(methodName, '~');
    }

    /**
     * Compiles a class definition node.
     * 
     * @param node
     *            the class definition
     * @return the class definition method
     */
    MethodName classDefinition(ClassDefinition node) {
        MethodDefinition constructor = node.getConstructor();
        MethodDefinition callConstructor = node.getCallConstructor();
        String methodName = newUniqueName(node);

        CompletableFuture<String> source = getSource(node);

        ClassCompiler compiler;
        if (node.getHeritage() == null) {
            compiler = ClassCompiler.BaseClass;
        } else {
            compiler = ClassCompiler.DerivedClass;
        }

        // instantiation method
        MethodCode constructInit = newMethod(scriptFrame(methodName, "_init"), compiler.desc.instantiation);
        new FunctionDeclarationInstantiationGenerator(this).generate(constructor, constructInit);

        // runtime method
        MethodCode constructBody = newMethod(scriptFrame(methodName), compiler.desc.body);
        boolean tailConstruct = compiler.body.compile(this, constructor, constructBody);

        // construct method
        MethodTypeDescriptor constructDesc;
        if (tailConstruct) {
            constructDesc = FunctionDesc.ConstructorFunctionTailCall.construct;
        } else {
            constructDesc = compiler.desc.construct;
        }
        MethodCode constructEntry = newMethod(hiddenFrame(methodName, "_construct"), constructDesc);
        FunctionCode construct = new FunctionCode(constructEntry.name(), constructInit.name(), constructBody.name(),
                tailConstruct);
        compiler.construct.compile(this, node, constructEntry, construct);

        FunctionCode call;
        Function<MethodName, MethodName> debugInfo = null;
        if (callConstructor != null) {
            FunctionCompiler<MethodDefinition> callCompiler = FunctionCompiler.CallConstructor;
            String callMethodName = newUniqueName(callConstructor);

            // instantiation method
            MethodCode callInit = newMethod(scriptFrame(callMethodName, "_init"), callCompiler.desc.instantiation);
            new FunctionDeclarationInstantiationGenerator(this).generate(callConstructor, callInit);

            // runtime method
            MethodCode callBody = newMethod(scriptFrame(callMethodName), callCompiler.desc.body);
            boolean tailCall = callCompiler.body.compile(this, callConstructor, callBody);

            // call method
            MethodCode callMethod = newMethod(hiddenFrame(methodName, "_call"), callCompiler.desc.call);
            call = new FunctionCode(callMethod.name(), callInit.name(), callBody.name(), tailCall);
            callCompiler.call.compile(this, callConstructor, callMethod, call);

            // debug-info method
            if (isEnabled(Compiler.Option.DebugInfo)) {
                debugInfo = rt -> {
                    MethodCode method = newMethod(hiddenFrame(methodName, "_dbg"), MethodDescriptors.DebugInfo);
                    debugInfo(method, rt, call.entry, call.instantiation, call.body, construct.entry,
                            construct.instantiation, construct.body);
                    return method.name();
                };
            }
        } else {
            // call method
            MethodCode callMethod = newMethod(hiddenFrame(methodName, "_call"), compiler.desc.call);
            call = new FunctionCode(callMethod.name(), null, null, false);
            compiler.call.compile(this, node, callMethod, call);

            // debug-info method
            if (isEnabled(Compiler.Option.DebugInfo)) {
                debugInfo = rt -> {
                    MethodCode method = newMethod(hiddenFrame(methodName, "_dbg"), MethodDescriptors.DebugInfo);
                    debugInfo(method, rt, call.entry, construct.entry, construct.instantiation, construct.body);
                    return method.name();
                };
            }
        }

        // runtime-info method
        MethodCode runtimeInfo = newMethod(hiddenFrame(methodName, "_rti"), MethodDescriptors.Function_RTI);
        new RuntimeInfoGenerator(this).runtimeInfo(constructor, runtimeInfo, call, construct, source.join(), debugInfo);

        return runtimeInfo.name();
    }

    private boolean isLegacy(FunctionDefinition node) {
        if (IsStrict(node)) {
            return false;
        }
        return isEnabled(CompatibilityOption.FunctionArguments) || isEnabled(CompatibilityOption.FunctionCaller);
    }

    /**
     * Compiles a function definition node.
     * 
     * @param node
     *            the function definition
     * @return the function definition method
     */
    MethodName functionDefinition(FunctionDefinition node) {
        if (isLegacy(node)) {
            return compile(node, FunctionCompiler.LegacyFunction);
        }
        return compile(node, FunctionCompiler.ConstructorFunction);
    }

    /**
     * Compiles the generator definition node.
     * 
     * @param node
     *            the generator definition
     * @return the generator definition method
     */
    MethodName generatorDefinition(GeneratorDefinition node) {
        return compile(node, FunctionCompiler.Generator);
    }

    /**
     * Compiles the async function definition node.
     * 
     * @param node
     *            the async function definition
     * @return the async function definition method
     */
    MethodName asyncFunctionDefinition(AsyncFunctionDefinition node) {
        return compile(node, FunctionCompiler.AsyncFunction);
    }

    /**
     * Compiles the async generator definition node.
     * 
     * @param node
     *            the async generator definition
     * @return the async generator definition method
     */
    MethodName asyncGeneratorDefinition(AsyncGeneratorDefinition node) {
        return compile(node, FunctionCompiler.AsyncGenerator);
    }

    /**
     * Compiles a generator comprehension node.
     * 
     * @param node
     *            the generator comprehension
     * @return the generator comprehension method
     */
    MethodName generatorComprehension(GeneratorComprehension node) {
        return compile(node, FunctionCompiler.GeneratorComprehension);
    }

    /**
     * Compiles the arrow function node.
     * 
     * @param node
     *            the arrow function
     * @return the arrow function method
     */
    MethodName arrowFunction(ArrowFunction node) {
        if (node.getExpression() != null) {
            return compile(node, FunctionCompiler.Arrow);
        }
        return compile(node, FunctionCompiler.Function);
    }

    /**
     * Compiles the async arrow function node.
     * 
     * @param node
     *            the async arrow function
     * @return the async arrow function method
     */
    MethodName asyncArrowFunction(AsyncArrowFunction node) {
        if (node.getExpression() != null) {
            return compile(node, FunctionCompiler.AsyncArrow);
        }
        return compile(node, FunctionCompiler.AsyncFunction);
    }

    /**
     * Compiles the method definition node.
     * 
     * @param node
     *            the method definition
     * @return the method definition method
     */
    MethodName methodDefinition(MethodDefinition node) {
        switch (node.getType()) {
        case AsyncFunction:
            return compile(node, FunctionCompiler.AsyncFunction);
        case AsyncGenerator:
            return compile(node, FunctionCompiler.AsyncGenerator);
        case Generator:
            return compile(node, FunctionCompiler.Generator);
        case Function:
        case Getter:
        case Setter:
            return compile(node, FunctionCompiler.Function);
        case CallConstructor:
        case ClassConstructor:
        default:
            throw new AssertionError();
        }
    }

    private <FUNCTION extends FunctionNode> MethodName compile(FUNCTION node, FunctionCompiler<FUNCTION> compiler) {
        CompletableFuture<String> source = getSource(node);
        String methodName = newUniqueName(node);

        FunctionCode call;
        if (node.isInline()) {
            // call method
            MethodCode callEntry = newMethod(scriptFrame(methodName), compiler.desc.call);

            boolean tailCall = compiler.callInline.compile(this, node, callEntry);
            call = new FunctionCode(callEntry.name(), null, null, tailCall);
        } else {
            // instantiation method
            MethodCode functionInit = newMethod(scriptFrame(methodName, "_init"), compiler.desc.instantiation);
            new FunctionDeclarationInstantiationGenerator(this).generate(node, functionInit);

            // runtime method
            MethodCode functionBody = newMethod(scriptFrame(methodName), compiler.desc.body);
            boolean tailCall = compiler.body.compile(this, node, functionBody);

            // call method
            MethodCode callEntry = newMethod(hiddenFrame(methodName, "_call"), compiler.desc.call);

            call = new FunctionCode(callEntry.name(), functionInit.name(), functionBody.name(), tailCall);
            compiler.call.compile(this, node, callEntry, call);
        }

        FunctionCode construct;
        Function<MethodName, MethodName> debugInfo = null;
        if (compiler.construct != null) {
            // construct method
            MethodTypeDescriptor constructDesc;
            if (call.tailCall) {
                assert node instanceof FunctionDefinition && IsStrict(node);
                constructDesc = FunctionDesc.ConstructorFunctionTailCall.construct;
            } else {
                constructDesc = compiler.desc.construct;
            }

            if (node.isInline()) {
                MethodCode constructEntry = newMethod(scriptFrame(methodName), constructDesc);

                boolean tailCall = compiler.constructInline.compile(this, node, constructEntry);
                construct = new FunctionCode(constructEntry.name(), null, null, tailCall);
            } else {
                MethodCode constructEntry = newMethod(hiddenFrame(methodName, "_construct"), constructDesc);

                construct = new FunctionCode(constructEntry.name(), call.instantiation, call.body, call.tailCall);
                compiler.construct.compile(this, node, constructEntry, construct);
            }

            // debug-info method
            if (isEnabled(Compiler.Option.DebugInfo)) {
                debugInfo = rt -> {
                    MethodCode method = newMethod(hiddenFrame(methodName, "_dbg"), MethodDescriptors.DebugInfo);
                    debugInfo(method, rt, call.entry, construct.entry, call.instantiation, call.body);
                    return method.name();
                };
            }
        } else {
            construct = null;

            // debug-info method
            if (isEnabled(Compiler.Option.DebugInfo)) {
                debugInfo = rt -> {
                    MethodCode method = newMethod(hiddenFrame(methodName, "_dbg"), MethodDescriptors.DebugInfo);
                    debugInfo(method, rt, call.entry, call.instantiation, call.body);
                    return method.name();
                };
            }
        }

        // runtime-info method
        MethodCode runtimeInfo = newMethod(hiddenFrame(methodName, "_rti"), MethodDescriptors.Function_RTI);
        new RuntimeInfoGenerator(this).runtimeInfo(node, runtimeInfo, call, construct, source.join(), debugInfo);

        return runtimeInfo.name();
    }

    private CompletableFuture<String> getSource(FunctionNode node) {
        if (INCLUDE_SOURCE && !isEnabled(Parser.Option.NativeFunction)) {
            return compressSource(node.getSource());
        }
        return NO_SOURCE;
    }

    private CompletableFuture<String> getSource(ClassDefinition node) {
        if (INCLUDE_SOURCE && !isEnabled(Parser.Option.NativeFunction)) {
            return compressSource(node.getSource());
        }
        return NO_SOURCE;
    }

    private CompletableFuture<String> compressSource(String source) {
        if (OFFTHREAD_SOURCE) {
            ExecutorService executor = getExecutor();
            if (!executor.isShutdown()) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        return SourceCompressor.compress(source);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }, executor);
            }
        }
        try {
            return CompletableFuture.completedFuture(SourceCompressor.compress(source));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean functionBody(FunctionNode node, MethodCode method) {
        return compileFunctionBody(node, method, body -> {
            Completion result = statements(node.getStatements(), body);
            if (!result.isAbrupt()) {
                // fall-thru, return undefined from function
                body.loadUndefined();
            }
            return result;
        });
    }

    private boolean functionCallInline(FunctionNode node, MethodCode method) {
        return compileInlineFunctionCall(node, method, body -> {
            Completion result = statements(node.getStatements(), body);
            if (!result.isAbrupt()) {
                // fall-thru, return undefined from function
                body.loadUndefined();
            }
            return result;
        });
    }

    private boolean functionConstructInline(FunctionNode node, MethodCode method) {
        return compileInlineFunctionConstruct(node, method, body -> {
            Variable<ExecutionContext> callerContext = body.getCallerContext();
            Variable<Constructor> newTarget = body.getNewTarget();
            Variable<ScriptObject> thisValue = body.newVariable("thisValue", ScriptObject.class);

            // OrdinaryCreateFromConstructor(callerContext, newTarget, %ObjectPrototype%)
            FunctionCodeGenerator.ordinaryCreateFromConstructor(callerContext, newTarget, thisValue, body);

            Completion result = statements(node.getStatements(), body);
            if (body.explicitReturn.isTarget()) {
                body.mark(body.explicitReturn);
                result = Completion.Normal;
            }
            if (!result.isAbrupt()) {
                // fall-thru, return `this` from function
                body.load(thisValue);
            }
            return result;
        });
    }

    private boolean arrowConciseBody(ArrowFunction node, MethodCode method) {
        return compileFunctionBody(node, method, body -> {
            // call expression in concise function body is always in tail-call position
            Expression expression = node.getExpression();
            body.enterTailCallPosition(expression);
            expressionBoxed(expression, body);
            return Completion.Normal;
        });
    }

    private boolean arrowConciseCallInline(ArrowFunction node, MethodCode method) {
        return compileInlineFunctionCall(node, method, body -> {
            // call expression in concise function body is always in tail-call position
            Expression expression = node.getExpression();
            body.enterTailCallPosition(expression);
            expressionBoxed(expression, body);
            return Completion.Normal;
        });
    }

    private boolean generatorBody(FunctionNode node, MethodCode method) {
        return compileGeneratorBody(node, method, body -> {
            Completion result = statements(node.getStatements(), body);
            if (!result.isAbrupt()) {
                // fall-thru, return undefined from function
                body.loadUndefined();
            }
            return result;
        });
    }

    private boolean asyncArrowConciseBody(AsyncArrowFunction node, MethodCode method) {
        return compileGeneratorBody(node, method, body -> {
            expressionBoxed(node.getExpression(), body);
            return Completion.Normal;
        });
    }

    private boolean generatorComprehensionBody(GeneratorComprehension node, MethodCode method) {
        return compileGeneratorBody(node, method, body -> {
            EvaluateGeneratorComprehension(this, node, body);
            body.loadUndefined();
            return Completion.Normal;
        });
    }

    private boolean compileFunctionBody(FunctionNode node, MethodCode method,
            Function<FunctionBodyVisitor, Completion> compiler) {
        FunctionBodyVisitor body = new FunctionBodyVisitor(method, node);
        return compileFunctionBody(node, compiler, body);
    }

    private boolean compileInlineFunctionCall(FunctionNode node, MethodCode method,
            Function<InlineFunctionCallVisitor, Completion> compiler) {
        InlineFunctionCallVisitor body = new InlineFunctionCallVisitor(method, node);
        return compileFunctionBody(node, compiler, body);
    }

    private boolean compileInlineFunctionConstruct(FunctionNode node, MethodCode method,
            Function<InlineFunctionConstructVisitor, Completion> compiler) {
        InlineFunctionConstructVisitor body = new InlineFunctionConstructVisitor(method, node);
        return compileFunctionBody(node, compiler, body);
    }

    private <VISITOR extends CodeVisitor> boolean compileFunctionBody(FunctionNode node,
            Function<VISITOR, Completion> compiler, VISITOR body) {
        body.lineInfo(node);
        body.begin();

        body.enterFunction(node);
        Completion result = compiler.apply(body);
        body.exitFunction();

        if (!result.isAbrupt()) {
            body._return();
        }
        body.end();
        return body.hasTailCalls();
    }

    private boolean compileGeneratorBody(FunctionNode node, MethodCode method,
            Function<GeneratorBodyVisitor, Completion> compiler) {
        GeneratorBodyVisitor body = new GeneratorBodyVisitor(method, node);
        body.lineInfo(node);
        body.begin();
        GeneratorState generatorState = body.generatorPrologue();

        body.enterFunction(node);
        Completion result = compiler.apply(body);
        body.exitFunction();

        if (!result.isAbrupt()) {
            body._return();
        }
        body.generatorEpilogue(generatorState);
        body.end();
        return body.hasTailCalls();
    }

    private void debugInfo(MethodCode method, MethodName... names) {
        InstructionAssembler asm = new InstructionAssembler(method);
        asm.begin();

        asm.anew(Methods.DebugInfo_new);
        for (MethodName name : names) {
            if (name != null) {
                asm.dup();
                asm.handle(name);
                asm.invoke(Methods.DebugInfo_addMethod);
            }
        }

        asm._return();
        asm.end();
    }

    /* ----------------------------------------------------------------------------------------- */

    private static final class ScriptCodeVisitor extends CodeVisitor {
        ScriptCodeVisitor(MethodCode method, Script node) {
            super(method, node);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }

        @Override
        protected Variable<Object> createCompletionVariable() {
            return newVariable("completion", Object.class);
        }

        @Override
        protected boolean hasCompletionValue() {
            return true;
        }
    }

    private static final class ModuleCodeVisitor extends CodeVisitor {
        ModuleCodeVisitor(MethodCode method, Module node) {
            super(method, node);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static final class FunctionBodyVisitor extends CodeVisitor {
        FunctionBodyVisitor(MethodCode method, FunctionNode node) {
            super(method, node);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static final class GeneratorBodyVisitor extends CodeVisitor {
        GeneratorBodyVisitor(MethodCode method, FunctionNode node) {
            super(method, node);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("rp", 1, Types.ResumptionPoint);
        }
    }

    private static final class InlineFunctionCallVisitor extends CodeVisitor {
        InlineFunctionCallVisitor(MethodCode method, FunctionNode node) {
            super(method, node);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterNameUnchecked("function", 0);
            setParameterName("callerContext", 1, Types.ExecutionContext);
            setParameterName("thisValue", 2, Types.Object);
            setParameterName("arguments", 3, Types.Object_);
        }

        @Override
        protected Variable<ExecutionContext> executionContextParameter() {
            return getParameter(1, ExecutionContext.class);
        }
    }

    private static final class InlineFunctionConstructVisitor extends CodeVisitor {
        final Jump explicitReturn = new Jump();

        InlineFunctionConstructVisitor(MethodCode method, FunctionNode node) {
            super(method, node);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterNameUnchecked("function", 0);
            setParameterName("callerContext", 1, Types.ExecutionContext);
            setParameterName("newTarget", 2, Types.Constructor);
            setParameterName("arguments", 3, Types.Object_);
        }

        Variable<ExecutionContext> getCallerContext() {
            return getParameter(1, ExecutionContext.class);
        }

        Variable<Constructor> getNewTarget() {
            return getParameter(2, Constructor.class);
        }

        @Override
        protected Variable<ExecutionContext> executionContextParameter() {
            return getCallerContext();
        }

        @Override
        protected void returnForCompletion() {
            // We only expect a single (boxed) literal on the stack.
            assert getStackSize() == 1 && isBoxedPrimitive(getStack()[0]);
            pop();
            goTo(explicitReturn);
        }

        private static boolean isBoxedPrimitive(Type t) {
            return ValType.of(t).isPrimitive() && !t.isPrimitive();
        }
    }
}
