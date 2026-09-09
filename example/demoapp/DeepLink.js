/**
 * @format
 */

import React from 'react';
import {
    ScrollView,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import { parseDeepLink } from './deepLinkUrl';

/**
 * Shows the deep link the app was opened with. The URL comes from index.js, which owns the
 * single Linking subscription and hands it over as a route param.
 *
 * A push whose "l" is an app scheme (pwdemo://demo/screen?from=t1) reaches JS through
 * React Native's Linking. An http/https link does not: the native SDK routes it to the
 * browser or, for a domain the app claims, to the Universal Links handler.
 */
const DeepLink = ({ route }) => {
    const link = route.params?.url;

    if (!link) {
        return (
            <View style={styles.container}>
                <Text style={styles.title}>No deep link yet</Text>
                <Text style={styles.hint}>
                    Send a push with the link pwdemo://demo/screen?from=t1 and tap it.
                </Text>
            </View>
        );
    }

    const parsed = parseDeepLink(link);

    return (
        <ScrollView contentContainerStyle={styles.container}>
            <Text style={styles.title}>Opened with</Text>
            <Text style={styles.url}>{link}</Text>

            {parsed && (
                <View style={styles.table}>
                    <Row label="Scheme" value={parsed.scheme} />
                    <Row label="Host" value={parsed.host} />
                    <Row label="Path" value={parsed.path} />
                    {Object.entries(parsed.params).map(([key, value]) => (
                        <Row key={key} label={key} value={value} />
                    ))}
                </View>
            )}
        </ScrollView>
    );
};

const Row = ({ label, value }) => (
    <View style={styles.row}>
        <Text style={styles.rowLabel}>{label}</Text>
        <Text style={styles.rowValue}>{value}</Text>
    </View>
);

const styles = StyleSheet.create({
    container: {
        flexGrow: 1,
        padding: 20,
    },
    title: {
        fontSize: 20,
        fontWeight: 'bold',
        color: '#6A1B9A',
        marginBottom: 10,
    },
    hint: {
        fontSize: 15,
        color: '#555',
    },
    url: {
        fontSize: 15,
        marginBottom: 20,
    },
    table: {
        borderTopWidth: 1,
        borderTopColor: '#DDD',
    },
    row: {
        flexDirection: 'row',
        paddingVertical: 10,
        borderBottomWidth: 1,
        borderBottomColor: '#DDD',
    },
    rowLabel: {
        flex: 1,
        fontWeight: 'bold',
        color: '#6A1B9A',
    },
    rowValue: {
        flex: 2,
    },
});

export default DeepLink;
