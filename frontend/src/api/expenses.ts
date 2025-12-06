import axios, {AxiosInstance} from "axios";
import ExpenseIn from "@/src/types/Expense";
import {apiClient} from "@/src/api/client";


export async function listExpenses() {
    const res = await apiClient.get("/expenses");
    console.log(res.data)
    return res.data;
}

export async function createExpense(expense: ExpenseIn) {
    const res = await apiClient.post("/expenses", expense)
    console.log(res.request.data)
    console.log(res.data)
    return res.data;
}