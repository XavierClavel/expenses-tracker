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
        config.headers.Authorization = `Bearer ${sessionToken}`;
    }

    console.log("Sending request to", config.url  ,"with token", sessionToken, "body", config.data)

    return config;
});

apiClient.interceptors.response.use(
    function (response){
        console.log("Request to", response.request.url, "was successful")
        return response
    },
    function (error) {
        console.log("Request to", error.response.url, "failed with status", error.response.status, error.response.data)
        return Promise.reject(error)
    }
)