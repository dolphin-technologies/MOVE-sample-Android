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

package io.dolphin.move.sample.backend

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * API for creating a user on the MOVE Backend.
 * In real use this should be done by the BACKEND and NOT on the frontend.
 * Retrofit is used to create this API.
 */
interface MoveBackendApi {

    /**
     * Register a user on the MOVE Backend by using retrofit.
     *
     * @param userId An unique user id within your project.
     * @param authHeader The auth header containing the API key.
     * @return The response body containing the user's data.
     */
    @GET("/v20/user/authcode")
    fun registerUserWithAuthCode(
        @Query("userId") userId: String,
        @Header("Authorization") authHeader: String?
    ): Call<RegisterResponse>

    companion object {
        private var BASE_URL = "https://sdk.dolph.in/"

        /**
         * Create the MoveBackendApi.
         *
         * @return The the MoveBackendApi .
         */
        fun create(): MoveBackendApi {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(MoveBackendApi::class.java)
        }
    }
}