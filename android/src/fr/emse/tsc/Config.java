package fr.emse.tsc;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Config extends PreferenceActivity {
	
	private static final String DEF_LENSERVER = "192.168.0.14";
	private static final String DEF_LENPORT = "30303";
	private static final String DEF_LENNAME = "alice";
	private static final String DEF_WENSERVER = "192.168.0.14";
	private static final String DEF_WENPORT = "40404";
	private static final String DEF_WENNAME = "bob";
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
	}
	
	public static String getLServer(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("lenserver", DEF_LENSERVER);
	}

	public static String getLPort(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("lenport", DEF_LENPORT);
	}

	public static String getLName(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("lenname", DEF_LENNAME);
	}
	
	public static String getWServer(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("wenserver", DEF_WENSERVER);
	}

	public static String getWPort(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("wenport", DEF_WENPORT);
	}

	public static String getWName(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("wenname", DEF_WENNAME);
	}

}
