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

package org.dmfs.provider.tasks.model;

import org.dmfs.provider.tasks.TaskContract;
import org.dmfs.provider.tasks.TaskDatabaseHelper;
import org.dmfs.provider.tasks.model.adapters.FieldAdapter;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * A {@link TaskAdapter} that adapts a {@link Cursor} and a {@link ContentValues} instance. All changes are written to the {@link ContentValues} and can be
 * stored in the database with {@link #commit(SQLiteDatabase)}.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
public class CursorContentValuesTaskAdapter extends AbstractTaskAdapter
{
	private final long mId;
	private final Cursor mCursor;
	private final ContentValues mValues;


	public CursorContentValuesTaskAdapter(long id, Cursor cursor, ContentValues values)
	{
		mId = id;
		mCursor = cursor;
		mValues = values;
	}


	@Override
	public long id()
	{
		return mId;
	}


	@Override
	public <T> T valueOf(FieldAdapter<T> fieldAdapter)
	{
		return fieldAdapter.getFrom(mCursor, mValues);
	}


	@Override
	public <T> T oldValueOf(FieldAdapter<T> fieldAdapter)
	{
		return fieldAdapter.getFrom(mCursor);
	}


	@Override
	public <T> boolean isUpdated(FieldAdapter<T> fieldAdapter)
	{
		return mValues != null && fieldAdapter.isSetIn(mValues);
	}


	@Override
	public boolean isWriteable()
	{
		return true;
	}


	@Override
	public boolean hasUpdates()
	{
		return mValues != null && mValues.size() > 0;
	}


	@Override
	public <T> void set(FieldAdapter<T> fieldAdapter, T value) throws IllegalStateException
	{
		fieldAdapter.setIn(mValues, value);
	}


	@Override
	public <T> void unset(FieldAdapter<T> fieldAdapter) throws IllegalStateException
	{
		fieldAdapter.removeFrom(mValues);
	}


	@Override
	public int commit(SQLiteDatabase db)
	{
		if (mValues.size() == 0)
		{
			return 0;
		}

		return db.update(TaskDatabaseHelper.Tables.TASKS, mValues, TaskContract.TaskColumns._ID + "=" + mId, null);
	}

}