import React, { useState, useEffect, useCallback } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import LoginButton from './components/LoginButton';
import CustomersList from './components/CustomersList';
import CustomerHierarchy from './components/CustomerHierarchy';
import CompleteMccHierarchy from './components/CompleteMccHierarchy';
import ImpressionsList from './components/ImpressionsList';
import AdsList from './components/AdsList';
import AccountLinking from './components/AccountLinking';
import OAuthCallback from './components/OAuthCallback';
import axios from 'axios';

function App() {
  const [token, setToken] = useState(null);

  const [customers, setCustomers] = useState([]);
  const [customerHierarchy, setCustomerHierarchy] = useState([]);
  const [completeMccHierarchy, setCompleteMccHierarchy] = useState(null);
  const [impressions, setImpressions] = useState([]);
  const [ads, setAds] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('hierarchy'); // 'hierarchy', 'complete-mcc', 'customers', 'impressions', 'ads', or 'linking'
  const [selectedCustomerId, setSelectedCustomerId] = useState('');

  // Check for token in URL parameters on component mount
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const tokenParam = urlParams.get('token');

    if (tokenParam) {
      setToken(tokenParam);
      // Clean up the URL
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, []);



  // Define fetchCustomerHierarchy function
  const fetchCustomerHierarchy = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await axios.get('http://localhost:4000/api/customers/hierarchy');
      setCustomerHierarchy(response.data);
    } catch (err) {
      console.error('Error fetching customer hierarchy:', err);
      setError('Failed to fetch Google Ads customer hierarchy. Please try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  // Define fetchCustomers function
  const fetchCustomers = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await axios.get('http://localhost:4000/api/customers');

      setCustomers(response.data);
    } catch (err) {
      console.error('Error fetching customers:', err);
      setError('Failed to fetch Google Ads customers. Please try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  // Define fetchImpressions function
  const fetchImpressions = useCallback(async () => {
    if (!selectedCustomerId) {
      setError('Please select a customer ID first.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await axios.get(`http://localhost:4000/api/impressions/by-country/${selectedCustomerId}`);

      setImpressions(response.data);
    } catch (err) {
      console.error('Error fetching impressions:', err);
      setError('Failed to fetch impression data. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [selectedCustomerId]);

  // Define fetchAds function
  const fetchAds = useCallback(async () => {
    if (!selectedCustomerId) {
      setError('Please select a customer ID first.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await axios.get(`http://localhost:4000/api/ads/final-urls/${selectedCustomerId}`);

      setAds(response.data);
    } catch (err) {
      console.error('Error fetching ads:', err);
      setError('Failed to fetch ad information. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [selectedCustomerId]);

  // Define fetchCompleteMccHierarchy function
  const fetchCompleteMccHierarchy = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await axios.get('http://localhost:4000/api/experimental/mcc/complete-hierarchy');
      setCompleteMccHierarchy(response.data);
    } catch (err) {
      console.error('Error fetching complete MCC hierarchy:', err);
      setError('Failed to fetch complete MCC hierarchy. This experimental feature requires MCC access. Please try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch data when tab changes
  useEffect(() => {
    if (activeTab === 'hierarchy') {
      fetchCustomerHierarchy();
    } else if (activeTab === 'complete-mcc') {
      fetchCompleteMccHierarchy();
    } else if (activeTab === 'customers') {
      fetchCustomers();
    } else if (activeTab === 'impressions' && selectedCustomerId) {
      fetchImpressions();
    } else if (activeTab === 'ads' && selectedCustomerId) {
      fetchAds();
    }
  }, [activeTab, fetchCustomerHierarchy, fetchCompleteMccHierarchy, fetchCustomers, fetchImpressions, fetchAds, selectedCustomerId]);

  // Main application content
  const MainContent = () => {
    // if (!token) {
    //   return (
    //     <div>
    //       <p>Please sign in with your Google account to access the Google Ads API.</p>
    //       <LoginButton />
    //     </div>
    //   );
    // }

    return (
        <div>
          <p>You are signed in!</p>

          {/* Tabs for switching between hierarchy, customers, impressions, ads, and account linking */}
          <div style={{ marginBottom: '20px' }}>

            <button
              onClick={() => setActiveTab('hierarchy')}
              style={{
                padding: '10px 20px',
                backgroundColor: activeTab === 'hierarchy' ? '#4285F4' : '#f1f1f1',
                color: activeTab === 'hierarchy' ? 'white' : 'black',
                border: 'none',
                borderRadius: '4px 0 0 0',
                cursor: 'pointer'
              }}
            >
              Account Hierarchy
            </button>
            <button
              onClick={() => setActiveTab('complete-mcc')}
              style={{
                padding: '10px 20px',
                backgroundColor: activeTab === 'complete-mcc' ? '#4285F4' : '#f1f1f1',
                color: activeTab === 'complete-mcc' ? 'white' : 'black',
                border: 'none',
                borderRadius: '0',
                cursor: 'pointer'
              }}
            >
              Complete MCC Tree
            </button>
            <button
              onClick={() => setActiveTab('customers')}
              style={{
                padding: '10px 20px',
                backgroundColor: activeTab === 'customers' ? '#4285F4' : '#f1f1f1',
                color: activeTab === 'customers' ? 'white' : 'black',
                border: 'none',
                borderRadius: '0',
                cursor: 'pointer'
              }}
            >
              Customers (Flat)
            </button>
            <button
              onClick={() => setActiveTab('impressions')}
              style={{
                padding: '10px 20px',
                backgroundColor: activeTab === 'impressions' ? '#4285F4' : '#f1f1f1',
                color: activeTab === 'impressions' ? 'white' : 'black',
                border: 'none',
                borderRadius: '0',
                cursor: 'pointer'
              }}
            >
              Impressions
            </button>
            <button
              onClick={() => setActiveTab('ads')}
              style={{
                padding: '10px 20px',
                backgroundColor: activeTab === 'ads' ? '#4285F4' : '#f1f1f1',
                color: activeTab === 'ads' ? 'white' : 'black',
                border: 'none',
                borderRadius: '0',
                cursor: 'pointer'
              }}
            >
              Ads
            </button>
            <button
              onClick={() => setActiveTab('linking')}
              style={{
                padding: '10px 20px',
                backgroundColor: activeTab === 'linking' ? '#4285F4' : '#f1f1f1',
                color: activeTab === 'linking' ? 'white' : 'black',
                border: 'none',
                borderRadius: '0 4px 0 0',
                cursor: 'pointer'
              }}
            >
              Account Linking
            </button>
          </div>

          {/* Customer ID input for Impressions and Ads tabs */}
          {(activeTab === 'impressions' || activeTab === 'ads') && (
            <div style={{ marginBottom: '20px', padding: '15px', backgroundColor: '#f9f9f9', borderRadius: '4px' }}>
              <div style={{ marginBottom: '10px' }}>
                <label htmlFor="customerId" style={{ marginRight: '10px' }}>Customer ID:</label>
                <input
                  id="customerId"
                  type="text"
                  value={selectedCustomerId}
                  onChange={(e) => setSelectedCustomerId(e.target.value)}
                  placeholder="Enter Customer ID"
                  style={{ padding: '8px', width: '200px' }}
                />
                <button
                  onClick={activeTab === 'impressions' ? fetchImpressions : fetchAds}
                  style={{
                    marginLeft: '10px',
                    padding: '8px 16px',
                    backgroundColor: '#4285F4',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer'
                  }}
                >
                  Fetch Data
                </button>
              </div>
            </div>
          )}

          {loading ? (
            <p>Loading data...</p>
          ) : error ? (
            <div>
              <p style={{ color: 'red' }}>{error}</p>
              <button
                onClick={
                  activeTab === 'hierarchy' ? fetchCustomerHierarchy :
                  activeTab === 'complete-mcc' ? fetchCompleteMccHierarchy :
                  activeTab === 'customers' ? fetchCustomers :
                  activeTab === 'impressions' ? fetchImpressions : fetchAds
                }
                style={{
                  padding: '8px 16px',
                  backgroundColor: '#4285F4',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Try Again
              </button>
            </div>
          ) : (
            activeTab === 'hierarchy' ? (
              <CustomerHierarchy hierarchy={customerHierarchy} />
            ) : activeTab === 'complete-mcc' ? (
              <CompleteMccHierarchy hierarchyData={completeMccHierarchy} />
            ) : activeTab === 'customers' ? (
              <CustomersList customers={customers} />
            ) : activeTab === 'impressions' ? (
              <ImpressionsList
                impressions={impressions}
                customerId={selectedCustomerId}
              />
            ) : activeTab === 'ads' ? (
              <AdsList
                ads={ads}
                customerId={selectedCustomerId}
              />
            ) : (
              <AccountLinking />
            )
          )}
        </div>
    );
  };

  // Return the main component with routing
  return (
    <Router>
      <div className="App">
        <h1>Google Ads MCC Accounts</h1>
        <Routes>
          {/*<Route path="/oauth/callback" element={<OAuthCallback setToken={setToken} />} />*/}
          <Route path="/" element={<MainContent />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
