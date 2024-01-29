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

/**
 * API for creating a user on the MOVE Backend.
 * In real use this should be done by the BACKEND and NOT on the frontend.
 * Retrofit is used to create this API.
 */

/**
 * The RegisterRequest with the userId which will be send to the backend.
 */
data class RegisterRequest(
    var userId: String
)

/**
 * The RegisterResponse with the accessToken, refreshToken, userId, audience,
 * installationId and projectId which will be received from the backend.
 */
data class RegisterResponse(
    var accessToken: String, // accessToken ... allows communication with MOVE SDK backend
    var refreshToken: String, // refreshToken ... allows app to renew access token
    var userId: String, // userId ... to identify the user within the project.
    var audience: String, // audience ... not used in this example.
    var installationId: String, // installationId ... not used in this example.
    var projectId: String // projectId ... to identify the project.
)