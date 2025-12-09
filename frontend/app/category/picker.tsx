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
import {useSelectedExpenseStore} from "@/src/stores/selected-expense-store";
import {useSelectedTypeStore} from "@/src/stores/selected-type-store";

export default function FullPickerScreen() {
  const route = useRoute<any>();
  const backgroundColor = useThemeColor({}, 'background');
  const surfaceColor = useThemeColor({}, 'surface');
  const selectedExpenseStore = useSelectedExpenseStore()
  const setPickedCategory = usePickerStore((s) => s.setSelected)
    const categoriesStore = useCategoriesStore()
    const selectedTypeStore = useSelectedTypeStore()

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
      {categoriesStore.subcategories
          .filter((it) => !selectedExpenseStore.selected || it.type == selectedTypeStore.selected).map((item) => (
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
              <CategoryDisplay data={item}></CategoryDisplay>
          </Pressable>
      ))}
        </SafeAreaView>
    </View>
  );
}
