import SubcategoryOut from "@/src/types/SubcategoryOut";
import CategorySummary from "@/src/types/CategorySummary";

export default class TrendsDto{
    public constructor(
        public year: number | null,
        public month: number | null,
        public totalExpenses: string,
        public totalIncome: string,
        ) {}
}