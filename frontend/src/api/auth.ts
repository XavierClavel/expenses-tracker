import axios, {AxiosInstance} from "axios";
import {saveToken} from "@/src/storage/token";
import {useNavigation} from "expo-router";


const rawApi = axios.create({
    baseURL: "http://178.16.131.84:30081",
    headers: {
        "Content-Type": "application/json",
    },
});

export async function login(username: string, password: string): Promise<void> {
    const result = await rawApi.post("/auth/login", {}, {
        auth: {
            username: username,
            password: password,
        }
    });
    console.log(result)
    await saveToken(result.data.token)
}

export async function fetchMe(api: AxiosInstance) {
    const res = await api.get("/me");
    return res.data;
}
