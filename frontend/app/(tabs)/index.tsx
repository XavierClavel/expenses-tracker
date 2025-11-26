import { Image } from 'expo-image';
import {Button, Pressable, Platform, StyleSheet, Text, View} from 'react-native';

import { HelloWave } from '@/components/hello-wave';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Link } from 'expo-router';
import { CustomPieChart } from '@/components/custom-pie-chart';
import {data} from "browserslist";

const pieData = [
    {value: -900.97, label: 'Accomodation & charges', color: '#009FFF', icon: 'house'},
    {value: -736.14, label: 'Leisure', color: '#93FCF8', icon: 'video-games'},
    {value: -268.40, label: 'Food', color: '#BDB2FA', icon: 'groceries'},
    {value: -193.38, label: 'Shopping', color: '#FFA5BA', icon: 'clothes'},
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
                flexDirection: 'row',
                alignItems: 'center',
                width: "100%",
                padding: 10,
                marginTop: 70,
                justifyContent: "space-around"
            }}>
            <Pressable style={{ paddingVertical: 10, width:150, backgroundColor: '#34448B', borderRadius: 8 }}>
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>-2296,99€</Text>
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 12, paddingTop: 5  }}>Expenses</Text>
            </Pressable>
            <Pressable style={{ paddingVertical: 10, width:150, paddingHorizontal: 20, backgroundColor: '#34448B', borderRadius: 8 }}>
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>3067,63€</Text>
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 12, paddingTop: 5 }}>Income</Text>
            </Pressable>

        </View>
        <CustomPieChart data={pieData}/>
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
