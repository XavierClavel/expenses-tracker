import { Image } from 'expo-image';
import {Platform, Pressable, StyleSheet, Text, View} from 'react-native';

import { Collapsible } from '@/components/ui/collapsible';
import { ExternalLink } from '@/components/external-link';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import { BarChart } from "react-native-gifted-charts";
import {CustomPieChart} from "@/components/custom-pie-chart";
import {CustomBarChart} from "@/components/custom-bar-chart";
import {useThemeColor} from "@/hooks/use-theme-color";
import {getYearSummary} from "@/src/api/summary";
import {
    getMonthCategoryTrends, getMonthSubcategoryTrends,
    getMonthTrends,
    getYearCategoryTrends, getYearFlowTrends,
    getYearSubcategoryTrends,
    getYearTrends
} from "@/src/api/trends";
import {useCategoriesStore} from "@/src/stores/categories-store";
import {useEffect, useState} from "react";
import { ToggleButton } from "react-native-paper";
import {Dropdown } from 'react-native-element-dropdown';
import {useDataTypeStore} from "@/src/stores/data-type-store";
import {data} from "browserslist";
import {useSelectedTypeStore} from "@/src/stores/selected-type-store";
import {time} from "@expo/fingerprint/cli/build/utils/log";
import {useSelectedCategoryStore} from "@/src/stores/selected-category-store";
import {useSelectedSubcategoryStore} from "@/src/stores/selected-subcategory-store";
import {colors} from "@/constants/colors";
import {router} from "expo-router";
import {CategoryDisplay} from "@/components/category/categoryDisplay";
import CategoryOut from "@/src/types/CategoryOut";
import {useBarChartAggregationStore} from "@/src/stores/barchart-aggregation-store";


const colorExpense = '#da451a'
const colorIncome = '#71cc5d'

