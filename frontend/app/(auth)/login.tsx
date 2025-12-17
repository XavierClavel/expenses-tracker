import {Link, router, useNavigation} from 'expo-router';
import {StyleSheet, Text} from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import {TextInput} from "react-native-paper";
import {useState} from "react";
import { Button } from 'react-native-paper';
import {useThemeColor} from "@/hooks/use-theme-color";
import {login} from "@/src/api/auth";
import {navigate} from "expo-router/build/global-state/routing";
import {apiClient} from "@/src/api/client";
import * as AuthSession from 'expo-auth-session';
import * as WebBrowser from 'expo-web-browser';

export default function ModalScreen() {
    const navigation = useNavigation();
    const surfaceColor = useThemeColor({}, 'surface');

    const [mail, setMail] = useState("");
    const [password, setPassword] = useState("");

    const redirectUri = AuthSession.makeRedirectUri({
        scheme: 'expenses-tracker',
        path: 'home',
    });

    const authUrl =
        `http://178.16.131.84:30081/auth/login-oauth-google` +
        `?redirect_uri=${encodeURIComponent(redirectUri)}`;

    return (
        <ThemedView style={styles.container}>
            <ThemedText type="title">Login</ThemedText>
            {/*<Button*/}
            {/*    style={{*/}
            {/*        width: "100%",*/}
            {/*        paddingVertical: 5,*/}
            {/*        backgroundColor: surfaceColor,*/}
            {/*    }}*/}
            {/*    textColor='white'*/}
            {/*    onPress={async () => {*/}
            {/*        try {*/}
            {/*            const result = await WebBrowser.openAuthSessionAsync(*/}
            {/*                authUrl,*/}
            {/*                redirectUri*/}
            {/*            );*/}

            {/*            if (result.type === 'success') {*/}
            {/*                const url = result.url;*/}
            {/*                console.log(url);*/}
            {/*            }*/}
            {/*        } catch (e) {*/}
            {/*            console.error("Login failed", e);*/}
            {/*        }*/}
            {/*    }}*/}
            {/*/!*>OAuth</Button>*!/*/}
            <TextInput
                style={{
                    width: "100%",
                    marginVertical: 5,
                    backgroundColor: surfaceColor,
                    color: 'white'
                }}
                textColor='white'
                underlineColor='white'
                cursorColor='white'
                placeholderTextColor='white'
                selectionColor='orange'
                activeUnderlineColor='orange'

                label={<Text style={{color: 'white'}}>Email address</Text>}
                value={mail}
                onChangeText={text => setMail(text)}
            />
            <TextInput
                style={{
                    width: "100%",
                    marginVertical: 5,
                    backgroundColor: surfaceColor,
                    color: 'white'
                }}
                textColor='white'
                underlineColor='white'
                cursorColor='white'
                placeholderTextColor='white'
                selectionColor='orange'
                activeUnderlineColor='orange'

                label={<Text style={{color: 'white'}}>Password</Text>}
                value={password}
                onChangeText={text => setPassword(text)}
            />
            <Button
                style={{
                    width: "100%",
                    paddingVertical: 5,
                    backgroundColor: surfaceColor,
                }}
                textColor='white'
                onPress={async () => {
                    try {
                        await login(mail, password);
                        router.replace("/(app)/index");
                    } catch (e) {
                        console.error("Login failed", e);
                    }
                }}
            >Log in</Button>
            <Button
                style={{
                    width: "100%",
                    paddingVertical: 5,
                    backgroundColor: surfaceColor,
                }}
                textColor='white'
                onPress={async () => {
                    router.replace("signup");
                }}
            >Sign up</Button>
        </ThemedView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20,
    },
    link: {
        marginTop: 15,
        paddingVertical: 15,
    },
});
