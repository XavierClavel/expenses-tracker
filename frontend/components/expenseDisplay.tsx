import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import React, { PropsWithChildren, SetStateAction, useState} from 'react';
import FontAwesome5 from "@expo/vector-icons/FontAwesome5";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import {IconSymbol} from "@/components/ui/icon-symbol";
import {useThemeColor} from "@/hooks/use-theme-color";
import CategoryIn from "@/src/types/CategoryIn";
import ExpenseOut from "@/src/types/ExpenseOut";
import {useCategoriesStore} from "@/src/stores/categories-store";
import {sub} from "ob1";
import {colors} from "@/constants/colors";


type ExpenseDisplayProps = {
    data: ExpenseOut;
};



export function ExpenseDisplay({ data }: ExpenseDisplayProps) {
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');
    const categoriesStore = useCategoriesStore()
    const subcategory = categoriesStore.getSubcategory(data.categoryId)

    const renderIcon = (color: string, icon: string) => {
        return (
            <View
                style={{
                    height: 30,
                    width: 30,
                    borderRadius: 15,
                    backgroundColor: colors[color],
                    marginRight: 10,
                    alignItems: 'center',
                    justifyContent: 'center',
                }}
            >
                <IconSymbol name={icon} color={surfaceColor} size={20} />
            </View>
        );
    };

    return <View
        style={{
            flexDirection: 'row',
            alignItems: 'center',
            marginVertical: 5,
            paddingHorizontal: 10,
            width: "100%",
            borderRadius: 8,
            backgroundColor: surfaceColor,
            height: 50,
        }}>
        <View
            style={{
                flex: 1,
                flexDirection: "row",
                alignItems: "center",
                justifyContent: "space-between",
            }}
        >
        {renderIcon(
            subcategory?.color ? subcategory.color : 'lightgray',
            subcategory?.icon ? subcategory.icon : 'unknown',
        )}
        <Text style={{ color: textOnSurfaceColor, fontSize: 16, marginVertical: 8 }}>
            {data.title}
        </Text>
        <View style={{ flex: 1, alignItems: 'right' }}>
            <Text style={{ textAlign:'right', color: textOnSurfaceColor, fontWeight:'bold', fontSize: 16, marginVertical: 8 }}>
                {data.type == "EXPENSE" ? "-" : "+"} {data.amount}€
            </Text>
        </View>
        </View>
    </View>
}