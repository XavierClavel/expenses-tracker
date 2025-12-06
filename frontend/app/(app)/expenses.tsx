import { Image } from 'expo-image';
import {Pressable, Platform, StyleSheet, Text, View} from 'react-native';

import { HelloWave } from '@/components/hello-wave';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import {ExpenseDisplay} from "@/components/expenseDisplay";
import {FAB} from "react-native-paper";
import {router, useNavigation} from "expo-router";
import {useThemeColor} from "@/hooks/use-theme-color";
import {listExpenses} from "@/src/api/expenses";

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
    const navigation = useNavigation();
    listExpenses()
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
                padding: 10,
                marginTop: 70,
                justifyContent: "space-around"
            }}>
            {data.map((item, index) => (
                <Pressable
                    key={index}
                    onPress={() => {
                        router.navigate("expense/edit");
                    }}
                >
                <ExpenseDisplay data={item}/>
                </Pressable>
            ))}
        </View>
    </ParallaxScrollView>
              <FAB
                  icon="plus"
                  style={{ position: 'absolute', bottom: 16, alignSelf: 'center', backgroundColor: 'lightgray' }}
                  onPress={() => {navigation.navigate('expense/edit')}}
              />

      </View>
  );
}
