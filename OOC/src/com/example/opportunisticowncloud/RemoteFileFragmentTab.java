package com.example.opportunisticowncloud;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.NoHttpResponseException;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;

import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.impl.SardineException;

public class RemoteFileFragmentTab extends SherlockFragment implements
ActionBar.TabListener {
	private Fragment mFragment;
	/**
	 * global private
	 */
	private static final int DOWNLOAD_TOAST = 2;
	private static final int DELETE_TOAST = 3;
	private static final int PASTE_TOAST = 7;
	private String ROOT = null;//"http://128.111.52.223/owncloud/files/webdav.php/";
	private String USERNAME = null;//"testuser";
	private String PASSWORD = null;
	private String SERVERROOT = null;//"test";
	private String REMOTEDOWNROOT = null;//ROOT + "/";
	private String LOCALDOWNROOT = null;//Environment.getExternalStorageDirectory().getPath() + "/owncloud/";
	private List<DavResource> resources = null;
	private Sardine sardine = null;
	private int dept = 0;
	private String SOURCEURL = null; // Just for copy and move
	private String DESTINATIONFILE = null; // Just for copy and move
	private boolean isExistRootCache = false;
	private int pasteMode = 0; // 0: copy; 1: move

	Resources localResources = null;

	private final int CREATE_FOLDER = 1;
	private final int PASTE_FILE = 2;
	private int mPresentDown = 0;

	private int mPictures[];

	// private String tag = "ownClient-ServerSide";

	private ListView mFileDirList;

	private ArrayList<HashMap<String, Object>> recordItem;

	BroadcastReceiver mExternalStorageReceiver;
	private ProgressDialog progressDialog;
	
	private ArrayList<DavResource> list = null;

	// private OnFragmentInteractionListener mListener;

	/**
	 * The fragment's ListView/GridView.
	 */
	// private AbsListView mListView;

	/**
	 * The Adapter which will be used to populate the ListView/GridView with
	 * Views.
	 */
	private ListAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= 9) {
		    try {
		        // StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
		           Class<?> strictModeClass = Class.forName("android.os.StrictMode", true, Thread.currentThread()
		                        .getContextClassLoader());
		           Class<?> threadPolicyClass = Class.forName("android.os.StrictMode$ThreadPolicy", true, Thread.currentThread()
		                        .getContextClassLoader());
		           Field laxField = threadPolicyClass.getField("LAX");
		           Method setThreadPolicyMethod = strictModeClass.getMethod("setThreadPolicy", threadPolicyClass);
		                setThreadPolicyMethod.invoke(strictModeClass, laxField.get(null));
		    } 
		    catch (Exception e) { }
		}
		
		SharedPreferences userDetails = PreferenceManager
				.getDefaultSharedPreferences(this.getActivity().getApplicationContext());
		USERNAME = userDetails.getString("user", "");
		PASSWORD = userDetails.getString("password", "");
		ROOT = userDetails.getString("server", "");
		LOCALDOWNROOT = userDetails.getString("localdirectory", "") + "/";
		REMOTEDOWNROOT = ROOT + "/";
		SERVERROOT = ROOT + "/";
		
		// Get the view from fragment1.xml
		getActivity().setContentView(R.layout.fragment_file_list);

		initVariable();
		localResources = getActivity().getResources();
		progressDialog = ProgressDialog.show(getActivity(),
				localResources.getString(R.string.load_dialog_tile),
				localResources.getString(R.string.dialog_mess));
		new mThread().run();
	}

	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		mFragment = new RemoteFileFragmentTab();
		
		// Attach fragment1.xml layout
		ft.add(android.R.id.content, mFragment);
		ft.attach(mFragment);
	}

	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		// Remove fragment1.xml layout

		ft.remove(mFragment);
	}

	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub

	}

	/*
	 * =============================== BEGIN LISTENERS
	 * ===============================
	 */
	class ClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			mPresentDown = arg2;
			if (recordItem.get(mPresentDown).get("type").toString()
					.equalsIgnoreCase("file")) {
				createFunctionDialog().show();
			} else {
				changeDirectory();
			}

		}

	}

	class LongClickListener implements OnItemLongClickListener {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			if (arg2 > 0) {
				mPresentDown = arg2;
				createFunctionDialog().show();
			}
			return false;
		}

	}

	/*
	 * =============================== END LISTENERS
	 * ===============================
	 */

	/*
	 * =============================== BEGIN HANDLERS
	 * ===============================
	 */
	public static final class ComparatorValues implements
	Comparator<HashMap<String, Object>> {

		@Override
		public int compare(HashMap<String, Object> object1,
				HashMap<String, Object> object2) {

			int result = 0;
			String type1 = (String) object1.get("type");
			String type2 = (String) object2.get("type");
			String name1 = (String) object1.get("name");
			String name2 = (String) object2.get("name");
			if (type1.equals("currdirectory") || type2.equals("currdirectory")) {
				if (type1.equals("currdirectory")) {
					result = -1;
				} else {
					result = 1;
				}
			} else if (type1.equals("httpd/unix-directory")
					&& type2.equals("httpd/unix-directory")) {
				if (name1.compareTo(name2) > 0) {
					result = 1;
				} else {
					result = -1;
				}
			} else if (type1.equals("httpd/unix-directory")
					|| type2.equals("httpd/unix-directory")) {
				if (type1.equals("httpd/unix-directory")) {
					result = -1;
				} else {
					result = 1;
				}
			} else {
				if (name1.compareTo(name2) > 0) {
					result = 1;
				} else {
					result = -1;
				}
			}

			return result;

		}
	}

	private Handler handler = new Handler() {
		public void handleMessage(Message message) {
			progressDialog.dismiss();
			listFile();
			switch (message.what) {
			case 2:
				Toast.makeText(getActivity(),
						localResources.getString(R.string.download_toast),
						Toast.LENGTH_SHORT).show();
				break;
			case 3:
				Toast.makeText(getActivity(),
						localResources.getString(R.string.delete_toast),
						Toast.LENGTH_SHORT).show();
				break;
			case 7:
				Toast.makeText(getActivity(),
						localResources.getString(R.string.paste_toast),
						Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	/*
	 * =============================== END HANDLERS
	 * ===============================
	 */

	/*
	 * ====================== BEGIN HELPER FUNCTIONS ======================
	 */
	public void initVariable() {
		mFileDirList = (ListView) getActivity().findViewById(R.id.mFileList);
		mPictures = new int[] { R.drawable.back_icon, R.drawable.folder_icon,
				R.drawable.file_icon };
	}

	public void connecting() {
		sardine = SardineFactory.begin(USERNAME, PASSWORD);
	}

	public void listFile() {
		//try {
			//resources = sardine.list(REMOTEDOWNROOT);
			new listThread().run();
//
//		} catch (NoHttpResponseException nhre) {
//			nhre.printStackTrace();
//			// TODO
//			return;
//
//		} catch (SSLPeerUnverifiedException pue) {
//			pue.printStackTrace();
//			// TODO Redirect
//
//			return;
//		} catch (SardineException se) {
//			se.printStackTrace();
//			Log.e(getTag(), se.getMessage());
//		} catch (Exception e) {
//			// TODO Auto-generated catch block.
//			e.printStackTrace();
//
//		}
		if (resources == null) {
			error();
			return;
		}
		fillFile();
	}

	public void fillFile() {
		
		recordItem = null;
		recordItem = new ArrayList<HashMap<String, Object>>();
		list = new ArrayList<DavResource>();
		int count = 0;
		SimpleAdapter adapter;

		for (DavResource res : resources) {
			String type = res.getContentType();
			if (type == null){type = "file";}
			
			String relativepath = res.getPath();
			String name = res.getName();
			try{
				relativepath = convertPath(relativepath);
				
				}
			catch(Exception e){}
			
			//HashMap<String, Object> map = new HashMap<String, Object>();
			if (count == 0) { // Empty WebDAV 
				count++;
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("picture", mPictures[0]);
				map.put("type", "currdirectory");
				map.put("name", name);

				recordItem.add(map);
			} else if (res.getPath().endsWith("/") || type.equals(
					"httpd/unix-directory")) {
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("picture", mPictures[1]);
				map.put("type", "httpd/unix-directory");
				map.put("name", name);
				recordItem.add(map);
			}else if (type.equals("file")) {
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("picture", mPictures[2]);
				map.put("type", "file");
				map.put("name", name);
				recordItem.add(map);} 
			else {
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("picture", mPictures[2]);
				map.put("type", "file");
				map.put("name", name);
				recordItem.add(map);
			}
		}
		//Collections.sort(recordItem, new ComparatorValues());

		//mAdapter = new ArrayAdapter<DavResource>(getActivity(),
				// R.layout.rowbuttonlayout, android.R.id.text1,
		//		android.R.layout.simple_list_item_1, android.R.id.text1,
		//		resources);
		adapter = new SimpleAdapter(getActivity(), recordItem, R.layout.local_item, new String[] {"picture", "name"}, new int[]{ R.id.local_picture, R.id.local_text });
		
		mFileDirList.setAdapter(adapter);
		mFileDirList.setOnItemLongClickListener(new LongClickListener());
		mFileDirList.setOnItemClickListener(new ClickListener());
	}

	// private HashMap<String,LocalRemoteEntry>
	// createLRENameMap(List<LocalRemoteEntry> list) {
	// HashMap<String,LocalRemoteEntry> entrymap = new
	// HashMap<String,LocalRemoteEntry>();
	// for (LocalRemoteEntry lre : list) {
	// entrymap.put(lre.getFileName(), lre);
	// }
	//
	// return entrymap;
	// }

	private void error() {
		final Resources localResources = this.getResources();
		AlertDialog.Builder builder = new Builder(getActivity());
		builder.setMessage(localResources.getString(R.string.error));
		builder.setTitle(localResources.getString(R.string.prompt));
		builder.setPositiveButton(localResources.getString(R.string.ok),
				new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				Intent localIntent = new Intent();
				localIntent.setClass(getActivity(), MainActivity.class);
				getActivity().startActivity(localIntent);
				getActivity().finish();
			}
		});
		builder.create().show();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		// super.onCreateOptionsMenu(menu);
		menu.add(0, CREATE_FOLDER, 0, R.string.create_folder).setIcon(
				android.R.drawable.ic_menu_add);
		menu.add(0, PASTE_FILE, 0, R.string.paste).setIcon(
				android.R.drawable.ic_menu_set_as);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case CREATE_FOLDER:
			createDialog().show();
			break;
		case PASTE_FILE:
			PasteFile();
			break;
		}

		return true;
	}

	public Dialog createDialog() {
		AlertDialog.Builder builder = new Builder(getActivity());
		final View layout = View.inflate(getActivity(),
				R.layout.create_new_folder, null);
		final EditText localFileName = (EditText) layout
				.findViewById(R.id.folder_name);

		builder.setTitle(this.getResources().getString(R.string.create_folder));
		builder.setView(layout);
		builder.setPositiveButton(localResources.getString(R.string.ok),
				new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				try {
					sardine.createDirectory(ROOT
							+ localFileName.getText().toString().trim());
				} catch (IOException e) {
					e.printStackTrace();
				}
				listFile();
				dialog.dismiss();
			}
		});

		builder.setNegativeButton(localResources.getString(R.string.cancel),
				new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		return builder.create();
	}

	public Dialog createFunctionDialog() {
		AlertDialog.Builder builder = new Builder(getActivity());
		String[] localArray = { localResources.getString(R.string.download) /*
		 * Add
		 * future
		 * functionality
		 */};
		builder.setItems(localArray, new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0:
					progressDialog = ProgressDialog
					.show(getActivity(), localResources
							.getString(R.string.load_dialog_tile),
							localResources
							.getString(R.string.dialog_mess));
					new DownLoadThread().run();
					break;
				}
			}

		});
		return builder.create();
	}

	public void changeDirectory() {
		SharedPreferences userDetails = PreferenceManager
				.getDefaultSharedPreferences(this.getActivity().getApplicationContext());
		progressDialog = ProgressDialog.show(getActivity(),
				localResources.getString(R.string.load_dialog_tile),
				localResources.getString(R.string.dialog_mess));
		String selectedFile = (String) recordItem.get(mPresentDown).get("name");
		if (mPresentDown == 0 && dept > 0) {
			dept--;
			REMOTEDOWNROOT = REMOTEDOWNROOT.replaceAll(selectedFile + "/", "");
		} else if (recordItem.get(mPresentDown).get("type").toString()
				.equalsIgnoreCase("httpd/unix-directory")
				&& mPresentDown > 0) {
			dept++;
			REMOTEDOWNROOT = REMOTEDOWNROOT + selectedFile + "/";
		}
		
		listFile();
		
		Message msg_listData = new Message();
		handler.sendMessageDelayed(msg_listData, 500);
		
	}

	public void deleteFileFromDir(String fileName, String type) {
		String destDel = (ROOT + fileName).replaceAll(" ", "%20");
		if (type.equals("httpd/unix-directory")) {
			destDel = destDel + "/";
		}
		try {
			sardine.delete(destDel);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void download(String fileName, String type) {
		int tmpCount = 0;
		if (type == null){type = "file";}
		if (fileName.endsWith("/") || type.equalsIgnoreCase("httpd/unix-directory")) {
			REMOTEDOWNROOT = ROOT + "/" + fileName + "/";
			
			List<DavResource> downList = null;
			try {
				downList = sardine.list(REMOTEDOWNROOT);
			} catch (Exception e) {
				e.printStackTrace();
			}
			for (DavResource res : downList) {
				if (tmpCount == 0) {
					tmpCount++;
					continue;
				}
				String filetype = res.getContentType();
				if (filetype == null){filetype = "file";}
				
				if (res.getPath().endsWith("/") || filetype.equals("httpd/unix-directory")) {
					String fileName2 = res.getName();
					try{fileName2 = convertPath(res.getPath());}catch(Exception e){}
					DirDownLoadThread dthread = new DirDownLoadThread(fileName2, res.getContentType());
					dthread.run();
				} else {
					String relativepath = res.getPath();
					try{
						relativepath = convertPath(relativepath);
					}catch (Exception e){}
					downFile(relativepath);
				}
			}
		} else {
			downFile(fileName);
		}
	}

	public void downFile(String fileName) {
		try {
			
			File destDir = new File(LOCALDOWNROOT + fileName);
			if (!destDir.exists()) {
				destDir.getParentFile().mkdirs();
			}
			//File outputFile = new File(destDir,);

			InputStream fis = sardine.get(SERVERROOT
					+ fileName.replace(" ", "%20"));
			FileOutputStream fos = new FileOutputStream(destDir);
			byte[] buffer = new byte[1444];
			int byteread = 0;
			while ((byteread = fis.read(buffer)) != -1) {
				fos.write(buffer, 0, byteread);
			}
			fis.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * File management functions
	 */
	public Dialog RenameFile(final String fileName) {
		AlertDialog.Builder builder = new Builder(getActivity());
		final View layout = View.inflate(getActivity(),
				R.layout.create_new_folder, null);
		final EditText localFileName = (EditText) layout
				.findViewById(R.id.folder_name);
		localFileName.setText(fileName);

		builder.setTitle(this.getResources().getString(R.string.rename_prompt));
		builder.setView(layout);
		builder.setPositiveButton(localResources.getString(R.string.ok),
				new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				try {
					sardine.move(
							ROOT + fileName.replaceAll(" ", "%20"),
							ROOT
							+ localFileName.getText()
							.toString()
							.replaceAll(" ", "%20"));
				} catch (IOException e) {
					e.printStackTrace();
				}
				listFile();
				Toast.makeText(getActivity(),
						ROOT + localFileName.getText(),
						Toast.LENGTH_LONG).show();
				dialog.dismiss();
			}
		});

		builder.setNegativeButton(localResources.getString(R.string.cancel),
				new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		return builder.create();
	}

	public void CopyFile(String fileName, String type) {
		if (type.equalsIgnoreCase("httpd/unix-directory")) {
			SOURCEURL = ROOT + fileName.replace(" ", "%20") + "/";
			DESTINATIONFILE = fileName.replace(" ", "%20") + "/";
		} else {
			SOURCEURL = ROOT + fileName.replace(" ", "%20");
			DESTINATIONFILE = fileName.replace(" ", "%20");
		}
		isExistRootCache = true;
		pasteMode = 0;
		Toast.makeText(getActivity(),
				localResources.getString(R.string.copy_toast) + fileName,
				Toast.LENGTH_SHORT).show();
	}

	public void MoveFile(String fileName, String type) {
		if (type.equalsIgnoreCase("httpd/unix-directory")) {
			SOURCEURL = ROOT + fileName.replace(" ", "%20") + "/";
			DESTINATIONFILE = fileName.replace(" ", "%20") + "/";
		} else {
			SOURCEURL = ROOT + fileName.replace(" ", "%20");
			DESTINATIONFILE = fileName.replace(" ", "%20");
		}
		isExistRootCache = true;
		pasteMode = 1;
		Toast.makeText(getActivity(),
				localResources.getString(R.string.move_toast) + fileName,
				Toast.LENGTH_SHORT).show();
	}

	public void PasteFile() {
		if (!isExistRootCache) {
			Toast.makeText(getActivity(),
					localResources.getString(R.string.cannotpaste_toast),
					Toast.LENGTH_SHORT).show();
			return;
		}
		progressDialog = ProgressDialog.show(getActivity(),
				localResources.getString(R.string.load_dialog_tile),
				localResources.getString(R.string.dialog_mess));
		isExistRootCache = false;
		switch (pasteMode) {
		case 0:
			try {
				sardine.copy(SOURCEURL, ROOT + DESTINATIONFILE);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case 1:
			try {
				sardine.move(SOURCEURL, ROOT + DESTINATIONFILE);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}
		Message msg_listData = new Message();
		msg_listData.what = PASTE_TOAST;
		handler.sendMessageDelayed(msg_listData, 500);

	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent localIntent = new Intent();
			localIntent.setClass(getActivity(), MainActivity.class);
			getActivity().startActivity(localIntent);
			getActivity().finish();
		}
		return false;
	}

	/*
	 * =============================== END HELPER FUNCTIONS
	 * ===============================
	 */
	/*
	 * ==================================== BEGIN RUNNABLE
	 * ====================================
	 */
	class DownLoadThread implements Runnable {

		public void run() {
			download((String) recordItem.get(mPresentDown).get("name"),
					(String) recordItem.get(mPresentDown).get("type"));
			Message msg_listData = new Message();
			msg_listData.what = DOWNLOAD_TOAST;
			handler.sendMessageDelayed(msg_listData, 500);
		}

	}
	class DirDownLoadThread implements Runnable {
		private String threadname;
		private String threadtype;
		public DirDownLoadThread(String name, String type){
			threadname = name;
			threadtype = type;
		}
		public void run() {
			download(threadname,threadtype);
			Message msg_listData = new Message();
			msg_listData.what = DOWNLOAD_TOAST;
			handler.sendMessageDelayed(msg_listData, 500);
		}

	}
//	class DownloadThread implements Runnable {
//		private String threadpath;
//		private DavResource threaddav;
//
//		public DownloadThread(String path, DavResource file) {
//			threadpath = path;
//			threaddav = file;
//		}
//
//		public void run() {
//			downFile(threadpath, threaddav);
//		}
//
//	}

	class DeleteFileThread implements Runnable {

		public void run() {
			deleteFileFromDir(
					(String) recordItem.get(mPresentDown).get("name"),
					(String) recordItem.get(mPresentDown).get("type"));
			Message msg_listData = new Message();
			msg_listData.what = DELETE_TOAST;
			handler.sendMessageDelayed(msg_listData, 500);
		}

	}

	class mThread extends Thread {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			try {
				connecting();
				Message msg_listData = new Message();
				handler.sendMessageDelayed(msg_listData, 500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	/*
	 * ==================================== END RUNNABLE
	 * ====================================
	 */
	private String convertPath(String file) throws MalformedURLException {
		String filepath = "/";
		URL url = new URL(SERVERROOT);
		String[] directory = file.split(url.getPath());
		if (directory.length > 0) {
			filepath = filepath + directory[1];
		}
		return filepath;
	}
	class listThread extends Thread{
		@Override
		public void run(){
			super.run();
			try{
				resources = sardine.list(REMOTEDOWNROOT);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}

}
