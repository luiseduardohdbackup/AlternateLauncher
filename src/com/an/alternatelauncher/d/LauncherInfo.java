package com.an.alternatelauncher.d;

import com.an.debug.Debug;

public class LauncherInfo implements Comparable<LauncherInfo> {

	public final String packageName;
	public final String label0;
	public final String label;
	public final boolean show;
	
	public LauncherInfo(LauncherInfo item) {
		this(item.packageName, item.label0, item.label, item.show);
	}
	public LauncherInfo(AppEntry entry) {
		this(entry.packageName, entry.label0, entry.getLabel(), entry.getShow());
	}
	public LauncherInfo(String packageName, String label0, String label, boolean show) {
		this.packageName = packageName;
		this.label0 = label0;
		this.label = label;
		this.show = show;
	}
	
	public int compareTo(LauncherInfo another) {
		return this.show && !another.show ? -1 
						: another.show && !this.show ? 1
										: label0.compareTo(another.label0);
	}
	
	/**
	 * Only label and show are compared because the info should have been selected by package name
	 * 	so comparison only needs to be made on the remaining state variables
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + (show ? 1231 : 1237);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LauncherInfo other = (LauncherInfo) obj;
		if (show != other.show)
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("%1$s %2$s %3$s %4$s", packageName, label0, label, Debug.toYesNoString(show));
	}
	
}
