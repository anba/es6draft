/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.ClassPropertyGenerator.ClassPropertyEvaluation;
import static com.github.anba.es6draft.compiler.GeneratorComprehensionGenerator.EvaluateGeneratorComprehension;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;
import static com.github.anba.es6draft.semantics.StaticSemantics.TemplateStrings;

import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.MethodDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.ExpressionVisitor.GeneratorState;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.Code;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.MethodTypeDescriptor;
import com.github.anba.es6draft.compiler.assembler.Stack;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.compiler.assembler.Variables;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ImmediateFuture;
import com.github.anba.es6draft.runtime.internal.JVMNames;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 
 */
final class CodeGenerator {
    private static final class Methods {
        // class: CompiledFunction
        static final MethodName CompiledFunction_Constructor = MethodName
                .findConstructor(Types.CompiledFunction,
                        Type.methodType(Type.VOID_TYPE, Types.RuntimeInfo$Function));

        // class: CompiledModule
        static final MethodName CompiledModule_Constructor = MethodName
                .findConstructor(Types.CompiledModule,
                        Type.methodType(Type.VOID_TYPE, Types.RuntimeInfo$ModuleBody));

        // class: CompiledScript
        static final MethodName CompiledScript_Constructor = MethodName
                .findConstructor(Types.CompiledScript,
                        Type.methodType(Type.VOID_TYPE, Types.RuntimeInfo$ScriptBody));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_GetTemplateObject = MethodName.findStatic(
                Types.ScriptRuntime, "GetTemplateObject", Type.methodType(Types.ArrayObject,
                        Type.INT_TYPE, Types.MethodHandle, Types.ExecutionContext));
    }

    private static final class MethodDescriptors {
        static final MethodTypeDescriptor FunctionConstructor = Type.methodType(Type.VOID_TYPE);
        static final MethodTypeDescriptor ModuleConstructor = Type.methodType(Type.VOID_TYPE);
        static final MethodTypeDescriptor ScriptConstructor = Type.methodType(Type.VOID_TYPE);

        static final MethodTypeDescriptor TemplateLiteral = Type.methodType(Types.String_);

        static final MethodTypeDescriptor StatementListMethod = Type.methodType(Types.Object,
                Types.ExecutionContext, Types.Object);

        static final MethodTypeDescriptor SpreadElementMethod = Type.methodType(Type.INT_TYPE,
                Types.ExecutionContext, Types.ArrayObject, Type.INT_TYPE);

        static final MethodTypeDescriptor PropertyDefinitionsMethod = Type.methodType(
                Type.VOID_TYPE, Types.ExecutionContext, Types.OrdinaryObject);

        static final MethodTypeDescriptor MethodDefinitionsMethod = Type.methodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.OrdinaryConstructorFunction, Types.OrdinaryObject);

        static final MethodTypeDescriptor ExpressionMethod = Type.methodType(Types.Object,
                Types.ExecutionContext);

        static final MethodTypeDescriptor BlockDeclarationInit = Type.methodType(
                Types.LexicalEnvironment, Types.ExecutionContext, Types.LexicalEnvironment);

        static final MethodTypeDescriptor AsyncFunction_Call = Type.methodType(Types.PromiseObject,
                Types.OrdinaryAsyncFunction, Types.ExecutionContext, Types.Object, Types.Object_);
        static final MethodTypeDescriptor Function_Call = Type.methodType(Types.Object,
                Types.OrdinaryFunction, Types.ExecutionContext, Types.Object, Types.Object_);
        static final MethodTypeDescriptor Generator_Call = Type.methodType(Types.GeneratorObject,
                Types.OrdinaryGenerator, Types.ExecutionContext, Types.Object, Types.Object_);

        static final MethodTypeDescriptor AsyncFunction_Construct = Type.methodType(
                Types.PromiseObject, Types.OrdinaryAsyncFunction, Types.ExecutionContext,
                Types.Constructor, Types.Object_);
        static final MethodTypeDescriptor Function_Construct = Type.methodType(Types.ScriptObject,
                Types.OrdinaryConstructorFunction, Types.ExecutionContext, Types.Constructor,
                Types.Object_);
        static final MethodTypeDescriptor Function_ConstructTailCall = Type.methodType(
                Types.Object, Types.OrdinaryConstructorFunction, Types.ExecutionContext,
                Types.Constructor, Types.Object_);
        static final MethodTypeDescriptor Generator_Construct = Type.methodType(
                Types.GeneratorObject, Types.OrdinaryGenerator, Types.ExecutionContext,
                Types.Constructor, Types.Object_);

