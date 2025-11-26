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
    const setSize = 16  + 7 + 3
    return <View style={{padding: 0, marginTop: 100, alignItems: 'center'}}>
        <View
            style={{
                position: "absolute",
                left: windowWidth / 2,
                top: 0,
                bottom: 0,
                width: 1,
                backgroundColor: "rgba(255,255,255,0.2)", // light, subtle line
                zIndex: 0,   // make sure it stays behind
                marginBottom: 25,
            }}
        />
        <View style={{ zIndex: 1 }}>
        <BarChart
            //adjustToWidth
            width={windowWidth}
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
            yAxisThickness={0}
            xAxisColor={'lightgray'}
            yAxisTextStyle={{color: 'lightgray'}}
            stepValue={1000}
            maxValue={6000}
            noOfSections={6}
            labelWidth={40}
            xAxisLabelTextStyle={{color: 'lightgray', textAlign: 'center'}}
            hideYAxisText
            //showLine
            onScroll={(item: any)=> {
                const x = item.nativeEvent.contentOffset.x
                const index = Math.round(x / setSize)
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
    </View>
}