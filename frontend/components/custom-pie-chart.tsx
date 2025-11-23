import {Pressable, View, type ViewProps} from 'react-native';
import { StyleSheet, Text, type TextProps } from 'react-native';
import { PieChart } from "react-native-gifted-charts";
import { PropsWithChildren, SetStateAction, useState} from 'react';



const data = [
    {value: -900.97, label: 'Accomodation & charges', color: '#009FFF', gradientCenterColor: '#006DFF'},
    {value: -736.14, label: 'Leisure', color: '#93FCF8', gradientCenterColor: '#3BE9DE'},
    {value: -268.40, label: 'Food', color: '#BDB2FA', gradientCenterColor: '#8F80F3'},
    {value: -193.38, label: 'Shopping', color: '#FFA5BA', gradientCenterColor: '#FF7F97'},
];

const total = data.reduce((accumulator, object) => {
    return accumulator + Math.abs(object.value);
}, 0);

const pieData = data.map(it => {
    return {
        value: Math.abs(it.value),
        color: it.color,
        gradientCenterColor: it.color
    }
})

console.log(pieData)


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
                    if (focusedItem == index) {
                        setFocusedItem(-1)
                    } else {
                        setFocusedItem(index)
                    }
                }}
                data={pieData}
                donut
                showGradient
                radius={120}
                innerRadius={90}
                innerCircleColor={'#232B5D'}
                centerLabelComponent={() => {
                    if (data[focusedItem] == null) {
                        return
                    }
                    return (
                        <View style={{justifyContent: 'center', alignItems: 'center'}}>
                            <Text
                                style={{fontSize: 22, color: 'white', fontWeight: 'bold'}}>
                                {data[focusedItem].value}€
                            </Text>
                            <Text style={{fontSize: 14, color: 'white'}}>{data[focusedItem].label}</Text>
                        </View>
                    );
                }}
            />
        </View>
        <View
            style={{
                flex: 1,
                flexDirection: 'row',
                alignItems: 'center',
                marginTop: 5,
                marginBottom: 50,
                marginHorizontal: 50,
                paddingHorizontal: 10,
                borderRadius: 8,
                backgroundColor: '#34448B',
                height: 50,
            }}>
            <Pressable style={{ paddingVertical: 10, backgroundColor: '#34448B', borderRadius: 8 }} onPress={
                console.log("")
            }>
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>{'<'}</Text>
            </Pressable>
            <Text style={{ color: 'white', textAlign: 'center', fontSize: 16, marginVertical: 8 }}>
                {data[focusedItem]?.label  || ''}
            </Text>
            <Pressable style={{backgroundColor: '#34448B', borderRadius: 8 }}>
                <Text style={{ color: 'white', textAlign: 'center', fontSize: 17, fontWeight: 'bold' }}>{'>'}</Text>
            </Pressable>
        </View>
            <View style={{
                flex: 1,
                justifyContent: "center",   // vertical center
                alignItems: "center",        // horizontal center
            }}>
                {data.map((item, index) => (
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
                            height: 50,
                        }}>
                        {renderDot(item.color)}
                        <Text key={index} style={{ color: 'white', fontSize: 16, marginVertical: 8 }}>
                            {item.label}
                        </Text>
                        <View style={{ flex: 1, alignItems: 'right' }}>
                            <Text key={index} style={{ textAlign:'right', color: 'white', fontWeight:'bold', fontSize: 16, marginVertical: 8 }}>
                                {item.value}€
                            </Text>
                        </View>
                    </View>
                ))}
            </View>
    </View>
}