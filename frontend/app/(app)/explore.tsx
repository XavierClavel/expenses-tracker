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
import {getMonthTrends} from "@/src/api/trends";
import {useCategoriesStore} from "@/src/stores/categories-store";
import {useEffect, useState} from "react";

const colorExpense = '#da451a'
const colorIncome = '#71cc5d'

export default function TabTwoScreen() {
    const backgroundColor = useThemeColor({}, 'background');
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');

    const [trends, setTrends] = useState([])

    const loadTrends = async () => {
        const trends = await getMonthTrends()
        const result = []
        for (const v of trends) {
            const date = new Date(v.year, v.month - 1)
            const currentDate = new Date()
            const displayDate= date.getFullYear() == currentDate.getFullYear() ?
                date.toLocaleString('default', { month: 'short' })
                : date.toLocaleString('default', { month: 'short', year: 'numeric' })
            result.push({
                value: Number(v.totalIncome),
                frontColor: colorIncome,
                spacing: 6,
                label: displayDate
            })
            result.push({
                value: Number(v.totalExpenses),
                frontColor: colorExpense,
            })
        }
        setTrends(result)
    }

    useEffect(() => {
        loadTrends()
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
        <CustomBarChart data={trends} />
        <View style={{
            flexDirection: 'row',
            justifyContent: 'space-evenly',
            //flex: 1,
            width: "100%",
            marginVertical: 5
        }}>
            <Pressable style={{ paddingVertical: 10, width:75, backgroundColor: surfaceColor, borderRadius: 8 }}>
                <Text style={{ color: textOnSurfaceColor, textAlign: 'center', fontSize: 14, fontWeight: 'bold' }}>
                    Month
                </Text>
            </Pressable>
            <Pressable style={{ paddingVertical: 10, width:75, backgroundColor: surfaceColor, borderRadius: 8 }}>
                <Text style={{ color: textOnSurfaceColor, textAlign: 'center', fontSize: 14, fontWeight: 'bold' }}>
                    Year
                </Text>
            </Pressable>
        </View>
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
