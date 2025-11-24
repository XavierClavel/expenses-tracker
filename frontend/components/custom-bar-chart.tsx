import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import {BarChart, PieChart} from "react-native-gifted-charts";
import { PropsWithChildren, SetStateAction, useState} from 'react';



//TODO: prepare input
//TODO: set y scale


export function CustomBarChart({ data }) {
    return <View style={{padding: 20, alignItems: 'center'}}>
        <BarChart
            data={data}
            barWidth={16}
            initialSpacing={10}
            spacing={14}
            barBorderRadius={3}
            showGradient
            yAxisThickness={0}
            xAxisType={'dashed'}
            xAxisColor={'lightgray'}
            yAxisTextStyle={{color: 'lightgray'}}
            stepValue={1000}
            maxValue={6000}
            noOfSections={6}
            yAxisLabelTexts={['0', '1k', '2k', '3k', '4k', '5k', '6k']}
            labelWidth={40}
            xAxisLabelTextStyle={{color: 'lightgray', textAlign: 'center'}}
            showLine
            lineConfig={{
                color: '#F29C6E',
                thickness: 3,
                curved: true,
                hideDataPoints: true,
                shiftY: 20,
                initialSpacing: -30,
            }}
        />
    </View>
}