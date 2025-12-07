import { Image } from 'expo-image';
import {Pressable, Platform, StyleSheet, Text, View} from 'react-native';

import { HelloWave } from '@/components/hello-wave';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import {ExpenseDisplay} from "@/components/expenseDisplay";
import {FAB, TextInput} from "react-native-paper";
import {useCallback, useEffect, useState} from "react";
import {DatePickerInput, DatePickerModal} from "react-native-paper-dates";
import {SafeAreaView} from "react-native-safe-area-context";
import {useThemeColor} from "@/hooks/use-theme-color";
import {router, useFocusEffect, useLocalSearchParams, useNavigation, useSegments} from "expo-router";
import {usePickerStore} from "@/src/stores/category-picker-store";
import {CategoryDisplay} from "@/components/category/categoryDisplay";
import CategoryIn from "@/src/types/CategoryIn";
import {createExpense, updateExpense} from "@/src/api/expenses";
import ExpenseIn from "@/src/types/Expense";
import {login} from "@/src/api/auth";
import {useSelectedExpenseStore} from "@/src/stores/selected-expense-store";
import {createCategory, listCategories} from "@/src/api/categories";
import {useSelectedCategoryStore} from "@/src/stores/selected-category-store";
import SubcategoryIn from "@/src/types/SubcategoryIn";
import {createSubcategory, updateSubcategory} from "@/src/api/subcategories";
import {useSelectedSubcategoryStore} from "@/src/stores/selected-subcategory-store";
import CategoryOut from "@/src/types/CategoryOut";
import {useCategoriesStore} from "@/src/stores/categories-store";




export default function a() {
    const segments = useSegments();
    const selectedCategoryStore = useSelectedCategoryStore()
    const selectedSubcategoryStore = useSelectedSubcategoryStore()
    const categoriesStore = useCategoriesStore()

    const navigation = useNavigation();
    const surfaceColor = useThemeColor({}, 'surface');
    const backgroundColor = useThemeColor({}, 'background');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');

    const [title, setTitle] = useState(selectedSubcategoryStore.selected?.name || "");
    const categoryPickerStore = usePickerStore()


    return (
        <View style={{ flex: 1}}>
            <ParallaxScrollView
                headerBackgroundColor={{ light: '#A1CEDC', dark: '#1D3D47' }}
                headerImage={
                    <Image
                        source={require('@/assets/images/partial-react-logo.png')}
                    />
                }>
                <SafeAreaView
                    style={{
                        flex: 1,
                        flexDirection: 'column',
                        alignItems: 'center',
                        width: "100%",
                        padding: 20,
                        justifyContent: "space-around",
                    }}>
                    <TextInput
                        style={{
                            width: "100%",
                            marginVertical: 5,
                            backgroundColor: surfaceColor,
                            color: 'white',
                            borderRadius: 15,
                        }}
                        theme={{ colors: { primary: 'transparent'}}}
                        mode='outlined'
                        textColor={textOnSurfaceColor}
                        selectionColor='darkgray'
                        outlineColor={backgroundColor}
                        cursorColor='white'
                        placeholderTextColor={textOnSurfaceColor}
                        label={<Text style={{color: 'white'}}>Title</Text>}
                        value={title}
                        onChangeText={text => setTitle(text)}
                    />

                    <Pressable
                        style={{
                            marginVertical: 5,
                        }}
                        onPress={() => {
                            router.navigate("category/picker");
                        }}
                    >
                        <CategoryDisplay data={ categoryPickerStore.selected}></CategoryDisplay>
                    </Pressable>

                    <Pressable
                        style={{
                            width: "100%",
                            borderRadius: 8,
                            height: 50,
                            backgroundColor: surfaceColor,
                            justifyContent: 'center',
                            marginVertical: 5,
                        }}
                        onPress={async () => {
                            const subcategory = new SubcategoryIn(
                                title,
                                "EXPENSE",
                                "school",
                                categoryPickerStore.selected.id
                            )
                            try {
                                if (selectedSubcategoryStore.selected) {
                                    await updateSubcategory(selectedSubcategoryStore.selected.id, subcategory)
                                } else {
                                    await createSubcategory(subcategory)
                                }
                                const categories = await listCategories()
                                console.log(categories[0].subcategories)
                                console.log(categories[1].subcategories)
                                categoriesStore.setSelected(categories)
                                router.back();
                            } catch (e) {
                                console.error("Expense creation failed", e);
                            }

                        }}
                    >
                        <Text
                            style={{ color: textOnSurfaceColor, textAlign: 'center', justifyContent: 'center', fontSize: 17, fontWeight: 'bold' }}
                        >Save</Text>
                    </Pressable>
                </SafeAreaView>

            </ParallaxScrollView>
        </View>
    );
}
