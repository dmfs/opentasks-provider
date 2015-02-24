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

package org.dmfs.provider.tasks.handler;

import org.dmfs.provider.tasks.TaskContract.Property.Relation;
import org.dmfs.provider.tasks.TaskContract.Tasks;
import org.dmfs.provider.tasks.TaskDatabaseHelper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * Handles any inserts, updates and deletes on the relations table.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
public class RelationHandler extends PropertyHandler
{

	@Override
	public ContentValues validateValues(SQLiteDatabase db, long taskId, long propertyId, boolean isNew, ContentValues values, boolean isSyncAdapter)
	{
		if (values.containsKey(Relation.RELATED_CONTENT_URI))
		{
			throw new IllegalArgumentException("setting of RELATED_CONTENT_URI not allowed");
		}

		Long id = values.getAsLong(Relation.RELATED_ID);
		String uid = values.getAsString(Relation.RELATED_UID);
		String uri = values.getAsString(Relation.RELATED_URI);

		if (id == null && uri == null && uid != null)
		{
			values.putNull(Relation.RELATED_ID);
			values.putNull(Relation.RELATED_URI);
		}
		else if (id == null && uid == null && uri != null)
		{
			values.putNull(Relation.RELATED_ID);
			values.putNull(Relation.RELATED_UID);
		}
		else if (id != null && uid == null && uri == null)
		{
			values.putNull(Relation.RELATED_URI);
			values.putNull(Relation.RELATED_UID);
		}
		else
		{
			throw new IllegalArgumentException("exactly one of RELATED_ID, RELATED_UID and RELATED_URI must be non-null");
		}

		return values;
	}


	@Override
	public long insert(SQLiteDatabase db, long taskId, ContentValues values, boolean isSyncAdapter)
	{
		validateValues(db, taskId, -1, true, values, isSyncAdapter);
		resolveFields(db, values);
		return super.insert(db, taskId, values, isSyncAdapter);
	}


	@Override
	public int update(SQLiteDatabase db, long taskId, long propertyId, ContentValues values, boolean isSyncAdapter)
	{
		validateValues(db, taskId, propertyId, false, values, isSyncAdapter);
		resolveFields(db, values);
		return super.update(db, taskId, propertyId, values, isSyncAdapter);
	}


	/**
	 * Resolve <code>_id</code> or <code>_uid</code>, depending of which value is given. We can't resolve anything if only {@link Relation#RELATED_URI} is
	 * given. The given values are update in-place.
	 * <p>
	 * TODO: store links into the calendar provider if we find an event that matches the UID.
	 * </p>
	 * 
	 * @param db
	 *            The task database.
	 * @param values
	 *            The {@link ContentValues}.
	 */
	private void resolveFields(SQLiteDatabase db, ContentValues values)
	{
		Long id = values.getAsLong(Relation.RELATED_ID);
		String uid = values.getAsString(Relation.RELATED_UID);

		if (id != null)
		{
			values.put(Relation.RELATED_UID, resolveTaskStringField(db, Tasks._ID, id.toString(), Tasks._UID));
		}
		else if (uid != null)
		{
			values.put(Relation.RELATED_ID, resolveTaskIntegerField(db, Tasks._UID, uid, Tasks._ID));
		}
	}


	private Integer resolveTaskIntegerField(SQLiteDatabase db, String selectionField, String selectionValue, String resultField)
	{
		String result = resolveTaskStringField(db, selectionField, selectionValue, resultField);
		if (result != null)
		{
			return Integer.parseInt(result);
		}
		return null;
	}


	private String resolveTaskStringField(SQLiteDatabase db, String selectionField, String selectionValue, String resultField)
	{
		Cursor c = db.query(TaskDatabaseHelper.Tables.TASKS, new String[] { resultField }, selectionField + "=?", new String[] { selectionValue }, null, null,
			null);
		if (c != null)
		{
			try
			{
				if (c.moveToNext())
				{
					return c.getString(0);
				}
			}
			finally
			{
				c.close();
			}
		}
		return null;
	}
}
