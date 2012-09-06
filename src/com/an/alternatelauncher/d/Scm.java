package com.an.alternatelauncher.d;

import android.provider.BaseColumns;

final class Scm {
	private Scm(){};
	
	static final class Activities implements BaseColumns {
		private Activities(){};
		static final String NAME = "activities";
		
		static final String LABEL0 = "label0";
		
		// @reminder Check activity exists on launch to avoid launching uninstalled apps
		static final String PACKAGE = "package";
		
		static final String ACTIVITY = "activity";
		
		/**
		 * <ul>
		 * 	<li>Insert of existing LABEL0 replaces old row</li>
		 * </ul>
		 */
		static final String CREATE = String.format(
						"CREATE TABLE IF NOT EXISTS %1$s" +
						"(%2$s INTEGER PRIMARY KEY AUTOINCREMENT" +
						",%3$s TEXT" +
						",%4$s TEXT UNIQUE ON CONFLICT REPLACE" +
						",%5$s TEXT" +
						",CHECK(%3$s<>'')" +
						",CHECK(%4$s<>'')" +
						",CHECK(%5$s<>'')" +
						");"
						,NAME, _ID, LABEL0, PACKAGE, ACTIVITY
						);
	}
	
	static final class Attributes {
		private Attributes(){};
		static final String NAME = "attributes";
		
		static final String ACTIVITY_KEY = "a_key";
		
		static final String LABEL = "label";
		
		static final String SHOW = "show";
		
		static final String LAUNCH_COUNT = "launch_count";
		
		/**
		 * <ul>
		 * 	<li>Delete on Activities table cascades</li>
		 * </ul>
		 */
		static final String CREATE = String.format(
						"CREATE TABLE IF NOT EXISTS %1$s" +
						"(%2$s INTEGER PRIMARY KEY" +
						",%3$s TEXT" +
						",%4$s INTEGER DEFAULT 1" +
						",%5$s INTEGER DEFAULT 0" +
						",CHECK(%3$s<>'')" +
						",FOREIGN KEY (%2$s) REFERENCES %6$s(%7$s) ON DELETE CASCADE" +
						");"
						,NAME, ACTIVITY_KEY, LABEL, SHOW, LAUNCH_COUNT
						,Activities.NAME, Activities._ID
						);
	}
	
	enum View {
		
		ACTIVITIES_INFOS("visible_activities", String.format(
						" select %1$s,%2$s,%3$s,%8$s,%9$s,%10$s,%6$s" +
						" from %4$s ac inner join %5$s at on ac.%6$s=at.%7$s"
						,Activities.PACKAGE, Activities.LABEL0, Attributes.LABEL
						,Activities.NAME, Attributes.NAME, Activities._ID, Attributes.ACTIVITY_KEY
						,Attributes.SHOW, Attributes.LAUNCH_COUNT, Activities.ACTIVITY
						))
		;		
		public final String CREATE;
		public final String NAME;
		
		View(String name, String selectStatement) {
			NAME = name;
			CREATE = String.format("create view if not exists %1$s as ",name) + selectStatement;
		}
		
	}
	
	enum Triggers {
		
		;
		public final String CREATE;
		public final String NAME;
		
		Triggers(String name, String selectStatement) {
			NAME = name;
			CREATE = String.format("create trigger if not exists %1$s as ",name) + selectStatement;
		}
		
	}
	
}
