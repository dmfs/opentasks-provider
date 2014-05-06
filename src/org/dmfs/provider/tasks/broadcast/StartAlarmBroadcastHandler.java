package org.dmfs.provider.tasks.broadcast;

import java.lang.ref.SoftReference;

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
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * 
 */
public class StartAlarmBroadcastHandler extends BroadcastReceiver
{

	private static int REQUEST_CODE_START_ALARM = 1338;
	public static String EXTRA_TASK_ID = "task_id";
	public static String EXTRA_TASK_START_TIME = "task_start";
	public static String EXTRA_TASK_TITLE = "task_title";

	public static String BROADCAST_START_ALARM = "org.dmfs.android.tasks.TASK_START";

	private static SoftReference<Context> mContext;
	private AlarmManager mAlarmManager;
	private SQLiteDatabase mDb;


	/**
	 * Empty constructor only for the broadcast receiver.
	 */
	public StartAlarmBroadcastHandler()
	{

	}


	/**
	 * Creates the {@link StartAlarmBroadcastHandler}.
	 * 
	 * @param context
	 *            A {@link Context}.
	 */
	public StartAlarmBroadcastHandler(Context context)
	{
		mContext = new SoftReference<Context>(context);
		mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	}


	/**
	 * Registers a system alarm for the start date of the task.
	 * 
	 * @param taskId
	 *            The row id of the task to set an alarm for.
	 * @param startTime
	 *            The date in milliseconds when the task starts.
	 * @param taskTitle
	 *            The title of the task.
	 */
	@TargetApi(19)
	public void setStartAlarm(long taskId, long startTime, String taskTitle)
	{
		Context context = mContext.get();
		if (context != null)
		{
			Intent intentAlarm = new Intent(context, StartAlarmBroadcastHandler.class);
			intentAlarm.putExtra(EXTRA_TASK_ID, taskId);
			intentAlarm.putExtra(EXTRA_TASK_START_TIME, startTime);
			intentAlarm.putExtra(EXTRA_TASK_TITLE, taskTitle);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_START_ALARM, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

			// AlarmManager API changed in v19 (KitKat) and the "set" method is not called at the exact time anymore
			if (Build.VERSION.SDK_INT > 18)
			{
				mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, startTime, pendingIntent);
			}
			else
			{
				mAlarmManager.set(AlarmManager.RTC_WAKEUP, startTime, pendingIntent);
			}
		}
	}


	/**
	 * Query the database for the next upcoming task start instance and sets the alarm for it.
	 * 
	 * @param db
	 *            The {@link SQLiteDatabase}.
	 * @param time
	 *            The absolute minimum time in milliseconds when the next alarm stars.
	 * 
	 */
	public void setUpcomingStartAlarm(SQLiteDatabase db, long time)
	{
		// search for next upcoming instance which are open
		mDb = db;
		String[] projection = new String[] { Instances.TASK_ID, Instances.INSTANCE_START, Tasks.TITLE };
		String selection = time + " <= " + Instances.INSTANCE_START + " AND " + Instances.IS_CLOSED + " = 0";
		Cursor cursor = db.query(Tables.INSTANCE_VIEW, projection, selection, null, null, null, Instances.INSTANCE_START, "1");

		if (cursor != null)
		{
			try
			{
				if (cursor.moveToFirst())
				{
					setStartAlarm(cursor.getLong(0), cursor.getLong(1), cursor.getString(2));
				}
			}
			finally
			{
				cursor.close();
			}
		}
	}


	/**
	 * Is called in connection with a broadcast. Handles both bootup and alarm broadcasts.
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{

		mContext = new SoftReference<Context>(context);
		mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (intent.hasExtra(EXTRA_TASK_START_TIME))
		{

			SQLiteDatabase db;
			if (mDb == null)
			{
				SQLiteOpenHelper dBHelper = new TaskProvider().getDatabaseHelper(context);
				db = dBHelper.getReadableDatabase();
				mDb = db;
			}

			// check for all tasks which are due since the start alarm was set plus 1 second
			long currentStartTime = intent.getExtras().getLong(EXTRA_TASK_START_TIME);
			long nextStartTime = currentStartTime + 1000;
			String[] projection = new String[] { Instances.TASK_ID, Instances.INSTANCE_START, Tasks.TITLE };
			String selection = nextStartTime + " > " + Instances.INSTANCE_START + " AND " + currentStartTime + " <= " + Instances.INSTANCE_START + " AND "
				+ Instances.IS_CLOSED + " = 0";
			Cursor cursor = mDb.query(Tables.INSTANCE_VIEW, projection, selection, null, null, null, Instances.INSTANCE_START);

			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
					{
						while (!cursor.isAfterLast())
						{
							// inform the application
							sendTaskStartAlarmBroadcast(cursor.getLong(0), cursor.getLong(1), cursor.getString(2));
							cursor.moveToNext();
						}

					}
				}
				finally
				{
					cursor.close();
				}
			}
			// Set the next alarm
			setUpcomingStartAlarm(mDb, nextStartTime);
			mDb.close();
		}
		else if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
		{
			// device booted -> set upcoming alarm
			mContext = new SoftReference<Context>(context);
			mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			SQLiteOpenHelper dBHelper = new TaskProvider().getDatabaseHelper(context);
			SQLiteDatabase db = dBHelper.getReadableDatabase();
			setUpcomingStartAlarm(db, System.currentTimeMillis());
			db.close();

		}
	}


	/**
	 * Notifies the main application about the task start.
	 * 
	 * @param taskId
	 *            The row id of the task to set an alarm for.
	 * @param dueDate
	 *            The date in milliseconds when the task starts.
	 * @param taskTitle
	 *            The title of the task.
	 */
	private void sendTaskStartAlarmBroadcast(long taskId, long startDate, String taskTitle)
	{
		Context context = mContext.get();
		if (context != null)
		{
			Intent intent = new Intent(BROADCAST_START_ALARM);
			intent.setData(ContentUris.withAppendedId(TaskContract.Tasks.CONTENT_URI, taskId));
			intent.putExtra(EXTRA_TASK_ID, taskId);
			intent.putExtra(EXTRA_TASK_START_TIME, startDate);
			intent.putExtra(EXTRA_TASK_TITLE, taskTitle);

			context.sendBroadcast(intent);
		}

	}
}
