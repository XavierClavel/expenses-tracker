import { createApiClient } from "../api/client";
import { fetchMe, login } from "../api/auth";
import { loadToken, saveToken, clearToken } from "../storage/token";
import axios, {AxiosInstance} from "axios";

export type BootstrapResult = {
    api: AxiosInstance;
    me: any;
};

export async function bootstrapAuth(): Promise<BootstrapResult> {
    let token = await loadToken();

    if (token) {
        try {
            const api = createApiClient(token);
            const me = await fetchMe(api);
            return { api, me };
        } catch (err) {
            console.warn("Stored token invalid, retrying login...");
            await clearToken();
        }
    }

    const newToken = await login();
    await saveToken(newToken);

    const api = createApiClient(newToken);
    const me = await fetchMe(api);

    return { api, me };
}
