import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import { PropsWithChildren, SetStateAction, useState} from 'react';






export function Expense({ data }) {

    const renderDot = (color: string) => {
        return (
            <View
                style={{
                    height: 10,
                    width: 10,
                    borderRadius: 5,
                    backgroundColor: color,
                    marginRight: 10,
                }}
            />
        );
    };

    return <View
        style={{
            flex: 1,
            flexDirection: 'row',
            alignItems: 'center',
            marginVertical: 5,
            paddingHorizontal: 10,
            width: "100%",
            borderRadius: 8,
            backgroundColor: '#34448B',
            height: 50,
        }}>
        {renderDot(data.color)}
        <Text style={{ color: 'white', fontSize: 16, marginVertical: 8 }}>
            {data.label}
        </Text>
        <View style={{ flex: 1, alignItems: 'right' }}>
            <Text style={{ textAlign:'right', color: 'white', fontWeight:'bold', fontSize: 16, marginVertical: 8 }}>
                {data.value}€
            </Text>
        </View>
    </View>
}