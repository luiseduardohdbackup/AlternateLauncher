package com.an.alternatelauncher.d;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import com.an.alternatelauncher.d.Api.Columns;
import com.an.alternatelauncher.d.Scm.Activities;
import com.an.alternatelauncher.d.Scm.Attributes;

public class Pro extends ContentProvider {
	
	private static final String INVALID_INSERT_DATA = "Insert is missing required data";
	private static final String INSERT_FAILED = "Failed to insert data";
	private static final String MISSING_UPDATE_IDENTIFIER = "Update requires ID or package";
	
	private Dbh mDbh;
	private SQLiteDatabase mDb;
	
	private volatile boolean mBatchInProcess;

	@Override
	public boolean onCreate() {
		
		mDbh = new Dbh(getContext());
		mDb = mDbh.getWritableDatabase();
		
		mBatchInProcess = false;
		
		return mDb != null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// No deleting
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return Api.LAUNCHER_ITEM_URI_STRING;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		
		if(!validInsert(values))
			throw new SQLException(INVALID_INSERT_DATA);
		
		Uri recordUri = null;
		
		mDb.beginTransaction();
		
		try {
			
			long rowId = mDb.insert(Activities.NAME, null, extractActivitiesCv(values));
			
			if(rowId > -1) {
				
				mDb.insert(Attributes.NAME, null, extractAttributesCv(rowId, values));
				
				recordUri = ContentUris.withAppendedId(Api.CONTENT_URI, rowId);
				
				if(!mBatchInProcess)
					getContext().getContentResolver().notifyChange(recordUri, null);
			}
			
			mDb.setTransactionSuccessful();
			
//			Debug.d(Dbh.DATABASE_NAME, "Inserted", values, rowId);
			
		} catch(SQLException e) {
			throw new SQLException(INSERT_FAILED);			
		} finally {
			mDb.endTransaction();
		}
		
        return recordUri;
		
	}
	
	private boolean validInsert(ContentValues values) {
		return values.containsKey(Columns.PACKAGE) 
						&& values.containsKey(Columns.ACTIVITY) 
						&& values.containsKey(Columns.LABEL0)
						;
	}
	
	private ContentValues extractActivitiesCv(ContentValues values) {
		ContentValues cv = new ContentValues(3);
		cv.put(Columns.PACKAGE, values.getAsString(Columns.PACKAGE));
		cv.put(Columns.ACTIVITY, values.getAsString(Columns.ACTIVITY));
		cv.put(Columns.LABEL0, values.getAsString(Columns.LABEL0));
		return cv;
	}
	
	private ContentValues extractAttributesCv(ContentValues values) {
		// @reminder Size of three for ID if needed (overloaded)
		ContentValues cv = new ContentValues(3);
		cv.put(Attributes.LABEL, values.getAsString(Columns.LABEL));
		cv.put(Attributes.SHOW, values.getAsBoolean(Columns.SHOW) ? 1 : 0);
		return cv;
	}
	private ContentValues extractAttributesCv(long activityId, ContentValues values) {
		ContentValues cv = extractAttributesCv(values);
		cv.put(Attributes.ACTIVITY_KEY, activityId);
		return cv;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
					String sortOrder) {
		if(!uri.equals(Api.CONTENT_URI))
			throw new IllegalArgumentException("Unknown URI " + uri);
		
		return mDb.rawQuery(Dbh.Statement.SELECT_ENTRIES_FOR_DISPLAY.s, null);
		
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

		/*
		 * Package name increments launch count
		 * ID updates attributes
		 */
		String packageName = values.getAsString(Columns.PACKAGE);
		Integer id = values.getAsInteger(Columns._ID);
		
		if(id == null && TextUtils.isEmpty(packageName))
			throw new IllegalArgumentException(MISSING_UPDATE_IDENTIFIER);
		
		if(id == null) {
			
//			Debug.d("cp update", "incrementing launch count", packageName);
			
			// Increment the launch count and broadcast uri modification
			mDb.execSQL(Dbh.Statement.INCREMENT_LAUNCH_COUNT.s, new Object[]{packageName});
			
		} else {
			
//			Debug.d("cp update", "updating attributes", values);
			
			// @reminder Attributes table defines activity key and not ID
			mDb.update(Attributes.NAME, extractAttributesCv(values), Attributes.ACTIVITY_KEY+"=?"
							, new String[]{""+values.getAsLong(Columns._ID)});
		}

		if(!mBatchInProcess)
			getContext().getContentResolver().notifyChange(uri, null);
		
		return 1;
	}
	
	@Override
	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
					throws OperationApplicationException {
		
		mBatchInProcess = true;
		
		ContentProviderResult[] results = super.applyBatch(operations);
		
		mBatchInProcess = false;
		
		getContext().getContentResolver().notifyChange(Api.CONTENT_URI, null);
		
		return results;
	}
	
	@TargetApi(11)
	@Override
	public void shutdown() {
		mDb.close();
		mDbh.close();
		super.shutdown();
	}
}
