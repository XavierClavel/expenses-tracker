export default class SubcategoryOut{
    public constructor(
        public id: number,
        public name: string,
        public type: "EXPENSE" | "INCOME",
        public color: string,
        public icon: string,
        public isDefault: boolean,
) {}
}