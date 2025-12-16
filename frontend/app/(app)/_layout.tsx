import { Tabs } from 'expo-router';
import React from 'react';

import { HapticTab } from '@/components/haptic-tab';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import Ionicons from '@expo/vector-icons/Ionicons';
import FontAwesome5 from '@expo/vector-icons/FontAwesome5';
import {ThemeProvider} from "@react-navigation/core";
import {DarkTheme} from "@react-navigation/native";
import {DefaultTheme} from "react-native-paper";

export default function TabLayout() {
  const colorScheme = useColorScheme();

  return (
      <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <Tabs
      screenOptions={{
        tabBarActiveTintColor: 'white',
        headerShown: false,
        tabBarButton: HapticTab,
      }}>
        <Tabs.Screen
            name="index"
            options={{
                title: 'Expenses',
                tabBarIcon: ({ color }) => <FontAwesome5 size={28} name="receipt" color={color} />,
            }}
        />
          <Tabs.Screen
              name="list"
              options={{
                  title: 'Categories',
                  tabBarIcon: ({ color }) => <FontAwesome size={28} name="folder" color={color} />,
              }}
          />
      <Tabs.Screen
        name="summary"
        options={{
          title: 'Report',
          tabBarIcon: ({ color }) => <FontAwesome size={28} name="pie-chart" color={color} />,
        }}
      />
      <Tabs.Screen
        name="explore"
        options={{
          title: 'Trend',
          tabBarIcon: ({ color }) => <Ionicons size={28} name="stats-chart" color={color} />,
        }}
      />
    </Tabs>
      </ThemeProvider>
  );
}
