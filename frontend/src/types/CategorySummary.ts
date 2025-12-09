import SubcategoryOut from "@/src/types/SubcategoryOut";

export default class CategorySummary{
    public constructor(
        public categoryId: number,
        public categoryName: string,
        public total: string,
        ) {}
}