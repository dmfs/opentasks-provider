package org.dmfs.provider.tasks;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;


public class UriFactory
{
	public final String authority;

	private final Map<String, Uri> mUriMap = new HashMap<String, Uri>(16);


	UriFactory(String authority)
	{
		this.authority = authority;
		mUriMap.put((String) null, Uri.parse("content://" + authority));
	}


	void addUri(String path)
	{
		mUriMap.put(path, Uri.parse("content://" + authority + "/" + path));
	}


	public Uri getUri()
	{
		return mUriMap.get(null);
	}


	public Uri getUri(String path)
	{
		return mUriMap.get(path);
	}
}
