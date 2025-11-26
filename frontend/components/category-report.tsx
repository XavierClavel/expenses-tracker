import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import React, { PropsWithChildren, SetStateAction, useState} from 'react';
import {ProgressBar} from "@/components/progress-bar";
import {IconSymbol} from "@/components/ui/icon-symbol";

export function CategoryReport({ item, percent }) {

    const displayedPercent = percent >= 1 ? `${Math.round(percent.toFixed(0))}%` : '<1%'

    const renderIcon = (color: string, icon: string) => {
        return (
            <View
                style={{
                    height: 30,
                    width: 30,
                    borderRadius: 15,
                    backgroundColor: color,
                    marginRight: 15,
                    alignItems: 'center',
                    justifyContent: 'center',
                }}
            >
                <IconSymbol name={icon} color={'#34448B'} size={20} />
            </View>
        );
    };


    return (
        <View
            style={{
                flex: 1,
                flexDirection: 'row',
                alignItems: 'center',
                marginVertical: 5,
                paddingHorizontal: 10,
                width: "100%",
                borderRadius: 8,
                backgroundColor: '#34448B',
                height: 70,
            }}>
            {renderIcon(item.color, item.icon)}
            <View
                style={{
                    flex: 1,
                    flexDirection: 'column',
                    paddingBottom: 5,
                }}
            >
                <View
                    style={{
                        flexDirection: 'row',
                        justifyContent: 'space-between',
                        marginVertical: 4,
                    }}
                >
                    <Text style={{ color: 'white', fontSize: 16 }}>
                        {item.label}
                    </Text>
                        <Text style={{ textAlign:'right', color: 'white', fontWeight:'bold', fontSize: 16}}>
                            {item.value}€
                        </Text>
                </View>

                <View
                    style={{
                        flexDirection: 'row',
                        marginRight: 40,
                        alignItems: 'center',
                    }}
                >
                <Text style={{ color: 'white', fontSize: 14, fontWeight: 'bold', width: 40, justifyContent: 'center' }}>
                    {displayedPercent}
                </Text>
                <ProgressBar progress={percent} color={item.color}></ProgressBar>
            </View>
            </View>

        </View>
    );
}