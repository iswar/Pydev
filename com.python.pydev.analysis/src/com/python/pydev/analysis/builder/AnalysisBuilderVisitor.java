/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.builder;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PyCodeCompletionVisitor;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.fastparser.FastDefinitionsParser;
import org.python.pydev.plugin.DebugSettings;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

public class AnalysisBuilderVisitor extends PyDevBuilderVisitor{

    /**
     * Do you want to gather information about dependencies?
     */
    public static final boolean DEBUG_DEPENDENCIES = false;

    @Override
    protected int getPriority() {
        return PyCodeCompletionVisitor.PRIORITY_CODE_COMPLETION+1; //just after the code-completion priority
    }

    
    
    @Override
    public void visitChangedResource(final IResource resource, final IDocument document, final IProgressMonitor monitor) {
        if(document == null){
            return;
        }
        
        //we may need to 'force' the analysis when a module is renamed, because the first message we receive is
        //a 'delete' and after that an 'add' -- which is later mapped to this method, so, if we don't have info
        //on the module we should analyze it because it is 'probably' a rename.
        final PythonNature nature = getPythonNature(resource);
        if(nature == null){
            return;
        }
        boolean analyzeDependent;
        if(isFullBuild()){
            analyzeDependent = false;
        }else{
            analyzeDependent = true;
        }
        
        
        //depending on the level of analysis we have to do, we'll decide whether we want
        //to make the full parse (slower) or the definitions parse (faster but only with info
        //related to the definitions)
        ICallback<IModule, Integer> moduleCallback = new ICallback<IModule, Integer>(){

			public IModule call(Integer arg) {
				if(arg == AnalysisBuilderRunnable.FULL_MODULE){
					return getSourceModule(resource, document, nature);
				}else if(arg == AnalysisBuilderRunnable.DEFINITIONS_MODULE){
	                if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
	                    Log.toLogFile(this, "PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()");
	                }
	                IFile f = (IFile) resource;
	                String file = f.getRawLocation().toOSString();
	                String moduleName = getModuleName(resource, nature);
	                return new SourceModule(moduleName, new File(file), 
	                        FastDefinitionsParser.parse(document.get(), moduleName), null);
				}else{
					throw new RuntimeException("Unexpected parameter: "+arg);
				}
			}}
        ;
		doVisitChangedResource(nature, resource, document, moduleCallback, null, analyzeDependent, monitor, false, 
                AnalysisBuilderRunnable.ANALYSIS_CAUSE_BUILDER);
    }
    


    /**
     * here we have to detect errors / warnings from the code analysis
     * Either the module callback or the module must be set.
     */
    public void doVisitChangedResource(IPythonNature nature, IResource resource, IDocument document, 
    		ICallback<IModule, Integer> moduleCallback, final IModule module, boolean analyzeDependent, IProgressMonitor monitor, boolean forceAnalysis,
            int analysisCause) {
        
        if(module != null){
        	if(moduleCallback != null){
        		throw new AssertionError("Only the module or the moduleCallback must be specified.");
        	}
        	setModuleInCache(module);
        	
        	moduleCallback = new ICallback<IModule, Integer>(){
        		
        		public IModule call(Integer arg) {
        			return module;
        		}};
        }else{
        	//don't set module in the cache if we only have the callback
        	//moduleCallback is already defined
        	if(moduleCallback == null){
        		throw new AssertionError("Either the module or the moduleCallback must be specified.");
        	}
        }
        
        final String moduleName = getModuleName(resource, nature);
        final AnalysisBuilderRunnable runnable = AnalysisBuilderRunnable.createRunnable(
                document, resource, moduleCallback, analyzeDependent, monitor, isFullBuild(), moduleName, 
                forceAnalysis, analysisCause);
        
        if(isFullBuild()){
            runnable.run();
        }else{
            Job workbenchJob = new Job("") {
            
                @Override
                public IStatus run(IProgressMonitor monitor) {
                    this.getThread().setName("AnalysisBuilderThread :"+moduleName);
                    runnable.run();
                    return Status.OK_STATUS;
                }
            
            };
            workbenchJob.setSystem(true);
            workbenchJob.setPriority(Job.BUILD);
            workbenchJob.schedule();
        }
    }




    @Override
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        PythonNature nature = getPythonNature(resource);
        if(nature == null){
            return;
        }
        String moduleName = getModuleName(resource, nature);
        fillDependenciesAndRemoveInfo(moduleName, nature, true, monitor, isFullBuild());
    }
    

    @Override
    public void visitingWillStart(IProgressMonitor monitor, boolean isFullBuild, IPythonNature nature) {
        if(isFullBuild){
            AbstractAdditionalDependencyInfo info = AdditionalProjectInterpreterInfo.
                getAdditionalInfoForProject(nature);
            
            info.clearAllInfo();
        }
    }
    
    /**
     * @param moduleName this is the module name
     * @param nature this is the nature
     * @param analyzeDependent determines if we should add dependent modules to be analyzed later
     */
    public static void fillDependenciesAndRemoveInfo(String moduleName, PythonNature nature, 
            boolean analyzeDependent, IProgressMonitor monitor, boolean isFullBuild) {
        if(moduleName != null && nature != null){
            AbstractAdditionalDependencyInfo info = AdditionalProjectInterpreterInfo.
                getAdditionalInfoForProject(nature);
            
            boolean generateDelta;
            if(isFullBuild){
                generateDelta = false;
            }else{
                generateDelta = true;
            }
            info.removeInfoFromModule(moduleName, generateDelta);
        }
    }

}
