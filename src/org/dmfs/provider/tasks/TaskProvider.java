/*
 * Copyright (C) 2012 Marten Gajda <marten@dmfs.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.dmfs.provider.tasks;

import java.sql.RowId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import org.dmfs.provider.tasks.TaskContract.Alarms;
import org.dmfs.provider.tasks.TaskContract.Categories;
import org.dmfs.provider.tasks.TaskContract.CategoriesColumns;
import org.dmfs.provider.tasks.TaskContract.CommonSyncColumns;
import org.dmfs.provider.tasks.TaskContract.Instances;
import org.dmfs.provider.tasks.TaskContract.Properties;
import org.dmfs.provider.tasks.TaskContract.PropertyColumns;
import org.dmfs.provider.tasks.TaskContract.TaskColumns;
import org.dmfs.provider.tasks.TaskContract.TaskListColumns;
import org.dmfs.provider.tasks.TaskContract.TaskListSyncColumns;
import org.dmfs.provider.tasks.TaskContract.TaskLists;
import org.dmfs.provider.tasks.TaskContract.TaskSyncColumns;
import org.dmfs.provider.tasks.TaskContract.Tasks;
import org.dmfs.provider.tasks.TaskDatabaseHelper.Tables;
import org.dmfs.provider.tasks.broadcast.DueAlarmBroadcastHandler;
import org.dmfs.provider.tasks.broadcast.StartAlarmBroadcastHandler;
import org.dmfs.provider.tasks.handler.PropertyHandler;
import org.dmfs.provider.tasks.handler.PropertyHandlerFactory;
import org.dmfs.provider.tasks.taskhooks.AbstractTaskHook;
import org.dmfs.provider.tasks.taskhooks.RelationUpdaterHook;
import org.dmfs.provider.tasks.taskhooks.RemoveLocalTasksHook;
import org.dmfs.provider.tasks.taskhooks.TestHook;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;


/**
 * The provider for tasks.
 * 
 * TODO: add support for recurring tasks
 * 
 * TODO: add support for reminders
 * 
 * TODO: add support for attendees
 * 
 * TODO: refactor the selection stuff
 * 
 * @author Marten Gajda <marten@dmfs.org>
 * @author Tobias Reinsch <tobias@dmfs.org>
 * 
 */
public final class TaskProvider extends SQLiteContentProvider
{

	private static final int LISTS = 1;
	private static final int LIST_ID = 2;
	private static final int TASKS = 101;
	private static final int TASK_ID = 102;
	private static final int INSTANCES = 103;
	private static final int INSTANCE_ID = 104;
	private static final int CATEGORIES = 1001;
	private static final int CATEGORY_ID = 1002;
	private static final int PROPERTIES = 1003;
	private static final int PROPERTY_ID = 1004;
	private static final int ALARMS = 1005;
	private static final int ALARM_ID = 1006;
	private static final int SEARCH = 1007;

	private static final String[] TASK_ID_PROJECTION = { Tasks._ID };
	private static final String[] TASK_SYNC_ID_PROJECTION = { Tasks._SYNC_ID };
	private static final String[] TASKLIST_ID_PROJECTION = { TaskLists._ID };

	private static final String SYNC_ID_SELECTION = Tasks._SYNC_ID + "=?";
	private static final String TASK_ID_SELECTION = Tasks._ID + "=?";
	private static final String TASKLISTS_ID_SELECTION = TaskLists._ID + "=?";

	private final static Set<String> TASK_LIST_SYNC_COLUMNS = new HashSet<String>(Arrays.asList(TaskLists.SYNC_ADAPTER_COLUMNS));

	/**
	 * A list of {@link AbstractTaskHook} to execute on any modification of a task.
	 * <p>
	 * TODO: allow dynamic configuration of the hooks.
	 * </p>
	 */
	private final static AbstractTaskHook[] TASK_HOOKS = { new RelationUpdaterHook(), new RemoveLocalTasksHook(), new TestHook() };

	/**
	 * A helper to check {@link Integer} values for equality with <code>1</code>. You can use it like
	 * 
	 * <pre>
	 * ONE.equals(someLong)
	 * </pre>
	 * 
	 * which is shorter and less error prone (you can't forget the <code>null</code> check with the method above) than
	 * 
	 * <pre>
	 * someLong != null &amp;&amp; someLong == 1
	 * </pre>
	 */
	private final static Integer ONE = 1;

	/**
	 * The task database helper that provides access to the actual database.
	 */
	private TaskDatabaseHelper mDBHelper;

	/**
	 * Our current authority.
	 */
	private String mAuthority;

	private UriMatcher mUriMatcher;

	private interface HookExecutor
	{
		public void execute(AbstractTaskHook hook);
	}


	@Override
	public boolean onCreate()
	{
		boolean result = super.onCreate();
		mAuthority = getContext().getString(R.string.org_dmfs_tasks_authority);

		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI(mAuthority, TaskContract.TaskLists.CONTENT_URI_PATH, LISTS);

		mUriMatcher.addURI(mAuthority, TaskContract.TaskLists.CONTENT_URI_PATH + "/#", LIST_ID);

		mUriMatcher.addURI(mAuthority, TaskContract.Tasks.CONTENT_URI_PATH, TASKS);
		mUriMatcher.addURI(mAuthority, TaskContract.Tasks.CONTENT_URI_PATH + "/#", TASK_ID);

		mUriMatcher.addURI(mAuthority, TaskContract.Instances.CONTENT_URI_PATH, INSTANCES);
		mUriMatcher.addURI(mAuthority, TaskContract.Instances.CONTENT_URI_PATH + "/#", INSTANCE_ID);

		mUriMatcher.addURI(mAuthority, TaskContract.Properties.CONTENT_URI_PATH, PROPERTIES);
		mUriMatcher.addURI(mAuthority, TaskContract.Properties.CONTENT_URI_PATH + "/#", PROPERTY_ID);

		mUriMatcher.addURI(mAuthority, TaskContract.Categories.CONTENT_URI_PATH, CATEGORIES);
		mUriMatcher.addURI(mAuthority, TaskContract.Categories.CONTENT_URI_PATH + "/#", CATEGORY_ID);

		mUriMatcher.addURI(mAuthority, TaskContract.Alarms.CONTENT_URI_PATH, ALARMS);
		mUriMatcher.addURI(mAuthority, TaskContract.Alarms.CONTENT_URI_PATH + "/#", ALARM_ID);

		mUriMatcher.addURI(mAuthority, TaskContract.Tasks.SEARCH_URI_PATH, SEARCH);
		return result;
	}


	/**
	 * Return true if the caller is a sync adapter (i.e. if the Uri contains the query parameter {@link TaskContract#CALLER_IS_SYNCADAPTER} and its value is
	 * true).
	 * 
	 * @param uri
	 *            The {@link Uri} to check.
	 * @return <code>true</code> if the caller pretends to be a sync adapter, <code>false</code> otherwise.
	 */
	@Override
	public boolean isCallerSyncAdapter(Uri uri)
	{
		String param = uri.getQueryParameter(TaskContract.CALLER_IS_SYNCADAPTER);
		return param != null && !"false".equals(param);
	}


