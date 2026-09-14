/**
 * @format
 */

import React from 'react';
import {Linking} from 'react-native';
import {
  NavigationContainer,
  createNavigationContainerRef,
} from '@react-navigation/native';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import Ionicons from 'react-native-vector-icons/Ionicons';
import Actions from './Actions';
import Settings from './Settings';
import DeepLink from './DeepLink';

type RootTabParamList = {
  Actions: undefined;
  Settings: undefined;
  DeepLink: {url: string} | undefined;
};

const Tab = createBottomTabNavigator<RootTabParamList>();
const navigationRef = createNavigationContainerRef<RootTabParamList>();

// A deep link should land the user on the screen that shows it, cold start or live app. The URL
// travels as a route param because iOS getInitialURL() only ever returns the launch URL.
export const handleDeepLink = (url: string | null | undefined) => {
  if (!url) {
    return;
  }

  if (navigationRef.isReady()) {
    navigationRef.navigate('DeepLink', {url});
  }
};

// React Navigation 6 names the tab button's testID option tabBarTestID and 7 renamed it to
// tabBarButtonTestID. Both samples pass both, spread rather than written inline so the option
// the installed version does not know stays out of the type check. The end-to-end flows address
// the tabs through these ids.
const tabTestID = (id: string) => ({tabBarTestID: id, tabBarButtonTestID: id});

const tabIcons: Record<keyof RootTabParamList, [string, string]> = {
  Actions: ['list', 'list-outline'],
  Settings: ['settings', 'settings-outline'],
  DeepLink: ['link', 'link-outline'],
};

const App = () => {
  return (
    <NavigationContainer
      ref={navigationRef}
      onReady={() => {
        Linking.getInitialURL().then(handleDeepLink);
      }}>
      <Tab.Navigator
        screenOptions={({route}) => ({
          tabBarIcon: ({focused, color, size}) => {
            const [active, inactive] = tabIcons[route.name];
            return (
              <Ionicons
                name={focused ? active : inactive}
                size={size}
                color={color}
              />
            );
          },
        })}>
        <Tab.Screen
          name="Actions"
          component={Actions}
          options={{
            title: 'PUSHWOOSH DEMO',
            tabBarLabel: 'Actions',
            ...tabTestID('tab-actions'),
          }}
        />
        <Tab.Screen
          name="Settings"
          component={Settings}
          options={{
            title: 'PUSHWOOSH DEMO',
            tabBarLabel: 'Settings',
            ...tabTestID('tab-settings'),
          }}
        />
        <Tab.Screen
          name="DeepLink"
          component={DeepLink}
          options={{
            title: 'PUSHWOOSH DEMO',
            tabBarLabel: 'Deep Link',
            ...tabTestID('tab-deep-link'),
          }}
        />
      </Tab.Navigator>
    </NavigationContainer>
  );
};

export default App;
