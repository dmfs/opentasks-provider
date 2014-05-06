package org.dmfs.provider.tasks.broadcast;

import org.dmfs.provider.tasks.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * This {@link BroadcastReceiver} is supposed to listen to the accunt change broadcast in order to clean the database on account deletion.
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * 
 */
public class AccountsChangeBroadcastReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		// check for deleted accounts and clean associated lists
		Utils.cleanUpLists(context);
	}
}
