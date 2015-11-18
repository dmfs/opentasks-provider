/*
 * Copyright (C) 2015 Marten Gajda <marten@dmfs.org>
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

package org.dmfs.provider.tasks.model;

import org.dmfs.provider.tasks.TaskContract;
import org.dmfs.provider.tasks.TaskContract.Tasks;
import org.dmfs.provider.tasks.model.adapters.BinaryFieldAdapter;
import org.dmfs.provider.tasks.model.adapters.BooleanFieldAdapter;
import org.dmfs.provider.tasks.model.adapters.DateTimeArrayFieldAdapter;
import org.dmfs.provider.tasks.model.adapters.DateTimeFieldAdapter;
import org.dmfs.provider.tasks.model.adapters.DurationFieldAdapter;
import org.dmfs.provider.tasks.model.adapters.IntegerFieldAdapter;
import org.dmfs.provider.tasks.model.adapters.LongFieldAdapter;
import org.dmfs.provider.tasks.model.adapters.RRuleFieldAdapter;
import org.dmfs.provider.tasks.model.adapters.StringFieldAdapter;
import org.dmfs.provider.tasks.model.adapters.UrlFieldAdapter;


/**
 * This class holds a static reference to all field adapters.
 * <p/>
 * TODO: Consider to move this to the {@link TaskContract} as it might be useful for clients. Optionally move it to some kind of internal contract.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
public final class TaskFieldAdapters
{
	/**
	 * Adapter for the row id of a task.
	 */
	public final static LongFieldAdapter _ID = new LongFieldAdapter(Tasks._ID);

	/**
	 * Adapter for the row id of the list of a task.
	 */
	public final static LongFieldAdapter LIST_ID = new LongFieldAdapter(Tasks.LIST_ID);

	/**
	 * Adapter for the row id of the list of a task.
	 */
	public final static StringFieldAdapter LIST_OWNER = new StringFieldAdapter(Tasks.LIST_OWNER);

	/**
	 * Adapter for the row id of original instance of a task.
	 */
	public final static LongFieldAdapter ORIGINAL_INSTANCE_ID = new LongFieldAdapter(Tasks.ORIGINAL_INSTANCE_ID);

	/**
	 * Adapter for the sync_id of original instance of a task.
	 */
	public final static StringFieldAdapter ORIGINAL_INSTANCE_SYNC_ID = new StringFieldAdapter(Tasks.ORIGINAL_INSTANCE_SYNC_ID);

	/**
	 * Adapter for the all day flag of a task.
	 */
	public final static BooleanFieldAdapter IS_ALLDAY = new BooleanFieldAdapter(Tasks.IS_ALLDAY);

	/**
	 * Adapter for the percent complete value of a task.
	 */
	public final static IntegerFieldAdapter PERCENT_COMPLETE = new IntegerFieldAdapter(Tasks.PERCENT_COMPLETE);

	/**
	 * Adapter for the status of a task.
	 */
	public final static IntegerFieldAdapter STATUS = new IntegerFieldAdapter(Tasks.STATUS);

	/**
	 * Adapter for the priority value of a task.
	 */
	public final static IntegerFieldAdapter PRIORITY = new IntegerFieldAdapter(Tasks.PRIORITY);

	/**
	 * Adapter for the classification value of a task.
	 */
	public final static IntegerFieldAdapter CLASSIFICATION = new IntegerFieldAdapter(Tasks.CLASSIFICATION);

	/**
	 * Adapter for the list name of a task.
	 */
	public final static StringFieldAdapter LIST_NAME = new StringFieldAdapter(Tasks.LIST_NAME);

	/**
	 * Adapter for the account name of a task.
	 */
	public final static StringFieldAdapter ACCOUNT_NAME = new StringFieldAdapter(Tasks.ACCOUNT_NAME);

	/**
	 * Adapter for the account type of a task.
	 */
	public final static StringFieldAdapter ACCOUNT_TYPE = new StringFieldAdapter(Tasks.ACCOUNT_TYPE);

	/**
	 * Adapter for the title of a task.
	 */
	public final static StringFieldAdapter TITLE = new StringFieldAdapter(Tasks.TITLE);

	/**
	 * Adapter for the location of a task.
	 */
	public final static StringFieldAdapter LOCATION = new StringFieldAdapter(Tasks.LOCATION);

	/**
	 * Adapter for the description of a task.
	 */
	public final static StringFieldAdapter DESCRIPTION = new StringFieldAdapter(Tasks.DESCRIPTION);

	/**
	 * Adapter for the start date of a task.
	 */
	public final static DateTimeFieldAdapter DTSTART = new DateTimeFieldAdapter(Tasks.DTSTART, Tasks.TZ, Tasks.IS_ALLDAY);

	/**
	 * Adapter for the raw start date timestamp of a task.
	 */
	public final static LongFieldAdapter DTSTART_RAW = new LongFieldAdapter(Tasks.DTSTART);

	/**
	 * Adapter for the due date of a task.
	 */
	public final static DateTimeFieldAdapter DUE = new DateTimeFieldAdapter(Tasks.DUE, Tasks.TZ, Tasks.IS_ALLDAY);

	/**
	 * Adapter for the raw due date timestamp of a task.
	 */
	public final static LongFieldAdapter DUE_RAW = new LongFieldAdapter(Tasks.DUE);

	/**
	 * Adapter for the start date of a task.
	 */
	public final static DurationFieldAdapter DURATION = new DurationFieldAdapter(Tasks.DURATION);

	/**
	 * Adapter for the dirty flag of a task.
	 */
	public final static BooleanFieldAdapter _DIRTY = new BooleanFieldAdapter(Tasks._DIRTY);

	/**
	 * Adapter for the deleted flag of a task.
	 */
	public final static BooleanFieldAdapter _DELETED = new BooleanFieldAdapter(Tasks._DELETED);

	/**
	 * Adapter for the completed date of a task.
	 */
	public final static DateTimeFieldAdapter COMPLETED = new DateTimeFieldAdapter(Tasks.COMPLETED, null, null);

	/**
	 * Adapter for the created date of a task.
	 */
	public final static DateTimeFieldAdapter CREATED = new DateTimeFieldAdapter(Tasks.CREATED, null, null);

	/**
	 * Adapter for the last modified date of a task.
	 */
	public final static DateTimeFieldAdapter LAST_MODIFIED = new DateTimeFieldAdapter(Tasks.LAST_MODIFIED, null, null);

	/**
	 * Adapter for the URL of a task.
	 */
	public final static UrlFieldAdapter URL = new UrlFieldAdapter(TaskContract.Tasks.URL);

	/**
	 * Adapter for the UID of a task.
	 */
	public final static StringFieldAdapter _UID = new StringFieldAdapter(TaskContract.Tasks._UID);

	/**
	 * Adapter for the raw time zone of a task.
	 */
	public final static StringFieldAdapter TIMEZONE_RAW = new StringFieldAdapter(TaskContract.Tasks.TZ);

	/**
	 * Adapter for the Color of the task.
	 * */
	public final static IntegerFieldAdapter LIST_COLOR = new IntegerFieldAdapter(TaskContract.Tasks.LIST_COLOR);

	/**
	 * Adapter for the access level of the task list.
	 * */
	public final static IntegerFieldAdapter LIST_ACCESS_LEVEL = new IntegerFieldAdapter(TaskContract.Tasks.LIST_ACCESS_LEVEL);

	/**
	 * Adapter for the visibility setting of the task list.
	 * */
	public final static BooleanFieldAdapter LIST_VISIBLE = new BooleanFieldAdapter(TaskContract.Tasks.VISIBLE);

	/**
	 * Adpater for the ID of the task.
	 * */
	public static final IntegerFieldAdapter TASK_ID = new IntegerFieldAdapter(TaskContract.Tasks._ID);

	/**
	 * Adapter for the IS_CLOSED flag of a task.
	 * */
	public static final BooleanFieldAdapter IS_CLOSED = new BooleanFieldAdapter(TaskContract.Tasks.IS_CLOSED);

	/**
	 * Adapter for the IS_NEW flag of a task.
	 * */
	public static final BooleanFieldAdapter IS_NEW = new BooleanFieldAdapter(TaskContract.Tasks.IS_NEW);

	/**
	 * Adapter for the PINNED flag of a task.
	 * */
	public static final BooleanFieldAdapter PINNED = new BooleanFieldAdapter(TaskContract.Tasks.PINNED);

	/**
	 * Adapter for the HAS_ALARMS flag of a task.
	 * */
	public static final BooleanFieldAdapter HAS_ALARMS = new BooleanFieldAdapter(TaskContract.Tasks.HAS_ALARMS);

	/**
	 * Adapter for the HAS_PROPERTIES flag of a task.
	 * */
	public static final BooleanFieldAdapter HAS_PROPERTIES = new BooleanFieldAdapter(TaskContract.Tasks.HAS_PROPERTIES);

	/**
	 * Adapter for the RRULE of a task.
	 * */
	public static final RRuleFieldAdapter RRULE = new RRuleFieldAdapter(TaskContract.Tasks.RRULE);

	/**
	 * Adapter for the RDATE of a task.
	 * */
	public static final DateTimeArrayFieldAdapter RDATE = new DateTimeArrayFieldAdapter(TaskContract.Tasks.RDATE, TaskContract.Tasks.TZ);

	/**
	 * Adapter for the EXDATE of a task.
	 * */
	public static final DateTimeArrayFieldAdapter EXDATE = new DateTimeArrayFieldAdapter(TaskContract.Tasks.EXDATE, TaskContract.Tasks.TZ);

	/**
	 * Adapter for the SYNC1 field of a task.
	 * */
	public static final BinaryFieldAdapter SYNC1 = new BinaryFieldAdapter(TaskContract.Tasks.SYNC1);

	/**
	 * Adapter for the SYNC2 field of a task.
	 * */
	public static final BinaryFieldAdapter SYNC2 = new BinaryFieldAdapter(TaskContract.Tasks.SYNC2);

	/**
	 * Adapter for the SYNC3 field of a task.
	 * */
	public static final BinaryFieldAdapter SYNC3 = new BinaryFieldAdapter(TaskContract.Tasks.SYNC3);

	/**
	 * Adapter for the SYNC4 field of a task.
	 * */
	public static final BinaryFieldAdapter SYNC4 = new BinaryFieldAdapter(TaskContract.Tasks.SYNC4);

	/**
	 * Adapter for the SYNC5 field of a task.
	 * */
	public static final BinaryFieldAdapter SYNC5 = new BinaryFieldAdapter(TaskContract.Tasks.SYNC5);

	/**
	 * Adapter for the SYNC6 field of a task.
	 * */
	public static final BinaryFieldAdapter SYNC6 = new BinaryFieldAdapter(TaskContract.Tasks.SYNC6);

	/**
	 * Adapter for the SYNC7 field of a task.
	 * */
	public static final BinaryFieldAdapter SYNC7 = new BinaryFieldAdapter(TaskContract.Tasks.SYNC7);

	/**
	 * Adapter for the SYNC8 field of a task.
	 * */
	public static final BinaryFieldAdapter SYNC8 = new BinaryFieldAdapter(TaskContract.Tasks.SYNC8);

	/**
	 * Adapter for the SYNC_VERSION field of a task.
	 * */
	public static final BinaryFieldAdapter SYNC_VERSION = new BinaryFieldAdapter(TaskContract.Tasks.SYNC_VERSION);

	/**
	 * Adapter for the SYNC_ID field of a task.
	 * */
	public static final StringFieldAdapter SYNC_ID = new StringFieldAdapter(TaskContract.Tasks._SYNC_ID);


	/**
	 * Private constructor to prevent instantiation.
	 */
	private TaskFieldAdapters()
	{
	}
}
