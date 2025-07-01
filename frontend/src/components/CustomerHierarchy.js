import React, { useState } from 'react';
import { formatCustomerId } from '../utils/formatters';

const CustomerHierarchy = ({ hierarchy }) => {
  if (!hierarchy || hierarchy.length === 0) {
    return <p>No Google Ads customer hierarchy found.</p>;
  }

  return (
    <div>
      <h2>Google Ads Account Hierarchy</h2>
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
          <HierarchyNode
            key={rootNode.customerId}
            node={rootNode}
            level={0}
            isLast={index === hierarchy.length - 1}
            parentPrefix=""
          />
        ))}
      </div>
    </div>
  );
};

const HierarchyNode = ({ node, level, isLast, parentPrefix }) => {
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
    return {
      color: node.manager ? '#1976d2' : '#388e3c',
      fontWeight: node.manager ? 'bold' : 'normal'
    };
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
        {node.accessRole && (
          <span style={{ marginLeft: '8px', fontSize: '12px', color: '#666' }}>
            - {node.accessRole}
          </span>
        )}
        {node.currencyCode && (
          <span style={{ marginLeft: '8px', fontSize: '12px', color: '#666' }}>
            - {node.currencyCode}
          </span>
        )}
      </div>

      {node.children && node.children.length > 0 && isExpanded && (
        <div>
          {node.children.map((childNode, index) => (
            <HierarchyNode
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

export default CustomerHierarchy;
