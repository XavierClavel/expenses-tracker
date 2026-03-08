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
import {useAccountsStore} from "@/src/stores/accounts-store";


const colorExpense = '#da451a'
const colorIncome = '#71cc5d'


function PeriodSelector() {
    const [visible, setVisible] = React.useState(false);
    const backgroundColor = useThemeColor({}, 'background');

    const timescale = useSummaryDateStore(s => s.timescale)
    const setTimescale = useSummaryDateStore(s => s.setTimescale)

    const display = useAccountsStore(s => s.display)
    const setDisplay = useAccountsStore(s => s.setDisplay)

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
                        }}
                        value={timescale}
                    >
                        <RadioButton.Item label="Month" value="month" />
                        <RadioButton.Item label="Year" value="year" />
                    </RadioButton.Group>

                    <Text style={{color: "white", fontWeight: "bold"}}>Display</Text>
                    <RadioButton.Group
                        onValueChange={newValue => {
                            setDisplay(newValue)
                        }}
                        value={display}
                    >
                        <RadioButton.Item label="Value" value="value" />
                        <RadioButton.Item label="Change" value="diff" />
                        <RadioButton.Item label="Change percent" value="diff_percent" />

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

    const selectedAccount = useSelectedAccountStore(s => s.selected)
    const timescale = useSummaryDateStore(s => s.timescale)
    const display = useAccountsStore(s => s.display)


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
            const key = display == "value" ? "balance" : display == "diff" ? "change" : "proportionalChange"
            let value = Number(v[key])
            if (key == "proportionalChange") {
                value *= 100
            }
            console.log(key, value)
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
            const key = display == "value" ? "balance" : display == "diff" ? "change" : "proportionalChange"
            let value = Number(v[key])
            if (key == "proportionalChange") {
                value *= 100
            }
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
            const key = display == "value" ? "balance" : display == "diff" ? "change" : "proportionalChange"
            let value = Number(v[key])
            if (key == "proportionalChange") {
                value *= 100
            }
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
            const key = display == "value" ? "balance" : display == "diff" ? "change" : "proportionalChange"
            let value = Number(v[key])
            if (key == "proportionalChange") {
                value *= 100
            }
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

    }, [timescale, selectedAccount, display]);

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
        <CustomBarChart data={trends} amount={1} suffix={display == "diff_percent" ? "%" : "€"} />
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
