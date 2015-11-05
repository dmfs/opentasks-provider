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
import org.dmfs.provider.tasks.TaskContract.Tasks;
import org.dmfs.provider.tasks.TaskDatabaseHelper;
import org.dmfs.provider.tasks.model.TaskAdapter;
import org.dmfs.provider.tasks.model.TaskFieldAdapters;

import android.database.sqlite.SQLiteDatabase;


/**
 * A {@link TaskProcessor} to remove deleted local tasks.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
public class LocalTaskProcessor extends AbstractTaskProcessor
{

	@Override
	public void beforeDelete(SQLiteDatabase db, TaskAdapter task, boolean isSyncAdapter)
	{
		if (isSyncAdapter)
		{
			// this can't be the local account
			return;
		}

		String accountType = task.valueOf(TaskFieldAdapters.ACCOUNT_TYPE);
		if (TaskContract.LOCAL_ACCOUNT_TYPE.equals(accountType))
		{
			// this is a local task that was will be marked as deleted. We'll delete it right away, to avoid stale tasks in our database.
			// note that we don't do that in afterDelete, because we won't have a cursor to check the account type then
			db.delete(TaskDatabaseHelper.Tables.TASKS, Tasks._ID + "=" + task.id(), null);
		}
	}
}
