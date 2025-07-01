import React, { useState } from 'react';
import { formatCustomerId } from '../utils/formatters';

const CompleteMccHierarchy = ({ hierarchyData }) => {
  if (!hierarchyData) {
    return <p>No MCC hierarchy data available.</p>;
  }

  const { hierarchy, totalAccountsDiscovered, directAccessAccounts, mccDiscoveredAccounts, 
          errorsEncountered, inaccessibleAccounts, maxDepthReached, traversalTimeMs, 
          entryPointMccIds } = hierarchyData;

  if (!hierarchy || hierarchy.length === 0) {
    return (
      <div>
        <h2>Complete MCC Hierarchy (Experimental)</h2>
        <p>No MCC hierarchy found. This could mean:</p>
        <ul>
          <li>You don't have access to any MCC accounts</li>
          <li>The MCC accounts don't have any child accounts</li>
          <li>There was an error during traversal</li>
        </ul>
        {errorsEncountered > 0 && (
          <p style={{ color: 'red' }}>
            Errors encountered: {errorsEncountered}
          </p>
        )}
      </div>
    );
  }

  return (
    <div>
      <h2>Complete MCC Hierarchy (Experimental)</h2>
      
      {/* Traversal Statistics */}
      <div style={{
        backgroundColor: '#f8f9fa',
        padding: '12px',
        borderRadius: '6px',
        marginBottom: '16px',
        border: '1px solid #e9ecef'
      }}>
        <h3 style={{ margin: '0 0 8px 0', fontSize: '16px' }}>Traversal Statistics</h3>
        <div style={{ fontSize: '14px', lineHeight: '1.4' }}>
          <div><strong>Total Accounts:</strong> {totalAccountsDiscovered}</div>
          <div><strong>Direct Access:</strong> {directAccessAccounts}</div>
          <div><strong>Discovered via MCC:</strong> {mccDiscoveredAccounts}</div>
          <div><strong>Max Depth:</strong> {maxDepthReached}</div>
          <div><strong>Traversal Time:</strong> {traversalTimeMs}ms</div>
          <div><strong>Entry Point MCCs:</strong> {entryPointMccIds?.join(', ') || 'None'}</div>
          {errorsEncountered > 0 && (
            <div style={{ color: '#dc3545' }}>
              <strong>Errors:</strong> {errorsEncountered}
            </div>
          )}
          {inaccessibleAccounts && inaccessibleAccounts.length > 0 && (
            <div style={{ color: '#ffc107' }}>
              <strong>Inaccessible Accounts:</strong> {inaccessibleAccounts.length}
            </div>
          )}
        </div>
      </div>

      {/* Hierarchy Tree */}
      <div style={{
        fontFamily: 'Consolas, "Courier New", monospace',
        fontSize: '14px',
        lineHeight: '1.6',
        backgroundColor: '#f8f9fa',
        padding: '16px',
        borderRadius: '8px',
        border: '1px solid #e9ecef',
        overflow: 'auto'
      }}>
        {hierarchy.map((rootNode, index) => (
          <MccHierarchyNode
            key={rootNode.customerId}
            node={rootNode}
            level={0}
            isLast={index === hierarchy.length - 1}
            parentPrefix=""
          />
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
          <div><span style={{ color: '#666' }}>Gray:</span> Account discovered via MCC traversal</div>
          <div>📂/📁 Folder icons indicate manager accounts with/without expanded children</div>
          <div>📄 File icon indicates client accounts (leaf nodes)</div>
        </div>
      </div>
    </div>
  );
};

const MccHierarchyNode = ({ node, level, isLast, parentPrefix }) => {
  const [isExpanded, setIsExpanded] = useState(true);
  const [isHovered, setIsHovered] = useState(false);

  const toggleExpanded = () => {
    if (node.children && node.children.length > 0) {
      setIsExpanded(!isExpanded);
    }
  };

  const getTreePrefix = () => {
    if (level === 0) return '';

    const connector = isLast ? '└─ ' : '├─ ';
    return parentPrefix + connector;
  };

  const getChildPrefix = () => {
    if (level === 0) return '';

    const extension = isLast ? '   ' : '│  ';
    return parentPrefix + extension;
  };

  const getIcon = () => {
    if (!node.children || node.children.length === 0) {
      return '📄'; // File icon for client accounts (leaf nodes)
    }
    return isExpanded ? '📂' : '📁'; // Open/closed folder icons for manager accounts
  };

  const getAccountTypeStyle = () => {
    const baseStyle = {
      fontWeight: node.isManager ? 'bold' : 'normal'
    };

    if (node.accessLevel === 'Direct Access') {
      baseStyle.color = node.isManager ? '#1976d2' : '#388e3c';
    } else {
      baseStyle.color = '#666'; // Gray for accounts discovered via MCC
    }

    return baseStyle;
  };

  const nodeStyle = {
    cursor: (node.children && node.children.length > 0) ? 'pointer' : 'default',
    padding: '3px 6px',
    userSelect: 'none',
    whiteSpace: 'pre',
    borderRadius: '4px',
    transition: 'background-color 0.2s ease',
    backgroundColor: isHovered && node.children && node.children.length > 0 ? '#e3f2fd' : 'transparent'
  };

  return (
    <div>
      <div
        style={nodeStyle}
        onClick={toggleExpanded}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
      >
        <span style={{ color: '#888', fontFamily: 'monospace' }}>
          {getTreePrefix()}
        </span>
        <span style={{ marginRight: '6px' }}>{getIcon()}</span>
        <span style={getAccountTypeStyle()}>
          {formatCustomerId(node.customerId)}
        </span>
        {node.descriptiveName && (
          <span style={{ marginLeft: '8px', color: '#666' }}>
            ({node.descriptiveName})
          </span>
        )}
        <span style={{ marginLeft: '8px', fontSize: '12px', color: '#999' }}>
          [{node.accountType}]
        </span>
        <span style={{ marginLeft: '8px', fontSize: '12px', color: '#666' }}>
          - {node.accessLevel}
        </span>
        {node.currencyCode && (
          <span style={{ marginLeft: '8px', fontSize: '12px', color: '#666' }}>
            - {node.currencyCode}
          </span>
        )}
        {node.discoveredViaMccId && node.discoveredViaMccId !== node.customerId && (
          <span style={{ marginLeft: '8px', fontSize: '11px', color: '#999', fontStyle: 'italic' }}>
            (via {formatCustomerId(node.discoveredViaMccId)})
          </span>
        )}
      </div>

      {node.children && node.children.length > 0 && isExpanded && (
        <div>
          {node.children.map((childNode, index) => (
            <MccHierarchyNode
              key={childNode.customerId}
              node={childNode}
              level={level + 1}
              isLast={index === node.children.length - 1}
              parentPrefix={getChildPrefix()}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default CompleteMccHierarchy;
