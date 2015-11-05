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
 * TaskProcessors are called before and after any operation on a task. They can be used to perform additional operations for each tasks.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
public interface TaskProcessor
{
	/**
	 * Called before a task is inserted.
	 * 
	 * @param db
	 *            A writable task database.
	 * @param taskAdapter
	 *            The {@link TaskAdapter} that's about to be inserted. You can modify the task at this stage. {@link TaskAdapter#id()} will return an invalid
	 *            value.
	 * @param isSyncAdapter
	 */
	public void beforeInsert(SQLiteDatabase db, TaskAdapter taskAdapter, boolean isSyncAdapter);


	/**
	 * Called after a task has been inserted.
	 * 
	 * @param db
	 *            A writable task database.
	 * @param taskAdapter
	 *            The {@link TaskAdapter} that's has been inserted. Modifying the task has no effect.
	 * @param isSyncAdapter
	 */
	public void afterInsert(SQLiteDatabase db, TaskAdapter taskAdapter, boolean isSyncAdapter);


	/**
	 * Called before a task is updated.
	 * 
	 * @param db
	 *            A writable task database.
	 * @param taskAdapter
	 *            The {@link TaskAdapter} that's about to be updated. You can modify the task at this stage.
	 * @param isSyncAdapter
	 */
	public void beforeUpdate(SQLiteDatabase db, TaskAdapter taskAdapter, boolean isSyncAdapter);


	/**
	 * Called after a task has been updated.
	 * 
	 * @param db
	 *            A writable task database.
	 * @param taskAdapter
	 *            The {@link TaskAdapter} that's has been updated. Modifying the task has no effect.
	 * @param isSyncAdapter
	 */
	public void afterUpdate(SQLiteDatabase db, TaskAdapter taskAdapter, boolean isSyncAdapter);


	/**
	 * Called before a task is deleted.
	 * <p>
	 * Note that this can be called twice for each task. Once when the task is marked deleted by the UI and once when it's actually removed by the sync adapter.
	 * Both cases can be distinguished by the isSyncAdapter parameter. If a task is removed because it was deleted on the server, this will be called only once
	 * with <code>isSyncAdapter == true</code>.
	 * </p>
	 * <p>
	 * Also note that no hook is called when a task is removed automatically by a database trigger (i.e. when the entire task list is removed).
	 * </p>
	 * 
	 * @param db
	 *            A writable task database.
	 * @param taskAdapter
	 *            The {@link TaskAdapter} that's about to be deleted. Modifying the task has no effect.
	 * @param isSyncAdapter
	 */
	public void beforeDelete(SQLiteDatabase db, TaskAdapter taskAdapter, boolean isSyncAdapter);


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
	 *            A writable task database.
	 * @param taskAdapter
	 *            The {@link TaskAdapter} that was deleted. The value of {@link TaskAdapter#id()} contains the id of the deleted task. Modifying the task has no
	 *            effect.
	 * @param isSyncAdapter
	 */
	public void afterDelete(SQLiteDatabase db, TaskAdapter taskAdapter, boolean isSyncAdapter);
}
