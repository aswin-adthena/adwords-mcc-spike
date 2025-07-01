// Utility function to format customer ID from numeric to xxx-xxx-xxxx format
export const formatCustomerId = (customerId) => {
  if (!customerId) return '';
  const id = customerId.toString();
  if (id.length === 10) {
    return `${id.slice(0, 3)}-${id.slice(3, 6)}-${id.slice(6)}`;
  }
  return id; // Return as-is if not 10 digits
};
