import React, { useState } from 'react';
import axios from 'axios';

const AccountLinking = () => {
  // Manager ID is now read from application.properties
  const [clientId, setClientId] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [operation, setOperation] = useState('send'); // 'send' or 'accept'

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setResult(null);

    try {
      let response;
      if (operation === 'send') {
        response = await axios.post(
          `http://localhost:4000/api/account-links/send-invitation/${clientId}`
        );
      } else {
        response = await axios.post(
          `http://localhost:4000/api/account-links/accept-invitation/${clientId}`
        );
      }

      setResult({
        success: true,
        data: response.data
      });
    } catch (error) {
      console.error('Error:', error);
      setResult({
        success: false,
        error: error.response ? error.response.data : 'An error occurred'
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>Account Linking</h2>

      <div style={{ marginBottom: '20px' }}>
        <label style={{ marginRight: '10px' }}>
          <input
            type="radio"
            name="operation"
            value="send"
            checked={operation === 'send'}
            onChange={() => setOperation('send')}
          />
          Send Invitation (Manager to Client)
        </label>
        <label>
          <input
            type="radio"
            name="operation"
            value="accept"
            checked={operation === 'accept'}
            onChange={() => setOperation('accept')}
          />
          Accept Invitation (Client to Manager)
        </label>
      </div>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: '10px' }}>
          <p style={{ marginBottom: '10px' }}>
            <strong>Note:</strong> Manager Account ID is configured in the server's application.properties file.
          </p>
        </div>

        <div style={{ marginBottom: '15px' }}>
          <label htmlFor="clientId" style={{ display: 'block', marginBottom: '5px' }}>
            Client Account ID:
          </label>
          <input
            id="clientId"
            type="text"
            value={clientId}
            onChange={(e) => setClientId(e.target.value)}
            style={{ padding: '8px', width: '300px' }}
            required
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          style={{
            padding: '10px 20px',
            backgroundColor: '#4285F4',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: loading ? 'not-allowed' : 'pointer'
          }}
        >
          {loading ? 'Processing...' : operation === 'send' ? 'Send Invitation' : 'Accept Invitation'}
        </button>
      </form>

      {result && (
        <div style={{ marginTop: '20px', padding: '15px', backgroundColor: result.success ? '#e6f7e6' : '#ffebee', borderRadius: '4px' }}>
          <h3>{result.success ? 'Success' : 'Error'}</h3>
          <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
            {JSON.stringify(result.success ? result.data : result.error, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
};

export default AccountLinking;