	/**
	 * Return true if the URI indicates to a load extended properties with {@link TaskContract#LOAD_PROPERTIES}.
	 * 
	 * @param uri
	 *            The {@link Uri} to check.
	 * @return <code>true</code> if the URI requests to load extended properties, <code>false</code> otherwise.
	 */
	public boolean shouldLoadProperties(Uri uri)
	{
		String param = uri.getQueryParameter(TaskContract.LOAD_PROPERTIES);
		return param != null && !"false".equals(param);
	}


	/**
	 * Get the account name from the given {@link Uri}.
	 * 
	 * @param uri
	 *            The Uri to check.
	 * @return The account name or null if no account name has been specified.
	 */
	protected String getAccountName(Uri uri)
	{
		return uri.getQueryParameter(TaskContract.ACCOUNT_NAME);
	}


	/**
	 * Get the account type from the given {@link Uri}.
	 * 
	 * @param uri
	 *            The Uri to check.
	 * @return The account type or null if no account type has been specified.
	 */
	protected String getAccountType(Uri uri)
	{
		return uri.getQueryParameter(TaskContract.ACCOUNT_TYPE);
	}


	/**
	 * Get any id from the given {@link Uri}.
	 * 
	 * @param uri
	 *            The Uri.
	 * @return The last path segment (which should contain the id).
	 */
	private long getId(Uri uri)
	{
		return Long.parseLong(uri.getPathSegments().get(1));
	}


	/**
	 * Build a selection string that selects the account specified in <code>uri</code>.
	 * 
	 * @param uri
	 *            A {@link Uri} that specifies an account.
	 * @return A {@link StringBuilder} with a selection string for the account.
	 */
	protected StringBuilder selectAccount(Uri uri)
	{
		StringBuilder sb = new StringBuilder(256);
		return selectAccount(sb, uri);
	}


	/**
	 * Append the selection of the account specified in <code>uri</code> to the {@link StringBuilder} <code>sb</code>.
	 * 
	 * @param sb
	 *            A {@link StringBuilder} that the selection is appended to.
	 * @param uri
	 *            A {@link Uri} that specifies an account.
	 * @return <code>sb</code>.
	 */
	protected StringBuilder selectAccount(StringBuilder sb, Uri uri)
	{
		String accountName = getAccountName(uri);
		String accountType = getAccountType(uri);

		if (accountName != null || accountType != null)
		{

			if (accountName != null)
			{
				if (sb.length() > 0)
				{
					sb.append(" AND ");
				}

				sb.append(TaskListSyncColumns.ACCOUNT_NAME);
				sb.append("=");
				DatabaseUtils.appendEscapedSQLString(sb, accountName);
			}
			if (accountType != null)
			{

				if (sb.length() > 0)
				{
					sb.append(" AND ");
				}

				sb.append(TaskListSyncColumns.ACCOUNT_TYPE);
				sb.append("=");
				DatabaseUtils.appendEscapedSQLString(sb, accountType);
			}
		}
		return sb;
	}


	/**
	 * Append the selection of the account specified in <code>uri</code> to the an {@link SQLiteQueryBuilder}.
	 * 
	 * @param sqlBuilder
	 *            A {@link SQLiteQueryBuilder} that the selection is appended to.
	 * @param uri
	 *            A {@link Uri} that specifies an account.
	 */
	protected void selectAccount(SQLiteQueryBuilder sqlBuilder, Uri uri)
	{
		String accountName = getAccountName(uri);
		String accountType = getAccountType(uri);

		if (accountName != null)
		{
			sqlBuilder.appendWhere(" AND ");
			sqlBuilder.appendWhere(TaskListSyncColumns.ACCOUNT_NAME);
			sqlBuilder.appendWhere("=");
			sqlBuilder.appendWhereEscapeString(accountName);
		}
		if (accountType != null)
		{
			sqlBuilder.appendWhere(" AND ");
			sqlBuilder.appendWhere(TaskListSyncColumns.ACCOUNT_TYPE);
			sqlBuilder.appendWhere("=");
			sqlBuilder.appendWhereEscapeString(accountType);
		}
	}


	private StringBuilder _selectId(StringBuilder sb, long id, String key)
	{
		if (sb.length() > 0)
		{
			sb.append(" AND ");
		}
		sb.append(key);
		sb.append("=");
		sb.append(id);
		return sb;
	}


	protected StringBuilder selectId(Uri uri)
	{
		StringBuilder sb = new StringBuilder(128);
		return selectId(sb, uri);
	}


	protected StringBuilder selectId(StringBuilder sb, Uri uri)
	{
		return _selectId(sb, getId(uri), TaskListColumns._ID);
	}


	protected StringBuilder selectTaskId(Uri uri)
	{
		StringBuilder sb = new StringBuilder(128);
		return selectTaskId(sb, uri);
	}


	protected StringBuilder selectTaskId(long id)
	{
		StringBuilder sb = new StringBuilder(128);
		return selectTaskId(sb, id);
	}


	protected StringBuilder selectTaskId(StringBuilder sb, Uri uri)
	{
		return selectTaskId(sb, getId(uri));
	}


	protected StringBuilder selectTaskId(StringBuilder sb, long id)
	{
		return _selectId(sb, id, Instances.TASK_ID);

	}


	protected StringBuilder selectPropertyId(Uri uri)
	{
		StringBuilder sb = new StringBuilder(128);
		return selectPropertyId(sb, uri);
	}


	protected StringBuilder selectPropertyId(StringBuilder sb, Uri uri)
	{
		return selectPropertyId(sb, getId(uri));
	}


	protected StringBuilder selectPropertyId(long id)
	{
		StringBuilder sb = new StringBuilder(128);
		return selectPropertyId(sb, id);
	}


	protected StringBuilder selectPropertyId(StringBuilder sb, long id)
	{
		return _selectId(sb, id, PropertyColumns.PROPERTY_ID);
	}


	/**
	 * Add a selection by ID to the given {@link SQLiteQueryBuilder}. The id is taken from the given Uri.
	 * 
	 * @param sqlBuilder
	 *            The {@link SQLiteQueryBuilder} to append the selection to.
	 * @param idColumn
	 *            The column that must match the id.
	 * @param uri
	 *            An {@link Uri} that contains the id.
	 */
	protected void selectId(SQLiteQueryBuilder sqlBuilder, String idColumn, Uri uri)
	{
		sqlBuilder.appendWhere(" AND ");
		sqlBuilder.appendWhere(idColumn);
		sqlBuilder.appendWhere("=");
		sqlBuilder.appendWhere(String.valueOf(getId(uri)));
	}


