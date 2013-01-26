/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.DeclarationBindingInstantiation.FunctionDeclarationInstantiation;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Generator;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Scriptable;

/**
 * TODO: for now basically a copy of {@link OrdinaryFunction}
 */
public class OrdinaryGenerator extends OrdinaryObject implements Generator {
    private RuntimeInfo.FormalParameterList parameters;
    private RuntimeInfo.Code code;
    private LexicalEnvironment scope;
    private boolean strict;
    private Scriptable home;
    private String methodName;
    private Realm realm;
    private ThisMode thisMode;

    private String source = null;

    public OrdinaryGenerator(Realm realm) {
        super(realm);
    }

    /**
     * 
     */
    public static Generator GeneratorCreate(Realm realm, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope) {
        return GeneratorCreate(realm, kind, function, scope, null, null, null);
    }

    /**
     * 
     */
    public static Generator GeneratorCreate(Realm realm, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope, Scriptable prototype,
            Scriptable homeObject, String methodName) {
        assert function.isGenerator();

        RuntimeInfo.FormalParameterList parameters = function.formals();
        RuntimeInfo.Code body = function.code();
        boolean strict = function.isStrict();
        /* Note: [13.6 FunctionCreate] steps 2,3,6,7 are methods */
        /* [13.6 FunctionCreate] step 1 */
        OrdinaryGenerator f = new OrdinaryGenerator(realm);
        /* [13.6 FunctionCreate] step 4 */
        if (prototype == null) {
            prototype = realm.getIntrinsic(Intrinsics.FunctionPrototype);
        }
        /* [13.6 FunctionCreate] step 5 */
        f.setPrototype(prototype);
        /* [13.6 FunctionCreate] step 8 */
        f.scope = scope;
        /* [13.6 FunctionCreate] step 9 */
        f.parameters = parameters;
        /* [13.6 FunctionCreate] step 10 */
        f.code = body;
        /* [13.6 FunctionCreate] step 11 */
        // f.[[Extensible]] = true (implicit)
        /* [13.6 FunctionCreate] step 12 */
        f.realm = realm;
        /* [13.6 FunctionCreate] step 13 */
        f.home = homeObject;
        /* [13.6 FunctionCreate] step 14 */
        f.methodName = methodName;
        /* [13.6 FunctionCreate] step 15-17 */
        if (kind == FunctionKind.Arrow) {
            f.thisMode = ThisMode.Lexical;
        } else if (strict) {
            f.thisMode = ThisMode.Strict;
        } else {
            f.thisMode = ThisMode.Global;
        }
        /* [13.6 FunctionCreate] step 18 */
        int len = parameters.expectedArgumentCount();
        /* [13.6 FunctionCreate] step 19 */
        f.defineOwnProperty("length", new PropertyDescriptor(len, false, false, false));
        String name = function.functionName() != null ? function.functionName() : "";
        f.defineOwnProperty("name", new PropertyDescriptor(name, false, false, false));
        /* [13.6 FunctionCreate] step 20 */
        if (kind == FunctionKind.Normal && strict) {
            AddRestrictedFunctionProperties(realm, f);
        }
        /* [13.6 FunctionCreate] step 21 */
        f.strict = strict;
        /* [13.6 FunctionCreate] step 22 */
        return f;
    }

    /**
     * 
     */
    public static Generator InstantiateGeneratorObject(Realm realm, LexicalEnvironment scope,
            RuntimeInfo.Function fd) {
        /* step 1-2 */
        Generator f = GeneratorCreate(realm, FunctionKind.Normal, fd, scope);
        /* step 3 */
        MakeConstructor(realm, f);
        /* step 4 */
        return f;
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        /* [13.6 FunctionCreate] step 3 */
        return BuiltinBrand.BuiltinFunction;
    }

    @Override
    public String toSource() {
        String source = this.source;
        if (source == null) {
            String src = code.source();
            if (src != null) {
                try {
                    source = SourceCompressor.decompress(src).call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                source = "function F() { /* source not available */ }";
            }
            this.source = source;
        }
        return source;
    }

    /**
     * 8.3.19.1 [[Call]] Internal Method
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        /* step 1-11 */
        ExecutionContext calleeContext = ExecutionContext.newFunctionExecutionContext(this,
                thisValue);
        /* step 12-13 */
        FunctionDeclarationInstantiation(calleeContext, this, args);
        /* step 14-15 */
        GeneratorObject result = new GeneratorObject(getRealm(), this, calleeContext);
        result.initialise(getRealm());
        /* step 16 */
        return result;
    }

    /**
     * 8.3.19.2 [[Construct]] Internal Method
     */
    @Override
    public Object construct(Object... args) {
        return OrdinaryConstruct(realm, this, args);
    }

    /**
     * [[Scope]]
     */
    @Override
    public LexicalEnvironment getScope() {
        return scope;
    }

    /**
     * [[FormalParameters]]
     */
    @Override
    public RuntimeInfo.FormalParameterList getParameterList() {
        return parameters;
    }

    /**
     * [[Code]]
     */
    @Override
    public RuntimeInfo.Code getCode() {
        return code;
    }

    /**
     * [[Realm]]
     */
    @Override
    public Realm getRealm() {
        return realm;
    }

    /**
     * [[ThisMode]]
     */
    @Override
    public ThisMode getThisMode() {
        return thisMode;
    }

    /**
     * [[Strict]]
     */
    @Override
    public boolean isStrict() {
        return strict;
    }

    /**
     * [[Home]]
     */
    @Override
    public Scriptable getHome() {
        return home;
    }

    /**
     * [[MethodName]]
     */
    @Override
    public String getMethodName() {
        return methodName;
    }
}
