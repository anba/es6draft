/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async.iteration;

import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;

/**
 * AsyncGeneratorRequest Records
 */
public final class AsyncGeneratorRequest {
    enum CompletionType {
        Normal, Return, Throw
    }

    private final CompletionType completionType;

    /** [[Completion]] */
    private final Object completion;

    /** [[Capability]] */
    private final PromiseCapability<PromiseObject> capability;

    public AsyncGeneratorRequest(CompletionType completionType, Object completion,
            PromiseCapability<PromiseObject> capability) {
        this.completionType = completionType;
        this.completion = completion;
        this.capability = capability;
    }

    /**
     * Returns the completion type.
     * 
     * @return the completion type
     */
    public CompletionType getCompletionType() {
        return completionType;
    }

    /**
     * [[Completion]]
     * 
     * @return the completion value
     */
    public Object getCompletion() {
        return completion;
    }

    /**
     * [[Capability]]
     * 
     * @return the promise capability
     */
    public PromiseCapability<PromiseObject> getCapability() {
        return capability;
    }

    @Override
    public String toString() {
        return String.format("AsyncGeneratorRequest<type=%s, completion=%s>", completionType, completion);
    }
}
