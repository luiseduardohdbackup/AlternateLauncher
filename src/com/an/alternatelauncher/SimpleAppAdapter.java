package com.an.alternatelauncher;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.an.alternatelauncher.d.AppEntry;
import com.an.alternatelauncher.d.LauncherInfo;


final class SimpleAppAdapter extends BaseAdapter implements OnFocusChangeListener, OnClickListener {
//	private static final String TAG = SimpleAppAdapter.class.getSimpleName();
	
	private final LayoutInflater mLi;
	
	private List<AppEntry> mAppEntries;
	
	private final int iconWidth;
	
	public SimpleAppAdapter(Context context) {
		mLi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mAppEntries = new ArrayList<AppEntry>();
        iconWidth = (int) (48 * context.getResources().getDisplayMetrics().density + 0.5f);
	}
	
	synchronized void setApps(List<AppEntry> appEntries) {
		mAppEntries = new ArrayList<AppEntry>(appEntries);
		notifyDataSetChanged();
	}
	
	/**
	 * Returns a list of launcher infos describing the current state of the adapter
	 * @return
	 */
	public List<LauncherInfo> getLauncherInfos() {
		List<LauncherInfo> infos = new ArrayList<LauncherInfo>();
		// @todo Check if focus is active and account for that change before sending off
		synchronized(mAppEntries) {
			for(AppEntry entry:mAppEntries)
				infos.add(new LauncherInfo(entry));
//			Debug.d(TAG, "Returning list of info");
		}
		return infos;
	}

	public int getCount() {
		return mAppEntries != null ? mAppEntries.size() : 0;
	}

	public Object getItem(int position) {
		return mAppEntries == null ? null : mAppEntries.get(position);
	}

	public long getItemId(int position) {
		return position;
	}
	
	public void onFocusChange(View v, boolean hasFocus) {
		
		if(!hasFocus) {

			if(v.getTag(R.id.position) != null) {
				synchronized (mAppEntries) {
					int position = (Integer) v.getTag(R.id.position);
					/*
					 * @reminder Must check this because for some reason, app is destroyed
					 * 	prior to EditText losing focus
					 */
					if(position < mAppEntries.size()) {
						AppEntry appEntry = mAppEntries.get(position);
						appEntry.setLabel(((TextView)v).getText());
						// No need to broadcast change since EditText already reflects it
					}
//					else Debug.w(TAG, "Focusing out of", position, "in array of size"
//									,mAppEntries.size());
				}
			}
		}
	}
	
	public void onClick(View v) {
		
		CheckBox checkbox = (CheckBox) (v instanceof CheckBox ? v
						: ((View)v.getParent()).findViewById(R.id.checkbox));
		
//		Debug.d(TAG, "Checked changed", checkbox, checkbox.isChecked(), checkbox.getTag(R.id.position));
		if(checkbox.getTag(R.id.position) != null) {
			
			// Must toggle checkbox if anything other than the checkbox was pressed
			if(!(v instanceof CheckBox)) {
				checkbox.setChecked(!checkbox.isChecked());
			}
			
			boolean checked = checkbox.isChecked();
			synchronized (mAppEntries) {
				AppEntry appEntry = mAppEntries.get((Integer) checkbox.getTag(R.id.position));
				appEntry.setShow(checked);
				// No need to broadcast change since EditText already reflects it
			}
			
		}
	}

	public View getView(int position, View convertView, ViewGroup parent) {
//		Debug.d(TAG, "Loading view", position);
		
		ViewHolder holder;

        if (convertView == null) {
            convertView = mLi.inflate(R.layout.app_list_item, null);

            holder = new ViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.label = (EditText) convertView.findViewById(R.id.label);
            holder.checkbox = (CheckBox) convertView.findViewById(R.id.checkbox);
            
            holder.label.setOnFocusChangeListener(this);
            holder.checkbox.setOnClickListener(this);
            holder.icon.setOnClickListener(this);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppEntry appEntry = (AppEntry) getItem(position);
        
        if(appEntry != null) {
        	
            holder.label.setText(appEntry.getLabel());
            holder.label.setHint(appEntry.label0);
            
            holder.icon.setImageDrawable(appEntry.getIcon());
            holder.icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.icon.setLayoutParams(new LinearLayout.LayoutParams(iconWidth, iconWidth));
            
            // @reminder This triggers onCheckChange so don't listen unless logic is tailored
            holder.checkbox.setChecked(appEntry.getShow());
            
//            Debug.d(TAG, appEntry);
            
            holder.checkbox.setTag(R.id.position, position);
            holder.label.setTag(R.id.position, position);

        }
        
        return convertView;
	}
	
	private class ViewHolder {
		CheckBox checkbox;
		ImageView icon;
		EditText label;
	}
	
}
