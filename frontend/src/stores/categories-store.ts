import { create } from "zustand";
import CategoryIn from "@/src/types/CategoryIn";
import ExpenseOut from "@/src/types/ExpenseOut";
import CategoryOut from "@/src/types/CategoryOut";
import SubcategoryOut from "@/src/types/SubcategoryOut";

type CategoriesStore = {
    selected: CategoryOut[];
    setSelected: (value: CategoryOut[]) => void;
    reset: () => void;
    getSubcategories: () => SubcategoryOut[];
    getSubcategory: (id: number) => SubcategoryOut | undefined;
};

export const useCategoriesStore = create<CategoriesStore>((set, get) => ({
    selected: [],
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: [] }),
    getSubcategories: () => get().selected.flatMap((it) => it.subcategories),
    getSubcategory: (id: number) => get().selected.flatMap((it) => it.subcategories).find((it) => it.id == id)
}));