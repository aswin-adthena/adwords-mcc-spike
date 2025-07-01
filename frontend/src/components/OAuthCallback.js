import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';

const OAuthCallback = ({ setToken }) => {
  const [loading, setLoading] = useState(true);
  const [loadingMessage, setLoadingMessage] = useState('Authenticating...');
  const [error, setError] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const exchangeCodeForToken = async () => {
      try {
        // Get the authorization code from the URL query parameters
        const urlParams = new URLSearchParams(location.search);
        const code = urlParams.get('code');

        if (!code) {
          setError('No authorization code found in the URL');
          setLoading(false);
          return;
        }

        setLoadingMessage('Exchanging authorization code for tokens...');
        console.log('Authorization code:', code);

        // Exchange the code for tokens (access token and refresh token)
        const response = await axios.post('http://localhost:4000/api/auth/token', { code });
        console.log('Token response:', response.data);

        // Set the access token in the parent component
        setToken(response.data.access_token);

        setLoadingMessage('Authentication successful! Redirecting...');

        // Short delay before redirecting to show the success message
        setTimeout(() => {
          // Navigate to the main page
          navigate('/');
        }, 1500);
      } catch (err) {
        console.error('Error exchanging code for token:', err);
        setError(`Failed to authenticate with Google: ${err.response?.data?.error || err.message}`);
        setLoading(false);
      }
    };

    exchangeCodeForToken();
  }, [location, navigate, setToken]);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', marginTop: '50px' }}>
        <h2>{loadingMessage}</h2>
        <div style={{ margin: '30px auto', width: '50px', height: '50px', border: '5px solid #f3f3f3', borderTop: '5px solid #4285F4', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></div>
        <style>{`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}</style>
        <p>Please wait while we complete the authentication process.</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ textAlign: 'center', marginTop: '50px' }}>
        <h2 style={{ color: 'red' }}>Authentication Error</h2>
        <p>{error}</p>
        <button
          onClick={() => navigate('/')}
          style={{
            backgroundColor: '#4285F4',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            padding: '10px 20px',
            fontSize: '16px',
            cursor: 'pointer',
            marginTop: '20px'
          }}
        >
          Return to Home
        </button>
      </div>
    );
  }

  return null;
};

export default OAuthCallback;
