/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.notification.collection.coalescer

import com.android.systemui.log.LogBuffer
import com.android.systemui.log.LogLevel
import com.android.systemui.log.dagger.NotificationLog
import javax.inject.Inject

class GroupCoalescerLogger @Inject constructor(
    @NotificationLog private val buffer: LogBuffer
) {
    fun logEventCoalesced(key: String) {
        buffer.log(TAG, LogLevel.INFO, {
            str1 = key
        }, {
            "COALESCED: $str1"
        })
    }

    fun logEmitBatch(groupKey: String) {
        buffer.log(TAG, LogLevel.DEBUG, {
            str1 = groupKey
        }, {
            "Emitting event batch for group $str1"
        })
    }

    fun logEarlyEmit(modifiedKey: String, groupKey: String) {
        buffer.log(TAG, LogLevel.DEBUG, {
            str1 = modifiedKey
            str2 = groupKey
        }, {
            "Modification of notif $str1 triggered early emit of batched group $str2"
        })
    }

    fun logMaxBatchTimeout(modifiedKey: String, groupKey: String) {
        buffer.log(TAG, LogLevel.INFO, {
            str1 = modifiedKey
            str2 = groupKey
        }, {
            "Modification of notif $str1 triggered TIMEOUT emit of batched group $str2"
        })
    }
}

private const val TAG = "GroupCoalescer"