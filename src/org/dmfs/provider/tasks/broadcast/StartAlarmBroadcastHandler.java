/*
 * Copyright (C) 2014 Marten Gajda <marten@dmfs.org>
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

package org.dmfs.provider.tasks.broadcast;

import org.dmfs.provider.tasks.R;
import org.dmfs.provider.tasks.TaskContract;
import org.dmfs.provider.tasks.TaskContract.Instances;
import org.dmfs.provider.tasks.TaskContract.Tasks;
import org.dmfs.provider.tasks.TaskDatabaseHelper.Tables;
import org.dmfs.provider.tasks.TaskProvider;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;


/**
 * This class is used to register and manager system alarm for task starts and notifying the main task app.
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * @author Marten Gajda <marten@dmfs.org>
 */
public class StartAlarmBroadcastHandler extends BroadcastReceiver
{
	public final static String EXTRA_TASK_ID = "task_id";
	public final static String EXTRA_TASK_START_TIME = "task_start";
	public final static String EXTRA_TASK_START_ALLDAY = "task_start_allday";
	public final static String EXTRA_TASK_TITLE = "task_title";

	public final static String BROADCAST_START_ALARM = "org.dmfs.android.tasks.TASK_START";

	private final static int REQUEST_CODE_START_ALARM = 1338;
	private final static String[] PROJECTION = new String[] { Instances.TASK_ID, Instances.INSTANCE_START, Tasks.TITLE, Tasks.IS_ALLDAY };


	/**
	 * Empty constructor only for the broadcast receiver.
	 */
	public StartAlarmBroadcastHandler()
	{

	}


	/**
	 * Registers a system alarm for the start date of the task.
	 * 
	 * @param context
	 *            A Context.
	 * @param taskId
	 *            The row id of the task to set an alarm for.
	 * @param startTime
	 *            The date in milliseconds when the task starts.
	 * @param taskTitle
	 *            The title of the task.
	 */
	@TargetApi(19)
	public static void setStartAlarm(Context context, long taskId, long startTime, String taskTitle)
	{
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intentAlarm = new Intent(context, StartAlarmBroadcastHandler.class);
		intentAlarm.putExtra(EXTRA_TASK_ID, taskId);
		intentAlarm.putExtra(EXTRA_TASK_START_TIME, startTime);
		intentAlarm.putExtra(EXTRA_TASK_TITLE, taskTitle);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_START_ALARM, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

		// AlarmManager API changed in v19 (KitKat) and the "set" method is not called at the exact time anymore
		if (Build.VERSION.SDK_INT > 18)
		{
			am.setExact(AlarmManager.RTC_WAKEUP, startTime, pendingIntent);
		}
		else
		{
			am.set(AlarmManager.RTC_WAKEUP, startTime, pendingIntent);
		}
	}


	/**
	 * Query the database for the next upcoming task start instance and sets the alarm for it.
	 * 
	 * @param context
	 *            A Context.
	 * @param db
	 *            The {@link SQLiteDatabase}.
	 * @param time
	 *            The absolute minimum time in milliseconds when the next alarm stars.
	 * 
	 */
	public static void setUpcomingStartAlarm(Context context, SQLiteDatabase db, long time)
	{
		// search for next upcoming instance which are open
		String[] projection = new String[] { Instances.TASK_ID, Instances.INSTANCE_START, Tasks.TITLE };
		String selection = time + " <= " + Instances.INSTANCE_START + " AND " + Instances.IS_CLOSED + " = 0 AND " + Tasks._DELETED + "=0";
		Cursor cursor = db.query(Tables.INSTANCE_VIEW, projection, selection, null, null, null, Instances.INSTANCE_START, "1");

		try
		{
			if (cursor.moveToFirst())
			{
				setStartAlarm(context, cursor.getLong(0), cursor.getLong(1), cursor.getString(2));
			}
		}
		finally
		{
			cursor.close();
		}
	}


	/**
	 * Is called in connection with a broadcast. Handles both bootup and alarm broadcasts.
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{
		SQLiteOpenHelper dBHelper = TaskProvider.getDatabaseHelperStatic(context);
		SQLiteDatabase db = dBHelper.getReadableDatabase();

		try
		{
			if (intent.hasExtra(EXTRA_TASK_START_TIME))
			{
				// check for all tasks which are due since the start alarm was set plus 1 second
				long currentStartTime = intent.getExtras().getLong(EXTRA_TASK_START_TIME);
				long nextStartTime = currentStartTime + 1000;
				String selection = nextStartTime + " > " + Instances.INSTANCE_START + " AND " + currentStartTime + " <= " + Instances.INSTANCE_START + " AND "
					+ Instances.IS_CLOSED + " = 0 AND " + Tasks._DELETED + "=0";
				Cursor cursor = db.query(Tables.INSTANCE_VIEW, PROJECTION, selection, null, null, null, Instances.INSTANCE_START);

				try
				{
					while (cursor.moveToNext())
					{
						// inform the application
						sendTaskStartAlarmBroadcast(context, cursor.getLong(0), cursor.getLong(1), cursor.getInt(3) != 0, cursor.getString(2));
					}
				}
				finally
				{
					cursor.close();
				}

				// Set the next alarm
				setUpcomingStartAlarm(context, db, nextStartTime);
			}
			else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
			{
				// device booted -> set upcoming alarm
				setUpcomingStartAlarm(context, db, System.currentTimeMillis());
			}
		}
		finally
		{
			if (db != null)
			{
				db.close();
			}
		}
	}


	/**
	 * Notifies the main application about the task start.
	 * 
	 * @param context
	 *            A Context.
	 * @param taskId
	 *            The row id of the task to set an alarm for.
	 * @param dueDate
	 *            The date in milliseconds when the task starts.
	 * @param taskTitle
	 *            The title of the task.
	 */
	private static void sendTaskStartAlarmBroadcast(Context context, long taskId, long startDate, boolean isAllDay, String taskTitle)
	{
		Intent intent = new Intent(BROADCAST_START_ALARM);
		intent.setData(ContentUris.withAppendedId(TaskContract.Tasks.getContentUri(context.getString(R.string.org_dmfs_tasks_authority)), taskId));
		intent.putExtra(EXTRA_TASK_ID, taskId);
		intent.putExtra(EXTRA_TASK_START_TIME, startDate);
		intent.putExtra(EXTRA_TASK_START_ALLDAY, isAllDay);
		intent.putExtra(EXTRA_TASK_TITLE, taskTitle);

		context.sendBroadcast(intent);
	}
}
