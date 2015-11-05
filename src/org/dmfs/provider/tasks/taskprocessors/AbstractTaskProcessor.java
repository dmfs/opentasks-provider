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

import org.dmfs.provider.tasks.model.TaskAdapter;

import android.database.sqlite.SQLiteDatabase;


/**
 * A default implementation of {@link TaskProcessor} that does nothing. It can be used as the basis of concrete {@link TaskProcessor}s without having to
 * override all the methods.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
public abstract class AbstractTaskProcessor implements TaskProcessor
{
	@Override
	public void beforeInsert(SQLiteDatabase db, TaskAdapter task, boolean isSyncAdapter)
	{
		// the default implementation doesn't do anything
	}


	@Override
	public void afterInsert(SQLiteDatabase db, TaskAdapter task, boolean isSyncAdapter)
	{
		// the default implementation doesn't do anything
	}


	@Override
	public void beforeUpdate(SQLiteDatabase db, TaskAdapter task, boolean isSyncAdapter)
	{
		// the default implementation doesn't do anything
	}


	@Override
	public void afterUpdate(SQLiteDatabase db, TaskAdapter task, boolean isSyncAdapter)
	{
		// the default implementation doesn't do anything
	}


	@Override
	public void beforeDelete(SQLiteDatabase db, TaskAdapter task, boolean isSyncAdapter)
	{
		// the default implementation doesn't do anything
	}


	@Override
	public void afterDelete(SQLiteDatabase db, TaskAdapter task, boolean isSyncAdapter)
	{
		// the default implementation doesn't do anything
	}

}
