package com.an.alternatelauncher.d;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.an.alternatelauncher.d.Scm.Activities;
import com.an.alternatelauncher.d.Scm.Attributes;
import com.an.alternatelauncher.d.Scm.View;
import com.an.debug.Debug;

public final class Dbh extends SQLiteOpenHelper {
	
	public static final String DATABASE_NAME = "launcher.sqlite";
	private static final int DATABASE_VERSION = 3;
	
	private static final String APPLICATION_ERROR = "Application Error";
	
	public Dbh(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override public void onCreate(SQLiteDatabase db) {
		Builder.createSchema(db);
	}
	
	/**
	 * 20120828	Version 1
	 * 20120829 Version 2 added launch count
	 * 20120830 Version 3 more descriptors
	 */
	@Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Debug.i(DATABASE_NAME, "Upgrading from", oldVersion, "to", newVersion);
	}
	
	public boolean isEmpty() {
		long recordCount = 0;
		
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.rawQuery(Statement.SELECT_VISIBLE_COUNT.s, null);
		if(c.moveToFirst())
			recordCount = c.getLong(0);
		c.close();
		db.close();
		
		return recordCount == 0;
	}
	
	public ArrayList<LauncherInfo> getInfos() {
		return getInfos(Statement.SELECT_ENTRIES.s);
	}

	public ArrayList<LauncherInfo> getDisplayInfo() {
		return getInfos(Statement.SELECT_ENTRIES_FOR_DISPLAY.s);
	}
	
	/**
	 * Requires the query return _ID PACKAGE LABEL0 LABEL SHOW
	 * Activity name doesn't matter here
	 * @param query
	 * @return
	 */
	private ArrayList<LauncherInfo> getInfos(String query) {
		ArrayList<LauncherInfo> items = new ArrayList<LauncherInfo>();
		
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.rawQuery(query, null);
		if(c.moveToFirst()) {
			while(!c.isAfterLast()) {
				items.add(new LauncherInfo(c.getInt(0), c.getString(1), "", c.getString(2)
								, c.getString(3), c.getInt(4)!=0));
				c.moveToNext();
			}
		}
		c.close();
		db.close();
		
		return items;
	}
	
	public void recreate() {
		SQLiteDatabase db = getWritableDatabase();
		Builder.recreateSchema(db);
		db.close();
	}
	
	private static final class Builder {
		private Builder(){}
		
		static final boolean createSchema(SQLiteDatabase db) {
			boolean success = createTables(db)
					&& createViews(db)
					&& createTriggers(db);
			if(!success)
				Debug.e(APPLICATION_ERROR, 
						String.format("Unable to create database %s", DATABASE_NAME));
			return success;
		}

		static final boolean createTables(SQLiteDatabase db) {
			boolean success = true;
			try {
				db.execSQL(Activities.CREATE);
				db.execSQL(Attributes.CREATE);
			} catch(SQLException e) {
				success = false;
				Debug.e(DATABASE_NAME, e.getMessage());
			}
			return success;
		}

		static final boolean createViews(SQLiteDatabase db) {
			boolean success = true;
			for(Scm.View v:Scm.View.values()) {
				try {
					db.execSQL(v.CREATE);
				} catch(SQLException e) {
					success = false;
					Debug.e(DATABASE_NAME, e.getMessage());
				}
			}
			return success;
		}
		
		static final boolean createTriggers(SQLiteDatabase db) {
			boolean success = true;
			for(Scm.Triggers t:Scm.Triggers.values()) {
				try {
					db.execSQL(t.CREATE);
				} catch(SQLException e) {
					success = false;
					Debug.e(DATABASE_NAME, e.getMessage());
				}
			}
			return success;
		}		
		
		static final boolean recreateSchema(SQLiteDatabase db) {
			boolean success = dropViews(db)
					&& dropTables(db)
					&& createSchema(db);
			if(!success)
				Debug.e(APPLICATION_ERROR, 
						String.format("Unable to recreate database [%s]", DATABASE_NAME));
			return success;
		}
	
		static final boolean dropTables(SQLiteDatabase db) {
			return dropTable(db, Activities.NAME)
							&& dropTable(db, Attributes.NAME);
		}
		
		static final boolean dropTable(SQLiteDatabase db, String table_name) {
			boolean success = true;
			try {
				db.execSQL("DROP TABLE IF EXISTS " + table_name);
			} catch(SQLException e) {
				success = false;
				Debug.e(DATABASE_NAME, e.getMessage());
			}
			return success;
		}
		
		static final boolean dropViews(SQLiteDatabase db) {
			boolean success = true;
			for(Scm.View v:Scm.View.values()) {
				try {
					db.execSQL("DROP VIEW IF EXISTS " + v.NAME);
				} catch(SQLException e) {
					success = false;
				}
			}
			return success;
		}
				
	}
	
	enum Statement {
		
		/** count */
		SELECT_VISIBLE_COUNT(String.format("select count(*) from %1$s where %2$s<>0"
						,View.ACTIVITIES_INFOS.NAME, Attributes.SHOW))
		
		/** _ID PACKAGE LABEL0 LABEL SHOW */
		,SELECT_ENTRIES(String.format("select %6$s, %1$s, %2$s, %3$s, %4$s from %5$s"
				,Activities.PACKAGE, Activities.LABEL0, Attributes.LABEL, Attributes.SHOW
				,View.ACTIVITIES_INFOS.NAME, Activities._ID
				))

		/** _ID PACKAGE LABEL0 LABEL SHOW ACTIVITY_NAME */				
		,SELECT_ENTRIES_FOR_DISPLAY(String.format("select %6$s, %1$s, %2$s, %3$s, %4$s, %7$s" +
				" from %5$s where %4$s<>0 order by %6$s desc"
				,Activities.PACKAGE, Activities.LABEL0, Attributes.LABEL, Attributes.SHOW
				,View.ACTIVITIES_INFOS.NAME, Attributes.LAUNCH_COUNT, Activities.ACTIVITY
				,Activities._ID
				))

		,INCREMENT_LAUNCH_COUNT(String.format(
						"update %1$s set %2$s=%2$s+1 where %3$s=" +
						"(select %4$s from %5$s where %6$s=?)"
						,Scm.Attributes.NAME, Scm.Attributes.LAUNCH_COUNT
						,Scm.Attributes.ACTIVITY_KEY, Scm.Activities._ID, Scm.Activities.NAME
						,Scm.Activities.PACKAGE
						))
		;
		
		final String s;
		Statement(String statement) { s = statement; }

	}
	
}
