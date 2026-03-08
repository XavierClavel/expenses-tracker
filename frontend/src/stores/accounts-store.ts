import { create } from "zustand";
import AccountOut from "@/src/types/AccountOut";

type AccountsStore = {
    selected: AccountOut[];
    setSelected: (value: AccountOut[]) => void;
    display: "value" | "diff" | "diff_percent";
    setDisplay: (value: "value" | "diff" | "diff_percent") => void;
};

export const useAccountsStore = create<AccountsStore>((set, get) => ({
    selected: [],
    setSelected: (value) => set({
        selected: value,
    }),
    display: "value",
    setDisplay:(value) => set({
        display: value,
    })

}));