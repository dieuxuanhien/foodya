export function formatVndCurrency(amount: number | null | undefined) {
  const rounded = Math.round(Number(amount ?? 0));
  const sign = rounded < 0 ? '-' : '';
  const digits = Math.abs(rounded).toString();
  const groups: string[] = [];

  for (let end = digits.length; end > 0; end -= 3) {
    const start = Math.max(0, end - 3);
    groups.unshift(digits.slice(start, end));
  }

  return `${sign}${groups.join('.')} đ`;
}
