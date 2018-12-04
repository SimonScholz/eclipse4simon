package com.simonscholz.services.monitoring;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

public interface FreezeMonitorService {

	void createAndStartMonitorThread(Display display);

	void setPreferences(IPreferenceStore preferenceStore);

	void setPreferences(IEclipsePreferences eclipsePreferences);

}
