/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.interpreter;

import static com.github.anba.es6draft.interpreter.DeclarationBindingInstantiation.EvalDeclarationInstantiation;
import static com.github.anba.es6draft.interpreter.DeclarationBindingInstantiation.GlobalDeclarationInstantiation;

import java.util.Objects;

import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.DebugInfo;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * 
 */
final class InterpretedScriptBody implements RuntimeInfo.ScriptBody {
    private final Script parsedScript;

    InterpretedScriptBody(Script parsedScript) {
        this.parsedScript = parsedScript;
    }

    @Override
    public String sourceName() {
        return parsedScript.getSource().getName();
    }

    @Override
    public String sourceFile() {
        return Objects.toString(parsedScript.getSource().getFile(), null);
    }

    @Override
    public boolean isStrict() {
        return parsedScript.isStrict();
    }

    @Override
    public void globalDeclarationInstantiation(ExecutionContext cx,
            LexicalEnvironment<GlobalEnvironmentRecord> globalEnv,
            LexicalEnvironment<?> lexicalEnv, boolean deletableBindings) {
        GlobalDeclarationInstantiation(cx, parsedScript, globalEnv, lexicalEnv, deletableBindings);
    }

    @Override
    public void evalDeclarationInstantiation(ExecutionContext cx, LexicalEnvironment<?> varEnv,
            LexicalEnvironment<?> lexEnv, boolean deletableBindings) {
        EvalDeclarationInstantiation(cx, parsedScript, varEnv, lexEnv, deletableBindings);
    }

    @Override
    public Object evaluate(ExecutionContext cx) {
        return parsedScript.accept(new Interpreter(parsedScript), cx);
    }

    @Override
    public DebugInfo debugInfo() {
        return null;
    }
}
