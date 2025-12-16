import {apiClient} from "@/src/api/client";
import CategoryOut from "@/src/types/CategoryOut";
import SubcategoryOut from "@/src/types/SubcategoryOut";
import CategoryIn from "@/src/types/CategoryIn";
import Summary from "@/src/types/Summary";
import CategorySummary from "@/src/types/CategorySummary";


export async function getYearSummary(year: number): Promise<Summary> {
    const response = await apiClient.get( `/summary/year/${year}`);
    const v = response.data
    const summary: Summary = new Summary(
        v.year,
        v.month,
        v.day,
        v.totalExpenses,
        v.totalIncome,
        v.expensesByCategory.map((category) => {
            return new CategorySummary(
                category.categoryId,
                category.categoryName,
                category.total,
            )
        }),
        v.incomeByCategory.map((category) => {
            return new CategorySummary(
                category.categoryId,
                category.categoryName,
                category.total,
            )
        }),
    )
    return summary
}

export async function getMonthSummary(year: number, month: number): Promise<Summary> {
    const response = await apiClient.get( `/summary/year/${year}/month/${month}`);
    const v = response.data
    const summary: Summary = new Summary(
        v.year,
        v.month,
        v.day,
        v.totalExpenses,
        v.totalIncome,
        v.expensesByCategory.map((category) => {
            return new CategorySummary(
                category.categoryId,
                category.categoryName,
                category.total,
            )
        }),
        v.incomeByCategory.map((category) => {
            return new CategorySummary(
                category.categoryId,
                category.categoryName,
                category.total,
            )
        }),
    )
    return summary
}
