package com.an.alternatelauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;

import com.an.alternatelauncher.c.Page;
import com.an.alternatelauncher.d.AppEntry;
import com.an.alternatelauncher.d.Dbh;
import com.an.alternatelauncher.d.LauncherInfo;
import com.an.debug.Debug;

public final class AppListFragment extends ListFragment implements LoaderCallbacks<List<AppEntry>>
		,Page {
	private static final String TAG = AppListFragment.class.getSimpleName();
	
	ModelFrag mModelFrag;
	
	SimpleAppAdapter mAdapter;
	
	private Map<String,AppEntry> mLauncherApps;
	
	public static Fragment newInstance() {
		Fragment fragment = new AppListFragment();
		
		return fragment;
	}
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        Debug.d(TAG, TAG);
        
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
        
        setEmptyText("Lacking apps, download some");

        setHasOptionsMenu(false);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new SimpleAppAdapter(activity); 
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader.  Either re-connect with an existing one, or start a new one.
        getLoaderManager().initLoader(0, null, this);
        
        // TODO Read from xml, or at least use DP
        getListView().setPadding(0, 10, 0, 10);
        
    }
	
	@Override
	public void onDestroy() {
		cleanup();
		super.onDestroy();
	}
	
	public void pageIn(int fromDirection) {}
	
	public void pageOut(int toDirection) {
		cleanup();
	}
	
	private void cleanup() {
		if(mAdapter != null && mModelFrag != null) {
			mModelFrag.cleanup(mAdapter.getLauncherInfos());
		}
	}

	public Loader<List<AppEntry>> onCreateLoader(int arg0, Bundle arg1) {
		return new AppListLoader(getActivity());
	}
	

	public void onLoadFinished(Loader<List<AppEntry>> loader, final List<AppEntry> list) {
		
//		Debug.d(TAG, "Load complete", list.size());
		
		new Thread(new Runnable() {
			
			public void run() {
				
				mLauncherApps = new HashMap<String, AppEntry>();
				
				for(AppEntry entry:list)
					mLauncherApps.put(entry.packageName, entry);
				
				checkLoadState();
				
			}
			
		}).run();
		
	}

	public void onLoaderReset(Loader<List<AppEntry>> arg0) {
//		Debug.d(TAG, "Loader reset");
		mAdapter.setApps(new ArrayList<AppEntry>());
	}
	
	private void checkLoadState() {
		
		if(mLauncherApps != null && mModelFrag.mLoaded) {
			
//			Debug.d(TAG, "Data ready");
			
			/*
			 * For some reason, if we process on another thread, refresh won't work
			 * For now, this is fine, the number of items are low that we won't get ANR, at least
			 * 	not on the Nexus 7
			 * This works for now because Loader takes longer than backend load
			 */
//			new Thread(new Runnable() {
//				public void run() {
					
					List<LauncherInfo> savedInfo = mModelFrag.getInfoList();
					
					List<AppEntry> apps = new ArrayList<AppEntry>();
					
					synchronized(mLauncherApps) {
						
						// Update with saved info
						for(LauncherInfo info:savedInfo) {
							AppEntry entry = mLauncherApps.get(info.packageName);
							if(entry != null) {
								entry.setLabel(info.label);
								entry.setShow(info.show);
							}
						}
						
						// Clone info for display
						for(Map.Entry<String,AppEntry> entry:mLauncherApps.entrySet())
							apps.add(entry.getValue());

					}
					
					Collections.sort(apps);
										
					mAdapter.setApps(apps);
					
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							if (isResumed()) {
					            setListShown(true);
					        } else {
					            setListShownNoAnimation(true);
					        }
						}
					});
//					
//				}
//			});
			
		}

	}
	
	public static final class ModelFrag extends Fragment {
		private static final String FRAG_TAG = 
				AppListFragment.class.getSimpleName() + "." + ModelFrag.class.getSimpleName();
		
		private Map<String,LauncherInfo> mLis;
		private volatile boolean mLoaded;
		
	    public ModelFrag() {
	    	mLis = new HashMap<String,LauncherInfo>();
	    	mLoaded = false;
	    }
	    
	    private void loaded() {
	    	mLoaded = true;
	    	Activity activity = getActivity();
	    	if(activity != null) {
	    		synchronized (activity) {
	    	    	final Fragment fragment = getTargetFragment();
	    	    	if(fragment != null) {
//	    	    		activity.runOnUiThread(new Runnable() {
//	    					public void run() {
	    						((AppListFragment)fragment).checkLoadState();
//	    					}
//	    				});
	    	    	}
				}
	    	}
//	    	Debug.d(FRAG_TAG, "Load frag loaded");
	    }
	    
	    public List<LauncherInfo> getInfoList() {
	    	return Collections.unmodifiableList(new ArrayList<LauncherInfo>(mLis.values()));
	    }
	    
		public void cleanup(final List<LauncherInfo> infos) {
			
			final Activity activity = getActivity();
			
			new Thread(new Runnable() {
				public void run() {
					
					// First, get and update those that have changed
					List<LauncherInfo> changedInfos = new ArrayList<LauncherInfo>();
					
					synchronized(mLis) {
						for(LauncherInfo info:infos) {
							LauncherInfo oldInfo = mLis.get(info.packageName);
							if(oldInfo == null || !oldInfo.equals(info)) {
//								Debug.d(FRAG_TAG, "changed", oldInfo, info);
								changedInfos.add(info);
								mLis.put(info.packageName, info);
							}
						}
					}
					
					new mCleanupTask(activity, changedInfos).execute();
				}
			}).run();
		}
	    
		private class mCleanupTask extends AsyncTask<Void, Void, Void> {

			private final Context mContext;
			private final List<LauncherInfo> mInfos;

			mCleanupTask(Context context, List<LauncherInfo> infos) {
				mContext = context;
				mInfos = infos;
			}

			@Override
			protected Void doInBackground(Void... params) {

				if(mInfos.size() > 0) {
					
					// Save to db
					Dbh dbh = new Dbh(mContext);
					dbh.updateInfos(mInfos);
					
					// Call on activity, not context since context can still exist without activity 
					Activity activity = getActivity();
					if(activity != null) {
						LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(activity);
						lbm.sendBroadcastSync(new Intent(getString(R.string.action_info_changed)));
//						Debug.d(FRAG_TAG, "new info");
					}
					
				}
				
				return null;
				
			}
			
		};
		
		private Map<String,LauncherInfo> listToMap(List<LauncherInfo> list) {
			Map<String,LauncherInfo> lisMap = new HashMap<String, LauncherInfo>();
        	for(LauncherInfo li:list)
        		lisMap.put(li.packageName, new LauncherInfo(li));
        	return lisMap;
		}
		
	    private final Thread mLoadThread = new Thread() {
	        @Override
	        public void run() {
	        	
	        	List<LauncherInfo> lis = new ArrayList<LauncherInfo>();
	        	
	        	Activity activity = getActivity();
	        	if(activity != null) {
	        		Dbh dbh = new Dbh(activity);
	        		lis = dbh.getInfos();
	        		dbh.close();
				}
	        	
	        	if(lis != null && lis.size() > 0) {
		        	
//		        	Debug.d(FRAG_TAG, "Loaded", lis.size());
		        	Map<String,LauncherInfo> lisMap = listToMap(lis);
		        	
	        		synchronized(mLis) {
	        			mLis = lisMap;
		        	}
		        	
	        	}
	        	
        		loaded();
	        	
	        }
	    };

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setRetainInstance(true);
	        mLoadThread.start();
//	        Debug.d(FRAG_TAG, "Model frag created", this);
	    }
	    
	}
	
}
