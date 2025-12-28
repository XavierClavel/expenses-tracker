import { Image } from 'expo-image';
import {Pressable, Platform, StyleSheet, Text, View, FlatList, ActivityIndicator, SectionList} from 'react-native';

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
import {usePickerStore} from "@/src/stores/category-picker-store";
import {useSelectedTypeStore} from "@/src/stores/selected-type-store";
import {Section} from "@jridgewell/trace-mapping/src/types";
import {useSelectedCategoryStore} from "@/src/stores/selected-category-store";
import {useSelectedSubcategoryStore} from "@/src/stores/selected-subcategory-store";


export default function HomeScreen() {
    const navigation = useNavigation();
    const [expenses, setExpenses] = useState<ExpenseOut[]>([]);
    const [expensesSections, setExpensesSections] = useState<Section<ExpenseOut>[]>([])
    const [page, setPage] = useState(0);
    const [pageSize] = useState(50);
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const backgroundColor = useThemeColor({}, 'background');
    const selectedExpenseStore = useSelectedExpenseStore()
    const selectedTypeStore = useSelectedTypeStore()
    const pickedCategoryStore = usePickerStore()
    const categoriesStore = useCategoriesStore()
    const setSelectedCategory = useSelectedCategoryStore((s) => s.setSelected)
    const setSelectedSubcategory = useSelectedSubcategoryStore((s) => s.setSelected)

    const textOnBackgroundColor = useThemeColor({}, 'textOnBackground');

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
        categoriesStore.setSelected(categories)
    }

    useEffect(() => {
        if (expenses.length > 0) return
        loadExpenses(0);
        loadCategories()
    }, []);

    useEffect(() => {
        console.log("expenses", expenses);

        setExpensesSections(
            expenses.reduce((acc, expense) => {
                const date = new Intl.DateTimeFormat("fr", {
                    day: "numeric",
                    month: "long",
                    year: "numeric",
                }).format(expense.date);

                let section = acc.find(s => s.title === date);

                if (!section) {
                    section = { title: date, data: [] };
                    acc.push(section);
                }

                section.data.push(expense);
                return acc;
            }, [] as { title: string; data: ExpenseOut[] }[])
        );

        console.log("map", expensesSections)
    }, [expenses]);


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
            <SectionList
                style={{
                    width:"100%",

                }}
                sections={expensesSections}
                keyExtractor={(item) => item.id.toString()}
                renderSectionHeader={({ section }) => (
                    <Text style={{ fontWeight: "bold", marginVertical: 8, color:textOnBackgroundColor }}>
                        {section.title}
                    </Text>
                )}
                renderItem={({ item }) => (
                    <Pressable
                        onPress={() => {
                            selectedExpenseStore.setSelected(item);
                            selectedTypeStore.setSelected(item.type);
                            pickedCategoryStore.setSelected(
                                categoriesStore.getSubcategory(item.categoryId)
                            );
                            router.navigate("expense/edit");
                        }}
                    >
                        <ExpenseDisplay data={item} />
                    </Pressable>
                )}
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
                  color={backgroundColor}
                  style={{ position: 'absolute', bottom: 16, alignSelf: 'center', backgroundColor: 'lightgray' }}
                  onPress={() => {
                      selectedExpenseStore.setSelected(null)
                      selectedTypeStore.setSelected("EXPENSE")
                      setSelectedCategory(null)
                      setSelectedSubcategory(null)
                      navigation.navigate('expense/edit')
                  }}
              />

      </View>
  );
}
