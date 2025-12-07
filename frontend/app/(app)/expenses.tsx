import { Image } from 'expo-image';
import {Pressable, Platform, StyleSheet, Text, View, FlatList, ActivityIndicator} from 'react-native';

import { HelloWave } from '@/components/hello-wave';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import {ExpenseDisplay} from "@/components/expenseDisplay";
import {FAB} from "react-native-paper";
import {router, useNavigation} from "expo-router";
import {useThemeColor} from "@/hooks/use-theme-color";
import {listExpenses} from "@/src/api/expenses";
import {useEffect, useState} from "react";
import ExpenseOut from "@/src/types/ExpenseOut";
import {SafeAreaView} from "react-native-safe-area-context";
import {useSelectedExpenseStore} from "@/src/stores/selected-expense-store";
import {listCategories} from "@/src/api/categories";
import {useCategoriesStore} from "@/src/stores/categories-store";

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
    const [expenses, setExpenses] = useState<ExpenseOut[]>([]);
    const [page, setPage] = useState(0);
    const [pageSize] = useState(20);
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const backgroundColor = useThemeColor({}, 'background');
    const selectedExpenseStore = useSelectedExpenseStore()
    const categoriesStore = useCategoriesStore()

    const loadExpenses = async (pageToLoad: number) => {
        if (loading || !hasMore) return;

        setLoading(true);

        const newExpenses = await listExpenses(pageToLoad, pageSize);

        setExpenses(prev => [...prev, ...newExpenses]);
        setHasMore(newExpenses.length === pageSize);
        setLoading(false);
    };

    const loadCategories = async () => {
        const categories = await listCategories()
        console.log(categories)
        categoriesStore.setSelected(categories)
    }

    useEffect(() => {
        loadExpenses(0);
        loadCategories()
    }, []);


  return (
      <View style={{ flex: 1, backgroundColor: backgroundColor, paddingTop: 50}}>
        <View
            style={{
                flex: 1,
                flexDirection: 'column',
                alignItems: 'center',
                width: "100%",
                paddingHorizontal: 10,
                justifyContent: "space-around"
            }}>
            <FlatList
                style={{
                    width: "100%",
                }}
                data={expenses}
                keyExtractor={(item) => item.id.toString()}
                renderItem={({ item }) =>
                    <Pressable
                        key={item.id}
                        onPress={() => {
                            selectedExpenseStore.setSelected(item)
                            router.navigate("expense/edit");
                        }}
                    >
                        <ExpenseDisplay data={item}/>
                    </Pressable>}
                onEndReached={() => {
                    console.log("end reached")
                    if (!loading && hasMore) {
                        const next = page + 1;
                        setPage(next);
                        loadExpenses(next);
                    }
                }}
                onEndReachedThreshold={0.5}
                ListFooterComponent={loading ? <ActivityIndicator/>: null}
            />
        </View>
              <FAB
                  icon="plus"
                  style={{ position: 'absolute', bottom: 16, alignSelf: 'center', backgroundColor: 'lightgray' }}
                  onPress={() => {
                      selectedExpenseStore.setSelected(null)
                      navigation.navigate('expense/edit')
                  }}
              />

      </View>
  );
}
