import { create } from "zustand";
import CategoryIn from "@/src/types/CategoryIn";

type BarChartAggregationStore = {
    selected: string | null;
    setSelected: (value: string | null) => void;
};

export const useBarChartAggregationStore = create<BarChartAggregationStore>((set) => ({
    selected: "total",
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));