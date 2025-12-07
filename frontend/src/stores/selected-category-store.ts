import { create } from "zustand";
import CategoryIn from "@/src/types/CategoryIn";
import CategoryOut from "@/src/types/CategoryOut";

type SelectedCategoryStore = {
    selected: CategoryOut | null;
    setSelected: (value: CategoryOut | null) => void;
};

export const useSelectedCategoryStore = create<SelectedCategoryStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));