export default function TabTwoScreen() {
    const backgroundColor = useThemeColor({}, 'background');
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');
    const [visible, setVisible] = useState(false);
    const [value, setValue] = useState<string>();
    const setDataType = useDataTypeStore(s => s.setType)
    const dataType = useDataTypeStore(s => s.type)
    const timescale = useDataTypeStore(s => s.timescale)
    const setTimescale = useDataTypeStore(s => s.setTimescale)
    const categories = useCategoriesStore(s => s.selected)
    const subcategories = useCategoriesStore(s => s.subcategories)
    const selectedCategory = useSelectedCategoryStore(s => s.selected)
    const setSelectedCategory = useSelectedCategoryStore(s => s.setSelected)
    const selectedSubcategory = useSelectedSubcategoryStore(s => s.selected)
    const setSelectedSubcategory = useSelectedSubcategoryStore(s => s.setSelected)
    const selectedAggregation = useBarChartAggregationStore(s => s.selected)
    const setSelectedAggregation = useBarChartAggregationStore(s => s.setSelected)

    const itemsType = [
        { label: 'In / Out', value: 'income_expense' },
        { label: 'Flow', value: 'flow' },
        { label: 'Category', value: 'category'},
        { label: 'Subcategory', value: 'subcategory'},
    ];

    const itemsTimescale = [
        { label: 'Month', value: 'month' },
        { label: 'Year', value: 'year' },
    ];

    const itemsAggregation = [
        {label: 'Total', value: 'total'},
        {label: 'Mean', value: 'average'},
        {label: 'Median', value: 'median'},
    ]

    const [trends, setTrends] = useState([])

    const loadMonthTrends = async () => {
        const trends = await getMonthTrends()
        const result = []
        for (const v of trends) {
            const date = new Date(v.year, v.month - 1)
            const currentDate = new Date()
            const displayDate= date.getFullYear() == currentDate.getFullYear() ?
                date.toLocaleString('default', { month: 'short' })
                : date.toLocaleString('default', { month: 'short', year: 'numeric' })
            result.push({
                value: Number(v.totalIncome),
                frontColor: colorIncome,
                spacing: 6,
                label: displayDate
            })
            result.push({
                value: Number(v.totalExpenses),
                frontColor: colorExpense,
            })
        }
        setTrends(result)
    }

    const loadMonthFlow = async () => {
        const trends = await getMonthTrends()
        const result = []
        for (const v of trends) {
            const date = new Date(v.year, v.month - 1)
            const currentDate = new Date()
            const displayDate= date.getFullYear() == currentDate.getFullYear() ?
                date.toLocaleString('default', { month: 'short' })
                : date.toLocaleString('default', { month: 'short', year: 'numeric' })
            const value = Number(v.totalIncome) - Number(v.totalExpenses)
            result.push({
                value: value,
                frontColor: value > 0 ? colorIncome :  colorExpense,
                label: displayDate
            })
        }
        setTrends(result)
    }

    const loadYearTrends = async () => {
        const trends = await getYearTrends()
        const result = []
        for (const v of trends) {
            result.push({
                value: Number(v[`${selectedAggregation}Income`]),
                frontColor: colorIncome,
                spacing: 6,
                label: v.year
            })
            result.push({
                value: Number(v[`${selectedAggregation}Expenses`]),
                frontColor: colorExpense,
            })
        }
        console.log(result)
        setTrends(result)
    }

    const loadYearFlow = async () => {
        const trends = await getYearFlowTrends()
        const result = []
        for (const v of trends) {
            const value = Number(v[selectedAggregation])
            console.log(value)
            result.push({
                value: value,
                frontColor: value > 0 ? colorIncome :  colorExpense,
                label: v.year
            })
        }
        setTrends(result)
    }

    const loadMonthCategory = async () => {
        console.log("here")
        console.log(selectedCategory)
        const trends = await getMonthCategoryTrends(selectedCategory.id)
        const result = []
        for (const v of trends) {
            const date = new Date(v.year, v.month - 1)
            const currentDate = new Date()
            const displayDate= date.getFullYear() == currentDate.getFullYear() ?
                date.toLocaleString('default', { month: 'short' })
                : date.toLocaleString('default', { month: 'short', year: 'numeric' })
            const value = Number(v.totalExpenses)
            result.push({
                value: value,
                frontColor: colors[selectedCategory?.color || 'unknown'],
                label: displayDate
            })
        }
        setTrends(result)
    }

    const loadYearCategory = async () => {
        console.log("here")
        console.log(selectedCategory)
        const trends = await getYearCategoryTrends(selectedCategory.id)
        const result = []
        for (const v of trends) {
            const value = Number(v[selectedAggregation])
            result.push({
                value: value,
                frontColor: colors[selectedCategory?.color || 'unknown'],
                label: v.year
            })
        }
        setTrends(result)
    }

    const loadMonthSubcategory = async () => {
        console.log("here")
        console.log(selectedSubcategory)
        const trends = await getMonthSubcategoryTrends(selectedSubcategory.id)
        const result = []
        for (const v of trends) {
            const date = new Date(v.year, v.month - 1)
            const currentDate = new Date()
            const displayDate= date.getFullYear() == currentDate.getFullYear() ?
                date.toLocaleString('default', { month: 'short' })
                : date.toLocaleString('default', { month: 'short', year: 'numeric' })
            const value = Number(v.totalExpenses)
            result.push({
                value: value,
                frontColor: colors[selectedSubcategory?.color || 'unknown'],
                label: displayDate
            })
        }
        setTrends(result)
    }

    const loadYearSubcategory = async () => {
        console.log("here")
        console.log(selectedSubcategory)
        const trends = await getYearSubcategoryTrends(selectedSubcategory.id)
        const result = []
        for (const v of trends) {
            const value = Number(v[selectedAggregation])
            result.push({
                value: value,
                frontColor: colors[selectedSubcategory?.color || 'unknown'],
                label: v.year
            })
        }
        setTrends(result)
    }

    useEffect(() => {
        if (dataType == "flow") {
            if (timescale == "month") {
                loadMonthFlow()
            } else {
                loadYearFlow()
            }
        } else if (dataType == "income_expense") {
            if (timescale == "month") {
                loadMonthTrends()
            } else {
                loadYearTrends()
            }
        } else if (dataType == "category" && selectedCategory != null) {
            if (timescale == "month") {
                loadMonthCategory()
            } else {
                loadYearCategory()
            }
        } else if (dataType == "subcategory" && selectedSubcategory != null) {
            if (timescale == "month") {
                loadMonthSubcategory()
            } else {
                loadYearSubcategory()
            }
        }

    }, [dataType, timescale, selectedCategory, selectedSubcategory, selectedAggregation]);

    return (
      <View
        style={{
            flex: 1,
            flexDirection: 'column',
            width: "100%",
            height: "100%",
            backgroundColor: backgroundColor,
        }}>
          <View
              style={{
                  flexDirection: 'column',
                  width: "100%",
                  marginBottom: 10,
                  position: 'absolute',
                  bottom: 0,
              }}>
        <CustomBarChart data={trends} amount={dataType == "income_expense" ? 2 : 1} />
        <View style={{
            flexDirection: 'row',
            justifyContent: 'space-evenly',
            //flex: 1,
            marginVertical: 5,
        }}>
            <View style={{ marginHorizontal: 0, width: "40%"}}>
                <Dropdown
                    style={{ height: 40, borderWidth: 1, borderRadius: 4, paddingHorizontal: 8, borderColor: "lightgray" }}
                    placeholderStyle={{ color: "lightgray" }}
                    selectedTextStyle={{ color: "lightgray" }}
                    labelField="label"
                    valueField="value"
                    placeholder="Select timescale"
                    value={timescale}
                    data={itemsTimescale}
                    onChange={item => {
                        setTimescale(item.value);
                    }}
                    dropdownPosition={"top"}
                    searchPlaceholderTextColor={'white'}
                />
            </View>

            <View style={{ paddingHorizontal: 5, width: "40%" }}>
                <Dropdown
                    style={{ height: 40, borderWidth: 1, borderRadius: 4, paddingHorizontal: 8, borderColor: "lightgray" }}
                    placeholderStyle={{ color: "lightgray" }}
                    selectedTextStyle={{ color: "lightgray" }}
                    labelField="label"
                    valueField="value"
                    placeholder="Select data"
                    value={dataType}
                    data={itemsType}
                    onChange={item => {
                        setDataType(item.value);
                    }}
                    dropdownPosition={"top"}
                />
            </View>
        </View>
              { timescale == "year" &&
              <View style={{
                  flexDirection: 'row',
                  justifyContent: 'space-evenly',
                  //flex: 1,
                  marginVertical: 0,
              }}>
              <View style={{ paddingHorizontal: 5, width: "60%" }}>
                  <Dropdown
                      style={{ height: 40, borderWidth: 1, borderRadius: 4, paddingHorizontal: 8, borderColor: "lightgray" }}
                      placeholderStyle={{ color: "lightgray" }}
                      selectedTextStyle={{ color: "lightgray" }}
                      labelField="label"
                      valueField="value"
                      placeholder="Select data"
                      value={selectedAggregation}
                      data={itemsAggregation}
                      onChange={item => {
                          setSelectedAggregation(item.value);
                      }}
                      dropdownPosition={"top"}
                  />
              </View>
              </View>
              }
              { dataType == "category" &&
              <View style={{ paddingHorizontal: 20, paddingVertical: 0 }}>
                  <Pressable
                      style={{
                          marginVertical: 5,
                      }}
                      onPress={() => {
                          router.navigate("/picker/categories");
                      }}
                  >
                      <CategoryDisplay data={ selectedCategory || new CategoryOut(-1, "No category selected", 'lightgray', 'unknown')}></CategoryDisplay>
                  </Pressable>
              </View>
              }

              { dataType == "subcategory" &&
                  <View style={{ paddingHorizontal: 20, paddingVertical: 5 }}>

                      <Pressable
                          style={{
                              marginVertical: 5,
                          }}
                          onPress={() => {
                              router.navigate("/picker/subcategories");
                          }}
                      >
                          <CategoryDisplay data={ selectedSubcategory || new CategoryOut(-1, "No category selected", 'lightgray', 'unknown')}></CategoryDisplay>
                      </Pressable>
                  </View>
              }
          </View>
    </View>
  );
}

const styles = StyleSheet.create({
  headerImage: {
    color: '#808080',
    bottom: -90,
    left: -35,
    position: 'absolute',
  },
  titleContainer: {
    flexDirection: 'row',
    gap: 8,
  },
});
