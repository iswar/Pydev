/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.codecompletion.revisited.ModulesFoundStructure.ZipContents;
import org.python.pydev.editor.codecompletion.revisited.ModulesKeyTreeMap.Entry;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JythonModulesManagerUtils;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModuleForZip;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

/**
 * This class manages the modules that are available
 * 
 * @author Fabio Zadrozny
 */
public abstract class ModulesManager implements IModulesManager, Serializable {

    private final static boolean DEBUG_BUILD = false;
    
    private final static boolean DEBUG_TEMPORARY_MODULES = false;

    private final static boolean DEBUG_IO = false;
    
    private final static boolean DEBUG_ZIP = false;

    public ModulesManager() {
    }

    /**
     * This class is a cache to help in getting the managers that are referenced or referred.
     * 
     * It will not actually make any computations (the managers must be set from the outside)
     */
    protected static class CompletionCache {
        public IModulesManager[] referencedManagers;

        public IModulesManager[] referredManagers;

        public IModulesManager[] getManagers(boolean referenced) {
            if (referenced) {
                return this.referencedManagers;
            } else {
                return this.referredManagers;
            }
        }

        public void setManagers(IModulesManager[] ret, boolean referenced) {
            if (referenced) {
                this.referencedManagers = ret;
            } else {
                this.referredManagers = ret;
            }
        }
    }

    /**
     * A stack for keeping the completion cache
     */
    protected transient volatile CompletionCache completionCache = null;
    protected transient Object lockCompletionCache = new Object();

    private transient volatile int completionCacheI = 0;

    /**
     * This method starts a new cache for this manager, so that needed info is kept while the request is happening
     * (so, some info may not need to be re-asked over and over for requests) 
     */
    public boolean startCompletionCache() {
        synchronized(lockCompletionCache){
            if (completionCache == null) {
                completionCache = new CompletionCache();
            }
            completionCacheI += 1;
        }
        return true;
    }

    public void endCompletionCache() {
        synchronized(lockCompletionCache){
            completionCacheI -= 1;
            if (completionCacheI == 0) {
                completionCache = null;
            } else if (completionCacheI < 0) {
                throw new RuntimeException("Completion cache negative (request unsynched)");
            }
        }
    }

    /**
     * Modules that we have in memory. This is persisted when saved.
     * 
     * Keys are ModulesKey with the name of the module. Values are AbstractModule objects.
     * 
     * Implementation changed to contain a cache, so that it does not grow to much (some out of memo errors
     * were thrown because of the may size when having too many modules).
     * 
     * It is sorted so that we can get things in a 'subtree' faster
     */
    protected transient ModulesKeyTreeMap<ModulesKey, ModulesKey> modulesKeys = new ModulesKeyTreeMap<ModulesKey, ModulesKey>();

    protected static transient ModulesManagerCache cache = new ModulesManagerCache();

    /**
     * This is the set of files that was found just right after unpickle (it should not be changed after that,
     * and serves only as a reference cache).
     */
    protected transient Set<File> files = new HashSet<File>();

    /**
     * Helper for using the pythonpath. Also persisted.
     */
    protected PythonPathHelper pythonPathHelper = new PythonPathHelper();

    public PythonPathHelper getPythonPathHelper() {
        return pythonPathHelper;
    }

    public void setPythonPathHelper(Object pathHelper) {
        if (!(pathHelper instanceof PythonPathHelper)) {
          throw new IllegalArgumentException();
        }
        pythonPathHelper = (PythonPathHelper)pathHelper;
    }
    
    /**
     * The version for deserialization
     */
    private static final long serialVersionUID = 2L;

