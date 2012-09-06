package com.an.debug;

import java.util.Map;
import java.util.Set;

import android.util.Log;

/**
 * Debug helper class
 * @author Qui
 *
 */
public final class Debug {
	private Debug() {}
	
	public static String toYesNoString(boolean b) { return b ? "yes" : "no"; }
	
	public static String toSss(Object[] args) { return toSpaceSeparatedString(args); }
	public static String toSpaceSeparatedString(Object[] args) {
		StringBuilder sb = new StringBuilder();
		for(Object a : args)
			sb.append(a + " ");
		return sb.toString();
	}
	
	public static void d(final String TAG, final Map<String,?> m) {
		Set<String> keys = m.keySet();
		for(String k:keys)
			d(TAG, k + ":" + m.get(k).toString());
	}
	
	public static void d(String tag, Object... args) {
		if(Setting.d)
			Log.d(tag, toSss(args));
	}
	public static void d(String tag, String cs, Throwable t) {
		if(Setting.d) Log.d(tag, cs, t);
	}
	public static void dt(String tag, Object... args) {
		if(Setting.d)
			Log.d(tag, toSss(args) + System.currentTimeMillis());
	}
	public static void dt(String tag, String cs, Throwable t) {
		if(Setting.d) Log.d(tag, cs + ' ' + System.currentTimeMillis(), t);
	}
	
	public static void i(String tag, Object... args) {
		if(Setting.d)
			Log.i(tag, toSss(args));
	}
	public static void i(String tag, String cs, Throwable t) {
		if(Setting.d) Log.i(tag, cs, t);
	}
	public static void it(String tag, Object... args) {
		if(Setting.d) Log.i(tag, toSss(args) + System.currentTimeMillis());
	}
	public static void it(String tag, String cs, Throwable t) {
		if(Setting.d) Log.i(tag, cs + ' ' + System.currentTimeMillis(), t);
	}
	
	public static void v(String tag, Object... args) {
		if(Setting.d) 
			Log.v(tag, toSss(args));
	}
	public static void v(String tag, String cs, Throwable t) {
		if(Setting.d) Log.v(tag, cs, t);
	}
	
	public static void w(String tag, Object... args) {
		if(Setting.d) 
			Log.w(tag, toSss(args));
	}
	public static void w(String tag, String cs, Throwable t) {
		if(Setting.d) Log.w(tag, cs, t);
	}

	public static void e(String tag, Object... args) {
		if(Setting.d)
			Log.e(tag, toSss(args));
	}
	public static void e(String tag, String cs, Throwable t) {
		if(Setting.d) Log.e(tag, cs, t);
	}
	
	public static void wtf(String tag, Object... args) {
		if(Setting.d) 
			Log.wtf(tag, toSss(args));
	}
	public static void wtf(String tag, String cs, Throwable t) {
		if(Setting.d) Log.wtf(tag, cs, t);
	}

}
