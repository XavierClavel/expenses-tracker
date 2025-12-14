import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import React, { PropsWithChildren, SetStateAction, useState} from 'react';
import FontAwesome5 from "@expo/vector-icons/FontAwesome5";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import {IconSymbol} from "@/components/ui/icon-symbol";
import {useThemeColor} from "@/hooks/use-theme-color";
import CategoryIn from "@/src/types/CategoryIn";
import CategoryOut from "@/src/types/CategoryOut";
import {colors} from "@/constants/colors";
import {StandardIcon} from "@/components/standard-icon";



type CategoryDisplayProps = {
    data: CategoryOut
};


export function CategoryDisplay( { data }: CategoryDisplayProps) {
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');

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
                justifyContent: "flex-start",
            }}
        >
            <StandardIcon icon={data.icon} color={data.color} />
        <Text style={{ color: textOnSurfaceColor, fontSize: 16, marginVertical: 8 }}>
            {data.name}
        </Text>
    </View>
    </View>
}