    /**
     * Custom deserialization is needed.
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream aStream) throws IOException, ClassNotFoundException {
        lockCompletionCache = new Object();
        completionCacheI = 0;
        modulesKeys = new ModulesKeyTreeMap<ModulesKey, ModulesKey>();
        
        temporaryModules = new HashMap<String, SortedMap<Integer, IModule>>();
        lockTemporaryModules = new Object();
        nextHandle = 0;

        files = new HashSet<File>();
        aStream.defaultReadObject();
        Set<ModulesKey> set = (Set<ModulesKey>) aStream.readObject();
        if (DEBUG_IO) {
            for (ModulesKey key : set) {
                System.out.println("Read:" + key);
            }
        }
        for (Iterator<ModulesKey> iter = set.iterator(); iter.hasNext();) {
            ModulesKey key = iter.next();
            //restore with empty modules.
            modulesKeys.put(key, key);
            if (key.file != null) {
                files.add(key.file);
            }
        }
        
        if(this.pythonPathHelper == null){
            throw new IOException("Pythonpath helper not properly restored. "+this.getClass().getName());
        }
        
        if(this.pythonPathHelper.getPythonpath() == null){
            throw new IOException("Pythonpath helper pythonpath not properly restored. "+this.getClass().getName());
        }
        
        if(this.pythonPathHelper.getPythonpath().size() == 0){
            throw new IOException("Pythonpath helper pythonpath restored with no contents. "+this.getClass().getName());
        }
        
        if(set.size() < 15){ //if we have to few modules, that may indicate a problem... 
                             //if the project is really small, this will be fast, otherwise, it'll fix the problem.
            throw new IOException("Only "+set.size()+" modules restored in I/O. "+this.getClass().getName());
        }
    }

    /**
     * Custom serialization is needed.
     */
    private void writeObject(ObjectOutputStream aStream) throws IOException {
        synchronized (modulesKeys) {
            aStream.defaultWriteObject();
            //write only the keys
            HashSet<ModulesKey> set = new HashSet<ModulesKey>(modulesKeys.keySet());
            if (DEBUG_IO) {
                for (ModulesKey key : set) {
                    System.out.println("Write:" + key);
                }
            }
            aStream.writeObject(set);
        }
    }

    /**
     * @param modules The modules to set.
     */
    private void setModules(ModulesKeyTreeMap<ModulesKey, ModulesKey> keys) {
        this.modulesKeys = keys;
    }

    /**
     * @return Returns the modules.
     */
    protected Map<ModulesKey, AbstractModule> getModules() {
        throw new RuntimeException("Deprecated");
    }


