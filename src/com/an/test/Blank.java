package com.an.test;

import android.app.Activity;
import android.os.Bundle;

import com.an.alternatelauncher.d.Dbh;
import com.an.debug.Debug;

/**
 * Empty activity to run tests on db
 * Remember to enable in Manifest (disable other launcher)
 *
 */
public class Blank extends Activity {
	private static final String TAG = Blank.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Debug.d(TAG, "Blank started");
		
		Dbh dbh = new Dbh(this);
//		dbh.testV3Upgrade();
//		dbh.recreate();
		dbh.close();
	}
	
	protected void onDestroy() {
		Debug.d(TAG, "Blank done");
		super.onDestroy();
	};

}
