import { Image } from 'expo-image';
import {Platform, Pressable, StyleSheet, Text, View} from 'react-native';

import {CustomBarChart} from "@/components/custom-bar-chart";
import {useThemeColor} from "@/hooks/use-theme-color";
import React, {useEffect, useState} from "react";
import {Divider, IconButton, Modal, Portal, Provider, RadioButton, ToggleButton} from "react-native-paper";
import {Dropdown } from 'react-native-element-dropdown';
import {useDataTypeStore} from "@/src/stores/data-type-store";
import {getAccountTrendsMonth, getAccountTrendsYear, getUserTrendsMonth, getUserTrendsYear} from "@/src/api/accounts";
import {useSelectedAccountStore} from "@/src/stores/selected-account-store";
import {useSummaryDateStore} from "@/src/stores/sumary-date-store";


const colorExpense = '#da451a'
const colorIncome = '#71cc5d'


function PeriodSelector() {
    const [visible, setVisible] = React.useState(false);
    const backgroundColor = useThemeColor({}, 'background');

    const timescale = useSummaryDateStore(s => s.timescale)
    const setTimescale = useSummaryDateStore(s => s.setTimescale)

    return (
        <View style={{justifyContent: 'center'}}>
            <IconButton
                icon="calendar"
                onPress={() => setVisible(true)}
                style={{marginVertical: 0}}
            />

            <Portal>
                <Modal
                    visible={visible}
                    onDismiss={() => setVisible(false)}
                    contentContainerStyle={{
                        backgroundColor: backgroundColor,
                        padding: 20,
                        margin: 20,
                        borderRadius: 12,
                    }}
                >

                    <Text style={{color: "white", fontWeight: "bold"}}>Timescale</Text>
                    <RadioButton.Group
                        onValueChange={newValue => {
                            setTimescale(newValue)
                            setVisible(false)
                        }}
                        value={timescale}
                    >
                        <RadioButton.Item label="Month" value="month" />
                        <RadioButton.Item label="Year" value="year" />
                    </RadioButton.Group>
                </Modal>
            </Portal>
        </View>
    );
}

export default function AccountCharts() {
    const backgroundColor = useThemeColor({}, 'background');
    const surfaceColor = useThemeColor({}, 'surface');
    const textOnSurfaceColor = useThemeColor({}, 'textOnSurface');
    const setTimescale = useDataTypeStore(s => s.setTimescale)

    const selectedAccount = useSelectedAccountStore(s => s.selected)
    const timescale = useSummaryDateStore(s => s.timescale)

    const [trends, setTrends] = useState([])

    const loadAccountTrendsMonth = async () => {
        const trends = await getAccountTrendsMonth(selectedAccount?.id)
        const result = []
        for (const v of trends) {
            const date = new Date(v.year, v.month - 1)
            const currentDate = new Date()
            const displayDate= date.getFullYear() == currentDate.getFullYear() ?
                date.toLocaleString('default', { month: 'short' })
                : date.toLocaleString('default', { month: 'short', year: 'numeric' })
            const value = Number(v.balance)
            result.push({
                value: value,
                frontColor: value >= 0 ? colorIncome : colorExpense,
                label: displayDate
            })
        }
        setTrends(result)
    }

    const loadAccountTrendsYear = async () => {
        const trends = await getAccountTrendsYear(selectedAccount?.id)
        const result = []
        for (const v of trends) {
            const value = Number(v.balance)
            result.push({
                value: value,
                frontColor: value >= 0 ? colorIncome : colorExpense,
                label: v.year
            })
        }
        setTrends(result)
    }

    const loadUserTrendsMonth = async () => {
        const trends = await getUserTrendsMonth()
        const result = []
        for (const v of trends) {
            const date = new Date(v.year, v.month - 1)
            const currentDate = new Date()
            const displayDate= date.getFullYear() == currentDate.getFullYear() ?
                date.toLocaleString('default', { month: 'short' })
                : date.toLocaleString('default', { month: 'short', year: 'numeric' })
            const value = Number(v.balance)
            result.push({
                value: value,
                frontColor: value >= 0 ? colorIncome : colorExpense,
                label: displayDate
            })
        }
        setTrends(result)
    }

    const loadUserTrendsYear = async () => {
        const trends = await getUserTrendsYear()
        const result = []
        for (const v of trends) {
            const value = Number(v.balance)
            result.push({
                value: value,
                frontColor: value >= 0 ? colorIncome : colorExpense,
                label: v.year
            })
        }
        setTrends(result)
    }


    useEffect(() => {
        console.log("account", selectedAccount)
        console.log("is account null", selectedAccount == null)
        if (selectedAccount == null) {
            if (timescale == "month") {
                loadUserTrendsMonth()
            } else {
                loadUserTrendsYear()
            }
        } else {
            console.log("loading account")
            if (timescale == "month") {
                loadAccountTrendsMonth()
            } else {
                loadAccountTrendsYear()
            }
        }

    }, [timescale, selectedAccount]);

    return (
        <Provider>
      <View
        style={{
            flex: 1,
            flexDirection: 'column',
            width: "100%",
            height: "100%",
            backgroundColor: backgroundColor,
        }}>
          <PeriodSelector/>
          <View
              style={{
                  flexDirection: 'column',
                  width: "100%",
                  marginBottom: 10,
                  position: 'absolute',
                  bottom: 0,
              }}>
        <CustomBarChart data={trends} amount={1} />
          </View>
    </View>
        </Provider>
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
