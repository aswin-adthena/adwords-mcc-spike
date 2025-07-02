import React, { useState } from 'react';
import { formatCustomerId } from '../utils/formatters';

const CompleteMccHierarchy = ({ hierarchyData }) => {
  if (!hierarchyData) {
    return <p>No MCC hierarchy data available.</p>;
  }

  // Handle both string (raw JSON) and object data
  let parsedData;

  if (typeof hierarchyData === 'string') {
    try {
      parsedData = JSON.parse(hierarchyData);
    } catch (e) {
      return (
        <div>
          <h2>Complete MCC Hierarchy - Flat Structure</h2>
          <p style={{ color: 'red' }}>Error parsing JSON data: {e.message}</p>
        </div>
      );
    }
  } else {
    parsedData = hierarchyData;
  }

  // Extract data from parsed structure
  const {
    entryPointMccIds = [],
    directAccessCustomers = [],
    mccHierarchies = [],
    totalAccounts = 0,
    totalErrors = 0,
    traversalTimeMs = 0,
    error
  } = parsedData;

  if (error) {
    return (
      <div>
        <h2>Complete MCC Hierarchy - Flat Structure</h2>
        <p style={{ color: 'red' }}>Error: {error}</p>
      </div>
    );
  }

  if (!mccHierarchies || mccHierarchies.length === 0) {
    return (
      <div>
        <h2>Complete MCC Hierarchy - Flat Structure</h2>
        <p>No MCC hierarchy found. This could mean:</p>
        <ul>
          <li>You don't have access to any MCC accounts</li>
          <li>The MCC accounts don't have any direct child accounts</li>
          <li>There was an error during retrieval</li>
        </ul>
        {totalErrors > 0 && (
          <p style={{ color: 'red' }}>
            Errors encountered: {totalErrors}
          </p>
        )}
      </div>
    );
  }

  return (
    <div>
      <h2>Complete MCC Hierarchy - Flat Structure</h2>

      {/* Retrieval Statistics */}
      <div style={{
        backgroundColor: '#f8f9fa',
        padding: '12px',
        borderRadius: '6px',
        marginBottom: '16px',
        border: '1px solid #e9ecef'
      }}>
        <h3 style={{ margin: '0 0 8px 0', fontSize: '16px' }}>Retrieval Statistics</h3>
        <div style={{ fontSize: '14px', lineHeight: '1.4' }}>
          <div><strong>Total Accounts:</strong> {totalAccounts}</div>
          <div><strong>MCC Hierarchies Retrieved:</strong> {mccHierarchies.length}</div>
          <div><strong>Retrieval Time:</strong> {traversalTimeMs}ms</div>
          <div><strong>Entry Point MCCs:</strong> {entryPointMccIds.join(', ') || 'None'}</div>
          {totalErrors > 0 && (
            <div style={{ color: '#dc3545' }}>
              <strong>Errors:</strong> {totalErrors}
            </div>
          )}
        </div>
      </div>

      {/* Hierarchical Tree View */}
      <div style={{
        backgroundColor: '#f8f9fa',
        padding: '16px',
        borderRadius: '8px',
        border: '1px solid #e9ecef'
      }}>
        <h3 style={{ margin: '0 0 16px 0', fontSize: '16px' }}>Hierarchical Tree View</h3>
        {mccHierarchies.map((mccHierarchy, index) => (
          <HierarchicalTreeDisplay key={`tree-${mccHierarchy.mccId || index}`} mccHierarchy={mccHierarchy} />
        ))}
      </div>

      {/* Legend */}
      <div style={{
        marginTop: '16px',
        padding: '12px',
        backgroundColor: '#e7f3ff',
        borderRadius: '6px',
        border: '1px solid #b3d9ff'
      }}>
        <h4 style={{ margin: '0 0 8px 0', fontSize: '14px' }}>Legend</h4>
        <div style={{ fontSize: '12px', lineHeight: '1.4' }}>
          <div><span style={{ color: '#1976d2', fontWeight: 'bold' }}>Blue Bold:</span> Manager (MCC) accounts</div>
          <div><span style={{ color: '#388e3c' }}>Green:</span> Client accounts</div>
          <div>📂 Manager accounts, 📄 Client accounts</div>
          <div>Shows only direct children (level 1) of root MCC accounts with case-sensitive sorting</div>
        </div>
      </div>
    </div>
  );
};

