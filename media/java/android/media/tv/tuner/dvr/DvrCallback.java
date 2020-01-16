/*
 * Copyright 2019 The Android Open Source Project
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

package android.media.tv.tuner.dvr;

/**
 * Callback interface for receiving information from DVR interfaces.
 *
 * @hide
 */
public interface DvrCallback {
    /**
     * Invoked when record status changed.
     */
    void onRecordStatusChanged(int status);
    /**
     * Invoked when playback status changed.
     */
    void onPlaybackStatusChanged(int status);
}