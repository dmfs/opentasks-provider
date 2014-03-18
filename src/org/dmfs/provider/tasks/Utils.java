/*
 * Copyright (C) 2013 Marten Gajda <marten@dmfs.org>
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

package org.dmfs.provider.tasks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


/**
 * The Class Utils.
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * @author Marten Gajda <marten@dmfs.org>
 */
public class Utils
{
	private static String PREFERENCE_ALARM = "preference_alarm";
	private static String PREFS_NAME = "provider_preferences";


	public static void sendActionProviderChangedBroadCast(Context context)
	{
		Intent providerChangedIntent = new Intent(Intent.ACTION_PROVIDER_CHANGED, TaskContract.CONTENT_URI);
		context.sendBroadcast(providerChangedIntent);
	}


	/**
	 * Sets the alarm notification preference.
	 * 
	 * @param context
	 *            A {@link Context}.
	 * @param activated
	 *            The preference value.
	 */
	public static void setAlarmPreference(Context context, Boolean activated)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		Editor editor = settings.edit();
		editor.putBoolean(PREFERENCE_ALARM, activated);
		editor.commit();
	}


	/**
	 * Return the alarm notification preference.
	 * 
	 * @param context
	 *            A {@link Context}.
	 * @return The previously set preference or <code>true</code>.
	 */
	public static boolean getAlarmPreference(Context context)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		return settings.getBoolean(PREFERENCE_ALARM, true);
	}
}
