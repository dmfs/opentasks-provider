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

package org.dmfs.provider.tasks.taskhooks;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * Hooks are called before and after any operation on a task. They can be used to perform additional operations for each tasks.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
public abstract class AbstractTaskHook
{
	/**
	 * Called before a task is inserted.
	 * 
	 * @param db
	 * @param taskId
	 * @param values
	 * @param isSyncAdapter
	 */
	public void beforeInsert(SQLiteDatabase db, ContentValues values, boolean isSyncAdapter)
	{
		// the default implementation doesn't do anything
	}


	/**
	 * Called after a task has been inserted.
	 * 
	 * @param db
	 * @param taskId
	 * @param values
	 * @param isSyncAdapter
	 */
	public void afterInsert(SQLiteDatabase db, long taskId, ContentValues values, boolean isSyncAdapter)
	{
		// the default implementation doesn't do anything
	}


	/**
	 * Called before a task is updated.
	 * 
	 * @param db
	 * @param taskId
	 * @param cursor
	 * @param values
	 * @param isSyncAdapter
	 */
	public void beforeUpdate(SQLiteDatabase db, long taskId, Cursor cursor, ContentValues values, boolean isSyncAdapter)
	{
		// the default implementation doesn't do anything
	}


	/**
	 * Called after a task has been updated.
	 * 
	 * @param db
	 * @param taskId
	 * @param cursor
	 * @param values
	 * @param isSyncAdapter
	 */
	public void afterUpdate(SQLiteDatabase db, long taskId, Cursor cursor, ContentValues values, boolean isSyncAdapter)
	{
		// the default implementation doesn't do anything
	}


	/**
	 * Called before a task is deleted.
	 * <p>
	 * Note that can be called twice for each task. Once when the task is marked deleted by the UI and once when it's actually removed by the sync adapter. Both
	 * cases can be distinguished by the isSyncAdapter parameter. If a task is removed because it was deleted on the server, this will be called only once with
	 * <code>isSyncAdapter == true</code>.
	 * </p>
	 * <p>
	 * Also note that no hook is called when a task is removed automatically by a database trigger (i.e. when the entire task list is removed).
	 * </p>
	 * 
	 * @param db
	 * @param taskId
	 * @param cursor
	 * @param isSyncAdapter
	 */
	public void beforeDelete(SQLiteDatabase db, long taskId, Cursor cursor, boolean isSyncAdapter)
	{
		// the default implementation doesn't do anything
	}


	/**
	 * Called after a task is deleted.
	 * <p>
	 * Note that can be called twice for each task. Once when the task is marked deleted by the UI and once when it's actually removed by the sync adapter. Both
	 * cases can be distinguished by the isSyncAdapter parameter. If a task is removed because it was deleted on the server, this will be called only once with
	 * <code>isSyncAdapter == true</code>.
	 * </p>
	 * <p>
	 * Also note that no hook is called when a task is removed automatically by a database trigger (i.e. when the entire task list is removed).
	 * </p>
	 * 
	 * @param db
	 * @param taskId
	 *            The old row id of the task. The id no longer valid when this method is called, so don't try to access it.
	 * @param isSyncAdapter
	 */
	public void afterDelete(SQLiteDatabase db, long taskId, boolean isSyncAdapter)
	{
		// the default implementation doesn't do anything
	}

}
