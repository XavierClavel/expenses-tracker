

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

export function generateYears(
    startDate: Date,
) {
    const months = [];
    const date = new Date(startDate);
    const currentDate = new Date()

    while (date.getFullYear() <= currentDate.getFullYear()) {
        months.push({
            key: `${date.getFullYear()}`,
            label: date.toLocaleString("default", {
                year: "numeric",
            }),
            year: date.getFullYear(),
        });
        date.setFullYear(date.getFullYear() + 1);
    }

    return months;
}