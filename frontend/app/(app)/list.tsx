import {router, useNavigation} from "expo-router";
import {useEffect, useState} from "react";
import ExpenseOut from "@/src/types/ExpenseOut";
import {useThemeColor} from "@/hooks/use-theme-color";
import {useSelectedExpenseStore} from "@/src/stores/selected-expense-store";
import {useCategoriesStore} from "@/src/stores/categories-store";
import {listExpenses} from "@/src/api/expenses";
import {listCategories} from "@/src/api/categories";
import {ActivityIndicator, FlatList, Pressable, View} from "react-native";
import {ExpenseDisplay} from "@/components/expenseDisplay";
import {FAB} from "react-native-paper";
import {CategoryDisplay} from "@/components/category/categoryDisplay";
import {useSelectedCategoryStore} from "@/src/stores/selected-category-store";
import {useSelectedSubcategoryStore} from "@/src/stores/selected-subcategory-store";
import {usePickerStore} from "@/src/stores/category-picker-store";
import {useColorPickerStore} from "@/src/stores/color-picker-store";
import {useIconPickerStore} from "@/src/stores/icon-picker-store";

export default function HomeScreen() {
    const navigation = useNavigation();
    const backgroundColor = useThemeColor({}, 'background');
    const categoriesStore = useCategoriesStore()
    const selectedCategoryStore = useSelectedCategoryStore()
    const selectedSubcategoryStore = useSelectedSubcategoryStore()
    const categoryPickerStore = usePickerStore()
    const colorPickerStore = useColorPickerStore()
    const iconPickerStore = useIconPickerStore()


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
                    style={{
                        width: "100%",
                    }}
                    data={categoriesStore.selected}
                    keyExtractor={(item) => item.id.toString()}
                    renderItem={({ item }) =>
                        <View>
                        <Pressable
                            key={item.id}
                            onPress={() => {
                                selectedCategoryStore.setSelected(item)
                                colorPickerStore.setSelected(item.color)
                                iconPickerStore.setSelected(item.icon)
                                router.navigate("category/edit");
                            }}
                        >
                            <CategoryDisplay data={item}/>
                        </Pressable>
                            {item.subcategories.filter((it) => !it.isDefault).map((item) => (
                                <Pressable
                                    key={item.id}
                                    style={{marginLeft: 40}}
                                    onPress={() => {
                                        selectedSubcategoryStore.setSelected(item)
                                        categoryPickerStore.setSelected(categoriesStore.getParent(item.id))
                                        iconPickerStore.setSelected(item.icon)
                                        router.navigate("subcategory/edit");
                                    }}
                                >
                                <CategoryDisplay key={item.id} data={item}/>
                                </Pressable>
                            ))
                            }
                        </View>
}
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
                    navigation.navigate('category/edit')
                }}
            />

        </View>
    );
}
