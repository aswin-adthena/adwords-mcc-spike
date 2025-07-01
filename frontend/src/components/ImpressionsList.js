import React from 'react';

const ImpressionsList = ({ impressions, customerId }) => {
  if (!impressions || impressions.length === 0) {
    return <p>No impression data found for the selected customer.</p>;
  }

  return (
    <div>
      <h2>Impressions by Country (Last 7 Days)</h2>
      <p>
        <strong>Customer ID:</strong> {customerId}
      </p>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th style={tableHeaderStyle}>Country Criterion ID</th>
            <th style={tableHeaderStyle}>Impressions</th>
          </tr>
        </thead>
        <tbody>
          {impressions.map((impression) => (
            <tr key={impression.countryCriterionId}>
              <td style={tableCellStyle}>{impression.countryCriterionId}</td>
              <td style={tableCellStyle}>{impression.impressions.toLocaleString()}</td>
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

export default ImpressionsList;
