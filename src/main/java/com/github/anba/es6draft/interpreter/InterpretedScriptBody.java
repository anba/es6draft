/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.interpreter;

import static com.github.anba.es6draft.interpreter.DeclarationBindingInstantiation.EvalDeclarationInstantiation;
import static com.github.anba.es6draft.interpreter.DeclarationBindingInstantiation.GlobalDeclarationInstantiation;

import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.DebugInfo;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.Source;

/**
 * 
 */
final class InterpretedScriptBody implements RuntimeInfo.ScriptBody {
    private final Script parsedScript;

    InterpretedScriptBody(Script parsedScript) {
        this.parsedScript = parsedScript;
    }

    @Override
    public Source toSource() {
        return new Source(parsedScript.getSource().getFile(), parsedScript.getSource().getName(), 1);
    }

    @Override
    public boolean isStrict() {
        return parsedScript.isStrict();
    }

    @Override
    public void globalDeclarationInstantiation(ExecutionContext cx,
            LexicalEnvironment<GlobalEnvironmentRecord> globalEnv) {
        GlobalDeclarationInstantiation(cx, parsedScript, globalEnv);
    }

    @Override
    public void evalDeclarationInstantiation(ExecutionContext cx, LexicalEnvironment<?> varEnv,
            LexicalEnvironment<?> lexEnv) {
        EvalDeclarationInstantiation(cx, parsedScript, varEnv, lexEnv);
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
