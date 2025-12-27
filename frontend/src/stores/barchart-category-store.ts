import { create } from "zustand";
import CategoryIn from "@/src/types/CategoryIn";

type BarChartCategoryStore = {
    selected: number | null;
    setSelected: (value: number | null) => void;
};

export const useBarChartCategoryStore = create<BarChartCategoryStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));