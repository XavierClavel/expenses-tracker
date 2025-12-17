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

export default function HomeScreen() {
    const navigation = useNavigation();
    const backgroundColor = useThemeColor({}, 'background');
    const [expanded, setExpanded] = useState<Set<number>>(new Set());
    const categoriesStore = useCategoriesStore()
    const selectedCategoryStore = useSelectedCategoryStore()
    const selectedSubcategoryStore = useSelectedSubcategoryStore()
    const categoryPickerStore = usePickerStore()
    const colorPickerStore = useColorPickerStore()
    const iconPickerStore = useIconPickerStore()
    const [type, setType] = useState<string>("EXPENSE")
    const selectedTypeStore = useSelectedTypeStore()



    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');

    const toggleCategory = (id: number) => {
        setExpanded(prev => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    };


    return (
        <View style={{ flex: 1, backgroundColor: backgroundColor, paddingTop: 50}}>
            <SegmentedButtons
                style={{
                    margin: 5
                }}
                value={type}
                onValueChange={setType}
                buttons={[
                    {
                        value: 'EXPENSE',
                        label: 'Expense',
                    },
                    {
                        value: 'INCOME',
                        label: 'Income',
                    },
                ]}
            />
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
                    data={categoriesStore.selected.filter((it) => it.type == type)}
                    keyExtractor={(item) => item.id.toString()}
                    renderItem={({ item }) => {
                        const isExpanded = expanded.has(item.id);
                        const children = item.subcategories.filter(it => !it.isDefault);

                        return (
                            <View>
                                <View style={{ flexDirection: "row", alignItems: "center" }}>
                                    <Pressable
                                        style={{ flex: 1 }}
                                        onPress={() => {
                                            selectedCategoryStore.setSelected(item);
                                            selectedTypeStore.setSelected(item.type);
                                            colorPickerStore.setSelected(item.color);
                                            iconPickerStore.setSelected(item.icon);
                                            router.navigate("category/edit");
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
                                                {children.length > 0 && (
                                                    <Pressable
                                                        onPress={() => toggleCategory(item.id)}
                                                        style={{ marginLeft: "auto", padding: 8 }}
                                                    >
                                                        <MaterialIcons
                                                            color={'white'}
                                                            size={30}
                                                            name={expanded.has(item.id)  ? 'keyboard-arrow-up' : 'keyboard-arrow-down'} />
                                                    </Pressable>
                                                )}
                                            </View>
                                        </View>
                                    </Pressable>

                                </View>

                                {isExpanded &&
                                    children.map((child) => (
                                        <Pressable
                                            key={child.id}
                                            style={{ marginLeft: 40 }}
                                            onPress={() => {
                                                selectedSubcategoryStore.setSelected(child);
                                                categoryPickerStore.setSelected(
                                                    categoriesStore.getParent(child.id)
                                                );
                                                iconPickerStore.setSelected(child.icon);
                                                router.navigate("subcategory/edit");
                                            }}
                                        >
                                            <CategoryDisplay data={child} />
                                        </Pressable>
                                    ))}
                            </View>
                        );
                    }}
                    onEndReachedThreshold={0.5}
                />

            </View>
            <FAB
                icon="plus"
                style={{ position: 'absolute', bottom: 16, alignSelf: 'center', backgroundColor: 'lightgray' }}
                onPress={() => {
                    selectedCategoryStore.setSelected(null)
                    colorPickerStore.setSelected(null)
                    iconPickerStore.setSelected(null)
                    selectedTypeStore.setSelected("EXPENSE")
                    navigation.navigate('category/edit')
                }}
            />

        </View>
    );
}
