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
