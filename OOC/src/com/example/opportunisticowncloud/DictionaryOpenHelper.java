package com.example.opportunisticowncloud;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * Class for assisting in the echange of LocalToRemote and RemoteToLocal 
 * objects between tabs and for the Sync method.
 * @author student
 *
 */
public class DictionaryOpenHelper extends SQLiteOpenHelper {
	
	private static String KEY_WORD;
	private static String KEY_DEFINITION;
	private static String DATABASE_NAME;

	private static int DATABASE_VERSION;
    private static String DICTIONARY_TABLE_NAME;
    private static String DICTIONARY_TABLE_CREATE;

	
	/**
	 * Constructor
	 * @param context
	 * @param name
	 * @param factory
	 * @param version
	 */
	public DictionaryOpenHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
		DICTIONARY_TABLE_NAME = name;
		DATABASE_VERSION = version;
		KEY_WORD = "KEY_WORD";
		KEY_DEFINITION = "KEY_DEFINITION";
		DATABASE_NAME = "DATABASE_NAME";
		DICTIONARY_TABLE_CREATE = "CREATE TABLE " + DICTIONARY_TABLE_NAME + " (" +
                KEY_WORD + " TEXT, " +
                KEY_DEFINITION + " TEXT);";
	}

	/**
	 * onCreate
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		
		
	}

	/**
	 * onUpgrade
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}

}
