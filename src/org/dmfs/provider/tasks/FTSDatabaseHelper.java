package org.dmfs.provider.tasks;

import org.dmfs.provider.tasks.TaskContract.Properties;
import org.dmfs.provider.tasks.TaskContract.Tasks;
import org.dmfs.provider.tasks.TaskDatabaseHelper.Tables;
import org.dmfs.provider.tasks.handler.PropertyHandler;
import org.dmfs.provider.tasks.handler.PropertyHandlerFactory;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * Supports the {@link TaskDatabaseHelper} in the manner of full-tex-search.
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * 
 */
public class FTSDatabaseHelper
{

	/**
	 * Search content columns. Defines all the columns for the full text search
	 * 
	 * @author Tobias Reinsch <tobias@dmfs.org>
	 */
	public interface FTSContentColumns
	{
		/**
		 * The row id of the belonging task.
		 */
		public static final String TASK_ID = "fts_task_id";

		/**
		 * The the property id of the searchable entry or <code>null</code> if the entry is not related to a property.
		 */
		public static final String PROPERTY_ID = "fts_property_ID";

		/**
		 * The the type of the searchable entry
		 */
		public static final String TYPE = "fts_type";

		/**
		 * The searchable text for a task.
		 */
		public static final String TEXT = "fts_text";

	}

	public static final String FTS_CONTENT_TABLE = "FTS_Content";

	/**
	 * SQL command to create the virtual fts table for full text search
	 */
	private final static String SQL_CREATE_SEARCH_CONTENT_TABLE = "CREATE VIRTUAL TABLE " + FTS_CONTENT_TABLE + " USING fts3 ( " + FTSContentColumns.TASK_ID
		+ ", " + FTSContentColumns.PROPERTY_ID + ", " + FTSContentColumns.TYPE + ", " + FTSContentColumns.TEXT + ")";

	private final static String SQL_CREATE_SEARCH_TASK_DELETE_TRIGGER = "CREATE TRIGGER search_task_delete_trigger AFTER DELETE ON " + Tables.TASKS + " BEGIN "
		+ " DELETE FROM " + FTS_CONTENT_TABLE + " WHERE " + FTSContentColumns.TASK_ID + " =  old." + Tasks._ID + "; END";

	private final static String SQL_CREATE_SEARCH_TASK_DELETE_PROPERTY_TRIGGER = "CREATE TRIGGER search_task_delete_property_trigger AFTER DELETE ON "
		+ Tables.PROPERTIES + " BEGIN " + " DELETE FROM " + FTS_CONTENT_TABLE + " WHERE " + FTSContentColumns.TASK_ID + " =  old." + Properties.TASK_ID
		+ " AND " + FTSContentColumns.PROPERTY_ID + " = old." + Properties.PROPERTY_ID + "; END";

	private final static String SQL_UPDATE_SELECTION = FTSContentColumns.TASK_ID + " = ? AND " + FTSContentColumns.PROPERTY_ID + " IS NULL AND "
		+ FTSContentColumns.TYPE + " = ?";

	private final static String SQL_UPDATE_PROPERTY_SELECTION = FTSContentColumns.TASK_ID + " = ? AND " + FTSContentColumns.PROPERTY_ID + " = ? AND "
		+ FTSContentColumns.TYPE + " = ?";

	/**
	 * The different types of searchable entries for tasks linked to the <code>TYPE</code> column.
	 * 
	 * @author Tobias Reinsch <tobias@dmfs.org>
	 */
	public interface SearchableTypes
	{
		public static final String TITLE = "title";
		public static final String DESCRIPTION = "description";
		public static final String PROPERTY = "property";

	}


