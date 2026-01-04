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
import AccountIn from "@/src/types/AccountIn";
import {createAccount, deleteAccount, updateAccount} from "@/src/api/accounts";
import {useSelectedAccountStore} from "@/src/stores/selected-account-store";




export default function a() {
    const surfaceColor = useThemeColor({}, 'surface');
    const backgroundColor = useThemeColor({}, 'background');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');
    const selectedAccount = useSelectedAccountStore(s => s.selected)
    const setSelectedAccount = useSelectedAccountStore(s => s.setSelected)

    const [title, setTitle] = useState(selectedAccount?.name || "");


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
                        deleteAccount(selectedAccount.id)
                        router.navigate("/(app)");
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
                      const account = new AccountIn(
                          title
                      )
                      try {
                          if (selectedAccount) {
                              await updateAccount(selectedAccount.id, account)
                          } else {
                              await createAccount(account)
                          }
                          router.back();
                      } catch (e) {
                          console.error("Expense creation failed", e);
                      }

                  }}
              >
                    <Text
                        style={{ color: textOnSurfaceColor, textAlign: 'center', justifyContent: 'center', fontSize: 17, fontWeight: 'bold' }}
                  >Save</Text>
              </Pressable>
            {selectedAccount &&
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
