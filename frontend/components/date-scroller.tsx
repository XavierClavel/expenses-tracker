import React, {useEffect, useRef, useState} from "react";
import {
    FlatList,
    View,
    Text,
    Dimensions,
    StyleSheet,
} from "react-native";
import {generateMonths, generateYears} from "@/src/utils/misc";
import {useSummaryDateStore} from "@/src/stores/sumary-date-store";
import {IconButton, Menu, Modal, PaperProvider, Portal, RadioButton} from "react-native-paper";
import {useThemeColor} from "@/hooks/use-theme-color";

const { width } = Dimensions.get("window");

const ITEM_WIDTH = 160;
const SPACING = (width - ITEM_WIDTH) / 2;
const months = generateMonths(2023);
const defaultIndex = months.length - 1

export function PeriodSelector() {
    const [visible, setVisible] = React.useState(false);
    const backgroundColor = useThemeColor({}, 'background');
    const setTimescale = useSummaryDateStore(s => s.setTimescale)
    const timescale = useSummaryDateStore(s => s.timescale)

    return (
        <View style={{justifyContent: 'center'}}>
            <IconButton
                icon="calendar"
                onPress={() => setVisible(true)}
                style={{marginVertical: 0}}
            />

            <Portal>
                <Modal
                    visible={visible}
                    onDismiss={() => setVisible(false)}
                    contentContainerStyle={{
                        backgroundColor: backgroundColor,
                        padding: 20,
                        margin: 20,
                        borderRadius: 12,
                    }}
                >

                    <RadioButton.Group
                        onValueChange={newValue => {
                            setTimescale(newValue)
                            setVisible(false)
                        }}
                        value={timescale}
                    >
                        <RadioButton.Item label="Month" value="month" />
                        <RadioButton.Item label="Year" value="year" />
                    </RadioButton.Group>
                </Modal>
            </Portal>
        </View>
    );
}

export default function DateScroller() {
    const setSelectedMonth = useSummaryDateStore(s => s.setMonth)
    const setSelectedYear = useSummaryDateStore(s => s.setYear)
    const selectedMonth = useSummaryDateStore(s => s.month)
    const selectedYear = useSummaryDateStore(s => s.year)
    const oldest = useSummaryDateStore(s => s.oldest)
    const timescale = useSummaryDateStore(s => s.timescale)
    const listRef = useRef<FlatList>(null);
    let months = generateMonths(oldest);
    let years = generateYears(oldest)
    let selectedIndex = timescale == "month" ?  months.findIndex(
        it => it.month === selectedMonth && it.year === selectedYear
    ) : years.findIndex(
        it => it.year == selectedYear
    )
    const didMountRef = useRef(false);

    useEffect(() => {
        console.log("new value", oldest)
        months = generateMonths(oldest);
        years = generateYears(oldest)
        console.log("selected", selectedIndex)
    }, [oldest]);

    useEffect(() => {
        if (timescale == "year") return
        console.log(selectedMonth, new Date().getMonth() + 1)
        if (selectedYear == oldest?.getFullYear() && selectedMonth < oldest.getMonth() + 1) {
            setSelectedMonth(oldest.getMonth() + 1)
        } else if (selectedYear == new Date().getFullYear() && selectedMonth > new Date().getMonth() + 1) {
            setSelectedMonth(new Date().getMonth() + 1)
        }

    }, [timescale]);


    useEffect(() => {
        console.log("selected index modified",selectedIndex)
        if (selectedIndex < 0) return;
        if (timescale == "year" && selectedIndex > years.length) return;

        listRef.current?.scrollToIndex({
            index: selectedIndex,
            animated: didMountRef.current,
        });
        didMountRef.current = true
    }, [selectedIndex]);


    const onMomentumScrollEnd = (event: any) => {
        const index = Math.round(
            event.nativeEvent.contentOffset.x / ITEM_WIDTH
        );
        if (index == selectedIndex) return
        if (timescale == "month") {
            const value =  months[index]
            setSelectedYear(value.year)
            setSelectedMonth(value.month)
        } else {
            const value =  years[index]
            setSelectedYear(value.year)
        }

    };

    return (
        <View style={{flexDirection: 'row', alignItems: 'center', justifyContent: 'center', paddingTop: 16}}>
            <FlatList
                ref={listRef}
                data={timescale == "month" ? months : years}
                initialScrollIndex={selectedIndex}
                horizontal
                showsHorizontalScrollIndicator={false}
                snapToInterval={ITEM_WIDTH}
                decelerationRate="fast"
                contentContainerStyle={{
                    paddingHorizontal: SPACING,
                }}
                onMomentumScrollEnd={onMomentumScrollEnd}
                getItemLayout={(_, index) => ({
                    length: ITEM_WIDTH,
                    offset: ITEM_WIDTH * index,
                    index,
                })}
                renderItem={({ item, index }) => {
                    const isSelected = index === selectedIndex;

                    return (
                        <View
                            style={[
                                styles.item,
                                isSelected && styles.selectedItem,
                            ]}
                        >
                            <Text
                                style={[
                                    styles.text,
                                    isSelected && styles.selectedText,
                                ]}
                            >
                                {item.label}
                            </Text>
                        </View>
                    );
                }}
            />
            <View style={{alignItems: 'center'}}>
                <PeriodSelector />

            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    item: {
        width: ITEM_WIDTH,
        justifyContent: "center",
        alignItems: "center",
    },
    selectedItem: {
        transform: [{ scale: 1.1 }],
    },
    text: {
        fontSize: 16,
        color: "#999",
    },
    selectedText: {
        fontSize: 18,
        fontWeight: "600",
        color: "white",
    },
});
