import React, { useEffect, useState } from 'react';
import { View, StyleSheet, TextInput, ScrollView, TouchableOpacity, Text, Image } from 'react-native';
import Pushwoosh from 'pushwoosh-react-native-plugin';
import { pushText, subscribeToEvents } from './eventLog';

// testID names the button; its text fields are `${testID}-input` and `${testID}-input-2`.
const ButtonWithTextInput = ({ testID, buttonText, onPress, placeholder1, placeholder2 }) => {
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
                testID={testID}
            >
                <Text style={styles.buttonText}>{buttonText}</Text>
            </TouchableOpacity>
            {placeholder1 && (
                <TextInput
                    style={[styles.textField, { flex: 1, marginRight: 5 }]}
                    placeholder={placeholder1}
                    value={inputValue1}
                    onChangeText={text => setInputValue1(text)}
                    testID={`${testID}-input`}
                />
            )}
            {placeholder2 && (
                <TextInput
                    style={[styles.textField, { flex: 1 }]}
                    placeholder={placeholder2}
                    value={inputValue2}
                    onChangeText={text => setInputValue2(text)}
                    testID={`${testID}-input-2`}
                />
            )}
        </View>
    );
};

const Button = ({ testID, buttonText, onPress }) => {
    return (
        <View style={styles.header}>
            <TouchableOpacity
                style={[styles.button, { flex: 1 }]}
                onPress={onPress}
                testID={testID}
            >
                <Text style={styles.buttonText}>{buttonText}</Text>
            </TouchableOpacity>
        </View>
    );
};

