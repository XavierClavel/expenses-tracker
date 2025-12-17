import { Image } from 'expo-image';
import {Pressable, Platform, StyleSheet, Text, View, Alert, Button} from 'react-native';

import { HelloWave } from '@/components/hello-wave';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import {ExpenseDisplay} from "@/components/expenseDisplay";
import {FAB, SegmentedButtons, TextInput} from "react-native-paper";
import {useCallback, useEffect, useState} from "react";
import {DatePickerInput, DatePickerModal} from "react-native-paper-dates";
import {SafeAreaView} from "react-native-safe-area-context";
import {useThemeColor} from "@/hooks/use-theme-color";
import {router, useFocusEffect, useNavigation, useSegments} from "expo-router";
import {usePickerStore} from "@/src/stores/category-picker-store";
import {CategoryDisplay} from "@/components/category/categoryDisplay";
import CategoryIn from "@/src/types/CategoryIn";
import {createExpense, deleteExpense, updateExpense} from "@/src/api/expenses";
import ExpenseIn from "@/src/types/Expense";
import {login} from "@/src/api/auth";
import {useSelectedExpenseStore} from "@/src/stores/selected-expense-store";
import CategoryOut from "@/src/types/CategoryOut";
import {useSelectedTypeStore} from "@/src/stores/selected-type-store";




export default function a() {
    const segments = useSegments();
    const selectedExpenseStore = useSelectedExpenseStore()
    const selectedTypeStore = useSelectedTypeStore()

    const [selected, setSelected] = useState<string | null>(null);
    const navigation = useNavigation();
    const surfaceColor = useThemeColor({}, 'surface');
    const backgroundColor = useThemeColor({}, 'background');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');

    const [title, setTitle] = useState(selectedExpenseStore.selected?.title || "");
    const [amount, setAmount] = useState(selectedExpenseStore.selected?.amount || "");
    const [date, setDate] = useState(selectedExpenseStore.selected?.date || new Date());
    const [open, setOpen] = useState(false);
    const pickedCategory = usePickerStore((s) => s.selected);
    const setPickedCategory = usePickerStore((s) => s.setSelected)
    const [selectedExpenseCategory, setSelectedExpenseCategory] = useState(selectedTypeStore.selected == 'EXPENSE' ? pickedCategory : null)

    useEffect(() => {
        return () => {
            setPickedCategory(null);
        };
    }, []);

    const selectType = (type) => {
        selectedTypeStore.setSelected(type)
        setPickedCategory(null)
    }

    const confirmDelete = () => {
        Alert.alert(
            'Confirm action',
            'Are you sure you want to delete this item?',
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Delete',
                    style: 'destructive',
                    onPress: () => {
                        deleteExpense(selectedExpenseStore.selected.id)
                        router.replace("/(app)/index");
                    },
                },
            ],
            { cancelable: true }
        );
    };

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
            <SegmentedButtons
                value={selectedTypeStore.selected}
                onValueChange={selectType}
                buttons={[
                    {
                        value: 'EXPENSE',
                        label: 'Expense',
                    },
                    {
                        value: 'INCOME',
                        label: 'Income',
                    },
                ]}
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
                  <CategoryDisplay data={ pickedCategory || new CategoryOut(-1, "No category selected", 'lightgray', 'unknown')}></CategoryDisplay>
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
                  onPress={async () => {
                      const expense = new ExpenseIn(
                          title,
                          amount,
                          "EUR",
                          date.toLocaleDateString('sv-SE'),
                          pickedCategory?.id || null,
                          selectedTypeStore.selected,
                      )
                      try {
                          if (selectedExpenseStore.selected) {
                              await updateExpense(selectedExpenseStore.selected.id, expense)
                          } else {
                              await createExpense(expense)
                          }
                          router.replace("/(app)/index");
                      } catch (e) {
                          console.error("Expense creation failed", e);
                      }

                  }}
              >
                    <Text
                        style={{ color: textOnSurfaceColor, textAlign: 'center', justifyContent: 'center', fontSize: 17, fontWeight: 'bold' }}
                  >Save</Text>
              </Pressable>
            {selectedExpenseStore.selected &&
            <Pressable
                style={{
                    width: "100%",
                    borderRadius: 8,
                    height: 50,
                    backgroundColor: surfaceColor,
                    justifyContent: 'center',
                    marginVertical: 5,
                }}
                onPress={confirmDelete}
            >
                <Text
                    style={{ color: textOnSurfaceColor, textAlign: 'center', justifyContent: 'center', fontSize: 17, fontWeight: 'bold' }}
                >Delete</Text>
            </Pressable>
          }
        </SafeAreaView>

      </ParallaxScrollView>
      </View>
  );
}