const HierarchicalTreeDisplay = ({ mccHierarchy }) => {
  const { mccId, hierarchyTree, totalAccounts = 0, error } = mccHierarchy;

  if (error) {
    return (
      <div style={{
        backgroundColor: '#f8d7da',
        padding: '12px',
        borderRadius: '6px',
        marginBottom: '16px',
        border: '1px solid #f5c6cb'
      }}>
        <h4 style={{ margin: '0 0 8px 0', fontSize: '14px', color: '#721c24' }}>
          MCC {formatCustomerId(mccId)} - Tree View Error
        </h4>
        <p style={{ margin: 0, fontSize: '12px', color: '#721c24' }}>{error}</p>
      </div>
    );
  }

  if (!hierarchyTree || Object.keys(hierarchyTree).length === 0) {
    return (
      <div style={{
        backgroundColor: '#fff3cd',
        padding: '12px',
        borderRadius: '6px',
        marginBottom: '16px',
        border: '1px solid #ffeaa7'
      }}>
        <h4 style={{ margin: '0 0 8px 0', fontSize: '14px', color: '#856404' }}>
          MCC {formatCustomerId(mccId)} - No Tree Data
        </h4>
        <p style={{ margin: 0, fontSize: '12px', color: '#856404' }}>
          No hierarchical tree structure available for this MCC.
        </p>
      </div>
    );
  }

  return (
    <div style={{
      backgroundColor: 'white',
      padding: '16px',
      borderRadius: '6px',
      marginBottom: '16px',
      border: '1px solid #dee2e6'
    }}>
      <h4 style={{ margin: '0 0 12px 0', fontSize: '14px', color: '#1976d2', fontWeight: 'bold' }}>
        🌳 MCC {formatCustomerId(mccId)} Tree Structure ({totalAccounts} total accounts)
      </h4>

      <div style={{
        fontFamily: 'Consolas, "Courier New", monospace',
        fontSize: '12px',
        lineHeight: '1.6',
        backgroundColor: '#f8f9fa',
        padding: '12px',
        borderRadius: '4px',
        border: '1px solid #e9ecef'
      }}>
        <TreeNode node={hierarchyTree} level={0} isLast={true} prefix="" />
      </div>
    </div>
  );
};

const TreeNode = ({ node, level, isLast, prefix }) => {
  const [isExpanded, setIsExpanded] = useState(true);

  const {
    customerId,
    descriptiveName,
    isManager,
    currencyCode,
    timeZone,
    children = []
  } = node;

  const hasChildren = children && children.length > 0;

  const getTreeConnector = () => {
    if (level === 0) return '';
    return isLast ? '└── ' : '├── ';
  };

  const getChildPrefix = () => {
    if (level === 0) return '';
    return prefix + (isLast ? '    ' : '│   ');
  };

  const getIcon = () => {
    if (!hasChildren) return '📄'; // File icon for leaf nodes
    return isExpanded ? '📂' : '📁'; // Open/closed folder icons
  };

  const getNodeStyle = () => ({
    color: isManager ? '#1976d2' : '#388e3c',
    fontWeight: isManager ? 'bold' : 'normal'
  });

  const handleToggle = () => {
    if (hasChildren) {
      setIsExpanded(!isExpanded);
    }
  };

  return (
    <div>
      <div
        style={{
          cursor: hasChildren ? 'pointer' : 'default',
          padding: '2px 0',
          userSelect: 'none'
        }}
        onClick={handleToggle}
      >
        <span style={{ color: '#888', fontFamily: 'monospace' }}>
          {prefix}{getTreeConnector()}
        </span>
        <span style={{ marginRight: '6px' }}>{getIcon()}</span>
        <span style={getNodeStyle()}>
          {formatCustomerId(customerId)}
        </span>
        {descriptiveName && descriptiveName !== 'MCC Root' && (
          <span style={{ marginLeft: '8px', color: '#666' }}>
            ({descriptiveName})
          </span>
        )}
        <span style={{ marginLeft: '8px', fontSize: '11px', color: '#999' }}>
          [{isManager ? 'Manager' : 'Client'}]
        </span>
        {currencyCode && (
          <span style={{ marginLeft: '8px', fontSize: '11px', color: '#666' }}>
            {currencyCode}
          </span>
        )}
        {timeZone && (
          <span style={{ marginLeft: '8px', fontSize: '11px', color: '#666' }}>
            {timeZone}
          </span>
        )}
        {hasChildren && (
          <span style={{ marginLeft: '8px', fontSize: '11px', color: '#999' }}>
            ({children.length} children)
          </span>
        )}
      </div>

      {hasChildren && isExpanded && (
        <div>
          {children.map((child, index) => (
            <TreeNode
              key={child.customerId || index}
              node={child}
              level={level + 1}
              isLast={index === children.length - 1}
              prefix={getChildPrefix()}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default CompleteMccHierarchy;
