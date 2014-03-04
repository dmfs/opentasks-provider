package org.dmfs.provider.tasks.handler;

import org.dmfs.provider.tasks.TaskContract.Property.Alarm;
import org.dmfs.provider.tasks.TaskContract.Property.Category;


/**
 * A factory that creates the matching {@link PropertyHandler} the given mime type
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 * 
 */
public class PropertyHandlerFactory
{
	public static PropertyHandler create(String mimeType)
	{
		if (Category.CONTENT_ITEM_TYPE.equals(mimeType))
		{
			return new CategoryHandler();
		}
		if (Alarm.CONTENT_ITEM_TYPE.equals(mimeType))
		{
			return new AlarmHandler();
		}
		return null;
	}
}
