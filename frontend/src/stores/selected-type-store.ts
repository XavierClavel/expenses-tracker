import { create } from "zustand";

type SelectedTypeStore = {
    selected: string | null;
    setSelected: (value: string | null | undefined) => void;
};

export const useSelectedTypeStore = create<SelectedTypeStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));