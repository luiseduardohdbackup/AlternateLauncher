package com.an.alternatelauncher.w;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.an.alternatelauncher.Constants;
import com.an.alternatelauncher.R;
import com.an.alternatelauncher.d.Api;

@TargetApi(11)
public class RemoteViewService extends RemoteViewsService{

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new GridRemoteViewsFactory(this.getApplicationContext(), intent);
	}

}

class GridRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
//	private static final String TAG = GridRemoteViewsFactory.class.getSimpleName();
	
	private Context mContext;
	private PackageManager mPm;
	private Cursor mCursor;
//	private int mAppWidgetId;
    
    private final int mIconLength;

    public GridRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mPm = context.getPackageManager();
        
//        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
//                AppWidgetManager.INVALID_APPWIDGET_ID);
        
        mIconLength = (int) (Constants.DP_ICON_LENGTH * 
        				context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public void onCreate() {
        // Since we reload the cursor in onDataSetChanged() which gets called immediately after
        // onCreate(), we do nothing here.
    }

    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public int getCount() {
        return mCursor.getCount();
    }

    public RemoteViews getViewAt(int position) {
        
    	// Get the data for this position from the content provider
    	String packageName = "";
        String label = "";
        Bitmap icon = null;
        
        if (mCursor.moveToPosition(position)) {
        	
        	final int packageIndex = mCursor.getColumnIndex(Api.Columns.PACKAGE);
            packageName = mCursor.getString(packageIndex);
            
            final int activityIndex = mCursor.getColumnIndex(Api.Columns.ACTIVITY);
            String activityName = mCursor.getString(activityIndex);
            
            final int labelIndex = mCursor.getColumnIndex(Api.Columns.LABEL);
            label = mCursor.getString(labelIndex);
            if(TextUtils.isEmpty(label)) {
                final int label0Index = mCursor.getColumnIndex(Api.Columns.LABEL0);
            	label = mCursor.getString(label0Index);
            }
            
            try {
            	
    			icon = ((BitmapDrawable) mPm.getActivityIcon(
            					new ComponentName(packageName, activityName))).getBitmap();
            	
            } catch(Exception e) {}
            
//        	Debug.d(TAG, "getViewAt", packageName, activityName, label, icon);
        }

        // Create individual widget views
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);
        
        rv.setTextViewText(R.id.label, label);
        
        if(icon != null)
        	rv.setImageViewBitmap(R.id.icon, 
        					Bitmap.createScaledBitmap(icon, mIconLength, mIconLength, false));

        // Add package name to fill in intent to launch app on click
        final Intent fillInIntent = new Intent();
        final Bundle extras = new Bundle();
        extras.putString(WidgetProvider.PACKAGE_KEY, packageName);
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.launcher_item, fillInIntent);
        
        return rv;
    }
    
    public RemoteViews getLoadingView() {
        return new RemoteViews(mContext.getPackageName(), R.layout.widget_loading_view);
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mContext.getContentResolver().query(Api.CONTENT_URI, null, null, null
        				, null);
        
    }
}
