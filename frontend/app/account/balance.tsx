import {Pressable, Platform, StyleSheet, Text, View, FlatList, ActivityIndicator, SectionList} from 'react-native';

import {ExpenseDisplay} from "@/components/expenseDisplay";
import {FAB} from "react-native-paper";
import {router, useNavigation} from "expo-router";
import {useThemeColor} from "@/hooks/use-theme-color";
import {useEffect, useState} from "react";
import {Section} from "@jridgewell/trace-mapping/src/types";
import {listAccountReports} from "@/src/api/account-reports";
import {useSelectedAccountStore} from "@/src/stores/selected-account-store";
import {useSelectedAccountReportStore} from "@/src/stores/selected-account-report-store";
import {with2Decimals, withReadableThousands} from "@/src/utils/math";
import AccountReportOut from "@/src/types/AccountReportOut";

export default function AccountBalance() {
    const navigation = useNavigation();
    const [accountReports, setAccountReports] = useState<AccountReportOut[]>([]);
    const [expensesSections, setExpensesSections] = useState<Section<AccountReportOut>[]>([])
    const [page, setPage] = useState(0);
    const [pageSize] = useState(50);
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const backgroundColor = useThemeColor({}, 'background');
    const selectedAccount = useSelectedAccountStore(s => s.selected)
    const setSelectedAccount = useSelectedAccountStore(s => s.setSelected)
    const selectedAccountReport = useSelectedAccountStore(s => s.selected)
    const setSelectedAccountReport = useSelectedAccountReportStore(s => s.setSelected)

    const textOnBackgroundColor = useThemeColor({}, 'textOnBackground');

    const loadAccountReports = async (pageToLoad: number) => {
        if (loading || !hasMore) return;

        setLoading(true);

        const newExpenses = await listAccountReports(selectedAccount.id, pageToLoad, pageSize);

        setAccountReports(prev => [...prev, ...newExpenses]);
        setHasMore(newExpenses.length === pageSize);
        setLoading(false);
    };

    useEffect(() => {
        if (accountReports.length > 0) return
        loadAccountReports(0);
    }, []);

    useEffect(() => {
        console.log("expenses", accountReports);

        setExpensesSections(
            accountReports.reduce((acc, expense) => {
                const date = new Intl.DateTimeFormat("fr", {
                    day: "numeric",
                    month: "long",
                    year: "numeric",
                }).format(expense.date);

                let section = acc.find(s => s.title === date);

                if (!section) {
                    section = { title: date, data: [] };
                    acc.push(section);
                }

                section.data.push(expense);
                return acc;
            }, [] as { title: string; data: AccountReportOut[] }[])
        );

        console.log("map", expensesSections)
    }, [accountReports]);


    return (
        <View style={{ flex: 1, backgroundColor: backgroundColor}}>
            <View
                style={{
                    flex: 1,
                    flexDirection: 'column',
                    alignItems: 'center',
                    width: "100%",
                    paddingHorizontal: 10,
                    justifyContent: "space-around"
                }}>
                <SectionList
                    style={{
                        width:"100%",
                        marginTop: 10,
                    }}
                    sections={expensesSections}
                    keyExtractor={(item) => item.id.toString()}
                    renderSectionHeader={({ section }) => (
                        <Text style={{ fontWeight: "bold", marginVertical: 8, color:textOnBackgroundColor }}>
                            {section.title}
                        </Text>
                    )}
                    renderItem={({ item }) => (
                        <Pressable
                            onPress={() => {
                            }}
                        >
                            <ExpenseDisplay data={item} />
                        </Pressable>
                    )}
                    onEndReached={() => {
                        console.log("end reached")
                        if (!loading && hasMore) {
                            const next = page + 1;
                            setPage(next);
                            loadAccountReports(next);
                        }
                    }}
                    onEndReachedThreshold={0.5}
                    ListFooterComponent={loading ? <ActivityIndicator/>: null}
                />
            </View>
            <FAB
                icon="plus"
                color={backgroundColor}
                style={{ position: 'absolute', bottom: 16, alignSelf: 'center', backgroundColor: 'lightgray' }}
                onPress={() => {
                    setSelectedAccountReport(null)
                    navigation.navigate('account-report/edit')
                }}
            />

        </View>)
}