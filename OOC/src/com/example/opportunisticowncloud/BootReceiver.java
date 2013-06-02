package com.example.opportunisticowncloud;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	private int seconds = 180;

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Calendar cal = Calendar.getInstance();
	
		Log.e("Boot Receiver", "system reboot completed... Starting service.");
		Intent intent1 = new Intent(context, SyncService.class);
		PendingIntent pintent = PendingIntent.getService(context, 0, intent1, 0);
		
		SharedPreferences userDetails = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		
		String sync = userDetails.getString("sync_frequency", "");
		Log.e("prefs",sync);
		
		try{
			int seconds = (Integer.valueOf(sync)*60);
		}
		catch (Exception e){
			Log.e("Exception",e.toString());
		}
		
		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), seconds*1000, pintent); 
		
	}

}
