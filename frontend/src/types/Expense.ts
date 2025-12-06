export default class ExpenseIn{
    public constructor(
        public label: string,
        public amount: string,
        public currency: string,
        public date: string,
        public categoryId: number,
    ) {}
}