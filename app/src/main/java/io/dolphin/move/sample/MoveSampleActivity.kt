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

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import io.dolphin.move.sample.ui.MoveSampleFragment

const val PERMISSIONS_REQUEST_CODE = 123

/**
 * The MoveSampleActivity which contains the MoveSampleFragment.
 */
class MoveSampleActivity : AppCompatActivity() {

    /**
     * Create the MoveSampleActivity, set the toolbar and add the MoveSampleFragment.
     *
     * @param savedInstanceState The saved instance state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.move_sample_activity)
        val toolbar = findViewById<Toolbar>(R.id.activity_toolbar)
        setSupportActionBar(toolbar)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MoveSampleFragment.newInstance())
                .commitNow()
        }
    }

    /**
     * Handle the result of the permissions request reentering the app.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult(), allowing you to identify who this result came from.
     * @param resultCode The integer result code returned by the child activity through its setResult().
     * @param data An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            notifyPermissionState()
        }
    }

    /**
     * Handle the result of the permissions request.
     *
     * @param requestCode The request code passed in requestPermissions(Activity, String[], int)
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions which is either PackageManager.PERMISSION_GRANTED or PackageManager.PERMISSION_DENIED. Never null.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // Handle the result of the permissions request.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    notifyPermissionState()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    /**
     * Notify the MOVE SDK that the app has handled the occurred errors.
     */
    private fun notifyPermissionState() {
        MoveSdkManager.getInstance(this).moveSdk?.resolveError()
    }
}