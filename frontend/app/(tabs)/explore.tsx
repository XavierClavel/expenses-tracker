import { Image } from 'expo-image';
import {Platform, StyleSheet, View} from 'react-native';

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
    <ParallaxScrollView
      headerBackgroundColor={{ light: '#D0D0D0', dark: '#353636' }}
      headerImage={
          <Image
              source={require('@/assets/images/partial-react-logo.png')}
          />
      }>
        <View
            style={{
                flex: 1,
                flexDirection: 'row',
                alignItems: 'center',
                width: "100%",
                marginTop: 70,
                justifyContent: "space-around"
            }}>
            <CustomBarChart data={data} />
        </View>
    </ParallaxScrollView>
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
