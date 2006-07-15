/*
 * Created on Jul 1, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.uiutils.RunInUiThread;
import org.python.pydev.plugin.PydevPlugin;

public class PyLinkedModeCompletionProposal extends PyCompletionProposal implements ICompletionProposalExtension2{

    private int firstParameterLen = 0;
    private PyCompletionPresentationUpdater presentationUpdater;
    private int fLen;
    private boolean fLastIsPar;
    
    public PyLinkedModeCompletionProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo, int priority, int onApplyAction, String args) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority, onApplyAction, args);
        presentationUpdater = new PyCompletionPresentationUpdater(this);
    }
    
    /*
     * @see ICompletionProposal#getSelection(IDocument)
     */
    public Point getSelection(IDocument document) {
        if(onApplyAction == ON_APPLY_JUST_SHOW_CTX_INFO){
            return null;
        }
        if(onApplyAction == ON_APPLY_DEFAUL ){
            return new Point(fReplacementOffset + fCursorPosition, firstParameterLen); //the difference is the firstParameterLen here (instead of 0)
        }
        if(onApplyAction == ON_APPLY_SHOW_CTX_INFO_AND_ADD_PARAMETETRS ){
            return new Point(fReplacementOffset + fCursorPosition-1, firstParameterLen); //the difference is the firstParameterLen here (instead of 0)
        }
        throw new RuntimeException("Unexpected apply mode:"+onApplyAction);
    }


    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        boolean eat = (stateMask & SWT.MOD1) != 0;
        
        IDocument doc = viewer.getDocument();
        if(onApplyAction == ON_APPLY_JUST_SHOW_CTX_INFO){
            return;
        }
        if(onApplyAction == ON_APPLY_SHOW_CTX_INFO_AND_ADD_PARAMETETRS){
            try {
                String args = fArgs.substring(0, fArgs.length()-1); //remove the parentesis
                char c = doc.getChar(offset);
                boolean addedPar = false;
                if(c != ')'){
                    addedPar = true;
                    args += ')';
                }
                doc.replace(fReplacementOffset+2, fReplacementLength-2, args);
                int iPar = -1;
                int exitPos = offset + args.length();
                if(addedPar){
                    exitPos -= 1;
                }
                goToLinkedModeFromArgs(viewer, offset-1, doc, exitPos, iPar, args);

            }catch(BadLocationException e){
                PydevPlugin.log(e);
            }
            return;
        }
        
        
        if(onApplyAction == ON_APPLY_DEFAUL){
            int dif = offset - fReplacementOffset;
            try {
                String strToAdd = fReplacementString.substring(dif);
                
                if(eat){
                    String rep = fReplacementString;
                    int i = rep.indexOf('(');
                    if(fLastIsPar && i != -1){
                        rep = rep.substring(0, i);
                        doc.replace(offset-dif, dif+this.fLen, rep);
                        //if the last was a parenthesis, there's nothing to link, so, let's return
                        return;
                    }else{
                        doc.replace(offset-dif, dif+this.fLen, rep);
                    }
                }else{
                    doc.replace(offset-dif, dif, fReplacementString);
                }
                
                //ok, now, on to the linking part
                int iPar = strToAdd.indexOf('(');
                if(iPar != -1 && strToAdd.charAt(strToAdd.length()-1) == ')'){
                    String newStr = strToAdd.substring(iPar+1, strToAdd.length()-1);
                    goToLinkedModeFromArgs(viewer, offset, doc, offset + strToAdd.length(), iPar, newStr);
                }
            } catch (BadLocationException e) {
                PydevPlugin.log(e);
            }
        }
        
        throw new RuntimeException("Unexpected apply mode:"+onApplyAction);

    }

    private void goToLinkedModeFromArgs(ITextViewer viewer, int offset, IDocument doc, int exitPos, int iPar, String newStr) throws BadLocationException {
        List<Integer> offsetsAndLens = new ArrayList<Integer>();
        
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < newStr.length(); i++) {
            char c = newStr.charAt(i);
            
            if(Character.isJavaIdentifierPart(c)){
                if(buffer.length() == 0){
                    offsetsAndLens.add(i);
                    buffer.append(c);
                }else{
                    buffer.append(c);
                }
            }else{
                if(buffer.length() > 0){
                    offsetsAndLens.add(buffer.length());
                    buffer = new StringBuffer();
                }
            }
        }
        if(buffer.length() > 0){
            offsetsAndLens.add(buffer.length());
        }
        buffer = null;
        
        goToLinkedMode(viewer, offset, doc, exitPos, iPar, offsetsAndLens);
    }

    private void goToLinkedMode(ITextViewer viewer, int offset, IDocument doc, int exitPos, int iPar, List<Integer> offsetsAndLens) throws BadLocationException {
        if(offsetsAndLens.size() > 0){
            LinkedModeModel model= new LinkedModeModel();
            
            for (int i = 0; i < offsetsAndLens.size(); i++) {
                Integer offs = offsetsAndLens.get(i);
                i++;
                Integer len = offsetsAndLens.get(i);
                if(i == 1){
                    firstParameterLen = len;
                }
                int location = offset+iPar+offs+1;
                LinkedPositionGroup group= new LinkedPositionGroup();
                ProposalPosition proposalPosition = new ProposalPosition(doc, location, len, 0 , new ICompletionProposal[0]);
                group.addPosition(proposalPosition);
                model.addGroup(group);
            }
            
            model.forceInstall();
            
            final LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
            ui.setDoContextInfo(true); //set it to request the ctx info from the completion processor
            ui.setExitPosition(viewer, exitPos, 0, Integer.MAX_VALUE);
            Runnable r = new Runnable(){
                public void run() {
                    ui.enter();
                }
            };
            RunInUiThread.async(r);

        }
    }
    
    

    /**
     * Called when Ctrl is selected during the completions
     * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(org.eclipse.jface.text.ITextViewer, boolean)
     */
    public void selected(ITextViewer viewer, boolean smartToggle) {
        if(smartToggle){
            StyledText text= viewer.getTextWidget();
            if (text == null || text.isDisposed())
                return;

            int widgetCaret= text.getCaretOffset();
            IDocument document = viewer.getDocument();
            int finalOffset = widgetCaret;
            
            try {
                if(finalOffset >= document.getLength()){
                    unselected(viewer);
                    return;
                }
                char c;
                do{
                    c = document.getChar(finalOffset);
                    finalOffset++;
                }while(isValidChar(c) && finalOffset < document.getLength());
                
                if(c == '('){
                    fLastIsPar = true;
                }else{
                    fLastIsPar = false;
                }
                
                if(!isValidChar(c)){
                    finalOffset--;
                }
                
                this.fLen = finalOffset-widgetCaret;
                this.presentationUpdater.updateStyle(viewer, widgetCaret, this.fLen);
            } catch (BadLocationException e) {
                PydevPlugin.log(e);
            }
            
        }else{
            unselected(viewer);
        }
    }

    /**
     * @param c
     * @return
     */
    private boolean isValidChar(char c) {
        return c != ' ' && c != '(' && c!= '{' && c != '[' && c != '"' && c!= '.' && c!= '\n' && c != '\n' &&
               c != ')' && c != ']' && c != '}';
    }

    public void unselected(ITextViewer viewer) {
        this.presentationUpdater.repairPresentation(viewer);
    }

    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        String[] strs = PySelection.getActivationTokenAndQual(document, offset, false); 

        String qualifier = strs[1].toLowerCase();
        
        String displayString = getDisplayString().toLowerCase();
        if(displayString.startsWith(qualifier)){
            return true;
        }
        
        return false;
    }

}