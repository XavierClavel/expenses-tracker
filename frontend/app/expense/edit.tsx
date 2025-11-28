import { Image } from 'expo-image';
import {Pressable, Platform, StyleSheet, Text, View} from 'react-native';

import { HelloWave } from '@/components/hello-wave';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import {Expense} from "@/components/expense";
import {FAB, TextInput} from "react-native-paper";
import {useState} from "react";




export default function a() {
    const [title, setTitle] = useState("");
    const [amount, setAmount] = useState("");
  return (
      <View style={{ flex: 1}}>
      <ParallaxScrollView
      headerBackgroundColor={{ light: '#A1CEDC', dark: '#1D3D47' }}
      headerImage={
        <Image
          source={require('@/assets/images/partial-react-logo.png')}
        />
      }>
        <View
            style={{
                flex: 1,
                flexDirection: 'column',
                alignItems: 'center',
                width: "100%",
                padding: 20,
                marginTop: 70,
                justifyContent: "space-around"
            }}>
            <TextInput
                style={{width: "100%"}}
                label="Title"
                value={title}
                onChangeText={text => setTitle(text)}
            />
            <TextInput
                style={{width: "100%"}}
                label="Amount"
                value={amount}
                onChangeText={text => setAmount(text)}
                inputMode='decimal'
            />








            ....
        </View>
    </ParallaxScrollView>
      </View>
  );
}
