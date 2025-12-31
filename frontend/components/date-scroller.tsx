import React, {useEffect, useRef, useState} from "react";
import {
    FlatList,
    View,
    Text,
    Dimensions,
    StyleSheet,
} from "react-native";
import {generateMonths} from "@/src/utils/misc";
import {useSummaryDateStore} from "@/src/stores/sumary-date-store";

const { width } = Dimensions.get("window");

const ITEM_WIDTH = 160;
const SPACING = (width - ITEM_WIDTH) / 2;
const months = generateMonths(2023);
const defaultIndex = months.length - 1


export default function DateScroller() {
    const setSelectedMonth = useSummaryDateStore(s => s.setMonth)
    const setSelectedYear = useSummaryDateStore(s => s.setYear)
    const selectedMonth = useSummaryDateStore(s => s.month)
    const selectedYear = useSummaryDateStore(s => s.year)
    const listRef = useRef<FlatList>(null);
    const selectedIndex = months.findIndex(
        it => it.month === selectedMonth && it.year === selectedYear
    );
    const didMountRef = useRef(false);

    useEffect(() => {
        if (selectedIndex < 0) return;

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
        const value = months[index]
        setSelectedYear(value.year)
        setSelectedMonth(value.month)
    };

    return (
        <View>
            <FlatList
                ref={listRef}
                data={months}
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
        </View>
    );
}

const styles = StyleSheet.create({
    item: {
        width: ITEM_WIDTH,
        justifyContent: "center",
        alignItems: "center",
        paddingTop: 16,
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
