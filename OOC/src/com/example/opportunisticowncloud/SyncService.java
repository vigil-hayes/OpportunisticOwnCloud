package com.example.opportunisticowncloud;

import java.io.File;
import java.lang.Math;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;

import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.impl.SardineException;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import android.widget.Toast;

public class SyncService extends IntentService {
	private String TAG = "SyncService";
	private List<DavResource> resources = null;
	private String ROOT = null;
	private String USERNAME = null;
	private String PASSWORD = null;
	private String REMOTEDOWNROOT = null;
	private String LOCALDOWNROOT = null;
	private Sardine sardine = null;
	private File[] localFiles;
	private boolean opportunistic = false;
	private boolean refresh = false;
	private boolean connectchange = false;
	Handler mMainThreadHandler = null;

	public SyncService() {
		super(SyncService.class.getName());
		mMainThreadHandler = new Handler();

	}

	@Override
	protected void onHandleIntent(Intent intent) {

		SharedPreferences userDetails = PreferenceManager
				.getDefaultSharedPreferences(this.getApplicationContext());

		connectchange = intent.getBooleanExtra("ConnectivityChange", false);
		opportunistic = userDetails.getBoolean("opportunistic", false);
		refresh = intent.getBooleanExtra("Refresh", false);
		if (!opportunistic || connectchange || refresh) {

			USERNAME = userDetails.getString("user", "");
			PASSWORD = userDetails.getString("password", "");
			ROOT = userDetails.getString("server", "");
			LOCALDOWNROOT = userDetails.getString("localdirectory", "") + "/";
			REMOTEDOWNROOT = ROOT + "/";

			File localPath = new File(android.os.Environment
					.getExternalStorageDirectory().getAbsolutePath()
					+ "/owncloud".trim());
			localFiles = getFilesInDirectory(localPath);
			HashMap<String, DavResource> rootfiles = getHashMap(LOCALDOWNROOT);

			sync(localFiles, rootfiles);

		}

	}

	private HashMap<String, DavResource> getHashMap(String path) {
		HashMap<String, DavResource> davresources = new HashMap<String, DavResource>();
		new mThread().run();

		try {
			resources = sardine.list(convertPath(path));
			if (resources != null) {
				for (DavResource res : resources) {

					davresources.put(res.getName(), res);
				}
			} else {
				mMainThreadHandler.post(new Runnable() {
					@Override
					public void run() {
						Toast toast = Toast.makeText(getApplicationContext(),
								"Connection Error - Check Settings",
								Toast.LENGTH_SHORT);
						toast.show();
					}
				});
			}

		} catch (NoHttpResponseException nhre) {
			nhre.printStackTrace();
			// TODO
			return null;

		} catch (SSLPeerUnverifiedException pue) {
			pue.printStackTrace();
			// TODO Redirect

			return null;
		} catch (SardineException se) {
			se.printStackTrace();
			Log.e("SyncService", se.getMessage());
			resources = null;
			stopSelf();
		} catch (Exception e) {
			// TODO Auto-generated catch block.
			e.printStackTrace();
			stopSelf();

		}
		if (resources == null) {
			stopSelf();
			return null;
		}
		return davresources;
	}

