# API Documentation

## Overview

This document provides comprehensive API documentation for the SOFT40051 Cloud Storage System services.

## Table of Contents

1. [Load Balancer API](#load-balancer-api)
2. [Aggregator Service API](#aggregator-service-api)
3. [Host Manager API](#host-manager-api)
4. [Common Protocols](#common-protocols)

## Load Balancer API

### Base URL

Internal: `http://load-balancer:6869`

### Endpoints

#### POST /upload

Upload a file through the load balancer.

**Request**

```http
POST /upload HTTP/1.1
Content-Type: multipart/form-data
Authorization: Bearer <token>
```

**Form Data**
- `file`: The file to upload (binary)
- `filename`: Name of the file (string)
- `userId`: User ID (integer)

**Response**

```json
{
  "status": "success",
  "fileId": "abc123",
  "chunks": 4,
  "processingTime": 1234,
  "message": "File uploaded successfully"
}
```

**Status Codes**
- `200 OK`: File uploaded successfully
- `400 Bad Request`: Invalid request format
- `401 Unauthorized`: Invalid or missing authentication
- `413 Payload Too Large`: File exceeds size limit
- `500 Internal Server Error`: Server error occurred
- `503 Service Unavailable`: No healthy backend nodes

#### GET /download/{fileId}

Download a file through the load balancer.

**Request**

```http
GET /download/abc123 HTTP/1.1
Authorization: Bearer <token>
```

**Response**

Binary file content with appropriate headers:

```http
HTTP/1.1 200 OK
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="example.pdf"
Content-Length: 1234567
```

**Status Codes**
- `200 OK`: File downloaded successfully
- `401 Unauthorized`: Invalid or missing authentication
- `403 Forbidden`: User doesn't have permission
- `404 Not Found`: File not found
- `500 Internal Server Error`: Server error occurred

#### GET /health

Check load balancer health status.

**Request**

```http
GET /health HTTP/1.1
```

**Response**

```json
{
  "status": "healthy",
  "timestamp": "2024-01-09T12:00:00Z",
  "backends": {
    "healthy": 2,
    "unhealthy": 0,
    "total": 2
  },
  "queueSize": 5,
  "activeWorkers": 3
}
```

#### GET /stats

Get load balancer statistics.

**Request**

```http
GET /stats HTTP/1.1
Authorization: Bearer <admin_token>
```

**Response**

```json
{
  "totalRequests": 1234,
  "successfulRequests": 1200,
  "failedRequests": 34,
  "averageLatency": 1250,
  "currentQueueSize": 5,
  "schedulerType": "ROUND_ROBIN",
  "backends": [
    {
      "id": "node-1",
      "host": "aggservice-1",
      "port": 9000,
      "healthy": true,
      "requestsProcessed": 600,
      "lastHealthCheck": "2024-01-09T12:00:00Z"
    },
    {
      "id": "node-2",
      "host": "aggservice-2",
      "port": 9000,
      "healthy": true,
      "requestsProcessed": 600,
      "lastHealthCheck": "2024-01-09T12:00:00Z"
    }
  ]
}
```

## Aggregator Service API

### Base URL

Internal: `http://aggregator:9000`

### Endpoints

#### POST /process/upload

Process and store a file.

**Request**

```http
POST /process/upload HTTP/1.1
Content-Type: multipart/form-data
```

**Form Data**
- `file`: File content (binary)
- `filename`: Original filename (string)
- `userId`: User ID (integer)
- `encryption`: Encryption enabled (boolean, default: true)

**Response**

```json
{
  "status": "success",
  "fileId": "abc123",
  "originalSize": 1048576,
  "chunks": [
    {
      "chunkId": "chunk-1",
      "nodeId": "storage-1",
      "checksum": "a1b2c3d4",
      "size": 262144
    },
    {
      "chunkId": "chunk-2",
      "nodeId": "storage-2",
      "checksum": "e5f6g7h8",
      "size": 262144
    }
  ],
  "totalChunks": 4,
  "encrypted": true,
  "processingTime": 850
}
```

#### GET /process/download/{fileId}

Retrieve and reconstruct a file.

**Request**

```http
GET /process/download/abc123 HTTP/1.1
Authorization: Bearer <token>
```

**Response**

Binary file content or error message.

**Status Codes**
- `200 OK`: File retrieved successfully
- `404 Not Found`: File not found
- `500 Internal Server Error`: Error reconstructing file

#### GET /chunks/{fileId}

Get chunk information for a file.

**Request**

```http
GET /chunks/abc123 HTTP/1.1
Authorization: Bearer <token>
```

**Response**

```json
{
  "fileId": "abc123",
  "filename": "example.pdf",
  "totalSize": 1048576,
  "totalChunks": 4,
  "encrypted": true,
  "chunks": [
    {
      "chunkId": "chunk-1",
      "sequence": 0,
      "storageNode": "storage-1",
      "size": 262144,
      "checksum": "a1b2c3d4",
      "status": "stored"
    }
  ]
}
```

#### POST /verify/{fileId}

Verify file integrity.

**Request**

```http
POST /verify/abc123 HTTP/1.1
Authorization: Bearer <token>
```

**Response**

```json
{
  "fileId": "abc123",
  "verified": true,
  "chunksVerified": 4,
  "chunksFailed": 0,
  "details": [
    {
      "chunkId": "chunk-1",
      "verified": true,
      "expectedChecksum": "a1b2c3d4",
      "actualChecksum": "a1b2c3d4"
    }
  ]
}
```

## Host Manager API

### Base URL

Internal: `http://hostmanager:8080`

### Endpoints

#### GET /nodes

List all storage nodes.

**Request**

```http
GET /nodes HTTP/1.1
```

**Response**

```json
{
  "nodes": [
    {
      "nodeId": "storage-1",
      "host": "soft40051-files-container1",
      "port": 22,
      "status": "healthy",
      "capacity": 10737418240,
      "used": 2147483648,
      "available": 8589934592,
      "lastCheck": "2024-01-09T12:00:00Z"
    }
  ],
  "totalNodes": 4,
  "healthyNodes": 4,
  "unhealthyNodes": 0
}
```

#### GET /nodes/{nodeId}

Get details for a specific node.

**Request**

```http
GET /nodes/storage-1 HTTP/1.1
```

**Response**

```json
{
  "nodeId": "storage-1",
  "host": "soft40051-files-container1",
  "port": 22,
  "status": "healthy",
  "capacity": 10737418240,
  "used": 2147483648,
  "available": 8589934592,
  "files": 123,
  "lastCheck": "2024-01-09T12:00:00Z",
  "uptime": 86400
}
```

#### POST /nodes/{nodeId}/health

Perform health check on a node.

**Request**

```http
POST /nodes/storage-1/health HTTP/1.1
```

**Response**

```json
{
  "nodeId": "storage-1",
  "status": "healthy",
  "latency": 15,
  "timestamp": "2024-01-09T12:00:00Z"
}
```

## Common Protocols

### Authentication

All authenticated endpoints require a Bearer token:

```http
Authorization: Bearer <token>
```

Obtain a token through the authentication service (not documented here as it's part of the GUI).

### Error Response Format

All errors follow a consistent format:

```json
{
  "status": "error",
  "code": "FILE_NOT_FOUND",
  "message": "The requested file was not found",
  "timestamp": "2024-01-09T12:00:00Z",
  "requestId": "req-abc123"
}
```

### MQTT Topics

#### System Events

- `system/health`: Health status updates
- `system/alerts`: System alerts and warnings
- `system/metrics`: Performance metrics

#### File Operations

- `files/upload/started`: Upload initiated
- `files/upload/completed`: Upload completed
- `files/upload/failed`: Upload failed
- `files/download/started`: Download initiated
- `files/download/completed`: Download completed

#### Load Balancer

- `loadbalancer/queue`: Queue size updates
- `loadbalancer/scheduling`: Scheduling decisions
- `loadbalancer/health`: Backend health changes

#### Storage Nodes

- `storage/node/{nodeId}/status`: Node status changes
- `storage/node/{nodeId}/capacity`: Capacity updates

### Message Format

MQTT messages use JSON format:

```json
{
  "timestamp": "2024-01-09T12:00:00Z",
  "event": "file.upload.completed",
  "data": {
    "fileId": "abc123",
    "userId": 1,
    "size": 1048576,
    "duration": 1250
  }
}
```

## Rate Limiting

- **Upload**: 10 requests per minute per user
- **Download**: 20 requests per minute per user
- **API calls**: 100 requests per minute per user

Rate limit headers are included in responses:

```http
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1609459200
```

## Pagination

List endpoints support pagination:

```http
GET /files?page=1&perPage=20
```

Response includes pagination metadata:

```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "perPage": 20,
    "total": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

## SDK Examples

### Java

```java
// Upload file
CloudStorageClient client = new CloudStorageClient("http://load-balancer:6869");
client.authenticate(username, password);

File file = new File("example.pdf");
UploadResponse response = client.upload(file);
System.out.println("File ID: " + response.getFileId());

// Download file
byte[] content = client.download(response.getFileId());
Files.write(Paths.get("downloaded.pdf"), content);
```

### Python (Conceptual)

```python
from cloudstorage import CloudStorageClient

# Upload file
client = CloudStorageClient("http://load-balancer:6869")
client.authenticate(username, password)

with open("example.pdf", "rb") as f:
    response = client.upload(f)
    print(f"File ID: {response['fileId']}")

# Download file
content = client.download(response['fileId'])
with open("downloaded.pdf", "wb") as f:
    f.write(content)
```

## Versioning

API version is specified in the URL or header:

```http
GET /v1/files HTTP/1.1
```

or

```http
GET /files HTTP/1.1
X-API-Version: 1
```

Current version: **v1**

## Support

For API support:
- GitHub Issues: Report bugs or request features
- Documentation: Check docs/ directory
- Examples: See examples/ directory (if available)
