import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import {BarChart, PieChart} from "react-native-gifted-charts";
import { PropsWithChildren, SetStateAction, useState} from 'react';
import {Dimensions} from 'react-native';


//TODO: prepare input
//TODO: set y scale


export function CustomBarChart({ data }) {
    const windowWidth = Dimensions.get('window').width;
    const [focusedItem, setFocusedItem] = useState([])
    return <View style={{padding: 20, marginTop: 100, alignItems: 'center'}}>
        <BarChart
            //adjustToWidth
            width={300}
            //focusBarOnPress
            highlightEnabled
            scrollToEnd
            focusedBarIndex={focusedItem}
            //style={{width: "50%"}}
            height={400}
            data={data}
            barWidth={16}
            initialSpacing={windowWidth / 2}
            spacing={14}
            barBorderRadius={3}
            //showGradient
            yAxisThickness={0}
            xAxisType={'solid'}
            xAxisColor={'lightgray'}
            yAxisTextStyle={{color: 'lightgray'}}
            stepValue={1000}
            maxValue={6000}
            noOfSections={6}
            yAxisLabelTexts={['0', '1k', '2k', '3k', '4k', '5k', '6k']}
            labelWidth={40}
            xAxisLabelTextStyle={{color: 'lightgray', textAlign: 'center'}}
            hideYAxisText
            //showLine
            onScroll={(item: any)=> {
                const x = item.nativeEvent.contentOffset.x
                const index = Math.round(x / 30)
                if (index % 2 == 0) {
                    setFocusedItem([index, index + 1])
                } else {
                    setFocusedItem([index -1, index])
                }
                console.log(index)
            }}
            onPress={(item: any, index: number) => {
                console.log(index)
            }}
        />
    </View>
}