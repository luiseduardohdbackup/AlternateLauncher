package com.an.alternatelauncher.d;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.an.debug.Debug;

public class AppEntry implements Comparable<AppEntry> {

    private final PackageManager packageManager;
    public final ResolveInfo resolveInfo;
    public final String packageName;
    public final String activityName;
    public final String label0;
    
    private String mLabel;
    private Drawable mIcon;
    private boolean mShow;
    
    public AppEntry(PackageManager packageManager, ResolveInfo info) {
        this.packageManager = packageManager;
        resolveInfo = info;
        packageName = info.activityInfo.packageName;
        activityName = info.activityInfo.name;
    	label0 = info.loadLabel(packageManager).toString();
    	
        mLabel = label0;
        mShow = true;
        
        // @debug
//        mShow = false;
    }

    public ResolveInfo getApplicationInfo() {
        return resolveInfo;
    }
    
    /**
     * Will default to the original label if {@code label} is empty
     * @param label
     */
    public void setLabel(CharSequence label) {
    	mLabel = TextUtils.isEmpty(label) ? label0 : label.toString();
    }
    
    public String getLabel() {
        return mLabel;
    }
    
    public Drawable getIcon() {
        if(mIcon == null)
        	mIcon = resolveInfo.loadIcon(packageManager);
        return mIcon;
    }
    
    public boolean getShow() {
    	return mShow;
    }
    
    public void setShow(boolean show) {
    	mShow = show;
    }
    
    @Override public String toString() {
        return String.format("%1$s %2$s %3$s", Debug.toYesNoString(mShow), mLabel, label0);
    }
    
    public int compareTo(AppEntry another) {
    	return mShow && !another.mShow ? -1
    					: !mShow && another.mShow ? 1
    									: mLabel.compareTo(another.mLabel);
    }

}