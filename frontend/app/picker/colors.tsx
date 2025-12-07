import { View, Text, Pressable } from "react-native";
import { useRoute, useNavigation } from "@react-navigation/native";
import {useThemeColor} from "@/hooks/use-theme-color";
import {SafeAreaView} from "react-native-safe-area-context";
import {CategoryDisplay} from "@/components/category/categoryDisplay";
import {ExpenseDisplay} from "@/components/expenseDisplay";
import {router, useFocusEffect, useSegments} from "expo-router";
import {usePickerStore} from "@/src/stores/category-picker-store";
import CategoryIn from "@/src/types/CategoryIn";
import {useCallback} from "react";
import {useCategoriesStore} from "@/src/stores/categories-store";
import {ColorDisplay} from "@/components/category/color-display";

const colors = new Map([
    ['Blue', '#009FFF'],
    ['Light blue', '#93FCF8'],
    ['Purple', '#BDB2FA'],
    ['Red', '#FFA5BA'],
    ['Yellow', '#e1d481'],
    ['Green', '#b4f1a7'],
    ['Magenta', '#e193d9'],
    ['Light green', '#efffa5'],
    ['Brown', '#bcb8a5'],
    ['Pink', '#FFA5BA'],
])


export default function ColorPickerScreen() {
    const route = useRoute<any>();
    const backgroundColor = useThemeColor({}, 'background');
    const surfaceColor = useThemeColor({}, 'surface');
    const setPickedCategory = usePickerStore((s) => s.setSelected)
    const categoriesStore = useCategoriesStore()

    return (
        <View style={{
            flex: 1,
            padding: 8,
            backgroundColor: backgroundColor,
            flexDirection: 'column',
            alignItems: 'center',
            width: "100%",
        }}>

            <SafeAreaView>
                {colors.entries().map((item) => (
                    <Pressable
                        key={item.id}
                        onPress={() => {
                            setPickedCategory(item)
                            //navigation.goBack()
                            router.back()
                        }}
                        style={{
                            width: "100%"
                        }}
                    >
                        <ColorDisplay data={item}></ColorDisplay>
                    </Pressable>
                ))}
            </SafeAreaView>
        </View>
    );
}
