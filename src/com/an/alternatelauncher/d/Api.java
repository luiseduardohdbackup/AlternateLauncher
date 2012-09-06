package com.an.alternatelauncher.d;

import android.net.Uri;

/**
 * Exposed schema for content provider(s)
 * @author Qui
 *
 */
public class Api {
	private Api(){}

	public static final String AUTHORITY = "com.an.alternatelauncher.w.provider";

	static final String LAUNCHER_ITEM_URI_STRING;
	public static final Uri CONTENT_URI;
	
	static {
		LAUNCHER_ITEM_URI_STRING = "content://" + AUTHORITY + "/launcheritems";
		CONTENT_URI = Uri.parse(LAUNCHER_ITEM_URI_STRING);
	}
	
	public static final class Columns {
		public static final String _ID = Scm.Activities._ID;
		public static final String PACKAGE = Scm.Activities.PACKAGE;
		public static final String ACTIVITY = Scm.Activities.ACTIVITY;
		public static final String LABEL0 = Scm.Activities.LABEL0;
		public static final String LABEL = Scm.Attributes.LABEL;
		public static final String SHOW = Scm.Attributes.SHOW;
	}
	
}