	public static void onCreate(SQLiteDatabase db)
	{
		db.execSQL(SQL_CREATE_SEARCH_CONTENT_TABLE);
		db.execSQL(SQL_CREATE_SEARCH_TASK_DELETE_TRIGGER);
		db.execSQL(SQL_CREATE_SEARCH_TASK_DELETE_PROPERTY_TRIGGER);
	}


	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if (oldVersion < 8)
		{
			db.execSQL(SQL_CREATE_SEARCH_CONTENT_TABLE);
			db.execSQL(SQL_CREATE_SEARCH_TASK_DELETE_TRIGGER);
			db.execSQL(SQL_CREATE_SEARCH_TASK_DELETE_PROPERTY_TRIGGER);
			initializeFTS(db);
		}
	}


	/**
	 * Creates the FTS content table and generates FTS entries for the existing tasks.
	 * 
	 * @param db
	 *            The writable {@link SQLiteDatabase}.
	 */
	private static void initializeFTS(SQLiteDatabase db)
	{
		String[] task_projection = new String[] { Tasks._ID, Tasks.TITLE, Tasks.DESCRIPTION };
		Cursor c = db.query(Tables.TASKS_PROPERTY_VIEW, task_projection, null, null, null, null, null);
		while (c.moveToNext())
		{
			insertTaskFTSEntries(db, c.getString(0), c.getString(1), c.getString(2));
		}
		c.close();
	}


	/**
	 * Inserts the searchable texts of the task in the database.
	 * 
	 * @param db
	 *            The writable {@link SQLiteDatabase}.
	 * @param values
	 *            The {@link ContentValues} of the task.
	 * @param rowId
	 *            The row id of the task.
	 */
	public static void insertTaskFTSEntries(SQLiteDatabase db, ContentValues values, long rowId)
	{
		if (values != null)
		{

			String title = null;
			if (values.containsKey(Tasks.TITLE))
			{
				title = values.getAsString(Tasks.TITLE);
			}
			String description = null;
			if (values.containsKey(Tasks.DESCRIPTION))
			{
				description = values.getAsString(Tasks.DESCRIPTION);
			}
			insertTaskFTSEntries(db, String.valueOf(rowId), title, description);
		}
	}


	/**
	 * Inserts the searchable texts of the task in the database.
	 * 
	 * @param db
	 *            The writable {@link SQLiteDatabase}.
	 * 
	 * @param taskId
	 *            The row id of the task.
	 * @param title
	 *            The title of the task.
	 * @param description
	 *            The description of the task.
	 */
	private static void insertTaskFTSEntries(SQLiteDatabase db, String taskId, String title, String description)
	{
		ContentValues values = new ContentValues(3);
		values.put(FTSContentColumns.TASK_ID, taskId);

		// title
		values.put(FTSContentColumns.TYPE, SearchableTypes.TITLE);
		values.put(FTSContentColumns.TEXT, title);
		db.insert(SQL_CREATE_SEARCH_CONTENT_TABLE, null, values);

		// description
		values.put(FTSContentColumns.TYPE, SearchableTypes.DESCRIPTION);
		values.put(FTSContentColumns.TEXT, description);
		db.insert(SQL_CREATE_SEARCH_CONTENT_TABLE, null, values);

	}


	/**
	 * Updates the existing searchables entries for the task.
	 * 
	 * @param db
	 *            The writable {@link SQLiteDatabase}.
	 * @param taskId
	 *            The row id of the task.
	 * @param newValues
	 *            The {@link ContentValues} to update for that task.
	 */
	public static void updateTaskFTSEntries(SQLiteDatabase db, String taskId, ContentValues newValues)
	{
		if (newValues != null)
		{
			ContentValues values = new ContentValues(1);

			// title
			if (values.containsKey(Tasks.TITLE))
			{
				String title = newValues.getAsString(Tasks.TITLE);
				values.put(FTSContentColumns.TEXT, title);
				db.update(FTS_CONTENT_TABLE, values, SQL_UPDATE_SELECTION, new String[] { taskId, SearchableTypes.TITLE });
			}

			// description
			if (values.containsKey(Tasks.DESCRIPTION))
			{
				String description = newValues.getAsString(Tasks.DESCRIPTION);
				values.put(FTSContentColumns.TEXT, description);
				db.update(FTS_CONTENT_TABLE, values, SQL_UPDATE_SELECTION, new String[] { taskId, SearchableTypes.DESCRIPTION });
			}
		}

	}


	/**
	 * Inserts the searchable entries for a property.
	 * 
	 * @param db
	 *            The writable {@link SQLiteDatabase}.
	 * @param values
	 *            The {@link ContentValues} of the property.
	 * @param rowId
	 *            The row id of the property.
	 */
	public static void insertPropertyFTSEntry(SQLiteDatabase db, ContentValues values, long rowId)
	{
		if (values != null && values.containsKey(Properties.MIMETYPE))
		{
			String mimetype = values.getAsString(Properties.MIMETYPE);
			PropertyHandler handler = PropertyHandlerFactory.create(mimetype);
			String searchableText = handler.getSearchableEntry(values);

			ContentValues insertValues = new ContentValues(4);
			insertValues.put(FTSContentColumns.TASK_ID, values.getAsString(Properties.TASK_ID));
			insertValues.put(FTSContentColumns.PROPERTY_ID, String.valueOf(rowId));
			insertValues.put(FTSContentColumns.TYPE, SearchableTypes.PROPERTY);
			insertValues.put(FTSContentColumns.TEXT, searchableText);
			db.insert(FTS_CONTENT_TABLE, null, insertValues);
		}

	}


	/**
	 * Updates the searchable entries for a property.
	 * 
	 * @param db
	 *            The writable {@link SQLiteDatabase}.
	 * @param values
	 *            The {@link ContentValues} of the property.
	 * @param handler
	 *            The {@link PropertyHandler} for the property.
	 */
	public static void updatePropertyFTSEntry(SQLiteDatabase db, ContentValues values, PropertyHandler handler)
	{
		if (values != null && values.containsKey(Properties.PROPERTY_ID))
		{
			String propertyId = values.getAsString(Properties.PROPERTY_ID);
			String taskId = values.getAsString(Properties.TASK_ID);
			String searchableText = handler.getSearchableEntry(values);

			ContentValues updateValues = new ContentValues(1);
			updateValues.put(FTSContentColumns.TEXT, searchableText);
			db.update(FTS_CONTENT_TABLE, updateValues, SQL_UPDATE_PROPERTY_SELECTION, new String[] { taskId, propertyId, SearchableTypes.PROPERTY });
		}

	}
}
