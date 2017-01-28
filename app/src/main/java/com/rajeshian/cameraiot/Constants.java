/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.rajeshian.cameraiot;

import com.amazonaws.regions.Regions;

public class Constants {

    /*
     * You should replace these values with your own. See the README for details
     * on what to fill in.
     */


    /*
     * Note, you must first create a bucket using the S3 console before running
     * the sample (https://console.aws.amazon.com/s3/). After creating a bucket,
     * put it's name in the field below.
     */
    public static final String BUCKET_NAME = "camiotgallery";

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    public static final String CUSTOMER_SPECIFIC_ENDPOINT = "aqwf0ncryp7rw.iot.ap-southeast-2.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    public static final String COGNITO_POOL_ID = "ap-southeast-2:5296b281-1e1c-40c3-81f8-f0c292d87cf0";

    // Region of AWS IoT
    public static final Regions MY_REGION = Regions.AP_SOUTHEAST_2;

    //Topic for sending device commands
    public static final String TOPIC_DEVICE_COMMAND = "sdk/test/Onoff";
    public static final String TOPIC_DEVICE_IP="sdk/test/IPadd";
    public static final String TOPIC_DEVICE_NOTIFICATION = "sdk/test/Notif";
    public static final String TOPIC_DEVICE_SNAP="sdk/test/Snap";



}
