// Fallback for using MaterialIcons on Android and web.

import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { SymbolWeight, SymbolViewProps } from 'expo-symbols';
import { ComponentProps } from 'react';
import { OpaqueColorValue, type StyleProp, type TextStyle } from 'react-native';
import {FontAwesome6, MaterialCommunityIcons} from "@expo/vector-icons";
import FontAwesome5 from "@expo/vector-icons/FontAwesome5";
import {icons} from "@/constants/icons";
import Ionicons from "@expo/vector-icons/Ionicons";

type IconMapping = Record<SymbolViewProps['name'], ComponentProps<typeof MaterialIcons>['name']>;
type IconSymbolName = keyof typeof MAPPING;

/**
 * An icon component that uses native SF Symbols on iOS, and Material Icons on Android and web.
 * This ensures a consistent look across platforms, and optimal resource usage.
 * Icon `name`s are based on SF Symbols and require manual mapping to Material Icons.
 */
export function IconSymbol({
  name,
  size = 24,
  color,
}: {
  name: string;
  size?: number;
  color: string | OpaqueColorValue;
  weight?: SymbolWeight;
}) {

    switch (name) {
        case 'house':
            return <FontAwesome6 color={color} size={17} name='house-chimney' />;
        case 'groceries':
            return <MaterialIcons color={color} size={size} name='local-grocery-store' />;
        case 'video_games':
            return <MaterialIcons color={color} size={size} name='sports-esports' />;
        case 'school':
            return <MaterialIcons color={color} size={size} name='school' />;
        case 'restaurant':
            return <MaterialIcons color={color} size={size} name='fastfood' />;
        case 'car':
            return <MaterialIcons color={color} size={size} name='directions-car' />;
        case 'baby':
            return <MaterialIcons color={color} size={size} name='child-friendly' />;
        case 'plane':
            return <MaterialCommunityIcons color={color} size={size} name='airplane' />;
        case 'trip':
            return <MaterialCommunityIcons color={color} size={size} name='island' />;
        case 'bus':
            return <FontAwesome5 color={color} size={17} name={'bus'} />;
        case 'road':
            return <FontAwesome5 color={color} size={size} name={'road'} />;
        case 'oil':
            return <FontAwesome6 color={color} size={20} name={'gas-pump'} />
        case 'train':
            return <MaterialCommunityIcons color={color} size={size} name='train' />;
        case 'clothes':
            return <FontAwesome5 color={color} size={size-3} name='tshirt' />;
        case 'gift':
            return <Ionicons color={color} size={20} name='gift' />
        case 'beer':
            return <MaterialCommunityIcons color={color} size={20} name='beer' />
        case 'electricity':
            return <MaterialIcons color={color} size={20} name='electric-bolt' />
        case 'fire':
            return <FontAwesome6 color={color} size={20} name={'fire'} />
        case 'umbrella':
            return <FontAwesome5 color={color} size={20} name='umbrella' />
        case 'wifi':
            return <FontAwesome5 color={color} size={size-3} name='wifi' />;
        case 'sim':
            return <MaterialIcons color={color} size={size} name='sim-card' />
        case 'play':
            return <FontAwesome6 color={color} size={size-3} name='play' />
        case 'work':
            return <MaterialCommunityIcons color={color} size={size} name='briefcase' />
        case 'coffee':
            return <MaterialCommunityIcons color={color} size={size} name='coffee' />
        case 'rocket':
            return <MaterialIcons color={color} size={size} name='rocket-launch' />
        case 'unknown':
            return <FontAwesome5 color={color} size={size-3} name='question' />;
    }
  return <MaterialIcons color={color} size={size} name='' />;
}
