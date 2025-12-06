import { View, Text, Pressable } from "react-native";
import { useRoute, useNavigation } from "@react-navigation/native";
import {useThemeColor} from "@/hooks/use-theme-color";
import {SafeAreaView} from "react-native-safe-area-context";
import {CategoryDisplay} from "@/components/category/categoryDisplay";
import {ExpenseDisplay} from "@/components/expenseDisplay";
import {router, useFocusEffect, useSegments} from "expo-router";
import {usePickerStore} from "@/src/stores/category-picker-store";
import Category from "@/src/types/Category";
import {useCallback} from "react";

const OPTIONS = [
    new Category(1, 'Accomodation & charges', '#009FFF', 'house'),
    new Category(2,  'Leisure', '#93FCF8', 'train'),
    new Category(3, 'Food', '#BDB2FA', 'groceries'),
    new Category(4, 'Shopping', '#FFA5BA', 'trip'),
    new Category(5, 'Video games', '#e1d481', 'video-games'),
    new Category(6,  'School', '#b4f1a7', 'school'),
];

export default function FullPickerScreen() {
  const route = useRoute<any>();
  const backgroundColor = useThemeColor({}, 'background');
  const surfaceColor = useThemeColor({}, 'surface');
  const setPickedCategory = usePickerStore((s) => s.setSelected)

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
      {OPTIONS.map((item) => (
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
