import {apiClient} from "@/src/api/client";
import CategoryOut from "@/src/types/CategoryOut";
import SubcategoryOut from "@/src/types/SubcategoryOut";
import CategoryIn from "@/src/types/CategoryIn";
import Summary from "@/src/types/Summary";
import CategorySummary from "@/src/types/CategorySummary";
import TrendsDto from "@/src/types/TrendsDto";
import YearTrendDto from "@/src/types/YearTrendDto";


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
    console.log(result)
    return result
}

export async function getMonthCategoryTrends(id: number): Promise<TrendsDto[]> {
    const response = await apiClient.get( `/trends/category/${id}/month`);
    const result = response.data.map((it) => {
        return new TrendsDto(
            it.year,
            it.month,
            it.total,
            it.total,
        )
    })
    console.log(response.data)
    return result
}

export async function getYearCategoryTrends(id: number): Promise<YearTrendDto[]> {
    const response = await apiClient.get( `/trends/category/${id}/year`);
    const result = response.data.map((it) => {
        return new YearTrendDto(
            it.year,
            it.month,
            it.total,
            it.average,
            it.median,
        )
    })
    return result
}

export async function getMonthSubcategoryTrends(id: number): Promise<TrendsDto[]> {
    const response = await apiClient.get( `/trends/subcategory/${id}/month`);
    const result = response.data.map((it) => {
        return new TrendsDto(
            it.year,
            it.month,
            it.total,
            it.total,
        )
    })
    console.log(response.data)
    return result
}

export async function getYearSubcategoryTrends(id: number): Promise<YearTrendDto[]> {
    const response = await apiClient.get( `/trends/subcategory/${id}/year`);
    const result = response.data.map((it) => {
        return new YearTrendDto(
            it.year,
            it.month,
            it.total,
            it.average,
            it.median,
        )
    })
    return result
}

export async function getYearFlowTrends(): Promise<YearTrendDto[]> {
    const response = await apiClient.get( `/trends/flow/year`);
    console.log(response.data)
    const result = response.data.map((it) => {
        return new YearTrendDto(
            it.year,
            it.month,
            it.total,
            it.average,
            it.median,
        )
    })
    return result
}