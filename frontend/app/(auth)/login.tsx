import { Link } from 'expo-router';
import {StyleSheet, Text} from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import {TextInput} from "react-native-paper";
import {useState} from "react";
import { Button } from 'react-native-paper';
import {useThemeColor} from "@/hooks/use-theme-color";

export default function ModalScreen() {
    const surfaceColor = useThemeColor({}, 'surface');

    const [mail, setMail] = useState("");
    const [password, setPassword] = useState("");

    return (
        <ThemedView style={styles.container}>
            <ThemedText type="title">Login</ThemedText>
            <TextInput
                style={{
                    width: "100%",
                    marginVertical: 5,
                    backgroundColor: surfaceColor,
                    color: 'white'
                }}
                textColor='white'
                underlineColor='white'
                cursorColor='white'
                placeholderTextColor='white'
                selectionColor='orange'
                activeUnderlineColor='orange'

                label={<Text style={{color: 'white'}}>Email address</Text>}
                value={mail}
                onChangeText={text => setMail(text)}
            />
            <TextInput
                style={{
                    width: "100%",
                    marginVertical: 5,
                    backgroundColor: surfaceColor,
                    color: 'white'
                }}
                textColor='white'
                underlineColor='white'
                cursorColor='white'
                placeholderTextColor='white'
                selectionColor='orange'
                activeUnderlineColor='orange'

                label={<Text style={{color: 'white'}}>Password</Text>}
                value={password}
                onChangeText={text => setPassword(text)}
            />
            <Button
                style={{
                    width: "100%",
                    paddingVertical: 5,
                    backgroundColor: surfaceColor,
                }}
                textColor='white'
            >Log in</Button>
        </ThemedView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20,
    },
    link: {
        marginTop: 15,
        paddingVertical: 15,
    },
});
