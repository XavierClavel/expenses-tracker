import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import React, { PropsWithChildren, SetStateAction, useState} from 'react';
import FontAwesome5 from "@expo/vector-icons/FontAwesome5";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import {IconSymbol} from "@/components/ui/icon-symbol";






export function Expense({ data }) {

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
                <IconSymbol name={icon} color={'#34448B'} size={20} />
            </View>
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
        {renderIcon(data.color, data.icon)}
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