import {
    Text,
    View,
    useWindowDimensions
} from 'react-native';

import {useThemeColor} from "@/hooks/use-theme-color";
import {useEffect, useState} from "react";
import {listAccounts} from "@/src/api/accounts";
import {with2Decimals, withReadableThousands} from "@/src/utils/math";
import {TabBar, TabView} from "react-native-tab-view";
import AccountsList from "@/app/account/list";
import {useAccountsStore} from "@/src/stores/accounts-store";
import AccountCharts from "@/app/account/charts";

const routes = [
    { key: 'balance', title: 'Balance' },
    { key: 'history', title: 'History' },
    { key: 'charts', title: 'Charts' },
];


export default function HomeScreen() {
    const accounts = useAccountsStore(s => s.selected)
    const setAccounts = useAccountsStore(s => s.setSelected)

    const total = accounts
        .reduce((accumulator, object) => {
            return accumulator + Number(object.amount);
        }, 0)

    const backgroundColor = useThemeColor({}, 'background');
    const layout = useWindowDimensions();
    const [index, setIndex] = useState(0);

    const textOnBackgroundColor = useThemeColor({}, 'textOnBackground');

    const loadAccounts = async (pageToLoad: number) => {
        const newAccounts = await listAccounts();
        setAccounts(newAccounts);
    };

    useEffect(() => {
        if (accounts.length > 0) return
        loadAccounts(0);
    }, []);


    const renderScene = ({ route }) => {
        switch (route.key) {
            case 'balance':
                return <AccountsList/>;
            case 'history':
                return <Text>bbbbbb</Text>;
            case 'charts':
                return <AccountCharts/>;
        }
    };

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


                <TabView
                    navigationState={{ index, routes }}
                    renderScene={renderScene}
                    onIndexChange={setIndex}
                    initialLayout={{ width: layout.width, height: 5 }}
                    renderTabBar={props => (
                        <TabBar
                            {...props}
                            indicatorStyle={{ backgroundColor: '#1976d2' }}
                            style={{ backgroundColor: backgroundColor, height: 50 }}
                            labelStyle={{ color: 'black', fontWeight: '600' }}
                        />
                    )}
                />
          </View>
      </View>
  );
}
