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

const OPTIONS = [
    new CategoryIn(1, 'Accomodation & charges', '#009FFF', 'house'),
    new CategoryIn(2,  'Leisure', '#93FCF8', 'train'),
    new CategoryIn(3, 'Food', '#BDB2FA', 'groceries'),
    new CategoryIn(4, 'Shopping', '#FFA5BA', 'trip'),
    new CategoryIn(5, 'Video games', '#e1d481', 'video-games'),
    new CategoryIn(6,  'School', '#b4f1a7', 'school'),
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
