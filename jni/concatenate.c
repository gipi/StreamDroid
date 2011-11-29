/*
 * Copyright (C) 2009 The Android Open Source Project
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
 *
 */
#include <string.h>
#include <jni.h>
#include <android/log.h>

/**
 * DONT Use _ in method names.
 */
/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/example/HelloJni/HelloJni.java
 */
jstring
Java_org_ktln2_android_streamdroid_StreamDroidActivity_concatenate( JNIEnv* env,
                                                  jobject thiz )
{
    return (*env)->NewStringUTF(env, "Hello from JNI !");
}

// http://java.sun.com/docs/books/jni/html/objtypes.html#4013
// http://docs.oracle.com/javase/1.3/docs/guide/jni/spec/types.doc.html
jstring Java_org_ktln2_android_streamdroid_StreamDroidActivity_concatenateBis( JNIEnv* env, jobject thiz, jstring myString)
{
	return myString;
}

jint Java_org_ktln2_android_streamdroid_StreamDroidActivity_printArray   (JNIEnv* env, jobject thiz, jobjectArray videoNames) {
	const char* videoURI[2];
	jsize n = (*env)->GetArrayLength(env, videoNames);
	int cycle;
	for (cycle = 0 ; cycle < n ; cycle++) {
		// no casting to jstring needed?
		jobject string = (*env)->GetObjectArrayElement(env, videoNames, cycle);
	
		videoURI[cycle] = (*env)->GetStringUTFChars(env, string, 0);
		__android_log_print(ANDROID_LOG_DEBUG, "NDK", "NDK:LC: [%s]", videoURI[cycle]);
	}

	// TODO: release videoURI[] elements

	return 0;
}
