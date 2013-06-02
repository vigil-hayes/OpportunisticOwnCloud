package com.example.opportunisticowncloud;

public class LocalRemoteEntry {
	
	private static long mID;
	private static String mFileName;
	private static String mResourceName;
	private static long mFileModifiedTime;
	private static long mResourceModifiedTime;
	
	/**
	 * Set the ID
	 * @param id
	 */
	public void setID(long id) {
		mID =id;
	}
	
	/**
	 * Set the filename
	 * @param fname
	 */
	public void setFileName(String fname) {
		mFileName = fname;
	}
	
	public void setResourceName(String resname) {
		mResourceName = resname;
	}
	
	public void setFileModified(long fm) {
		mFileModifiedTime = fm;
	}
	
	public void setResourceModified(long rm) {
		mResourceModifiedTime = rm;
	}
	
	public long getID() {
		return mID;
	}
	
	public String getFileName() {
		return mFileName;
	}
	
	public String getResourceName() {
		return mResourceName;
	}
	
	public long getFileModified() {
		return mFileModifiedTime;
	}
	
	public long getReourceModified() {
		return mResourceModifiedTime;
	}

}
