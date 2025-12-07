import SubcategoryOut from "@/src/types/SubcategoryOut";

export default class CategoryOut{
    public constructor(
        public id: number,
        public name: string,
        public color: string,
        public icon: string,
        public type: "EXPENSE" | "INCOME",
        public subcategories: SubcategoryOut[],
        ) {}
}