const Actions = () => {
    // What the last action returned, and the push events the plugin delivered so far.
    const [result, setResult] = useState('');
    const [events, setEvents] = useState([]);

    useEffect(() => subscribeToEvents(setEvents), []);

    const report = (text) => {
        console.log(text);
        setResult(text);
    };

    return (
        <View style={styles.screen}>
            {/* Stays above the buttons so what an action did is visible without scrolling. */}
            <View style={styles.panel}>
                <View style={styles.panelHeader}>
                    <Image
                        source={require('./logos.png')}
                        style={styles.image}
                    />
                    <View style={styles.panelBody}>
                        <Text style={styles.panelTitle}>Result</Text>
                        <Text style={styles.panelText} testID="result">{result}</Text>
                    </View>
                </View>
                <Text style={styles.panelTitle}>Push events</Text>
                {events.length === 0 && (
                    <Text style={styles.panelText} testID="event-log-empty">No push events yet</Text>
                )}
                {events.slice(0, 3).map((event, index) => (
                    <Text key={`${event.at}-${index}`} style={styles.panelText} testID={`event-${index}`}>
                        {`${event.name}: ${pushText(event.payload)}`}
                    </Text>
                ))}
            </View>
        {/* "handled": with the keyboard up, a tap on a button presses it instead of only closing the keyboard. */}
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
            <ButtonWithTextInput testID="set-user-id" buttonText="SET USER ID" onPress={(inputValue1) => {
                // An empty user id is not one, and neither native SDK says so: iOS overwrites the id
                // on the server with it and reports success, Android returns without calling back at
                // all. An app should keep such a value away from the SDK, so the demo does.
                if (!inputValue1.trim()) {
                    report('User ID must not be empty');
                    return;
                }
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
                        report(`User ID set: ${inputValue1}`);
                    },
                    (error) => {
                        report(`Failed to set User ID: ${error}`);
                    }
                );
             }} placeholder1="USER ID" />

            <ButtonWithTextInput testID="post-event" buttonText="POST EVENT" onPress={(inputValue1) => {
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
                report(`Event posted: ${inputValue1}`);
            }} placeholder1="EVENT NAME" />

            <ButtonWithTextInput testID="set-language" buttonText="SET LANGUAGE" onPress={(inputValue1) => {
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
                report(`Language set: ${inputValue1}`);
            }} placeholder1="en" />

            <ButtonWithTextInput testID="set-tags" buttonText="SET TAGS" onPress={(inputValue1, inputValue2) => {
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
                if (!inputValue1) {
                    report('SET TAGS: enter a tag name in the KEY field');
                    return;
                }

                Pushwoosh.setTags({ [inputValue1]: inputValue2 },
                () => {
                    report(`Tags set: ${inputValue1} = ${inputValue2}`);
                },
                (error) => {
                    report(`Failed to set tags: ${error}`);
                });
            }} placeholder1="KEY" placeholder2="VALUE" />

            <ButtonWithTextInput testID="user-emails" buttonText="USER EMAILS" onPress={(inputValue1, inputValue2) => {
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
                        report(`User emails set: ${inputValue1} / ${inputValue2}`);
                    },
                    (error) => {
                        report(`Failed to set user emails: ${error}`);
                    }
                );
            }} placeholder1="USER" placeholder2="EMAILS" />
            <ButtonWithTextInput testID="set-emails" buttonText="SET EMAILS" onPress={(inputValue1) => {
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
                        report(`Emails set: ${inputValue1}`);
                    },
                    (error) => {
                        report(`Failed to set emails: ${error}`);
                    }
                );
            }} placeholder1="EMAILS" />

            <Button testID="get-hwid" buttonText="GET HWID" onPress={() => {
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
                    report(`HWID: ${hwid}`);
                });
            }}/>

            <Button testID="get-push-token" buttonText="GET PUSH TOKEN" onPress={() => {
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
                    report(`Push token: ${token || 'none'}`);
                });
            }}/>
            <Button testID="get-user-id" buttonText="GET USER ID" onPress={() => {
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
                    report(`User ID: ${userId}`);
                });
            }}/>
            <Button testID="get-tags" buttonText="GET TAGS" onPress={() => {
                /**
                * Tags the device is currently associated with.
                *
                * PUSHWOOSH CODE
                *    |   |
                *   _|   |_
                *   \     /
                *    \   /
                *     \_/
                */
                Pushwoosh.getTags((tags) => {
                    report(`Tags: ${JSON.stringify(tags)}`);
                },
                (error) => {
                    report(`Failed to get tags: ${error}`);
                });
            }}/>
            <Button testID="show-inbox" buttonText="SHOW INBOX" onPress={() => {
                /**
                * Opens the Message Inbox screen with the messages delivered to this device.
                *
                * PUSHWOOSH CODE
                *    |   |
                *   _|   |_
                *   \     /
                *    \   /
                *     \_/
                */
                Pushwoosh.presentInboxUI();
            }}/>
            <Button testID="show-push-alert"
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
                        report(willShow ? 'Push notification alert enabled' : 'Push notification alert disabled');
                    });
                }}
            />
            <ButtonWithTextInput testID="register-sms" buttonText="REGISTER SMS NUMBER" onPress={(phoneNumber) => {
                /**
                * REGISTER SMS NUMBER
                * Registers phone number associated to the current user.
                * SMS numbers must be in E.164 format (e.g., "+1234567890") and be valid.
                *
                * PUSHWOOSH CODE
                *    |   |
                *   _|   |_
                *   \     /
                *    \   /
                *     \_/
                */
                Pushwoosh.registerSMSNumber(phoneNumber);
                report(`SMS number registration sent: ${phoneNumber}`);
            }} placeholder1="+1234567890" />

            <ButtonWithTextInput testID="register-whatsapp" buttonText="REGISTER WHATSAPP NUMBER" onPress={(phoneNumber) => {
                /**
                * REGISTER WHATSAPP NUMBER
                * Registers WhatsApp number associated to the current user.
                * WhatsApp numbers must be in E.164 format (e.g., "+1234567890") and be valid
                *
                * PUSHWOOSH CODE
                *    |   |
                *   _|   |_
                *   \     /
                *    \   /
                *     \_/
                */
                Pushwoosh.registerWhatsappNumber(phoneNumber);
                report(`WhatsApp number registration sent: ${phoneNumber}`);
            }} placeholder1="+1234567890" />
        </ScrollView>
        </View>
    );
};

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: '#ffffff',
    },
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
    image: {
        width: 50,
        height: 50,
        borderRadius: 12,
        marginRight: 10,
    },
    panel: {
        marginHorizontal: 20,
        marginTop: 10,
        marginBottom: 4,
        padding: 10,
        borderWidth: 1,
        borderColor: '#cccccc',
        borderRadius: 5,
    },
    panelHeader: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    panelBody: {
        flex: 1,
    },
    panelTitle: {
        fontWeight: 'bold',
        color: '#800080',
        marginTop: 4,
    },
    panelText: {
        color: '#000000',
        paddingVertical: 2,
    },
});

export default Actions;
