import { create } from "zustand";
import AccountOut from "@/src/types/AccountOut";

type AccountsStore = {
    selected: AccountOut[];
    setSelected: (value: AccountOut[]) => void;
};

export const useAccountsStore = create<AccountsStore>((set, get) => ({
    selected: [],
    subcategories: [],
    setSelected: (value) => set({
        selected: value,
    }),
}));