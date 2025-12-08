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
import {useColorPickerStore} from "@/src/stores/color-picker-store";
import {colors} from "@/constants/colors";




export default function ColorPickerScreen() {
    const route = useRoute<any>();
    const backgroundColor = useThemeColor({}, 'background');
    const surfaceColor = useThemeColor({}, 'surface');
    const colorPickerStore = useColorPickerStore()

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
                {Object.entries(colors).map((item) => (
                    <Pressable
                        key={item[0]}
                        onPress={() => {
                            colorPickerStore.setSelected(item[0])
                            router.back()
                        }}
                        style={{
                            width: "100%"
                        }}
                    >
                        <ColorDisplay color={item[1]} label={item[0]}></ColorDisplay>
                    </Pressable>
                ))}
            </SafeAreaView>
        </View>
    );
}
