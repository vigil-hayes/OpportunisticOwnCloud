package com.example.opportunisticowncloud;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class RemoteOpenHelper extends SQLiteOpenHelper {
	private static RemoteOpenHelper mInstance = null;
	public static final String TABLE_REMOTE = "remote";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_RESOURCE = "resource";
	public static final String COLUMN_MODIFIED = "modified";
	public static final String COLUMN_RESOURCEMOD = "resourcemod";
	
	private static final String DATABASE_CREATE = "create table "
		      + TABLE_REMOTE + "(" + COLUMN_ID + " integer primary key autoincrement, " +
		      						COLUMN_NAME + " text, " + 
		      						COLUMN_RESOURCE + " text not null, " +
		      						COLUMN_MODIFIED + " text, " + 
		      						COLUMN_RESOURCEMOD + " text);";
	
	private static final String DATABASE_NAME = "RemoteDB";
	private static final int DATABASE_VERSION = 1;

	public RemoteOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		try {
			db.execSQL(DATABASE_CREATE);
		} catch (SQLException se) {
			Log.e("SQL", se.getMessage());
		} catch (Exception e) {
			Log.e("Exception", e.getMessage());
		}
		
	}
	
	public static RemoteOpenHelper getInstance(Context ctx) {
	      
	    // Use the application context, which will ensure that you 
	    // don't accidentally leak an Activity's context.
	    // See this article for more information: http://bit.ly/6LRzfx
	    if (mInstance == null) {
	      mInstance = new RemoteOpenHelper(ctx.getApplicationContext());
	    }
	    return mInstance;
	  }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(LocalOpenHelper.class.getName(),
		        "Upgrading database from version " + oldVersion + " to "
		            + newVersion + ", which will destroy all old data");
		    db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMOTE);
		    onCreate(db);
		
	}

}
