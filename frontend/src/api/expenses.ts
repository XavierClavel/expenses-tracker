import axios, {AxiosInstance} from "axios";
import ExpenseIn from "@/src/types/Expense";
import {apiClient} from "@/src/api/client";
import ExpenseOut from "@/src/types/ExpenseOut";


export async function listExpenses(page: number, size: number, subcategory: number | null, from: Date | null, to: Date | null): Promise<ExpenseOut[]> {
    const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
    });
    if (subcategory != null) {
        params.append("subcategoryId", subcategory!.toString())
    }
    if (from != null) {
        params.append("from", from.toISOString().split('T')[0])
    }
    if (to != null) {
        params.append("to", to.toISOString().split('T')[0])
    }

    const response = await apiClient.get( `/expenses?${params}`);

    const expenses: ExpenseOut[] = response.data.map((e: any) =>
        new ExpenseOut(
            e.id,
            e.title,
            e.amount,
            e.currency,
            new Date(e.date),
            e.categoryId,
            e.type,
        )
    );
    return expenses
}

export async function createExpense(expense: ExpenseIn) {
    const res = await apiClient.post("/expenses", expense)
    return res.data;
}

export async function updateExpense(id: number, expense: ExpenseIn) {
    const res = await apiClient.put(`/expenses/${id}`, expense)
    return res.data;
}

export async function deleteExpense(id: number) {
    await apiClient.delete(`/expenses/${id}`)
}

export async function getOldestExpenseDate(): Promise<Date> {
    const res = await apiClient.get("/expenses/oldest")
    console.log(res.request.data)
    console.log(res.data)
    const date = res.data.date != null ? new Date(res.data.date) : new Date()
    return date;
}