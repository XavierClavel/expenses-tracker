export function generateMonths(
    startDate: Date,
) {
    const months = [];
    const date = new Date(startDate);
    const currentDate = new Date()

    while (date <= currentDate) {
        months.push({
            key: `${date.getFullYear()}-${date.getMonth()}`,
            label: date.toLocaleString("default", {
                month: "long",
                year: "numeric",
            }),
            year: date.getFullYear(),
            month: date.getMonth()+1,
        });
        date.setMonth(date.getMonth() + 1);
    }

    return months;
}