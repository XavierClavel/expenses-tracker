import { create } from "zustand";
import CategoryIn from "@/src/types/CategoryIn";
import ExpenseOut from "@/src/types/ExpenseOut";
import CategoryOut from "@/src/types/CategoryOut";
import SubcategoryOut from "@/src/types/SubcategoryOut";

type CategoriesStore = {
    selected: CategoryOut[];
    setSelected: (value: CategoryOut[]) => void;
    reset: () => void;
    subcategories: SubcategoryOut[];
    getCategory: (id: number) => CategoryOut | undefined;
    getSubcategory: (id: number) => SubcategoryOut | undefined;
    getParent: (id: number) => CategoryOut | undefined;
    getChildren: (id: number) => SubcategoryOut[];
};

export const useCategoriesStore = create<CategoriesStore>((set, get) => ({
    selected: [],
    subcategories: [],
    setSelected: (value) => set({
        selected: value,
        subcategories: value.flatMap((it) => it.subcategories)
    }),
    reset: () => set({
        selected: [],
        subcategories: [],
    }),
    getCategory: (id: number) => get().selected.find(it => it.id == id),
    getSubcategory: (id: number) => get().subcategories.find((it) => it.id == id),
    getParent: (id: number) => get().selected.find((it) => it.subcategories.find((it) => it.id == id)),
    getChildren: (id: number) => get().getCategory(id)?.subcategories || [],
}));