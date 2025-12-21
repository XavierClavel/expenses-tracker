import { create } from "zustand";
import CategoryIn from "@/src/types/CategoryIn";
import CategoryOut from "@/src/types/CategoryOut";

type SelectedCategoryStore = {
    year: number;
    month: number;
    setYear: (value: number) => void;
    setMonth: (value: number) => void;
};

export const useSummaryDateStore = create<SelectedCategoryStore>((set) => ({
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1,
    setYear: (value) => set({year: value}),
    setMonth: (value) => set({month: value}),
}));