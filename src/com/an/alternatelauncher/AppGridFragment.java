package com.an.alternatelauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.an.alternatelauncher.c.Page;
import com.an.alternatelauncher.d.AppEntry;
import com.an.alternatelauncher.d.Dbh;
import com.an.alternatelauncher.d.LauncherInfo;
import com.an.debug.Debug;

public class AppGridFragment extends Fragment implements LoaderCallbacks<List<AppEntry>>, Page {
	private static final String TAG = AppGridFragment.class.getSimpleName();

	ModelFrag mModelFrag;
	
	private List<AppEntry> mLauncherApps;
	
	private BroadcastReceiver mInfoChangedBr;
	
	private void registerLocalReceivers() {
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
		lbm.registerReceiver( mInfoChangedBr,
				new IntentFilter(getString(R.string.action_info_changed)));
	}
	private void unregisterLocalReceivers() {
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
		lbm.unregisterReceiver(mInfoChangedBr);
	}
	
	public static Fragment newInstance() {
		Fragment fragment = new AppGridFragment();
		
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mInfoChangedBr = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
//				Debug.d(TAG, "info changed broadcast received in grid");
				// @reminder Activty could be destroyed so check before proceeding
				
				if(getActivity() != null) {
					if(mModelFrag != null) {
						getLoaderManager().restartLoader(0, null, AppGridFragment.this);
						mModelFrag.reload();
					}
				}
				
			}
		};
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.app_grid_layout, null);

		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		FragmentActivity activity = getActivity();
        
        FragmentManager fm = activity.getSupportFragmentManager();
		mModelFrag = (ModelFrag) fm.findFragmentByTag(ModelFrag.FRAG_TAG);

		if(mModelFrag == null) {
			mModelFrag = new ModelFrag();
			mModelFrag.setTargetFragment(this, getId());
			fm.beginTransaction().add(mModelFrag, ModelFrag.FRAG_TAG).commit();
		}
		else
			mModelFrag.setTargetFragment(this, getId());
		
		// Prepare the loader.  Either re-connect with an existing one, or start a new one.
        getLoaderManager().initLoader(0, null, this);
		
