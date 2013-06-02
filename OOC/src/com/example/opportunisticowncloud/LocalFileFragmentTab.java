package com.example.opportunisticowncloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

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
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;

import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;

public class LocalFileFragmentTab extends SherlockFragment implements
		ActionBar.TabListener {
	public static final int UPLOAD_TOAST = 1;
	private Fragment mFragment;
	/**
	 * global private
	 */
	private String ROOT = null;
	private String USERNAME = null;
	private String PASSWORD = null;
	private String REMOTEDOWNROOT = null;
	private String LOCALDOWNROOT = null;
	private Sardine sardine = null;

	Resources localResources = null;
	
	private int mPresentClick = 0;

	private int mPictures[];

	private String tag = "ownClient-ClientSide";

	private ListView mFileDirList;

	private File mPresentFile;
	private List<File> list = null;
	private ArrayList<HashMap<String, Object>> recordItem;
	private File[] localFiles;

	BroadcastReceiver mExternalStorageReceiver;
	static UpdateUiHandler mUpdateUiHandler;
	private ProgressDialog progressDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences userDetails = PreferenceManager
				.getDefaultSharedPreferences(this.getActivity()
						.getApplicationContext());
		USERNAME = userDetails.getString("user", "");
		PASSWORD = userDetails.getString("password", "");
		ROOT = userDetails.getString("server", "");
		REMOTEDOWNROOT = ROOT + "/";
		LOCALDOWNROOT = userDetails.getString("localdirectory", "");

		// Get the view from fragment1.xml
		getActivity().setContentView(R.layout.fragment_file_list);
		initVariable();
		createDirectory();
		listFile();
		localResources = this.getResources();

	}

	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		mFragment = new LocalFileFragmentTab();
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
	 * ================================== BEGIN LISTENERS
	 * ==================================
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent localIntent = new Intent();
			localIntent.setClass(getActivity(), MainActivity.class);
			getActivity().startActivity(localIntent);
			getActivity().finish();
		}
		return false;
	}

	private class FileChooserListener implements OnItemClickListener {

		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			File file = list.get(arg2);
			mPresentFile = file;
			if (file.isDirectory()) {
				getActivity().setTitle(file.getAbsolutePath());
				File[] files = file.listFiles();
				fillFile(files);
			}
		}
	}

	class LongClickListener implements OnItemLongClickListener {

		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			// TODO Auto-generated method stub
			mPresentClick = arg2;
			createFunctionDialog().show();
			return false;
		}

	}

	/*
	 * ================================== END LISTENERS
	 * ==================================
	 */
	/*
	 * ================================== BEGIN HANDLER CLASSES
	 * ==================================
	 */
	private Handler handler = new Handler() {
		public void handleMessage(Message message) {
			progressDialog.dismiss();
			listFile();
			if (message.what == 1) {
				Toast.makeText(getActivity(),
						localResources.getString(R.string.upload_toast),
						Toast.LENGTH_SHORT).show();
			}
		}
	};

	public class UpdateUiHandler extends Handler {
		public void handleMessage(final Message msg) {
			getActivity().setTitle("OwnCloud" + msg.what + "%");
		}
	}

	/*
	 * ==================================== END HANDLER CLASSES
	 * ====================================
	 */
	/*
	 * ==================================== BEGIN HELPER FUNCTIONS
	 * ====================================
	 */

	public void initVariable() {
		mFileDirList = (ListView) getActivity().findViewById(R.id.mFileList);
		mPictures = new int[] { R.drawable.back_icon, R.drawable.folder_icon,
				R.drawable.file_icon };
		mUpdateUiHandler = new UpdateUiHandler();
		sardine = SardineFactory.begin(USERNAME, PASSWORD);

	}

	public void createDirectory() {
		File localPath = new File(LOCALDOWNROOT);

		if (localPath.exists()) {
			Log.i(tag, "localPath.exists");
			mPresentFile = localPath;
			if (!localPath.exists()) {
				Log.i(tag, "mkdir");
				localPath.mkdirs();
			}
		}
	}

	public void listFile() {
		File localPath = new File(LOCALDOWNROOT);
		localFiles = localPath.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.toLowerCase().startsWith(".");
			}
		});
		Arrays.sort(localFiles, new Comparator<File>() {

			@Override
			public int compare(File f1, File f2) {
				int result = 0;
				if (isRegularFile(f1) == 0 && isRegularFile(f2) == 0) {
					if (f1.getName().compareTo(f2.getName()) > 0) {
						result = 1;
					} else {
						result = -1;
					}
				} else if (isRegularFile(f1) == 0 || isRegularFile(f2) == 0) {
					if (isRegularFile(f1) == 0) {
						result = -1;
					} else {
						result = 1;
					}
				} else {
					if (f1.getName().compareTo(f2.getName()) > 0) {
						result = 1;
					} else {
						result = -1;
					}
				}
				return result;
			}

		});
		getActivity().setTitle(localPath.getAbsolutePath());
		fillFile(localFiles);
	}

	public void fillFile(File[] paramFiles) {
		SimpleAdapter adapter = null;
		recordItem = new ArrayList<HashMap<String, Object>>();
		list = new ArrayList<File>();

		for (File f : paramFiles) {

			if (isRegularFile(f) == 0) {
				list.add(f);
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("picture", mPictures[1]);
				map.put("name", f.getName());
				recordItem.add(map);

			}
			if (isRegularFile(f) == 1) {
				list.add(f);
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("picture", mPictures[2]);
				map.put("name", f.getName());
				recordItem.add(map);

			}
		}
		adapter = new SimpleAdapter(getActivity(), recordItem,
				R.layout.local_item, new String[] { "picture", "name" },
				new int[] { R.id.local_picture, R.id.local_text });
		mFileDirList.setAdapter(adapter);
		mFileDirList.setOnItemClickListener(new FileChooserListener());
		mFileDirList.setOnItemLongClickListener(new LongClickListener());
	}

	private int isRegularFile(File f) {
		if (f.isDirectory()) {
			return 0;
		} else {
			return 1;
		}
	}

	public void uploadFile() {
		InputStream fis;
		File localPath = new File(LOCALDOWNROOT);
		/* Get the local files */
		localFiles = localPath.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				/* Filter out files beginning with . */
				return !name.toLowerCase().startsWith(".");
			}
		});
		Arrays.sort(localFiles, new Comparator<File>() {

			@Override
			public int compare(File f1, File f2) {
				int result = 0;
				if (isRegularFile(f1) == 0 && isRegularFile(f2) == 0) {
					if (f1.getName().compareTo(f2.getName()) > 0) {
						result = 1;
					} else {
						result = -1;
					}
				} else if (isRegularFile(f1) == 0 || isRegularFile(f2) == 0) {
					if (isRegularFile(f1) == 0) {
						result = -1;
					} else {
						result = 1;
					}
				} else {
					if (f1.getName().compareTo(f2.getName()) > 0) {
						result = 1;
					} else {
						result = -1;
					}
				}
				return result;
			}

		});
		File t = localFiles[mPresentClick];
		try {
			fis = new FileInputStream(t);
			sardine.put(REMOTEDOWNROOT + t.getName(), fis);
			fis.close();
		} catch (FileNotFoundException e1) {
			Log.e(tag, t.getName());
			e1.printStackTrace();
		} catch (SocketException se) {
			se.printStackTrace();
		} catch (ClientProtocolException cpe) {
			cpe.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TODO deal with mLocalRemote
	}

	public Dialog createFunctionDialog() {

		AlertDialog.Builder builder = new Builder(getActivity());
		String[] localArray = { localResources.getString(R.string.upload_file) /*
																				 * Add
																				 * delete
																				 * at
																				 * a
																				 * later
																				 * time
																				 */};
		builder.setItems(localArray, new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0:
					progressDialog = ProgressDialog.show(getActivity(),
							localResources
									.getString(R.string.upload_dialog_tile),
							localResources.getString(R.string.dialog_mess));
					new UpLoadThread().run();
					break;
				}
			}

		});
		return builder.create();
	}

	/*
	 * ==================================== END HELPER FUNCTIONS
	 * ====================================
	 */
	/*
	 * ==================================== BEGIN RUNNABLE
	 * ====================================
	 */
	class UpLoadThread implements Runnable {

		public void run() {
			uploadFile();
			Message msg_uploadedData = new Message();
			msg_uploadedData.what = UPLOAD_TOAST;
			handler.sendMessageDelayed(msg_uploadedData, 500);
		}

	}

	/*
	 * ==================================== END RUNNABLE
	 * ====================================
	 */
}
