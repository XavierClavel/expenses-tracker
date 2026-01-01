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
import DateScroller from "@/components/date-scroller";
import {useSummaryStore} from "@/src/stores/summary-store";
import {useSummaryDateStore} from "@/src/stores/sumary-date-store";


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
    const summarySubcategory = useSummaryStore(s => s.subcategory)
    const textOnBackgroundColor = useThemeColor({}, 'textOnBackground');
    const summaryMonth = useSummaryDateStore(s => s.month)
    const summaryYear = useSummaryDateStore(s => s.year)
    const timescale = useSummaryDateStore(s => s.timescale)

    const loadExpenses = async (pageToLoad: number) => {
        setLoading(true);
        const from = timescale == "month" ? new Date(Date.UTC(summaryYear, summaryMonth-1, 1)) : new Date(Date.UTC(summaryYear, 0, 1))
        const to = timescale == "month" ? new Date(Date.UTC(summaryYear, summaryMonth, 0)) : new Date(Date.UTC(summaryYear, 12, 0))

        const newExpenses = await listExpenses(pageToLoad, pageSize, summarySubcategory, from, to);

        setExpenses(prev =>
            pageToLoad === 0 ? newExpenses : [...prev, ...newExpenses]
        );        setHasMore(newExpenses.length === pageSize);
        setLoading(false);
    };

    useEffect(() => {
        setExpenses([]);
        setHasMore(true);
        setPage(0);
    }, [summaryMonth, summaryYear]);

    useEffect(() => {
        if (loading) return;
        if (!hasMore && page !== 0) return;

        loadExpenses(page);
    }, [page, summaryMonth, summaryYear]);

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
            <DateScroller />
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
                        }
                    }}
                    onEndReachedThreshold={0.5}
                    ListFooterComponent={loading ? <ActivityIndicator/>: null}
                />
            </View>
        </View>
    );
}
