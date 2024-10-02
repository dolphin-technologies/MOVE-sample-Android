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
 * The RegisterResponse with authCode which will be received from the backend.
 */
data class RegisterResponse(
    var authCode: String // authCode ... The authentication code for this project for user with userId.
)