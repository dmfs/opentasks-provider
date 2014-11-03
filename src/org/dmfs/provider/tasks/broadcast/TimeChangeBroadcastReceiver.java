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

import org.dmfs.provider.tasks.TaskProvider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * This {@link BroadcastReceiver} is supposed to listen to the time set broadcast in order to update the alarms.
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * 
 */
public class TimeChangeBroadcastReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		// update alarms
		long now = System.currentTimeMillis();
		SQLiteOpenHelper dBHelper = TaskProvider.getDatabaseHelperStatic(context);
		SQLiteDatabase db = dBHelper.getReadableDatabase();

		DueAlarmBroadcastHandler.setUpcomingDueAlarm(context, db, now);
		StartAlarmBroadcastHandler.setUpcomingStartAlarm(context, db, now);
	}
}
