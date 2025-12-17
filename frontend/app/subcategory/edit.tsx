import { Image } from 'expo-image';
import {Pressable, Platform, StyleSheet, Text, View, Alert} from 'react-native';

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
import {createCategory, deleteCategory, listCategories} from "@/src/api/categories";
import {useSelectedCategoryStore} from "@/src/stores/selected-category-store";
import SubcategoryIn from "@/src/types/SubcategoryIn";
import {createSubcategory, deleteSubcategory, updateSubcategory} from "@/src/api/subcategories";
import {useSelectedSubcategoryStore} from "@/src/stores/selected-subcategory-store";
import CategoryOut from "@/src/types/CategoryOut";
import {useCategoriesStore} from "@/src/stores/categories-store";
import {IconDisplay} from "@/components/category/icon-display";
import {useIconPickerStore} from "@/src/stores/icon-picker-store";




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
    const iconPickerStore = useIconPickerStore()

    const confirmDelete = () => {
        Alert.alert(
            'Confirm action',
            'Are you sure you want to delete this item?',
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Delete',
                    style: 'destructive',
                    onPress: () => {
                        deleteSubcategory(selectedSubcategoryStore.selected.id)
                        router.replace("/(app)/list");
                    },
                },
            ],
            { cancelable: true }
        );
    };


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
                            marginVertical: 5,
                        }}
                        onPress={() => {
                            router.navigate("picker/icons");
                        }}
                    >
                        <IconDisplay icon={iconPickerStore.selected}
                        ></IconDisplay>
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
                                iconPickerStore.selected,
                                categoryPickerStore.selected.id
                            )
                            try {
                                if (selectedSubcategoryStore.selected) {
                                    await updateSubcategory(selectedSubcategoryStore.selected.id, subcategory)
                                } else {
                                    await createSubcategory(subcategory)
                                }
                                const categories = await listCategories()
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
                    {selectedSubcategoryStore.selected &&
                        <Pressable
                            style={{
                                width: "100%",
                                borderRadius: 8,
                                height: 50,
                                backgroundColor: surfaceColor,
                                justifyContent: 'center',
                                marginVertical: 5,
                            }}
                            onPress={confirmDelete}
                        >
                            <Text
                                style={{ color: textOnSurfaceColor, textAlign: 'center', justifyContent: 'center', fontSize: 17, fontWeight: 'bold' }}
                            >Delete</Text>
                        </Pressable>
                    }
                </SafeAreaView>

            </ParallaxScrollView>
        </View>
    );
}
