import { useEffect, useState } from "react";
import { Text, View } from "react-native";
import { bootstrapAuth } from "./src/auth/bootstrap";
import axios, {AxiosInstance} from "axios";

export default function App() {
    const [api, setApi] = useState<AxiosInstance | null>(null);
    const [me, setMe] = useState<any>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function init() {
            try {
                const result = await bootstrapAuth();
                setApi(result.api);
                setMe(result.me);
            } catch (e) {
                console.error("Auth bootstrap failed:", e);
            } finally {
                setLoading(false);
            }
        }

        init();
    }, []);

    if (loading) return <Text>Loading...</Text>;

    if (!me) return <Text>Not logged in</Text>;

    return (
        <View>
            <Text>Welcome {me.username}</Text>
        </View>
    );
}

