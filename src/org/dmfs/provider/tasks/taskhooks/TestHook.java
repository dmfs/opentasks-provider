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
import android.util.Log;


/**
 * A simple debugging hook. It just logs ever operation.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
public class TestHook extends AbstractTaskHook
{

	@Override
	public void beforeInsert(SQLiteDatabase db, ContentValues values, boolean isSyncAdapter)
	{
		Log.i("TestHook", "before insert hook called");
	}


	@Override
	public void afterInsert(SQLiteDatabase db, long taskId, ContentValues values, boolean isSyncAdapter)
	{
		Log.i("TestHook", "after insert hook called for " + taskId);
	}


	@Override
	public void beforeUpdate(SQLiteDatabase db, long taskId, Cursor cursor, ContentValues values, boolean isSyncAdapter)
	{
		Log.i("TestHook", "before update hook called for " + taskId);
	}


	@Override
	public void afterUpdate(SQLiteDatabase db, long taskId, Cursor cursor, ContentValues values, boolean isSyncAdapter)
	{
		Log.i("TestHook", "after update hook called for " + taskId);
	}


	@Override
	public void beforeDelete(SQLiteDatabase db, long taskId, Cursor cursor, boolean isSyncAdapter)
	{
		Log.i("TestHook", "before delete hook called for " + taskId);
	}


	@Override
	public void afterDelete(SQLiteDatabase db, long taskId, boolean isSyncAdapter)
	{
		Log.i("TestHook", "after delete hook called for " + taskId);
	}
}
