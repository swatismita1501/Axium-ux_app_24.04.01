/*---------------------------------------------------------------------------------*
 * INGENICO Technical Software Department                                          *
 *---------------------------------------------------------------------------------*
 * Copyright (c) 2025 Ingenico Inc.                                         *
 * 3025 Windward Plaza, Suite 600, Alpharetta, Georgia, 30005, United States       *
 * All rights reserved.                                                            *
 * This source program is the property of the INGENICO Company mentioned above     *
 * and may not be copied in any form or by any means, whether in part or in whole, *
 * except under license expressly granted by such INGENICO company.                *
 * All copies of this source program, whether in part or in whole, and whether     *
 * modified or not, must display this and all other embedded copyright and         *
 * ownership notices in full.                                                      *
 *---------------------------------------------------------------------------------*/

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "com.ingenico.retail.ux.app"
        minSdk = 29
        targetSdk = 29
        versionName = getVersionName()
        versionCode = getVersionCode(versionName as String)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    applicationVariants.all {
        outputs.forEach { output ->
            (output as? com.android.build.gradle.internal.api.BaseVariantOutputImpl)?.apply {
                outputFileName = if (buildType.name == "debug")
                    "${rootProject.name}-${buildType.name}-$versionName.apk"
                else
                    "${rootProject.name}-$versionName.apk"
            }
        }
    }

    signingConfigs {
        create("arcSigning") {
            keyAlias = "key0"
            keyPassword = "admin123"
            storePassword = "admin123"
            storeFile = file("nar_test_key.jks")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("arcSigning")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("arcSigning")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

fun getVersionName(): String {
    var year = "XX"
    var releaseNumber = "XX"
    var patch = "XX"
    var build = "XXXX"
    File("${project.projectDir}/version.txt").forEachLine {
        "SET\\(version.year (\\d+)\\)".toRegex().matchEntire(it)?.let { res ->
            year = res.groups[1]?.value ?: "XX"
        }
        "SET\\(version.releaseNum (\\d+)\\)".toRegex().matchEntire(it)?.let { res ->
            releaseNumber = res.groups[1]?.value ?: "XX"
        }
        "SET\\(version.patch (\\d+)\\)".toRegex().matchEntire(it)?.let { res ->
            patch = res.groups[1]?.value ?: "XX"
        }
        "SET\\(version.build (\\d+)\\)".toRegex().matchEntire(it)?.let { res ->
            build = res.groups[1]?.value ?: "XX"
        }
    }
    return "$year.$releaseNumber.$patch-$build"
}

fun getVersionCode(version: String): Int {
    return getVersions(version)?.let { (year, releaseNumber, patch, build) ->
        val code = (year - 22) * 10000000 + (releaseNumber) * 100000 + (patch) * 1000 + (build).rem(1000)
        if (code == 0) 1 else code
    } ?: throw GradleException("version doesn't respect the version rule 'year.releaseNumber.patch.kernel-build': $version")
}

fun getVersions(version: String?): Array<Int>? {
    return version?.let {
        "(\\d+)\\.?(\\d+)?\\.?(\\d+)\\-?(.+)?".toRegex().matchEntire(version)?.let { matchEnt ->

            matchEnt.let {
                val year = matchEnt.groups[1]?.value?.toInt() ?: 22
                val releaseNumber = matchEnt.groups[2]?.value?.toInt() ?: 0
                val patch = matchEnt.groups[3]?.value?.toInt() ?: 0
                val build = matchEnt.groups[4]?.value?.toInt() ?: 0

                arrayOf(year, releaseNumber, patch, build)
            }
        }
    }
}

dependencies {

    // > gradlew assemble -PuseLocalAar
    // Unfortunately this flag is not applied for settings.gradle file.
    // Need to have local copy (settings.gradle.local).
    if (project.hasProperty("useLocalAar")) {
        implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    } else {
        implementation(project(":arc-types:arc-types"))
        implementation(project(":arc-ux-server:ux-server"))
    }

    // uSDK
    implementation("com.usdk:api:13.14.0")
    implementation("com.usdk:api-extension:2.2.4")
    implementation ("com.ingenico.acc:dxfw-lib-platform-common-android:2.7.1-dev-1@aar")
    {
        exclude(group = "com.usdk.api", module = "extension")
    }

    implementation("com.google.code.gson:gson:2.8.9")

    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Barcode camera dependencies
    implementation("androidx.camera:camera-lifecycle:1.1.0")
    implementation("androidx.camera:camera-camera2:1.1.0")
    implementation("androidx.camera:camera-view:1.1.0")
    implementation("androidx.camera:camera-core:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.2.0")
}