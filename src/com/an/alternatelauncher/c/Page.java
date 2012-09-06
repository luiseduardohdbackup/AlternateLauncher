package com.an.alternatelauncher.c;

public interface Page {
	
	public static final int LEFT = -1;
	public static final int RIGHT = 1;
	
	/**
	 * 
	 * @param toDirection Constant {@code Page.LEFT|Page.RIGHT} indicating side of new page
	 */
	public void pageOut(int toDirection);
	
	/**
	 * 
	 * @param fromDirection Constant {@code Page.LEFT|Page.RIGHT} indicating side of previous page
	 */
	public void pageIn(int fromDirection);
}
