/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 11, 2006
 */
package com.python.pydev.refactoring.refactorer;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

public class ClassHierarchySearchTest extends AdditionalInfoTestsBase  {

    public static void main(String[] args) {
        try {
            ClassHierarchySearchTest test = new ClassHierarchySearchTest();
            test.setUp();
            test.testFindHierarchy5();
            test.tearDown();
            
            junit.textui.TestRunner.run(ClassHierarchySearchTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private Refactorer refactorer;

    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(false);
        refactorer = new Refactorer();
        SourceModule.TESTING = true;
    }

    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        ProjectModulesManager projectModulesManager = ((ProjectModulesManager)nature.getAstManager().getModulesManager());
        projectModulesManager.doRemoveSingleModule(new ModulesKey("foo", null));
        projectModulesManager.doRemoveSingleModule(new ModulesKey("fooIn1", null));
        
        projectModulesManager = ((ProjectModulesManager)nature2.getAstManager().getModulesManager());
        projectModulesManager.doRemoveSingleModule(new ModulesKey("fooIn2", null));
        super.tearDown();
    }
    
    public void testFindHierarchy() {
        final int line = 1;
        final int col = 9;
        
        RefactoringRequest request = setUpFooModule(line, col);
        
        HierarchyNodeModel node = refactorer.findClassHierarchy(request);
        assertEquals("Bar", node.name);
        assertEquals("foo", node.moduleName);
        
        assertIsIn("Pickler", "pickle", node.parents);
        assertIsIn("Foo", "foo", node.children);
    }

    public void testFindHierarchy2() {
        final int line = 3;
        final int col = 9;
        
        RefactoringRequest request = setUpFooModule(line, col);
        
        HierarchyNodeModel node = refactorer.findClassHierarchy(request);
        assertEquals("Foo", node.name);
        assertEquals("foo", node.moduleName);
        
        HierarchyNodeModel model = assertIsIn("Bar", "foo", node.parents);
        assertIsIn("Pickler", "pickle", model.parents);
        
    }
    
    public void testFindHierarchy3() {
        String str ="" +
        "import pickle             \n" +
        "class Bar:\n" +
        "    pass                  \n" +
        "class Foo(Bar, pickle.Pickler):\n" +
        "    pass                  \n" +
        "\n" +
        "";
        final int line = 3;
        final int col = 9;
        
        RefactoringRequest request = setUpFooModule(line, col, str);
        
        HierarchyNodeModel node = refactorer.findClassHierarchy(request);
        assertEquals("Foo", node.name);
        assertEquals("foo", node.moduleName);
        
        assertIsIn("Bar", "foo", node.parents);
        assertIsIn("Pickler", "pickle", node.parents);
        
    }
    
    public void testFindHierarchy4() {
        String str ="" +
        "class Bar:                \n" +
        "    pass                  \n" +
        "class Foo(Bar):           \n" +
        "    pass                  \n" +
        "class Foo1(Foo):          \n" +
        "    pass                  \n" +
        "\n" +
        "";
        final int line = 0;
        final int col = 8;
        
        RefactoringRequest request = setUpFooModule(line, col, str);
        
        HierarchyNodeModel node = refactorer.findClassHierarchy(request);
        assertEquals("Bar", node.name);
        assertEquals("foo", node.moduleName);
        
        node = assertIsIn("Foo", "foo", node.children);
        assertIsIn("Foo1", "foo", node.children);
        
    }
    
    
    public void testFindHierarchy5() {
        String str ="" +
        "class Root(object):\n" +
        "    pass\n" +
        "class Mid1(Root):\n" +
        "    pass\n" +
        "class Mid2(Root):\n" +
        "    pass\n" +
        "class Leaf(Mid1, Mid2):\n" +
        "    pass\n" +
        "\n" +
        "";
        
        final int line = 6;
        final int col = 8;
        
        RefactoringRequest request = setUpFooModule(line, col, str);
        
        HierarchyNodeModel node = refactorer.findClassHierarchy(request);
        assertEquals("Leaf", node.name);
        assertEquals("foo", node.moduleName);
        
        HierarchyNodeModel mid1 = assertIsIn("Mid1", "foo", node.parents);
        HierarchyNodeModel mid2 = assertIsIn("Mid2", "foo", node.parents);
        assertIsIn("Root", "foo", mid1.parents);
        HierarchyNodeModel root = assertIsIn("Root", "foo", mid2.parents);
        assertIsIn("object", null, root.parents);
        
    }
    
    public void testFindHierarchy6() {
        String str ="" +
        "class Root(object):\n" +
        "    pass\n" +
        "class Mid1(Root):\n" +
        "    pass\n" +
        "class Mid2(Root):\n" +
        "    pass\n" +
        "class Leaf(Mid1, Mid2):\n" +
        "    pass\n" +
        "import pickle\n" +
        "class Bla(Leaf, Foo):\n" +
        "    pass\n" +
        "class Foo:\n" +
        "    pass\n" +
        "";
        
        final int line = 9;
        final int col = 8;
        
        RefactoringRequest request = setUpFooModule(line, col, str);
        
        HierarchyNodeModel node = refactorer.findClassHierarchy(request);
        assertEquals("Bla", node.name);
        assertEquals("foo", node.moduleName);
        
        HierarchyNodeModel foo = assertIsIn("Foo", "foo", node.parents);
        assertEquals(0, foo.parents.size());
        
    }
    
    public void testFindHierarchy8() {
        String str  ="class FooIn1(object):pass\n";
        String str2 ="from fooIn1 import FooIn1\nclass FooIn2(FooIn1):pass\n";
        
        final int line = 0;
        final int col = 8;
        
        RefactoringRequest request;
        request = setUpModule(line, col, str2, "fooIn2", nature2);
        request = setUpModule(line, col, str, "fooIn1", nature);
        
        HierarchyNodeModel node = refactorer.findClassHierarchy(request);
        assertEquals("FooIn1", node.name);
        assertEquals("fooIn1", node.moduleName);
        
        HierarchyNodeModel foo = assertIsIn("FooIn2", "fooIn2", node.children);
        assertEquals(0, foo.parents.size());
    }

    private RefactoringRequest setUpFooModule(final int line, final int col) {
        String str ="" +
        "import pickle\n" +
        "class Bar(pickle.Pickler):\n" +
        "    pass\n" +
        "class Foo(Bar):\n" +
        "    pass\n" +
        "\n" +
        "";
        return setUpFooModule(line, col, str);
    }
    
    private RefactoringRequest setUpFooModule(final int line, final int col, String str) {
        String modName = "foo";
        PythonNature natureToAdd = nature;
        return setUpModule(line, col, str, modName, natureToAdd);
    }

    private RefactoringRequest setUpModule(final int line, final int col, String str, String modName, PythonNature natureToAdd) {
        Document doc = new Document(str);
        PySelection ps = new PySelection(doc, line, col);
        
        RefactoringRequest request = new RefactoringRequest(null, ps, natureToAdd);
        request.moduleName = modName;
        final SimpleNode ast = request.getAST();
        
        addModuleToNature(ast, modName, natureToAdd);
        return request;
    }


    private HierarchyNodeModel assertIsIn(String name, String modName, List<HierarchyNodeModel> parents) {
        FastStringBuffer available = new FastStringBuffer();
        
        for (HierarchyNodeModel model : parents) {
            available.append(model.name).append(" - ").append(model.moduleName);
            if(model.name.equals(name)){
                if(modName == null){
                    return model;
                }else if(model.moduleName.equals(modName)){
                    return model;
                }
            }
        }
        fail("Unable to find node with name:"+name+" mod:"+modName+ "\nAvailable:"+available);
        return null;
    }
}
