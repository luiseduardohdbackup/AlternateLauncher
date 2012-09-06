package com.an.alternatelauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import com.an.alternatelauncher.c.Page;
import com.an.alternatelauncher.d.Api;
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
		return new AppListFragment();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLauncherApps = new HashMap<String, AppEntry>();
	}
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        FragmentActivity activity = getActivity();
        
        FragmentManager fm = activity.getSupportFragmentManager();
        
        // (Re)Attach the model frag
		mModelFrag = (ModelFrag) fm.findFragmentByTag(ModelFrag.TAG);
		if(mModelFrag == null) {
			mModelFrag = new ModelFrag();
			mModelFrag.setTargetFragment(this, getId());
			fm.beginTransaction().add(mModelFrag, ModelFrag.TAG).commit();
		}
		else
			mModelFrag.setTargetFragment(this, getId());
        
        setEmptyText(getString(R.string.no_apps));

        setHasOptionsMenu(false);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new SimpleAppAdapter(activity); 
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one, or start a new one.
        getLoaderManager().initLoader(0, null, this);
        
        // TODO Read from xml, or at least use DP
        getListView().setPadding(0, 10, 0, 10);

//        Debug.d(TAG, "onActivityCreated", mModelFrag, mAdapter);
        
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
	
	/*
	 * For some reason, when we go into an app, home out (not back out) and open AL again
	 * 	the entire activity will report as null
	 * Open AL, select querty, home out, open AL again, go to list, make changes and swipe to grid
	 * 	Debug conditional the check below and watch expressions getActivity() or this and it will
	 * 	be null
	 */
	private void cleanup() {
		Debug.d(TAG, "cleanup", mAdapter, mModelFrag);
		if(mAdapter != null && mModelFrag != null) {
			mModelFrag.cleanup(mAdapter.getLauncherInfos());
		}
	}

	public Loader<List<AppEntry>> onCreateLoader(int arg0, Bundle arg1) {
		return new AppsLoader(getActivity());
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
			
			new Thread(new Runnable() {
				
				public void run() {
					
//					Debug.d(TAG, "Data ready for view", mModelFrag, mAdapter);
					
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
										
					final List<AppEntry> adapterData = apps;
					
					// Update list view
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							
							mAdapter.setApps(adapterData);
							
							if (isResumed()) {
					            setListShown(true);
					        } else {
					            setListShownNoAnimation(true);
					        }
							
						}
					});
					
				}
			}).start();
			
		}

	}
	
	public static final class ModelFrag extends Fragment {
		private static final String TAG = 
				 ModelFrag.class.getSimpleName() + "." + AppListFragment.class.getSimpleName();
		
		private Map<String,LauncherInfo> mLis;
		private volatile boolean mLoaded;
		
	    public ModelFrag() {
	    	mLis = new HashMap<String,LauncherInfo>();
	    	mLoaded = false;
	    }
	    
	    public List<LauncherInfo> getInfoList() {
	    	return Collections.unmodifiableList(new ArrayList<LauncherInfo>(mLis.values()));
	    }
	    
		public void cleanup(final List<LauncherInfo> infos) {
			
//			Debug.d(TAG, "Cleanup");
			
			final Activity activity = getActivity();

			new Thread(new Runnable() {
				
				private static final int INSERT = 0;
				private static final int UPDATE = INSERT+1;

				public void run() {
					
					ArrayList<ContentProviderOperation> operations = 
									new ArrayList<ContentProviderOperation>();
					
					synchronized(mLis) {
						for(LauncherInfo info:infos) {
							
							LauncherInfo oldInfo = mLis.get(info.packageName);
							
							// New info, should trigger refresh after push to backend
							if(oldInfo == null) {
								operations.add(infoNew(info));
								
							// Exisiting info
							} else if(!oldInfo.equals(info)) {
								operations.add(infoUpdate(oldInfo.id, info));
								mLis.put(info.packageName, info);

							}
							
						}
					}
					
					if(operations.size() > 0)
						new mCleanupTask(activity, operations).execute();
				}
				
				private ContentProviderOperation.Builder baseInfo(int opType, LauncherInfo info) {
					return (opType == UPDATE 
									? ContentProviderOperation.newUpdate(Api.CONTENT_URI)
									: ContentProviderOperation.newInsert(Api.CONTENT_URI)
									)
									.withValue(Api.Columns.LABEL, 
													TextUtils.isEmpty(info.label) ? 
																	info.label0 : info.label)
									.withValue(Api.Columns.SHOW, info.show);
				}
				
				private ContentProviderOperation infoNew(LauncherInfo info) {
					return baseInfo(INSERT, info)
									.withValue(Api.Columns.PACKAGE, info.packageName)
									.withValue(Api.Columns.ACTIVITY, info.activityName)
									.withValue(Api.Columns.LABEL0, info.label0)
									.build();
				}
				
				private ContentProviderOperation infoUpdate(int id, LauncherInfo info) {
					return baseInfo(UPDATE, info)
									.withValue(Api.Columns._ID, id)
									.build();
				}
				
			}).run();
		}
	    
		private class mCleanupTask extends AsyncTask<Void, Void, Void> {

			private final Context mContext;
			private final ArrayList<ContentProviderOperation> mOperations;

			mCleanupTask(Context context, ArrayList<ContentProviderOperation> operations) {
				mContext = context;
				mOperations = operations;
			}
			
			@Override
			protected Void doInBackground(Void... params) {
				
				try {
					ContentProviderResult[] results = 
									mContext.getContentResolver().applyBatch(
													Api.AUTHORITY, mOperations);
					
					Debug.d(TAG, "Cleanup complete", results);
					
					// Reload data if push to backend was successful (set IDs)
					if(results.length > 0) {
						mLoaded = false;
						new LoadTask().execute();
					}
					
				} catch (Exception e) {
					Debug.e(TAG, "Unable to batch data");
				}
					
				return null;
			}
			
		};
		
	    private class LoadTask extends AsyncTask<Void, Void, Map<String,LauncherInfo>> {
	    	
			@Override
			protected Map<String,LauncherInfo> doInBackground(Void... params) {
				
				List<LauncherInfo> lis = new ArrayList<LauncherInfo>();
	        	
				Activity activity = getActivity();
	        	if(activity != null) {
	        		Dbh dbh = new Dbh(activity);
	        		lis = dbh.getInfos();
	        		dbh.close();
				}
	        	
				return listToMap(lis);
			}

			private Map<String,LauncherInfo> listToMap(List<LauncherInfo> list) {
				Map<String,LauncherInfo> lisMap = new HashMap<String, LauncherInfo>();
	        	for(LauncherInfo li:list)
	        		lisMap.put(li.packageName, new LauncherInfo(li));
	        	return lisMap;
			}
			
			@Override
			protected void onPostExecute(Map<String,LauncherInfo> result) {
				
				synchronized(mLis) {
					mLis = result;
				}
				
		    	mLoaded = true;
		    	Activity activity = getActivity();
		    	if(activity != null) {
	    	    	final Fragment fragment = getTargetFragment();
	    	    	if(fragment != null) {
						((AppListFragment)fragment).checkLoadState();
	    	    	}
		    	}
		    	
		    	Debug.d(TAG, "Load frag loaded");
		    	
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
