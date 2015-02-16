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

import java.util.TimeZone;

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
import android.text.format.Time;


/**
 * This class is used to register and manager system alarm for tasks and notifying the main task app.
 * 
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * @author Marten Gajda <marten@dmfs.org>
 */
public class DueAlarmBroadcastHandler extends BroadcastReceiver
{

	private final static String ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON";

	public final static String EXTRA_TASK_ID = "org.dmfs.provider.tasks.extra.task_id";
	public final static String EXTRA_TASK_DUE_TIME = "org.dmfs.provider.tasks.extra.task_due";
	public final static String EXTRA_TASK_DUE_ALLDAY = "org.dmfs.provider.tasks.extra.task_due_allday";
	public final static String EXTRA_TASK_TIMEZONE = "org.dmfs.provider.tasks.extra.task_timezone";
	public final static String EXTRA_TASK_TITLE = "org.dmfs.provider.tasks.extra.task_title";

	/** The boolean notification extra to deliver notifications silently. eg. for follow up notifications **/
	public final static String EXTRA_SILENT_NOTIFICATION = "org.dmfs.provider.tasks.extra.silent_notification";

	public final static String BROADCAST_DUE_ALARM = "org.dmfs.android.tasks.TASK_DUE";

	private final static int REQUEST_CODE_DUE_ALARM = 1337;
	private final static String[] PROJECTION = new String[] { Instances.TASK_ID, Instances.INSTANCE_DUE, Tasks.TITLE, Tasks.IS_ALLDAY, Tasks.TZ };


	/**
	 * Empty constructor only for the broadcast receiver.
	 */
	public DueAlarmBroadcastHandler()
	{

	}


	/**
	 * Registers a system alarm for the due date of the task.
	 * 
	 * @param context
	 *            A Context.
	 * @param taskId
	 *            The row id of the task to set an alarm for.
	 * @param dueTime
	 *            The date in milliseconds when the task is due.
	 * @param taskTitle
	 *            The title of the task.
	 */
	@TargetApi(19)
	public static void setDueAlarm(Context context, long taskId, long dueTime, String taskTitle, String timezone)
	{
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intentAlarm = new Intent(context, DueAlarmBroadcastHandler.class);
		intentAlarm.putExtra(EXTRA_TASK_ID, taskId);
		intentAlarm.putExtra(EXTRA_TASK_DUE_TIME, dueTime);
		intentAlarm.putExtra(EXTRA_TASK_TITLE, taskTitle);
		intentAlarm.putExtra(EXTRA_TASK_TIMEZONE, timezone);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_DUE_ALARM, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

		// cancel old
		am.cancel(PendingIntent.getBroadcast(context, REQUEST_CODE_DUE_ALARM, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));

		// AlarmManager API changed in v19 (KitKat) and the "set" method is not called at the exact time anymore
		if (Build.VERSION.SDK_INT > 18)
		{
			am.setExact(AlarmManager.RTC_WAKEUP, dueTime, pendingIntent);
		}
		else
		{
			am.set(AlarmManager.RTC_WAKEUP, dueTime, pendingIntent);
		}
	}


