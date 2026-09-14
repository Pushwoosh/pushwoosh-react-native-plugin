/**
 * @format
 */

import {name as appName} from './app.json';
import {AppRegistry, DeviceEventEmitter, Linking} from 'react-native';
import {enableScreens} from 'react-native-screens';
import Pushwoosh from 'pushwoosh-react-native-plugin';
import App, {handleDeepLink} from './App';
import {recordEvent} from './eventLog';

Pushwoosh.init({pw_appid: 'XXXXX-XXXXX'});

AppRegistry.registerComponent(appName, () => App);

Linking.addEventListener('url', ({url}) => handleDeepLink(url));

// this event is fired when the push is received in the app
DeviceEventEmitter.addListener('pushReceived', e => {
  console.log('pushReceived: ' + JSON.stringify(e));
  // shows a push is received. Implement passive reaction to a push, such as UI update or data download.
  recordEvent('pushReceived', e);
});

// this event is fired when user clicks on notification
DeviceEventEmitter.addListener('pushOpened', e => {
  console.log('pushOpened: ' + JSON.stringify(e));
  // shows a user tapped the notification. Implement user interaction, such as showing push details
  recordEvent('pushOpened', e);
});

enableScreens();
