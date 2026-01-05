import {ActivityIndicator, FlatList, Pressable, Text, useWindowDimensions, View} from "react-native";
import {router, useNavigation} from "expo-router";
import {AccountDisplay} from "@/components/account-display";
import {FAB} from "react-native-paper";
import {useThemeColor} from "@/hooks/use-theme-color";
import {useSelectedAccountStore} from "@/src/stores/selected-account-store";
import {useAccountsStore} from "@/src/stores/accounts-store";
import AccountsList from "@/app/account/list";
import {TabBar, TabView} from "react-native-tab-view";
import {useState} from "react";
import AccountBalance from "@/app/account/balance";
import {with2Decimals, withReadableThousands} from "@/src/utils/math";
import AccountCharts from "@/app/account/charts";

const routes = [
    { key: 'balance', title: 'Balance' },
    { key: 'history', title: 'History' },
    { key: 'charts', title: 'Charts' },
];

export default function AccountView() {
    const layout = useWindowDimensions();
    const [index, setIndex] = useState(0);
    const textOnBackgroundColor = useThemeColor({}, 'textOnBackground');
    const selectedAccount = useSelectedAccountStore(s => s.selected)

    const backgroundColor = useThemeColor({}, 'background');

    const renderScene = ({ route }) => {
        switch (route.key) {
            case 'balance':
                return <AccountBalance/>;
            case 'history':
                return <Text>bbbbbb</Text>;
            case 'charts':
                return <AccountCharts/>;
        }
    };

    return <View style={{ flex: 1, backgroundColor: backgroundColor, paddingTop: 50}}>
    <View
        style={{
            flex: 1,
            flexDirection: 'column',
            alignItems: 'center',
            width: "100%",
            padding: 10,
            justifyContent: "space-around"
        }}>
        <Text
            style={{
                fontSize: 25,
                fontWeight: 'bold',
                color: textOnBackgroundColor,
            }}
        >{selectedAccount?.name}</Text>
        <Text
            style={{
                fontSize: 20,
                color: textOnBackgroundColor,
            }}
        >Balance: {withReadableThousands(with2Decimals(selectedAccount?.amount))}€</Text>
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
}