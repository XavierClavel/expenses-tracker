import { Image } from 'expo-image';
import {Platform, Pressable, StyleSheet, Text, View} from 'react-native';

import { Collapsible } from '@/components/ui/collapsible';
import { ExternalLink } from '@/components/external-link';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import { BarChart } from "react-native-gifted-charts";
import {CustomPieChart} from "@/components/custom-pie-chart";
import {CustomBarChart} from "@/components/custom-bar-chart";
import {useThemeColor} from "@/hooks/use-theme-color";
import {getYearSummary} from "@/src/api/summary";
import {
    getMonthCategoryTrends, getMonthSubcategoryTrends,
    getMonthTrends,
    getYearCategoryTrends, getYearFlowTrends,
    getYearSubcategoryTrends,
    getYearTrends
} from "@/src/api/trends";
import {useCategoriesStore} from "@/src/stores/categories-store";
import {useEffect, useState} from "react";
import { ToggleButton } from "react-native-paper";
import {Dropdown } from 'react-native-element-dropdown';
import {useDataTypeStore} from "@/src/stores/data-type-store";
import {data} from "browserslist";
import {useSelectedTypeStore} from "@/src/stores/selected-type-store";
import {time} from "@expo/fingerprint/cli/build/utils/log";
import {useSelectedCategoryStore} from "@/src/stores/selected-category-store";
import {useSelectedSubcategoryStore} from "@/src/stores/selected-subcategory-store";
import {colors} from "@/constants/colors";
import {router} from "expo-router";
import {CategoryDisplay} from "@/components/category/categoryDisplay";
import CategoryOut from "@/src/types/CategoryOut";
import {useBarChartAggregationStore} from "@/src/stores/barchart-aggregation-store";
import {getAccountTrendsMonth, getUserTrendsMonth} from "@/src/api/accounts";
import {useSelectedAccountStore} from "@/src/stores/selected-account-store";


const colorExpense = '#da451a'
const colorIncome = '#71cc5d'

export default function AccountCharts() {
    const backgroundColor = useThemeColor({}, 'background');
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');
    const setTimescale = useDataTypeStore(s => s.setTimescale)

    const selectedAccount = useSelectedAccountStore(s => s.selected)

    const itemsTimescale = [
        { label: 'Month', value: 'month' },
        { label: 'Year', value: 'year' },
    ];

    const [trends, setTrends] = useState([])

    const loadAccountTrendsMonth = async () => {
        const trends = await getAccountTrendsMonth(selectedAccount?.id)
        const result = []
        for (const v of trends) {
            const date = new Date(v.year, v.month - 1)
            const currentDate = new Date()
            const displayDate= date.getFullYear() == currentDate.getFullYear() ?
                date.toLocaleString('default', { month: 'short' })
                : date.toLocaleString('default', { month: 'short', year: 'numeric' })
            const value = Number(v.balance)
            result.push({
                value: value,
                frontColor: value >= 0 ? colorIncome : colorExpense,
                label: displayDate
            })
        }
        setTrends(result)
    }

    const loadUserTrendsMonth = async () => {
        const trends = await getUserTrendsMonth()
        const result = []
        for (const v of trends) {
            const date = new Date(v.year, v.month - 1)
            const currentDate = new Date()
            const displayDate= date.getFullYear() == currentDate.getFullYear() ?
                date.toLocaleString('default', { month: 'short' })
                : date.toLocaleString('default', { month: 'short', year: 'numeric' })
            const value = Number(v.balance)
            result.push({
                value: value,
                frontColor: value >= 0 ? colorIncome : colorExpense,
                label: displayDate
            })
        }
        setTrends(result)
    }


    useEffect(() => {
        console.log("account", selectedAccount)
        console.log("is account null", selectedAccount == null)
        if (selectedAccount == null) {
            loadUserTrendsMonth()
        } else {
            console.log("loading account")
            loadAccountTrendsMonth()
        }

    }, []);

    return (
      <View
        style={{
            flex: 1,
            flexDirection: 'column',
            width: "100%",
            height: "100%",
            backgroundColor: backgroundColor,
        }}>
          <View
              style={{
                  flexDirection: 'column',
                  width: "100%",
                  marginBottom: 10,
                  position: 'absolute',
                  bottom: 0,
              }}>
        <CustomBarChart data={trends} amount={1} />
          </View>
    </View>
  );
}

const styles = StyleSheet.create({
  headerImage: {
    color: '#808080',
    bottom: -90,
    left: -35,
    position: 'absolute',
  },
  titleContainer: {
    flexDirection: 'row',
    gap: 8,
  },
});
