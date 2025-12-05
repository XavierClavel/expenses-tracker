import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import { PropsWithChildren, SetStateAction, useState} from 'react';
import {useThemeColor} from "@/hooks/use-theme-color";

export function ProgressBar({ progress, color }) {
    const backgroundColor = useThemeColor({}, 'background');
    return (
        <View style={{
            height: 4,
            width: "100%",
            backgroundColor: backgroundColor,
            borderRadius: 10,
            overflow: "hidden",
        }}>
            <View style={{
                height: "100%",
                width: `${progress}%`,
                backgroundColor: color,
            }} />
        </View>
    );
}