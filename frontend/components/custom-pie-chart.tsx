import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import { PropsWithChildren, SetStateAction, useState} from 'react';
import {CategoryReport} from "@/components/category-report";






export function CustomPieChart({ data }) {
    const total = data.reduce((accumulator, object) => {
        return accumulator + object.value;
    }, 0);

    let newFocusedItem = 0

    const pieData = data.map(it => {
        return {
            value: Math.abs(it.value),
            color: it.color,
            gradientCenterColor: it.color,
            radius: newFocusedItem === data.indexOf(it) ? 130 : 120,
        }
    })

    const [focusedItem, setFocusedItem] = useState(0)

    return <View
        style={{
            padding: 16,
        }}>
        <View style={{padding: 20, alignItems: 'center'}}>
            <PieChart
                sectionAutoFocus
                focusedPieIndex={focusedItem}

                onPress={(item: any, index: number) => {
                    if (focusedItem == index) {
                        setFocusedItem(-1)
                    } else {
                        setFocusedItem(index)
                    }
                }}


                data={pieData}
                donut
                showGradient
                radius={120}
                innerRadius={90}
                innerCircleColor={'#232B5D'}
                centerLabelComponent={() => {
                    if (data[focusedItem] == null) {
                        return(
                            <View style={{justifyContent: 'center', alignItems: 'center'}}>
                                <Text
                                    style={{fontSize: 22, color: 'white', fontWeight: 'bold'}}>
                                    {(Math.round(total * 100) / 100).toFixed(2)}€
                                </Text>
                            </View>
                        )
                    }
                    return (
                        <View style={{justifyContent: 'center', alignItems: 'center'}}>
                            <Text
                                style={{fontSize: 22, color: 'white', fontWeight: 'bold'}}>
                                {data[focusedItem].value}€
                            </Text>
                            <Text style={{fontSize: 14, color: 'white'}}>{data[focusedItem].label}</Text>
                        </View>
                    );
                }}
            />
        </View>
        <View
            style={{
                flex: 1,
                flexDirection: 'row',
                alignItems: 'center',
                marginTop: 5,
                marginBottom: 50,
                marginHorizontal: 50,
                paddingHorizontal: 10,
                borderRadius: 8,
                backgroundColor: '#34448B',
                height: 50,
            }}>
            <Pressable style={{ width: 50, paddingVertical: 10, backgroundColor: '#34448B', borderRadius: 8 }} onPress={() => {
                console.log("minus")
                if (focusedItem > 0) {
                    setFocusedItem(focusedItem - 1)
                } else {
                    setFocusedItem(data.length - 1)
                }
            }}
                >
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>{'<'}</Text>
            </Pressable>
            <View style={{ flex: 1, alignItems: 'center' }}>
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 16, marginVertical: 8 }}>
                    {data[focusedItem]?.label  || 'Total'}
                </Text>
            </View>
            <Pressable style={{ width: 50, backgroundColor: '#34448B', borderRadius: 8 }} onPress={() => {
                console.log("minus")
                if (focusedItem < data.length - 1) {
                    setFocusedItem(focusedItem + 1)
                } else {
                    setFocusedItem(0)
                }
            }}>
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>{'>'}</Text>
            </Pressable>
        </View>
            <View style={{
                flex: 1,
                justifyContent: "center",   // vertical center
                alignItems: "center",        // horizontal center
            }}>
                {data.map((item, index) => (
                    <CategoryReport item={item} percent={item.value / total * 100}/>
                ))}
            </View>
    </View>
}