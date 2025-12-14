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



type ColorDisplayProps = {
    icon: string,
    color: string,
};


export function StandardIcon( { icon, color }: ColorDisplayProps) {
    const surfaceColor = useThemeColor({}, 'surface');

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
}