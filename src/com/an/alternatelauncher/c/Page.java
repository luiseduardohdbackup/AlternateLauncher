package com.an.alternatelauncher.c;

public interface Page {
	
	public static final int LEFT = -1;
	public static final int RIGHT = 1;
	
	public void pageOut(int toDirection);
	public void pageIn(int fromDirection);
}
