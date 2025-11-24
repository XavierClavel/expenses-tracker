import { Image } from 'expo-image';
import {Button, Pressable, Platform, StyleSheet, Text, View} from 'react-native';

import { HelloWave } from '@/components/hello-wave';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import {Expense} from "@/components/expense";

const data = [
    {value: -900.97, label: 'Accomodation & charges', color: '#009FFF', gradientCenterColor: '#006DFF'},
    {value: -736.14, label: 'Leisure', color: '#93FCF8', gradientCenterColor: '#3BE9DE'},
    {value: -268.40, label: 'Food', color: '#BDB2FA', gradientCenterColor: '#8F80F3'},
    {value: -193.38, label: 'Shopping', color: '#FFA5BA', gradientCenterColor: '#FF7F97'},
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
