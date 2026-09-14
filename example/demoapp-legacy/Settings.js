import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, Switch, TouchableOpacity } from 'react-native';
import Pushwoosh from 'pushwoosh-react-native-plugin';

const Settings = () => {
    const [isEnabledRegister, setIsEnabledRegister] = useState(false);
    const [isEnabledServer, setIsEnabledServer] = useState(true);
    const [isModalRichMedia, setIsModalRichMedia] = useState(false);
    // What the last action reported.
    const [result, setResult] = useState('');

    const report = (text) => {
        console.log(text);
        setResult(text);
    };

    // Initialize registration state from push token
    useEffect(() => {
        Pushwoosh.getPushToken((token) => {
            setIsEnabledRegister(token != null && token !== "");
        });
    }, []);

    const toggleSwitchNotification = (isChecked) => {
        setIsEnabledRegister(isChecked);
        if (isChecked) {
            /**
             * To register for push notifications, call the following method:
             *
             * PUSHWOOSH CODE
             *    |   |
             *   _|   |_
             *   \     /
             *    \   /
             *     \_/
             */
            Pushwoosh.register(
                (token) => {
                    report(`Registered for pushes: ${token}`);
                },
                (error) => {
                    report(`Failed to register: ${error}`);
                }
            );
        } else {
            /**
             * To unregister for push notifications, call the following method:
             *
             * PUSHWOOSH CODE
             *    |   |
             *   _|   |_
             *   \     /
             *    \   /
             *     \_/
             */
            Pushwoosh.unregister();
            report('Unregistered from pushes');
        }
    };

    const toggleSwitchServerCommunication = () => {
        setIsEnabledServer(previousState => !previousState);
    };

    useEffect(() => {
        if (isEnabledServer) {
            /**
             * Server Communication Enable = true
             */
            Pushwoosh.setCommunicationEnabled(true);
        } else {
            /**
             * Server Communication Enable = false
             */
            Pushwoosh.setCommunicationEnabled(false);
        }
    }, [isEnabledServer]);

    useEffect(() => {
        Pushwoosh.getRichMediaType((type) => {
            setIsModalRichMedia(type === Pushwoosh.RichMediaStyle.MODAL);
        });
    }, []);

    const toggleSwitchRichMedia = (isChecked) => {
        setIsModalRichMedia(isChecked);
        if (isChecked) {
            /**
             * Set Rich Media to Modal mode
             */
            Pushwoosh.setRichMediaType(Pushwoosh.RichMediaStyle.MODAL);
        } else {
            /**
             * Set Rich Media to Default (full-screen) mode
             */
            Pushwoosh.setRichMediaType(Pushwoosh.RichMediaStyle.LEGACY);
        }
    };

    const clearNotificationCenter = () => {
        /**
         * Removes the notifications this app has already delivered from the notification centre
         * (iOS) or the status bar (Android).
         *
         * PUSHWOOSH CODE
         *    |   |
         *   _|   |_
         *   \     /
         *    \   /
         *     \_/
         */
        Pushwoosh.clearNotificationCenter();
        report('Notification center cleared');
    };

    const createLocalNotification = () => {
        /**
         * Schedules a notification on the device itself, without the Pushwoosh server. Tapping it
         * opens the app like a push does.
         *
         * PUSHWOOSH CODE
         *    |   |
         *   _|   |_
         *   \     /
         *    \   /
         *     \_/
         */
        Pushwoosh.createLocalNotification({
            msg: 'Local notification from the demo',
            seconds: 1,
            userData: { source: 'demo' },
        });
        report('Local notification scheduled');
    };

    return (
        <View style={styles.container}>
            <View style={styles.row}>
                <Text style={styles.label}>Register For Push Notifications</Text>
                <Switch
                    trackColor={{ false: "#767577", true: "#81b0ff" }}
                    thumbColor={isEnabledRegister ? "#f5dd4b" : "#f4f3f4"}
                    ios_backgroundColor="#3e3e3e"
                    onValueChange={toggleSwitchNotification}
                    value={isEnabledRegister}
                    testID="register-switch"
                />
            </View>
            <View style={styles.row}>
                <Text style={styles.label}>Server Communication Enabled</Text>
                <Switch
                    trackColor={{ false: "#767577", true: "#81b0ff" }}
                    thumbColor={isEnabledServer ? "#f5dd4b" : "#f4f3f4"}
                    ios_backgroundColor="#3e3e3e"
                    onValueChange={toggleSwitchServerCommunication}
                    value={isEnabledServer}
                    testID="server-communication-switch"
                />
            </View>
            <View style={styles.row}>
                <Text style={styles.label}>Modal Rich Media</Text>
                <Switch
                    trackColor={{ false: "#767577", true: "#81b0ff" }}
                    thumbColor={isModalRichMedia ? "#f5dd4b" : "#f4f3f4"}
                    ios_backgroundColor="#3e3e3e"
                    onValueChange={toggleSwitchRichMedia}
                    value={isModalRichMedia}
                    testID="modal-rich-media-switch"
                />
            </View>
            <View style={styles.row}>
                <TouchableOpacity
                    style={styles.button}
                    onPress={createLocalNotification}
                    testID="create-local-notification"
                >
                    <Text style={styles.buttonText}>CREATE LOCAL NOTIFICATION</Text>
                </TouchableOpacity>
            </View>
            <View style={styles.row}>
                <TouchableOpacity
                    style={styles.button}
                    onPress={clearNotificationCenter}
                    testID="clear-notification-center"
                >
                    <Text style={styles.buttonText}>CLEAR NOTIFICATION CENTER</Text>
                </TouchableOpacity>
            </View>
            <View style={styles.row}>
                <Text style={styles.result} testID="settings-result">{result}</Text>
            </View>
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'flex-start',
        alignItems: 'center',
        backgroundColor: '#ffffff',
        paddingTop: 20,
    },
    row: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        paddingVertical: 10,
        width: '100%',
    },
    label: {
        fontSize: 18,
        color: '#000000',
        flex: 1,
    },
    button: {
        flex: 1,
        backgroundColor: '#cccccc',
        borderRadius: 10,
        paddingVertical: 10,
        paddingHorizontal: 20,
        elevation: 5,
    },
    buttonText: {
        color: '#800080',
        fontWeight: 'bold',
        textAlign: 'center',
    },
    result: {
        flex: 1,
        color: '#000000',
    },
});

export default Settings;