//		Debug.d(TAG, "onActivityCreated()");
		
	}
	
	@Override
	public void onStart() {
		super.onStart();
		registerLocalReceivers();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		unregisterLocalReceivers();
	}
	
	public Loader<List<AppEntry>> onCreateLoader(int arg0, Bundle arg1) {
		return new AppListLoader(getActivity());
	}
	

	public void onLoadFinished(Loader<List<AppEntry>> loader, final List<AppEntry> list) {
		
//		Debug.d(TAG, "Load complete", list.size());
		
		new Thread(new Runnable() {
			
			public void run() {
				
				mLauncherApps = list;
				
				checkLoadState();
				
			}
			
		}).run();
		
	}

	public void onLoaderReset(Loader<List<AppEntry>> arg0) {
//		Debug.d(TAG, "Loader reset");
	}
	
	public void pageIn(int fromDirection) {
		Activity activity = getActivity();
		if(activity != null) {
			
			// Hide input
			new Handler().postDelayed(new Runnable() {
				public void run() {
					final InputMethodManager imm = (InputMethodManager) 
									getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				    imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
				}
			}, 250);
			
		}
	}
	public void pageOut(int toDirection) {}
	
	private void checkLoadState() {
		
		if(mLauncherApps != null && mModelFrag.mLoaded) {
			
			Debug.d(TAG, "Loading grid");
			
			new Thread(new Runnable() {
				public void run() {
					
					Activity activity = getActivity();
					
					Map<String,LauncherInfo> infos = mModelFrag.getInfoMap();
					List<AppEntry> apps = new ArrayList<AppEntry>();
					
					for(AppEntry entry:mLauncherApps) {
						CharSequence packageName = entry.packageName;
						if(infos.containsKey(packageName)) {
							LauncherInfo info = infos.get(packageName);
							entry.setLabel(info.label);
							apps.add(entry);
						}
					}
					
					synchronized (mLauncherApps) {
						mLauncherApps = apps;
					}
					
					activity.runOnUiThread(new Runnable() {
						public void run() {
							GridView grid = (GridView) getView().findViewById(R.id.grid);
							AppsAdapter gridAdapter = new AppsAdapter();
							grid.setAdapter(gridAdapter);
							grid.setOnItemClickListener(gridAdapter);
						}
					});
					
				}
			}).start();
			
		}

	}

	private void modelLoaded() {
		checkLoadState();
	}
	
	/*
	 * Save all labels and map to alternate name where original name is default
	 */
	public class AppsAdapter extends BaseAdapter implements OnItemClickListener {
		
		public final float DP;
		
		public AppsAdapter() {
			DP = getResources().getDisplayMetrics().density;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			
			Activity activity = getActivity();
			PackageManager pm = activity.getPackageManager();
			
//			final TypedArray a = activity.obtainStyledAttributes(
//			                null, R.styleable.Keyboard, 0, R.style.app_icon_textview);
			
			// TODO Load styles from xml, set height from styles
			TextView textView = new TextView(activity);
			textView.setGravity(Gravity.CENTER_HORIZONTAL);
			textView.setMinHeight((int)(90*DP));
			
			AppEntry entry = mLauncherApps.get(position);
			
//			Debug.d(TAG, info.loadLabel(pm), info.activityInfo.hashCode());
			
			Drawable icon = entry.resolveInfo.activityInfo.loadIcon(pm);
			final int w = (int) (48 * getResources().getDisplayMetrics().density + 0.5f);
			icon.setBounds(new Rect(0, 0, w, w));
			
			textView.setCompoundDrawables(null, icon, null, null);
			textView.setText(entry.getLabel());
			
			textView.setTag(entry.packageName);
			
			return textView;
			
		}
		
		public final int getCount() {
			return mLauncherApps.size();
		}
		
		public final Object getItem(int position) {
			return mLauncherApps.get(position);
		}
		
		public final long getItemId(int position) {
			return position;
		}
		
		// @todo Record launch in db
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//			Debug.d(TAG, parent, view, position, id, view.getTag());
			if(view.getTag() != null)
				startActivity(getActivity().getPackageManager().getLaunchIntentForPackage(
								(String) view.getTag()));
			
		}
	}
	
	public static final class ModelFrag extends Fragment {
		private static final String FRAG_TAG = 
				AppGridFragment.class.getSimpleName() + "." + ModelFrag.class.getSimpleName();
		
		private List<LauncherInfo> mLis;
		private volatile boolean mLoaded;
		
	    public ModelFrag() {
	    	mLis = new ArrayList<LauncherInfo>();
	    	mLoaded = false;
	    }

	    public Map<String,LauncherInfo> getInfoMap() {
	    	return listToMap(Collections.unmodifiableList(mLis));
	    }
	    
	    private Map<String,LauncherInfo> listToMap(List<LauncherInfo> list) {
			Map<String,LauncherInfo> lisMap = new HashMap<String, LauncherInfo>();
        	for(LauncherInfo li:list)
        		lisMap.put(li.packageName, new LauncherInfo(li));
        	return lisMap;
		}
	    
	    void reload() {
	    	mLoaded = false;
	    	new LoadThread().start();
	    }
	    
	    private void loaded() {
	    	mLoaded = true;
	    	Activity activity = getActivity();
	    	if(activity != null) {
	    		synchronized (activity) {
	    	    	final Fragment fragment = getTargetFragment();
	    	    	if(fragment != null) {
	    	    		activity.runOnUiThread(new Runnable() {
	    					public void run() {
	    						((AppGridFragment)fragment).modelLoaded();
	    					}
	    				});
	    	    	}
				}
	    	}
	    	Debug.d(FRAG_TAG, "loaded/reloaded");
	    }
	    
	    private final class LoadThread extends Thread {
	        @Override
	        public void run() {
	        	
	        	List<LauncherInfo> lis = new ArrayList<LauncherInfo>();
	        	
	        	Activity activity = getActivity();
	        	if(activity != null) {
	        		Dbh dbh = new Dbh(activity);
	        		lis = dbh.getDisplayInfo();
	        		dbh.close();
				}
	        	
	        	if(lis != null && lis.size() > 0) {
		        	
//		        	Debug.d(FRAG_TAG, "Loaded", lis.size());
		        	
	        		synchronized(mLis) {
	        			mLis = lis;
		        	}
		        	
	        	}
	        	
        		loaded();
	        	
	        }
	    };

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setRetainInstance(true);
	        new LoadThread().start();
//	        Debug.d(FRAG_TAG, "Model frag created", this);
	    }
	    
	}
	
}
