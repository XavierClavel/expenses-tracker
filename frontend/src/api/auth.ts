import axios, {AxiosInstance} from "axios";
import {saveToken} from "@/src/storage/token";
import {useNavigation} from "expo-router";
import {apiClient, setSessionToken} from "@/src/api/client";

export async function login(username: string, password: string): Promise<void> {
    const result = await apiClient.post("/auth/login", {}, {
        auth: {
            username: username,
            password: password,
        }
    });
    console.log("Login successful, received token", result.data.token)
    setSessionToken(result.data.token)
    await saveToken(result.data.token)
}


export async function signup(username: string, password: string): Promise<void> {
    const result = await apiClient.post("/auth/signup", {
        username: "user",
        emailAddress: username,
        password: password
    });
}

export async function fetchMe() {
    const res = await apiClient.get("/auth/me");
    console.log("User is already logged in")
    return res.data;
}
