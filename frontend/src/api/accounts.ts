import axios, {AxiosInstance} from "axios";
import ExpenseIn from "@/src/types/Expense";
import {apiClient} from "@/src/api/client";
import ExpenseOut from "@/src/types/ExpenseOut";
import AccountOut from "@/src/types/AccountOut";
import AccountIn from "@/src/types/AccountIn";
import AccountTrendDto from "@/src/types/AccountTrendDto";


export async function listAccounts(): Promise<AccountOut[]> {
    const response = await apiClient.get( `/account`);

    const accounts: AccountOut[] = response.data.map((e: any) =>
        new AccountOut(
            e.id,
            e.name,
            e.amount,
        )
    );
    return accounts
}

export async function getAccountTrendsMonth(accountId: number) {
    const response = await apiClient.get(`/account/${accountId}/trends/month`)
    const trends: AccountTrendDto[] = response.data.map((e: any) =>
        new AccountTrendDto(
            e.year,
            e.month,
            e.balance,
        )
    );
    return trends;
}

export async function createAccount(account: AccountIn) {
    const res = await apiClient.post("/account", account)
    return res.data;
}

export async function updateAccount(id: number, account: AccountIn) {
    const res = await apiClient.put(`/account/${id}`, account)
    return res.data;
}

export async function deleteAccount(id: number) {
    const res = await apiClient.delete(`/account/${id}`)
    return res.data;
}
