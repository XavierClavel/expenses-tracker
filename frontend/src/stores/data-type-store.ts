import {create} from "zustand";

type DataTypeStore = {
    type: string | null;
    setType: (value: string | null | undefined) => void;
    timescale: string | null;
    setTimescale: (value: string | null | undefined) => void;
};

export const useDataTypeStore = create<DataTypeStore>((set) => ({
    type: "income_expense",
    setType: (value) => set({ type: value }),
    timescale: "month",
    setTimescale: (value) => set({ timescale: value }),
}));