export default class CategoryIn {
    public constructor(
        public name: string,
        public type: "EXPENSE" | "INCOME",
        public color: string,
        public icon: string) {}
}