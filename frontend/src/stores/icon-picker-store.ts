import { create } from "zustand";

type IconPickerStore = {
    selected: string | null;
    setSelected: (value: string | null | undefined) => void;
};

export const useIconPickerStore = create<IconPickerStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));