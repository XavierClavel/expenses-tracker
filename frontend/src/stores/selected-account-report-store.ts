import { create } from "zustand";
import AccountReportOut from "@/src/types/AccountReportOut";

type SelectedAccountReportStore = {
    selected: AccountReportOut | null;
    setSelected: (value: AccountReportOut | null) => void;
};

export const useSelectedAccountReportStore = create<SelectedAccountReportStore>((set) => ({
    selected: null,
    setSelected: (value) => set({ selected: value }),
    reset: () => set({selected: null })
}));