import { Image } from 'expo-image';
import {Button, Pressable, Platform, StyleSheet, Text, View} from 'react-native';

import { HelloWave } from '@/components/hello-wave';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import {Link, useFocusEffect} from 'expo-router';
import { CustomPieChart } from '@/components/custom-pie-chart';
import {data} from "browserslist";
import {useThemeColor} from "@/hooks/use-theme-color";
import React, {useEffect, useState} from "react";
import {getMonthSummary, getYearSummary} from "@/src/api/summary";
import {useSummaryStore} from "@/src/stores/summary-store";
import {useCategoriesStore} from "@/src/stores/categories-store";
import {colors} from "@/constants/colors";
import {SegmentedButtons} from "react-native-paper";
import {useSelectedTypeStore} from "@/src/stores/selected-type-store";
import {with2Decimals, withReadableThousands} from "@/src/utils/math";

const pieData = [
    {value: -900.97, label: 'Accomodation & charges', color: '#009FFF', icon: 'house'},
    {value: -736.14, label: 'Leisure', color: '#93FCF8', icon: 'video-games'},
    {value: -268.40, label: 'Food', color: '#BDB2FA', icon: 'groceries'},
    {value: -193.38, label: 'Shopping', color: '#FFA5BA', icon: 'clothes'},
];



export default function HomeScreen() {
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');
    const summaryStore = useSummaryStore()
    const summary = useSummaryStore(state => state.selected);
    const [data, setData] = useState([])
    const categoryStore = useCategoriesStore()
    const selectedTypeStore = useSelectedTypeStore()

    const loadSummary = async () => {
        console.log("load summary")
        const date = new Date()
        const summary = await getMonthSummary(date.getFullYear(), date.getMonth()+1)
        summaryStore.setSelected(summary)
    }

    const selectType = (type) => {
        console.log("selecting")
        selectedTypeStore.setSelected(type)
        let subcategories = []
        let categories = []
        if (type == 'EXPENSE') {
            subcategories = summary?.expensesByCategory || []
            categories = categoryStore.selected.filter((it) => it.type == 'EXPENSE')

        } else {
            subcategories = summary?.incomeByCategory || []
            categories = categoryStore.selected.filter((it) => it.type == 'INCOME')
        }
        const result = categories.map((c) => {
            const total = subcategories
                .filter((it) => categoryStore.getParent(it.categoryId)?.id == c.id)
                .reduce((accumulator, object) => {
                    return accumulator + Number(object.total);
                }, 0)
            return {
                value: with2Decimals(total),
                label: c.name,
                color: c.color,
                icon: c.icon,
            }
        })
        setData(result)
    }

    useEffect(() => {
        syncData()
    }, []);

    async function syncData() {
        if (!summary) {
            await loadSummary()
        }
        selectType('EXPENSE')
    }

    useFocusEffect(

        React.useCallback(() => {
            //syncData()
            // Do something when the screen is focused
            return () => {
                // Do something when the screen is unfocused
                // Useful for cleanup functions
            };
        }, [])
    );

    return (

    <ParallaxScrollView
      headerBackgroundColor={{ light: '#A1CEDC', dark: '#1D3D47' }}
      headerImage={
        <Image
          source={require('@/assets/images/partial-react-logo.png')}
          style={styles.reactLogo}
        />
      }>
        <SegmentedButtons
            style={{
                marginTop: 50,
                padding: 10,
            }}
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
        <View
            style={{
                flex: 1,
                flexDirection: 'row',
                alignItems: 'center',
                width: "100%",
                padding: 10,
                marginTop: 10,
                justifyContent: "space-around"
            }}>
            <Pressable style={{ paddingVertical: 10, width:150, backgroundColor: surfaceColor, borderRadius: 8 }}>
                <Text style={{ color: textOnSurfaceColor, textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>- {withReadableThousands(summary?.totalExpenses)}€ </Text>
                <Text style={{ color: textOnSurfaceColor, textAlign: 'center', fontSize: 12, paddingTop: 5  }}>Expenses</Text>
            </Pressable>
            <Pressable style={{ paddingVertical: 10, width:150, paddingHorizontal: 20, backgroundColor: surfaceColor, borderRadius: 8 }}>
                <Text style={{ color: textOnSurfaceColor, textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>{withReadableThousands(summary?.totalIncome)}€</Text>
                <Text style={{ color: textOnSurfaceColor, textAlign: 'center', fontSize: 12, paddingTop: 5 }}>Income</Text>
            </Pressable>

        </View>
        <CustomPieChart data={data}/>
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

