// Fallback for using MaterialIcons on Android and web.

import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { SymbolWeight, SymbolViewProps } from 'expo-symbols';
import { ComponentProps } from 'react';
import { OpaqueColorValue, type StyleProp, type TextStyle } from 'react-native';
import {FontAwesome6, MaterialCommunityIcons} from "@expo/vector-icons";
import FontAwesome5 from "@expo/vector-icons/FontAwesome5";

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
            return <FontAwesome6 color={color} size={size -3} name='house-chimney' />;
        case 'groceries':
            return <MaterialIcons color={color} size={size} name='local-grocery-store' />;
        case 'video-games':
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
        case 'train':
            return <MaterialCommunityIcons color={color} size={size} name='train' />;
        case 'clothes':
            return <FontAwesome5 color={color} size={size-3} name='tshirt' />;
        case 'unknown':
            return <FontAwesome5 color={color} size={size-3} name='question' />;
    }
  return <MaterialIcons color={color} size={size} name='' />;
}
