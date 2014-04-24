package org.dmfs.provider.tasks.handler;

import java.lang.ref.SoftReference;

import org.dmfs.provider.tasks.TaskContract.Instances;
import org.dmfs.provider.tasks.TaskContract.Tasks;
import org.dmfs.provider.tasks.TaskDatabaseHelper.Tables;
import org.dmfs.provider.tasks.TaskProvider;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;


/**
 * This class is used to register and manager system alarm for tasks and notifying the main task app.
 * 
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * 
 */
public class AlarmNotificationHandler extends BroadcastReceiver
{

	private static int REQUEST_CODE_DUE_ALARM = 1337;
	public static String EXTRA_TASK_ID = "task_id";
	public static String EXTRA_TASK_DUE_TIME = "task_due";
	public static String EXTRA_TASK_TITLE = "task_title";

	private static SoftReference<Context> mContext;
	private AlarmManager mAlarmManager;
	private SQLiteDatabase mDb;


	/**
	 * Empty constructor only for the broadcast receiver.
	 */
	public AlarmNotificationHandler()
	{

	}


	/**
	 * Creates the {@link AlarmNotificationHandler}.
	 * 
	 * @param context
	 *            A {@link Context}.
	 */
	public AlarmNotificationHandler(Context context)
	{
		mContext = new SoftReference<Context>(context);
		mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	}


	/**
	 * Registers a system alarm for the due date of the task.
	 * 
	 * @param taskId
	 *            The row id of the task to set an alarm for.
	 * @param dueTime
	 *            The date in milliseconds when the task is due.
	 * @param taskTitle
	 *            The title of the task.
	 */
	@TargetApi(19)
	public void setDueAlarm(long taskId, long dueTime, String taskTitle)
	{
		Context context = mContext.get();
		if (context != null)
		{
			Intent intentAlarm = new Intent(context, AlarmNotificationHandler.class);
			intentAlarm.putExtra(EXTRA_TASK_ID, taskId);
			intentAlarm.putExtra(EXTRA_TASK_DUE_TIME, dueTime);
			intentAlarm.putExtra(EXTRA_TASK_TITLE, taskTitle);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_DUE_ALARM, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

			// AlarmManager API changed in v19 (KitKat) and the "set" method is not called at the exact time anymore
			if (Build.VERSION.SDK_INT > 18)
			{
				mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, dueTime, pendingIntent);
			}
			else
			{
				mAlarmManager.set(AlarmManager.RTC_WAKEUP, dueTime, pendingIntent);
			}
		}
	}


	/**
	 * Query the database for the next upcoming due task instance and sets the alarm for it.
	 * 
	 * @param db
	 *            The {@link SQLiteDatabase}.
	 * @param time
	 *            The absolute minimum time in milliseconds when the next alarm can be due.
	 * 
	 */
	public void setUpcomingDueAlarm(SQLiteDatabase db, long time)
	{
		// search for next upcoming instance which are open
		mDb = db;
		String[] projection = new String[] { Instances.TASK_ID, Instances.INSTANCE_DUE, Tasks.TITLE };
		String selection = time + " <= " + Instances.INSTANCE_DUE + " AND " + Instances.IS_CLOSED + " = 0";
		Cursor cursor = db.query(Tables.INSTANCE_VIEW, projection, selection, null, null, null, Instances.INSTANCE_DUE, "1");

		if (cursor != null)
		{
			try
			{
				if (cursor.moveToFirst())
				{
					setDueAlarm(cursor.getLong(0), cursor.getLong(1), cursor.getString(2));
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
		if (intent.hasExtra(EXTRA_TASK_ID))
		{

			SQLiteDatabase db;
			if (mDb == null)
			{
				SQLiteOpenHelper dBHelper = new TaskProvider().getDatabaseHelper(context);
				db = dBHelper.getReadableDatabase();
				mDb = db;
			}

			// check for all tasks which are due since the due alarm was set plus 1 second
			long currentDueTime = intent.getExtras().getLong(EXTRA_TASK_DUE_TIME);
			long nextDueTime = currentDueTime + 1000;
			String[] projection = new String[] { Instances.TASK_ID, Instances.INSTANCE_DUE, Tasks.TITLE };
			String selection = nextDueTime + " > " + Instances.INSTANCE_DUE + " AND " + currentDueTime + " <= " + Instances.INSTANCE_DUE + " AND "
				+ Instances.STATUS + " IS NOT " + Instances.STATUS_COMPLETED + " AND " + Instances.STATUS + " IS NOT " + Instances.STATUS_CANCELLED;
			Cursor cursor = mDb.query(Tables.INSTANCE_VIEW, projection, selection, null, null, null, Instances.INSTANCE_DUE);

			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
					{
						while (!cursor.isAfterLast())
						{
							// inform the application
							sendTaskDueAlarmBroadcast(cursor.getLong(0), cursor.getLong(1), cursor.getString(2));
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
			setUpcomingDueAlarm(mDb, nextDueTime);
			mDb.close();
		}
		else if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
		{
			// device booted -> set upcoming alarm
			mContext = new SoftReference<Context>(context);
			mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			SQLiteOpenHelper dBHelper = new TaskProvider().getDatabaseHelper(context);
			SQLiteDatabase db = dBHelper.getReadableDatabase();
			setUpcomingDueAlarm(db, System.currentTimeMillis());
			db.close();

		}
	}


	/**
	 * Notifies the main application about the due task.
	 * 
	 * @param taskId
	 *            The row id of the task to set an alarm for.
	 * @param dueDate
	 *            The date in milliseconds when the task is due.
	 * @param taskTitle
	 *            The title of the task.
	 */
	private void sendTaskDueAlarmBroadcast(long taskId, long dueDate, String taskTitle)
	{
		Context context = mContext.get();
		if (context != null)
		{
			Intent intent = new Intent();
			intent.putExtra(EXTRA_TASK_ID, taskId);
			intent.putExtra(EXTRA_TASK_DUE_TIME, dueDate);
			intent.putExtra(EXTRA_TASK_TITLE, taskTitle);
			intent.setAction("org.dmfs.android.tasks.taskdue");
			context.sendBroadcast(intent);
		}

	}
}
