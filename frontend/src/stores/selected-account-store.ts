import { create } from "zustand";
import AccountOut from "@/src/types/AccountOut";

type SelectedAccountStore = {
    selected: AccountOut | null;
    setSelected: (value: AccountOut | null) => void;
};

export const useSelectedAccountStore = create<SelectedAccountStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));