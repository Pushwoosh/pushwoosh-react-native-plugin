# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in $HOME/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Shipped to the consuming app through consumerProguardFiles.
#
# The Pushwoosh SDK stores its requests with WorkManager, which keeps them in a Room database.
# room-runtime ships `-keep class * extends androidx.room.RoomDatabase`, which holds the generated
# class but not the constructor Room instantiates it with, and the R8 full mode AGP 8 runs by
# default drops that constructor. A minified Release app then dies on startup in androidx.startup
# with "Failed to create an instance of class androidx.work.impl.WorkDatabase".
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# The same shape one layer up: WorkManager instantiates the work request's input merger by name,
# and work-runtime ships `-keep class * extends androidx.work.InputMerger`, which holds the class
# but not that constructor. Under R8 full mode every worker the SDK enqueues then dies before it
# runs with "Could not create Input Merger androidx.work.OverwritingInputMerger", so registering
# for pushes reports neither a token nor an error - the app just never hears back.
-keep class * extends androidx.work.InputMerger { <init>(); }
