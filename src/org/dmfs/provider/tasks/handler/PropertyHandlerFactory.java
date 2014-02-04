package org.dmfs.provider.tasks.handler;

import org.dmfs.provider.tasks.TaskContract.Property.Category;


public class PropertyHandlerFactory
{
	public static PropertyHandler create(String mimeType)
	{
		if (Category.CONTENT_ITEM_TYPE.equals(mimeType))
		{
			return new CategoryHandler();
		}
		return null;
	}
}
