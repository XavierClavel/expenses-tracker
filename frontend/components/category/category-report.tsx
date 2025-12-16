import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import React, { PropsWithChildren, SetStateAction, useState} from 'react';
import {ProgressBar} from "@/components/progress-bar";
import {IconSymbol} from "@/components/ui/icon-symbol";
import {useThemeColor} from "@/hooks/use-theme-color";
import {StandardIcon} from "@/components/standard-icon";
import {colors} from "@/constants/colors";
import {withNoDecimals, withReadableThousands} from "@/src/utils/math";

export function CategoryReport({ item, percent }) {
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');

    const displayedPercent = percent >= 1 ? `${withNoDecimals(percent)}%` : '<1%'

    return (
        <View
            style={{
                flexDirection: 'row',
                alignItems: 'center',
                marginVertical: 5,
                paddingHorizontal: 10,
                width: "100%",
                borderRadius: 8,
                backgroundColor: surfaceColor,
                height: 70,
            }}>
        <View
            style={{
                flex: 1,
                flexDirection: 'row',
                alignItems: 'center',
            }}>
            <StandardIcon icon={item.icon} color={item.color} />
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
                    <Text style={{ color: textOnSurfaceColor, fontSize: 16 }}>
                        {item.label}
                    </Text>
                        <Text style={{ textAlign:'right', color: textOnSurfaceColor, fontWeight:'bold', fontSize: 16}}>
                            {withReadableThousands(item.value)}€
                        </Text>
                </View>

                <View
                    style={{
                        flexDirection: 'row',
                        marginRight: 40,
                        alignItems: 'center',
                    }}
                >
                <Text style={{ color: textOnSurfaceColor, fontSize: 14, fontWeight: 'bold', width: 40, justifyContent: 'center' }}>
                    {displayedPercent}
                </Text>
                <ProgressBar progress={percent} color={colors[item.color || 'unknown']}></ProgressBar>
            </View>
            </View>
        </View>
        </View>
    );
}