package com.example.opportunisticowncloud;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		ConnectivityManager conMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInf = conMgr.getAllNetworkInfo();

		for (NetworkInfo inf : netInf) {
			if (inf.getTypeName().compareToIgnoreCase("wifi") == 0) {
				if (inf.isConnected()) {
					Intent intent2 = new Intent(context, SyncService.class);
					intent2.putExtra("ConnectivityChange", true);
					context.startService(intent2);
				}

			}
		}
		

	}

}