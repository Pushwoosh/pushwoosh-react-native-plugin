/**
 * @format
 */

// import App from './App';
import { name as appName } from './app.json';
import {
    Alert,
    AppRegistry,
    DeviceEventEmitter,
    Linking
} from 'react-native';
import { NavigationContainer, createNavigationContainerRef } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { enableScreens } from 'react-native-screens';
import Ionicons from 'react-native-vector-icons/Ionicons';
import Actions from './Actions';
import Settings from './Settings';
import DeepLink from './DeepLink';
import Pushwoosh from 'pushwoosh-react-native-plugin';

Pushwoosh.init({ "pw_appid": "XXXXX-XXXXX" });

const Tab = createBottomTabNavigator();
const navigationRef = createNavigationContainerRef();

// A deep link should land the user on the screen that shows it, cold start or live app. The URL
// travels as a route param because iOS getInitialURL() only ever returns the launch URL.
const handleDeepLink = (url) => {
    if (!url) {
        return;
    }

    Alert.alert('Deep link', url);

    if (navigationRef.isReady()) {
        navigationRef.navigate('DeepLink', { url });
    }
};

const App = () => {
    return (
        <NavigationContainer
            ref={navigationRef}
            onReady={() => {
                Linking.getInitialURL().then(handleDeepLink);
            }}
        >
            <Tab.Navigator
                screenOptions={({ route }) => ({
                    tabBarIcon: ({ focused, color, size }) => {
                        let iconName;
                        if (route.name === 'Actions') {
                            iconName = focused ? 'list' : 'list-outline';
                        } else if (route.name === 'Settings') {
                            iconName = focused ? 'settings' : 'settings-outline';
                        } else if (route.name === 'DeepLink') {
                            iconName = focused ? 'link' : 'link-outline';
                        }
                        return <Ionicons name={iconName} size={size} color={color} />;
                    },
                })}
            >
                <Tab.Screen name="Actions" component={Actions} options={{
                    title: 'PUSHWOOSH DEMO',
                    tabBarLabel: 'Actions',
                }} />
                <Tab.Screen name="Settings" component={Settings} options={{ title: 'PUSHWOOSH DEMO', tabBarLabel: 'Settings' }} />
                <Tab.Screen name="DeepLink" component={DeepLink} options={{ title: 'PUSHWOOSH DEMO', tabBarLabel: 'Deep Link' }} />
            </Tab.Navigator>
        </NavigationContainer>
    );
};

AppRegistry.registerComponent(appName, () => App);

Linking.addEventListener('url', ({ url }) => handleDeepLink(url));

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