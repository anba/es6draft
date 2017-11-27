/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

import com.github.anba.es6draft.ast.scope.FunctionScope;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 */
public interface FunctionNode extends TopLevelNode<StatementListItem>, ScopedNode {
    enum StrictMode {
        NonStrict, ImplicitStrict, ExplicitStrict
    }

    enum ThisMode {
        Lexical, Strict, Global
    }

    /**
     * Returns the binding identifier of this function or {@code null} if not present.
     * 
     * @return the binding identifier or {@code null}
     */
    BindingIdentifier getIdentifier();

    /**
     * Returns the method name for compilation.
     * 
     * @return the method name
     */
    String getMethodName();

    /**
     * Sets the method name for compilation.
     * 
     * @param methodName
     *            the method name
     */
    void setMethodName(String methodName);

    /**
     * Returns the function name.
     * 
     * @return the function name
     */
    String getFunctionName();

    /**
     * Sets the function name for this object.
     * 
     * @param functionName
     *            the function name
     */
    void setFunctionName(String functionName);

    /**
     * Returns the formal parameter list for this function.
     * 
     * @return the parameters
     */
    FormalParameterList getParameters();

    /**
     * Returns the strict-mode for this object.
     * 
     * @return the strict-mode
     */
    StrictMode getStrictMode();

    /**
     * Sets the strict-mode for this object.
     * 
     * @param strictMode
     *            the new strict-mode
     */
    void setStrictMode(StrictMode strictMode);

    /**
     * Returns the source string for this function.
     * 
     * @return the source string
     */
    String getSource();

    @Override
    List<StatementListItem> getStatements();

    @Override
    void setStatements(List<StatementListItem> statements);

    @Override
    FunctionScope getScope();

    /**
     * Returns the [[ThisMode]] of this function.
     * 
     * @return the this-mode field
     */
    ThisMode getThisMode();

    /**
     * Returns <code>true</code> if this function is a generator function.
     * 
     * @return <code>true</code> if this node is a generator
     */
    boolean isGenerator();

    /**
     * Returns <code>true</code> if this function is an async function.
     * 
     * @return <code>true</code> if this node is an async function
     */
    boolean isAsync();

    /**
     * Returns <code>true</code> if this function is a constructor function.
     * 
     * @return <code>true</code> if this node is a constructor function
     */
    boolean isConstructor();

    /**
     * Returns <code>true</code> if this function is inlineable.
     * 
     * @return <code>true</code> if this node is inlineable
     */
    default boolean isInline() {
        return Functions.isInlinable(this);
    }
}