	/**
	 * Append any arbitrary selection string to the selection in <code>sb</code>
	 * 
	 * @param sb
	 *            A {@link StringBuilder} that already contains a selection string.
	 * @param selection
	 *            A valid SQL selection string.
	 * @return A string with the final selection.
	 */
	protected String updateSelection(StringBuilder sb, String selection)
	{
		if (selection != null)
		{
			if (sb.length() > 0)
			{
				sb.append("AND ( ").append(selection).append(" ) ");
			}
			else
			{
				sb.append(" ( ").append(selection).append(" ) ");
			}
		}
		return sb.toString();
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		final SQLiteDatabase db = mDBHelper.getWritableDatabase();
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		// initialize appendWhere, this allows us to append all other selections with a preceding "AND"
		sqlBuilder.appendWhere(" 1=1 ");
		boolean isSyncAdapter = isCallerSyncAdapter(uri);

		switch (mUriMatcher.match(uri))
		{
			case LISTS:
				// add account to selection if any
				selectAccount(sqlBuilder, uri);
				sqlBuilder.setTables(Tables.LISTS);
				if (sortOrder == null || sortOrder.length() == 0)
					sortOrder = TaskContract.TaskLists.DEFAULT_SORT_ORDER;

				break;

			case LIST_ID:
				// add account to selection if any
				selectAccount(sqlBuilder, uri);
				sqlBuilder.setTables(Tables.LISTS);
				selectId(sqlBuilder, TaskListColumns._ID, uri);
				if (sortOrder == null || sortOrder.length() == 0)
					sortOrder = TaskContract.TaskLists.DEFAULT_SORT_ORDER;
				break;

			case TASKS:
				if (shouldLoadProperties(uri))
				{
					// extended properties were requested, therefore change to task view that includes these properties
					sqlBuilder.setTables(Tables.TASKS_PROPERTY_VIEW);
				}
				else
				{
					sqlBuilder.setTables(Tables.TASKS_VIEW);
				}
				if (!isSyncAdapter)
				{
					// do not return deleted rows if caller is not a sync adapter
					sqlBuilder.appendWhere(" AND ");
					sqlBuilder.appendWhere(Tasks._DELETED);
					sqlBuilder.appendWhere("=0");
				}
				if (sortOrder == null || sortOrder.length() == 0)
					sortOrder = TaskContract.Tasks.DEFAULT_SORT_ORDER;
				break;

			case TASK_ID:
				if (shouldLoadProperties(uri))
				{
					// extended properties were requested, therefore change to task view that includes these properties
					sqlBuilder.setTables(Tables.TASKS_PROPERTY_VIEW);
				}
				else
				{
					sqlBuilder.setTables(Tables.TASKS_VIEW);
				}
				selectId(sqlBuilder, TaskColumns._ID, uri);
				if (!isSyncAdapter)
				{
					// do not return deleted rows if caller is not a sync adapter
					sqlBuilder.appendWhere(" AND ");
					sqlBuilder.appendWhere(Tasks._DELETED);
					sqlBuilder.appendWhere("=0");
				}
				if (sortOrder == null || sortOrder.length() == 0)
					sortOrder = TaskContract.Tasks.DEFAULT_SORT_ORDER;
				break;

			case INSTANCES:
				if (shouldLoadProperties(uri))
				{
					// extended properties were requested, therefore change to instance view that includes these properties
					sqlBuilder.setTables(Tables.INSTANCE_PROPERTY_VIEW);
				}
				else
				{
					sqlBuilder.setTables(Tables.INSTANCE_VIEW);
				}
				if (!isSyncAdapter)
				{
					// do not return deleted rows if caller is not a sync adapter
					sqlBuilder.appendWhere(" AND ");
					sqlBuilder.appendWhere(Tasks._DELETED);
					sqlBuilder.appendWhere("=0");
				}
				if (sortOrder == null || sortOrder.length() == 0)
					sortOrder = TaskContract.Instances.DEFAULT_SORT_ORDER;
				break;

			case INSTANCE_ID:
				if (shouldLoadProperties(uri))
				{
					// extended properties were requested, therefore change to instance view that includes these properties
					sqlBuilder.setTables(Tables.INSTANCE_PROPERTY_VIEW);
				}
				else
				{
					sqlBuilder.setTables(Tables.INSTANCE_VIEW);
				}
				selectId(sqlBuilder, Instances._ID, uri);
				if (!isSyncAdapter)
				{
					// do not return deleted rows if caller is not a sync adapter
					sqlBuilder.appendWhere(" AND ");
					sqlBuilder.appendWhere(Tasks._DELETED);
					sqlBuilder.appendWhere("=0");
				}
				if (sortOrder == null || sortOrder.length() == 0)
					sortOrder = TaskContract.Instances.DEFAULT_SORT_ORDER;
				break;

			case CATEGORIES:
				selectAccount(sqlBuilder, uri);
				sqlBuilder.setTables(Tables.CATEGORIES);
				if (sortOrder == null || sortOrder.length() == 0)
					sortOrder = TaskContract.Categories.DEFAULT_SORT_ORDER;
				break;

			case CATEGORY_ID:
				selectAccount(sqlBuilder, uri);
				sqlBuilder.setTables(Tables.CATEGORIES);
				selectId(sqlBuilder, CategoriesColumns._ID, uri);
				if (sortOrder == null || sortOrder.length() == 0)
					sortOrder = TaskContract.Categories.DEFAULT_SORT_ORDER;
				break;

			case PROPERTIES:
				sqlBuilder.setTables(Tables.PROPERTIES);
				break;

			case PROPERTY_ID:
				sqlBuilder.setTables(Tables.PROPERTIES);
				selectId(sqlBuilder, PropertyColumns.PROPERTY_ID, uri);
				break;

			case SEARCH:
				String searchString = uri.getQueryParameter(Tasks.SEARCH_QUERY_PARAMETER);
				searchString = Uri.decode(searchString);
				Cursor searchCursor = FTSDatabaseHelper.getTaskSearchCursor(db, searchString, projection, selection, selectionArgs, sortOrder);
				if (searchCursor != null)
				{
					// attach tasks uri for notifications, that way the search results are updated when a task changes
					searchCursor.setNotificationUri(getContext().getContentResolver(), Tasks.getContentUri(mAuthority));
				}
				return searchCursor;

			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		Cursor c = sqlBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		if (c != null)
		{
			c.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return c;
	}


	@Override
	public int deleteInTransaction(Uri uri, String selection, String[] selectionArgs, final boolean isSyncAdapter)
	{
		final SQLiteDatabase db = mDBHelper.getWritableDatabase();
		int count = 0;
		String accountName = getAccountName(uri);
		String accountType = getAccountType(uri);

		switch (mUriMatcher.match(uri))
		{
		/*
		 * Deleting task lists is only allowed to sync adapters. They must provide ACCOUNT_NAME and ACCOUNT_TYPE.
		 */
			case LIST_ID:
				// add _id to selection and fall through
				selection = updateSelection(selectId(uri), selection);
			case LISTS:
				if (isSyncAdapter)
				{

					if (TextUtils.isEmpty(accountType) || TextUtils.isEmpty(accountName))
					{
						throw new IllegalArgumentException("Sync adapters must specify an account and account type: " + uri);
					}

					selection = updateSelection(selectAccount(uri), selection);

					count = db.delete(Tables.LISTS, selection, selectionArgs);

					updateNotifications();
				}
				else
				{
					throw new UnsupportedOperationException("Caller must be a sync adapter to delete task lists");
				}
				break;

			/*
			 * Task won't be removed, just marked as deleted if the caller isn't a sync adapter. Sync adapters can remove tasks immediately.
			 */
			case TASK_ID:
				// add id to selection and fall through
				selection = updateSelection(selectId(uri), selection);

			case TASKS:
			{
				// TODO: filter by account name and type if present in uri.

				final ContentValues values = new ContentValues();

				if (isSyncAdapter)
				{
					if (TextUtils.isEmpty(accountType) || TextUtils.isEmpty(accountName))
					{
						throw new IllegalArgumentException("Sync adapters must specify an account and account type: " + uri);
					}
				}
				else
				{
					// mark task as deleted and dirty, the sync adapter will remove it later
					values.put(TaskSyncColumns._DELETED, true);
					values.put(CommonSyncColumns._DIRTY, true);
				}

				// iterate over all tasks that match the selection. We iterate "manually" to execute any hooks before or after deletion.
				final Cursor cursor = db.query(Tables.TASKS_VIEW, null, selection, selectionArgs, null, null, null, null);

				int idCol = cursor.getColumnIndex(Tasks._ID);

				// we use a StringBuilder that we can recycle in case multiple tasks are deleted at once
				// even if there is only one task to delete, this won't cause any overhead
				StringBuilder selectionBuilder = new StringBuilder(Tasks._ID);
				selectionBuilder.append("=");
				int selectionBaseLen = selectionBuilder.length();

				try
				{
					while (cursor.moveToNext())
					{
						final long taskId = cursor.getLong(idCol);

						// execute beforeDelete hooks
						executeHooks(new HookExecutor()
						{
							@Override
							public void execute(AbstractTaskHook hook)
							{
								hook.beforeDelete(db, taskId, cursor, isSyncAdapter);
							}
						});

						selectionBuilder.setLength(selectionBaseLen);
						selectionBuilder.append(taskId);

						String taskIdSelection = selectionBuilder.toString();

						if (isSyncAdapter)
						{
							// delete this task
							count += db.delete(Tables.TASKS, taskIdSelection, null);
						}
						else
						{
							// update this task
							db.update(Tables.TASKS, values, taskIdSelection, null);
							/*
							 * For local tasks, this would return 0, because they will be removed in beforeDelete. To ensure the cursors are refreshed, we count
							 * manually.
							 */
							count++;
						}

						// execute afterDelete hooks
						executeHooks(new HookExecutor()
						{
							@Override
							public void execute(AbstractTaskHook hook)
							{
								hook.afterDelete(db, taskId, isSyncAdapter);
							}
						});
					}
				}
				finally
				{
					cursor.close();
				}

				updateNotifications();
				break;
			}
			case ALARM_ID:
				// add id to selection and fall through
				selection = updateSelection(selectId(uri), selection);

			case ALARMS:

				count = db.delete(Tables.ALARMS, selection, selectionArgs);
				break;

			case PROPERTY_ID:
				selection = updateSelection(selectPropertyId(uri), selection);

			case PROPERTIES:
				// fetch all properties that match the selection
				Cursor cursor = db.query(Tables.PROPERTIES, null, selection, selectionArgs, null, null, null);

				try
				{
					int propIdCol = cursor.getColumnIndex(Properties.PROPERTY_ID);
					int taskIdCol = cursor.getColumnIndex(Properties.TASK_ID);
					int mimeTypeCol = cursor.getColumnIndex(Properties.MIMETYPE);
					while (cursor.moveToNext())
					{
						long propertyId = cursor.getLong(propIdCol);
						long taskId = cursor.getLong(taskIdCol);
						String mimeType = cursor.getString(mimeTypeCol);
						if (mimeType != null)
						{
							PropertyHandler handler = PropertyHandlerFactory.get(mimeType);
							count += handler.delete(db, taskId, propertyId, cursor, isSyncAdapter);
						}
					}
				}
				finally
				{
					cursor.close();
				}
				postNotifyUri(Properties.getContentUri(mAuthority));
				break;

			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		if (count > 0)
		{
			postNotifyUri(uri);
			postNotifyUri(Instances.getContentUri(mAuthority));
			postNotifyUri(Tasks.getContentUri(mAuthority));
		}
		return count;
	}


	@Override
	public Uri insertInTransaction(Uri uri, final ContentValues values, final boolean isSyncAdapter)
	{
		final SQLiteDatabase db = mDBHelper.getWritableDatabase();
		long rowId = 0;
		Uri result_uri = null;

		String accountName = getAccountName(uri);
		String accountType = getAccountType(uri);

		switch (mUriMatcher.match(uri))
		{
			case LISTS:
				if (isSyncAdapter)
				{
					validateTaskListValues(values, true, isSyncAdapter);
					// only sync adapter can create task lists!

					if (TextUtils.isEmpty(accountType) || TextUtils.isEmpty(accountName))
					{
						throw new IllegalArgumentException("Sync adapters must specify an account name and an account type: " + uri);
					}

					values.put(TaskContract.ACCOUNT_NAME, accountName);
					values.put(TaskContract.ACCOUNT_TYPE, accountType);
					rowId = db.insert(Tables.LISTS, "", values);
					result_uri = TaskContract.TaskLists.getContentUri(mAuthority);
				}
				else
				{
					throw new UnsupportedOperationException("Caller must be a sync adapter to create task lists");
				}
				break;

			case TASKS:
				validateTaskValues(db, values, true, isSyncAdapter);

				if (!isSyncAdapter)
				{
					// new tasks are always dirty
					values.put(CommonSyncColumns._DIRTY, true);

					// set creation time and last modified
					long currentMillis = System.currentTimeMillis();
					values.put(TaskColumns.CREATED, currentMillis);
					values.put(TaskColumns.LAST_MODIFIED, currentMillis);
				}

				// execute beforeInsert hook
				executeHooks(new HookExecutor()
				{
					@Override
					public void execute(AbstractTaskHook hook)
					{
						hook.beforeInsert(db, values, isSyncAdapter);
					}
				});

				// insert task
				rowId = db.insert(Tables.TASKS, "", values);

				// add entries to Instances
				createInstances(db, uri, values, rowId);

				final long rid = rowId;

				// execute afterInsert hook
				executeHooks(new HookExecutor()
				{
					@Override
					public void execute(AbstractTaskHook hook)
					{
						hook.afterInsert(db, rid, values, isSyncAdapter);
					}
				});

				// insert FTS entries
				FTSDatabaseHelper.updateTaskFTSEntries(db, rowId, values);

				result_uri = TaskContract.Tasks.getContentUri(mAuthority);

				updateNotifications();
				break;

			case PROPERTIES:
				String mimetype = values.getAsString(Properties.MIMETYPE);

				if (mimetype == null)
				{
					throw new IllegalArgumentException("missing mimetype in property values");
				}

				Long taskId = values.getAsLong(Properties.TASK_ID);
				if (taskId == null)
				{
					throw new IllegalArgumentException("missing task id in property values");
				}

				if (values.containsKey(Properties.PROPERTY_ID))
				{
					throw new IllegalArgumentException("property id can not be written");
				}

				PropertyHandler handler = PropertyHandlerFactory.get(mimetype);
				rowId = handler.insert(db, taskId, values, isSyncAdapter);
				result_uri = TaskContract.Properties.getContentUri(mAuthority);
				if (rowId >= 0)
				{
					postNotifyUri(Tasks.getContentUri(mAuthority));
					postNotifyUri(Instances.getContentUri(mAuthority));
				}
				break;

			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		if (rowId > 0 && result_uri != null)
		{
			result_uri = ContentUris.withAppendedId(result_uri, rowId);
			postNotifyUri(result_uri);
			postNotifyUri(uri);
			return result_uri;
		}
		throw new SQLException("Failed to insert row into " + uri);
	}


	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public int updateInTransaction(Uri uri, final ContentValues values, String selection, String[] selectionArgs, final boolean isSyncAdapter)
	{
		final SQLiteDatabase db = mDBHelper.getWritableDatabase();
		int count = 0;
		switch (mUriMatcher.match(uri))
		{
			case LISTS:
				validateTaskListValues(values, false, isSyncAdapter);

				count = db.update(Tables.LISTS, values, selection, selectionArgs);
				break;

			case LIST_ID:
				String newListSelection = updateSelection(selectId(uri), selection);

				validateTaskListValues(values, false, isSyncAdapter);

				count = db.update(Tables.LISTS, values, newListSelection, selectionArgs);
				break;

			case TASK_ID:
				// update selection and fall through
				selection = updateSelection(selectId(uri), selection);

			case TASKS:
			{
				// validate tasks
				validateTaskValues(db, values, false, isSyncAdapter);

				if (!isSyncAdapter)
				{
					// mark task as dirty
					values.put(CommonSyncColumns._DIRTY, true);
					values.put(TaskColumns.LAST_MODIFIED, System.currentTimeMillis());
				}

				// iterate over all tasks that match the selection. We iterate "manually" to execute any hooks before or after insert.
				final Cursor cursor = db.query(Tables.TASKS_VIEW, null, selection, selectionArgs, null, null, null, null);

				int idCol = cursor.getColumnIndex(Tasks._ID);

				// we use a StringBuilder that we can recycle in case multiple tasks are deleted at once
				// even if there is only one task to delete, this won't cause any overhead
				StringBuilder selectionBuilder = new StringBuilder(Tasks._ID);
				selectionBuilder.append("=");
				int selectionBaseLen = selectionBuilder.length();
				try
				{
					while (cursor.moveToNext())
					{
						final long taskId = cursor.getLong(idCol);

						// execute beforeUpdate hooks
						executeHooks(new HookExecutor()
						{
							@Override
							public void execute(AbstractTaskHook hook)
							{
								hook.beforeUpdate(db, taskId, cursor, values, isSyncAdapter);
							}
						});

						selectionBuilder.setLength(selectionBaseLen);
						selectionBuilder.append(taskId);

						String taskIdSelection = selectionBuilder.toString();

						// update this task
						count += db.update(Tables.TASKS, values, taskIdSelection, null);
						updateInstancesOfOneTask(db, taskId, values, taskIdSelection, null);

						// execute afterUpdate hooks
						executeHooks(new HookExecutor()
						{
							@Override
							public void execute(AbstractTaskHook hook)
							{
								hook.afterUpdate(db, taskId, cursor, values, isSyncAdapter);
							}
						});
					}
				}
				finally
				{
					cursor.close();
				}

				updateNotifications();
				break;
			}

			case PROPERTY_ID:
				selection = updateSelection(selectPropertyId(uri), selection);

			case PROPERTIES:
				if (values.containsKey(Properties.MIMETYPE))
				{
					throw new IllegalArgumentException("property mimetypes can not be modified");
				}

				if (values.containsKey(Properties.TASK_ID))
				{
					throw new IllegalArgumentException("task id can not be changed");
				}

				if (values.containsKey(Properties.PROPERTY_ID))
				{
					throw new IllegalArgumentException("property id can not be changed");
				}

				// fetch all properties that match the selection
				Cursor cursor = db.query(Tables.PROPERTIES, null, selection, selectionArgs, null, null, null);

				try
				{
					int propIdCol = cursor.getColumnIndex(Properties.PROPERTY_ID);
					int taskIdCol = cursor.getColumnIndex(Properties.TASK_ID);
					int mimeTypeCol = cursor.getColumnIndex(Properties.MIMETYPE);
					while (cursor.moveToNext())
					{
						long propertyId = cursor.getLong(propIdCol);
						long taskId = cursor.getLong(taskIdCol);
						String mimeType = cursor.getString(mimeTypeCol);
						if (mimeType != null)
						{
							PropertyHandler handler = PropertyHandlerFactory.get(mimeType);
							count += handler.update(db, taskId, propertyId, values, cursor, isSyncAdapter);
						}
					}
				}
				finally
				{
					cursor.close();
				}
				postNotifyUri(Properties.getContentUri(mAuthority));
				break;

			case CATEGORY_ID:
				String newCategorySelection = updateSelection(selectId(uri), selection);
				validateCategoryValues(values, false, isSyncAdapter);
				count = db.update(Tables.CATEGORIES, values, newCategorySelection, selectionArgs);
				break;
			case ALARM_ID:
				String newAlarmSelection = updateSelection(selectId(uri), selection);
				validateAlarmValues(values, false, isSyncAdapter);
				count = db.update(Tables.ALARMS, values, newAlarmSelection, selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// get the keys in values
		Set<String> keys;
		if (android.os.Build.VERSION.SDK_INT < 11)
		{
			keys = new HashSet<String>();
			for (Entry<String, Object> entry : values.valueSet())
			{
				keys.add(entry.getKey());
			}
		}
		else
		{
			keys = values.keySet();
		}

		if (!TASK_LIST_SYNC_COLUMNS.containsAll(keys))
		{
			// send notifications, because non-sync columns have been updated
			postNotifyUri(uri);
		}

		return count;
	}


	/**
	 * Update task due and task start notifications.
	 */
	private void updateNotifications()
	{
		// update alarms
		Context context = getContext();
		DueAlarmBroadcastHandler.setUpcomingDueAlarm(context, mDb, System.currentTimeMillis());
		StartAlarmBroadcastHandler.setUpcomingStartAlarm(context, mDb, System.currentTimeMillis());
	}


	/**
	 * Create new {@link ContentValues} for insertion into the instances table and initialize dates & times with task {@link ContentValues}.
	 * 
	 * @param values
	 *            {@link ContentValues} of a task.
	 * @return {@link ContentValues} of the instance of this task.
	 */
	private ContentValues setInstanceTimes(ContentValues values)
	{
		ContentValues instanceValues = new ContentValues();

		// get the relevant values from values
		Long dtstart = values.getAsLong(Tasks.DTSTART);
		Long due = values.getAsLong(Tasks.DUE);
		String durationStr = values.getAsString(Tasks.DURATION);

		if (values.containsKey(TaskColumns.DTSTART))
		{
			// copy dtstart as is
			instanceValues.put(Instances.INSTANCE_START, dtstart);
		}

		if (values.containsKey(TaskColumns.DUE))
		{
			// copy due and calculate the actual duration, if any
			instanceValues.put(Instances.INSTANCE_DUE, due);
			if (dtstart != null && due != null)
			{
				Long instanceDuration = due - dtstart;
				instanceValues.put(Instances.INSTANCE_DURATION, instanceDuration);
			}
			else
			{
				instanceValues.putNull(Instances.INSTANCE_DURATION);
			}
		}

		if (values.containsKey(TaskColumns.DURATION) && due == null) // actually due and duration should not be set at the same time
		{
			if (durationStr != null && dtstart != null)
			{
				// calculate the actual due value from dtstart and the duration string
				Duration duration = new Duration(durationStr);
				Time tStart = new Time(values.getAsString(Tasks.TZ));
				Boolean isAllDay = values.getAsBoolean(Tasks.IS_ALLDAY);
				if (isAllDay != null)
				{
					tStart.allDay = isAllDay;
				}
				tStart.set(dtstart);
				Long instanceDue = duration.addTo(tStart).toMillis(false);
				instanceValues.put(Instances.INSTANCE_DUE, instanceDue);
				// actual duration is the difference between due and dtstart
				Long instanceDuration = instanceDue - dtstart;
				instanceValues.put(Instances.INSTANCE_DURATION, instanceDuration);
			}
			else
			{
				instanceValues.putNull(Instances.INSTANCE_DURATION);
				instanceValues.putNull(Instances.INSTANCE_DUE);
			}
		}
		return instanceValues;
	}


	/**
	 * Creates new instances for the given task {@link ContentValues}.
	 * <p>
	 * TODO: expand recurrence
	 * </p>
	 * 
	 * @param uri
	 *            The {@link Uri} used when inserting the task.
	 * @param values
	 *            The {@link ContentValues} of the task.
	 * @param rowId
	 *            The new {@link RowId} of the task.
	 */
	private void createInstances(SQLiteDatabase db, Uri uri, ContentValues values, long rowId)
	{
		ContentValues instanceValues = setInstanceTimes(values);

		// set rowID of current Task
		instanceValues.put(Instances.TASK_ID, rowId);

		String tz = values.getAsString(Instances.TZ);
		boolean allday = values.getAsInteger(Tasks.IS_ALLDAY) != null && values.getAsInteger(Tasks.IS_ALLDAY) > 0;

		// add start sorting if start value is present
		Long instanceStart = instanceValues.getAsLong(Instances.INSTANCE_START);
		if (instanceStart != null)
		{
			instanceValues
				.put(Instances.INSTANCE_START_SORTING, instanceStart + (tz == null || allday ? 0 : TimeZone.getTimeZone(tz).getOffset(instanceStart)));
		}

		// add due sorting if due value is present
		Long instanceDue = instanceValues.getAsLong(Instances.INSTANCE_DUE);
		if (instanceDue != null)
		{
			instanceValues.put(Instances.INSTANCE_DUE_SORTING, instanceDue + (tz == null || allday ? 0 : TimeZone.getTimeZone(tz).getOffset(instanceDue)));
		}

		db.insert(Tables.INSTANCES, null, instanceValues);
		postNotifyUri(Instances.getContentUri(mAuthority));
	}


	private void updateInstancesOfOneTask(SQLiteDatabase db, long task_id, ContentValues values, String selection, String[] selectionArgs)
	{
		// check if either one of the following has been updated: DTSTART, DUE, DURATION, RRULE, RDATE, EXDATE
		// right now we only update DTSTART, DUE and DURATION

		/*
		 * DTSTART, DUE, DURATION, RRULE, RDATE, EXDATE in values? => update recurrence set
		 * 
		 * DTSTART, DUE, DURATION => update values (in this very instance)
		 * 
		 * All values must be given! DTSTART + DURATION -> DUE must be omitted DSTART + DUE -> DURATION must be omitted
		 */

		ContentValues instanceValues = setInstanceTimes(values);
		instanceValues.put(Instances.TASK_ID, task_id);

		if (values.getAsString(Tasks.RRULE) != null || values.getAsString(Tasks.RDATE) != null || values.getAsString(Tasks.EXDATE) != null)
		{
			// TODO: update recurrence table
		}

		/*
		 * Calculate sorting values for start and due times. If start or due values are non-null and non-allday we add the time zone offset to UTC. That ensures
		 * allday events (which are always in UTC) are sorted properly, independent of any time zone.
		 */
		String tz = values.getAsString(Instances.TZ);
		Integer allday = values.getAsInteger(Tasks.IS_ALLDAY);

		Long instanceStart = instanceValues.getAsLong(Instances.INSTANCE_START);
		if (instanceStart != null)
		{
			instanceValues.put(Instances.INSTANCE_START_SORTING, instanceStart
				+ (tz == null || (allday != null && allday > 0) ? 0 : TimeZone.getTimeZone(tz).getOffset(instanceStart)));
		}
		else if (values.containsKey(Tasks.DTSTART))
		{
			// dtstart must have been set to null, so remove sorting value
			instanceValues.putNull(Instances.INSTANCE_START_SORTING);
		}

		Long instanceDue = instanceValues.getAsLong(Instances.INSTANCE_DUE);
		if (instanceDue != null)
		{
			instanceValues.put(Instances.INSTANCE_DUE_SORTING, instanceDue
				+ (tz == null || (allday != null && allday > 0) ? 0 : TimeZone.getTimeZone(tz).getOffset(instanceDue)));
		}
		else if (values.containsKey(Tasks.DUE))
		{
			// due must have been set to null, so remove sorting value
			instanceValues.putNull(Instances.INSTANCE_DUE_SORTING);
		}

		db.update(Tables.INSTANCES, instanceValues, selection, selectionArgs);

		// update FTS entries
		FTSDatabaseHelper.updateTaskFTSEntries(db, task_id, values);

		postNotifyUri(Instances.getContentUri(mAuthority));
	}


	/**
	 * Validate the given task list values.
	 * 
	 * @param values
	 *            The task list properties to validate.
	 * @throws IllegalArgumentException
	 *             if any of the values is invalid.
	 */
	private void validateTaskListValues(ContentValues values, boolean isNew, boolean isSyncAdapter)
	{
		// row id can not be changed or set manually
		if (values.containsKey(TaskColumns._ID))
		{
			throw new IllegalArgumentException("_ID can not be set manually");
		}

		if (isNew != values.containsKey(TaskListSyncColumns.ACCOUNT_NAME) && (!isNew || values.get(TaskListSyncColumns.ACCOUNT_NAME) != null))
		{
			throw new IllegalArgumentException("ACCOUNT_NAME is write-once and required on INSERT");
		}

		if (isNew != values.containsKey(TaskListSyncColumns.ACCOUNT_TYPE) && (!isNew || values.get(TaskListSyncColumns.ACCOUNT_TYPE) != null))
		{
			throw new IllegalArgumentException("ACCOUNT_TYPE is write-once and required on INSERT");
		}

		if (!isSyncAdapter && values.containsKey(TaskLists.LIST_COLOR))
		{
			throw new IllegalArgumentException("Only sync adapters can change the LIST_COLOR.");
		}
		if (!isSyncAdapter && values.containsKey(TaskLists._SYNC_ID))
		{
			throw new IllegalArgumentException("Only sync adapters can change the _SYNC_ID.");
		}
		if (!isSyncAdapter && values.containsKey(TaskLists.SYNC_VERSION))
		{
			throw new IllegalArgumentException("Only sync adapters can change SYNC_VERSION.");
		}
	}


	/**
	 * Validate the given task values.
	 * 
	 * @param values
	 *            The task properties to validate.
	 * @throws IllegalArgumentException
	 *             if any of the values is invalid.
	 */
	private void validateTaskValues(SQLiteDatabase db, ContentValues values, boolean isNew, boolean isSyncAdapter)
	{
		// row id can not be changed or set manually
		if (values.containsKey(TaskColumns._ID))
		{
			throw new IllegalArgumentException("_ID can not be set manually");
		}

		// setting a LIST_ID is allowed only for new tasks, it must also refer to an existing TaskList
		// TODO: cache valid ids to speed up inserts
		if (isNew)
		{
			String[] listId = { values.getAsString(TaskColumns.LIST_ID) };
			if (listId[0] == null)
			{
				throw new IllegalArgumentException("LIST_ID is required on INSERT");
			}

			Cursor cursor = db.query(Tables.LISTS, TASKLIST_ID_PROJECTION, TASKLISTS_ID_SELECTION, listId, null, null, null);
			try
			{
				if (cursor == null || cursor.getCount() != 1)
				{
					throw new IllegalArgumentException("LIST_ID must refer to an existing TaskList");
				}
			}
			finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}
		}
		else if (values.containsKey(TaskColumns.LIST_ID))
		{
			throw new IllegalArgumentException("LIST_ID is write-once");
		}

		if (!isSyncAdapter && !isNew && (values.containsKey(Tasks.ORIGINAL_INSTANCE_SYNC_ID) || values.containsKey(Tasks.ORIGINAL_INSTANCE_ID)))
		{
			throw new IllegalArgumentException("ORIGINAL_INSTANCE_SYNC_ID and ORIGINAL_INSTANCE_ID can be modified by sync adapters only");
		}

		if (values.containsKey(Tasks.ORIGINAL_INSTANCE_SYNC_ID) && values.containsKey(Tasks.ORIGINAL_INSTANCE_ID))
		{
			throw new IllegalArgumentException("ORIGINAL_INSTANCE_SYNC_ID and ORIGINAL_INSTANCE_ID must not be specified at the same time");
		}

		// Find corresponding ORIGINAL_INSTANCE_ID
		if (values.get(Tasks.ORIGINAL_INSTANCE_SYNC_ID) != null)
		{
			String[] syncId = { values.getAsString(Tasks.ORIGINAL_INSTANCE_SYNC_ID) };
			Cursor cursor = db.query(Tables.TASKS, TASK_ID_PROJECTION, SYNC_ID_SELECTION, syncId, null, null, null);
			try
			{
				if (cursor != null && cursor.getCount() == 1)
				{
					cursor.moveToNext();
					Long originalId = cursor.getLong(0);
					values.put(Tasks.ORIGINAL_INSTANCE_ID, originalId);
				}
			}
			finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}
		}
		else if (values.get(Tasks.ORIGINAL_INSTANCE_ID) != null) // Find corresponding ORIGINAL_INSTANCE_SYNC_ID
		{
			String[] id = { values.getAsString(Tasks.ORIGINAL_INSTANCE_ID) };
			Cursor cursor = db.query(Tables.TASKS, TASK_SYNC_ID_PROJECTION, TASK_ID_SELECTION, id, null, null, null);
			try
			{
				if (cursor != null && cursor.getCount() == 1)
				{
					cursor.moveToNext();
					String originalSyncId = cursor.getString(0);
					values.put(Tasks.ORIGINAL_INSTANCE_SYNC_ID, originalSyncId);
				}
			}
			finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}
		}

		// account name is read only for tasks
		if (values.containsKey(Tasks.ACCOUNT_NAME))
		{
			throw new IllegalArgumentException("ACCOUNT_NAME is read-only for tasks");
		}

		// account type is read only for tasks
		if (values.containsKey(Tasks.ACCOUNT_TYPE))
		{
			throw new IllegalArgumentException("ACCOUNT_TYPE is read-only for tasks");
		}

		// list color is read only for tasks
		if (values.containsKey(Tasks.LIST_COLOR))
		{
			throw new IllegalArgumentException("LIST_COLOR is read-only for tasks");
		}

		// no one can undelete a task!
		if (values.containsKey(TaskSyncColumns._DELETED))
		{
			throw new IllegalArgumentException("modification of _DELETE is not allowed");
		}

		// only sync adapters are allowed to change the UID
		if (!isSyncAdapter && values.containsKey(TaskSyncColumns._UID))
		{
			throw new IllegalArgumentException("modification of _UID is not allowed");
		}

		// only sync adapters are allowed to remove the dirty flag
		if (!isSyncAdapter && values.containsKey(CommonSyncColumns._DIRTY))
		{
			throw new IllegalArgumentException("modification of _DIRTY is not allowed");
		}

		// only sync adapters are allowed to set creation time
		if (!isSyncAdapter && values.containsKey(TaskColumns.CREATED))
		{
			throw new IllegalArgumentException("modification of CREATED is not allowed");
		}

		// IS_NEW is set automatically
		if (values.containsKey(Tasks.IS_NEW))
		{
			throw new IllegalArgumentException("modification of IS_NEW is not allowed");
		}

		// IS_CLOSED is set automatically
		if (values.containsKey(Tasks.IS_CLOSED))
		{
			throw new IllegalArgumentException("modification of IS_CLOSED is not allowed");
		}

		// only sync adapters are allowed to set modification time
		if (!isSyncAdapter && values.containsKey(TaskColumns.LAST_MODIFIED))
		{
			throw new IllegalArgumentException("modification of MODIFICATION_TIME is not allowed");
		}

		// check that PRIORITY is an Integer between 0 and 9 if given
		if (values.containsKey(TaskColumns.PRIORITY))
		{
			Integer priority = values.getAsInteger(TaskColumns.PRIORITY);
			if (priority != null && (priority < 0 || priority > 9))
			{
				throw new IllegalArgumentException("PRIORITY must be an integer between 0 and 9");
			}
			else if (priority != null && priority == 0)
			{
				// replace priority 0 by null, we need that for proper sorting
				values.putNull(TaskColumns.PRIORITY);
			}
		}

		// check that CLASSIFICATION is an Integer between 0 and 2 if given
		if (values.containsKey(TaskColumns.CLASSIFICATION))
		{
			Integer classification = values.getAsInteger(TaskColumns.CLASSIFICATION);
			if (classification != null && (classification < 0 || classification > 2))
			{
				throw new IllegalArgumentException("CLASSIFICATION must be an integer between 0 and 2");
			}
		}

		// ensure that DUE and DURATION are set properly if DTSTART is given
		Long dtStart = values.getAsLong(TaskColumns.DTSTART);
		Long due = values.getAsLong(TaskColumns.DUE);
		String duration = values.getAsString(TaskColumns.DURATION);

		if (dtStart != null)
		{
			if (due != null && duration != null)
			{
				throw new IllegalArgumentException("Only one of DUE or DURATION must be supplied.");
			}
			else if (due != null)
			{
				if (due < dtStart)
				{
					throw new IllegalArgumentException("DUE must not be < DTSTART");
				}
			}
			else if (duration != null)
			{
				Duration d = new Duration(duration); // throws exception if duration string is invalid
				if (d.sign == -1)
				{
					throw new IllegalArgumentException("DURATION must not be negative");
				}
			}
		}
		else if (duration != null)
		{
			throw new IllegalArgumentException("DURATION must not be supplied without DTSTART");
		}

		// if one of DTSTART or DUE is given, TZ must not be null
		if ((dtStart != null || due != null) && !ONE.equals(values.getAsInteger(TaskColumns.IS_ALLDAY)) && values.getAsString(TaskColumns.TZ) == null)
		{
			throw new IllegalArgumentException("TIMEZONE must be supplied if one of DTSTART or DUE is not null");
		}

		// set proper STATUS if task has been completed
		if (!isSyncAdapter && values.getAsLong(Tasks.COMPLETED) != null && !values.containsKey(Tasks.STATUS))
		{
			values.put(Tasks.STATUS, Tasks.STATUS_COMPLETED);
		}

		// check that PERCENT_COMPLETE is an Integer between 0 and 100 if supplied also update status and completed accordingly
		if (values.containsKey(TaskColumns.PERCENT_COMPLETE))
		{
			Integer percent = values.getAsInteger(TaskColumns.PERCENT_COMPLETE);
			if (percent != null && (percent < 0 || percent > 100))
			{
				throw new IllegalArgumentException("PERCENT_COMPLETE must be null or an integer between 0 and 100");
			}

			if (!isSyncAdapter && percent != null && percent == 100)
			{
				if (!values.containsKey(Tasks.STATUS))
				{
					values.put(Tasks.STATUS, Tasks.STATUS_COMPLETED);
				}

				if (!values.containsKey(Tasks.COMPLETED))
				{
					values.put(Tasks.COMPLETED, System.currentTimeMillis());
					values.put(Tasks.COMPLETED_IS_ALLDAY, 0);
				}
			}
			else if (!isSyncAdapter && percent != null)
			{
				if (!values.containsKey(Tasks.COMPLETED))
				{
					values.putNull(Tasks.COMPLETED);
				}
			}
		}

		// validate STATUS and set IS_NEW and IS_CLOSED accordingly
		if (values.containsKey(Tasks.STATUS) || isNew)
		{
			Integer status = values.getAsInteger(Tasks.STATUS);
			if (status == null)
			{
				status = Tasks.STATUS_DEFAULT;
				values.put(Tasks.STATUS, status);
			}
			else if (status < Tasks.STATUS_NEEDS_ACTION || status > Tasks.STATUS_CANCELLED)
			{
				throw new IllegalArgumentException("invalid STATUS: " + status);
			}
			values.put(Tasks.IS_NEW, status == null || status == Tasks.STATUS_NEEDS_ACTION ? 1 : 0);
			values.put(Tasks.IS_CLOSED, status != null && (status == Tasks.STATUS_COMPLETED || status == Tasks.STATUS_CANCELLED) ? 1 : 0);

			/*
			 * Update PERCENT_COMPLETE and COMPLETED (if not given). Sync adapters should know what they're doing, so don't update anything if caller is a sync
			 * adapter.
			 */
			if (status == Tasks.STATUS_COMPLETED && !isSyncAdapter)
			{
				values.put(Tasks.PERCENT_COMPLETE, 100);
				if (!values.containsKey(Tasks.COMPLETED))
				{
					values.put(Tasks.COMPLETED, System.currentTimeMillis());
					values.put(Tasks.COMPLETED_IS_ALLDAY, 0);
				}
			}
			else if (!isSyncAdapter)
			{
				values.putNull(Tasks.COMPLETED);
			}
		}
	}


	/**
	 * Validate the given category values.
	 * 
	 * @param values
	 *            The category properties to validate.
	 * @throws IllegalArgumentException
	 *             if any of the values is invalid.
	 */
	private void validateCategoryValues(ContentValues values, boolean isNew, boolean isSyncAdapter)
	{
		// row id can not be changed or set manually
		if (values.containsKey(Categories._ID))
		{
			throw new IllegalArgumentException("_ID can not be set manually");
		}

		if (isNew != values.containsKey(Categories.ACCOUNT_NAME) && (!isNew || values.get(Categories.ACCOUNT_NAME) != null))
		{
			throw new IllegalArgumentException("ACCOUNT_NAME is write-once and required on INSERT");
		}

		if (isNew != values.containsKey(Categories.ACCOUNT_TYPE) && (!isNew || values.get(Categories.ACCOUNT_TYPE) != null))
		{
			throw new IllegalArgumentException("ACCOUNT_TYPE is write-once and required on INSERT");
		}
	}


	/**
	 * Validate the given alarm values.
	 * 
	 * @param values
	 *            The alarm values to validate
	 * @throws IllegalArgumentException
	 *             if any of the values is invalid.
	 */
	private void validateAlarmValues(ContentValues values, boolean isNew, boolean isSyncAdapter)
	{
		if (values.containsKey(Alarms.ALARM_ID))
		{
			throw new IllegalArgumentException("ALARM_ID can not be set manually");
		}
	}


	/**
	 * Execute the given {@link HookExecutor} for all task hooks.
	 * 
	 * @param executor
	 *            The {@link HookExecutor} to execute;
	 */
	private void executeHooks(HookExecutor executor)
	{
		long start = System.currentTimeMillis();
		for (AbstractTaskHook hook : TASK_HOOKS)
		{
			executor.execute(hook);
		}
		Log.d("TaskProvider", "time to process hooks " + (System.currentTimeMillis() - start) + " ms");
	}


	@Override
	public String getType(Uri uri)
	{
		/**
		 * TODO: create the types in advance
		 */
		switch (mUriMatcher.match(uri))
		{
			case LISTS:
				return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + mAuthority + "." + TaskLists.CONTENT_URI_PATH;
			case LIST_ID:
				return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + mAuthority + "." + TaskLists.CONTENT_URI_PATH;
			case TASKS:
				return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + mAuthority + "." + Tasks.CONTENT_URI_PATH;
			case TASK_ID:
				return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + mAuthority + "." + Tasks.CONTENT_URI_PATH;
			case INSTANCES:
				return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + mAuthority + "." + Instances.CONTENT_URI_PATH;
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	static
	{

	}


	@Override
	protected void onEndTransaction(boolean callerIsSyncAdapter)
	{
		super.onEndTransaction(callerIsSyncAdapter);
		Utils.sendActionProviderChangedBroadCast(getContext(), mAuthority);
	};


	@Override
	public SQLiteOpenHelper getDatabaseHelper(Context context)
	{
		synchronized (this)
		{
			if (mDBHelper == null)
			{
				mDBHelper = getDatabaseHelperStatic(context);
			}
			return mDBHelper;
		}
	}


	public static TaskDatabaseHelper getDatabaseHelperStatic(Context context)
	{
		return new TaskDatabaseHelper(context);
	}


	@Override
	protected boolean syncToNetwork(Uri uri)
	{
		return true;
	}
}
