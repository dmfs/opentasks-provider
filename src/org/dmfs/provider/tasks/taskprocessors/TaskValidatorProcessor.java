/*
 * Copyright (C) 2015 Marten Gajda <marten@dmfs.org>
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

package org.dmfs.provider.tasks.taskprocessors;

import org.dmfs.provider.tasks.TaskContract.TaskLists;
import org.dmfs.provider.tasks.TaskContract.Tasks;
import org.dmfs.provider.tasks.TaskDatabaseHelper.Tables;
import org.dmfs.provider.tasks.model.TaskAdapter;
import org.dmfs.provider.tasks.model.TaskFieldAdapters;
import org.dmfs.rfc5545.Duration;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * A {@link TaskProcessor} to validate the values of a task.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
public class TaskValidatorProcessor extends AbstractTaskProcessor
{

	private static final String[] TASKLIST_ID_PROJECTION = { TaskLists._ID };
	private static final String TASKLISTS_ID_SELECTION = TaskLists._ID + "=";


	@Override
	public void beforeInsert(SQLiteDatabase db, TaskAdapter task, boolean isSyncAdapter)
	{
		verifyCommon(task, isSyncAdapter);

		// LIST_ID must be present and refer to an existing TaskList row id
		Long listId = task.valueOf(TaskFieldAdapters.LIST_ID);
		if (listId == null)
		{
			throw new IllegalArgumentException("LIST_ID is required on INSERT");
		}

		// TODO: get rid of this query and use a cache instead
		// TODO: ensure that the list is writable unless the caller is a sync adapter
		Cursor cursor = db.query(Tables.LISTS, TASKLIST_ID_PROJECTION, TASKLISTS_ID_SELECTION + listId, null, null, null, null);
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


	@Override
	public void beforeUpdate(SQLiteDatabase db, TaskAdapter task, boolean isSyncAdapter)
	{
		verifyCommon(task, isSyncAdapter);

		// setting a LIST_ID is allowed only for new tasks
		if (task.isUpdated(TaskFieldAdapters.LIST_ID))
		{
			throw new IllegalArgumentException("LIST_ID is write-once");
		}

		// only sync adapters can modify original sync id and original instance id of an existing task
		if (!isSyncAdapter && (task.isUpdated(TaskFieldAdapters.ORIGINAL_INSTANCE_ID) || task.isUpdated(TaskFieldAdapters.ORIGINAL_INSTANCE_SYNC_ID)))
		{
			throw new IllegalArgumentException("ORIGINAL_INSTANCE_SYNC_ID and ORIGINAL_INSTANCE_ID can be modified by sync adapters only");
		}
	}


	/**
	 * Performs tests that are common to insert an update opeations.
	 * 
	 * @param task
	 *            The {@link TaskAdapter} to verify.
	 * @param isSyncAdapter
	 *            <code>true</code> if the caller is a sync adapter, false otherwise.
	 */
	private void verifyCommon(TaskAdapter task, boolean isSyncAdapter)
	{
		// row id can not be changed or set manually
		if (task.isUpdated(TaskFieldAdapters._ID))
		{
			throw new IllegalArgumentException("_ID can not be set manually");
		}

		// account name can not be set on a tasks
		if (task.isUpdated(TaskFieldAdapters.ACCOUNT_NAME))
		{
			throw new IllegalArgumentException("ACCOUNT_NAME can not be set on a tasks");
		}

		// account type can not be set on a tasks
		if (task.isUpdated(TaskFieldAdapters.ACCOUNT_TYPE))
		{
			throw new IllegalArgumentException("ACCOUNT_TYPE can not be set on a tasks");
		}

		// list color is read only for tasks
		if (task.isUpdated(TaskFieldAdapters.LIST_COLOR))
		{
			throw new IllegalArgumentException("LIST_COLOR can not be set on a tasks");
		}

		// no one can undelete a task!
		if (task.isUpdated(TaskFieldAdapters._DELETED))
		{
			throw new IllegalArgumentException("modification of _DELETE is not allowed");
		}

		// only sync adapters are allowed to change the UID
		if (!isSyncAdapter && task.isUpdated(TaskFieldAdapters._UID))
		{
			throw new IllegalArgumentException("modification of _UID is not allowed");
		}

		// only sync adapters are allowed to remove the dirty flag
		if (!isSyncAdapter && task.isUpdated(TaskFieldAdapters._DIRTY))
		{
			throw new IllegalArgumentException("modification of _DIRTY is not allowed");
		}

		// only sync adapters are allowed to set creation time
		if (!isSyncAdapter && task.isUpdated(TaskFieldAdapters.CREATED))
		{
			throw new IllegalArgumentException("modification of CREATED is not allowed");
		}

		// IS_NEW is set automatically
		if (task.isUpdated(TaskFieldAdapters.IS_NEW))
		{
			throw new IllegalArgumentException("modification of IS_NEW is not allowed");
		}

		// IS_CLOSED is set automatically
		if (task.isUpdated(TaskFieldAdapters.IS_CLOSED))
		{
			throw new IllegalArgumentException("modification of IS_CLOSED is not allowed");
		}

		// HAS_PROPERTIES is set automatically
		if (task.isUpdated(TaskFieldAdapters.HAS_PROPERTIES))
		{
			throw new IllegalArgumentException("modification of HAS_PROPERTIES is not allowed");
		}

		// HAS_ALARMS is set automatically
		if (task.isUpdated(TaskFieldAdapters.HAS_ALARMS))
		{
			throw new IllegalArgumentException("modification of HAS_ALARMS is not allowed");
		}

		// only sync adapters are allowed to set modification time
		if (!isSyncAdapter && task.isUpdated(TaskFieldAdapters.LAST_MODIFIED))
		{
			throw new IllegalArgumentException("modification of MODIFICATION_TIME is not allowed");
		}

		if (task.isUpdated(TaskFieldAdapters.ORIGINAL_INSTANCE_SYNC_ID) && task.isUpdated(TaskFieldAdapters.ORIGINAL_INSTANCE_SYNC_ID))
		{
			throw new IllegalArgumentException("ORIGINAL_INSTANCE_SYNC_ID and ORIGINAL_INSTANCE_ID must not be specified at the same time");
		}

		// check that CLASSIFICATION is an Integer between 0 and 2 if given
		if (task.isUpdated(TaskFieldAdapters.CLASSIFICATION))
		{
			Integer classification = task.valueOf(TaskFieldAdapters.CLASSIFICATION);
			if (classification != null && (classification < 0 || classification > 2))
			{
				throw new IllegalArgumentException("CLASSIFICATION must be an integer between 0 and 2");
			}
		}

		// check that PRIORITY is an Integer between 0 and 9 if given
		if (task.isUpdated(TaskFieldAdapters.PRIORITY))
		{
			Integer priority = task.valueOf(TaskFieldAdapters.PRIORITY);
			if (priority != null && (priority < 0 || priority > 9))
			{
				throw new IllegalArgumentException("PRIORITY must be an integer between 0 and 9");
			}
		}

		// check that PERCENT_COMPLETE is an Integer between 0 and 100
		if (task.isUpdated(TaskFieldAdapters.PERCENT_COMPLETE))
		{
			Integer percent = task.valueOf(TaskFieldAdapters.PERCENT_COMPLETE);
			if (percent != null && (percent < 0 || percent > 100))
			{
				throw new IllegalArgumentException("PERCENT_COMPLETE must be null or an integer between 0 and 100");
			}
		}

		// validate STATUS
		if (task.isUpdated(TaskFieldAdapters.STATUS))
		{
			Integer status = task.valueOf(TaskFieldAdapters.STATUS);
			if (status != null && (status < Tasks.STATUS_NEEDS_ACTION || status > Tasks.STATUS_CANCELLED))
			{
				throw new IllegalArgumentException("invalid STATUS: " + status);
			}
		}

		// ensure that DUE and DURATION are set properly if DTSTART is given
		Long dtStart = task.valueOf(TaskFieldAdapters.DTSTART_RAW);
		Long due = task.valueOf(TaskFieldAdapters.DUE_RAW);
		Duration duration = task.valueOf(TaskFieldAdapters.DURATION);

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
				if (duration.getSign() == -1)
				{
					throw new IllegalArgumentException("DURATION must not be negative");
				}
			}
		}
		else if (duration != null)
		{
			throw new IllegalArgumentException("DURATION must not be supplied without DTSTART");
		}

		// if one of DTSTART or DUE is given, TZ must not be null unless it's an all-day task
		if ((dtStart != null || due != null) && !task.valueOf(TaskFieldAdapters.IS_ALLDAY) && task.valueOf(TaskFieldAdapters.TIMEZONE_RAW) == null)
		{
			throw new IllegalArgumentException("TIMEZONE must be supplied if one of DTSTART or DUE is not null and not all-day");
		}
	}
}
