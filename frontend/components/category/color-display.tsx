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



type ColorDisplayProps = {
    label: string,
    color: string,
};


export function ColorDisplay( { label, color }: ColorDisplayProps) {
    console.log("data", label, color)
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');

    const renderIcon = (color: string) => {
        return (
            <View
                style={{
                    height: 30,
                    width: 30,
                    borderRadius: 15,
                    backgroundColor: color,
                    marginRight: 10,
                    alignItems: 'center',
                    justifyContent: 'center',
                }}
            >
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
                justifyContent: "flex-start",
            }}
        >
        {renderIcon(color)}
        <Text style={{ color: textOnSurfaceColor, fontSize: 16, marginVertical: 8 }}>
            {label}
        </Text>
    </View>
    </View>
}