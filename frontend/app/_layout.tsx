import {Redirect, Stack} from "expo-router";
import { useEffect, useState } from "react";
import { loadToken, clearToken } from "@/src/storage/token";
import { setSessionToken} from "@/src/api/client";
import { fetchMe } from "@/src/api/auth";
import * as SplashScreen from "expo-splash-screen";
import {ThemeProvider} from "@react-navigation/core";
import {DarkTheme} from "@react-navigation/native";
import {SafeAreaProvider, SafeAreaView} from "react-native-safe-area-context";
import {Provider} from "react-native-paper";

SplashScreen.preventAutoHideAsync();

async function isAuthenticated(): Promise<boolean> {
    let token = await loadToken();

    if (!token) {
        console.log("No token found")
        return false;
    }

    setSessionToken(token)
    console.log("Token found", token)

    try {
        await fetchMe();
        return true;
    } catch (err) {
        console.warn("Stored token invalid, retrying login...");
        await clearToken();
        return false;
    }
}

export default function RootLayout() {
    const [authenticated, setAuthenticated] = useState<boolean | null>(null);

    useEffect(() => {
        async function bootstrap() {
            try {
                const result = await isAuthenticated();
                setAuthenticated(result);
            } finally {
                await SplashScreen.hideAsync();
            }
        }

        bootstrap();
    }, []);

    if (authenticated === null) return <Stack screenOptions={{ headerShown: false }} />;

    return (
        <ThemeProvider value={DarkTheme}>
            <Provider>
            <SafeAreaProvider>
                {!authenticated && <Redirect href="/(auth)/login" />}
                {authenticated && <Redirect href="/(app)" />}
                <Stack screenOptions={{ headerShown: false }}>
                </Stack>
            </SafeAreaProvider>
            </Provider>
        </ThemeProvider>
    );
}
