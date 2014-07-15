package org.dmfs.provider.tasks.handler;

import org.dmfs.provider.tasks.TaskContract.Properties;
import org.dmfs.provider.tasks.TaskDatabaseHelper.Tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * Abstract class that is used as template for specific property handlers.
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * 
 */
public abstract class PropertyHandler
{

	/**
	 * Validates the content of the property prior to insert and update transactions.
	 * 
	 * @param db
	 *            The {@link SQLiteDatabase}.
	 * @param isNew
	 *            Indicates that the content is new and not an update.
	 * @param values
	 *            The {@link ContentValues} to validate.
	 * @param isSyncAdapter
	 *            Indicates that the transaction was triggered from a SyncAdapter.
	 * 
	 * @return The valid {@link ContentValues}.
	 * 
	 * @throws IllegalArgumentException
	 *             if the {@link ContentValues} are invalid.
	 */
	public abstract ContentValues validateValues(SQLiteDatabase db, boolean isNew, ContentValues values, boolean isSyncAdapter);


	/**
	 * Inserts the property {@link ContentValues} into the database.
	 * 
	 * @param db
	 *            The {@link SQLiteDatabase}.
	 * @param values
	 *            The {@link ContentValues} to insert.
	 * @param isSyncAdapter
	 *            Indicates that the transaction was triggered from a SyncAdapter.
	 * 
	 * @return The row id of the new property as <code>long</code>
	 */
	public long insert(SQLiteDatabase db, ContentValues values, boolean isSyncAdapter)
	{
		return db.insert(Tables.PROPERTIES, "", values);
	}


	/**
	 * Updates the property {@link ContentValues} in the database.
	 * 
	 * @param db
	 *            The {@link SQLiteDatabase}.
	 * @param values
	 *            The {@link ContentValues} to update.
	 * @param selection
	 *            The selection <code>String</code> to update the right row.
	 * @param selectionArgs
	 *            The arguments for the selection <code>String</code>.
	 * @param isSyncAdapter
	 *            Indicates that the transaction was triggered from a SyncAdapter.
	 * 
	 * @return The number of rows affected.
	 */
	public int update(SQLiteDatabase db, ContentValues values, String selection, String[] selectionArgs, boolean isSyncAdapter)
	{

		return db.update(Tables.PROPERTIES, values, selection, selectionArgs);
	}


	/**
	 * Deletes the property in the database.
	 * 
	 * @param db
	 *            The belonging database.
	 * @param selection
	 *            The selection <code>String</code> to delete the correct row.
	 * @param selectionArgs
	 *            The arguments for the selection <code>String</code>
	 * @param isSyncAdapter
	 *            Indicates that the transaction was triggered from a SyncAdapter.
	 * @return
	 */
	public int delete(SQLiteDatabase db, String selection, String[] selectionArgs, boolean isSyncAdapter)
	{
		return db.delete(Tables.PROPERTIES, selection, selectionArgs);

	}


	/**
	 * Method hook to insert FTS entries on database migration.
	 * 
	 * @param db
	 *            The {@link SQLiteDatabase}.
	 * @param propertyCursor
	 *            The cursor to the {@link Properties} table, containing all columns.
	 */
	public void insertFTSEntry(SQLiteDatabase db, Cursor propertyCursor)
	{

	}


	/**
	 * Method hook to return a searchable text for the property.
	 * 
	 * @param values
	 *            The {@link ContentValues} for the property.
	 * 
	 * @return The searchable text as {@link String}.
	 */
	public String getSearchableEntry(ContentValues values)
	{
		return null;
	};

}
