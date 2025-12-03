import axios from "axios";

export function createApiClient(token: string) {
    return axios.create({
        baseURL: process.env.BACK_URL,
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        },
        withCredentials: false,
    });
}
