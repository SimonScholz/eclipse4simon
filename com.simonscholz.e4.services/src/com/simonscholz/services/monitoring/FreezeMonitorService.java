package com.simonscholz.services.monitoring;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

public interface FreezeMonitorService {

	/**
	 * Create a Monitoring Thread with default values.
	 * 
	 * <p>
	 * CAUTION: It is recommended to rather use
	 * {@link #setPreferencesAndStartIfNecessary(IEclipsePreferences, Display)} or
	 * {@link #setPreferencesAndStartIfNecessary(IPreferenceStore, Display)}.
	 * </p>
	 * 
	 * @param display {@link Display}, which is used for the application
	 */
	void createAndStartMonitorThread(Display display);

	/**
	 * Reads values from the given {@link IPreferenceStore} and starts the
	 * monitoring thread in case monitoring is activated in the preferences.
	 * 
	 * @param preferenceStore {@link IPreferenceStore}
	 * @param display         {@link Display}, which is used for the application
	 * 
	 * @see PreferenceConstants
	 */
	void setPreferencesAndStartIfNecessary(IPreferenceStore preferenceStore, Display display);

	/**
	 * Reads values from the given {@link IEclipsePreferences} and starts the
	 * monitoring thread in case monitoring is activated in the preferences.
	 * 
	 * @param eclipsePreferences {@link IEclipsePreferences}
	 * @param display            {@link Display}, which is used for the application
	 * 
	 * @see PreferenceConstants
	 */
	void setPreferencesAndStartIfNecessary(IEclipsePreferences eclipsePreferences, Display display);

}
