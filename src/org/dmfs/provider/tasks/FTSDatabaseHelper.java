package org.dmfs.provider.tasks;

import java.util.HashSet;
import java.util.Set;

import org.dmfs.ngrams.NGramGenerator;
import org.dmfs.provider.tasks.TaskContract.Properties;
import org.dmfs.provider.tasks.TaskContract.TaskColumns;
import org.dmfs.provider.tasks.TaskContract.Tasks;
import org.dmfs.provider.tasks.TaskDatabaseHelper.Tables;
import org.dmfs.provider.tasks.handler.PropertyHandler;
import org.dmfs.provider.tasks.handler.PropertyHandlerFactory;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;


/**
 * Supports the {@link TaskDatabaseHelper} in the manner of full-tex-search.
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * @author Marten Gajda <marten@dmfs.org>
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
		 * An n-gram for a task.
		 */
		public static final String NGRAM_ID = "fts_ngram_id";

	}

	/**
	 * The columns of the N-gram table for the FTS search
	 * 
	 * @author Tobias Reinsch <tobias@dmfs.org>
	 */
	public interface NGramColumns
	{
		/**
		 * The row id of the N-gram.
		 */
		public static final String NGRAM_ID = "ngram_id";

		/**
		 * The content of the N-gram
		 */
		public static final String TEXT = "ngram_text";

	}

	public static final String FTS_CONTENT_TABLE = "FTS_Content";
	public static final String FTS_NGRAM_TABLE = "FTS_Ngram";
	public static final String FTS_TASK_VIEW = "FTS_Task_View";
	public static final String FTS_TASK_PROPERTY_VIEW = "FTS_Task_Property_View";

	/**
	 * SQL command to create the table for full text search and contains relationships between ngrams and tasks
	 */
	private final static String SQL_CREATE_SEARCH_CONTENT_TABLE = "CREATE TABLE " + FTS_CONTENT_TABLE + "( " + FTSContentColumns.TASK_ID + " Integer, "
		+ FTSContentColumns.NGRAM_ID + " Integer, " + FTSContentColumns.PROPERTY_ID + " Integer, " + FTSContentColumns.TYPE + " Text, " + "FOREIGN KEY("
		+ FTSContentColumns.TASK_ID + ") REFERENCES " + Tables.TASKS + "(" + TaskColumns._ID + ")," + "FOREIGN KEY(" + FTSContentColumns.TASK_ID
		+ ") REFERENCES " + Tables.TASKS + "(" + TaskColumns._ID + ") UNIQUE (" + FTSContentColumns.TASK_ID + ", " + FTSContentColumns.TYPE + ", "
		+ FTSContentColumns.PROPERTY_ID + ") ON CONFLICT IGNORE )";

	/**
	 * SQL command to create the table that stores the NGRAMS
	 */
	private final static String SQL_CREATE_NGRAM_TABLE = "CREATE TABLE " + FTS_NGRAM_TABLE + "( " + NGramColumns.NGRAM_ID
		+ " Integer PRIMARY KEY AUTOINCREMENT, " + NGramColumns.TEXT + " Text UNIQUE ON CONFLICT IGNORE)";

	private final static String SQL_CREATE_SEARCH_TASK_VIEW = "CREATE VIEW " + FTS_TASK_VIEW + " AS SELECT " + Tables.TASKS_VIEW + ".* ," + FTS_NGRAM_TABLE
		+ "." + NGramColumns.TEXT + " from " + FTS_NGRAM_TABLE + " join " + FTS_CONTENT_TABLE + " on (" + FTS_NGRAM_TABLE + "." + NGramColumns.NGRAM_ID + "="
		+ FTS_CONTENT_TABLE + "." + FTSContentColumns.NGRAM_ID + ") join " + Tables.TASKS_VIEW + " on (" + Tables.TASKS_VIEW + "." + Tasks._ID + " = "
		+ FTS_CONTENT_TABLE + "." + FTSContentColumns.TASK_ID + ");";

	// FIXME: at present the minimum score is hard coded can we leave that decision to the caller?
	private final static String SQL_RAW_QUERY_SEARCH_TASK = "SELECT %s " + ", min(1.0*count(*)/?, 1.0) as " + TaskContract.Tasks.SCORE + " from "
		+ FTS_NGRAM_TABLE + " join " + FTS_CONTENT_TABLE + " on (" + FTS_NGRAM_TABLE + "." + NGramColumns.NGRAM_ID + "=" + FTS_CONTENT_TABLE + "."
		+ FTSContentColumns.NGRAM_ID + ") join " + Tables.INSTANCE_VIEW + " on (" + Tables.INSTANCE_VIEW + "." + Tasks._ID + " = " + FTS_CONTENT_TABLE + "."
		+ FTSContentColumns.TASK_ID + ") where %s group by " + Tasks._ID + " having " + TaskContract.Tasks.SCORE + " > 0.3 " + " order by %s;";

	private final static String SQL_RAW_QUERY_SEARCH_TASK_DEFAULT_PROJECTION = Tables.INSTANCE_VIEW + ".* ," + FTS_NGRAM_TABLE + "." + NGramColumns.TEXT;

	private final static String SQL_CREATE_SEARCH_TASK_PROPERTY_VIEW = "CREATE VIEW " + FTS_TASK_PROPERTY_VIEW + " AS SELECT " + Tables.TASKS_PROPERTY_VIEW
		+ ".*  " + " from " + FTS_CONTENT_TABLE + " join " + Tables.TASKS_PROPERTY_VIEW + " on (" + Tables.TASKS_PROPERTY_VIEW + "." + Tasks._ID + "="
		+ FTS_CONTENT_TABLE + "." + FTSContentColumns.TASK_ID + ");";

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
		initializeFTS(db);
	}


	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if (oldVersion < 8)
		{
			initializeFTS(db);
			initializeFTSContent(db);
		}
	}


	/**
	 * Creates the tables and triggers which are necessary for the FTS.
	 * 
	 * @param db
	 *            The {@link SQLiteDatabase}.
	 */
	private static void initializeFTS(SQLiteDatabase db)
	{
		db.execSQL(SQL_CREATE_SEARCH_CONTENT_TABLE);
		db.execSQL(SQL_CREATE_NGRAM_TABLE);
		// db.execSQL(SQL_CREATE_SEARCH_TASK_VIEW);
		// db.execSQL(SQL_CREATE_SEARCH_TASK_PROPERTY_VIEW);
		db.execSQL(SQL_CREATE_SEARCH_TASK_DELETE_TRIGGER);
		db.execSQL(SQL_CREATE_SEARCH_TASK_DELETE_PROPERTY_TRIGGER);

		// create indices
		db.execSQL(TaskDatabaseHelper.createIndexString(FTS_NGRAM_TABLE, NGramColumns.TEXT));
		db.execSQL(TaskDatabaseHelper.createIndexString(FTS_CONTENT_TABLE, FTSContentColumns.NGRAM_ID));
		db.execSQL(TaskDatabaseHelper.createIndexString(FTS_CONTENT_TABLE, FTSContentColumns.TASK_ID));
	}


	/**
	 * Creates the FTS entries for the existing tasks.
	 * 
	 * @param db
	 *            The writable {@link SQLiteDatabase}.
	 */
	private static void initializeFTSContent(SQLiteDatabase db)
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

		NGramGenerator generator = new NGramGenerator(3, 1);

		// title
		if (title != null && title.length() > 0)
		{
			Set<String> titleNgrams = generator.getNgrams(title);
			Set<Long> titleNgramIds = insertNGrams(db, titleNgrams);
			insertNGramRelations(db, titleNgramIds, taskId, null, SearchableTypes.TITLE);
		}

		// description
		if (description != null && description.length() > 0)
		{
			Set<String> descriptionNgrams = generator.getNgrams(description);
			Set<Long> descriptionNgramIds = insertNGrams(db, descriptionNgrams);
			insertNGramRelations(db, descriptionNgramIds, taskId, null, SearchableTypes.DESCRIPTION);
		}

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
			NGramGenerator generator = new NGramGenerator(3, 1);
			// title
			if (newValues.containsKey(Tasks.TITLE))
			{
				// delete title relations
				deleteNGramRelations(db, taskId, null, SearchableTypes.TITLE);

				String title = newValues.getAsString(Tasks.TITLE);

				if (title != null && title.length() > 0)
				{
					// insert title ngrams
					Set<String> titleNgrams = generator.getNgrams(title);
					Set<Long> titleNgramIds = insertNGrams(db, titleNgrams);
					insertNGramRelations(db, titleNgramIds, taskId, null, SearchableTypes.TITLE);
				}
			}

			// description
			if (newValues.containsKey(Tasks.DESCRIPTION))
			{
				// delete description relations
				deleteNGramRelations(db, taskId, null, SearchableTypes.DESCRIPTION);

				String description = newValues.getAsString(Tasks.DESCRIPTION);

				if (description != null && description.length() > 0)
				{
					// insert description ngrams
					Set<String> descriptionNgrams = generator.getNgrams(description);
					Set<Long> descriptionNgramIds = insertNGrams(db, descriptionNgrams);
					insertNGramRelations(db, descriptionNgramIds, taskId, null, SearchableTypes.DESCRIPTION);
				}
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
			String taskId = values.getAsString(Properties.TASK_ID);

			if (searchableText != null && searchableText.length() > 0 && taskId != null)
			{
				// generate nGrams
				NGramGenerator generator = new NGramGenerator(3, 1);
				Set<String> propertyNgrams = generator.getNgrams(searchableText);

				// insert ngrams
				Set<Long> propertyNgramIds = insertNGrams(db, propertyNgrams);

				// insert ngram relations
				insertNGramRelations(db, propertyNgramIds, taskId, rowId, SearchableTypes.DESCRIPTION);
			}
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
			Long propertyId = values.getAsLong(Properties.PROPERTY_ID);
			String taskId = values.getAsString(Properties.TASK_ID);
			String searchableText = handler.getSearchableEntry(values);

			// delete NGram relations
			deleteNGramRelations(db, taskId, propertyId, SearchableTypes.PROPERTY);

			if (searchableText != null && searchableText.length() > 0 && taskId != null)
			{
				// generate nGrams
				NGramGenerator generator = new NGramGenerator(3, 1);
				Set<String> propertyNgrams = generator.getNgrams(searchableText);

				// insert ngrams
				Set<Long> propertyNgramIds = insertNGrams(db, propertyNgrams);

				// insert ngram relations
				insertNGramRelations(db, propertyNgramIds, taskId, propertyId, SearchableTypes.DESCRIPTION);
			}
		}

	}


	/**
	 * Inserts NGrams into the NGram database.
	 * 
	 * @param db
	 *            A writable {@link SQLiteDatabase}.
	 * @param ngrams
	 *            The set of NGrams.
	 * @return The ids of the ngrams.
	 */
	private static Set<Long> insertNGrams(SQLiteDatabase db, Set<String> ngrams)
	{
		Set<Long> nGramIds = new HashSet<Long>(ngrams.size());
		for (String ngram : ngrams)
		{
			ContentValues values = new ContentValues(1);
			values.put(NGramColumns.TEXT, ngram);
			long nGramId = db.insertWithOnConflict(FTS_NGRAM_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			if (nGramId == -1)
			{
				Cursor c = db
					.query(FTS_NGRAM_TABLE, new String[] { NGramColumns.NGRAM_ID }, NGramColumns.TEXT + "=?", new String[] { ngram }, null, null, null);
				try
				{
					if (c.moveToFirst())
					{
						nGramId = c.getLong(0);
					}
				}
				finally
				{
					c.close();
				}

			}
			nGramIds.add(nGramId);
		}
		return nGramIds;

	}


	/**
	 * Inserts NGrams relations for a task entry.
	 * 
	 * @param db
	 *            A writable {@link SQLiteDatabase}.
	 * @param ngramIds
	 *            The set of NGram ids.
	 * @param taskId
	 *            The row id of the task.
	 * @param propertyId
	 *            The row id of the property.
	 * @param The
	 *            entry type of the relation (title, description, property).
	 */
	private static void insertNGramRelations(SQLiteDatabase db, Set<Long> ngramIds, String taskId, Long propertyId, String contentType)
	{
		for (Long ngramId : ngramIds)
		{
			ContentValues values = new ContentValues(3);
			values.put(FTSContentColumns.TASK_ID, taskId);
			if (propertyId != null)
			{
				values.put(FTSContentColumns.PROPERTY_ID, propertyId);
			}
			values.put(FTSContentColumns.NGRAM_ID, ngramId);
			values.put(FTSContentColumns.TYPE, contentType);
			db.insertWithOnConflict(FTS_CONTENT_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
		}

	}


	/**
	 * Deletes the NGram relations of an task
	 * 
	 * @param db
	 *            The writable {@link SQLiteDatabase}.
	 * @param taskId
	 *            The task row id.
	 * @param propertyId
	 *            The property row id.
	 * @param contentType
	 *            The {@link SearchableTypes} type.
	 * @return The number of deleted relations.
	 */
	private static int deleteNGramRelations(SQLiteDatabase db, String taskId, Long propertyId, String contentType)
	{
		String[] selectionArgs = new String[] { taskId, contentType };
		StringBuilder whereClause = new StringBuilder(FTSContentColumns.TASK_ID).append(" = ?");
		whereClause.append(" AND ").append(FTSContentColumns.TYPE).append(" = ?");
		if (propertyId != null)
		{
			whereClause.append(" AND ").append(FTSContentColumns.PROPERTY_ID).append(" = ").append(propertyId);
		}
		return db.delete(FTS_CONTENT_TABLE, whereClause.toString(), selectionArgs);
	}


	/**
	 * Queries the task database to get a cursor with the search results.
	 * 
	 * @param db
	 *            The {@link SQLiteDatabase}.
	 * @param searchString
	 *            The search query string.
	 * @param projection
	 *            The database projection for the query.
	 * @param selection
	 *            The selection for the query.
	 * @param selectionArgs
	 *            The arguments for the query.
	 * @param sortOrder
	 *            The sorting order of the query.
	 * @return A cursor of the task database with the search result.
	 */
	public static Cursor getTaskSearchCursor(SQLiteDatabase db, String searchString, String[] projection, String selection, String[] selectionArgs,
		String sortOrder)
	{

		StringBuilder selectionBuilder = new StringBuilder(1024);

		if (!TextUtils.isEmpty(selection))
		{
			selectionBuilder.append(" (");
			selectionBuilder.append(selection);
			selectionBuilder.append(") AND (");
		}
		else
		{
			selectionBuilder.append(" (");
		}

		NGramGenerator generator = new NGramGenerator(3, 1);
		Set<String> ngrams = generator.getNgrams(searchString);

		String[] queryArgs;

		if (searchString != null && searchString.length() > 2)
		{

			selectionBuilder.append(NGramColumns.TEXT);
			selectionBuilder.append(" in (");

			for (int i = 0, count = ngrams.size(); i < count; ++i)
			{
				if (i > 0)
				{
					selectionBuilder.append(",");
				}
				selectionBuilder.append("?");

			}

			// selection arguments
			if (selectionArgs != null && selectionArgs.length > 0)
			{
				queryArgs = new String[selectionArgs.length + ngrams.size() + 1];
				queryArgs[0] = String.valueOf(ngrams.size());
				System.arraycopy(selectionArgs, 0, queryArgs, 1, selectionArgs.length);
				String[] ngramArray = ngrams.toArray(new String[ngrams.size()]);
				System.arraycopy(ngramArray, 0, queryArgs, selectionArgs.length + 1, ngramArray.length);
			}
			else
			{
				String[] temp = ngrams.toArray(new String[ngrams.size()]);

				queryArgs = new String[temp.length + 1];
				queryArgs[0] = String.valueOf(ngrams.size());
				System.arraycopy(temp, 0, queryArgs, 1, temp.length);
			}
			selectionBuilder.append(" ) ");
		}
		else
		{
			selectionBuilder.append(NGramColumns.TEXT);
			selectionBuilder.append(" like ?");

			// selection arguments
			if (selectionArgs != null && selectionArgs.length > 0)
			{
				queryArgs = new String[selectionArgs.length + 2];
				queryArgs[0] = String.valueOf(ngrams.size());
				System.arraycopy(selectionArgs, 0, queryArgs, 1, selectionArgs.length);
				queryArgs[queryArgs.length - 1] = searchString + "%";
			}
			else
			{
				queryArgs = new String[2];
				queryArgs[0] = String.valueOf(ngrams.size());
				queryArgs[1] = searchString + "%";
			}

		}

		selectionBuilder.append(") AND ");
		selectionBuilder.append(Tasks._DELETED);
		selectionBuilder.append(" = 0");

		if (sortOrder == null)
		{
			sortOrder = Tasks.SCORE + " desc";
		}
		else
		{
			sortOrder = Tasks.SCORE + " desc, " + sortOrder;
		}

		Cursor c = db.rawQueryWithFactory(null,
			String.format(SQL_RAW_QUERY_SEARCH_TASK, SQL_RAW_QUERY_SEARCH_TASK_DEFAULT_PROJECTION, selectionBuilder.toString(), sortOrder), queryArgs, null);
		return c;
	}
}
