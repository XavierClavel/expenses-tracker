import { Image } from 'expo-image';
import {Alert, Pressable, Text, View} from 'react-native';

import ParallaxScrollView from '@/components/parallax-scroll-view';
import {FAB, SegmentedButtons, TextInput} from "react-native-paper";
import {useState} from "react";
import {SafeAreaView} from "react-native-safe-area-context";
import {useThemeColor} from "@/hooks/use-theme-color";
import {router, useNavigation, useSegments} from "expo-router";
import {usePickerStore} from "@/src/stores/category-picker-store";
import CategoryIn from "@/src/types/CategoryIn";
import {createCategory, deleteCategory, updateCategory} from "@/src/api/categories";
import {useSelectedCategoryStore} from "@/src/stores/selected-category-store";
import {useSelectedSubcategoryStore} from "@/src/stores/selected-subcategory-store";
import {ColorDisplay} from "@/components/category/color-display";
import {useColorPickerStore} from "@/src/stores/color-picker-store";
import {colors} from "@/constants/colors";
import {IconDisplay} from "@/components/category/icon-display";
import {useIconPickerStore} from "@/src/stores/icon-picker-store";
import {useSelectedTypeStore} from "@/src/stores/selected-type-store";
import {deleteExpense} from "@/src/api/expenses";




export default function a() {
    const segments = useSegments();
    const selectedCategoryStore = useSelectedCategoryStore()
    const selectedSubcategoryStore = useSelectedSubcategoryStore()
    const selectedTypeStore = useSelectedTypeStore()
    const categoryPickerStore = usePickerStore()
    const colorPickerStore = useColorPickerStore()
    const iconPickerStore = useIconPickerStore()

    const navigation = useNavigation();
    const surfaceColor = useThemeColor({}, 'surface');
    const backgroundColor = useThemeColor({}, 'background');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');

    const [title, setTitle] = useState(selectedCategoryStore.selected?.name || "");

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
                        deleteCategory(selectedCategoryStore.selected.id)
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
            <SegmentedButtons
                value={selectedTypeStore.selected}
                onValueChange={selectedTypeStore.setSelected}
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
                    router.navigate("picker/colors");
                }}
            >
                <ColorDisplay
                    color={ colorPickerStore.selected ? colors[colorPickerStore.selected] : 'lightgray'}
                    label={colorPickerStore.selected ? colorPickerStore.selected : 'No color selected'}
                ></ColorDisplay>
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
                    selectedSubcategoryStore.setSelected(null)
                    categoryPickerStore.setSelected(selectedCategoryStore.selected)
                    router.navigate("subcategory/edit");
                }}
            >
                <Text
                    style={{ color: textOnSurfaceColor, textAlign: 'center', justifyContent: 'center', fontSize: 17, fontWeight: 'bold' }}
                >New subcategory</Text>
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
                      const category = new CategoryIn(
                          title,
                          selectedCategoryStore.selected?.type || selectedTypeStore.selected, //No updating type of existing category
                          colorPickerStore.selected,
                          iconPickerStore.selected,
                      )
                      try {
                          if (selectedCategoryStore.selected) {
                              await updateCategory(selectedCategoryStore.selected.id, category)
                          } else {
                              await createCategory(category)
                          }
                          router.replace("/(app)/list");
                      } catch (e) {
                          console.error("Category creation failed", e);
                      }

                  }}
              >
                    <Text
                        style={{ color: textOnSurfaceColor, textAlign: 'center', justifyContent: 'center', fontSize: 17, fontWeight: 'bold' }}
                  >Save</Text>
              </Pressable>
            {selectedCategoryStore.selected &&
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
