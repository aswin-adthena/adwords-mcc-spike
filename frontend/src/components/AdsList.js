import React from 'react';

const AdsList = ({ ads, customerId }) => {
  if (!ads || ads.length === 0) {
    return <p>No ads with final URLs found for the selected customer.</p>;
  }

  return (
    <div>
      <h2>Enabled Ads with Final URLs (Non-Empty)</h2>
      <p>
        <strong>Customer ID:</strong> {customerId}
      </p>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th style={tableHeaderStyle}>Ad ID</th>
            <th style={tableHeaderStyle}>Ad Name</th>
            <th style={tableHeaderStyle}>Final URLs</th>
            <th style={tableHeaderStyle}>Status</th>
          </tr>
        </thead>
        <tbody>
          {ads.map((ad) => (
            <tr key={ad.adId}>
              <td style={tableCellStyle}>{ad.adId}</td>
              <td style={tableCellStyle}>{ad.adName}</td>
              <td style={tableCellStyle}>
                <ul style={{ margin: 0, paddingLeft: '20px' }}>
                  {ad.finalUrls.map((url, index) => (
                    <li key={index}>
                      <a href={url} target="_blank" rel="noopener noreferrer">{url}</a>
                    </li>
                  ))}
                </ul>
              </td>
              <td style={tableCellStyle}>{ad.status}</td>
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

export default AdsList;
