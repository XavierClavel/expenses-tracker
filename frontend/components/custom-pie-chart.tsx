import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import { PropsWithChildren, SetStateAction, useState} from 'react';
import {CategoryReport} from "@/components/category-report";
import {useTheme} from "@react-navigation/core";
import {useThemeColor} from "@/hooks/use-theme-color";






export function CustomPieChart({ data }) {
    const backgroundColor = useThemeColor({}, 'background');
    const surfaceColor = useThemeColor({}, 'surface');

    const total = data.reduce((accumulator, object) => {
        return accumulator + object.value;
    }, 0);

    let newFocusedItem = 0

    const pieData = data.map(it => {
        return {
            value: Math.abs(it.value),
            color: it.color,
            icon: it.icon,
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
                radius={120}
                innerRadius={90}
                innerCircleColor={backgroundColor}
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
                    const percent = data[focusedItem].value / total * 100
                    return (
                        <View style={{justifyContent: 'center', alignItems: 'center'}}>
                            <Text
                                style={{fontSize: 22, color: 'white', fontWeight: 'bold'}}>
                                {data[focusedItem].value}€
                            </Text>
                            <Text style={{fontSize: 14, color: 'white'}}>{
                                percent >= 1 ? `${Math.round(percent.toFixed(0))}%` : '<1%'
                            }</Text>
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
                borderRadius: 15,
                backgroundColor: focusedItem != -1 ? data[focusedItem].color : surfaceColor,
                height: 50,
            }}>
            <Pressable style={{ width: 50, paddingVertical: 14, borderRadius: 8}} onPress={() => {
                if (focusedItem > 0) {
                    setFocusedItem(focusedItem - 1)
                } else {
                    setFocusedItem(data.length - 1)
                }
            }}
                >
                <Text style={{ color: focusedItem == -1 ? 'white' : 'black', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>{'<'}</Text>
            </Pressable>
            <View style={{ flex: 1, alignItems: 'center' }}>
                <Text style={{ color: focusedItem == -1 ? 'white' : 'black', textAlign: 'center', fontSize: 14, marginVertical: 8, fontWeight: 'bold' }}>
                    {data[focusedItem]?.label  || 'Total'}
                </Text>
            </View>
            <Pressable style={{ width: 50, paddingVertical: 14, borderRadius:8}} onPress={() => {
                if (focusedItem < data.length - 1) {
                    setFocusedItem(focusedItem + 1)
                } else {
                    setFocusedItem(0)
                }
            }}>
                <Text style={{ color: focusedItem == -1 ? 'white' : 'black', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>{'>'}</Text>
            </Pressable>
        </View>
            <View style={{
                flex: 1,
                justifyContent: "center",   // vertical center
                alignItems: "center",        // horizontal center
            }}>
                {data.map((item, index) => (
                    <CategoryReport item={item} percent={item.value / total * 100} key={index}/>
                ))}
            </View>
    </View>
}