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


/**
 * The Class Utils.
 */
public class Utils
{
	public static void sendActionProviderChangedBroadCast(Context context)
	{
		Intent providerChangedIntent = new Intent(Intent.ACTION_PROVIDER_CHANGED, TaskContract.CONTENT_URI);
		context.sendBroadcast(providerChangedIntent);
	}
}