    /**
     * Change the pythonpath (used for both: system and project)
     * 
     * @param project: may be null
     * @param defaultSelectedInterpreter: may be null
     */
    public void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor) {
        pythonPathHelper.setPythonPath(pythonpath);
        ModulesFoundStructure modulesFound = pythonPathHelper.getModulesFoundStructure(monitor);

        //now, on to actually filling the module keys
        ModulesKeyTreeMap<ModulesKey, ModulesKey> keys = new ModulesKeyTreeMap<ModulesKey, ModulesKey>();
        int j = 0;

        FastStringBuffer buffer = new FastStringBuffer();
        //now, create in memory modules for all the loaded files (empty modules).
        for (Iterator<Map.Entry<File, String>> iterator = modulesFound.regularModules.entrySet().iterator(); iterator.hasNext()
                && monitor.isCanceled() == false; j++) {
            Map.Entry<File, String> entry = iterator.next();
            File f = entry.getKey();
            String m = entry.getValue();

            if (j % 15 == 0) {
                //no need to report all the time (that's pretty fast now)
                buffer.clear();
                monitor.setTaskName(buffer.append("Module resolved: ").append(m).toString());
                monitor.worked(1);
            }

            if (m != null) {
                //we don't load them at this time.
                ModulesKey modulesKey = new ModulesKey(m, f);

                //no conflict (easy)
                if (!keys.containsKey(modulesKey)) {
                    keys.put(modulesKey, modulesKey);
                } else {
                    //we have a conflict, so, let's resolve which one to keep (the old one or this one)
                    if (PythonPathHelper.isValidSourceFile(f.getName())) {
                        //source files have priority over other modules (dlls) -- if both are source, there is no real way to resolve
                        //this priority, so, let's just add it over.
                        keys.put(modulesKey, modulesKey);
                    }
                }
            }
        }
        
        for (ZipContents zipContents : modulesFound.zipContents) {
            if (monitor.isCanceled()) {
                break;
            }
            for (String filePathInZip : zipContents.foundFileZipPaths) {
                String modName = StringUtils.stripExtension(filePathInZip).replace('/', '.');
                if(DEBUG_ZIP){
                    System.out.println("Found in zip:"+modName);
                }
                ModulesKey k = new ModulesKeyForZip(modName, zipContents.zipFile, filePathInZip, true);
                keys.put(k, k);
                
                if(zipContents.zipContentsType == ZipContents.ZIP_CONTENTS_TYPE_JAR){
                    //folder modules are only created for jars (because for python files, the __init__.py is required).
                    for(String s:new FullRepIterable(FullRepIterable.getWithoutLastPart(modName))){ //the one without the last part was already added
                        k = new ModulesKeyForZip(s, zipContents.zipFile, s.replace('.', '/'), false);
                        keys.put(k, k);
                    }
                }
            }
        }

        onChangePythonpath(keys);

        //assign to instance variable
        this.setModules(keys);

    }

    /**
     * Subclasses may do more things after the defaults were added to the cache (e.g.: the system modules manager may
     * add builtins)
     */
    protected void onChangePythonpath(SortedMap<ModulesKey, ModulesKey> keys) {
    }

    /**
     * This is the only method that should remove a module.
     * No other method should remove them directly.
     * 
     * @param key this is the key that should be removed
     */
    protected void doRemoveSingleModule(ModulesKey key) {
        synchronized (modulesKeys) {
            if (DEBUG_BUILD) {
                System.out.println("Removing module:" + key + " - " + this.getClass());
            }
            this.modulesKeys.remove(key);
            ModulesManager.cache.remove(key, this);
        }
    }

    /**
     * This method that actually removes some keys from the modules. 
     * 
     * @param toRem the modules to be removed
     */
    protected void removeThem(Collection<ModulesKey> toRem) {
        //really remove them here.
        for (Iterator<ModulesKey> iter = toRem.iterator(); iter.hasNext();) {
            doRemoveSingleModule(iter.next());
        }
    }

    public void removeModules(Collection<ModulesKey> toRem) {
        removeThem(toRem);
    }

    public IModule addModule(final ModulesKey key) {
        AbstractModule ret = AbstractModule.createEmptyModule(key);
        doAddSingleModule(key, ret);
        return ret;
    }

    /**
     * This is the only method that should add / update a module.
     * No other method should add it directly (unless it is loading or rebuilding it).
     * 
     * @param key this is the key that should be added
     * @param n 
     */
    public void doAddSingleModule(final ModulesKey key, AbstractModule n) {
        if (DEBUG_BUILD) {
            System.out.println("Adding module:" + key + " - " + this.getClass());
        }
        synchronized (modulesKeys) {
            this.modulesKeys.put(key, key);
            ModulesManager.cache.add(key, n, this);
        }
    }

    /**
     * @return a set of all module keys
     * 
     * Note: addDependencies ignored at this point.
     */
    public Set<String> getAllModuleNames(boolean addDependencies, String partStartingWithLowerCase) {
        Set<String> s = new HashSet<String>();
        synchronized (modulesKeys) {
            for (ModulesKey key : this.modulesKeys.keySet()) {
                if(key.hasPartStartingWith(partStartingWithLowerCase)){
                    s.add(key.name);
                }
            }
        }
        return s;
    }

    public SortedMap<ModulesKey, ModulesKey> getAllDirectModulesStartingWith(String strStartingWith) {
        if (strStartingWith.length() == 0) {
            synchronized (modulesKeys) {
                //we don't want it to be backed up by the same set (because it may be changed, so, we may get
                //a java.util.ConcurrentModificationException on places that use it)
                return new ModulesKeyTreeMap<ModulesKey, ModulesKey>(modulesKeys);
            }
        }
        ModulesKey startingWith = new ModulesKey(strStartingWith, null);
        ModulesKey endingWith = new ModulesKey(startingWith + "z", null);
        synchronized (modulesKeys) {
            //we don't want it to be backed up by the same set (because it may be changed, so, we may get
            //a java.util.ConcurrentModificationException on places that use it)
            return new ModulesKeyTreeMap<ModulesKey, ModulesKey>(modulesKeys.subMap(startingWith, endingWith));
        }
    }

    public SortedMap<ModulesKey, ModulesKey> getAllModulesStartingWith(String strStartingWith) {
        return getAllDirectModulesStartingWith(strStartingWith);
    }

    public ModulesKey[] getOnlyDirectModules() {
        synchronized (modulesKeys) {
            return (ModulesKey[]) this.modulesKeys.keySet().toArray(new ModulesKey[0]);
        }
    }

    /**
     * Note: no dependencies at this point (so, just return the keys)
     */
    public int getSize(boolean addDependenciesSize) {
        return this.modulesKeys.size();
    }

    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit) {
        return getModule(true, name, nature, dontSearchInit);
    }
    
    /**
     * Note that the access must be synched.
     */
    public transient Map<String, SortedMap<Integer, IModule>> temporaryModules = new HashMap<String, SortedMap<Integer, IModule>>();
    private transient Object lockTemporaryModules = new Object();
    private transient int nextHandle = 0;
    
    /**
     * Returns the handle to be used to remove the module added later on!
     */
    public int pushTemporaryModule(String moduleName, IModule module) {
    	synchronized (lockTemporaryModules) {
    		SortedMap<Integer, IModule> map = temporaryModules.get(moduleName);
    		if(map == null){
    			map = new TreeMap<Integer, IModule>(); //small initial size!
    			temporaryModules.put(moduleName, map);
    		}
    		nextHandle += 1; //Note: don't care about stack overflow!
    		map.put(nextHandle, module);
    		return nextHandle;
		}
    	
    }
    
    public void popTemporaryModule(String moduleName, int handle) {
    	synchronized (lockTemporaryModules) {
    		SortedMap<Integer, IModule> stack = temporaryModules.get(moduleName);
    		try {
    		    if(stack != null){
    				stack.remove(handle);
    				if(stack.size() == 0){
    					temporaryModules.remove(moduleName);
    				}
    		    }
			} catch (Throwable e) {
				Log.log(e);
			}
    	}
    }

    /**
     * This method returns the module that corresponds to the path passed as a parameter.
     * 
     * @param name the name of the module we're looking for  (e.g.: mod1.mod2)
     * @param dontSearchInit is used in a negative form because initially it was isLookingForRelative, but
     * it actually defines if we should look in __init__ modules too, so, the name matches the old signature.
     * 
     * NOTE: isLookingForRelative description was: when looking for relative imports, we don't check for __init__
     * @return the module represented by this name
     */
    protected IModule getModule(boolean acceptCompiledModule, String name, IPythonNature nature, boolean dontSearchInit) {
		synchronized (lockTemporaryModules) {
		    SortedMap<Integer, IModule> map = temporaryModules.get(name);
			if(map != null && map.size() > 0){
				if(DEBUG_TEMPORARY_MODULES){
					System.out.println("Returning temporary module: "+name);
				}
				return map.get(map.lastKey());
			}
		}
        AbstractModule n = null;
        ModulesKey keyForCacheAccess = new ModulesKey(null, null);

        if (!dontSearchInit) {
            if (n == null) {
                keyForCacheAccess.name = new StringBuffer(name).append(".__init__").toString();
                n = cache.getObj(keyForCacheAccess, this);
                if (n != null) {
                    name += ".__init__";
                }
            }
        }
        if (n == null) {
            keyForCacheAccess.name = name;
            n = cache.getObj(keyForCacheAccess, this);
        }

        if (n instanceof SourceModule) {
            //ok, module exists, let's check if it is synched with the filesystem version...
            SourceModule s = (SourceModule) n;
            if (!s.isSynched()) {
                //change it for an empty and proceed as usual.
                n = (AbstractModule) addModule(createModulesKey(s.getName(), s.getFile()));
            }
        }

        if (n instanceof EmptyModule) {
            EmptyModule e = (EmptyModule) n;

            boolean found = false;

            if (!found && e.f != null) {
                
                if (!e.f.exists()) {
                    //if the file does not exist anymore, just remove it.
                    keyForCacheAccess.name = name;
                    keyForCacheAccess.file = e.f;
                    doRemoveSingleModule(keyForCacheAccess);
                    n = null;
                    
                    
                } else {
                    //file exists
                    n = checkOverride(name, nature, n);
                    
                    
                    if(n instanceof EmptyModule){
                        //ok, handle case where the file is actually from a zip file...
                        if (e instanceof EmptyModuleForZip) {
                            EmptyModuleForZip emptyModuleForZip = (EmptyModuleForZip) e;
                            
                            if(emptyModuleForZip.pathInZip.endsWith(".class") || !emptyModuleForZip.isFile){
                                //handle java class... (if it's a class or a folder in a jar)
                                n = JythonModulesManagerUtils.createModuleFromJar(emptyModuleForZip);
                                n = decorateModule(n, nature);
                                
                            }else if(FileTypesPreferencesPage.isValidDll(emptyModuleForZip.pathInZip)){
                                //.pyd
                                n = new CompiledModule(name, IToken.TYPE_BUILTIN, this);
                                n = decorateModule(n, nature);
                                
                            }else if(PythonPathHelper.isValidSourceFile(emptyModuleForZip.pathInZip)){
                                //handle python file from zip... we have to create it getting the contents from the zip file
                                try {
                                    IDocument doc = REF.getDocFromZip(emptyModuleForZip.f, emptyModuleForZip.pathInZip);
                                    //NOTE: The nature (and so the grammar to be used) must be defined by this modules
                                    //manager (and not by the initial caller)!!
                                    n = AbstractModule.createModuleFromDoc(name, emptyModuleForZip.f, doc, this.getNature(), -1, false);
                                    SourceModule zipModule = (SourceModule) n;
                                    zipModule.zipFilePath = emptyModuleForZip.pathInZip;
                                    n = decorateModule(n, nature);
                                } catch (Exception exc1) {
                                    PydevPlugin.log(exc1);
                                    n = null;
                                }
                            }
                            
                            
                        } else {
                            //regular case... just go on and create it.
                            try {
                                //NOTE: The nature (and so the grammar to be used) must be defined by this modules
                                //manager (and not by the initial caller)!!
                                n = AbstractModule.createModule(name, e.f, this.getNature(), -1);
                                n = decorateModule(n, nature);
                            } catch (IOException exc) {
                                keyForCacheAccess.name = name;
                                keyForCacheAccess.file = e.f;
                                doRemoveSingleModule(keyForCacheAccess);
                                n = null;
                            } catch (MisconfigurationException exc) {
                                PydevPlugin.log(exc);
                                n=null;
                            }
                        }
                    }
                    
                }

            } else { //ok, it does not have a file associated, so, we treat it as a builtin (this can happen in java jars)
                n = checkOverride(name, nature, n);
                if(n instanceof EmptyModule){
                    if (acceptCompiledModule) {
                        n = new CompiledModule(name, IToken.TYPE_BUILTIN, this);
                        n = decorateModule(n, nature);
                    } else {
                        return null;
                    }
                }
            }

            if (n != null) {
                doAddSingleModule(createModulesKey(name, e.f), n);
            } else {
                System.err.println("The module " + name + " could not be found nor created!");
            }
        }

        if (n instanceof EmptyModule) {
            throw new RuntimeException("Should not be an empty module anymore!");
        }
        if(n instanceof SourceModule){
            SourceModule sourceModule = (SourceModule) n;
            //now, here's a catch... it may be a bootstrap module...
            if(sourceModule.isBootstrapModule()){
                //if it's a bootstrap module, we must replace it for the related compiled module.
                n = new CompiledModule(name, IToken.TYPE_BUILTIN, this);
                n = decorateModule(n, nature);
            }
        }
        
        return n;
    }
    
    /**
     * Called after the creation of any module. Used as a workaround for filling tokens that are in no way
     * available in the code-completion through the regular inspection.
     * 
     * The django objects class is the reason why this happens... It's structure for the creation on a model class
     * follows no real patterns for the creation of the 'objects' attribute in the class, and thus, we have no
     * real generic way of discovering it (actually, even by looking at the class definition this is very obscure),
     * so, the solution found is creating the objects by decorating the module with that info.
     */
    private AbstractModule decorateModule(AbstractModule n, IPythonNature nature){
        if(n instanceof SourceModule && "django.db.models.base".equals(n.getName())){
            SourceModule sourceModule = (SourceModule) n;
            SimpleNode ast = sourceModule.getAst();
            for(SimpleNode node:((Module)ast).body){
                if(node instanceof ClassDef && "Model".equals(NodeUtils.getRepresentationString(node))){
                    Object[][] metaclassAttrs = new Object[][]{
                            {"objects", NodeUtils.makeAttribute("django.db.models.manager.Manager()")},
                            {"DoesNotExist",new Name("Exception", Name.Load, false)},
                            {"MultipleObjectsReturned",new Name("Exception", Name.Load, false)},
                    };
                    
                    ClassDef classDef = (ClassDef) node;
                    stmtType[] newBody = new stmtType[classDef.body.length+metaclassAttrs.length];
                    System.arraycopy(classDef.body, 0, newBody, metaclassAttrs.length, classDef.body.length);

                    int i=0;
                    for(Object[] objAndType:metaclassAttrs){
                        //Note that the line/col is important so that we correctly acknowledge it inside the "class Model" scope. 
                        Name name = new Name((String)objAndType[0], Name.Store, false);
                        name.beginColumn = classDef.beginColumn+4;
                        name.beginLine = classDef.beginLine+1;
                        newBody[i] = new Assign(
                                new exprType[]{name}, 
                                (exprType) objAndType[1]
                        );
                        newBody[i].beginColumn = classDef.beginColumn+4;
                        newBody[i].beginLine = classDef.beginLine+1;
                        
                        i+= 1;
                    }
                    
                    
                    
                    
                    
                    
                    classDef.body = newBody;
                    break;
                }
            }
        }
        return n;
    }

    
    /**
     * Hook called to give clients a chance to override the module created (still experimenting, so, it's not public).
     */
    private AbstractModule checkOverride(String name, IPythonNature nature, AbstractModule emptyModule){
        return emptyModule;
    }
    

    private ModulesKey createModulesKey(String name, File f) {
        ModulesKey newEntry = new ModulesKey(name, f);
        Entry<ModulesKey, ModulesKey> oldEntry = this.modulesKeys.getEntry(newEntry);
        if(oldEntry != null){
            return oldEntry.getKey();
        }else{
            return newEntry;
        }
    }


    /**
     * Passes through all the compiled modules in memory and clears its tokens (so that
     * we restore them when needed).
     */
    public void clearCache() {
        ModulesManager.cache.internalCache.clear();
    }

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#isInPythonPath(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IProject)
     */
    public boolean isInPythonPath(IResource member, IProject container) {
        return resolveModule(member, container) != null;
    }

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#resolveModule(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IProject)
     */
    public String resolveModule(IResource member, IProject container) {
        File inOs = member.getRawLocation().toFile();
        return resolveModule(REF.getFileAbsolutePath(inOs));
    }

    protected String getResolveModuleErr(IResource member) {
        return "Unable to find the path " + member + " in the project were it\n" + 
            "is added as a source folder for pydev." + this.getClass();
    }

    /**
     * @param full
     * @return
     */
    public String resolveModule(String full) {
        return pythonPathHelper.resolveModule(full, false);
    }

    public List<String> getPythonPath() {
        if(pythonPathHelper == null){
            return new ArrayList<String>();
        }
        return pythonPathHelper.getPythonpath();
    }

}
