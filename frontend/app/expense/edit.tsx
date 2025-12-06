import { Image } from 'expo-image';
import {Pressable, Platform, StyleSheet, Text, View} from 'react-native';

import { HelloWave } from '@/components/hello-wave';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import {Expense} from "@/components/expense";
import {FAB, TextInput} from "react-native-paper";
import {useCallback, useEffect, useState} from "react";
import {DatePickerInput, DatePickerModal} from "react-native-paper-dates";
import {SafeAreaView} from "react-native-safe-area-context";
import {useThemeColor} from "@/hooks/use-theme-color";
import {router, useFocusEffect, useNavigation, useSegments} from "expo-router";
import {usePickerStore} from "@/src/stores/category-picker-store";
import {CategoryDisplay} from "@/components/category/categoryDisplay";
import Category from "@/src/types/Category";




export default function a() {
    const segments = useSegments();

    const [selected, setSelected] = useState<string | null>(null);
    const navigation = useNavigation();
    const surfaceColor = useThemeColor({}, 'surface');
    const backgroundColor = useThemeColor({}, 'background');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');

    const [title, setTitle] = useState("");
    const [amount, setAmount] = useState("");
    const [date, setDate] = useState(new Date());
    const [open, setOpen] = useState(false);
    const pickedCategory = usePickerStore((s) => s.selected);
    const setPickedCategory = usePickerStore((s) => s.setSelected)

    useEffect(() => {
        return () => {
            setPickedCategory(null);
        };
    }, []);

    const onDismissSingle = useCallback(() => {
        setOpen(false);
    }, [setOpen]);

    const onConfirmSingle = useCallback(
        (params) => {
            setOpen(false);
            setDate(params.date);
        },
        [setOpen, setDate]
    );
  return (
      <View style={{ flex: 1}}>
      <ParallaxScrollView
      headerBackgroundColor={{ light: '#A1CEDC', dark: '#1D3D47' }}
      headerImage={
        <Image
          source={require('@/assets/images/partial-react-logo.png')}
        />
      }>
        <SafeAreaView
            style={{
                flex: 1,
                flexDirection: 'column',
                alignItems: 'center',
                width: "100%",
                padding: 20,
                justifyContent: "space-around",
            }}>
            <TextInput
                style={{
                    width: "100%",
                    marginVertical: 5,
                    backgroundColor: surfaceColor,
                    color: 'white',
                    borderRadius: 15,
            }}
                theme={{ colors: { primary: 'transparent'}}}
                mode='outlined'
                textColor={textOnSurfaceColor}
                selectionColor='darkgray'
                outlineColor={backgroundColor}
                cursorColor='white'
                placeholderTextColor={textOnSurfaceColor}
                label={<Text style={{color: 'white'}}>Title</Text>}
                value={title}
                onChangeText={text => setTitle(text)}
            />
            <TextInput
                style={{
                    width: "100%",
                    marginVertical: 5,
                    backgroundColor: surfaceColor,
                    color: 'white',
                    borderRadius: 15,
                }}
                theme={{ colors: { primary: 'transparent'}}}
                mode='outlined'
                textColor={textOnSurfaceColor}
                selectionColor='darkgray'
                outlineColor={backgroundColor}
                cursorColor='white'
                placeholderTextColor={textOnSurfaceColor}
                label={<Text style={{color: 'white'}}>Amount</Text>}
                value={amount}
                onChangeText={text => setAmount(text)}
                inputMode='decimal'
            />
                <DatePickerInput
                    style={{
                        width: "100%",
                        marginVertical: 5,
                        backgroundColor: surfaceColor,
                        color: 'white',
                        borderRadius: 15,
                    }}
                    theme={{ colors: { primary: 'transparent', onSurfaceVariant: 'white',}}}
                    mode='outlined'
                    textColor={textOnSurfaceColor}
                    selectionColor='darkgray'
                    outlineColor={backgroundColor}
                    cursorColor='white'
                    placeholderTextColor={textOnSurfaceColor}
                    iconColor = 'white'
                    label='Date'
                    locale="en"
                    value={date}
                    onChange={(d) => setDate(d)}
                    inputMode="start"
                />
             <Pressable
                 style={{
                     marginVertical: 5,
                 }}
                  onPress={() => {
                      router.navigate("category/picker");
                  }}
              >
                  <CategoryDisplay data={ pickedCategory || new Category(-1, "No category selected", 'lightgray', '')}></CategoryDisplay>
              </Pressable>

              <Pressable
                  style={{
                      width: "100%",
                      borderRadius: 8,
                      height: 50,
                      backgroundColor: surfaceColor,
                      justifyContent: 'center',
                      marginVertical: 5,
                  }}
                  onPress={() => {
                      router.navigate("expenses");
                  }}
              >
                    <Text
                        style={{ color: textOnSurfaceColor, textAlign: 'center', justifyContent: 'center', fontSize: 17, fontWeight: 'bold' }}
                  >Save</Text>
              </Pressable>
        </SafeAreaView>

      </ParallaxScrollView>
      </View>
  );
}
