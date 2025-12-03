import axios, {AxiosInstance} from "axios";

const rawApi = axios.create({
    baseURL: process.env.BACK_URL,
    headers: {
        "Content-Type": "application/json",
    },
});

export async function login(): Promise<string> {
    const res = await rawApi.post("/login", {
        username: "demo",
        password: "demo",
    });
    return res.data.token;
}

export async function fetchMe(api: AxiosInstance) {
    const res = await api.get("/me");
    return res.data;
}
