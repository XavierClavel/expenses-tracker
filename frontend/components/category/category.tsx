import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import React, { PropsWithChildren, SetStateAction, useState} from 'react';
import FontAwesome5 from "@expo/vector-icons/FontAwesome5";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import {IconSymbol} from "@/components/ui/icon-symbol";
import {useThemeColor} from "@/hooks/use-theme-color";






export function Category({ data }) {
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');

    const renderIcon = (color: string, icon: string) => {
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
                justifyContent: "flex-start",
            }}
        >
        {renderIcon(data.color, data.icon)}
        <Text style={{ color: textOnSurfaceColor, fontSize: 16, marginVertical: 8 }}>
            {data.label}
        </Text>
    </View>
    </View>
}