/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.elements;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Note that the python file here does not actually mean a .py or .pyw file (it can be
 * any file, such as .txt, .gif, etc)
 * 
 * @author fabioz
 */
public class PythonFile extends WrappedResource<IFile>{
    
    public PythonFile(IWrappedResource parentElement, IFile actualObject, PythonSourceFolder pythonSourceFolder) {
        super(parentElement, actualObject, pythonSourceFolder, IWrappedResource.RANK_PYTHON_FILE);
        //System.out.println("Created PythonFile:"+this+" - "+actualObject+" parent:"+parentElement);
    }

    public InputStream getContents() throws CoreException {
        try{
            return this.actualObject.getContents();
        }catch (CoreException e) {
            //out of sync
            this.actualObject.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
            return this.actualObject.getContents();
        }
    }
}
