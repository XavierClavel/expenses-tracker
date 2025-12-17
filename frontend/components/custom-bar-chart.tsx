import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import {BarChart, PieChart} from "react-native-gifted-charts";
import {PropsWithChildren, SetStateAction, useEffect, useRef, useState} from 'react';
import {Dimensions} from 'react-native';
import {time} from "@expo/fingerprint/cli/build/utils/log";
import {with2Decimals, withReadableThousands} from "@/src/utils/math";


//TODO: set y scale


export function CustomBarChart({ data }) {
    const [focusedItem, setFocusedItem] = useState([]);
    const windowWidth = Dimensions.get('window').width;
    const scrollPosition = useRef(0)
    const barSize = 16
    const interSpacing = 6
    const spacing = 20
    const setSize = spacing + barSize * 2 + interSpacing
    const scrollRef = useRef(null);
    const isSnapping = useRef(false);
    const numberOfSteps = 5
    const maxValue = data.reduce(function(prev, current) {
        return (prev && prev.value > current.value) ? prev : current
    },0).value
    console.log("max", maxValue)
    const orderOfMagnitude = Math.max(3,Math.floor(Math.log10(maxValue)))
    const magnitude = Math.pow(10,orderOfMagnitude)
    console.log("order of magnitude", orderOfMagnitude)

    const roundedMax = Math.ceil(maxValue / magnitude) * magnitude
    console.log("chart max", roundedMax)
    const chartStep = Math.ceil(roundedMax  / (numberOfSteps * magnitude)) * magnitude
    const chartMax = chartStep * numberOfSteps
    console.log("step", chartStep)

    useEffect(() => {
        console.log(data)
        if (data.length >= 2) {
            setFocusedItem([data.length - 2, data.length - 1]);
        } else if (data.length === 1) {
            setFocusedItem([0]);
        } else {
            setFocusedItem([]);
        }
    }, [data]);

    if (data.length === 0) return <Text>No data</Text>;


    function renderValues() {
        if (data.length == 0 || focusedItem.some((it) => data.length <= it)) return <Text>No data</Text>
        if (focusedItem.length > 0) return <View style={{
            flexDirection: 'row',
            justifyContent: 'space-evenly',
            flex: 1,
            width: "100%",
            marginVertical: 20
        }}>
                <Text style={{ color: '#71cc5d', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>
                    {withReadableThousands(with2Decimals(data[focusedItem[0]].value))}€
                </Text>
                <Text style={{ color: '#da451a', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>
                    -{withReadableThousands(with2Decimals(data[focusedItem[1]].value))}€
                </Text>
        </View>
        else return
    }

    return  <View
        style={{
            flexDirection: 'column',
            justifyContent: 'space-between',
            marginVertical: 4,
        }}
    >

        <View
            style={{
                flexDirection: 'row',
                justifyContent: 'space-evenly'
            }}
        >
            {renderValues()}
        </View>

    <View style={{padding: 0, margin: 0, alignItems: 'stretch'}}>

        <View
            style={{
                position: "absolute",
                left: (windowWidth - setSize ) / 2,
                top: 0,
                bottom: 0,
                width: setSize,
                backgroundColor: "rgba(255,255,255,0.2)",
                zIndex: 0,
                marginBottom: 40,
                borderRadius: 5,
            }}
        />
        <View style={{
            zIndex: 1,
        }}>
        <BarChart
            xAxisTextNumberOfLines={2}
            scrollRef={scrollRef}
            //adjustToWidth
            width={windowWidth}
            initialSpacing={(windowWidth / 2) - barSize - interSpacing / 2 }
            endSpacing={windowWidth/2 - windowWidth /4 - 10}
            //focusBarOnPress
            highlightEnabled
            scrollToEnd
            focusedBarIndex={focusedItem}
            lineBehindBars
            yAxisLabelWidth={0}
            scrollToIndex={focusedItem[0]}

            //style={{width: "50%"}}
            height={400}
            data={data}
            barWidth={barSize}
            //initialSpacing={(windowWidth  / 2 ) - 19}
            //initialSpacing={0}
            //endSpacing={windowWidth / 2 - 112 }
            //endSpacing={windowWidth / 2}
            spacing={spacing}
            barBorderRadius={3}
            //yAxisThickness={0}
            xAxisColor={'lightgray'}
            //yAxisTextStyle={{color: 'lightgray'}}
            stepValue={chartStep}
            maxValue={chartMax}
            labelWidth={40}
            xAxisLabelTextStyle={{color: 'lightgray', textAlign: 'center'}}
            hideYAxisText
            secondaryYAxis
            yAxisThickness={0}
            yAxisColor={'red'}
            rulesType={"solid"}
            rulesColor={"rgba(255,255,255,0.2)"}
            //showLine
            onScroll={(item: any)=> {
                const x = item.nativeEvent.contentOffset.x
                const index = Math.round((x / setSize) )
                //console.log(x, index)
                setFocusedItem([index * 2, index*2+1])
                scrollPosition.current = x
                //console.log(x)
            }}
            onScrollEndDrag={(e, d) => {

            }}

            onMomentumScrollEnd={(e) => {
                //console.log("momentum scroll end")
                const x = scrollPosition.current
                let targetX = Math.round((x/setSize))*setSize ;
                //console.log(targetX)

                if (isSnapping.current) return
                isSnapping.current = true

                scrollRef.current?.scrollTo({
                    x: targetX,
                    animated: true,
                });

                setTimeout(() => {
                    isSnapping.current = false
                }, 200)


            }}






            onPress={(item: any, index: number) => {
                console.log(index)
            }}
        />
        </View>
    </View>
    </View>
}