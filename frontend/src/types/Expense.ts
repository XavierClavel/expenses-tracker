export default class ExpenseIn{
    public constructor(
        public title: string,
        public amount: string,
        public currency: string,
        public date: string,
        public categoryId: number | null,
        public type: "EXPENSE" | "INCOME",
    ) {}
}

