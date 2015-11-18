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

import org.dmfs.provider.tasks.TaskContract;
import org.dmfs.provider.tasks.TaskDatabaseHelper;
import org.dmfs.provider.tasks.model.CursorContentValuesTaskAdapter;
import org.dmfs.provider.tasks.model.TaskAdapter;
import org.dmfs.provider.tasks.model.TaskFieldAdapters;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * This {@link TaskProcessor} makes sure that changing the list a task belongs is properly handled by sync adapters. This is achieved by emulating an atomic
 * copy & delete operation.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
public class ChangeListProcessor extends AbstractTaskProcessor
{

	@Override
	public void beforeUpdate(SQLiteDatabase db, TaskAdapter task, boolean isSyncAdapter)
	{
		if (isSyncAdapter)
		{
			// sync-adapters have to implement the move logic themselves
			return;
		}

		if (!task.isUpdated(TaskFieldAdapters.LIST_ID))
		{
			// list has not been changed
			return;
		}

		long oldList = task.oldValueOf(TaskFieldAdapters.LIST_ID);
		long newList = task.valueOf(TaskFieldAdapters.LIST_ID);

		if (oldList == newList)
		{
			// list has not been changed
			return;
		}

		Long newMasterId;
		Long deletedMasterId = null;

		if (task.valueOf(TaskFieldAdapters.ORIGINAL_INSTANCE_ID) != null || task.valueOf(TaskFieldAdapters.ORIGINAL_INSTANCE_SYNC_ID) != null)
		{
			// this is an exception, move the master first
			newMasterId = task.valueOf(TaskFieldAdapters.ORIGINAL_INSTANCE_ID);
			if (newMasterId != null)
			{
				// find the master task
				Cursor c = db.query(TaskDatabaseHelper.Tables.TASKS, null, TaskContract.Tasks._ID + "=" + newMasterId, null, null, null, null);
				try
				{
					if (c.moveToFirst())
					{
						// move the master task
						deletedMasterId = moveTask(db, new CursorContentValuesTaskAdapter(newMasterId, c, new ContentValues(16)), oldList, newList, null, true);
					}

				}
				finally
				{
					c.close();
				}
			}

			// now move this exception, make sure we link the deleted exception to the deleted master
			moveTask(db, task, oldList, newList, deletedMasterId, false);
		}
		else
		{
			newMasterId = task.id();
			// move the task to the new list
			deletedMasterId = moveTask(db, task, oldList, newList, null, false);
		}

		if (task.isRecurring() || task.valueOf(TaskFieldAdapters.ORIGINAL_INSTANCE_ID) != null)
		{
			// This task is recurring and may have exceptions or it's an exception itself. Move all (other) exceptions to the new list.
			Cursor c = db.query(TaskDatabaseHelper.Tables.TASKS, null, TaskContract.Tasks.ORIGINAL_INSTANCE_ID + "=" + newMasterId + " and "
				+ TaskContract.Tasks._ID + "!=" + task.id(), null, null, null, null);
			try
			{
				while (c.moveToNext())
				{
					moveTask(db, new CursorContentValuesTaskAdapter(TaskFieldAdapters._ID.getFrom(c), c, new ContentValues(16)), oldList, newList,
						deletedMasterId, true);
				}
			}
			finally
			{
				c.close();
			}
		}

	}


	private Long moveTask(SQLiteDatabase db, TaskAdapter task, long oldList, long newList, Long deletedOriginalId, boolean commitTask)
	{
		/*
		 * The task has been moved to a different list. Sync adapters are not expected to support this (especially since the new list may belong to a completely
		 * different account or even account-type), so we emulate a copy & delete operation.
		 * 
		 * All sync adapter fields of the task are cleared, so it looks like a new task. In addition we create a new deleted task in the old list having the old
		 * sync adapter field values. This means that the _ID field of the "deleted" task will not equal the _ID field f the original task. Sync adapters should
		 * handle that correctly.
		 */

		Long result = null;

		// create a deleted task for the old one, unless the task has not been synced yet (which is always true for tasks in the local account)
		if (task.valueOf(TaskFieldAdapters.SYNC_ID) != null || task.valueOf(TaskFieldAdapters.ORIGINAL_INSTANCE_SYNC_ID) != null
			|| task.valueOf(TaskFieldAdapters.SYNC_VERSION) != null)
		{
			TaskAdapter deletedTask = task.duplicate();
			deletedTask.set(TaskFieldAdapters.LIST_ID, oldList);
			deletedTask.set(TaskFieldAdapters.ORIGINAL_INSTANCE_ID, deletedOriginalId);
			deletedTask.set(TaskFieldAdapters._DELETED, true);

			// make sure we unset any values that do not exist in the tasks table
			deletedTask.unset(TaskFieldAdapters.LIST_COLOR);
			deletedTask.unset(TaskFieldAdapters.LIST_NAME);
			deletedTask.unset(TaskFieldAdapters.ACCOUNT_NAME);
			deletedTask.unset(TaskFieldAdapters.ACCOUNT_TYPE);
			deletedTask.unset(TaskFieldAdapters.LIST_OWNER);
			deletedTask.unset(TaskFieldAdapters.LIST_ACCESS_LEVEL);
			deletedTask.unset(TaskFieldAdapters.LIST_VISIBLE);

			// create the deleted task
			deletedTask.commit(db);

			result = deletedTask.id();
		}

		// clear all sync fields to convert the existing task to a new task
		task.set(TaskFieldAdapters.LIST_ID, newList);
		task.set(TaskFieldAdapters._DIRTY, true);
		task.set(TaskFieldAdapters.SYNC1, null);
		task.set(TaskFieldAdapters.SYNC2, null);
		task.set(TaskFieldAdapters.SYNC3, null);
		task.set(TaskFieldAdapters.SYNC4, null);
		task.set(TaskFieldAdapters.SYNC5, null);
		task.set(TaskFieldAdapters.SYNC6, null);
		task.set(TaskFieldAdapters.SYNC7, null);
		task.set(TaskFieldAdapters.SYNC8, null);
		task.set(TaskFieldAdapters.SYNC_ID, null);
		task.set(TaskFieldAdapters.SYNC_VERSION, null);
		task.set(TaskFieldAdapters.ORIGINAL_INSTANCE_SYNC_ID, null);
		if (commitTask)
		{
			task.commit(db);
		}

		return result;
	}

}
