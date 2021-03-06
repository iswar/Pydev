/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.callbacks.ICallbackListener;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.parser.IParserObserver;
import org.python.pydev.core.parser.ISimpleNode;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.analysis.AnalysisRequestsTestWorkbench;
import com.python.pydev.analysis.builder.AnalysisParserObserver;
import com.python.pydev.analysis.builder.AnalysisRunner;

public class TddTestWorkbench extends AbstractWorkbenchTestCase implements IParserObserver {

    public static Test suite() {
        TestSuite suite = new TestSuite(TddTestWorkbench.class.getName());
        
        suite.addTestSuite(TddTestWorkbench.class); 
        
        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }
    
    private PyParser parser;
    private int parserNotified;
    
    public void testCheckTddQuickFixes() throws Exception {
        //We have to wait a bit until the info is setup for the tests to work...
        waitForModulesManagerSetup();
        
        checkCreateNewModule4();
        
        checkCreateFieldAtClass5();
        
        checkCreateConstant();
        
        checkCreateFieldAtClass4();
        
        checkCreateMethod2();
        
        checkCreateFieldAtClass3();
        
        checkCreateFieldAtClass2();
        
        checkCreateFieldAtClass();
        
        checkCreateMethodAtClass();
        
        checkCreateMethodAtClass2();
        
        checkCreateClass();

        checkCreateClassWithParams();
        
        checkCreateClassWithParams2();
        
        checkCreateClassInit();
        
        checkCreateClassInit2();
        
        checkCreateClassInit3();
        
        checkCreateClassAtOtherModule();
        
        checkCreateMethod();
        
        checkCreateMethodAtOtherModule();
        
        checkCreateBoundMethod();
        
        checkCreateMethodAtOtherModule2();
        
        checkCreateMethodAtOtherModule4();
        
        checkCreateNewModule();
        
        checkCreateNewModule2();
        
        checkCreateNewModuleWithClass();
        
        checkCreateNewModule3();
        
        checkCreateNewModuleWithClass2();
        
        checkCreateNewModuleWithClass3();
    }

    
    
