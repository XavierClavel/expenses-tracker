import {router, useNavigation} from "expo-router";
import {useThemeColor} from "@/hooks/use-theme-color";
import {useCategoriesStore} from "@/src/stores/categories-store";
import {FlatList, Pressable, Text, View} from "react-native";
import {FAB, SegmentedButtons} from "react-native-paper";
import {CategoryDisplay} from "@/components/category/categoryDisplay";
import {useSelectedCategoryStore} from "@/src/stores/selected-category-store";
import {useSelectedSubcategoryStore} from "@/src/stores/selected-subcategory-store";
import {usePickerStore} from "@/src/stores/category-picker-store";
import {useColorPickerStore} from "@/src/stores/color-picker-store";
import {useIconPickerStore} from "@/src/stores/icon-picker-store";
import {useSelectedTypeStore} from "@/src/stores/selected-type-store";
import React, {useState} from "react";
import {StandardIcon} from "@/components/standard-icon";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

export default function SubcategoriesPicker() {
    const navigation = useNavigation();
    const backgroundColor = useThemeColor({}, 'background');
    const [expanded, setExpanded] = useState<Set<number>>(new Set());
    const categoriesStore = useCategoriesStore()
    const selectedCategoryStore = useSelectedCategoryStore()
    const colorPickerStore = useColorPickerStore()
    const iconPickerStore = useIconPickerStore()
    const selectedType = useSelectedTypeStore(s => s.selected)
    const pickSubcategory = usePickerStore((s) => s.setSelected);
    const selectCategory = useSelectedCategoryStore(s => s.setSelected)


    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');


    return (
        <View style={{ flex: 1, backgroundColor: backgroundColor, paddingTop: 50}}>
            <View
                style={{
                    flex: 1,
                    flexDirection: 'column',
                    alignItems: 'center',
                    width: "100%",
                    paddingHorizontal: 10,
                    justifyContent: "space-around"
                }}>
                <FlatList
                    style={{ width: "100%" }}
                    data={categoriesStore.selected.filter((it) => it.type == selectedType)}
                    keyExtractor={(item) => item.id.toString()}
                    renderItem={({ item }) => {
                        const isExpanded = expanded.has(item.id);
                        return (
                            <View>
                                <View style={{ flexDirection: "row", alignItems: "center" }}>
                                    <Pressable
                                        style={{ flex: 1 }}
                                        onPress={() => {
                                            selectCategory(item)
                                            router.back()
                                        }}
                                    >
                                        <View
                                            style={{
                                                flexDirection: 'row',
                                                alignItems: 'center',
                                                marginVertical: 5,
                                                paddingHorizontal: 10,
                                                width: "100%",
                                                borderRadius: 8,
                                                backgroundColor: surfaceColor,
                                                height: 50,
                                            }}>
                                            <View
                                                style={{
                                                    flex: 1,
                                                    flexDirection: "row",
                                                    alignItems: "center",
                                                }}
                                            >
                                                <StandardIcon icon={item.icon} color={item.color} />
                                                <Text style={{ color: textOnSurfaceColor, fontSize: 16, marginVertical: 8 }}>
                                                    {item.name}
                                                </Text>
                                            </View>
                                        </View>
                                    </Pressable>

                                </View>
                            </View>
                        );
                    }}
                    onEndReachedThreshold={0.5}
                />

            </View>

        </View>
    );
}
