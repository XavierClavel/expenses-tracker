import axios from "axios";

export function createApiClient(token: string) {
    return axios.create({
        baseURL: "http://178.16.131.84:30081",
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        },
        withCredentials: false,
    });
}
