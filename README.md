React Native Pushwoosh Push Notifications module
===================================================

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

In **MainActivity.java** add:

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

## Step 3 - Use module

```js
var Pushwoosh = require('pushwoosh-react-native-plugin');

Pushwoosh.init({ "pw_appid" : "YOUR_PUSHWOOSH_PROJECT_ID" , "project_number" : "YOUR_GCM_PROJECT_NUMBER" });
Pushwoosh.register();
```
