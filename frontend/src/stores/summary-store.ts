import { create } from "zustand";
import Summary from "@/src/types/Summary";

type SummaryStore = {
    selected: Summary | null;
    setSelected: (value: Summary | null | undefined) => void;
    subcategory: number | null;
    setSubcategory: (value: number) => void;
};

export const useSummaryStore = create<SummaryStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null }),
    subcategory: null,
    setSubcategory: (value) => set({subcategory: value}),
}));