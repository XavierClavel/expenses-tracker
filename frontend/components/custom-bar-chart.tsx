import { Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import {BarChart, PieChart} from "react-native-gifted-charts";
import {PropsWithChildren, SetStateAction, useEffect, useRef, useState} from 'react';
import {Dimensions} from 'react-native';
import {time} from "@expo/fingerprint/cli/build/utils/log";
import {with2Decimals, withReadableThousands} from "@/src/utils/math";


type Props = {
    data: any[],
    amount: number,
};

export function CustomBarChart({ data, amount }: Props) {
    const [focusedItem, setFocusedItem] = useState([]);
    const windowWidth = Dimensions.get('window').width;
    const scrollPosition = useRef(0)
    const barSize = 16
    const interSpacing = 6 * (amount - 1)
    const spacing = 20
    const setSize = spacing + barSize * amount + interSpacing
    const scrollRef = useRef(null);
    const isSnapping = useRef(false);
    const numberOfSteps = 5
    const maxValue = data.reduce(function(prev, current) {
        return (prev && prev.value > current.value) ? prev : current
    },0).value
    const minValue = Math.min(0,data.reduce(function(prev, current) {
        return (prev && prev.value < current.value) ? prev : current
    },0).value)
    const orderOfMagnitude = Math.floor(Math.log10(maxValue))
    const magnitude = Math.pow(10,orderOfMagnitude)
    const roundedMax = Math.ceil((maxValue - minValue) / magnitude) * magnitude
    const chartStep = Math.ceil(roundedMax  / (numberOfSteps * magnitude)) * magnitude
    const chartMax = chartStep * numberOfSteps
    const [prevDataLength, setPrevDataLength] = useState(-1)

    useEffect(() => {
        if (data.length == 0) {
            return
        }
        console.log("data updated for bar chart")
        console.log(data.length, prevDataLength)
        console.log(focusedItem)
        if (prevDataLength == data.length) {
            console.log("same sample size")
            setFocusedItem(focusedItem)
            return
        }
        let newFocusedItem = [];

        if (data.length >= 2) {
            if (amount == 2) {
                newFocusedItem = [data.length - 2, data.length - 1]
            } else {
                newFocusedItem = [data.length - 1]
            }

        } else if (data.length === 1) {
            newFocusedItem = [0]
        } else {
            newFocusedItem = []
        }

        console.log("here", prevDataLength, data.length)

        if (prevDataLength != data.length) {
            const targetX = newFocusedItem[0] * setSize / amount
            console.log("target x", targetX, setSize)

            scrollRef.current?.scrollTo({
                x: targetX,
                animated: false,
            });
        }
        setFocusedItem(newFocusedItem)
        setPrevDataLength(data.length)
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
                <Text style={{ color: data[focusedItem[0]].value > 0 ? '#71cc5d' : '#da451a', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>
                    {withReadableThousands(with2Decimals(data[focusedItem[0]].value))}€
                </Text>
            {focusedItem.length > 1 &&
                <Text style={{color: '#da451a', textAlign: 'center', fontSize: 17, fontWeight: 'bold'}}>
            -{withReadableThousands(with2Decimals(data[focusedItem[1]].value))}€
        </Text>
    }
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
            initialSpacing={(windowWidth - barSize * amount - interSpacing) / 2 }
            endSpacing={windowWidth/2 - windowWidth /4 - interSpacing}
            //focusBarOnPress
            highlightEnabled
            focusedBarIndex={focusedItem}
            lineBehindBars
            yAxisLabelWidth={0}
            scrollToIndex={focusedItem[0]}
            //autoShiftLabels

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
            labelWidth={barSize + spacing}
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
                //console.log(x, index)
                if (amount == 2) {
                    const index = Math.round((x / setSize) )
                    setFocusedItem([index * 2, index * 2 + 1])
                } else {
                    const index = Math.round((x / setSize))
                    setFocusedItem([index])
                }
                scrollPosition.current = x
                //console.log(x)
            }}
            onScrollEndDrag={(e, d) => {

            }}

            onMomentumScrollEnd={(e) => {
                //console.log("momentum scroll end")
                const x = scrollPosition.current
                const targetX = Math.round((x/setSize))*setSize

                console.log(x, targetX, setSize)

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