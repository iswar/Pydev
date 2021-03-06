/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.cache.LRUCache;

/**
 * Default completion cache implementation
 *
 * @author Fabio
 */
public class CompletionCache extends LRUCache<Object, Object> implements ICompletionCache {

    public CompletionCache() {
        super(200);
    }

}
