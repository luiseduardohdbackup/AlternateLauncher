package com.an.alternatelauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
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
import com.an.alternatelauncher.d.Api;
import com.an.alternatelauncher.d.AppEntry;
import com.an.alternatelauncher.d.Dbh;
import com.an.alternatelauncher.d.LauncherInfo;
import com.an.debug.Debug;

public class AppGridFragment extends Fragment implements LoaderCallbacks<List<AppEntry>>, Page {
	private static final String TAG = AppGridFragment.class.getSimpleName();

	ModelFrag mModelFrag;
	
	private List<AppEntry> mLauncherApps;
	
	private ContentObserver mContentObserver;
	
	private class DataProviderObserver extends ContentObserver {

	    public DataProviderObserver(Handler handler) {
			super(handler);
		}

		@Override
	    public void onChange(boolean selfChange) {
	        getLoaderManager().restartLoader(0, null, AppGridFragment.this);
	        if(mModelFrag != null)
	        	mModelFrag.reload();
	    }
	}
	
	public static Fragment newInstance() {
		return new AppGridFragment();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLauncherApps = new ArrayList<AppEntry>();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.app_grid_layout, null);
		
		GridView grid = (GridView) view.findViewById(R.id.grid);
		grid.setEmptyView(view.findViewById(R.id.empty_view));
		
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		FragmentActivity activity = getActivity();
        
        FragmentManager fm = activity.getSupportFragmentManager();
        
        startObservingContent();
        
        // (Re)Attach the model frag 
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
	
	public Loader<List<AppEntry>> onCreateLoader(int arg0, Bundle arg1) {
		return new AppsLoader(getActivity());
	}
	
	public void onLoadFinished(Loader<List<AppEntry>> loader, final List<AppEntry> list) {
		
//		Debug.d(TAG, "Load complete", list.size());
		
		synchronized (mLauncherApps) {
			mLauncherApps = list;
		}
		
		checkLoadState();
				
	}

	public void onLoaderReset(Loader<List<AppEntry>> arg0) {
		mLauncherApps = new ArrayList<AppEntry>();
	}
	
	public void pageIn(int fromDirection) {
		
		Activity activity = getActivity();
		if(activity != null) {
			
			// Hide input since it is never required
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
			
//			Debug.d(TAG, "Loading grid");
			
			new Thread(new Runnable() {
				public void run() {
					
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
					
					Collections.sort(apps);
					
					synchronized (mLauncherApps) {
						mLauncherApps = apps;
					}
					
					Debug.d(TAG, "adapter size", mLauncherApps.size(), "infos size", infos.size());
					
					// Renew the grid adapter completely since data has changed
					final Activity activity = getActivity();
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
	
	private void startObservingContent() {
		if(mContentObserver == null) {
			mContentObserver = new DataProviderObserver(null);
			getActivity().getContentResolver().registerContentObserver(Api.CONTENT_URI, true
							, mContentObserver);
		}
	}

	private void modelLoaded() {
		checkLoadState();
	}
	
	/**
	 * Grid adapter
	 *
	 */
	public class AppsAdapter extends BaseAdapter implements OnItemClickListener {
		
		public final Context context;
		public final PackageManager packageManager;
		
		public final float DP;
		public final int ICON_LENGTH;
		
		public AppsAdapter() {
			context = getActivity();
			packageManager = context.getPackageManager();
			
			DP = getResources().getDisplayMetrics().density;
			ICON_LENGTH = (int) (Constants.DP_ICON_LENGTH * DP + 0.5f);
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			
//			final TypedArray a = activity.obtainStyledAttributes(
//			                null, R.styleable.Keyboard, 0, R.style.app_icon_textview);
			
			// TODO Load styles from xml, set height from styles
			TextView textView = new TextView(context);
			textView.setGravity(Gravity.CENTER_HORIZONTAL);
			textView.setMinHeight(getResources().getDimensionPixelSize(R.dimen.grid_length));
			
			AppEntry entry = mLauncherApps.get(position);
			
//			Debug.d(TAG, info.loadLabel(pm), info.activityInfo.hashCode());
			
			Drawable icon = entry.resolveInfo.activityInfo.loadIcon(packageManager);
			icon.setBounds(new Rect(0, 0, ICON_LENGTH, ICON_LENGTH));
			
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
		
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//			Debug.d(TAG, parent, view, position, id, view.getTag());
			if(view.getTag() != null) {
				String packageName = (String) view.getTag();
				startActivity(getActivity().getPackageManager().getLaunchIntentForPackage(
								packageName));
				
				// Update launch count
				ContentResolver cr = context.getContentResolver();
				ContentValues values = new ContentValues();
				values.put(Api.Columns.PACKAGE, packageName);
				cr.update(Api.CONTENT_URI, values, null, null);
			}
			
		}
	}
	
	public static final class ModelFrag extends Fragment {
		private static final String FRAG_TAG = 
				ModelFrag.class.getSimpleName() + "." + AppGridFragment.class.getSimpleName();
		
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
	    	new LoadTask().execute();
	    }
	    
	    private final class LoadTask extends AsyncTask<Void, Void, List<LauncherInfo>>{
	        
	    	@Override
	    	protected List<LauncherInfo> doInBackground(Void... params) {
	    		
	    		List<LauncherInfo> lis = new ArrayList<LauncherInfo>();
	        	
	        	Activity activity = getActivity();
	        	if(activity != null) {
	        		Dbh dbh = new Dbh(activity);
	        		lis = dbh.getDisplayInfo();
	        		dbh.close();
				}
	        	
	        	return lis;
	        	
	    	}
	    	
	    	@Override
	    	protected void onPostExecute(List<LauncherInfo> result) {
	    		
        		synchronized(mLis) {
        			mLis = result;
	        	}
	        	
		    	mLoaded = true;
		    	Activity activity = getActivity();
		    	if(activity != null) {
	    	    	final Fragment fragment = getTargetFragment();
	    	    	if(fragment != null) {
						((AppGridFragment)fragment).modelLoaded();
	    	    	}
		    	}

	    	}
		    
	    };

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setRetainInstance(true);
	        new LoadTask().execute();
	    }
	    
	}
	
}
