import { Stack } from "expo-router";
import { useEffect, useState } from "react";
import { loadToken, clearToken } from "@/src/storage/token";
import { createApiClient } from "@/src/api/client";
import { fetchMe } from "@/src/api/auth";
import * as SplashScreen from "expo-splash-screen";

SplashScreen.preventAutoHideAsync();

async function isAuthenticated(): Promise<boolean> {
    let token = await loadToken();

    if (!token) return false;

    try {
        const api = createApiClient(token);
        await fetchMe(api);
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
            const result = await isAuthenticated();
            setAuthenticated(result);
            await SplashScreen.hideAsync();
        }

        bootstrap();
    }, []);

    if (authenticated === null) return null;

    return (
        <Stack screenOptions={{ headerShown: false }}>
            <Stack.Screen name={authenticated ? "(app)" : "(auth)"} />
        </Stack>
    );
}
