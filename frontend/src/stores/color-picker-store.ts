import { create } from "zustand";

type ColorPickerStore = {
    selected: string | null;
    setSelected: (value: string | null | undefined) => void;
};

export const useColorPickerStore = create<ColorPickerStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));