/**
 * @format
 */

// import App from './App';
import {name as appName} from './app.json';
import {
    AppRegistry,
    DeviceEventEmitter} from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { enableScreens } from 'react-native-screens';
import Actions from './Actions';
import Settings from './Settings';

const Tab = createBottomTabNavigator();

const App = () => {
    return (
        <NavigationContainer>
            <Tab.Navigator>
                <Tab.Screen name="Actions" component={Actions} options={{ 
                  title: 'PUSHWOOSH DEMO', 
                  tabBarLabel: 'Actions',
                  }} />
                <Tab.Screen name="Settings" component={Settings} options={{ title: 'PUSHWOOSH DEMO', tabBarLabel: 'Settings' }}  />
            </Tab.Navigator>
        </NavigationContainer>
    );
};

AppRegistry.registerComponent(appName, () => App);

// this event is fired when the push is received in the app
DeviceEventEmitter.addListener('pushReceived', (e) => {
  console.warn("pushReceived: " + JSON.stringify(e));
  // shows a push is received. Implement passive reaction to a push, such as UI update or data download.
});

// this event is fired when user clicks on notification
DeviceEventEmitter.addListener('pushOpened', (e) => {
  console.warn("pushOpened: " + JSON.stringify(e));
  // shows a user tapped the notification. Implement user interaction, such as showing push details
});

enableScreens();