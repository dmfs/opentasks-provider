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

import org.dmfs.provider.tasks.model.adapters.FieldAdapter;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * A {@link TaskAdapter} that adapts a {@link Cursor}. This adapter doesn't support setting values. Any attempt doing so will result in an
 * {@link IllegalStateException}.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
public class CursorTaskAdapter extends AbstractTaskAdapter
{
	private final Cursor mCursor;


	public CursorTaskAdapter(Cursor cursor)
	{
		mCursor = cursor;
	}


	@Override
	public long id()
	{
		return TaskFieldAdapters._ID.getFrom(mCursor);
	}


	@Override
	public <T> T valueOf(FieldAdapter<T> fieldAdapter)
	{
		return fieldAdapter.getFrom(mCursor);
	}


	@Override
	public <T> T oldValueOf(FieldAdapter<T> fieldAdapter)
	{
		return fieldAdapter.getFrom(mCursor);
	}


	@Override
	public <T> boolean isUpdated(FieldAdapter<T> fieldAdapter)
	{
		return false;
	}


	@Override
	public boolean isWriteable()
	{
		return false;
	}


	@Override
	public boolean hasUpdates()
	{
		return false;
	}


	@Override
	public <T> void set(FieldAdapter<T> fieldAdapter, T value)
	{
		throw new IllegalStateException("This task adapter is not writeable");
	}


	@Override
	public <T> void unset(FieldAdapter<T> fieldAdapter)
	{
		throw new IllegalStateException("This task adapter is not writeable");
	}


	@Override
	public int commit(SQLiteDatabase db)
	{
		throw new IllegalStateException("This task adapter is not writeable");
	}

}
