import { create } from "zustand";
import SubcategoryOut from "@/src/types/SubcategoryOut";

type SelectedSubcategoryStore = {
    selected: SubcategoryOut | null;
    setSelected: (value: SubcategoryOut | null) => void;
};

export const useSelectedSubcategoryStore = create<SelectedSubcategoryStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));