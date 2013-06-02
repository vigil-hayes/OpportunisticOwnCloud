package com.example.opportunisticowncloud;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

import com.googlecode.sardine.DavResource;

public class RemoteToLocal implements Serializable {

	private HashMap<DavResource, File> mRemoteLocalMap;
	private long mLastSynced;
	
	public RemoteToLocal() {
		mRemoteLocalMap = new HashMap<DavResource, File>();
	}
	
	/**
	 * Returns true if the key exists in mRemoteLocalMap;
	 * returns false if not.
	 * @param key
	 * @return
	 */
	public boolean keyExists(DavResource key) {
		return mRemoteLocalMap.containsKey(key);
	}
	
	/**
	 * Returns true if the value exists in mRemoteLocalMap;
	 * returns false if not.
	 * @param val
	 * @return
	 */
	public boolean valExists(File val) {
		return mRemoteLocalMap.containsValue(val);
	}
	
	/**
	 * Add a value for specified key. If a value is already
	 * stored at that key, it will be replaced.
	 * @param key
	 * @param val
	 */
	public void addForSync(DavResource key, File val) {
		
		mRemoteLocalMap.put(key, val);
	}
	
	/**
	 * Remove a key:val pair from mRemoteLocalMap
	 * based on key value.
	 * @param key
	 */
	public void removeFromSync(DavResource key) {
		mRemoteLocalMap.remove(key);
	}
	
	/**
	 * If any of the local files has been modifed
	 * more recently than mLastSynced, then mRemoteLocalMap
	 * needs to be synced: return true. If not, return false.
	 * @return
	 */
	public boolean needsDownload() {
		for (DavResource key : mRemoteLocalMap.keySet()) {
			if (needsDownload(key)) {
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
	 * Returns true if there is any key in mRemoteLocalMap
	 * that has a modified time that is newer than mLastSynced.
	 * @param key
	 * @return
	 */
	public boolean needsDownload(DavResource key) {
		if (key.getModified().getTime() > mLastSynced) {
			/* key has been modified since the last update */
			return true;
		} else {
			/* key has not been modified since the last update */
			return false;
		}
	}
	

}