    private void checkCreateBoundMethod() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "" +
        		"class Foo(object):\n" +
        		"    def m1(self):\n" +
        		"        self.bar()";
        setContentsAndWaitReparseAndError(mod1Contents, false); //no error here
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        IDocument doc = editor.getDocument();
        int offset = doc.getLength()-"r()".length();
        PySelection ps = new PySelection(doc, offset);
        assertTrue(quickFix.isValid(ps, "", editor, offset));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
        try {
            findCompletion(props, "Create bar method at Foo").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertContentsEqual("" +
                    "class Foo(object):\n" +
                    "\n"+
                    "    \n"+
                    "    def bar(self):\n" +
                    "        pass\n" +
                    "    \n"+
                    "    \n"+
                    "    def m1(self):\n" +
                    "        self.bar()"+
                    "", editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    private void checkCreateMethod2() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "" +
        		"print i\n" + //just to have an error
        		"class Foo(object):\n" +
        		"\n" +
        		"\n" +
        		"    def m1(self):\n" +
        		"        self.m2()" +
        		"" +
        		"";
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        int offset = mod1Contents.length();
        PySelection ps = new PySelection(editor.getDocument(), offset);
        assertTrue(quickFix.isValid(ps, "", editor, offset));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
        try {
            findCompletion(props, "Create m2 method at Foo").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertContentsEqual("" +
                    "print i\n" + //just to have an error
                    "class Foo(object):\n" +
                    "\n" +
                    "\n" +
                    "    def m2(self):\n" +
                    "        pass\n" +
                    "    \n" +
                    "    \n" +
                    "    def m1(self):\n" +
                    "        self.m2()" +
                    "" +
                    "", editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    private void checkCreateMethod() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "Foo";
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        PySelection ps = new PySelection(editor.getDocument(), 0);
        assertTrue(quickFix.isValid(ps, "", editor, 0));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, 0);
        try {
            findCompletion(props, "Create Foo method").apply(editor.getISourceViewer(), '\n', 0, 0);
            assertContentsEqual("" +
                    "def Foo():\n" +
                    "    pass\n" +
                    "\n" +
                    "\n" +
                    "Foo" +
                    "", editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }

    private void checkCreateMethodAtOtherModule() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getFile(new Path("other_module2.py"));
        mod2.create(new ByteArrayInputStream("".getBytes()), true, null);
        PyEdit editor2 = (PyEdit) PyOpenEditor.doOpenEditor(mod2);
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "import other_module2\n" +
            "other_module2.Foo(a, b)";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length()-"o(a, b)".length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            findCompletion(props, "Create Foo method at other_module2.py").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertContentsEqual("" +
                    "def Foo(a, b):\n" +
                    "    pass\n" +
                    "\n" +
                    "\n" +
                    "", editor2.getDocument().get());
            
        } finally {
            editor2.close(false);
            mod2.delete(true, null);
        }
    }
    
    private void checkCreateMethodAtOtherModule2() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getFile(new Path("other_module3.py"));
        String str ="" +
        "class Bar(object):\n" +
        "    pass\n" +
        "";
        mod2.create(new ByteArrayInputStream(str.getBytes()), true, null);
        PyEdit editor2 = (PyEdit) PyOpenEditor.doOpenEditor(mod2);
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "import other_module3\n" +
            "other_module3.Bar.Foo(10, 20)";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length()-"o(a, b)".length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            findCompletion(props, "Create Foo classmethod at Bar in other_module3.py").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertContentsEqual("" +
                    "class Bar(object):\n" +
                    "    \n" +
                    "    \n" +
                    "    @classmethod\n" +
                    "    def Foo(cls, param1, param2):\n" +
                    "        pass\n" +
                    "    \n" +
                    "    \n" +
                    "\n" +
                    "", editor2.getDocument().get());
            
        } finally {
            editor2.close(false);
            mod2.delete(true, null);
        }
    }
    
    
    private void checkCreateMethodAtOtherModule4() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getFile(new Path("other_module4.py"));
        String str ="";
        mod2.create(new ByteArrayInputStream(str.getBytes()), true, null);
        PyEdit editor2 = (PyEdit) PyOpenEditor.doOpenEditor(mod2);
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "from pack1.pack2.other_module4 import Foo";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            findCompletion(props, "Create Foo class at other_module4.py").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertContentsEqual("" +
                    "class Foo(object):\n" +
                    "    pass\n" +
                    "\n" +
                    "\n" +
                    "", editor2.getDocument().get());
            
        } finally {
            editor2.close(false);
            mod2.delete(true, null);
        }
    }
    
    
    private void checkCreateNewModule() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getFile(new Path("module_new.py"));
        final List<PyEdit> pyEditCreated = new ArrayList<PyEdit>(); 
        ICallbackListener<PyEdit> listener = new ICallbackListener<PyEdit>() {
            
            public Object call(PyEdit obj) {
                pyEditCreated.add(obj);
                return null;
            }
        };
        PyEdit.onPyEditCreated.registerListener(listener);
        
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "import pack1.pack2.module_new";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            assertTrue(!mod2.exists());
            findCompletion(props, "Create pack1.pack2.module_new module").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertTrue(mod2.exists());
            
            assertEquals(1, pyEditCreated.size());
            
        } finally {
            for(PyEdit edit:pyEditCreated){
                edit.close(false);
            }
            PyEdit.onPyEditCreated.unregisterListener(listener);
            mod2.delete(true, null);
        }
    }
    
    private void checkCreateNewModule4() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod3 = initFile.getParent().getFile(new Path("module_new3.py"));
        final List<PyEdit> pyEditCreated = new ArrayList<PyEdit>(); 
        ICallbackListener<PyEdit> listener = new ICallbackListener<PyEdit>() {
            
            public Object call(PyEdit obj) {
                pyEditCreated.add(obj);
                return null;
            }
        };
        PyEdit.onPyEditCreated.registerListener(listener);
        
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "from pack1.pack2 import module_new3";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            assertTrue(!mod3.exists());
            findCompletion(props, "Create module_new3 module").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertTrue(mod3.exists());
            
            assertEquals(1, pyEditCreated.size());
            
        } finally {
            for(PyEdit edit:pyEditCreated){
                edit.close(false);
            }
            PyEdit.onPyEditCreated.unregisterListener(listener);
            mod3.delete(true, null);
        }
    }
    
    private void checkCreateNewModule2() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getFile(new Path("pack2a/module_new.py"));
        final List<PyEdit> pyEditCreated = new ArrayList<PyEdit>(); 
        ICallbackListener<PyEdit> listener = new ICallbackListener<PyEdit>() {
            
            public Object call(PyEdit obj) {
                pyEditCreated.add(obj);
                return null;
            }
        };
        PyEdit.onPyEditCreated.registerListener(listener);
        
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "import pack1.pack2.pack2a.module_new";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            assertTrue(!mod2.exists());
            findCompletion(props, "Create pack1.pack2.pack2a.module_new module").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertTrue(mod2.exists());
            
            assertEquals(1, pyEditCreated.size());
            
        } finally {
            for(PyEdit edit:pyEditCreated){
                edit.close(false);
            }
            PyEdit.onPyEditCreated.unregisterListener(listener);
            mod2.delete(true, null);
        }
    }
    
    private void checkCreateNewModule3() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getParent().getParent().getFile(new Path("newpack/module_new.py"));
        final List<PyEdit> pyEditCreated = new ArrayList<PyEdit>(); 
        ICallbackListener<PyEdit> listener = new ICallbackListener<PyEdit>() {
            
            public Object call(PyEdit obj) {
                pyEditCreated.add(obj);
                return null;
            }
        };
        PyEdit.onPyEditCreated.registerListener(listener);
        
        try {
            //Create module
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "import newpack.module_new";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            assertTrue(!mod2.exists());
            findCompletion(props, "Create newpack.module_new module").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertTrue(mod2.exists());
            
            assertEquals(1, pyEditCreated.size());
            
            PyEdit editCreated = pyEditCreated.get(0);
            
            //Create class at module
            mod1Contents = "" +
            "from newpack import module_new\n" +
            "module_new.NewClass";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            offset = mod1Contents.length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            findCompletion(props, "Create NewClass class at module_new.py").apply(editor.getISourceViewer(), '\n', 0, offset);

            String contents = editCreated.getDocument().get();
            assertContentsEqual("" +
            		"class NewClass(object):\n" +
            		"    pass\n" +
            		"\n" +
            		"\n" +
            		"", contents);
            editCreated.getSite().getPage().saveEditor(editCreated, false);
            
            //Create __init__ at class.
            mod1Contents = "" +
            "'''\n" +
            "'''\n" +
            "" +
            "def bar():\n" +
            "    from newpack import module_new\n" +
            "    module_new.NewClass(param)"; //the 'undefined param' will be the error
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            offset = mod1Contents.length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            findCompletion(props, "Create NewClass __init__ (newpack.module_new)").apply(editor.getISourceViewer(), '\n', 0, offset);

            contents = editCreated.getDocument().get();
            assertContentsEqual("" +
                    "class NewClass(object):\n" +
                    "    \n" +
                    "    \n" +
                    "    def __init__(self, param):\n" +
                    "        pass\n" +
                    "    \n" +
                    "    \n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "", contents);
            
            
            
            
        } finally {
            for(PyEdit edit:pyEditCreated){
                edit.close(false);
            }
            PyEdit.onPyEditCreated.unregisterListener(listener);
            mod2.delete(true, null);
        }
    }
    
    private void checkCreateNewModuleWithClass2() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getParent().getParent().getFile(new Path("newpack2/module_new.py"));
        final List<PyEdit> pyEditCreated = new ArrayList<PyEdit>(); 
        ICallbackListener<PyEdit> listener = new ICallbackListener<PyEdit>() {
            
            public Object call(PyEdit obj) {
                pyEditCreated.add(obj);
                return null;
            }
        };
        PyEdit.onPyEditCreated.registerListener(listener);
        
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "from newpack2.module_new import Foo";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            assertTrue(!mod2.exists());
            findCompletion(props, "Create Foo class at new module newpack2.module_new").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertTrue("Expected: "+mod2+" to exist.", mod2.exists());
            
            assertEquals(1, pyEditCreated.size());
            
        } finally {
            for(PyEdit edit:pyEditCreated){
                edit.close(false);
            }
            PyEdit.onPyEditCreated.unregisterListener(listener);
            mod2.delete(true, null);
        }
    }
    
    private void checkCreateNewModuleWithClass3() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getParent().getParent().getFile(new Path("newpack2/module_new9.py"));
        final List<PyEdit> pyEditCreated = new ArrayList<PyEdit>(); 
        ICallbackListener<PyEdit> listener = new ICallbackListener<PyEdit>() {
            
            public Object call(PyEdit obj) {
                pyEditCreated.add(obj);
                return null;
            }
        };
        PyEdit.onPyEditCreated.registerListener(listener);
        
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "class Foo:\n    from newpack2.module_new9 import Foo";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            assertTrue(!mod2.exists());
            findCompletion(props, "Create Foo class at new module newpack2.module_new9").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertTrue("Expected: "+mod2+" to exist.", mod2.exists());
            
            assertEquals(1, pyEditCreated.size());
            
        } finally {
            for(PyEdit edit:pyEditCreated){
                edit.close(false);
            }
            PyEdit.onPyEditCreated.unregisterListener(listener);
            mod2.delete(true, null);
        }
    }
    
    private void checkCreateNewModuleWithClass() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getFolder(new Path("pack3")).getFile(new Path("module_new2.py"));
        final List<PyEdit> pyEditCreated = new ArrayList<PyEdit>(); 
        ICallbackListener<PyEdit> listener = new ICallbackListener<PyEdit>() {
            
            public Object call(PyEdit obj) {
                pyEditCreated.add(obj);
                return null;
            }
        };
        PyEdit.onPyEditCreated.registerListener(listener);
        
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "from pack1.pack2.pack3.module_new2 import Foo";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            assertTrue(!mod2.exists());
            findCompletion(props, "Create Foo class at new module pack1.pack2.pack3.module_new2").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertTrue(mod2.exists());
            
            assertEquals(1, pyEditCreated.size());
            
            assertContentsEqual("" +
                    "class Foo(object):\n" +
                    "    pass\n" +
                    "\n" +
                    "\n" +
                    "", pyEditCreated.get(0).getDocument().get());

        } finally {
            for(PyEdit edit:pyEditCreated){
                edit.close(false);
            }
            PyEdit.onPyEditCreated.unregisterListener(listener);
            mod2.delete(true, null);
        }
    }
    
    
    private void checkCreateClass() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "Foo";
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        PySelection ps = new PySelection(editor.getDocument(), 0);
        assertTrue(quickFix.isValid(ps, "", editor, 0));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, 0);
        try {
            findCompletion(props, "Create Foo class").apply(editor.getISourceViewer(), '\n', 0, 0);
            assertContentsEqual("" +
                    "class Foo(object):\n" +
                    "    pass\n" +
                    "\n" +
                    "\n" +
                    "Foo" +
                    "", editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    private void checkCreateMethodAtClass() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "" +
        		"print i\n" + //just to have error on reparse.
        		"class Foo(object):\n" +
        		"    'doc'\n" +
        		"\n" +
        		"foo = Foo()\n" +
        		"foo.Met1()";
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        int offset = mod1Contents.length()-1;
        PySelection ps = new PySelection(editor.getDocument(), offset);
        assertTrue(quickFix.isValid(ps, "", editor, offset));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
        try {
            findCompletion(props, "Create Met1 method at Foo (pack1.pack2.mod1)").apply(editor.getISourceViewer(), '\n', 0, offset);
            String expected = "" +
            "print i\n" + 
            "class Foo(object):\n" +
            "    'doc'\n" +
            "\n" +
            "    \n" +
            "    def Met1(self):\n" +
            "        pass\n" +
            "    \n" +
            "    \n" +
            "\n" +
            "foo = Foo()\n" +
            "foo.Met1()";
            assertContentsEqual(expected, editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    private void checkCreateFieldAtClass() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "" +
        "print i\n" + //just to have error on reparse.
        "class Foo(object):\n" +
        "    'doc'\n" +
        "    def existing(self):\n" +
        "        pass\n" +
        "foo = Foo()\n" +
        "foo.new_field";
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        int offset = mod1Contents.length()-1;
        PySelection ps = new PySelection(editor.getDocument(), offset);
        assertTrue(quickFix.isValid(ps, "", editor, offset));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
        try {
            findCompletion(props, "Create new_field field at Foo (pack1.pack2.mod1)").apply(editor.getISourceViewer(), '\n', 0, offset);
            String expected = "" +
            "print i\n" + 
            "class Foo(object):\n" +
            "    'doc'\n" +
            "\n" +
            "    \n" +
            "    def __init__(self):\n" +
            "        self.new_field = None\n" +
            "    \n" +
            "    \n" +
            "    def existing(self):\n" +
            "        pass\n" +
            "foo = Foo()\n" +
            "foo.new_field";
            assertContentsEqual(expected, editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    
    private void checkCreateFieldAtClass5() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "" +
        "print i\n" +
        "class Foo(object):\n" +
        "    'doc'\n" +
        "    def bar(self):\n" +
        "        self.a = 10";
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        int offset = mod1Contents.length()-1;
        PySelection ps = new PySelection(editor.getDocument(), offset);
        assertTrue(quickFix.isValid(ps, "", editor, offset));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
        try {
            findCompletion(props, "Create a field at Foo").apply(editor.getISourceViewer(), '\n', 0, offset);
            String expected = "" +
            "print i\n" +
            "class Foo(object):\n" +
            "    'doc'\n" +
            "\n"+
            "    \n"+
            "    def __init__(self):\n" +
            "        self.a = None\n" +
            "    \n"+
            "    \n"+
            "    def bar(self):\n" +
            "        self.a = 10";
            assertContentsEqual(expected, editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    private void checkCreateFieldAtClass3() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "" +
        "print i\n" +
        "class Foo(object):\n" +
        "    'doc'\n" +
        "    def __init__(self):\n" +
        "        self.a = 10\n" +
        "\n" +
        "foo = Foo()\n" +
        "foo.new_field";
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        int offset = mod1Contents.length()-1;
        PySelection ps = new PySelection(editor.getDocument(), offset);
        assertTrue(quickFix.isValid(ps, "", editor, offset));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
        try {
            findCompletion(props, "Create new_field field at Foo (pack1.pack2.mod1)").apply(editor.getISourceViewer(), '\n', 0, offset);
            String expected = "" +
            "print i\n" +
            "class Foo(object):\n" +
            "    'doc'\n" +
            "    def __init__(self):\n" +
            "        self.a = 10\n" +
            "        self.new_field = None\n" +
            "\n" +
            "foo = Foo()\n" +
            "foo.new_field";
            assertContentsEqual(expected, editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    private void checkCreateFieldAtClass4() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "" +
        "print i\n" +
        "class Foo(object):\n" +
        "    def bar(self):\n" +
        "        self.new_field\n" +
        "    def __init__(self):\n" +
        "        self.a = 10" +
        "";
        
        String secondPart = "ld\n" +
        "    def __init__(self):\n" +
        "        self.a = 10" +
        "";
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        int offset = mod1Contents.length()-secondPart.length();
        PySelection ps = new PySelection(editor.getDocument(), offset);
        assertTrue(quickFix.isValid(ps, "", editor, offset));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
        try {
            findCompletion(props, "Create new_field field at Foo").apply(editor.getISourceViewer(), '\n', 0, offset);
            String expected = "" +
            "print i\n" +
            "class Foo(object):\n" +
            "    def bar(self):\n" +
            "        self.new_field\n" +
            "    def __init__(self):\n" +
            "        self.a = 10\n" +
            "        self.new_field = None" +
            "";
            assertContentsEqual(expected, editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    
    private void checkCreateConstant() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "" +
        "print i\n" +
        "class Foo(object):\n" +
        "    def bar(self):\n" +
        "        self.BAR" +
        "";
        
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        int offset = mod1Contents.length();
        PySelection ps = new PySelection(editor.getDocument(), offset);
        assertTrue(quickFix.isValid(ps, "", editor, offset));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
        try {
            findCompletion(props, "Create BAR constant at Foo").apply(editor.getISourceViewer(), '\n', 0, offset);
            String expected = "" +
            "print i\n" +
            "class Foo(object):\n" +
            "\n" +
            "    BAR = None\n" +
            "    def bar(self):\n" +
            "        self.BAR" +
            "";
            assertContentsEqual(expected, editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    
    private void checkCreateFieldAtClass2() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "" +
        "print i\n" +
        "class Foo(object):\n" +
        "    'doc'\n" +
        "    def __init__(self):\n" +
        "        pass\n" +
        "\n" +
        "foo = Foo()\n" +
        "foo.new_field";
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        int offset = mod1Contents.length()-1;
        PySelection ps = new PySelection(editor.getDocument(), offset);
        assertTrue(quickFix.isValid(ps, "", editor, offset));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
        try {
            findCompletion(props, "Create new_field field at Foo (pack1.pack2.mod1)").apply(editor.getISourceViewer(), '\n', 0, offset);
            String expected = "" +
            "print i\n" +
            "class Foo(object):\n" +
            "    'doc'\n" +
            "    def __init__(self):\n" +
            "        self.new_field = None\n" + //note we changed the pass for the field!
            "\n" +
            "foo = Foo()\n" +
            "foo.new_field";
            assertContentsEqual(expected, editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    
    private void checkCreateMethodAtClass2() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "" +
        "print i\n" + //just to have error on reparse.
        "class Foo(object):\n" +
        "    'doc'\n" +
        "\n" +
        "foo = Foo()\n" +
        "foo.Met1(param1=10)";
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        int offset = mod1Contents.length()-1;
        PySelection ps = new PySelection(editor.getDocument(), offset);
        assertTrue(quickFix.isValid(ps, "", editor, offset));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
        try {
            findCompletion(props, "Create Met1 method at Foo (pack1.pack2.mod1)").apply(editor.getISourceViewer(), '\n', 0, offset);
            String expected = "" +
            "print i\n" + 
            "class Foo(object):\n" +
            "    'doc'\n" +
            "\n" +
            "    \n" +
            "    def Met1(self, param1):\n" +
            "        pass\n" +
            "    \n" +
            "    \n" +
            "\n" +
            "foo = Foo()\n" +
            "foo.Met1(param1=10)";
            assertContentsEqual(expected, editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    private void checkCreateClassWithParams() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        mod1Contents = "Foo(call1(ueo), 'aa,bb', 10, cc)";
        setContentsAndWaitReparseAndError(mod1Contents);
        quickFix = new TddCodeGenerationQuickFixParticipant();
        ps = new PySelection(editor.getDocument(), 0);
        assertTrue(quickFix.isValid(ps, "", editor, 0));
        props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, 0);
        try {
            findCompletion(props, "Create Foo class").apply(editor.getISourceViewer(), '\n', 0, 0);
            assertContentsEqual(
                    "" + "class Foo(object):\n" + 
                    "    \n" + 
                    "    def __init__(self, call_1, param1, param2, cc):\n"+ 
                    "        pass\n" + 
                    "\n" + 
                    "\n" + 
                    "Foo(call1(ueo), 'aa,bb', 10, cc)" + 
                    "", editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }

    
    private void checkCreateClassWithParams2() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        mod1Contents = "Foo(a=10, b=20)";
        setContentsAndWaitReparseAndError(mod1Contents);
        quickFix = new TddCodeGenerationQuickFixParticipant();
        ps = new PySelection(editor.getDocument(), 0);
        assertTrue(quickFix.isValid(ps, "", editor, 0));
        props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, 0);
        try {
            findCompletion(props, "Create Foo class").apply(editor.getISourceViewer(), '\n', 0, 0);
            assertContentsEqual(
                    "" + 
                    "class Foo(object):\n" + 
                    "    \n" + 
                    "    def __init__(self, a, b):\n"+ 
                    "        pass\n" + 
                    "\n" + 
                    "\n" + 
                    "Foo(a=10, b=20)" + 
                    "", editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    
    private void checkCreateClassInit() throws CoreException, BadLocationException, MisconfigurationException {
        baseCheckCreateClassInit("o(a=10, b=20".length());
    }
    
    private void checkCreateClassInit2() throws CoreException, BadLocationException, MisconfigurationException {
        baseCheckCreateClassInit(0);
    }


    
    private void checkCreateClassInit3() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        mod1Contents = "" +
        "print i\n" + //this is just so that we have an error (which we'll wait in the reparse -- even though we won't use it).
        "\n" +
        "class Foo:\n" +
        "    'comment'\n" +
        "    def bar(self):\n" +
        "        pass\n" +
        "Foo(a=10, b=20)";
        setContentsAndWaitReparseAndError(mod1Contents);
        quickFix = new TddCodeGenerationQuickFixParticipant();
        int offset = mod1Contents.length();
        ps = new PySelection(editor.getDocument(), offset);
        assertTrue(quickFix.isValid(ps, "", editor, offset));
        props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
        try {
            findCompletion(props, "Create Foo __init__ (pack1.pack2.mod1)").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertContentsEqual("" +
                    "print i\n" +
                    "\n" +
                    "class Foo:\n" +
                    "    'comment'\n" +
                    "\n" +
                    "    \n" +
                    "    def __init__(self, a, b):\n" +
                    "        pass\n" +
                    "    \n" +
                    "    \n" +
                    "    def bar(self):\n" +
                    "        pass\n" +
                    "Foo(a=10, b=20)" +
                    "", editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    

    private void baseCheckCreateClassInit(int minusOffset) throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        mod1Contents = "" +
        		"print i\n" + //this is just so that we have an error (which we'll wait in the reparse -- even though we won't use it).
        		"\n" +
        		"class Foo:\n" +
        		"    'comment'\n" +
        		"\n" +
        		"Foo(a=10, b=20)";
        setContentsAndWaitReparseAndError(mod1Contents);
        quickFix = new TddCodeGenerationQuickFixParticipant();
        int offset = mod1Contents.length()-minusOffset;
        ps = new PySelection(editor.getDocument(), offset);
        assertTrue(quickFix.isValid(ps, "", editor, offset));
        props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
        try {
            findCompletion(props, "Create Foo __init__ (pack1.pack2.mod1)").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertContentsEqual("" +
                    "print i\n" +
                    "\n" +
                    "class Foo:\n" +
                    "    'comment'\n" +
                    "\n" +
                    "    \n" +
                    "    def __init__(self, a, b):\n" +
                    "        pass\n" +
                    "    \n" +
                    "    \n" +
                    "\n" +
                    "Foo(a=10, b=20)" +
            		"", editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    

    
    private void checkCreateClassAtOtherModule() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getFile(new Path("other_module.py"));
        mod2.create(new ByteArrayInputStream("".getBytes()), true, null);
        PyEdit editor2 = (PyEdit) PyOpenEditor.doOpenEditor(mod2);
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "import other_module\n" +
            "other_module.Foo";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length()-1;
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            findCompletion(props, "Create Foo class at other_module.py").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertContentsEqual("" +
                    "class Foo(object):\n" +
                    "    pass\n" +
                    "\n" +
                    "\n" +
                    "", editor2.getDocument().get());
            
        } finally {
            editor2.close(false);
            mod2.delete(true, null);
        }
    }

    private ICompletionProposalExtension2 findCompletion(List<ICompletionProposal> props, String expectedCompletion) {
        FastStringBuffer buf = new FastStringBuffer("Available: ",300);
        for (ICompletionProposal iCompletionProposal : props) {
            if(iCompletionProposal.getDisplayString().equals(expectedCompletion)){
                ICompletionProposalExtension2 p = (ICompletionProposalExtension2) iCompletionProposal;
                return p;
            }
            buf.append("\n");
            buf.append(iCompletionProposal.getDisplayString());
        }
        throw new AssertionError("Could not find completion: "+expectedCompletion+"\n"+buf);
    }
    

    private void setContentsAndWaitReparseAndError(String mod1Contents) throws CoreException {
        setContentsAndWaitReparseAndError(mod1Contents, true);
    }
    
    private void setContentsAndWaitReparseAndError(String mod1Contents, boolean waitForError) throws CoreException {
        setFileContents(mod1Contents);
        
        parser = editor.getParser();
        parser.addParseListener(this);
        
        ICallback<Boolean, Object> parseHappenedCondition = getParseHappenedCondition();
        
        parser.forceReparse(new Tuple<String, Boolean>(
                AnalysisParserObserver.ANALYSIS_PARSER_OBSERVER_FORCE, true));
        goToIdleLoopUntilCondition(parseHappenedCondition);

        if(waitForError){
            goToIdleLoopUntilCondition(getHasBothErrorMarkersCondition(editor.getIFile()));
        }
        goToManual(500);
    }
    

    private void assertContentsEqual(String expected, String generated) {
        assertEquals(StringUtils.replaceNewLines(expected, "\n"), StringUtils.replaceNewLines(generated, "\n"));
    }


    public void parserChanged(ISimpleNode root, IAdaptable file, IDocument doc) {
        parser.removeParseListener(this);
        this.parserNotified += 1;
    }

    public void parserError(Throwable error, IAdaptable file, IDocument doc) {
        parser.removeParseListener(this);
        this.parserNotified += 1;
    }
    
    
    private ICallback<Boolean, Object> getParseHappenedCondition() {
        final int currentNotified = this.parserNotified;
        return new ICallback<Boolean, Object>(){
            
            public Boolean call(Object arg) {
                return parserNotified > currentNotified;
            }
        };
    }
    
    /**
     * Callback that'll check if there are error markers in the mod1.py resource
     */
    private ICallback<Boolean, Object> getHasBothErrorMarkersCondition(final IFile file) {
        return new ICallback<Boolean, Object>(){
            
            public Boolean call(Object arg) {
                try {
                    //must have both problems: syntax and analysis error!!
                    IMarker[] markers = file.findMarkers(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
                    return markers.length > 0;
                } catch (CoreException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

}
