import {ActivityIndicator, FlatList, Pressable, Text, useWindowDimensions, View} from "react-native";
import {router, useNavigation} from "expo-router";
import {AccountDisplay} from "@/components/account-display";
import {FAB} from "react-native-paper";
import {useThemeColor} from "@/hooks/use-theme-color";
import {useSelectedAccountStore} from "@/src/stores/selected-account-store";
import {useAccountsStore} from "@/src/stores/accounts-store";


export default function AccountsList() {
    const navigation = useNavigation();
    const accounts = useAccountsStore(s => s.selected)

    const selectedAccount = useSelectedAccountStore(s => s.selected)
    const setSelectedAccount = useSelectedAccountStore(s => s.setSelected)

    const backgroundColor = useThemeColor({}, 'background');

    return <View
        style={{
            flex: 1,
            flexDirection: 'column',
            alignItems: 'center',
            width: "100%",
            padding: 10,
            justifyContent: "space-around"
        }}>
    <FlatList
        style={{
            width:"100%",
        }}
        data={accounts}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => (
            <Pressable
                onPress={() => {
                    setSelectedAccount(item)
                    router.navigate("/account/view")
                }}
            >
                <AccountDisplay data={item} />
            </Pressable>
        )}
        onEndReachedThreshold={0.5}
    />
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
}