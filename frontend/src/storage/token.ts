import * as SecureStore from "expo-secure-store";

const TOKEN_KEY = "sessionToken";

export async function saveToken(token: string) {
    await SecureStore.setItemAsync(TOKEN_KEY, token);
}

export async function loadToken(): Promise<string | null> {
    return SecureStore.getItemAsync(TOKEN_KEY);
}

export async function clearToken() {
    await SecureStore.deleteItemAsync(TOKEN_KEY);
}
