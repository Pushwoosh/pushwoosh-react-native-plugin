import React, { useState } from 'react';
import { View, StyleSheet, TextInput, ScrollView, TouchableOpacity, Text, Image, Alert } from 'react-native';
import Pushwoosh from 'pushwoosh-react-native-plugin';

const ButtonWithTextInput = ({ buttonText, onPress, placeholder1, placeholder2 }) => {
    const [inputValue1, setInputValue1] = useState('');
    const [inputValue2, setInputValue2] = useState('');

    const handlePress = () => {
        onPress(inputValue1, inputValue2);
    };

    return (
        <View style={styles.header}>
            <TouchableOpacity
                style={[styles.button, { flex: 1 }]}
                onPress={handlePress} 
            >
                <Text style={styles.buttonText}>{buttonText}</Text>
            </TouchableOpacity>
            {placeholder1 && (
                <TextInput
                    style={[styles.textField, { flex: 1, marginRight: 5 }]}
                    placeholder={placeholder1}
                    value={inputValue1}
                    onChangeText={text => setInputValue1(text)}
                />
            )}
            {placeholder2 && (
                <TextInput
                    style={[styles.textField, { flex: 1 }]}
                    placeholder={placeholder2}
                    value={inputValue2}
                    onChangeText={text => setInputValue2(text)}
                />
            )}
        </View>
    );
};

const Button = ({ buttonText, onPress }) => {
    return (
        <View style={styles.header}>
            <TouchableOpacity
                style={[styles.button, { flex: 1 }]}
                onPress={onPress} 
            >
                <Text style={styles.buttonText}>{buttonText}</Text>
            </TouchableOpacity>
        </View>
    );
};

