package com.example.opportunisticowncloud;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.opportunisticowncloud.SyncService.mThread;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends SherlockFragmentActivity {

	Tab tab;
	private final static String tag = "MainActivity";
	private Calendar cal = Calendar.getInstance();
	private Sardine sardine = null;
	private String USERNAME = null;
	private String PASSWORD = null;
	private String REMOTEDOWNROOT = null;


	@Override
	public void onCreate(Bundle savedInstanceState) {
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
		super.onCreate(savedInstanceState);
		getIntent().setAction("Already created");
		SharedPreferences userDetails = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		boolean firstrun = userDetails.getBoolean("firstrun", true);
		
		if (firstrun) {
			Intent intent = new Intent(MainActivity.this,
					SettingsActivity.class);
			startActivity(intent);
			userDetails.edit().putBoolean("firstrun", false).commit();
			
			
		}
		else{
		USERNAME = userDetails.getString("user", "");
		PASSWORD = userDetails.getString("password", "");
		REMOTEDOWNROOT = userDetails.getString("server", "") + "/";
		String sync = userDetails.getString("sync_frequency", "3600");
		String directory = userDetails.getString("localdirectory",
				Environment.getExternalStorageDirectory().getPath()
				+ "/owncloud/").trim();
		if (!directory.endsWith("/")){directory = directory + "/";}
		File localdir = new File(directory);
		
		//attempt login
		new mThread().run();
		
		if (!localdir.exists()) {
			if (!localdir.mkdirs()){Intent intent = new Intent(this.getApplicationContext(),
					SettingsActivity.class);
			startActivity(intent);}
		}
		// Create the Actionbar
		ActionBar actionBar = getSupportActionBar();

		// Show Actionbar Icon
		actionBar.setDisplayShowHomeEnabled(true);

		// Show Actionbar Title
		actionBar.setDisplayShowTitleEnabled(true);

		// Create Actionbar Tabs
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create Fragments
		LocalFileFragmentTab localtab = new LocalFileFragmentTab();
		RemoteFileFragmentTab remotetab = new RemoteFileFragmentTab();

		// Create first Tab
		tab = actionBar.newTab().setTabListener(localtab);
		// Create your own custom icon
		tab.setText("Local Files");
		actionBar.addTab(tab);

		// Create Second Tab
		tab = actionBar.newTab().setTabListener(remotetab);
		// Set Tab Title
		tab.setText("Remote Files");
		actionBar.addTab(tab);

		// default sync frequency
		int seconds = 648000;
		try {
			seconds = (Integer.valueOf(sync) * 60);
		} catch (Exception e) {
			Log.e("Exception", e.toString());
		}

		Intent intent1 = new Intent(this.getBaseContext(),
				SyncService.class);
		PendingIntent pintent = PendingIntent.getService(
				this.getBaseContext(), 0, intent1, 0);
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		Log.e("Setting sync time", String.valueOf(seconds * 1000));
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
				seconds * 1000, pintent);
		}

		

	}
	@Override
	public void onPause()
	{ 
	    super.onPause();   
	}
	@Override   
    protected void onResume() {
		String action = getIntent().getAction();
	    // Prevent endless loop by adding a unique action, don't restart if action is present
	    if(action == null || !action.equals("Already created")) {
	        Log.v("Example", "Force restart");
	        Intent intent = new Intent(this, MainActivity.class);
	        startActivity(intent);
	        finish();
	    }
	    // Remove the unique action so the next time onResume is called it will restart
	    else
	        getIntent().setAction(null);

	    super.onResume();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Refresh").setIcon(R.drawable.refresh)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		menu.add(0, 1, 0, "Settings").setIcon(R.drawable.settings)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle item selection
		Log.i(tag, String.valueOf(item.getTitle()));
		switch (item.getItemId()) {
		case 0:
			Intent intent2 = new Intent(this.getApplicationContext(),
					SyncService.class);
			intent2.putExtra("Refresh", true);
			this.getApplicationContext().startService(intent2);
			return true;
		case 1:
			Intent intent = new Intent(MainActivity.this,
					SettingsActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}
	class mThread extends Thread {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			try {
				sardine = SardineFactory.begin(USERNAME, PASSWORD);
				sardine.list(REMOTEDOWNROOT);
			} catch (Exception e) {
				e.printStackTrace();
				Toast toast = Toast.makeText(getApplicationContext(), "Settings Error", Toast.LENGTH_SHORT);
				toast.show();
				Intent intent = new Intent(MainActivity.this,
						SettingsActivity.class);
				startActivity(intent);
				
				
			}
		}

	}

}