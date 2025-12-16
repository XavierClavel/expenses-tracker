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
import {StandardIcon} from "@/components/standard-icon";
import {withReadableThousands} from "@/src/utils/math";


type ExpenseDisplayProps = {
    data: ExpenseOut;
};



export function ExpenseDisplay({ data }: ExpenseDisplayProps) {
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');
    const categoriesStore = useCategoriesStore()
    const subcategory = categoriesStore.getSubcategory(data.categoryId)

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
            <StandardIcon
                icon={subcategory?.icon ? subcategory.icon : 'unknown'}
                color={subcategory?.color ? subcategory.color : 'lightgray'}
            />
        <Text style={{ color: textOnSurfaceColor, fontSize: 16, marginVertical: 8 }}>
            {data.title}
        </Text>
        <View style={{ flex: 1, alignItems: 'right' }}>
            <Text style={{ textAlign:'right', color: textOnSurfaceColor, fontWeight:'bold', fontSize: 16, marginVertical: 8 }}>
                {data.type == "EXPENSE" ? "-" : "+"} {withReadableThousands(data.amount)}€
            </Text>
        </View>
        </View>
    </View>
}