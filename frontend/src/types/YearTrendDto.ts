import SubcategoryOut from "@/src/types/SubcategoryOut";
import CategorySummary from "@/src/types/CategorySummary";

export default class YearTrendDto{
    public constructor(
        public year: number | null,
        public month: number | null,
        public total: string,
        public average: string,
        public median: string,
        ) {}
}