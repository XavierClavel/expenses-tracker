import {apiClient} from "@/src/api/client";
import CategoryOut from "@/src/types/CategoryOut";
import SubcategoryOut from "@/src/types/SubcategoryOut";
import CategoryIn from "@/src/types/CategoryIn";
import Summary from "@/src/types/Summary";
import CategorySummary from "@/src/types/CategorySummary";
import TrendsDto from "@/src/types/TrendsDto";


export async function getMonthTrends(): Promise<TrendsDto[]> {
    const response = await apiClient.get( `/trends/month`);
    const result = response.data.map((it) => {
        return new TrendsDto(
            it.year,
            it.month,
            it.totalExpenses,
            it.totalIncome,
        )
    })
    return result
}

export async function getYearTrends(): Promise<TrendsDto[]> {
    const response = await apiClient.get( `/trends/year`);
    const result = response.data.map((it) => {
        return new TrendsDto(
            it.year,
            it.month,
            it.totalExpenses,
            it.totalIncome,
        )
    })
    return result
}