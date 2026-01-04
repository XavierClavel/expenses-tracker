import axios, {AxiosInstance} from "axios";
import ExpenseIn from "@/src/types/Expense";
import {apiClient} from "@/src/api/client";
import ExpenseOut from "@/src/types/ExpenseOut";
import AccountReportOut from "@/src/types/AccountReportOut";
import AccountReportIn from "@/src/types/AccountReportIn";


export async function listAccountReports(accountId: number | null, page: number, size: number): Promise<AccountReportOut[]> {
    const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
    });
    const response = await apiClient.get( `/account-report/account/${accountId}?${params}`);

    const accounts: AccountReportOut[] = response.data.map((e: any) =>
        new AccountReportOut(
            e.id,
            e.amount,
            new Date(e.date),
        )
    );
    return accounts
}

export async function createAccountReport(accountId: number, account: AccountReportIn) {
    const res = await apiClient.post(`/account-report/account/${accountId}`, account)
    return res.data;
}

export async function updateAccountReport(id: number, account: AccountReportIn) {
    const res = await apiClient.put(`/account-report/${id}`, account)
    return res.data;
}

export async function deleteAccountReport(id: number) {
    const res = await apiClient.delete(`/account-report/${id}`)
    return res.data;
}
