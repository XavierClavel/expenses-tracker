import { create } from "zustand";
import CategoryIn from "@/src/types/CategoryIn";
import ExpenseOut from "@/src/types/ExpenseOut";

type SelectedExpenseStore = {
    selected: ExpenseOut | null;
    setSelected: (value: ExpenseOut | null) => void;
};

export const useSelectedExpenseStore = create<SelectedExpenseStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));