	public void upFile(File file) {
		InputStream fis;
		Log.i(TAG, "Uploading: " + file.getName());
		String path = convertPath(file.getPath());
		try {
			fis = new FileInputStream(file);
			if (file.isDirectory()) {
				Log.e("creating remote dir", REMOTEDOWNROOT + file.getName());
				sardine.createDirectory(REMOTEDOWNROOT + file.getName());
			} else {
				sardine.put(path, fis);
			}
			fis.close();
		} catch (SocketException se) {
			se.printStackTrace();
		} catch (ClientProtocolException cpe) {
			cpe.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<DavResource> resourcelist = null;
		try {
			resourcelist = sardine.list(REMOTEDOWNROOT);
		} catch (Exception e) {
			// TODO Auto-generated catch block.
			e.printStackTrace();
		}
		for (DavResource res : resourcelist) {
			if (file.getName().equals(res.getName())) {
				file.setLastModified(res.getModified().getTime());
			}
		}
	}

	public void downFile(String path, DavResource file) {
		Log.i(TAG, "Downloading: " + file.getName());
		String fileName = convertPath(path);

		long modified = file.getModified().getTime();
		try {
			File destDir = new File(LOCALDOWNROOT);
			if (!destDir.exists()) {
				destDir.mkdirs();
			}
			File outputFile = new File(path);

			InputStream fis = sardine.get(fileName.replace(" ", "%20"));
			FileOutputStream fos = new FileOutputStream(outputFile);
			byte[] buffer = new byte[1444];
			int byteread = 0;
			while ((byteread = fis.read(buffer)) != -1) {
				fos.write(buffer, 0, byteread);
			}
			// force time to match server's - to avoid a future upload due to
			// device clock mismatch
			outputFile.setLastModified(modified);
			fis.close();
			fos.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sync(File[] filelist, HashMap<String, DavResource> davresources) {
		for (File f : filelist) {
			try {
				if (davresources.containsKey(f.getName())) {
					File[] newlist = getFilesInDirectory(f);
					if (f.isDirectory()) {
						// File is a directory. Recursively sync.
						sync(newlist, getHashMap(f.getPath()));
					} else {
						int comparecode = compareDates(
								davresources.get(f.getName()), f);
						if (comparecode == 1) {
							// File on device is newer. Upload
							UploadThread upThread = new UploadThread(f);
							upThread.run();
						} else if (comparecode == 2) {
							// File on server is newer. Download
							Log.e("ATTEMPTING TO DOWNLOAD!", f.getName());
							Log.e(String.valueOf(f.lastModified()),
									String.valueOf(davresources
											.get(f.getName()).getModified()
											.getTime()));
							DownloadThread downThread = new DownloadThread(
									f.getPath(), davresources.get(f.getName()));
							downThread.run();

						} else if (comparecode == -1) {
							Log.e(TAG, "Comparison Error");
						}
					}
				} else {
					// File no on server. Upload it
					UploadThread upThread = new UploadThread(f);
					upThread.run();
					if (f.isDirectory()) {
						// File is a directory. Recursively call sync.
						File[] newlist = getFilesInDirectory(f);
						sync(newlist, getHashMap(f.getPath()));

					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				stopSelf();
			}
		}

	}

	private int compareDates(DavResource dav, File file) {
		// compare the modified dates and return whether to upload, download, do
		// nothing, or error (-1)
		if (file.lastModified() - dav.getModified().getTime() > 1000) {
			return 1;
		} else if (dav.getModified().getTime() - file.lastModified() > 1000) {
			return 2;
		} else if (Math.abs(file.lastModified() - dav.getModified().getTime()) <= 1000) {
			return 3;
		}
		return -1;
	}

	private File[] getFilesInDirectory(File file) {
		File[] filelist = new File[0];
		if (file.isDirectory()) {
			filelist = file.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return !name.toLowerCase().startsWith(".");
				}
			});

		}
		return filelist;
	}

	private String convertPath(String file) {
		String remotepath = REMOTEDOWNROOT;
		String[] directory = file.split(LOCALDOWNROOT);
		if (directory.length > 0) {
			remotepath = remotepath + directory[1];
		}
		return remotepath;
	}

	class mThread extends Thread {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			try {
				sardine = SardineFactory.begin(USERNAME, PASSWORD);
			} catch (Exception e) {
				e.printStackTrace();
				stopSelf();
			}
		}

	}

	class DownloadThread implements Runnable {
		private String threadpath;
		private DavResource threaddav;

		public DownloadThread(String path, DavResource file) {
			threadpath = path;
			threaddav = file;
		}

		public void run() {
			downFile(threadpath, threaddav);
		}

	}

	class UploadThread implements Runnable {
		private File threadFile;

		public UploadThread(File file) {
			threadFile = file;
		}

		public void run() {
			upFile(threadFile);
		}

	}

}