	/**
	 * Query the database for the next upcoming due task instance and sets the alarm for it.
	 * 
	 * @param context
	 *            A Context.
	 * @param db
	 *            The {@link SQLiteDatabase}.
	 * @param time
	 *            The absolute minimum time in milliseconds when the next alarm can be due.
	 */
	public static void setUpcomingDueAlarm(Context context, SQLiteDatabase db, long time)
	{
		// calculate allday due time
		Time dueTime = new Time(TimeZone.getDefault().getID());
		dueTime.set(time);

		Time utcDueTime = new Time("UTC");
		utcDueTime.set(dueTime.second, dueTime.minute, dueTime.hour, dueTime.monthDay, dueTime.month, dueTime.year);
		long utcDueMillis = utcDueTime.toMillis(true);

		Long nextTaskId = null;
		Long nextTaskDueMillis = Long.MAX_VALUE;
		String nextTaskTitle = null;
		String nextTaskTimezone = null;

		// search for next upcoming instance which are open
		String[] projection = new String[] { Instances.TASK_ID, Instances.INSTANCE_DUE, Instances.TITLE, Instances.TZ };
		String selection = time + " <= " + Instances.INSTANCE_DUE + " AND " + Instances.IS_CLOSED + " = 0 AND " + Tasks._DELETED + "= 0 AND "
			+ Instances.IS_ALLDAY + " = 0";
		Cursor cursor = db.query(Tables.INSTANCE_VIEW, projection, selection, null, null, null, Instances.INSTANCE_DUE, "1");
		try
		{
			if (cursor.moveToFirst())
			{
				nextTaskId = cursor.getLong(0);
				nextTaskDueMillis = cursor.getLong(1);
				nextTaskTitle = cursor.getString(2);
				nextTaskTimezone = cursor.getString(3);
			}
		}
		finally
		{
			cursor.close();
		}

		// search for next upcoming instance which are open and all day
		selection = utcDueMillis + " <= " + Instances.INSTANCE_DUE + " AND " + Instances.IS_CLOSED + " = 0 AND " + Tasks._DELETED + "= 0 AND "
			+ Instances.IS_ALLDAY + " = 1";
		cursor = db.query(Tables.INSTANCE_VIEW, projection, selection, null, null, null, Instances.INSTANCE_DUE, "1");
		try
		{
			if (cursor.moveToFirst())
			{
				Long allDayTaskId = cursor.getLong(0);
				Long allDayTaskDueMillis = cursor.getLong(1);
				String allDayTaskTitle = cursor.getString(2);

				// compare the two tasks
				Time utcTaskDueTime = new Time("UTC");
				utcTaskDueTime.set(allDayTaskDueMillis);

				Time taskDueTime = new Time(TimeZone.getDefault().getID());
				taskDueTime.set(utcTaskDueTime.second, utcTaskDueTime.minute, utcTaskDueTime.hour, utcTaskDueTime.monthDay, utcTaskDueTime.month,
					utcTaskDueTime.year);
				allDayTaskDueMillis = taskDueTime.toMillis(true);

				if (nextTaskId == null || nextTaskDueMillis > allDayTaskDueMillis)
				{
					setDueAlarm(context, allDayTaskId, allDayTaskDueMillis, allDayTaskTitle, TimeZone.getDefault().getID());
				}
				else
				{
					setDueAlarm(context, nextTaskId, nextTaskDueMillis, nextTaskTitle, nextTaskTimezone);
				}
			}
			else if (nextTaskId != null)
			{

				setDueAlarm(context, nextTaskId, nextTaskDueMillis, nextTaskTitle, nextTaskTimezone);
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
			if (intent.hasExtra(EXTRA_TASK_DUE_TIME))
			{
				boolean silent = intent.getBooleanExtra(EXTRA_SILENT_NOTIFICATION, false);

				long currentDueTime = intent.getLongExtra(EXTRA_TASK_DUE_TIME, System.currentTimeMillis());
				long nextDueTime = currentDueTime + 1000;

				// calculate UTC offset
				Time dueTime = new Time(TimeZone.getDefault().getID());
				dueTime.setToNow();
				long defaultMillis = dueTime.toMillis(true);

				Time utcDueTime = new Time("UTC");
				utcDueTime.set(dueTime.second, dueTime.minute, dueTime.hour, dueTime.monthDay, dueTime.month, dueTime.year);
				long offsetMillis = utcDueTime.toMillis(true) - defaultMillis;

				long currentUTCDueTime = currentDueTime + offsetMillis;
				long nextUTCDueTime = nextDueTime + offsetMillis;

				// check for all tasks which are due since the start alarm was set plus 1 second
				String selection = "(( " + nextDueTime + " > " + Instances.INSTANCE_DUE + " AND " + currentDueTime + " <= " + Instances.INSTANCE_DUE + " AND "
					+ Instances.IS_ALLDAY + " = 0 ) or ( " + nextUTCDueTime + " > " + Instances.INSTANCE_DUE + " AND " + currentUTCDueTime + " <= "
					+ Instances.INSTANCE_DUE + " AND " + Instances.IS_ALLDAY + " = 1 )) AND " + Instances.IS_CLOSED + " = 0 AND " + Tasks._DELETED + "=0";

				Cursor cursor = db.query(Tables.INSTANCE_VIEW, PROJECTION, selection, null, null, null, Instances.INSTANCE_DUE);

				try
				{
					while (cursor.moveToNext())
					{
						// inform the application
						sendTaskDueAlarmBroadcast(context, cursor.getLong(0), cursor.getLong(1), cursor.getInt(3) != 0, cursor.getString(2),
							cursor.getString(4), silent);
					}
				}
				finally
				{
					cursor.close();
				}

				// Set the next alarm
				setUpcomingDueAlarm(context, db, nextDueTime);
			}
			else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || ACTION_QUICKBOOT_POWERON.equals(intent.getAction()))
			{
				// device booted -> set upcoming alarm
				setUpcomingDueAlarm(context, db, System.currentTimeMillis());

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
	 * Notifies the main application about the due task.
	 * 
	 * @param context
	 *            A Context.
	 * @param taskId
	 *            The row id of the task to set an alarm for.
	 * @param dueDate
	 *            The date in milliseconds when the task is due.
	 * @param taskTitle
	 *            The title of the task.
	 * @param silent
	 *            Indicates to deliver notifications silently.
	 */
	private static void sendTaskDueAlarmBroadcast(Context context, long taskId, long dueDate, boolean isAllDay, String taskTitle, String timezone,
		boolean silent)
	{
		Intent intent = new Intent(BROADCAST_DUE_ALARM);
		intent.setData(ContentUris.withAppendedId(TaskContract.Tasks.getContentUri(context.getString(R.string.org_dmfs_tasks_authority)), taskId));
		intent.putExtra(EXTRA_TASK_ID, taskId);
		intent.putExtra(EXTRA_TASK_DUE_TIME, dueDate);
		intent.putExtra(EXTRA_TASK_DUE_ALLDAY, isAllDay);
		intent.putExtra(EXTRA_TASK_TITLE, taskTitle);
		intent.putExtra(EXTRA_TASK_TIMEZONE, timezone);
		intent.putExtra(EXTRA_SILENT_NOTIFICATION, silent);
		context.sendBroadcast(intent);
	}
}
