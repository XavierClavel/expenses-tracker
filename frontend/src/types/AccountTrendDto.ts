import SubcategoryOut from "@/src/types/SubcategoryOut";
import CategorySummary from "@/src/types/CategorySummary";

export default class AccountTrendDto{
    public constructor(
        public year: number | null,
        public month: number | null,
        public balance: string,
        ) {}
}