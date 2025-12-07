import { create } from "zustand";
import CategoryIn from "@/src/types/CategoryIn";

type PickerStore = {
    selected: CategoryIn | null;
    setSelected: (value: CategoryIn | null) => void;
};

export const usePickerStore = create<PickerStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));