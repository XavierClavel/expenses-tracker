export default class ExpenseOut{
    public constructor(
        public id: number,
        public title: string,
        public amount: string,
        public currency: string,
        public date: Date,
        public categoryId: number,
        public type: "EXPENSE" | "INCOME",
    ) {}

}