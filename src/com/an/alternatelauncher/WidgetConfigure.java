package com.an.alternatelauncher;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Window;
import android.widget.RemoteViews;

public class WidgetConfigure extends FragmentActivity {
//	private static final String TAG = WidgetConfigure.class.getSimpleName();
	
	private int mAppWidgetId;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, 
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        
        FragmentManager fm = getSupportFragmentManager();
        if(fm.findFragmentById(android.R.id.content) == null) {
        	
        	Fragment fragment = AppListFragment.newInstance();
        	fm.beginTransaction().add(android.R.id.content, fragment).commit();
        	
        }
        
    }

	@Override
	protected void onDestroy() {
		
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		
		RemoteViews views = new RemoteViews(getPackageName(),
						R.layout.app_grid_layout);
		
		appWidgetManager.updateAppWidget(mAppWidgetId, views);
		
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);
//		finish();
		
		super.onDestroy();
	}
	
}
