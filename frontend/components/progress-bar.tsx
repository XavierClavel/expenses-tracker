import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import { PropsWithChildren, SetStateAction, useState} from 'react';

export function ProgressBar({ progress, color }) {
    return (
        <View style={{
            height: 4,
            width: "100%",
            backgroundColor: '#232B5D',
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