package com.an.alternatelauncher;

import java.util.ArrayList;
import java.util.List;

import com.an.alternatelauncher.d.AppEntry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v4.content.AsyncTaskLoader;

public class AppListLoader extends AsyncTaskLoader<List<AppEntry>> {
	
    private final InterestingConfigChanges mLastConfig = new InterestingConfigChanges();
    private final PackageManager mPm;

	/**
	 * Perform alphabetical comparison of application entry objects.
	 */
//	public final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
//	    private final Collator sCollator = Collator.getInstance();
//	    public int compare(AppEntry object1, AppEntry object2) {
//	        return sCollator.compare(object1.getLabel(), object2.getLabel());
//	    }
//	};

    List<AppEntry> mApps;
    PackageIntentReceiver mPackageObserver;

    public AppListLoader(Context context) {
        super(context);

        // Retrieve the package manager for later use; note we don't
        // use 'context' directly but instead the save global application
        // context returned by getContext().
        mPm = getContext().getPackageManager();
    }

    /**
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */
    @Override public List<AppEntry> loadInBackground() {
    	
    	Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        List<ResolveInfo> apps = mPm.queryIntentActivities(mainIntent, 0);
        if (apps == null) {
            apps = new ArrayList<ResolveInfo>();
        }

        // Create corresponding array of entries and load their labels.
        List<AppEntry> entries = new ArrayList<AppEntry>(apps.size());
        for(ResolveInfo resolveInfo:apps) {
            entries.add(new AppEntry(mPm, resolveInfo));
        }

        // Sort the list
//        Collections.sort(entries, ALPHA_COMPARATOR);

        return entries;
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override public void deliverResult(List<AppEntry> apps) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We don't need the result.
            if (apps != null) {
                onReleaseResources(apps);
            }
        }
        List<AppEntry> oldApps = apps;
        mApps = apps;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(apps);
        }

        // At this point we can release the resources associated with
        // 'oldApps' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldApps != null) {
            onReleaseResources(oldApps);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override protected void onStartLoading() {
        if (mApps != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mApps);
        }

        // Start watching for changes in the app data.
        if (mPackageObserver == null) {
            mPackageObserver = new PackageIntentReceiver(this);
        }

        // Has something interesting in the configuration changed since we
        // last built the app list?
        boolean configChange = mLastConfig.applyNewConfig(getContext().getResources());

        if (takeContentChanged() || mApps == null || configChange) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override public void onCanceled(List<AppEntry> apps) {
        super.onCanceled(apps);

        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(apps);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps' if needed.
        if (mApps != null) {
            onReleaseResources(mApps);
            mApps = null;
        }

        // Stop monitoring for changes.
        if (mPackageObserver != null) {
            getContext().unregisterReceiver(mPackageObserver);
            mPackageObserver = null;
        }
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<AppEntry> apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }
    
	/**
	 * Helper for determining if the configuration has changed in an interesting
	 * way so we need to rebuild the app list.
	 */
	public static class InterestingConfigChanges {
	    final Configuration mLastConfiguration = new Configuration();
	    int mLastDensity;
	
	    boolean applyNewConfig(Resources res) {
	        int configChanges = mLastConfiguration.updateFrom(res.getConfiguration());
	        boolean densityChanged = mLastDensity != res.getDisplayMetrics().densityDpi;
	        if (densityChanged || (configChanges&(ActivityInfo.CONFIG_LOCALE
	                |ActivityInfo.CONFIG_UI_MODE|ActivityInfo.CONFIG_SCREEN_LAYOUT)) != 0) {
	            mLastDensity = res.getDisplayMetrics().densityDpi;
	            return true;
	        }
	        return false;
	    }
	}
	
	/**
	 * Helper class to look for interesting changes to the installed apps
	 * so that the loader can be updated.
	 */
	public static class PackageIntentReceiver extends BroadcastReceiver {
	    final AppListLoader mLoader;
	
	    public PackageIntentReceiver(AppListLoader loader) {
	        mLoader = loader;
	        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
	        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
	        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
	        filter.addDataScheme("package");
	        mLoader.getContext().registerReceiver(this, filter);
	        // Register for events related to sdcard installation.
	        IntentFilter sdFilter = new IntentFilter();
	        sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
	        sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
	        mLoader.getContext().registerReceiver(this, sdFilter);
	    }
	
	    @Override public void onReceive(Context context, Intent intent) {
	        // Tell the loader about the change.
	        mLoader.onContentChanged();
	    }
	}
	
}
