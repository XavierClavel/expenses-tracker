export default class ExpenseOut{
    public constructor(
        public id: number,
        public label: string,
        public amount: string,
        public currency: string,
        public date: Date,
        public categoryId: number,
    ) {}

}