const Actions = () => {
    return (
        <ScrollView contentContainerStyle={styles.container}>
            <View style={styles.imageContainer}>
                <Image
                    source={require('./logos.png')} 
                    style={styles.image}
                />
            </View>
            <ButtonWithTextInput buttonText="SET USER ID" onPress={(inputValue1) => { 
                /**
                * Set User indentifier. This could be Facebook ID, username or email, or any other user ID.
                * This allows data and events to be matched across multiple user devices.
                * 
                * PUSHWOOSH CODE 
                *    |   |
                *   _|   |_
                *   \     /
                *    \   /
                *     \_/
                */
                Pushwoosh.setUserId(inputValue1, 
                    () => {
                        console.log("User ID was successfully set.");
                    },
                    (error) => {
                        console.error("Failed to set User ID:", error);
                    }
                );
             }} placeholder1="USER ID" />

            <ButtonWithTextInput buttonText="POST EVENT" onPress={(inputValue1) => {
            /**
            * Post events for In-App Messages. This can trigger In-App message HTML as specified in Pushwoosh Control Panel.
            * [event] is string name of the event
            * [attributes] is map contains additional event attributes
            * 
            * PUSHWOOSH CODE 
            *    |   |
            *   _|   |_
            *   \     /
            *    \   /
            *     \_/
            */
                Pushwoosh.postEvent(inputValue1, { attribute1: "value1", attribute2: "value2" });
            }} placeholder1="EVENT NAME" />

            <ButtonWithTextInput buttonText="SET LANGUAGE" onPress={(inputValue1) => { 
            /**
            * setLanguage(language: string): void; method
            * 
            * PUSHWOOSH CODE 
            *    |   |
            *   _|   |_
            *   \     /
            *    \   /
            *     \_/
            */
                Pushwoosh.setLanguage(inputValue1);
            }} placeholder1="en" />

            <ButtonWithTextInput buttonText="SET TAGS" onPress={(inputValue1, inputValue2) => { 
            /**
            * Associates device with given [tags]. If setTags request fails tags will be resent on the next application launch.
            * 
            * PUSHWOOSH CODE 
            *    |   |
            *   _|   |_
            *   \     /
            *    \   /
            *     \_/
            */
                Pushwoosh.setTags({ 'tags': inputValue1, 'value': inputValue2 },
                () => {
                    console.log("Tags were successfully set.");
                    
                },
                (error) => {
                    console.error("Failed to set tags:", error);
                });
            }} placeholder1="KEY" placeholder2="VALUE" />
            
            <ButtonWithTextInput buttonText="USER EMAILS" onPress={(inputValue1, inputValue2) => { 
                /**
                * SET USER EMAILS
                * setUserEmails(userId: string, emails: (string | string[]), success?: () => void, fail?: (error: Error) => void): void;
                *             
                * PUSHWOOSH CODE 
                *    |   |
                *   _|   |_
                *   \     /
                *    \   /
                *     \_/
                */
                Pushwoosh.setUserEmails(
                    inputValue1,
                    [inputValue2],
                    () => {
                        console.log("User emails were successfully set.");
                    },
                    (error) => {
                        console.error("Failed to set user emails:", error);
                    }
                );
            }} placeholder1="USER" placeholder2="EMAILS" />
            <ButtonWithTextInput buttonText="SET EMAILS" onPress={(inputValue1) => { 
                /**
                * SET EMAILS
                * setEmails(emails: (string | string[]), success?: () => void, fail?: (error: Error) => void): void; 
                *             
                * PUSHWOOSH CODE 
                *    |   |
                *   _|   |_
                *   \     /
                *    \   /
                *     \_/
                */
                Pushwoosh.setEmails( [inputValue1],
                    () => {
                        console.log("Emails were successfully set.");
                    },
                    (error) => {
                        console.error("Failed to set emails:", error);
                    }
                );
            }} placeholder1="EMAILS" />

            <Button buttonText="GET HWID" onPress={() => { 
                /**
                * Pushwoosh HWID associated with current device
                * 
                * PUSHWOOSH CODE 
                *    |   |
                *   _|   |_
                *   \     /
                *    \   /
                *     \_/
                */               
                Pushwoosh.getHwid((hwid) => { 
                    Alert.alert('HWID: ', hwid); 
                });
            }}/>

            <Button buttonText="GET PUSH TOKEN" onPress={() => { 
                /**
                * Push notification token or null if device is not registered yet.
                * 
                * PUSHWOOSH CODE 
                *    |   |
                *   _|   |_
                *   \     /
                *    \   /
                *     \_/
                */
                Pushwoosh.getPushToken((token) => { 
                    Alert.alert('PUSH TOKEN: ', token); 
                });
            }}/>
            <Button buttonText="GET USER ID" onPress={() => { 
                /**
                * GET USER ID
                * 
                * PUSHWOOSH CODE 
                *    |   |
                *   _|   |_
                *   \     /
                *    \   /
                *     \_/
                */
                Pushwoosh.getUserId((userId) => {
                    Alert.alert('USER ID: ', userId);
                });
            }}/>
            <Button 
                buttonText="SHOW PUSH NOTIFICATION ALERT"  onPress={() => { 
                /**
                * GET SHOW PUSH NOTIFICATION ALERT STATE
                * 
                * PUSHWOOSH CODE 
                *    |   |
                *   _|   |_
                *   \     /
                *    \   /
                *     \_/
                */
                    Pushwoosh.getShowPushnotificationAlert((willShow) => { 
                        if (willShow) {
                            Alert.alert('PUSH NOTIFICATION ALERT ENABLED');
                        } else {
                            Alert.alert('PUSH NOTIFICATION ALERT DISABLED');
                        }
                    }); 
                }} 
            />
        </ScrollView>
    );
};

const styles = StyleSheet.create({
    container: {
        flexGrow: 1,
        backgroundColor: '#ffffff',
        alignItems: 'center',
    },
    header: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        paddingVertical: 10,
    },
    textField: {
        borderWidth: 1,
        borderColor: '#cccccc',
        borderRadius: 5,
        padding: 10,
    },
    button: {
        backgroundColor: '#cccccc',
        borderRadius: 10,
        paddingVertical: 10,
        paddingHorizontal: 20,
        marginRight: 10,
        shadowColor: '#000', 
        shadowOffset: {
            width: 0,
            height: 2,
        },
        shadowOpacity: 0.25, 
        shadowRadius: 3.84, 
        elevation: 5,
    },
    buttonText: {
        color: '#800080',
        fontWeight: 'bold',
        textAlign: 'center',
    },
    imageContainer: {
        justifyContent: 'center',
        alignItems: 'center',
        marginTop: 20,
    },
    image: {
        width: 75,
        height: 75,
        borderRadius: 20
    },
});

export default Actions;
