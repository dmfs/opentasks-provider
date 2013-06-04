# task-provider

__A Task provider for Android__

This is a task provider for Android. It supports multiple accounts and multiple lists per account.

## Requirements

Android SDK Level 8.

## Usage

At present this is a library project to be bundled with a task app. To use it include the following into your AndroidManifest

		<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="your.package" android:versionCode="1" android:versionName="1.0" >
			...

			<permission
				android:name="your.package.permission.READ_TASKS"
				android:description="@string/permission_description_read_tasks"
				android:label="@string/permission_read_tasks"
				android:permissionGroup="android.permission-group.PERSONAL_INFO"
				android:protectionLevel="dangerous" >
			</permission>
			<permission
				android:name="your.package.permission.WRITE_TASKS"
				android:description="@string/permission_description_write_tasks"
				android:label="@string/permission_write_tasks"
				android:permissionGroup="android.permission-group.PERSONAL_INFO"
				android:protectionLevel="dangerous" >
			</permission>

			<uses-permission android:name="your.package.permission.READ_TASKS" />
			<uses-permission android:name="your.package.permission.WRITE_TASKS" />
			<uses-permission android:name="android.permission.GET_ACCOUNTS" />

			<application
				android:icon="@drawable/ic_launcher"
				android:label="@string/app_name"
				android:theme="@style/AppTheme" >

				<provider
					android:name="org.dmfs.provider.tasks.TaskProvider"
					android:authorities="your.package.tasks"
					android:label="@string/provider_label"
					android:multiprocess="false"
					android:readPermission="your.package.permission.READ_TASKS"
					android:writePermission="your.packagel.permission.WRITE_TASKS" />

			</application>

		</manifest>

## TODO:

* add support for recurrence
* add support for transactions
* add support for extended attributes like alarms, attendees and categories

## License

Copyright (c) Marten Gajda 2012, licensed under Apache2.

