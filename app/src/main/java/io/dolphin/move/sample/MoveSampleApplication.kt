/*
 *  Copyright 2021 Dolphin Technologies GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http:*www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package io.dolphin.move.sample

import android.app.Application

class MoveSampleApplication : Application() {

    companion object {
        const val PREF_SHARED_NAME = "movesample"

        const val PREF_ACCESS_TOKEN = "accessToken"
        const val PREF_REFRESH_TOKEN = "refreshToken"
        const val PREF_USER_ID = "userId"
        const val PREF_ENABLED = "enabled"
    }

    // Keep reference in your application to prevent garbage collection
    private lateinit var moveSdkManager: MoveSdkManager

    override fun onCreate() {
        super.onCreate()

        moveSdkManager = MoveSdkManager.getInstance(this)
    }
}