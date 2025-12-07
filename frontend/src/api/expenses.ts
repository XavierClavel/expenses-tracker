import axios, {AxiosInstance} from "axios";
import ExpenseIn from "@/src/types/Expense";
import {apiClient} from "@/src/api/client";
import ExpenseOut from "@/src/types/ExpenseOut";


export async function listExpenses(page: number, size: number): Promise<ExpenseOut[]> {
    const response = await apiClient.get( `/expenses?page=${page}&size=${size}`);

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

    console.log(expenses)

    return expenses
}

export async function createExpense(expense: ExpenseIn) {
    const res = await apiClient.post("/expenses", expense)
    console.log(res.request.data)
    console.log(res.data)
    return res.data;
}