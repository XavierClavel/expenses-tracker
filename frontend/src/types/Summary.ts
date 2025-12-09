import SubcategoryOut from "@/src/types/SubcategoryOut";
import CategorySummary from "@/src/types/CategorySummary";

export default class Summary{
    public constructor(
        public year: number | null,
        public month: number | null,
        public day: number | null,
        public totalExpenses: string,
        public totalIncome: string,
        public expensesByCategory: CategorySummary[],
        public incomeByCategory: CategorySummary[],
        ) {}
}