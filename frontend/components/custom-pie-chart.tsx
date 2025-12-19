import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import { PropsWithChildren, SetStateAction, useState} from 'react';
import {CategoryReport} from "@/components/category/category-report";
import {useTheme} from "@react-navigation/core";
import {useThemeColor} from "@/hooks/use-theme-color";
import {colors} from "@/constants/colors";
import {with2Decimals, withNoDecimals, withReadableThousands} from "@/src/utils/math";
import {TouchableRipple} from "react-native-paper";
import {router} from "expo-router";
import {useSelectedCategoryStore} from "@/src/stores/selected-category-store";
import {useCategoriesStore} from "@/src/stores/categories-store";






export function CustomPieChart({ data }) {
    const [focusedItem, setFocusedItem] = useState(0)

    const backgroundColor = useThemeColor({}, 'background');
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnBackgroundColor = useThemeColor({}, 'textOnBackground');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');
    const setSelectedCategory = useSelectedCategoryStore(s => s.setSelected)
    const categoriesStore = useCategoriesStore()

    let newFocusedItem = 0

    const pieData = data
        .sort((function(a, b) {
            return b.value - a.value;
        }))
        .filter((function(v) {
            return v.value > 0
        }))
        .map(it => {
            console.log(it.value)
        return {
            value: Math.abs(it.value),
            color: colors[it.color || 'unknown'],
            icon: it.icon,
            label: it.label,
            gradientCenterColor: it.color,
            radius: newFocusedItem === data.indexOf(it) ? 130 : 120,
            id: it.id,
        }
    })

    const categoryData = data
        .sort((function(a, b) {
            return b.value - a.value;
        }))
        .filter((function(v) {
            return v.value > 0
        }))
        .map(it => {
            return {
                value: it.value,
                color: it.color,
                icon: it.icon,
                label: it.label,
                id: it.id,
            }
        })

    if (pieData.length == 0) {
        console.log("no data")
        return <Text>No data</Text>
    }


    const total: number = pieData.reduce((accumulator, object) => {
        return accumulator + object.value;
    }, 0);

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
                    if (pieData[focusedItem] == null) {
                        return(
                            <View style={{justifyContent: 'center', alignItems: 'center'}}>
                                <Text
                                    style={{fontSize: 22, color: textOnBackgroundColor, fontWeight: 'bold'}}>
                                    {withReadableThousands(with2Decimals(total))}€
                                </Text>
                            </View>
                        )
                    }
                    const percent = pieData[focusedItem].value / total * 100
                    return (
                        <View style={{justifyContent: 'center', alignItems: 'center'}}>
                            <Text
                                style={{fontSize: 22, color: textOnBackgroundColor, fontWeight: 'bold'}}>
                                {withReadableThousands(with2Decimals(pieData[focusedItem].value))}€
                            </Text>
                            <Text style={{fontSize: 14, color: textOnBackgroundColor}}>{
                                percent >= 1 ? `${withNoDecimals(percent)}%` : '<1%'
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
                backgroundColor: focusedItem != -1 ? pieData[focusedItem].color : surfaceColor,
                height: 50,
            }}>
            <Pressable style={{ width: 50, paddingVertical: 14, borderRadius: 8}} onPress={() => {
                if (focusedItem > 0) {
                    setFocusedItem(focusedItem - 1)
                } else {
                    setFocusedItem(pieData.length - 1)
                }
            }}
                >
                <Text style={{ color: focusedItem == -1 ? textOnSurfaceColor: 'black', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>{'<'}</Text>
            </Pressable>
            <View style={{ flex: 1, alignItems: 'center' }}>
                <Text style={{ color: focusedItem == -1 ? textOnSurfaceColor: 'black', textAlign: 'center', fontSize: 14, marginVertical: 8, fontWeight: 'bold' }}>
                    {pieData[focusedItem]?.label  || 'Total'}
                </Text>
            </View>
            <Pressable style={{ width: 50, paddingVertical: 14, borderRadius:8}} onPress={() => {
                if (focusedItem < pieData.length - 1) {
                    setFocusedItem(focusedItem + 1)
                } else {
                    setFocusedItem(0)
                }
            }}>
                <Text style={{ color: focusedItem == -1 ? textOnSurfaceColor: 'black', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>{'>'}</Text>
            </Pressable>
        </View>
            <View style={{
                flex: 1,
                justifyContent: "center",   // vertical center
                alignItems: "center",        // horizontal center
            }}>
                {categoryData.map((item, index) => (
                    <TouchableRipple style={{width: "100%"}} key={index}>
                    <Pressable onPress={() => {
                        setSelectedCategory(categoriesStore.getCategory(item.id))
                        router.navigate("analytics/subcategories")
                    }}>
                    <CategoryReport item={item} percent={item.value / total * 100} />
                    </Pressable>
                    </TouchableRipple>
                ))}
            </View>
    </View>
}