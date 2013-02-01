/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;
import java.util.Set;

/**
 *
 */
public interface Scope {
    Scope getParent();

    boolean isTopLevel();

    Set<String> varDeclaredNames();

    Set<String> lexicallyDeclaredNames();

    List<StatementListItem> varScopedDeclarations();

    List<Declaration> lexicallyScopedDeclarations();
}
