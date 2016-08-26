React Native Pushwoosh Push Notifications module
===================================================

| [Guide](http://docs.pushwoosh.com/docs/react-native-pushwoosh-push-notifications-module-for-android) | [Documentation](docs/README.md) | [Sample](https://github.com/Pushwoosh/pushwoosh-react-native-sample) |
| ------------------------------------------------------------- | ---------------------------------------- | ------------------------------------- |


## Android Setup

### Step 1 - Install plugin

```
npm install pushwoosh-react-native-plugin --save
```

### Step 2 - Include pushwooshplugin library project

In **android/settings.gradle** file, make the following changes:

```gradle
...
include ':pushwooshplugin'
project(':pushwooshplugin').projectDir = new File(rootProject.projectDir, '../node_modules/pushwoosh-react-native-plugin/src/android')
```

In **android/app/build.gradle** add pushwooshplugin dependency:

```gradle
dependencies {
    ...
    compile project(':pushwooshplugin')
}
```

Optional: To use non-default `play-service-gcm` version (the plugin uses 8.4.0), modify your `android/app/build.gradle` dependencies.
This might be required in case of conflicts with other react-native plugins.
```gradle
...

dependencies {
    ...
    compile project(':pushwooshplugin')
    compile ('com.google.android.gms:play-services-gcm:9.+') {force = true;}
    compile ('com.google.android.gms:play-services-location:9.+') {force = true;}
}
```

For RN < v.0.29.0 In **MainActivity.java** add:

```java
...
import com.pushwoosh.reactnativeplugin.PushwooshPackage;

public class MainActivity extends ReactActivity {

    ...

    @Override
    protected List<ReactPackage> getPackages() {
        return Arrays.<ReactPackage>asList(
            new MainReactPackage(),
            new PushwooshPackage() // register Pushwoosh plugin here
        );
    }
}
```
For RN >= v.0.29.0 In **MainApplication.java** add:

```java
...
import com.pushwoosh.reactnativeplugin.PushwooshPackage;

public class MainApplication extends Application implements ReactApplication {
    ...

    @Override
    protected List<ReactPackage> getPackages() {
        return Arrays.<ReactPackage>asList(
            new MainReactPackage(),
            new PushwooshPackage() // register Pushwoosh plugin here
        );
    }
}
```

### Step 3 - Use module

```js
var Pushwoosh = require('pushwoosh-react-native-plugin');

Pushwoosh.init({ "pw_appid" : "YOUR_PUSHWOOSH_PROJECT_ID" , "project_number" : "YOUR_GCM_PROJECT_NUMBER" });
Pushwoosh.register();
```


## iOS Setup

### Step 1 - Install plugin

```
npm install pushwoosh-react-native-plugin --save
```

### Step 2 - Include PushwooshPlugin project

Drag the **PushwooshPlugin.xcodeproj** (located in **node_modules/pushwoosh-react-native-plugin/src/ios**) as a dependency project into your React Native XCode project.
Link your project with **libPushwooshPlugin.a**, **libstdc++** and **libz** libraries.

### Step 3 - Use module

```js
var Pushwoosh = require('pushwoosh-react-native-plugin');

Pushwoosh.init({ "pw_appid" : "YOUR_PUSHWOOSH_PROJECT_ID" });
Pushwoosh.register();
```
