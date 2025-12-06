import axios, {AxiosInstance} from "axios";
import ExpenseIn from "@/src/types/Expense";
import {apiClient} from "@/src/api/client";
import ExpenseOut from "@/src/types/ExpenseOut";


export async function listExpenses(): Promise<ExpenseOut[]> {
    const response = await apiClient.get("/expenses");

    const expenses: ExpenseOut[] = response.data.map((e: any) =>
        new ExpenseOut(
            e.id,
            e.label,
            e.amount,
            e.currency,
            new Date(e.date),
            e.categoryId
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