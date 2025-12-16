export function withReadableThousands(x: number) {
    if (x == null) {
        return null
    }
    const parts = x.toString().split(".");
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, " ");
    return parts.join(".");
}

export function with2Decimals(x: number) {
    return (Math.round(x * 100) / 100).toFixed(2)
}

export function withNoDecimals(x: number) {
    return Math.round(x).toFixed(0)
}