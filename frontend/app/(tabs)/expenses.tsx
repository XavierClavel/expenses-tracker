import { Image } from 'expo-image';
import {Button, Pressable, Platform, StyleSheet, Text, View} from 'react-native';

import { HelloWave } from '@/components/hello-wave';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import {Expense} from "@/components/expense";

const data = [
    {value: -900.97, label: 'Accomodation & charges', color: '#009FFF', icon: 'house'},
    {value: -736.14, label: 'Leisure', color: '#93FCF8', icon: 'train'},
    {value: -268.40, label: 'Food', color: '#BDB2FA', icon: 'groceries'},
    {value: -193.38, label: 'Shopping', color: '#FFA5BA', icon: 'trip'},
    {value: -13.38, label: 'Video games', color: '#e1d481', icon: 'video-games'},
    {value: -593.78, label: 'School', color: '#b4f1a7', icon: 'school'},
    {value: -593.78, label: 'Restaurant', color: '#e193d9', icon: 'restaurant'},
    {value: -593.78, label: 'Restaurant', color: '#efffa5', icon: 'plane'},
    {value: -593.78, label: 'Restaurant', color: '#bcb8a5', icon: 'car'},
    {value: -593.78, label: 'Restaurant', color: '#FFA5BA', icon: 'baby'},
];



export default function HomeScreen() {
  return (

    <ParallaxScrollView
      headerBackgroundColor={{ light: '#A1CEDC', dark: '#1D3D47' }}
      headerImage={
        <Image
          source={require('@/assets/images/partial-react-logo.png')}
          style={styles.reactLogo}
        />
      }>
        <View
            style={{
                flex: 1,
                flexDirection: 'column',
                alignItems: 'center',
                width: "100%",
                padding: 10,
                marginTop: 70,
                justifyContent: "space-around"
            }}>
            {data.map((item, index) => (
                <Expense data={item} />
            ))}
        </View>
    </ParallaxScrollView>
  );
}

const styles = StyleSheet.create({
  titleContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  stepContainer: {
    gap: 8,
    marginBottom: 8,
  },
  reactLogo: {
    height: 178,
    width: 290,
    bottom: 0,
    left: 0,
    position: 'absolute',
  },
});
