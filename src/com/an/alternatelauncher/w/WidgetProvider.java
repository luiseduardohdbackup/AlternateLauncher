package com.an.alternatelauncher.w;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.an.alternatelauncher.Launcher;
import com.an.alternatelauncher.R;
import com.an.alternatelauncher.d.Api;

@TargetApi(14)
public class WidgetProvider extends AppWidgetProvider {
//	private static final String TAG = WidgetProvider.class.getSimpleName();
	
	public static final String CLICK_ACTION = "com.an.alternatelauncher.CLICK";
	public static final String PACKAGE_KEY = "kp";
	
	private static DataProviderObserver sDataObserver;
	
	private class DataProviderObserver extends ContentObserver {
	    private AppWidgetManager mAppWidgetManager;
	    private ComponentName mComponentName;
	
	    DataProviderObserver(AppWidgetManager mgr, ComponentName cn, Handler h) {
	        super(h);
	        mAppWidgetManager = mgr;
	        mComponentName = cn;
	    }
	
	    @Override
	    public void onChange(boolean selfChange) {
	        // Data has changed, notify the widget that the collection view needs to be updated
	        // In response, the factory's onDataSetChanged() will be called which will requery the
	        // cursor for the new data
	        mAppWidgetManager.notifyAppWidgetViewDataChanged(
	                mAppWidgetManager.getAppWidgetIds(mComponentName), R.id.grid);
	    }
	}

	@Override
	public void onEnabled(Context context) {
        if (sDataObserver == null) {
            initializeContentObserver(context);
        }
	}
	
	private void initializeContentObserver(Context context) {
		final ContentResolver r = context.getContentResolver();
		final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        final ComponentName cn = new ComponentName(context, WidgetProvider.class);
        sDataObserver = new DataProviderObserver(mgr, cn, null);
        r.registerContentObserver(Api.CONTENT_URI, true, sDataObserver);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		
		String action = intent.getAction();
		
		if(action.equals(CLICK_ACTION)) {
			final String packageName = intent.getStringExtra(PACKAGE_KEY);
			
			if(!TextUtils.isEmpty(packageName)) {
				context.startActivity(context.getPackageManager().
								getLaunchIntentForPackage(packageName));
				
				// Increment launch count
				ContentResolver cr = context.getContentResolver();
				ContentValues values = new ContentValues();
				values.put(Api.Columns.PACKAGE, packageName);
				cr.update(Api.CONTENT_URI, values, null, null);				
			}
			
		}
		
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		
        for (int i = 0; i < appWidgetIds.length; ++i) {
        	
        	/*
        	 * Specify the service to provide views for the collection widget
        	 * We need to embed the appWidgetId via the data otherwise it will be ignored
        	 */
            final Intent intent = new Intent(context, RemoteViewService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            final RemoteViews rv = 
            				new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            rv.setRemoteAdapter(R.id.grid, intent);

            /*
             * Set the empty view to be displayed if the collection is empty
             * It must be a sibling view of the collection view
             * Wire to launch app on empty view click
             */
            rv.setEmptyView(R.id.grid, R.id.empty_view);
            final Intent launchIntent = new Intent(context, Launcher.class);
            final PendingIntent launchPendingIntent = PendingIntent.getActivity(context, 0,
            				launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.empty_view, launchPendingIntent);
            
            /*
             * We need to update the intent's data if we set an extra or the extras will be ignored 
             * otherwise 
             */
            final Intent onClickIntent = new Intent(context, WidgetProvider.class);
            onClickIntent.setAction(CLICK_ACTION);
            onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0,
                    onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.grid, onClickPendingIntent);
            
            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }
	}
	
}
