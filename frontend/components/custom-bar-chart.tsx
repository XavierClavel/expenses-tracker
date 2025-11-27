import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import {BarChart, PieChart} from "react-native-gifted-charts";
import {PropsWithChildren, SetStateAction, useRef, useState} from 'react';
import {Dimensions} from 'react-native';


//TODO: prepare input
//TODO: set y scale


export function CustomBarChart({ data }) {
    const windowWidth = Dimensions.get('window').width;
    //console.log("window width", windowWidth)
    const [focusedItem, setFocusedItem] = useState([data.length -2, data.length -1])
    const [scrollPosition, setScrollPosition] = useState(0)
    const barSize = 16
    const interSpacing = 6
    const spacing = 20
    const setSize = spacing + barSize * 2 + interSpacing
    const scrollRef = useRef(null);

    function renderValues() {
        return
        if (focusedItem.length > 0) return <View style={{
            flexDirection: 'row',
            justifyContent: 'space-evenly'
        }}>
            <Pressable style={{ paddingVertical: 10, width:150, backgroundColor: 'green', borderRadius: 8 }}>
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>
                    {data[focusedItem[0]].value}€
                </Text>
            </Pressable>
            <Pressable style={{ paddingVertical: 10, width:150, backgroundColor: 'red', borderRadius: 8 }}>
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>
                    -{data[focusedItem[1]].value}€
                </Text>
            </Pressable>
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
                backgroundColor: "rgba(255,255,255,0.2)", // light, subtle line
                zIndex: 0,   // make sure it stays behind
                marginBottom: 25,
                borderRadius: 5,
            }}
        />
        <View style={{ zIndex: 1 }}>
        <BarChart
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
            stepValue={1000}
            maxValue={6000}
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
                setScrollPosition(x)
                //console.log(x)
            }}
            onScrollEndDrag={(e, d) => {

            }}

            onMomentumScrollEnd={(e) => {
                console.log("momentum scroll end")
                const x = scrollPosition
                let targetX = Math.round((x/setSize))*setSize ;
                //console.log(targetX)

                scrollRef.current?.scrollTo({
                    x: targetX,
                    animated: false,
                });


            }}






            onPress={(item: any, index: number) => {
                console.log(index)
            }}
        />
        </View>
    </View>
    </View>
}