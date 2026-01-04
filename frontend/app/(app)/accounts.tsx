import { Image } from 'expo-image';
import {Pressable, Platform, StyleSheet, Text, View, FlatList, ActivityIndicator, SectionList} from 'react-native';

import { HelloWave } from '@/components/hello-wave';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import {ExpenseDisplay} from "@/components/expenseDisplay";
import {FAB} from "react-native-paper";
import {router, useNavigation} from "expo-router";
import {useThemeColor} from "@/hooks/use-theme-color";
import {getOldestExpenseDate, listExpenses} from "@/src/api/expenses";
import {useEffect, useState} from "react";
import ExpenseOut from "@/src/types/ExpenseOut";
import {SafeAreaView} from "react-native-safe-area-context";
import {useSelectedExpenseStore} from "@/src/stores/selected-expense-store";
import {listCategories} from "@/src/api/categories";
import {useCategoriesStore} from "@/src/stores/categories-store";
import {usePickerStore} from "@/src/stores/category-picker-store";
import {useSelectedTypeStore} from "@/src/stores/selected-type-store";
import {Section} from "@jridgewell/trace-mapping/src/types";
import {useSelectedCategoryStore} from "@/src/stores/selected-category-store";
import {useSelectedSubcategoryStore} from "@/src/stores/selected-subcategory-store";
import {useSummaryDateStore} from "@/src/stores/sumary-date-store";
import {listAccounts} from "@/src/api/accounts";
import AccountOut from "@/src/types/AccountOut";
import {useSelectedAccountStore} from "@/src/stores/selected-account-store";
import {AccountDisplay} from "@/components/account-display";
import {with2Decimals, withReadableThousands} from "@/src/utils/math";


export default function HomeScreen() {
    const navigation = useNavigation();
    const [accounts, setAccounts] = useState<AccountOut[]>([]);
    const [page, setPage] = useState(0);
    const [pageSize] = useState(50);
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const backgroundColor = useThemeColor({}, 'background');

    const total = accounts
        .reduce((accumulator, object) => {
            return accumulator + Number(object.amount);
        }, 0)

    const textOnBackgroundColor = useThemeColor({}, 'textOnBackground');

    const selectedAccount = useSelectedAccountStore(s => s.selected)
    const setSelectedAccount = useSelectedAccountStore(s => s.setSelected)

    const loadAccounts = async (pageToLoad: number) => {
        if (loading || !hasMore) return;

        setLoading(true);

        const newAccounts = await listAccounts(pageToLoad, pageSize);

        setAccounts(prev => [...prev, ...newAccounts]);
        setHasMore(newAccounts.length === pageSize);
        setLoading(false);
    };

    useEffect(() => {
        if (accounts.length > 0) return
        loadAccounts(0);
    }, []);

  return (
      <View style={{ flex: 1, backgroundColor: backgroundColor, paddingTop: 50}}>
        <View
            style={{
                flex: 1,
                flexDirection: 'column',
                alignItems: 'center',
                width: "100%",
                paddingHorizontal: 10,
                justifyContent: "space-around"
            }}>
            <Text
                style={{
                    fontSize: 25,
                    fontWeight: 'bold',
                    color: textOnBackgroundColor,
                }}
            >Total</Text>
            <Text
                style={{
                    fontSize: 20,
                    color: textOnBackgroundColor,
                }}
            >{withReadableThousands(with2Decimals(total))}€</Text>
            <FlatList
                style={{
                    width:"100%",
                    marginTop: 20,
                }}
                data={accounts}
                keyExtractor={(item) => item.id.toString()}
                renderItem={({ item }) => (
                    <Pressable
                        onPress={() => {
                            setSelectedAccount(item)
                            router.navigate("/account-report/list")
                        }}
                    >
                        <AccountDisplay data={item} />
                    </Pressable>
                )}
                onEndReached={() => {
                    console.log("end reached")
                    if (!loading && hasMore) {
                        const next = page + 1;
                        setPage(next);
                        loadAccounts(next);
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
                      setSelectedAccount(null)
                      navigation.navigate('account/edit')
                  }}
              />

      </View>
  );
}
