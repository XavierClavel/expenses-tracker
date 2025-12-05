import { create } from "zustand";
import Category from "@/src/types/Category";

type PickerStore = {
    selected: Category | null;
    setSelected: (value: Category | null) => void;
};

export const usePickerStore = create<PickerStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));