        static final MethodTypeDescriptor AsyncFunction_Code = Type.methodType(Types.Object,
                Types.ExecutionContext, Types.ResumptionPoint);
        static final MethodTypeDescriptor FunctionNode_Code = Type.methodType(Types.Object,
                Types.ExecutionContext);
        static final MethodTypeDescriptor Generator_Code = Type.methodType(Types.Object,
                Types.ExecutionContext, Types.ResumptionPoint);

        static final MethodTypeDescriptor FunctionNode_Init = Type.methodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.FunctionObject, Types.Object_);
        static final MethodTypeDescriptor FunctionNode_RTI = Type
                .methodType(Types.RuntimeInfo$Function);
        static final MethodTypeDescriptor FunctionNode_DebugInfo = Type.methodType(Types.DebugInfo);

        static final MethodTypeDescriptor Script_Code = Type.methodType(Types.Object,
                Types.ExecutionContext);
        static final MethodTypeDescriptor Script_Init = Type.methodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.LexicalEnvironment);
        static final MethodTypeDescriptor Script_EvalInit = Type.methodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.LexicalEnvironment, Types.LexicalEnvironment);
        static final MethodTypeDescriptor Script_RTI = Type
                .methodType(Types.RuntimeInfo$ScriptBody);
        static final MethodTypeDescriptor Script_DebugInfo = Type.methodType(Types.DebugInfo);

        static final MethodTypeDescriptor Module_Code = Type.methodType(Types.Object,
                Types.ExecutionContext);
        static final MethodTypeDescriptor Module_Init = Type
                .methodType(Type.VOID_TYPE, Types.ExecutionContext, Types.LexicalEnvironment,
                        Types.Realm, Types.Map, Types.Map);
        static final MethodTypeDescriptor Module_RTI = Type
                .methodType(Types.RuntimeInfo$ScriptBody);
        static final MethodTypeDescriptor Module_DebugInfo = Type.methodType(Types.DebugInfo);
    }

    private static final boolean INCLUDE_SOURCE = true;
    private static final Future<String> NO_SOURCE = new ImmediateFuture<>(null);
    private static final int MAX_FNAME_LENGTH = 0x400;

    private final Code code;
    private final ExecutorService executor;
    private final EnumSet<CompatibilityOption> options;
    private final EnumSet<Parser.Option> parserOptions;
    private final EnumSet<Compiler.Option> compilerOptions;

    private final StatementGenerator stmtgen = new StatementGenerator(this);
    private final ExpressionGenerator exprgen = new ExpressionGenerator(this);
    private final PropertyGenerator propgen = new PropertyGenerator(this);

    CodeGenerator(Code code, ExecutorService executor, EnumSet<CompatibilityOption> options,
            EnumSet<Parser.Option> parserOptions, EnumSet<Compiler.Option> compilerOptions) {
        this.code = code;
        this.executor = executor;
        this.options = options;
        this.parserOptions = parserOptions;
        this.compilerOptions = compilerOptions;
    }

    boolean isEnabled(CompatibilityOption option) {
        return options.contains(option);
    }

    boolean isEnabled(Parser.Option option) {
        return parserOptions.contains(option);
    }

    boolean isEnabled(Compiler.Option option) {
        return compilerOptions.contains(option);
    }

    // template strings
    private final HashMap<TemplateLiteral, Integer> templateKeys = new HashMap<>();

    private int templateKey(TemplateLiteral template) {
        Integer key = templateKeys.get(template);
        if (key == null) {
            templateKeys.put(template, key = templateKeys.size());
        }
        return key;
    }

    /* ----------------------------------------------------------------------------------------- */

    enum ScriptName {
        Code, Init, EvalInit, RTI, DebugInfo
    }

    enum ModuleName {
        Code, Init, RTI, DebugInfo
    }

    enum FunctionName {
        Call, Construct, ConstructTailCall, Code, Init, RTI, DebugInfo
    }

    /**
     * Map of nodes to base method names
     */
    private final HashMap<Node, String> methodNames = new HashMap<>(32);
    private final AtomicInteger methodCounter = new AtomicInteger(0);

    private boolean isCompiled(Node node) {
        return methodNames.containsKey(node);
    }

    private String methodName(Script node, ScriptName name) {
        switch (name) {
        case Code:
            return "~script";
        case Init:
            return "!script_init";
        case EvalInit:
            return "!script_evalinit";
        case RTI:
            return "!script_rti";
        case DebugInfo:
            return "!script_dbg";
        default:
            throw new AssertionError();
        }
    }

    private String methodName(Module node, ModuleName name) {
        switch (name) {
        case Code:
            return "~module";
        case Init:
            return "!module_init";
        case RTI:
            return "!module_rti";
        case DebugInfo:
            return "!module_dbg";
        default:
            throw new AssertionError();
        }
    }

    private String methodName(StatementListMethod node) {
        String name = methodNames.get(node);
        if (name == null) {
            throw new IllegalStateException("no method-name present for: " + node);
        }
        return name;
    }

    private String methodName(TopLevelNode<?> topLevel, StatementListMethod node) {
        String baseName;
        if (topLevel instanceof FunctionNode) {
            baseName = methodName((FunctionNode) topLevel, FunctionName.Code);
        } else {
            assert topLevel instanceof Script;
            baseName = methodName((Script) topLevel, ScriptName.Code);
        }
        return addMethodName(node, baseName, '\'');
    }

    private String methodName(TemplateLiteral node) {
        return methodName(node, "!template");
    }

    private String methodName(SpreadElementMethod node) {
        return methodName(node, "!spread");
    }

    private String methodName(PropertyDefinitionsMethod node) {
        return methodName(node, "!propdef");
    }

    private String methodName(MethodDefinitionsMethod node) {
        return methodName(node, "!mdef");
    }

    private String methodName(ExpressionMethod node) {
        return methodName(node, "!expr");
    }

    private String methodName(BlockStatement node) {
        return methodName(node, "!block");
    }

    private String methodName(SwitchStatement node) {
        return methodName(node, "!block");
    }

    private String methodName(Node node, String name) {
        String n = methodNames.get(node);
        if (n == null) {
            n = addMethodName(node, name, '~');
        }
        return n;
    }

    private String methodName(FunctionNode node, FunctionName name) {
        String fname = methodNames.get(node);
        if (fname == null) {
            fname = addMethodName(node, getMethodName(node), '~');
        }
        switch (name) {
        case Call:
            return insertMarker("!", fname, "_call");
        case Code:
            return insertMarker("", fname, "");
        case Construct:
        case ConstructTailCall:
            return insertMarker("!", fname, "_construct");
        case Init:
            return insertMarker("", fname, "_init");
        case RTI:
            return insertMarker("!", fname, "_rti");
        case DebugInfo:
            return insertMarker("!", fname, "_dbg");
        default:
            throw new AssertionError();
        }
    }

    private String getMethodName(FunctionNode node) {
        String name = node.getMethodName();
        if (name.isEmpty()) {
            name = "anonymous";
        } else if (name.length() > MAX_FNAME_LENGTH) {
            name = name.substring(0, MAX_FNAME_LENGTH);
        }
        return name;
    }

    private String insertMarker(String prefix, String fname, String suffix) {
        return JVMNames.addPrefixSuffix(fname, prefix, suffix);
    }

    private String addMethodName(Node node, String name, char sep) {
        assert !methodNames.containsKey(node);
        String n = JVMNames.toBytecodeName(name + sep + methodCounter.incrementAndGet());
        methodNames.put(node, n);
        return n;
    }

    /* ----------------------------------------------------------------------------------------- */

    private MethodTypeDescriptor methodDescriptor(TemplateLiteral node) {
        return MethodDescriptors.TemplateLiteral;
    }

    private MethodTypeDescriptor methodDescriptor(StatementListMethod node) {
        return MethodDescriptors.StatementListMethod;
    }

    private MethodTypeDescriptor methodDescriptor(SpreadElementMethod node) {
        return MethodDescriptors.SpreadElementMethod;
    }

    private MethodTypeDescriptor methodDescriptor(PropertyDefinitionsMethod node) {
        return MethodDescriptors.PropertyDefinitionsMethod;
    }

    private MethodTypeDescriptor methodDescriptor(MethodDefinitionsMethod node) {
        return MethodDescriptors.MethodDefinitionsMethod;
    }

    private MethodTypeDescriptor methodDescriptor(ExpressionMethod node) {
        return MethodDescriptors.ExpressionMethod;
    }

    private MethodTypeDescriptor methodDescriptor(BlockStatement node) {
        return MethodDescriptors.BlockDeclarationInit;
    }

    private MethodTypeDescriptor methodDescriptor(SwitchStatement node) {
        return MethodDescriptors.BlockDeclarationInit;
    }

    private MethodTypeDescriptor methodDescriptor(FunctionNode node, FunctionName name) {
        switch (name) {
        case Call:
            if (node.isGenerator()) {
                return MethodDescriptors.Generator_Call;
            }
            if (node.isAsync()) {
                return MethodDescriptors.AsyncFunction_Call;
            }
            return MethodDescriptors.Function_Call;
        case ConstructTailCall:
            assert node.isConstructor() && !node.isGenerator() && !node.isAsync();
            return MethodDescriptors.Function_ConstructTailCall;
        case Construct:
            assert node.isConstructor();
            if (node.isGenerator()) {
                return MethodDescriptors.Generator_Construct;
            }
            if (node.isAsync()) {
                return MethodDescriptors.AsyncFunction_Construct;
            }
            return MethodDescriptors.Function_Construct;
        case Code:
            if (node.isGenerator()) {
                return MethodDescriptors.Generator_Code;
            }
            if (node.isAsync()) {
                return MethodDescriptors.AsyncFunction_Code;
            }
            return MethodDescriptors.FunctionNode_Code;
        case Init:
            return MethodDescriptors.FunctionNode_Init;
        case RTI:
            return MethodDescriptors.FunctionNode_RTI;
        case DebugInfo:
            return MethodDescriptors.FunctionNode_DebugInfo;
        default:
            throw new AssertionError();
        }
    }

    private MethodTypeDescriptor methodDescriptor(Script node, ScriptName name) {
        switch (name) {
        case Code:
            return MethodDescriptors.Script_Code;
        case Init:
            return MethodDescriptors.Script_Init;
        case EvalInit:
            return MethodDescriptors.Script_EvalInit;
        case RTI:
            return MethodDescriptors.Script_RTI;
        case DebugInfo:
            return MethodDescriptors.Script_DebugInfo;
        default:
            throw new AssertionError();
        }
    }

    private MethodTypeDescriptor methodDescriptor(Module node, ModuleName name) {
        switch (name) {
        case Code:
            return MethodDescriptors.Module_Code;
        case Init:
            return MethodDescriptors.Module_Init;
        case RTI:
            return MethodDescriptors.Module_RTI;
        case DebugInfo:
            return MethodDescriptors.Module_DebugInfo;
        default:
            throw new AssertionError();
        }
    }

    /* ----------------------------------------------------------------------------------------- */

    /**
     * Map of concrete method names to class names
     */
    private final HashMap<String, Type> methodClasses = new HashMap<>(32 * 4);

    private MethodCode publicStaticMethod(String methodName, MethodTypeDescriptor methodDescriptor) {
        final int access = Modifier.PUBLIC | Modifier.STATIC;
        MethodCode method = code.newMethod(access, methodName, methodDescriptor);
        // System.out.printf("add <%s, %s>%n", methodName, method.classCode.className);
        assert !methodClasses.containsKey(methodName) : String.format(
                "method '%s' already compiled", methodName);
        methodClasses.put(methodName, method.classCode.classType);
        return method;
    }

    private MethodCode newMethod(TemplateLiteral node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod(TopLevelNode<?> topLevel, StatementListMethod node) {
        return publicStaticMethod(methodName(topLevel, node), methodDescriptor(node));
    }

    private MethodCode newMethod(SpreadElementMethod node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod(PropertyDefinitionsMethod node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod(MethodDefinitionsMethod node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod(ExpressionMethod node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod(BlockStatement node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod(SwitchStatement node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    MethodCode newMethod(FunctionNode node, FunctionName name) {
        return publicStaticMethod(methodName(node, name), methodDescriptor(node, name));
    }

    MethodCode newMethod(Script node, ScriptName name) {
        return publicStaticMethod(methodName(node, name), methodDescriptor(node, name));
    }

    MethodCode newMethod(Module node, ModuleName name) {
        return publicStaticMethod(methodName(node, name), methodDescriptor(node, name));
    }

    /* ----------------------------------------------------------------------------------------- */

    private Type owner(String methodName) {
        Type owner = methodClasses.get(methodName);
        assert owner != null : String.format("method '%s' not yet compiled", methodName);
        return owner;
    }

    MethodName methodDesc(TemplateLiteral node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    MethodName methodDesc(StatementListMethod node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    MethodName methodDesc(SpreadElementMethod node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    MethodName methodDesc(PropertyDefinitionsMethod node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    MethodName methodDesc(MethodDefinitionsMethod node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    MethodName methodDesc(ExpressionMethod node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    MethodName methodDesc(BlockStatement node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    MethodName methodDesc(SwitchStatement node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    MethodName methodDesc(FunctionNode node, FunctionName name) {
        String methodName = methodName(node, name);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node, name));
    }

    MethodName methodDesc(Script node, ScriptName name) {
        String methodName = methodName(node, name);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node, name));
    }

    MethodName methodDesc(Module node, ModuleName name) {
        String methodName = methodName(node, name);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node, name));
    }

    /* ----------------------------------------------------------------------------------------- */

    /**
     * [12.2.8.2.2] Runtime Semantics: GetTemplateObject
     * 
     * @param node
     *            the template literal
     * @param mv
     *            the expression visitor
     */
    void GetTemplateObject(TemplateLiteral node, ExpressionVisitor mv) {
        assert isCompiled(node);

        // GetTemplateObject
        mv.iconst(templateKey(node));
        mv.handle(methodDesc(node));
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_GetTemplateObject);
    }

    void compile(TemplateLiteral node) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            InstructionVisitor body = new InstructionVisitor(method);
            body.lineInfo(node.getBeginLine());
            body.begin();

            List<TemplateCharacters> strings = TemplateStrings(node);
            body.anewarray(strings.size() * 2, Types.String);
            for (int i = 0, size = strings.size(); i < size; ++i) {
                TemplateCharacters e = strings.get(i);
                int index = i << 1;
                body.astore(index, e.getValue());
                body.astore(index + 1, e.getRawValue());
            }

            body._return();
            body.end();
        }
    }

    void compile(Script node) {
        // initialization methods
        new GlobalDeclarationInstantiationGenerator(this).generate(node);
        new EvalDeclarationInstantiationGenerator(this).generate(node);

        // runtime method
        scriptBody(node);

        // runtime-info method
        new RuntimeInfoGenerator(this).runtimeInfo(node);

        // add default constructor
        defaultScriptConstructor(node);
    }

    private void scriptBody(Script node) {
        MethodCode method = newMethod(node, ScriptName.Code);
        StatementVisitor body = new ScriptStatementVisitor(method, node);
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

    private void defaultScriptConstructor(Script node) {
        InstructionVisitor mv = new InstructionVisitor(code.newConstructor(Modifier.PUBLIC,
                MethodDescriptors.ScriptConstructor));
        mv.begin();
        mv.loadThis();
        mv.invoke(methodDesc(node, ScriptName.RTI));
        mv.invoke(Methods.CompiledScript_Constructor);
        mv._return();
        mv.end();
    }

    void compile(Module node, ModuleRecord moduleRecord) {
        // initialization methods
        new ModuleDeclarationInstantiationGenerator(this).generate(node, moduleRecord);

        // runtime method
        moduleBody(node);

        // runtime-info method
        new RuntimeInfoGenerator(this).runtimeInfo(node);

        // add default constructor
        defaultModuleConstructor(node);
    }

    private void moduleBody(Module node) {
        MethodCode method = newMethod(node, ModuleName.Code);
        StatementVisitor body = new ModuleStatementVisitor(method, node);
        body.lineInfo(node);
        body.begin();
        // TODO: completion value not needed
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

    private void defaultModuleConstructor(Module node) {
        InstructionVisitor mv = new InstructionVisitor(code.newConstructor(Modifier.PUBLIC,
                MethodDescriptors.ModuleConstructor));
        mv.begin();
        mv.loadThis();
        mv.invoke(methodDesc(node, ModuleName.RTI));
        mv.invoke(Methods.CompiledModule_Constructor);
        mv._return();
        mv.end();
    }

    void compileFunction(FunctionNode function) {
        // TODO: async functions
        if (function instanceof FunctionDefinition) {
            compile((FunctionDefinition) function);
        } else {
            assert function instanceof GeneratorDefinition;
            compile((GeneratorDefinition) function);
        }

        // add default constructor
        defaultFunctionConstructor(function);
    }

    private void defaultFunctionConstructor(FunctionNode function) {
        InstructionVisitor mv = new InstructionVisitor(code.newConstructor(Modifier.PUBLIC,
                MethodDescriptors.FunctionConstructor));
        mv.begin();
        mv.loadThis();
        mv.invoke(methodDesc(function, FunctionName.RTI));
        mv.invoke(Methods.CompiledFunction_Constructor);
        mv._return();
        mv.end();
    }

    void compile(GeneratorComprehension node) {
        compile((FunctionNode) node);
    }

    void compile(FunctionDefinition node) {
        compile((FunctionNode) node);
    }

    void compile(GeneratorDefinition node) {
        compile((FunctionNode) node);
    }

    void compile(ArrowFunction node) {
        compile((FunctionNode) node);
    }

    void compile(MethodDefinition node) {
        compile((FunctionNode) node);
    }

    void compile(AsyncArrowFunction node) {
        compile((FunctionNode) node);
    }

    void compile(AsyncFunctionDefinition node) {
        compile((FunctionNode) node);
    }

    private void compile(FunctionNode node) {
        if (!isCompiled(node)) {
            Future<String> source = getSource(node);

            // initialization method
            new FunctionDeclarationInstantiationGenerator(this).generate(node);

            // runtime method
            boolean tailCall;
            if (node instanceof ArrowFunction && ((ArrowFunction) node).getExpression() != null) {
                tailCall = conciseFunctionBody((ArrowFunction) node);
            } else if (node instanceof AsyncArrowFunction
                    && ((AsyncArrowFunction) node).getExpression() != null) {
                tailCall = conciseAsyncFunctionBody((AsyncArrowFunction) node);
            } else if (node instanceof GeneratorComprehension) {
                tailCall = generatorComprehensionBody((GeneratorComprehension) node);
            } else if (node.isGenerator()) {
                tailCall = generatorBody(node);
            } else if (node.isAsync()) {
                tailCall = asyncFunctionBody(node);
            } else {
                tailCall = functionBody(node);
            }

            // call method
            new FunctionCodeGenerator(this).generate(node, tailCall);

            // runtime-info method
            new RuntimeInfoGenerator(this).runtimeInfo(node, tailCall, source);
        }
    }

    private Future<String> getSource(FunctionNode node) {
        if (INCLUDE_SOURCE && !isEnabled(Parser.Option.NativeFunction)) {
            String source = Strings.concat(node.getHeaderSource(), node.getBodySource());
            return executor.submit(SourceCompressor.compress(source));
        }
        return NO_SOURCE;
    }

    private boolean conciseFunctionBody(ArrowFunction node) {
        MethodCode method = newMethod(node, FunctionName.Code);
        ExpressionVisitor body = new ArrowFunctionVisitor(method, node);
        body.lineInfo(node);
        body.begin();

        // expression as value to ensure tail-call nodes set contains the value node
        Expression expression = node.getExpression().asValue();

        // call expression in concise function body is always in tail-call position
        body.enterTailCallPosition(expression);

        body.enterScope(node);
        expressionBoxedValue(expression, body);
        body.exitScope();

        body._return();
        body.end();

        return body.hasTailCalls();
    }

    private boolean functionBody(FunctionNode node) {
        MethodCode method = newMethod(node, FunctionName.Code);
        StatementVisitor body = new FunctionStatementVisitor(method, node);
        body.lineInfo(node);
        body.begin();

        body.enterScope(node);
        Completion result = statements(node.getStatements(), body);
        body.exitScope();

        if (!result.isAbrupt()) {
            // fall-thru, return <null> from function
            body.anull();
            body._return();
        }

        body.end();

        return body.hasTailCalls();
    }

    private boolean conciseAsyncFunctionBody(AsyncArrowFunction node) {
        MethodCode method = newMethod(node, FunctionName.Code);
        AsyncArrowFunctionVisitor body = new AsyncArrowFunctionVisitor(method, node);
        body.lineInfo(node);
        body.begin();
        Variable<ResumptionPoint> resume = body.getResumeParameter();
        GeneratorState state = null;
        if (body.isResumable() && !isEnabled(Compiler.Option.NoResume)) {
            state = body.prologue(resume);
        }

        body.enterScope(node);
        expressionBoxedValue(node.getExpression(), body);
        body.exitScope();

        body._return();
        if (state != null) {
            body.epilogue(resume, state);
        }
        body.end();

        return body.hasTailCalls();
    }

    private boolean asyncFunctionBody(FunctionNode node) {
        // Create the same function body as for generator functions
        return generatorBody(node);
    }

    private boolean generatorBody(FunctionNode node) {
        MethodCode method = newMethod(node, FunctionName.Code);
        GeneratorStatementVisitor body = new GeneratorStatementVisitor(method, node);
        body.lineInfo(node);
        body.begin();
        Variable<ResumptionPoint> resume = body.getResumeParameter();
        GeneratorState state = null;
        if (body.isResumable() && !isEnabled(Compiler.Option.NoResume)) {
            state = body.prologue(resume);
        }

        body.enterScope(node);
        Completion result = statements(node.getStatements(), body);
        body.exitScope();

        if (!result.isAbrupt()) {
            // TODO: change back to <undefined> and update GeneratorObject
            // fall-thru, return <null> from function
            body.anull();
            body._return();
        }

        if (state != null) {
            body.epilogue(resume, state);
        }
        body.end();

        return body.hasTailCalls();
    }

    private boolean generatorComprehensionBody(GeneratorComprehension node) {
        MethodCode method = newMethod(node, FunctionName.Code);
        GeneratorComprehensionVisitor body = new GeneratorComprehensionVisitor(method, node);
        body.lineInfo(node);
        body.begin();
        Variable<ResumptionPoint> resume = body.getResumeParameter();
        GeneratorState state = null;
        if (body.isResumable() && !isEnabled(Compiler.Option.NoResume)) {
            state = body.prologue(resume);
        }

        body.enterScope(node);
        EvaluateGeneratorComprehension(this, node, body);
        body.exitScope();

        body.loadUndefined();
        body._return();

        if (state != null) {
            body.epilogue(resume, state);
        }
        body.end();

        return body.hasTailCalls();
    }

    void compile(StatementListMethod node, StatementVisitor mv) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(mv.getTopLevelNode(), node);
            StatementVisitor body = new StatementListMethodStatementVisitor(method, mv);
            body.lineInfo(node);
            body.nop(); // force line-number entry
            body.begin();

            Completion result = statements(node.getStatements(), body);

            if (!result.isAbrupt()) {
                // fall-thru, return `null` sentinel or completion value
                body.loadCompletionValue();
                body._return();
            }

            body.end();
        }
    }

    void compile(SpreadElementMethod node, ExpressionVisitor mv) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            SpreadElementMethodVisitor body = new SpreadElementMethodVisitor(method, mv);
            body.lineInfo(node);
            body.begin();

            body.loadArrayObject();
            body.loadArrayIndex();
            expression(node.getExpression(), body);

            body._return();
            body.end();
        }
    }

    void compile(PropertyDefinitionsMethod node, ExpressionVisitor mv) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            PropertyDefinitionsMethodVisitor body = new PropertyDefinitionsMethodVisitor(method, mv);
            body.lineInfo(node);
            body.begin();

            Variable<OrdinaryObject> object = body.getObjectParameter();
            for (PropertyDefinition property : node.getProperties()) {
                body.load(object);
                propertyDefinition(property, body);
            }

            body._return();
            body.end();
        }
    }

    void compile(MethodDefinitionsMethod node, ExpressionVisitor mv) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            MethodDefinitionsMethodVisitor body = new MethodDefinitionsMethodVisitor(method, mv);
            body.lineInfo(node);
            body.begin();

            Variable<OrdinaryConstructorFunction> function = body.getFunctionParameter();
            Variable<OrdinaryObject> proto = body.getPrototypeParameter();
            ClassPropertyEvaluation(this, node.getProperties(), function, proto, body);

            body._return();
            body.end();
        }
    }

    void compile(ExpressionMethod node, ExpressionVisitor mv) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            ExpressionMethodVisitor body = new ExpressionMethodVisitor(method, mv);
            body.lineInfo(node);
            body.begin();

            expressionBoxedValue(node.getExpression(), body);

            body._return();
            body.end();
        }
    }

    void compile(BlockStatement node, StatementVisitor mv,
            BlockDeclarationInstantiationGenerator generator) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            BlockDeclInitMethodGenerator body = new BlockDeclInitMethodGenerator(method, mv);
            body.lineInfo(node);
            body.begin();

            body.loadLexicalEnvironment();
            generator.generateMethod(node, body);

            body._return();
            body.end();
        }
    }

    void compile(SwitchStatement node, StatementVisitor mv,
            BlockDeclarationInstantiationGenerator generator) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            BlockDeclInitMethodGenerator body = new BlockDeclInitMethodGenerator(method, mv);
            body.lineInfo(node);
            body.begin();

            body.loadLexicalEnvironment();
            generator.generateMethod(node, body);

            body._return();
            body.end();
        }
    }

    /* ----------------------------------------------------------------------------------------- */

    ValType expression(Expression node, ExpressionVisitor mv) {
        return node.accept(exprgen, mv);
    }

    ValType expressionValue(Expression node, ExpressionVisitor mv) {
        Expression valueNode = node.asValue();
        ValType type = valueNode.accept(exprgen, mv);
        assert type != ValType.Reference : "value node returned reference: " + valueNode.getClass();
        return type;
    }

    ValType expressionBoxedValue(Expression node, ExpressionVisitor mv) {
        ValType type = expressionValue(node, mv);
        if (type.isJavaPrimitive()) {
            mv.toBoxed(type);
            return ValType.Any;
        }
        return type;
    }

    void propertyDefinition(PropertyDefinition node, ExpressionVisitor mv) {
        node.accept(propgen, mv);
    }

    Completion statement(ModuleItem node, StatementVisitor mv) {
        return node.accept(stmtgen, mv);
    }

    Completion statements(List<? extends ModuleItem> statements, StatementVisitor mv) {
        // 13.1.13 Runtime Semantics: Evaluation<br>
        // StatementList : StatementList StatementListItem
        /* steps 1-6 */
        Completion result = Completion.Normal;
        for (ModuleItem item : statements) {
            if ((result = result.then(statement(item, mv))).isAbrupt()) {
                break;
            }
        }
        return result;
    }

    /* ----------------------------------------------------------------------------------------- */

    private static final class ScriptStatementVisitor extends StatementVisitor {
        ScriptStatementVisitor(MethodCode method, Script node) {
            super(method, IsStrict(node), node, node.isGlobalCode() ? CodeType.GlobalScript
                    : CodeType.NonGlobalScript);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static final class ModuleStatementVisitor extends StatementVisitor {
        ModuleStatementVisitor(MethodCode method, Module node) {
            super(method, true, node, CodeType.GlobalScript);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static final class FunctionStatementVisitor extends StatementVisitor {
        FunctionStatementVisitor(MethodCode method, FunctionNode node) {
            super(method, IsStrict(node), node, CodeType.Function);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static final class GeneratorStatementVisitor extends StatementVisitor {
        GeneratorStatementVisitor(MethodCode method, FunctionNode node) {
            super(method, IsStrict(node), node, CodeType.Function);
        }

        @Override
        protected Stack createStack(Variables variables) {
            return new StackImpl(variables);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("rp", 1, Types.ResumptionPoint);
        }

        Variable<ResumptionPoint> getResumeParameter() {
            return getParameter(1, ResumptionPoint.class);
        }
    }

    private static final class StatementListMethodStatementVisitor extends StatementVisitor {
        StatementListMethodStatementVisitor(MethodCode method, StatementVisitor parent) {
            super(method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("completion", 1, Types.Object);
        }
    }

    private static final class ArrowFunctionVisitor extends ExpressionVisitor {
        ArrowFunctionVisitor(MethodCode method, ArrowFunction node) {
            super(method, IsStrict(node), false, node.hasSyntheticNodes());
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static final class AsyncArrowFunctionVisitor extends ExpressionVisitor {
        AsyncArrowFunctionVisitor(MethodCode method, AsyncArrowFunction node) {
            super(method, IsStrict(node), false, node.hasSyntheticNodes());
        }

        @Override
        protected Stack createStack(Variables variables) {
            return new StackImpl(variables);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("rp", 1, Types.ResumptionPoint);
        }

        Variable<ResumptionPoint> getResumeParameter() {
            return getParameter(1, ResumptionPoint.class);
        }
    }

    private static final class GeneratorComprehensionVisitor extends ExpressionVisitor {
        GeneratorComprehensionVisitor(MethodCode method, GeneratorComprehension node) {
            super(method, IsStrict(node), false, node.hasSyntheticNodes());
        }

        @Override
        protected Stack createStack(Variables variables) {
            return new StackImpl(variables);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("rp", 1, Types.ResumptionPoint);
        }

        Variable<ResumptionPoint> getResumeParameter() {
            return getParameter(1, ResumptionPoint.class);
        }
    }

    private static final class ExpressionMethodVisitor extends ExpressionVisitor {
        ExpressionMethodVisitor(MethodCode method, ExpressionVisitor parent) {
            super(method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static final class SpreadElementMethodVisitor extends ExpressionVisitor {
        SpreadElementMethodVisitor(MethodCode method, ExpressionVisitor parent) {
            super(method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("array", 1, Types.ArrayObject);
            setParameterName("index", 2, Type.INT_TYPE);
        }

        void loadArrayObject() {
            loadParameter(1, ArrayObject.class);
        }

        void loadArrayIndex() {
            loadParameter(2, int.class);
        }
    }

    private static final class PropertyDefinitionsMethodVisitor extends ExpressionVisitor {
        PropertyDefinitionsMethodVisitor(MethodCode method, ExpressionVisitor parent) {
            super(method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("object", 1, Types.OrdinaryObject);
        }

        Variable<OrdinaryObject> getObjectParameter() {
            return getParameter(1, OrdinaryObject.class);
        }
    }

    private static final class MethodDefinitionsMethodVisitor extends ExpressionVisitor {
        MethodDefinitionsMethodVisitor(MethodCode method, ExpressionVisitor parent) {
            super(method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("F", 1, Types.OrdinaryConstructorFunction);
            setParameterName("proto", 2, Types.OrdinaryObject);
        }

        Variable<OrdinaryConstructorFunction> getFunctionParameter() {
            return getParameter(1, OrdinaryConstructorFunction.class);
        }

        Variable<OrdinaryObject> getPrototypeParameter() {
            return getParameter(2, OrdinaryObject.class);
        }
    }

    private static final class BlockDeclInitMethodGenerator extends ExpressionVisitor {
        BlockDeclInitMethodGenerator(MethodCode method, StatementVisitor parent) {
            super(method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("env", 1, Types.LexicalEnvironment);
        }

        void loadLexicalEnvironment() {
            loadParameter(1, LexicalEnvironment.class);
        }
    }
}
