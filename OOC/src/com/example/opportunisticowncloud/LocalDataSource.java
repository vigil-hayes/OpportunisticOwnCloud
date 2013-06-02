package com.example.opportunisticowncloud;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class LocalDataSource {
	
	// Database fields
	  private SQLiteDatabase database;
	  private LocalOpenHelper dbHelper;
	  private String[] allColumns = {LocalOpenHelper.COLUMN_ID, LocalOpenHelper.COLUMN_NAME,
	      LocalOpenHelper.COLUMN_RESOURCE, LocalOpenHelper.COLUMN_MODIFIED,
	      LocalOpenHelper.COLUMN_RESOURCEMOD};
	  private Context mCtx;

	  public LocalDataSource(Context context) {
		  mCtx = context;
		  dbHelper = LocalOpenHelper.getInstance(mCtx);
	  }
	  
	  public void open() throws SQLException {
		    database = dbHelper.getWritableDatabase();
	  }
	  
	  public void close() {
		    dbHelper.close();
	  }
	  
	  public LocalRemoteEntry createLocalToRemoteEntry(String name) {
		    ContentValues values = new ContentValues();
		    LocalRemoteEntry newLREntry = new LocalRemoteEntry();
		    values.put(LocalOpenHelper.COLUMN_NAME, name);
		    try {
		    	long insertId = database.insertOrThrow(LocalOpenHelper.TABLE_LOCAL, null,
		            values);
		    
		    	Cursor cursor = database.query(LocalOpenHelper.TABLE_LOCAL,
		    			allColumns, LocalOpenHelper.COLUMN_ID + " = " + insertId, null,
		    			null, null, null);
		    	if (cursor.moveToFirst()) {
		    		newLREntry = cursorToLocalRemoteEntry(cursor);
		    	}
		   
		   	 	cursor.close();
		    } catch (SQLiteConstraintException se) {
		    	Log.e("SQL", se.getMessage());
		    }
		    return newLREntry;
	  }
	  
	  public boolean entryExists(String name) {
		  for (LocalRemoteEntry lre : getAllLocalRemoteEntries()) {
			  if (lre.getFileName().equals(name)) {
				  return true;
			  }
		  }
		  return false;
	  }
	  
	  public List<LocalRemoteEntry> getAllLocalRemoteEntries() {
		    List<LocalRemoteEntry> LocalRemoteEntries = new ArrayList<LocalRemoteEntry>();
		    try {
		    	Cursor cursor = database.query(LocalOpenHelper.TABLE_LOCAL,
		        allColumns, null, null, null, null, null);
		    	cursor.moveToFirst();
			    while (!cursor.isAfterLast()) {
			      LocalRemoteEntry LocalRemoteEntry = cursorToLocalRemoteEntry(cursor);
			      LocalRemoteEntries.add(LocalRemoteEntry);
			      cursor.moveToNext();
			    }
			    // Make sure to close the cursor
			    cursor.close();
		    } catch (SQLException se) {
		    	Log.e("SQL", se.getMessage());
		    } catch (Exception e)  {
		    	Log.e("Exception", e.getMessage());
		    }
		    
		    

		    
		    return LocalRemoteEntries;
		  }

		  private LocalRemoteEntry cursorToLocalRemoteEntry(Cursor cursor) {
		    LocalRemoteEntry LocalRemoteEntry = new LocalRemoteEntry();
		    LocalRemoteEntry.setID(cursor.getLong(0));
		    LocalRemoteEntry.setFileName(cursor.getString(1));
		    LocalRemoteEntry.setResourceName(cursor.getString(2));
		    LocalRemoteEntry.setFileModified(cursor.getLong(3));
		    LocalRemoteEntry.setResourceModified(cursor.getLong(4));
		    return LocalRemoteEntry;
		  }
}
