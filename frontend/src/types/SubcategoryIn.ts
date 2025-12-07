export default class SubcategoryIn{
    public constructor(
        public name: string,
        public type: "EXPENSE" | "INCOME",
        public icon: string,
        public parentCategory: number,
) {}
}