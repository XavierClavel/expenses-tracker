package com.xavierclavel.bankable.constants

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ChildFriendly
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Signpost
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.SportsBar
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

val iconMap: Map<String, ImageVector> = mapOf(
    "house" to Icons.Outlined.Home,
    "groceries" to Icons.Outlined.ShoppingCart,
    "video_games" to Icons.Outlined.SportsEsports,
    "school" to Icons.Outlined.School,
    "restaurant" to Icons.Outlined.Restaurant,
    "car" to Icons.Outlined.DirectionsCar,
    "baby" to Icons.Outlined.ChildFriendly,
    "plane" to Icons.Outlined.Flight,
    "trip" to Icons.Outlined.Luggage,
    "bus" to Icons.Outlined.DirectionsBus,
    "road" to Icons.Outlined.Route,
    "oil" to Icons.Outlined.LocalGasStation,
    "train" to Icons.Outlined.Train,
    "clothes" to Icons.Outlined.Checkroom,
    "gift" to Icons.Outlined.CardGiftcard,
    "beer" to Icons.Outlined.SportsBar,
    "electricity" to Icons.Outlined.ElectricBolt,
    "fire" to Icons.Outlined.LocalFireDepartment,
    "water" to Icons.Outlined.WaterDrop,
    "umbrella" to Icons.Outlined.Umbrella,
    "wifi" to Icons.Outlined.Wifi,
    "sim" to Icons.Outlined.SimCard,
    "play" to Icons.Outlined.PlayCircle,
    "work" to Icons.Outlined.Work,
    "coffee" to Icons.Outlined.LocalCafe,
    "rocket" to Icons.Outlined.RocketLaunch,
    "bread" to Icons.Outlined.BakeryDining,
    "sofa" to Icons.Outlined.Weekend,
    "phone" to Icons.Outlined.Phone,
    "computer" to Icons.Outlined.Computer,
    "music" to Icons.Outlined.MusicNote,
    "book" to Icons.AutoMirrored.Outlined.MenuBook,
    "trend_up" to Icons.AutoMirrored.Outlined.TrendingUp,
    "friends" to Icons.Outlined.Group,
    "mobile" to Icons.Outlined.PhoneAndroid,
    "bank" to Icons.Outlined.AccountBalance,
    "cocktail" to Icons.Outlined.LocalBar,
    "scissors" to Icons.Outlined.ContentCut,
    "pill" to Icons.Outlined.Medication,
    "code" to Icons.Rounded.Terminal,
    "bike" to Icons.AutoMirrored.Rounded.DirectionsBike,
    "sport" to Icons.Outlined.SportsTennis,
    "art" to Icons.Outlined.ColorLens,
    "transfer" to Icons.Outlined.SwapHoriz,
    "movie" to Icons.Outlined.Movie,
    "camera" to Icons.Outlined.CameraAlt,
    "unknown" to Icons.AutoMirrored.Outlined.HelpOutline,
)

fun iconByName(name: String?): ImageVector = iconMap[name] ?: Icons.Outlined.HelpOutline
