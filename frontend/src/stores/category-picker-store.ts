import { create } from "zustand";
import SubcategoryOut from "@/src/types/SubcategoryOut";

type PickerStore = {
    selected: SubcategoryOut | null;
    setSelected: (value: SubcategoryOut | null | undefined) => void;
};

export const usePickerStore = create<PickerStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));