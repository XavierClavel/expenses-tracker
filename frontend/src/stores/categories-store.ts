import { create } from "zustand";
import Category from "@/src/types/Category";
import ExpenseOut from "@/src/types/ExpenseOut";
import CategoryOut from "@/src/types/CategoryOut";

type SelectedExpenseStore = {
    selected: CategoryOut[];
    setSelected: (value: CategoryOut[]) => void;
};

export const useSelectedExpenseStore = create<SelectedExpenseStore>((set) => ({
    selected: [],
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: [] })
}));