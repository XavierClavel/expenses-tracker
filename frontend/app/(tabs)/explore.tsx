import { Image } from 'expo-image';
import {Platform, Pressable, StyleSheet, Text, View} from 'react-native';

import { Collapsible } from '@/components/ui/collapsible';
import { ExternalLink } from '@/components/external-link';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import { BarChart } from "react-native-gifted-charts";
import {CustomPieChart} from "@/components/custom-pie-chart";
import {CustomBarChart} from "@/components/custom-bar-chart";

const colorExpense = '#da451a'
const colorIncome = '#71cc5d'


const data = [
    {value: 2500, frontColor: colorIncome, spacing: 6, label:'Jan'},
    {value: 2400, frontColor: colorExpense},
    {value: 3500, frontColor: colorIncome, spacing: 6, label:'Feb'},
    {value: 3000, frontColor: colorExpense},
    {value: 4500, frontColor: colorIncome, spacing: 6, label:'Mar'},
    {value: 4000, frontColor: colorExpense},
    {value: 5200, frontColor: colorIncome, spacing: 6, label:'Apr'},
    {value: 4900, frontColor: colorExpense},
    {value: 3000, frontColor: colorIncome, spacing: 6, label:'May'},
    {value: 2800, frontColor: colorExpense},
    {value: 2500, frontColor: colorIncome, spacing: 6, label:'Jan'},
    {value: 2400, frontColor: colorExpense},
    {value: 3500, frontColor: colorIncome, spacing: 6, label:'Feb'},
    {value: 3000, frontColor: colorExpense},
    {value: 4500, frontColor: colorIncome, spacing: 6, label:'Mar'},
    {value: 4000, frontColor: colorExpense},
    {value: 5200, frontColor: colorIncome, spacing: 6, label:'Apr'},
    {value: 4900, frontColor: colorExpense},
    {value: 3000, frontColor: colorIncome, spacing: 6, label:'May'},
    {value: 2800, frontColor: colorExpense},
];

export default function TabTwoScreen() {
  return (
      <View
        style={{
            flex: 1,
            flexDirection: 'column',
            width: "100%",
            height: "100%",
            backgroundColor: '#232B5D',
        }}>
          <View
              style={{
                  flexDirection: 'column',
                  width: "100%",
                  marginBottom: 10,
                  position: 'absolute',
                  bottom: 0,
              }}>
        <CustomBarChart data={data} />
        <View style={{
            flexDirection: 'row',
            justifyContent: 'space-evenly',
            //flex: 1,
            width: "100%",
            marginVertical: 5
        }}>
            <Pressable style={{ paddingVertical: 10, width:75, backgroundColor: '#34448B', borderRadius: 8 }}>
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 14, fontWeight: 'bold' }}>
                    Month
                </Text>
            </Pressable>
            <Pressable style={{ paddingVertical: 10, width:75, backgroundColor: '#34448B', borderRadius: 8 }}>
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 14, fontWeight: 'bold' }}>
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
