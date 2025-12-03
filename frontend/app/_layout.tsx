import { Stack } from "expo-router";


export default function RootLayout() {
    const isAuthenticated = false;

    return (
        <Stack screenOptions={{ headerShown: false }}>
            <Stack.Protected guard={isAuthenticated}>
                <Stack.Screen name="(app)" />
            </Stack.Protected>
            <Stack.Protected guard={!isAuthenticated}>
                <Stack.Screen name="(auth)" />
            </Stack.Protected>
        </Stack>
    );
}