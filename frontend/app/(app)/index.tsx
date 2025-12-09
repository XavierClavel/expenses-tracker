import { Image } from 'expo-image';
import {Button, Pressable, Platform, StyleSheet, Text, View} from 'react-native';

import { HelloWave } from '@/components/hello-wave';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Link } from 'expo-router';
import { CustomPieChart } from '@/components/custom-pie-chart';
import {data} from "browserslist";
import {useThemeColor} from "@/hooks/use-theme-color";
import {useEffect, useState} from "react";
import {getYearSummary} from "@/src/api/summary";
import {useSummaryStore} from "@/src/stores/summary-store";
import {useCategoriesStore} from "@/src/stores/categories-store";
import {colors} from "@/constants/colors";

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
    const [data, setData] = useState([])
    const categoryStore = useCategoriesStore()

    const loadSummary = async () => {
        const summary = await getYearSummary(2025)
        summaryStore.setSelected(summary)
        const subcategories = summary.expensesByCategory
        const categories = categoryStore.selected
            .filter((it) => it.type == 'EXPENSE')
        const result = categories.map((c) => {
            return {
                value: subcategories
                    .filter((it) => categoryStore.getParent(it.categoryId)?.id == c.id)
                    .reduce((accumulator, object) => {
                    return accumulator + Number(object.total);
                }, 0),
                label: c.name,
                color: colors[c.color || 'unknown'],
                icon: c.icon,
            }
        })
        setData(result)
    }

    useEffect(() => {
        loadSummary()
    }, []);

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
            <Pressable style={{ paddingVertical: 10, width:150, backgroundColor: surfaceColor, borderRadius: 8 }}>
                <Text style={{ color: textOnSurfaceColor, textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>- {summaryStore.selected?.totalExpenses}€ </Text>
                <Text style={{ color: textOnSurfaceColor, textAlign: 'center', fontSize: 12, paddingTop: 5  }}>Expenses</Text>
            </Pressable>
            <Pressable style={{ paddingVertical: 10, width:150, paddingHorizontal: 20, backgroundColor: surfaceColor, borderRadius: 8 }}>
                <Text style={{ color: textOnSurfaceColor, textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>{summaryStore.selected?.totalIncome}€</Text>
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
