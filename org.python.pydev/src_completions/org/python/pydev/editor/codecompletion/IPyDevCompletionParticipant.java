/*
 * Created on 21/08/2005
 */
package org.python.pydev.editor.codecompletion;

import java.util.Collection;

import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IToken;

public interface IPyDevCompletionParticipant {

    /**
     * PyDev can have code completion participants, that may return a list of:
     * ICompletionProposal
     * Itoken (will be automatically converted to completion proposals)
     * 
     * @param request the request that was done for the completion
     * @param state the state for the completion
     * 
     * @return a list of proposals or tokens
     * 
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal
     * @see org.python.pydev.core.IToken
     */
    Collection getGlobalCompletions(CompletionRequest request, ICompletionState state);
    
    Collection getStringGlobalCompletions(CompletionRequest request, ICompletionState state);

    Collection getArgsCompletion(ICompletionState state, ILocalScope localScope, Collection<IToken> interfaceForLocal);

}