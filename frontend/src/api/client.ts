import axios from "axios";
import {loadToken} from "@/src/storage/token";

let sessionToken: string | null = null;

export function setSessionToken(token: string | null) {
    sessionToken = token;
}

export const apiClient = axios.create({
    baseURL: "http://178.16.131.84:30081",
    headers: {
        "Content-Type": "application/json",
    },
});

apiClient.interceptors.request.use(async (config) => {
    if (!sessionToken) {
        sessionToken = await loadToken();
    }

    if (sessionToken) {
        config.headers.Cookie = `SESSION=${sessionToken}`
        config.headers.Authorization = `Bearer ${sessionToken}`;
    }

    return config;
});