import { View, type ViewProps } from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import { PropsWithChildren, SetStateAction, useState} from 'react';



const pieData = [
    {value: 47, label: 'Excellent', color: '#009FFF', gradientCenterColor: '#006DFF'},
    {value: 40, label: 'Good', color: '#93FCF8', gradientCenterColor: '#3BE9DE'},
    {value: 16, label: 'Okay', color: '#BDB2FA', gradientCenterColor: '#8F80F3'},
    {value: 3, label: 'Poor', color: '#FFA5BA', gradientCenterColor: '#FF7F97'},
];


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

export function CustomPieChart({}) {
    const [focusedItem, setFocusedItem] = useState(1)

    return <View
            style={{
                padding: 16,
            }}>
            <View style={{padding: 20, alignItems: 'center'}}>
                <PieChart
                    focusOnPress
                    toggleFocusOnPress
                    focusedPieIndex={focusedItem}
                    onPress={(item: any, index: number) => {
                        console.log(index)
                        setFocusedItem(index)
                        console.log(index)
                    }}
                    data={pieData}
                    donut
                    showGradient
                    radius={90}
                    innerRadius={60}
                    innerCircleColor={'#232B5D'}
                    centerLabelComponent={() => {
                        return (
                            <View style={{justifyContent: 'center', alignItems: 'center'}}>
                                <Text
                                    style={{fontSize: 22, color: 'white', fontWeight: 'bold'}}>
                                    {pieData[focusedItem].value}%
                                </Text>
                                <Text style={{fontSize: 14, color: 'white'}}>{pieData[focusedItem].label}</Text>
                            </View>
                        );
                    }}
                />
            </View>
            <View style={{
                flex: 1,
                justifyContent: "center",   // vertical center
                alignItems: "center",        // horizontal center
            }}>
                {pieData.map((item, index) => (
                    <View
                        style={{
                            flex: 1,
                            flexDirection: 'row',
                            alignItems: 'center',
                            marginVertical: 3,
                            paddingHorizontal: 10,
                            width: "100%",
                            borderRadius: 8,
                            backgroundColor: '#34448B',
                        }}>
                        {renderDot(item.color)}
                        <Text key={index} style={{ color: 'white', fontSize: 16, marginVertical: 8 }}>
                            {item.label}
                        </Text>
                    </View>
                ))}
            </View>
    </View>
}