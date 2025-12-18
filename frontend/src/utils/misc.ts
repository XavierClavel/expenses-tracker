export function generateMonths(
    startYear: number,
    endYear: number
) {
    const months = [];
    const date = new Date(startYear, 0, 1);

    while (date.getFullYear() <= endYear) {
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