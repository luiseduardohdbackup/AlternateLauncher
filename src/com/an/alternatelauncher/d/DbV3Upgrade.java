package com.an.alternatelauncher.d;

import android.provider.BaseColumns;

import com.an.alternatelauncher.d.Scm.Activities;

final class DbV3Upgrade {
	private DbV3Upgrade(){}
	
	static final class TempActivities implements BaseColumns {
		private TempActivities(){};
		static final String NAME = "temp_activities";
		
		static final String LABEL = "lbl";
		
		static final String LABEL0 = "lbl0";
		
		static final String SHOW = "shw";
		
		static final String CREATE = String.format(
						"CREATE TEMP TABLE IF NOT EXISTS %1$s" +
						"(%2$s INTEGER PRIMARY KEY AUTOINCREMENT" +
						",%3$s TEXT" +
						",%4$s TEXT" +
						",%5$s INTEGER DEFAULT 1" +
						");"
						,NAME, _ID, LABEL0, LABEL, SHOW
						);
		
		static final String POPULATE = String.format(
						"INSERT INTO %1$s(%2$s,%3$s,%4$s)" +
						" SELECT %5$s,%6$s,%7$s FROM %8$s"
						,NAME, LABEL, LABEL0, SHOW
						,LABEL, LABEL0, SHOW, Activities.NAME
						);
		
		/** LABEL0 LABEL SHOW */
		static final String SELECT_ALL = String.format(
						"SELECT %1$s,%2$s,%3$s FROM %4$s"
						,LABEL0, LABEL, SHOW, NAME
						);
	}
	

//	public void testV3Upgrade() {
//		SQLiteDatabase db = getWritableDatabase();
//		v3Temp(db);
//		db.close();
//	}
//	public void upgrade2To3(SQLiteDatabase db) {
//		
//		v3Temp(db);
//		
//		Builder.dropTable(db, Activities.NAME);
//		db.execSQL(Scm.Activities.CREATE);
//		db.execSQL(Scm.Attributes.CREATE);
//		
//		Cursor c = db.rawQuery(DbV3Upgrade.TempActivities.SELECT_ALL, null);
//
//		// Since package must be unique, we'll use placeholders for now
//		String packagePlaceholder = "package";
//		int pCounter = 0;
//		
//		if(c.moveToFirst()) {
//			while(!c.isAfterLast()) {
//				String label0 = c.getString(0);
//				String label = c.getString(1);
//				int show = c.getInt(2);
//				
//				ContentValues values =  new ContentValues();
//				db.beginTransaction();
//				try {
//					
//					values.put(Activities.LABEL0, label0);
//					values.put(Activities.PACKAGE, packagePlaceholder+(pCounter++));
//				
//					long id = db.insert(Activities.NAME, null, values);
//					
//					values.clear();
//					values.put(Attributes.ACTIVITY_KEY, id);
//					values.put(Attributes.LABEL, label);
//					values.put(Attributes.SHOW, show);
//					
//					db.insert(Attributes.NAME, null, values);
//					
//					Debug.d(DATABASE_NAME, "inserted", label0, label, show, pCounter);
//					
//					db.setTransactionSuccessful();
//					
//				} catch(Exception e) {
//					Debug.e(DATABASE_NAME, "upgrade transaction failed", 2, 3, e);
//				} finally {
//					db.endTransaction();
//				}
//				
//				c.moveToNext();
//			}
//		}
//		c.close();
//		
//		Debug.i(DATABASE_NAME, "Database upgraded from", 2, "to", 3);
//		
//	}
//	private void v3Temp(SQLiteDatabase db) {
//		Debug.i(DATABASE_NAME, DATABASE_VERSION, "v2 upgrade to v3");
//		db.execSQL(DbV3Upgrade.TempActivities.CREATE);
//		db.execSQL(DbV3Upgrade.TempActivities.POPULATE);
//		
//		Cursor c = db.rawQuery(DbV3Upgrade.TempActivities.SELECT_ALL, null);
//		if(c.moveToFirst()) {
//			while(!c.isAfterLast()) {
//				Debug.d(DATABASE_NAME, c.getString(0), c.getString(1), c.getInt(2));
//				c.moveToNext();
//			}
//		}
//		c.close();
//		
//	}
	
	
}
