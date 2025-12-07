import { create } from "zustand";
import CategoryOut from "@/src/types/CategoryOut";

type PickerStore = {
    selected: CategoryOut | null;
    setSelected: (value: CategoryOut | null) => void;
};

export const usePickerStore = create<PickerStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));