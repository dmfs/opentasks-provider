package org.dmfs.provider.tasks.handler;

import org.dmfs.provider.tasks.TaskContract.Alarms;
import org.dmfs.provider.tasks.TaskContract.Property;
import org.dmfs.provider.tasks.TaskDatabaseHelper.Tables;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;


/**
 * This is used to handle category property values
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * 
 */
public class AlarmHandler extends PropertyHandler
{

	private static final String[] ALARM_ID_PROJECTION = { Alarms.ALARM_ID };

	private static final String ALARM_SELECTION = Alarms.ALARM_ID + " =?";


	@Override
	public ContentValues validateValues(SQLiteDatabase db, boolean isNew, ContentValues values, boolean isSyncAdapter)
	{
		// row id can not be changed or set manually
		if (values.containsKey(Property.Alarm.PROPERTY_ID))
		{
			throw new IllegalArgumentException("_ID can not be set manually");
		}

		if (!values.containsKey(Property.Alarm.MINUTES_BEFORE) || values.getAsInteger(Property.Alarm.MINUTES_BEFORE) < 0)
		{
			throw new IllegalArgumentException("alarm property requires a time offset > 0");
		}

		if (!values.containsKey(Property.Alarm.REFERENCE) || values.getAsInteger(Property.Alarm.REFERENCE) < 0)
		{
			throw new IllegalArgumentException("alarm property requires a reference date ");
		}

		if (!values.containsKey(Property.Alarm.ALARM_TYPE))
		{
			throw new IllegalArgumentException("alarm property requires an alarm type");
		}

		return values;
	}


	@Override
	public long insert(SQLiteDatabase db, ContentValues values, boolean isSyncAdapter)
	{
		values = validateValues(db, true, values, isSyncAdapter);

		// insert property row
		return db.insert(Tables.PROPERTIES, "", values);
	}


	@Override
	public int update(SQLiteDatabase db, ContentValues values, String selection, String[] selectionArgs, boolean isSyncAdapter)
	{
		super.update(db, values, selection, selectionArgs, isSyncAdapter);
		values = validateValues(db, true, values, isSyncAdapter);

		// TODO: update alarms

		return db.update(Tables.PROPERTIES, values, selection, selectionArgs);
	}
}
