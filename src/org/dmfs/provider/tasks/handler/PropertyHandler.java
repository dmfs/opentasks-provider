package org.dmfs.provider.tasks.handler;

import org.dmfs.provider.tasks.TaskDatabaseHelper.Tables;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;


/**
 * 
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * 
 */
public abstract class PropertyHandler
{

	/**
	 * Validate the given values.
	 * 
	 * @param db
	 *            The belonging database
	 * 
	 * @param values
	 *            The task properties to validate.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the values is invalid.
	 */
	public ContentValues validateValues(SQLiteDatabase db, boolean isNew, ContentValues values, boolean isSyncAdapter)
	{
		return null;
	}


	/**
	 * Insert the given values
	 * 
	 * @param db
	 *            The belonging database
	 * 
	 * @param values
	 *            The task properties to insert.
	 * 
	 * @return The row id of the inserted property
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the values is invalid.
	 */
	public long insert(SQLiteDatabase db, ContentValues values, boolean isSyncAdapter)
	{
		return db.insert(Tables.PROPERTIES, "", values);
	}


	/**
	 * Update the given values
	 * 
	 * @param db
	 *            The belonging database
	 * 
	 * @param values
	 *            The task properties to update.
	 * 
	 * @return The row id of the inserted property
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the values is invalid.
	 */
	public int update(SQLiteDatabase db, ContentValues values, String selection, String[] selectionArgs, boolean isSyncAdapter)
	{

		if (!isSyncAdapter)
		{
			// mark task as dirty
			// values.put(CommonSyncColumns._DIRTY, true);
			// values.put(TaskColumns.LAST_MODIFIED, System.currentTimeMillis());
		}

		return 0;
	}


	public int delete(SQLiteDatabase db, String id, Uri uri, String selection, String[] selectionArgs, boolean isSyncAdapter)
	{
		return db.delete(Tables.PROPERTIES, selection, selectionArgs);

	}

}
