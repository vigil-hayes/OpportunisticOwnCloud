package com.example.opportunisticowncloud;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

import com.googlecode.sardine.DavResource;

public class LocalToRemote implements Serializable {
	private HashMap<File, DavResource> mLocalRemoteMap;
	private long mLastSynced;
	private long mID;
	
	public LocalToRemote() {
		mLocalRemoteMap = new HashMap<File, DavResource>();
	}
	
	
	/**
	 * Returns true if the key exists in mLocalRemoteMap;
	 * returns false if not.
	 * @param key
	 * @return
	 */
	public boolean keyExists(File key) {
		return mLocalRemoteMap.containsKey(key);
	}
	
	/**
	 * Returns true if the value exists in mLocalRemoteMap;
	 * returns false if not.
	 * @param val
	 * @return
	 */
	public boolean valExists(DavResource val) {
		return mLocalRemoteMap.containsValue(val);
	}
	
	/**
	 * Add a value for specified key. If a value is already
	 * stored at that key, it will be replaced.
	 * @param key
	 * @param val
	 */
	public void addForSync(File key, DavResource val) {
		
		mLocalRemoteMap.put(key, val);
	}
	
	/**
	 * Remove a key:val pair from mLocalRemoteMap
	 * based on key value.
	 * @param key
	 */
	public void removeFromSync(File key) {
		mLocalRemoteMap.remove(key);
	}
	
	/**
	 * If any of the local files has been modifed
	 * more recently than mLastSynced, then mLocalRemoteMap
	 * needs to be synced: return true. If not, return false.
	 * @return
	 */
	public boolean needsUpload() {
		for (File key : mLocalRemoteMap.keySet()) {
			if (needsUpload(key)) {
				return true;
			} 
		}
		return false;
	}
	
	
	/**
	 * Called after files have been synced
	 * @param datetime
	 */
	public void sync() {
		Date datetime = new Date();
		mLastSynced = datetime.getTime();	
	}
	
	
	/* Private helper functions */
	
	/**
	 * Returns true if there is any key in mLocalRemoteMap
	 * that has a modified time that is newer than mLastSynced.
	 * @param key
	 * @param val
	 * @return
	 */
	public boolean needsUpload(File key) {
		if (key.lastModified() > mLastSynced) {
			/* key has been modified since the last update */
			return true;
		} else {
			/* key has not been modified since the last update */
			return false;
		}
	}

}
