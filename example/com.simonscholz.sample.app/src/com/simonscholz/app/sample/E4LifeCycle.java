package com.simonscholz.app.sample;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.swt.widgets.Display;

import com.simonscholz.services.monitoring.FreezeMonitorService;
import com.simonscholz.services.monitoring.PreferenceConstants;

/**
 * This is a stub implementation containing e4 LifeCycle annotated methods.<br />
 * There is a corresponding entry in <em>plugin.xml</em> (under the
 * <em>org.eclipse.core.runtime.products' extension point</em>) that references
 * this class.
 **/
@SuppressWarnings("restriction")
public class E4LifeCycle {

	@PostContextCreate
	void postContextCreate(IEclipseContext workbenchContext, Display display, FreezeMonitorService freezeMonitorService, @Preference IEclipsePreferences prefs) {
		prefs.putBoolean(PreferenceConstants.MONITORING_ENABLED, true);
		
		freezeMonitorService.setPreferencesAndStartIfNecessary(prefs, display);
	}
}
