/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.utils.LabelFieldEditor;

public class DjangoTemplatesPreferencesPageRoot extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public void init(IWorkbench workbench) {
        setDescription("Django Templates Editor"); 
    }


    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        
        addField(new LabelFieldEditor("PREF_TO_IGNORE", 
                "\n" +
                "To change the editor colors use Aptana > Themes\n" +
                "\n" +
                "To change the keywords colored, edit the templates\n" +
                "with the Context 'Django tags'\n" +
                "", p));
    }

}
