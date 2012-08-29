package com.an.alternatelauncher.d;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.an.alternatelauncher.d.Scm.Activities;
import com.an.alternatelauncher.d.Scm.Attributes;
import com.an.alternatelauncher.d.Scm.View;
import com.an.debug.Debug;

// adb -s emulator-5554 shell
// sqlite3 /data/data/*/databases/flashcard.sqlite

public final class Dbh extends SQLiteOpenHelper {
	
	public static final String DATABASE_NAME = "launcher.sqlite";
	private static final int DATABASE_VERSION = 3;
	
	private static final String APPLICATION_ERROR = "Application Error";
	
	public Dbh(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
//		Debug.d(DATABASE_NAME, DATABASE_VERSION);
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
		Cursor c = db.rawQuery(Statement.SELECT_COUNT.s, null);
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
	 * Requires the query return PACKAGE, LABEL0, LABEL, SHOW
	 * @param query
	 * @return
	 */
	private ArrayList<LauncherInfo> getInfos(String query) {
		ArrayList<LauncherInfo> items = new ArrayList<LauncherInfo>();
		
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.rawQuery(query, null);
		if(c.moveToFirst()) {
			while(!c.isAfterLast()) {
				items.add(new LauncherInfo(c.getString(0), c.getString(1), c.getString(2)
								,c.getInt(3)!=0));
				c.moveToNext();
			}
		}
		c.close();
		db.close();
		
		return items;
	}
	
	/**
	 * Insert new (by ID) or update based on package name
	 * @param items
	 * @return
	 */
	public int updateInfos(List<LauncherInfo> items) {
		
		int added = 0;
		
		SQLiteDatabase db = getWritableDatabase();
		
		SQLiteStatement stmtInsertActivity = db.compileStatement(Statement.INSERT_ACTVITIY.s);
		SQLiteStatement stmtInsertAttributes = db.compileStatement(Statement.INSERT_ATTRIBUTES.s);
		SQLiteStatement stmtUpdate = db.compileStatement(Statement.UPDATE_ATTRIBUTES.s);
		
		Cursor c;
		for(LauncherInfo item:items) {
			// bindAllArgsAsStrings is only available in API 11
			c = db.rawQuery(Statement.SELECT_PACKAGE.s, new String[]{item.packageName});
			
			db.beginTransaction();
			
			try {
				
				String label = TextUtils.isEmpty(item.label) ? item.label0 : item.label;
				// Update
				if(c.moveToFirst()) {
					long id = c.getLong(0);
					
					stmtUpdate.bindString(1, label);
					stmtUpdate.bindLong(2, item.show ? 1 : 0);
					stmtUpdate.bindLong(3, id);
					
					if(stmtUpdate.executeInsert() > -1)
						added++;
					
					Debug.d(DATABASE_NAME, "Updated", item);
					
				// Insert
				} else {
					
					stmtInsertActivity.bindString(1, item.packageName);
					stmtInsertActivity.bindString(2, item.label0);
					
					long id = stmtInsertActivity.executeInsert();
					
					stmtInsertAttributes.bindLong(1, id);
					stmtInsertAttributes.bindString(2, label);
					stmtInsertAttributes.bindLong(3, item.show ? 1 : 0);
					
					if(stmtInsertAttributes.executeInsert() > -1)
						added++;
					
					Debug.d(DATABASE_NAME, "Inserted", item);
					
				}
				
				db.setTransactionSuccessful();
				
			} catch(Exception e) {
				Debug.e(DATABASE_NAME, "error in update/insert", e);
			} finally {
				db.endTransaction();
			}
			
			c.close();
		}
		
		stmtInsertActivity.close();
		stmtInsertAttributes.close();
		stmtUpdate.close();
		
		db.close();
		
		return added;

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
	
	private enum Statement {
		
		/** count */
		SELECT_COUNT(String.format("select count(*) from %1$s", Activities.NAME))
		
		/** _ID */
		,SELECT_PACKAGE(String.format("select _id from %1$s where %2$s=?"
						,Activities.NAME, Activities.PACKAGE))
		
		/** PACKAGE LABEL0 LABEL SHOW */
		,SELECT_ENTRIES(String.format("select %1$s, %2$s, %3$s, %4$s from %5$s"
				,Activities.PACKAGE, Activities.LABEL0, Attributes.LABEL, Attributes.SHOW
				,View.ACTIVITIES_INFO.NAME
				))
						
		/** PACKAGE LABEL0 LABEL SHOW */				
		,SELECT_ENTRIES_FOR_DISPLAY(String.format("select %1$s, %2$s, %3$s, %4$s" +
				" from %5$s where %4$s<>0"
				,Activities.PACKAGE, Activities.LABEL0, Attributes.LABEL, Attributes.SHOW
				,View.ACTIVITIES_INFO.NAME
				))

		/** PACKAGE LABEL0 */
		,INSERT_ACTVITIY(String.format("insert into %1$s(%2$s,%3$s) values(?,?)"
						,Activities.NAME, Activities.PACKAGE, Activities.LABEL0
						))
				
		/** ACTIVITY_KEY LABEL SHOW */
		,INSERT_ATTRIBUTES(String.format("insert into %1$s(%2$s,%3$s,%4$s) values(?,?,?)"
						,Attributes.NAME, Attributes.ACTIVITY_KEY, Attributes.LABEL, Attributes.SHOW
						))
						
		/** LABEL SHOW ACTIVITY_KEY */				
		,UPDATE_ATTRIBUTES(String.format("update %1$s set %2$s=?,%3$s=? where %4$s=?"
						,Attributes.NAME, Attributes.LABEL, Attributes.SHOW, Attributes.ACTIVITY_KEY
						))
						
		;
		
		final String s;
		Statement(String statement) { s = statement; }
		@Override public String toString() {return s;}

	}
	
}
