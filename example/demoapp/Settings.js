import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, Switch } from 'react-native';
import Pushwoosh from 'pushwoosh-react-native-plugin';

const Settings = () => {
    const [isEnabledRegister, setIsEnabledRegister] = useState(false);
    const [isEnabledServer, setIsEnabledServer] = useState(true);
    const [isModalRichMedia, setIsModalRichMedia] = useState(false);

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
            * initialize Pushwoosh SDK.
            * Example params: {"pw_appid": "application id", "project_number": "FCM sender id"}
            *
            * 1. app_id - YOUR_APP_ID
            * 2. sender_id - FCM_SENDER_ID
            */
            Pushwoosh.init({ "pw_appid" : "XXXXX-XXXXX", "project_number":"XXXXXXXXXXXX"});

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
                    console.warn("Registered for pushes: " + token);
                    Pushwoosh.getPushToken(function(token) {
                        console.warn("Push token: " + token);
                    });
                },
                (error) => {
                    console.warn("Failed to register: " + error);
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
                />
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
});

export default Settings;
