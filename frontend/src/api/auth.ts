import axios, {AxiosInstance} from "axios";
import {saveToken} from "@/src/storage/token";
import {useNavigation} from "expo-router";
import {apiClient} from "@/src/api/client";

export async function login(username: string, password: string): Promise<void> {
    const result = await apiClient.post("/auth/login", {}, {
        auth: {
            username: username,
            password: password,
        }
    });
    console.log(result)
    await saveToken(result.data.token)
}

export async function fetchMe() {
    const res = await apiClient.get("/me");
    console.log("User is logged in")
    return res.data;
}
