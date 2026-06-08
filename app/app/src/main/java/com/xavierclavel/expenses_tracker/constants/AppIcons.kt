package com.xavierclavel.expenses_tracker.constants

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.ui.graphics.vector.ImageVector

val iconMap: Map<String, ImageVector> = mapOf(
    "house" to Icons.Default.Home,
    "groceries" to Icons.Default.ShoppingCart,
    "video_games" to Icons.Default.SportsEsports,
    "school" to Icons.Default.School,
    "restaurant" to Icons.Default.Restaurant,
    "car" to Icons.Default.DirectionsCar,
    "baby" to Icons.Default.ChildFriendly,
    "plane" to Icons.Default.Flight,
    "trip" to Icons.Default.TravelExplore,
    "bus" to Icons.Default.DirectionsBus,
    "road" to Icons.Default.Signpost,
    "oil" to Icons.Default.LocalGasStation,
    "train" to Icons.Default.Train,
    "clothes" to Icons.Default.Checkroom,
    "gift" to Icons.Default.CardGiftcard,
    "beer" to Icons.Default.SportsBar,
    "electricity" to Icons.Default.ElectricBolt,
    "fire" to Icons.Default.LocalFireDepartment,
    "water" to Icons.Default.Water,
    "umbrella" to Icons.Default.Umbrella,
    "wifi" to Icons.Default.Wifi,
    "sim" to Icons.Default.SimCard,
    "play" to Icons.Default.PlayCircle,
    "work" to Icons.Default.Work,
    "coffee" to Icons.Default.LocalCafe,
    "rocket" to Icons.Default.RocketLaunch,
    "bread" to Icons.Default.BakeryDining,
    "sofa" to Icons.Default.Weekend,
    "phone" to Icons.Default.Phone,
    "music" to Icons.Default.MusicNote,
    "book" to Icons.Default.MenuBook,
    "trend_up" to Icons.Default.TrendingUp,
    "friends" to Icons.Default.Group,
    "mobile" to Icons.Default.PhoneAndroid,
    "bank" to Icons.Default.AccountBalance,
    "cocktail" to Icons.Default.LocalBar,
    "scissors" to Icons.Default.ContentCut,
    "pill" to Icons.Default.Medication,
    "dns" to Icons.Default.Dns,
    "server" to Icons.Rounded.ContentCut,
    "unknown" to Icons.Default.HelpOutline,
)

fun iconByName(name: String?): ImageVector = iconMap[name] ?: Icons.Default.HelpOutline
