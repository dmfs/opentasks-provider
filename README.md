# opentasks-provider

__An open source task provider for Android__

This is a task provider for Android. It supports multiple accounts and multiple lists per account. It aims to fully support RFC 5545 VTODO as well as other task models.

## Requirements

* Android SDK Level 8 or higher.
* [lib-recur](https://github.com/dmfs/lib-recur)
* [rfc5545-datetime](https://github.com/dmfs/rfc5545-datetime)

## Usage

This is a library project to be bundled with a task app. To use it include the following into your AndroidManifest

```xml
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
    <uses-permission android:name="com.android.alarm.permission.SET_ALARM" />

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

        <!-- This receives alarms that are fired when a task starts. It fires the actual due notification broadcast. -->
        <receiver android:name="org.dmfs.provider.tasks.broadcast.StartAlarmBroadcastHandler" >
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
            <intent-filter>
                <action android:name="org.dmfs.android.tasks.TASK_START" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.QUICKBOOT_POWERON" />
            </intent-filter>
        </receiver>

        <!-- This receives alarms that are fired when a task is due. It fires the actual due notification broadcast. -->
        <receiver android:name="org.dmfs.provider.tasks.broadcast.DueAlarmBroadcastHandler" >
            <intent-filter>
                <action android:name="org.dmfs.android.tasks.TASK_DUE" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.QUICKBOOT_POWERON" />
            </intent-filter>
        </receiver>

        <!-- Receiver that updates notifications when the time on the device has been changed -->
        <receiver
            android:name="org.dmfs.provider.tasks.broadcast.TimeChangeBroadcastReceiver"
            android:label="@string/app_name" >
            <intent-filter>
                <action android:name="android.intent.action.TIME_SET" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

## TODO:

* add support for recurrence
* add support for extended attributes like alarms, attendees and categories

## License

Copyright (c) Marten Gajda 2015, licensed under Apache2.

