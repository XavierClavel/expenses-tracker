import {FlatList, Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import { PropsWithChildren, SetStateAction, useState} from 'react';
import {CategoryReport} from "@/components/category/category-report";
import {useTheme} from "@react-navigation/core";
import {useThemeColor} from "@/hooks/use-theme-color";
import {colors} from "@/constants/colors";
import {with2Decimals, withNoDecimals, withReadableThousands} from "@/src/utils/math";
import {TouchableRipple} from "react-native-paper";
import {useSelectedCategoryStore} from "@/src/stores/selected-category-store";
import {useCategoriesStore} from "@/src/stores/categories-store";
import {useSummaryStore} from "@/src/stores/summary-store";
import {SafeAreaView} from "react-native-safe-area-context";
import DateScroller from "@/components/date-scroller";
import {router} from "expo-router";
import {useSummaryDateStore} from "@/src/stores/sumary-date-store";






export default function SummarySubcategories() {
    const [focusedItem, setFocusedItem] = useState(0)

    const backgroundColor = useThemeColor({}, 'background');
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnBackgroundColor = useThemeColor({}, 'textOnBackground');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');
    const categoryStore = useCategoriesStore()
    const selectedCategory = useSelectedCategoryStore(s => s.selected)
    const summary = useSummaryStore(s => s.selected)
    const setSummarySubcategory = useSummaryStore(s => s.setSubcategory)
    const data = summary?.expensesByCategory.concat(summary?.incomeByCategory)
        .filter((it) => categoryStore.getParent(it.categoryId)?.id == selectedCategory.id)
        .map(it => {return {
            label: it.categoryName,
            value: it.total,
            color: selectedCategory?.color,
            icon: categoryStore.getSubcategory(it.categoryId)?.icon,
            id: it.categoryId,
        }})
        .sort((function(a, b) {
            return b.value - a.value;
        }))
    const total = Number(summary?.totalExpenses)
    console.log(data)


    return <SafeAreaView style={{backgroundColor: backgroundColor, height: "100%"}}>
        <DateScroller />
    <View
        style={{
            padding: 16,
        }}>
        <FlatList
            style={{ width: "100%" }}
            data={data}
            keyExtractor={(item) => item.id.toString()}
            renderItem={({ item }) => {
                return (
                <TouchableRipple style={{width: "100%"}}>
                    <Pressable
                    onPress={() => {
                        setSummarySubcategory(item.id)
                        router.navigate("analytics/expenses")
                    }}
                    >
                        <CategoryReport item={item} percent={Number(item.value) / total * 100} />
                    </Pressable>
                </TouchableRipple>
            )}} />
    </View>
    </SafeAreaView>
}