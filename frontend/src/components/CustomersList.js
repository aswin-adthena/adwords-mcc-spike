import React from 'react';
import { formatCustomerId } from '../utils/formatters';

const CustomersList = ({ customers }) => {
  if (!customers || customers.length === 0) {
    return <p>No Google Ads customers found.</p>;
  }

  return (
    <div>
      <h2>Your Google Ads Customers</h2>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th style={tableHeaderStyle}>Customer ID</th>
            <th style={tableHeaderStyle}>Resource Name</th>
            <th style={tableHeaderStyle}>Access Level</th>
          </tr>
        </thead>
        <tbody>
          {customers.map((customer) => (
            <tr key={customer.customerId}>
              <td style={tableCellStyle}>{formatCustomerId(customer.customerId)}</td>
              <td style={tableCellStyle}>{customer.resourceName}</td>
              <td style={tableCellStyle}>{customer.accessRole || 'Unknown'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

const tableHeaderStyle = {
  backgroundColor: '#f2f2f2',
  padding: '10px',
  textAlign: 'left',
  borderBottom: '1px solid #ddd'
};

const tableCellStyle = {
  padding: '10px',
  borderBottom: '1px solid #ddd'
};

export default CustomersList;
